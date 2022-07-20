#include "py_connection_pool_udp_UDPEchoServer.h"
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <signal.h>
#include <pthread.h>
#include <sys/time.h>

#define TYPE32 int
#define TYPE64 long

int pause_socket = 0;

#define ERR_EXIT(m) \
        do \
        { \
                perror(m); \
                exit(EXIT_FAILURE); \
        } while(0)


void *echo_srv(void *sock) {
    int sock_int = *(int*)(sock);
    char recvbuf[1024] = {0};
    struct sockaddr_in peeraddr;
    socklen_t peerlen;
    int n;

    while (1)
    {
        peerlen = sizeof(peeraddr);
        memset(recvbuf, 0, sizeof(recvbuf));
        n = recvfrom(sock_int, recvbuf, sizeof(recvbuf), 0, (struct sockaddr*)&peeraddr, &peerlen);
        if (n == -1){
            if (errno == EINTR){
                continue;
            }else{
                free(sock);
                pthread_exit(NULL);
            }
        }else if (n > 0){
            if(pause_socket == 0){
              sendto(sock_int, recvbuf, n, 0, (struct sockaddr*)&peeraddr, peerlen);
            }
        }
    }
}


JNIEXPORT jint JNICALL Java_py_connection_pool_udp_UDPEchoServer_startEchoServer
(JNIEnv *env, jobject obj, jint port) {
    int err;
    jboolean flag = 1;
    pthread_t ntid;

    int *sock = (int*)malloc(sizeof(int));
    if(sock == NULL){
        return 0;
    }
    if ((*sock = socket(PF_INET, SOCK_DGRAM, 0)) < 0) {
        free(sock);
        return 0;
    }

    struct sockaddr_in servaddr;
    memset(&servaddr, 0, sizeof(servaddr));
    servaddr.sin_family = AF_INET;
    servaddr.sin_port = htons(port);
    servaddr.sin_addr.s_addr = htonl(INADDR_ANY);

    if (bind(*sock, (struct sockaddr*)&servaddr, sizeof(servaddr)) < 0) {
        free(sock);
        return 0;
    }

    err = pthread_create(&ntid, NULL, echo_srv, sock);
    if (err != 0) {
         free(sock);
         printf("can't create thread: %s\n", strerror(err));
         return 0;
    }

    return *sock;
}


JNIEXPORT jint JNICALL Java_py_connection_pool_udp_UDPEchoServer_stopEchoServer
(JNIEnv *env, jobject obj, jint pid){
     return close(pid);
}

JNIEXPORT void JNICALL Java_py_connection_pool_udp_UDPEchoServer_pauseEchoServer
(JNIEnv *env, jobject obj){
    pause_socket = 1;
}

JNIEXPORT void JNICALL Java_py_connection_pool_udp_UDPEchoServer_reviveEchoServer
(JNIEnv *env, jobject obj){
    pause_socket = 0;
}
