/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#define _GNU_SOURCE
#define _DEFAULT_SOURCE
#define _POSIX_C_SOURCE 200809L
#define _XOPEN_SOURCE   600


#include <errno.h>
#include <fcntl.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <poll.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

#ifndef TCP_FSTATE_USE_INOTIFY
#define TCP_FSTATE_USE_INOTIFY 1
#endif

#if TCP_FSTATE_USE_INOTIFY
#include <sys/inotify.h>
#endif


static volatile sig_atomic_t g_stop       = 0;
static volatile sig_atomic_t g_child_usr1 = 0;
static volatile sig_atomic_t g_child_stop = 0;


static void set_cloexec(const int fd)
{
    const int flags = fcntl(fd, F_GETFD);

    if(flags < 0) {
        perror("fcntl");
        exit(1);
    }

    if( fcntl(fd, F_SETFD, flags | FD_CLOEXEC) < 0 ) {
        perror("fcntl");
        exit(1);
    }
}


static void set_nonblock(const int fd)
{
    const int flags = fcntl(fd, F_GETFL);

    if(flags < 0) {
        perror("fcntl");
        exit(1);
    }

    if( fcntl(fd, F_SETFL, flags | O_NONBLOCK) < 0 ) {
        perror("fcntl");
        exit(1);
    }
}


static void on_sigterm(const int signo)
{
    (void) signo;

    g_stop = 1;
}


static void on_sigusr1(const int signo)
{
    (void) signo;

    g_child_usr1 = 1;
}


static void on_child_stop(const int signo)
{
    (void) signo;

    g_child_stop = 1;
}


static void install_handler(const int signo, void (*fn)(int))
{
    struct sigaction sa;

    memset( &sa, 0, sizeof(sa) );
    sa.sa_handler = fn;
    sigemptyset(&sa.sa_mask);

    if( sigaction(signo, &sa, NULL) < 0 ) {
        perror("sigaction");
        exit(1);
    }
}


static int read_state(const char* path)
{
    char c = 0;

    // Missing file means ASCII NUL
    const int fd = open(path, O_RDONLY);
    if(fd < 0) {
        if(errno == ENOENT) return 0;
        return 0;
    }

    while(1) {

        // Read only the first byte
        const ssize_t rd = read(fd, &c, 1);

        if(rd > 0) {
            close(fd);
            return (unsigned char) c;
        }

        // Empty file means ASCII NUL
        if(rd == 0) {
            close(fd);
            return 0;
        }

        if(errno == EINTR) continue;

        // Any read failure is treated as ASCII NUL for robustness
        close(fd);
        return 0;

    } // while
}


static int update_states(unsigned char* shared, const int n_files, char** argv)
{
    int changed = 0;

    // argv[2...] are the monitored file paths; read only the first byte of each
    // ('1' = device connected, '0' or missing = disconnected)
    for(int i = 0; i < n_files; ++i) {
        const unsigned char state = (unsigned char) read_state(argv[i + 2]);

        if(shared[i] != state) {
            shared[i] = state;
            changed = 1;
        }
    }

    return changed;
}


static void reap_children(pid_t* children, int* n_children)
{
    while(1) {

        int status = 0;
        const pid_t pid = waitpid(-1, &status, WNOHANG);

        if(pid <= 0) break;

        for(int i = 0; i < *n_children; ++i) {
            if(children[i] == pid) {
                children[i] = children[*n_children - 1];
                --(*n_children);
                break;
            }
        }

    } // while
}


static void broadcast_children(pid_t* children, const int n_children)
{
    // Notify every connected child that new shared states are ready
    for(int i = 0; i < n_children; ++i) {
        if(children[i] > 0) kill(children[i], SIGUSR1);
    }
}


static int wait_writable_or_readable(const int net_fd, const sigset_t* wait_mask, int* got_usr1, int* got_stop)
{
    struct pollfd pfd;

    pfd.fd      = net_fd;
    pfd.events  = POLLIN | POLLOUT;
    pfd.revents = 0;

    while(1) {
        const int pr = ppoll(&pfd, 1, NULL, wait_mask);

        if(pr > 0) {
            if( pfd.revents & (POLLHUP | POLLERR | POLLNVAL) ) return -1;
            return pfd.revents;
        }

        if(pr < 0 && errno == EINTR) {
            if(g_child_usr1) {
                *got_usr1 = 1;
                g_child_usr1 = 0;
            }

            if(g_child_stop) *got_stop = 1;

            if(*got_stop) return -1;
            continue;
        }

        if(pr < 0) return -1;
    }
}


static int drain_client_input(const int net_fd)
{
    char tmp[256];

    while(1) {

        const ssize_t rd = read( net_fd, tmp, sizeof(tmp) );

        if(rd > 0) continue;
        if(rd == 0) return 1;
        if(errno == EINTR) continue;
        if(errno == EAGAIN || errno == EWOULDBLOCK) break;
        return -1;

    } // while

    return 0;
}


static int send_frame(const int net_fd, unsigned char* shared, const int n_files, unsigned char* outbuf, const sigset_t* wait_mask, int* got_usr1, int* got_stop)
{
    // Drain and discard all client input before sending a fresh frame
    if( drain_client_input(net_fd) < 0 ) return -1;

    // Snapshot the shared states into a local packet buffer
    outbuf[0] = 0x02; // STX

    for(int i = 0; i < n_files; ++i) outbuf[i + 1] = shared[i];

    outbuf[n_files + 1] = 0x03; // ETX

          ssize_t off = 0;
    const ssize_t len = n_files + 2;

    // Keep writing until the full packet is sent or the client is gone
    while(off < len) {

        const ssize_t wr = write(net_fd, outbuf + off, len - off);

        if(wr > 0) {
            off += wr;
            continue;
        }

        if(wr < 0 && errno == EINTR) continue;

        if( wr < 0 && (errno == EAGAIN || errno == EWOULDBLOCK) ) {
            const int ev = wait_writable_or_readable(net_fd, wait_mask, got_usr1, got_stop);

            if(ev < 0) return -1;

            // If the client sent more data while we were blocked, discard it first
            if(ev & POLLIN) {
                if( drain_client_input(net_fd) < 0 ) return -1;
            }

            continue;
        }

        return -1;

    } // while

    return 0;
}


static void child_main(const int net_fd, unsigned char* shared, const int n_files)
{
    signal(SIGPIPE, SIG_IGN);

    install_handler(SIGUSR1, on_sigusr1   );
    install_handler(SIGTERM, on_child_stop);
    install_handler(SIGINT , on_child_stop);

    sigset_t blocked;
    sigset_t wait_mask;

    sigemptyset(&blocked);
    sigaddset(&blocked, SIGUSR1);
    sigaddset(&blocked, SIGTERM);
    sigaddset(&blocked, SIGINT );

    if( sigprocmask(SIG_BLOCK, &blocked, NULL) < 0 ) {
        perror("sigprocmask");
        close(net_fd);
        exit(1);
    }

    sigfillset(&wait_mask);
    sigdelset(&wait_mask, SIGUSR1);
    sigdelset(&wait_mask, SIGTERM);
    sigdelset(&wait_mask, SIGINT );

    unsigned char* outbuf = malloc( (size_t) n_files + 2 );

    if(outbuf == NULL) {
        perror("malloc");
        close(net_fd);
        exit(1);
    }

    int got_usr1 = 1;  // Send initial state immediately
    int got_stop = 0;

    g_child_usr1 = 0;
    g_child_stop = 0;

    while(!got_stop) {

        if(got_usr1) {
            got_usr1 = 0;

            if( send_frame(net_fd, shared, n_files, outbuf, &wait_mask, &got_usr1, &got_stop) < 0 ) break;
            if(got_stop) break;
        }

        struct pollfd pfd;

        pfd.fd      = net_fd;
        pfd.events  = POLLIN;
        pfd.revents = 0;

        const int pr = ppoll(&pfd, 1, NULL, &wait_mask);

        if(pr < 0) {
            if(errno == EINTR) {
                if(g_child_usr1) {
                    got_usr1 = 1;
                    g_child_usr1 = 0;
                }

                if(g_child_stop) {
                    got_stop = 1;
                }

                continue;
            }

            perror("ppoll");
            break;
        }

        if( pfd.revents & (POLLHUP | POLLERR | POLLNVAL) ) break;

        if( pfd.revents & POLLIN ) {
            if( send_frame(net_fd, shared, n_files, outbuf, &wait_mask, &got_usr1, &got_stop) < 0 ) break;
        }
    }

    free(outbuf);
    close(net_fd);
    exit(0);
}


#if TCP_FSTATE_USE_INOTIFY

struct watch_item {
    int   wd;
    int   dir_wd;
    char* path;
    char* dir;
    char* name;
};


static char* dup_str(const char* s)
{
    const size_t len = strlen(s);
    char* out = malloc(len + 1);

    if(out == NULL) {
        perror("malloc");
        exit(1);
    }

    memcpy(out, s, len + 1);

    return out;
}


static void split_path(const char* path, char** out_dir, char** out_name)
{
    const char* slash = strrchr(path, '/');

    if(slash == NULL) {
        *out_dir  = dup_str(".");
        *out_name = dup_str(path);
        return;
    }

    if(slash == path) {
        char* dir = malloc(2);
        if(dir == NULL) {
            perror("malloc");
            exit(1);
        }

        char* name = dup_str(slash + 1);

        dir[0] = '/';
        dir[1] = '\0';

        *out_dir  = dir;
        *out_name = name;
        return;
    }

    {
        const size_t dir_len  = (size_t) (slash - path);
        const size_t name_len = strlen(slash + 1);

        char* dir  = malloc(dir_len  + 1);
        char* name = malloc(name_len + 1);

        if(dir == NULL || name == NULL) {
            free(dir);
            free(name);
            perror("malloc");
            exit(1);
        }

        memcpy(dir, path, dir_len);
        dir[dir_len] = '\0';

        memcpy(name, slash + 1, name_len + 1);

        *out_dir  = dir;
        *out_name = name;
    }
}


static void free_watch_items(struct watch_item* items, const int n_files)
{
    if(items == NULL) return;

    for(int i = 0; i < n_files; ++i) {
        free(items[i].path);
        free(items[i].dir );
        free(items[i].name);
    }

    free(items);
}


static int is_relevant_event(const struct watch_item* items, const int n_files, const struct inotify_event* ev)
{
    for(int i = 0; i < n_files; ++i) {

        if(items[i].wd == ev->wd) return 1;

        if( items[i].dir_wd == ev->wd &&
            ev->len > 0 &&
            strcmp(items[i].name, ev->name) == 0
        ) return 1;

    } // for

    return 0;
}


static int setup_inotify(struct watch_item** out_items, const int n_files, char** argv)
{
    const int ifd = inotify_init1(IN_NONBLOCK | IN_CLOEXEC);

    if(ifd < 0) {
        perror("inotify_init1");
        exit(1);
    }

    struct watch_item* items = calloc( (size_t) n_files, sizeof(*items) );

    if(items == NULL) {
        perror("calloc");
        close(ifd);
        exit(1);
    }

    for(int i = 0; i < n_files; ++i) {
        items[i].wd     = -1;
        items[i].dir_wd = -1;

        items[i].path = dup_str(argv[i + 2]);
        split_path(items[i].path, &items[i].dir, &items[i].name);

        items[i].wd = inotify_add_watch(
            ifd,
            items[i].path,
            IN_CLOSE_WRITE | IN_ATTRIB | IN_DELETE_SELF | IN_MOVE_SELF | IN_MODIFY
        );

        items[i].dir_wd = inotify_add_watch(
            ifd,
            items[i].dir,
            IN_CREATE | IN_DELETE | IN_MOVED_FROM | IN_MOVED_TO | IN_ATTRIB | IN_CLOSE_WRITE
        );
    }

    *out_items = items;

    return ifd;
}

#endif


static int accept_client(const int srv_fd)
{
    int fd;

#if defined(SOCK_CLOEXEC) && defined(SOCK_NONBLOCK)
    fd = accept4(srv_fd, NULL, NULL, SOCK_CLOEXEC | SOCK_NONBLOCK);

    if(fd >= 0) return fd;

    if(errno != ENOSYS && errno != EINVAL) return -1;
#endif

    fd = accept(srv_fd, NULL, NULL);

    if(fd < 0) return -1;

    set_cloexec(fd);
    set_nonblock(fd);

    return fd;
}


int main(int argc, char** argv)
{
    // Ignore SIGPIPE so the daemon does not die on failed writes
    signal(SIGPIPE, SIG_IGN);

    install_handler(SIGTERM, on_sigterm);
    install_handler(SIGINT , on_sigterm);

    if(argc < 3) {
        errno = EINVAL;
        perror("argc");
        exit(1);
    }

    const int port = atoi(argv[1]);

    if(port <= 1023 || port > 65535) {
        errno = EINVAL;
        perror("port");
        exit(1);
    }

    const int n_files = argc - 2;

    // Shared anonymous memory holds one state byte per monitored file; all forked children
    // read from it and the parent writes to it, giving zero-copy state distribution
    unsigned char* shared = mmap(
        NULL,
        n_files,
        PROT_READ | PROT_WRITE,
        MAP_SHARED | MAP_ANONYMOUS,
        -1,
        0
    );

    if(shared == MAP_FAILED) {
        perror("mmap");
        exit(1);
    }

    memset(shared, 0, n_files);

    // Create listener socket
    const int srv_fd = socket(AF_INET, SOCK_STREAM, 0);

    if(srv_fd < 0) {
        perror("socket");
        munmap(shared, n_files);
        exit(1);
    }

    set_cloexec(srv_fd);
    set_nonblock(srv_fd);

    // Mimics:
    //     usr/bin/socat tcp-listen:<port>,reuseaddr,fork,sndbuf=2048,keepalive,keepidle=5,keepintvl=3,keepcnt=8 exec:...
    int opt;

    opt = 1   ; setsockopt( srv_fd, SOL_SOCKET , SO_REUSEADDR , &opt, sizeof(opt) ); // reuseaddr
    opt = 1   ; setsockopt( srv_fd, SOL_SOCKET , SO_KEEPALIVE , &opt, sizeof(opt) ); // keepalive + timings
    opt = 5   ; setsockopt( srv_fd, IPPROTO_TCP, TCP_KEEPIDLE , &opt, sizeof(opt) ); // ---
    opt = 3   ; setsockopt( srv_fd, IPPROTO_TCP, TCP_KEEPINTVL, &opt, sizeof(opt) ); // ---
    opt = 8   ; setsockopt( srv_fd, IPPROTO_TCP, TCP_KEEPCNT  , &opt, sizeof(opt) ); // ---
    opt = 2048; setsockopt( srv_fd, SOL_SOCKET , SO_SNDBUF    , &opt, sizeof(opt) ); // sndbuf

    struct linger lng = {
        .l_onoff  = 0,
        .l_linger = 0
    };

    // Avoid RST on close
    setsockopt( srv_fd, SOL_SOCKET, SO_LINGER, &lng, sizeof(lng) );

    struct sockaddr_in addr;

    memset( &addr, 0, sizeof(addr) );

    addr.sin_family      = AF_INET;
    addr.sin_addr.s_addr = htonl(INADDR_ANY);
    addr.sin_port        = htons(port);

    if( bind( srv_fd, (struct sockaddr*) &addr, sizeof(addr) ) < 0 ) {
        perror("bind");
        close(srv_fd);
        munmap(shared, n_files);
        exit(1);
    }

    if( listen(srv_fd, 16) < 0 ) {
        perror("listen");
        close(srv_fd);
        munmap(shared, n_files);
        exit(1);
    }

    pid_t* children  = NULL;
    int n_children   = 0;
    int cap_children = 0;

#if TCP_FSTATE_USE_INOTIFY
    struct watch_item* watch_items = NULL;
    const int ifd = setup_inotify(&watch_items, n_files, argv);
#endif

    // Initialize all states once before serving clients
    (void) update_states(shared, n_files, argv);

    while(!g_stop) {

        // Reap disconnected children
        reap_children(children, &n_children);

        struct pollfd fds[2];
        nfds_t nfds = 1;

        fds[0].fd      = srv_fd;
        fds[0].events  = POLLIN;
        fds[0].revents = 0;

#if TCP_FSTATE_USE_INOTIFY
        fds[1].fd      = ifd;
        fds[1].events  = POLLIN;
        fds[1].revents = 0;
        nfds = 2;
#endif

        int pr;

        do {

#if TCP_FSTATE_USE_INOTIFY
            pr = poll(fds, nfds, -1);
#else
            pr = poll(fds, nfds, 200);
#endif

        } while(pr < 0 && errno == EINTR && !g_stop);

        if(g_stop) break;

        if(pr < 0) {
            perror("poll");
            break;
        }

#if TCP_FSTATE_USE_INOTIFY
        if(fds[1].revents & POLLIN) {
            int  changed = 0;
            char buf[4096] __attribute__ ( ( aligned( __alignof__(struct inotify_event) ) ) );

            while(1) {
                const ssize_t rd = read( ifd, buf, sizeof(buf) );

                if(rd < 0) {
                    if(errno == EINTR) continue;
                    if(errno == EAGAIN || errno == EWOULDBLOCK) break;
                    perror("read");
                    g_stop = 1;
                    break;
                }

                if(rd == 0) break;

                ssize_t off = 0;

                while(off < rd) {
                    const struct inotify_event* ev = (const struct inotify_event*) ( buf + off );

                    if( is_relevant_event(watch_items, n_files, ev) ) changed = 1;

                    off += sizeof(*ev) + ev->len;
                }
            }

            if(g_stop) break;

            if(changed) {
                if( update_states(shared, n_files, argv) ) {
                    broadcast_children(children, n_children);
                }

                // Re-arm without rm_watch
                for(int i = 0; i < n_files; ++i) {
                    const int wd = inotify_add_watch(
                        ifd,
                        watch_items[i].path,
                        IN_CLOSE_WRITE | IN_ATTRIB | IN_DELETE_SELF | IN_MOVE_SELF | IN_MODIFY
                    );

                    if(wd >= 0) watch_items[i].wd = wd;

                    const int dir_wd = inotify_add_watch(
                        ifd,
                        watch_items[i].dir,
                        IN_CREATE | IN_DELETE | IN_MOVED_FROM | IN_MOVED_TO | IN_ATTRIB | IN_CLOSE_WRITE
                    );

                    if(dir_wd >= 0) watch_items[i].dir_wd = dir_wd;
                }
            }
        }
#else
        if( update_states(shared, n_files, argv) ) {
            broadcast_children(children, n_children);
        }
#endif

        if(fds[0].revents & POLLIN) {
            // Accept all queued clients
            while(1) {

                const int new_fd = accept_client(srv_fd);

                if(new_fd < 0) {
                    if(errno == EINTR) continue;
                    if(errno == EAGAIN || errno == EWOULDBLOCK) break;
                    perror("accept");
                    g_stop = 1;
                    break;
                }

                const pid_t pid = fork();

                if(pid < 0) {
                    perror("fork");
                    close(new_fd);
                    continue;
                }

                if(pid == 0) {
#if TCP_FSTATE_USE_INOTIFY
                    close(ifd);
                    free_watch_items(watch_items, n_files);
#endif
                    close(srv_fd);
                    free(children);

                    // New client child serves this socket until the client disconnects
                    child_main(new_fd, shared, n_files);
                }

                close(new_fd);

                if(n_children >= cap_children) {
                    const int new_cap = (cap_children > 0) ? (cap_children * 2) : 16;
                    pid_t* tmp = realloc( children, (size_t) new_cap * sizeof(*children) );

                    if(tmp == NULL) {
                        perror("realloc");
                        kill(pid, SIGTERM);
                        waitpid(pid, NULL, 0);
                        continue;
                    }

                    children     = tmp;
                    cap_children = new_cap;
                }

                children[n_children++] = pid;

            } // while
        }

    } // while

    for(int i = 0; i < n_children; ++i) {
        if(children[i] > 0) kill(children[i], SIGTERM);
    }

    for(int i = 0; i < n_children; ++i) {
        if(children[i] > 0) waitpid(children[i], NULL, 0);
    }

#if TCP_FSTATE_USE_INOTIFY
    free_watch_items(watch_items, n_files);
    close(ifd);
#endif

    free(children);
    close(srv_fd);
    munmap(shared, n_files);

    return 0;
}
