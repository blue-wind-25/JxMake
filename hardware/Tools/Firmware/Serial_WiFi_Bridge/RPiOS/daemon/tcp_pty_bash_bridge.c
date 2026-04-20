/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#define _GNU_SOURCE
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
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>


static void run_session(const int net_fd)
{
    // Loop so that a new bash is spawned automatically each time the previous one exits.
    // The outer caller (accept loop) handles client disconnect by returning from this function.
    while(1) {

        // Open a new pseudoterminal master device
        const int master_fd = posix_openpt(O_RDWR | O_NOCTTY);

        if(master_fd < 0) {
            perror("posix_openpt");
            exit(1);
        }

        if( fcntl(master_fd, F_SETFD, FD_CLOEXEC) < 0 ) { // Prevent leakage into future execs
            perror("fcntl");
            close(master_fd);
            exit(1);
        }

        if( grantpt(master_fd) < 0 ) { // Adjust permissions on the slave side to ensure the slave device can be opened by the calling process
            perror("grantpt");
            close(master_fd);
            exit(1);
        }

        if( unlockpt(master_fd) < 0 ) { // Unlock the slave side
            perror("unlockpt");
            close(master_fd);
            exit(1);
        }

        char slave_name_buf[128];
        if( ptsname_r( master_fd, slave_name_buf, sizeof(slave_name_buf) ) != 0 ) { // Get the '/dev/pts/N' path
            perror("ptsname_r");
            close(master_fd);
            exit(1);
        }

        const char* slave_name = slave_name_buf;

        const pid_t pid = fork();

        // Fork
        if(pid < 0) {
            perror("fork");
            close(master_fd);
            exit(1);
        }

        if(pid == 0) {
            // Break from parent session
            if( setsid() < 0 ) {
                perror("setsid");
                exit(1);
            }

            // Open slave and force it as the controlling terminal
            const int slave_fd = open(slave_name, O_RDWR);

            if(slave_fd < 0) {
                perror("open");
                exit(1);
            }

            if( fcntl(slave_fd, F_SETFD, FD_CLOEXEC) < 0 ) { // Prevent possible leakage into future execs
                perror("fcntl");
                close(slave_fd);
                exit(1);
            }

            // TIOCSCTTY with '1' forces it even if another terminal is stolen
            if( ioctl(slave_fd, TIOCSCTTY, 1) < 0 ) {
                perror("ioctl");
                close(slave_fd);
                exit(1);
            }

            // Set the Slave TTY to 'Sane' but Raw-ish
            struct termios t;

            if( tcgetattr(slave_fd, &t) < 0 ) {
                perror("tcgetattr");
                close(slave_fd);
                exit(1);
            }

            t.c_iflag     |= ICRNL;                                 // Input   flags      : map CR → NL so Enter works
            t.c_oflag     |= OPOST | ONLCR;                         // Output  flags      : expand NL → CRNL so lines look correct
            t.c_cflag     |= CS8   | CREAD  | CLOCAL;               // Control flags      : 8‑bit chars, enable receiver, ignore modem control
            t.c_lflag     |= ISIG  | ICANON | ECHO | ECHOE | ECHOK; // Local   flags      : canonical mode, signals enabled, echo enabled
            t.c_cc[VINTR]  = 0x03;                                  // Control characters : explicitly map VINTR (^C) to 0x03

            if( tcsetattr(slave_fd, TCSANOW, &t) < 0 ) {
                perror("tcsetattr");
                close(slave_fd);
                exit(1);
            }

            // Update the foreground process group
            const pid_t my_pid = getpid();

            if( setpgid(my_pid, my_pid) < 0 ) { // Put the child into its own process group
                if(errno != EPERM) perror("setpgid");
            }

            if( tcsetpgrp(slave_fd, my_pid) < 0 ) { // Make that process group the foreground group for the slave terminal
                perror("tcsetpgrp");
                close(slave_fd);
                exit(1);
            }

            if(!1) {
                struct winsize ws = {
                    .ws_row    = 30 , // Standard "comfortable" height
                    .ws_col    = 100, // Standard "comfortable" width
                    .ws_xpixel = 0  ,
                    .ws_ypixel = 0
                };

                if( ioctl(slave_fd, TIOCSWINSZ, &ws) < 0 ) {
                    perror("ioctl");
                }
            }

            // Redirect and Execute
            if( dup2(slave_fd, 0) < 0 ) {
                perror("dup2");
                close(slave_fd);
                exit(1);
            }

            if( dup2(slave_fd, 1) < 0 ) {
                perror("dup2");
                close(slave_fd);
                exit(1);
            }

            if( dup2(slave_fd, 2) < 0 ) {
                perror("dup2");
                close(slave_fd);
                exit(1);
            }

            if( close(slave_fd) < 0 ) {
                perror("close");
                exit(1);
            }

            execlp("bash", "bash", "-im", NULL); // Use '-im' to force interactive and monitor mode
            perror("execlp");
            exit(1);
        }

        else {
            // Parent - bridge data between net_fd (TCP client) and master_fd (PTY master).
            //
            // The 'bash_running' variable tracks whether the bash child is still alive; set to 0 on any unrecoverable error
            // or on PTY hangup so the loop exits and bash is reaped.
            int bash_running = 1;

            while(bash_running) {

                struct pollfd fds[2];

                fds[0].fd = net_fd;    fds[0].events = POLLIN;
                fds[1].fd = master_fd; fds[1].events = POLLIN;

                int pr;

                do {

                    pr = poll(fds, 2, -1);

                } while(pr < 0 && errno == EINTR);

                if( pr < 0 ) break;

                if( fds[0].revents & (POLLHUP | POLLERR | POLLNVAL) ) {
                    sleep(1);
                    if( kill(-pid, SIGKILL) < 0 ) perror("kill");
                    waitpid(pid, NULL, 0);
                    if( close(master_fd) < 0 ) perror("close");
                    return;
                }

                if(fds[0].revents & POLLIN) {
                          char    buf[256];
                    const ssize_t n = read( net_fd, buf, sizeof(buf) );
                    if(n <= 0) {
                        // The client disconnected -  we must kill the current bash and exit the function
                        sleep(1);
                        if( kill(-pid, SIGKILL) < 0 ) perror("kill");
                        waitpid(pid, NULL, 0);
                        if( close(master_fd) < 0 ) perror("close");
                        return;
                    }

                    // Write data to the PTY master in chunks; Ctrl+C is handled separately and its raw byte is not
                    // forwarded to avoid a second SIGINT from the line discipline (ISIG + VINTR) on top of the one
                    // already sent via kill
                    ssize_t start = 0;

                    while(start < n && bash_running) {

                        // Find the next Ctrl+C byte in the remaining window
                        const ssize_t limit     = n - start;
                        const char*   ctrlc_ptr = (const char*) memchr( buf + start, 0x03, (size_t) limit );
                        const ssize_t chunk     = ctrlc_ptr ? ( ctrlc_ptr - (buf + start) ) : limit;

                        // Write the bytes before the Ctrl+C (or all remaining bytes if there is no Ctrl+C)
                        ssize_t off = 0;

                        while(off < chunk) {

                            const ssize_t w = write( master_fd, buf + start + off, (size_t) (chunk - off) );

                            if(w > 0) {
                                off += w;
                                continue;
                            }

                            if(w < 0 && errno == EINTR) continue;

                            perror("write");
                            bash_running = 0;
                            break;

                        } // while

                        if(!bash_running) break;

                        start += chunk;

                        if(ctrlc_ptr) {

                            ++start; // Advance past the Ctrl+C byte itself; do not write it to the PTY

                            dprintf(STDERR_FILENO, "\n[LOG] Bridge detected Ctrl+C\n");

                            // Try the official way - get the foreground process group from the PTY master
                            pid_t fg_pgid;

                            if( ioctl(master_fd, TIOCGPGRP, &fg_pgid) == 0 && fg_pgid > 0 ) {
                                // Kill the group
                                if( kill(-fg_pgid, SIGINT) == -1 ) perror("kill failed");
                            }
                            else {
                                perror("ioctl");
                            }

                            // Safety net - send SIGINT directly to the bash child PID
                            if( kill(pid, SIGINT) < 0 ) perror("kill");

                            // Flush the output side of master so stale output does not confuse the client
                            if( tcflush(master_fd, TCOFLUSH) < 0 ) perror("tcflush");

                        } // if

                    } // while
                }

                if(fds[1].revents & POLLIN) {
                    char    buf[256];
                    const ssize_t n = read( master_fd, buf, sizeof(buf) );
                    if(n <= 0) {
                        bash_running = 0; // Bash closed the PTY
                    }
                    else {
                        ssize_t off = 0;

                        while(off < n) {
                            const ssize_t w = write( net_fd, buf + off, (size_t) (n - off) );

                            if(w < 0) {
                                if(errno == EINTR) continue;
                                perror("write");
                                bash_running = 0;
                                break;
                            }

                            off += w;
                        }
                    }
                }

                // Explicitly check for PTY hangup (bash exiting)
                if( fds[1].revents & (POLLHUP | POLLERR | POLLNVAL) ) {
                    bash_running = 0;
                }

            } // while

            // Block to reap the specific bash process before restarting
            waitpid(pid, NULL, 0);

            if( close(master_fd) < 0 ) perror("close");
        }

    } // while
}


int main()
{
    struct sigaction sa;

    // Ignore SIGPIPE so the daemon does not die on failed writes
    memset( &sa, 0, sizeof(sa) );
    sa.sa_handler = SIG_IGN;
    sigemptyset(&sa.sa_mask);

    if( sigaction(SIGPIPE, &sa, NULL) < 0 ) {
        perror("sigaction");
        exit(1);
    }

    /*
    // Force stderr to null if the journal is wonky
    int fd = open("/dev/null", O_RDWR);
    dup2(fd, 2);
    close(fd);
    //*/

    // # SIG_IGN for SIGCHLD causes the kernel to auto-reap per-connection child processes so the parent accept loop
    //   never needs to call waitpid for them.
    // # Each child resets SIGCHLD to SIG_DFL so it can waitpid for its own bash grandchild.
    memset( &sa, 0, sizeof(sa) );
    sa.sa_handler = SIG_IGN;
    sigemptyset(&sa.sa_mask);
    if( sigaction(SIGCHLD, &sa, NULL) < 0 ) {
        perror("sigaction");
        exit(1);
    }

    // Create listener socket
    const int srv_fd = socket(AF_INET, SOCK_STREAM, 0);

    if(srv_fd < 0) {
        perror("socket");
        exit(1);
    }

    // Prevent leakage into future execs (children will close it anyway)
    if( fcntl(srv_fd, F_SETFD, FD_CLOEXEC) < 0 ) {
        perror("fcntl");
        close(srv_fd);
        exit(1);
    }

    // Mimics:
    //     usr/bin/socat tcp-listen:2525,reuseaddr,fork,sndbuf=2048,keepalive,keepidle=5,keepintvl=3,keepcnt=8 exec:/mnt/data/pi/daemon/tcp_pty_bash_bridge.elf
    int opt;

    opt = 1   ; setsockopt( srv_fd, SOL_SOCKET , SO_REUSEADDR , &opt, sizeof(opt) ); // reuseaddr
    opt = 1   ; setsockopt( srv_fd, SOL_SOCKET , SO_KEEPALIVE , &opt, sizeof(opt) ); // keepalive + timings
    opt = 5   ; setsockopt( srv_fd, IPPROTO_TCP, TCP_KEEPIDLE , &opt, sizeof(opt) ); // ---
    opt = 3   ; setsockopt( srv_fd, IPPROTO_TCP, TCP_KEEPINTVL, &opt, sizeof(opt) ); // ---
    opt = 8   ; setsockopt( srv_fd, IPPROTO_TCP, TCP_KEEPCNT  , &opt, sizeof(opt) ); // ---
    opt = 2048; setsockopt( srv_fd, SOL_SOCKET , SO_SNDBUF    , &opt, sizeof(opt) ); // sndbuf

    struct linger lng = { // Avoid RST on close
        .l_onoff  = 0,
        .l_linger = 0
    };

    setsockopt( srv_fd, SOL_SOCKET, SO_LINGER, &lng, sizeof(lng) );

    struct sockaddr_in addr;

    memset( &addr, 0, sizeof(addr) );

    addr.sin_family      = AF_INET;
    addr.sin_addr.s_addr = htonl(INADDR_ANY);
    addr.sin_port        = htons(2525);

    if( bind( srv_fd, (struct sockaddr*) &addr, sizeof(addr) ) < 0 ) {
        perror("bind");
        close(srv_fd);
        exit(1);
    }

    if( listen(srv_fd, 16) < 0 ) {
        perror("listen");
        close(srv_fd);
        exit(1);
    }

    // Accept loop with fork-per-connection (like socat ... fork)
    while(1) {

        const int net_fd = accept(srv_fd, NULL, NULL);

        if(net_fd < 0) {
            if(errno == EINTR) continue;
            perror("accept");
            break;
        }

        const pid_t child = fork();

        if(child < 0) {
            perror("fork");
            if( close(net_fd) < 0 ) perror("close");
            continue;
        }

        if(child == 0) {
            // Child - handle this client
            if( close(srv_fd) < 0 ) perror("close");

            memset( &sa, 0, sizeof(sa) );
            sa.sa_handler = SIG_DFL;
            sigemptyset(&sa.sa_mask);

            if( sigaction(SIGCHLD, &sa, NULL) < 0 ) {
                perror("sigaction");
                close(net_fd);
                exit(1);
            }

            if( fcntl(net_fd, F_SETFD, FD_CLOEXEC) < 0 ) { // Prevent leakage into execs if ever extended
                perror("fcntl");
                close(net_fd);
                exit(1);
            }

            run_session(net_fd);

            if( close(net_fd) < 0 ) perror("close");
            exit(0);
        }

        // Parent - close client socket and continue accepting
        if( close(net_fd) < 0 ) perror("close");
    }

    if( close(srv_fd) < 0 ) perror("close");

    return 0;
}
