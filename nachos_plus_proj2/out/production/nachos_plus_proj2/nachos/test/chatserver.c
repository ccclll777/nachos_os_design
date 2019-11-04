#include "syscall.h"
#include "stdio.h"

#define MAX_TEXT_SIZE 1000
#define MAX_CLIENT_SOCKETS 16

int clientSockets[MAX_CLIENT_SOCKETS], receivedEnd;
char receivedText[MAX_TEXT_SIZE];

void broadcastFromClient(int clientNum);

int main(int argc, char* argv[]) {
    int newSocket = 0, i;
    char result[1];

    for (i = 0; i < MAX_CLIENT_SOCKETS; i++) {
        clientSockets[i] = -1;
    }


    while (1) {
        if (read(stdin, result, 1) != 0) {
            break;
        }
        newSocket = accept(15);
        if (newSocket != -1) {
            clientSockets[newSocket] = newSocket;
            printf("client %d connected\n", newSocket);

        }
        for (i = 0; i < MAX_CLIENT_SOCKETS; i++) {
            if (clientSockets[i] != -1) {
                broadcastFromClient(i);
            }
        }
    }

}

void broadcastFromClient(int clientNum) {
    int i, bytesWrit, bytesRead;
    char result[5];
    bytesRead = read(clientSockets[clientNum], result, 5);
    if (bytesRead == -1) {

        return;
    }
    if (bytesRead == 0) return;
    for (i = 0; i < MAX_CLIENT_SOCKETS; ++i)
        if (clientSockets[i] != -1 ) {
            bytesWrit = write(clientSockets[i], result, 5);
            if(bytesWrit<0)
            {
                printf("发送失败\n");}

        }
}
