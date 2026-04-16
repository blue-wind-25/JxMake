/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


#define _DEFAULT_SOURCE
#define _POSIX_C_SOURCE 200809L
#define _XOPEN_SOURCE   600


#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <poll.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <termios.h>
#include <unistd.h>


// ##### !!! TODO : This could hung the entire system if the serial device is disconnected !!! #####


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


static void make_flag_path(char* out, const size_t out_sz, const char* dev_name)
{
    /* Build a stable flag path from the device path
     *     Example:
     *         /dev/mycdc
     *     becomes:
     *         /tmp/serial_dev___dev_mycdc
     */

    size_t j = 0;
    const char* prefix = "/tmp/serial_dev__";

    memset(out, 0, out_sz);

    for(size_t i = 0; prefix[i] != '\0' && j + 1 < out_sz; ++i) out[j++] = prefix[i];

    for(size_t i = 0; dev_name[i] != '\0' && j + 1 < out_sz; ++i) {

        char c = dev_name[i];
        if(c == '/') c = '_';

        out[j++] = c;

    } // for

    out[j] = '\0';
}


static void make_flag_tmp_path(char* out, const size_t out_sz, const char* flag_path)
{
    const char* suffix = ".tmp"; // Temporary file used for atomic flag replacement

    memset(out, 0, out_sz);

    size_t j = 0;

    for(size_t i = 0; flag_path[i] != '\0' && j + 1 < out_sz; ++i) out[j++] = flag_path[i];
    for(size_t i = 0; suffix   [i] != '\0' && j + 1 < out_sz; ++i) out[j++] = suffix   [i];

    out[j] = '\0';
}


static void write_flag(const char* flag_path, const int is_up)
{
    // Best-effort atomic update of the flag file - readers never need locks; they will see either the old file or the new file

    char tmp_path[PATH_MAX];
    make_flag_tmp_path( tmp_path, sizeof(tmp_path), flag_path );

    const int fd = open(tmp_path, O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if(fd < 0) return;

    const char*  s  = is_up ? "1\n" : "0\n";
    const size_t sz = 2;

    ssize_t off = 0;

    while( off < (ssize_t) sz ) {

        const ssize_t wr = write(fd, s + off, sz - off);

        if(wr > 0) {
            off += wr;
            continue;
        }

        if(wr < 0 && errno == EINTR) continue;

        close(fd);
        unlink(tmp_path);
        return;

    } // while

    fsync(fd);
    close(fd);

    if( rename(tmp_path, flag_path) < 0 ) unlink(tmp_path);
}


static void update_flag(const char* flag_path, int* flag_is_up, const int is_up)
{
    if(*flag_is_up != is_up) {
        write_flag(flag_path, is_up);
        *flag_is_up = is_up;
    }
}


static int open_serial(const char* dev_name)
{
    // Open the CDC ACM device in nonblocking raw mode - 115200 is set explicitly to avoid ever selecting 1200 baud

    const int serial_fd = open(dev_name, O_RDWR | O_NOCTTY | O_NONBLOCK);
    if(serial_fd < 0) return -1;

    set_cloexec(serial_fd);

    struct termios t;

    if( tcgetattr(serial_fd, &t) < 0 ) {
        close(serial_fd);
        return -1;
    }

    cfmakeraw(&t);

    cfsetispeed(&t, B115200);
    cfsetospeed(&t, B115200);

    t.c_cflag |= CREAD | CLOCAL;
    t.c_cflag &= ~CRTSCTS;

    if( tcsetattr(serial_fd, TCSANOW, &t) < 0 ) {
        close(serial_fd);
        return -1;
    }

    return serial_fd;
}


static void close_serial(int* serial_fd, const char* flag_path, int* flag_is_up)
{
    if(*serial_fd >= 0) {
        close(*serial_fd);
        *serial_fd = -1;
    }

    update_flag(flag_path, flag_is_up, 0);
}


static void close_client(int* net_fd, ssize_t* net_to_ser_len, ssize_t* net_to_ser_off, ssize_t* ser_to_net_len, ssize_t* ser_to_net_off)
{
    if(*net_fd >= 0) {
        close(*net_fd);
        *net_fd = -1;
    }

    *net_to_ser_len = 0;
    *net_to_ser_off = 0;
    *ser_to_net_len = 0;
    *ser_to_net_off = 0;
}


static void drop_pending(ssize_t* len, ssize_t* off)
{
    *len = 0;
    *off = 0;
}


int main(int argc, char** argv)
{
    // Ignore SIGPIPE so the daemon does not die on failed writes
    signal(SIGPIPE, SIG_IGN);

    /*
    // Force stderr to null if the journal is wonky
    int fd = open("/dev/null", O_RDWR);
    dup2(fd, 2);
    close(fd);
    //*/

    if(argc != 3) {
        errno = EINVAL;
        perror("argc");
        exit(1);
    }

    const int   port     = atoi(argv[1]);
    const char* dev_name = argv[2];

    if(port <= 1023 || port > 65535) {
        errno = EINVAL;
        perror("port");
        exit(1);
    }

    char flag_path[PATH_MAX];

    make_flag_path(flag_path, sizeof(flag_path), dev_name);

    int flag_is_up = -1;
    update_flag(flag_path, &flag_is_up, 0);

    // Create listener socket
    const int srv_fd = socket(AF_INET, SOCK_STREAM, 0);

    if(srv_fd < 0) {
        perror("socket");
        exit(1);
    }

    set_cloexec(srv_fd);
    set_nonblock(srv_fd);

    // Mimics:
    //     usr/bin/socat tcp-listen:<port>,reuseaddr,fork,sndbuf=2048,keepalive,keepidle=5,keepintvl=3,keepcnt=8 exec:/mnt/data/pi/daemon/tcp_pty_bash_bridge.elf
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
    addr.sin_port        = htons(port);

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

    // One active TCP client at a time - extra clients are accepted and closed immediately
    int net_fd    = -1;
    int serial_fd = -1;

    // Small in-flight buffers - these preserve partial progress for the current chunk only (they are not a replay queue across reconnect)
    char net_to_ser[16 * 1024];
    char ser_to_net[16 * 1024];

    ssize_t net_to_ser_len = 0;
    ssize_t net_to_ser_off = 0;

    ssize_t ser_to_net_len = 0;
    ssize_t ser_to_net_off = 0;

    while(1) {

        // Keep trying to reconnect the serial device forever - the daemon must stay alive even if the device disappears
        if(serial_fd < 0) {
            serial_fd = open_serial(dev_name);

            if(serial_fd >= 0) update_flag(flag_path, &flag_is_up, 1);
            else               update_flag(flag_path, &flag_is_up, 0);
        }

        struct pollfd fds[3];

        short srv_events = POLLIN;
        short net_events = 0;
        short ser_events = 0;

        // Always watch the listening socket so extra clients can be rejected immediately
        if(net_fd >= 0) {
            net_events |= POLLIN;
            if(ser_to_net_len > ser_to_net_off) net_events |= POLLOUT;
        }

        // Watch the serial device if it is open
        if(serial_fd >= 0) {
            ser_events |= POLLIN;
            if(net_fd >= 0 && net_to_ser_len > net_to_ser_off) ser_events |= POLLOUT;
        }

        fds[0].fd      = srv_fd;
        fds[0].events  = srv_events;
        fds[0].revents = 0;

        fds[1].fd      = net_fd;
        fds[1].events  = net_events;
        fds[1].revents = 0;

        fds[2].fd      = serial_fd;
        fds[2].events  = ser_events;
        fds[2].revents = 0;

        // If the serial device is absent, wake periodically so we can retry opening it
        int timeout_ms = -1;

        if(serial_fd < 0) timeout_ms = 500;

        int pr;

        do {

            pr = poll(fds, 3, timeout_ms);

        } while(pr < 0 && errno == EINTR);

        if(pr < 0) {
            perror("poll");
            break;
        }

        if(pr == 0) continue;

        if(fds[0].revents & POLLIN) {
            // Accept all queued connection attempts - use the first one if there is no active client, otherwise close them immediately
            while(1) {

                const int new_fd = accept(srv_fd, NULL, NULL);

                if(new_fd < 0) {
                    if(errno == EINTR) continue;
                    if(errno == EAGAIN || errno == EWOULDBLOCK) break;
                    perror("accept");
                    close_client(&net_fd, &net_to_ser_len, &net_to_ser_off, &ser_to_net_len, &ser_to_net_off);
                    close_serial(&serial_fd, flag_path, &flag_is_up);
                    close(srv_fd);
                    exit(1);
                }

                set_cloexec(new_fd);
                set_nonblock(new_fd);

                if(net_fd < 0) net_fd = new_fd;
                else           close(new_fd);

            } // while
        }

        if( net_fd >= 0 && ( fds[1].revents & (POLLHUP | POLLERR | POLLNVAL) ) ) {
            close_client(&net_fd, &net_to_ser_len, &net_to_ser_off, &ser_to_net_len, &ser_to_net_off);
        }

        if( serial_fd >= 0 && ( fds[2].revents & (POLLHUP | POLLERR | POLLNVAL) ) ) {
            close_serial(&serial_fd, flag_path, &flag_is_up);
            drop_pending(&net_to_ser_len, &net_to_ser_off);
        }

        if( net_fd >= 0 && (fds[1].revents & POLLOUT) ) {
            // Continue a partial serial->network transfer
            if(ser_to_net_len > ser_to_net_off) {
                const ssize_t wr = write(net_fd, ser_to_net + ser_to_net_off, ser_to_net_len - ser_to_net_off);

                if(wr > 0) {
                    ser_to_net_off += wr;
                    if(ser_to_net_off >= ser_to_net_len) drop_pending(&ser_to_net_len, &ser_to_net_off);
                }
                else if(wr < 0) {
                    if(errno != EAGAIN && errno != EWOULDBLOCK && errno != EINTR) {
                        close_client(&net_fd, &net_to_ser_len, &net_to_ser_off, &ser_to_net_len, &ser_to_net_off);
                    }
                }
            }
        }

        if( serial_fd >= 0 && (fds[2].revents & POLLOUT) ) {
            // Continue a partial network->serial transfer
            if(net_to_ser_len > net_to_ser_off) {
                const ssize_t wr = write(serial_fd, net_to_ser + net_to_ser_off, net_to_ser_len - net_to_ser_off);

                if(wr > 0) {
                    net_to_ser_off += wr;
                    if(net_to_ser_off >= net_to_ser_len) drop_pending(&net_to_ser_len, &net_to_ser_off);
                }
                else if(wr < 0) {
                    if(errno != EAGAIN && errno != EWOULDBLOCK && errno != EINTR) {
                        // Do not replay old client data to a newly reconnected device
                        close_serial(&serial_fd, flag_path, &flag_is_up);
                        drop_pending(&net_to_ser_len, &net_to_ser_off);
                    }
                }
            }
        }

        if( net_fd >= 0 && (fds[1].revents & POLLIN) ) {
            // Read from the client only when the previous client->serial chunk is finished
            if(net_to_ser_len == net_to_ser_off) {
                drop_pending(&net_to_ser_len, &net_to_ser_off);

                const ssize_t n = read( net_fd, net_to_ser, sizeof(net_to_ser) );

                if(n > 0) {
                    if(serial_fd >= 0) net_to_ser_len = n;
                    else               drop_pending(&net_to_ser_len, &net_to_ser_off); // Serial is absent, so discard this client chunk
                }
                else if(n == 0) {
                    close_client(&net_fd, &net_to_ser_len, &net_to_ser_off, &ser_to_net_len, &ser_to_net_off);
                }
                else {
                    if(errno != EAGAIN && errno != EWOULDBLOCK && errno != EINTR) {
                        close_client(&net_fd, &net_to_ser_len, &net_to_ser_off, &ser_to_net_len, &ser_to_net_off);
                    }
                }
            }
        }

        if( serial_fd >= 0 && (fds[2].revents & POLLIN) ) {
            // Read from serial only when the previous serial->client chunk is finished - if there is no client, drain and discard serial input for robustness
            if(ser_to_net_len == ser_to_net_off) {
                drop_pending(&ser_to_net_len, &ser_to_net_off);

                const ssize_t n = read( serial_fd, ser_to_net, sizeof(ser_to_net) );

                if(n > 0) {
                    if(net_fd >= 0) ser_to_net_len = n;
                    else            drop_pending(&ser_to_net_len, &ser_to_net_off);
                }
                else if(n == 0) {
                    close_serial(&serial_fd, flag_path, &flag_is_up);
                    drop_pending(&net_to_ser_len, &net_to_ser_off);
                }
                else {
                    if(errno != EAGAIN && errno != EWOULDBLOCK && errno != EINTR) {
                        close_serial(&serial_fd, flag_path, &flag_is_up);
                        drop_pending(&net_to_ser_len, &net_to_ser_off);
                    }
                }
            }
        }

    } // while

    close_client(&net_fd, &net_to_ser_len, &net_to_ser_off, &ser_to_net_len, &ser_to_net_off);
    close_serial(&serial_fd, flag_path, &flag_is_up);
    close(srv_fd);

    return 0;
}
