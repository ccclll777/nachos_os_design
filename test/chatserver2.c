/* A simple server in the internet domain using TCP
 The port number is passed as an argument */

#include "syscall.h"
#include "stdio.h"

#define PORT 12
#define BUFFER_SIZE 80
struct Client {
    int fd;
    char buffer[BUFFER_SIZE];
    int currentIndex;
};

void addClient(struct Client* clients, int fd) {
    int index;
    do {
        if (clients[index].fd == -1) {
            clients[index].fd = fd;
            clients[index].currentIndex = 0;
        }
        index++;
    } while (index < 10);
}

int broadcastMessage(struct Client* clients, int sender, char* msg, int length) {
    int index;
    do {
        if (index != sender) {
            write(index, msg, length);
        }
        index++;
    } while (index < 10);
}

int main(int argc, char *argv[]) {
    struct Client clients[10];

    while(1) {
        int fileDescriptor = accept(PORT);

        if (fileDescriptor != -1) {
            addClient(clients, fileDescriptor);
        }
        int index;
        do {
           if (clients[index].fd == -1) {
              continue;
           }

           int charsRead = read(clients[index].fd,
                                clients[index].buffer + clients[index].currentIndex,
                                BUFFER_SIZE - clients[index].currentIndex);

           if (charsRead == 0) {
               continue;
           }

           if (charsRead == -1) {
               close(clients[index].fd);
               clients[index].fd = -1;
               continue;
           } else {
               clients[index].currentIndex += charsRead;
               if (clients[index].buffer[clients[index].currentIndex - 1] == '\n') {
                    broadcastMessage(clients,
                        index,
                        clients[index].buffer,
                        clients[index].currentIndex);
               }
           }
            index++;
        } while (index < 10);

        if (read(1, clients[0].buffer, 4))break;
    }
    return 0;
}

