#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define MAX_TEXT_SIZE 1000
#define false 0
#define true 1

char receivedText[MAX_TEXT_SIZE], sendText[MAX_TEXT_SIZE];
int  receivedEnd, sendEnd, host, socket, bytesRead, bytesWrit;

int main(int argc, char* argv[]) {
    int host, socket, bytesRead, bytesWrit, done = false;
    char lastByte;
    receivedEnd = 0;

    if (argc != 2) {
        printf("error: please supply host address\n");
        return 1;
    }

    host = atoi(argv[1]);
    socket = connect(host, 15);

    printf("Successfully connected to host %d\n", host);
    while(!done) {
        sendEnd = 0;


        if ((bytesRead = read(stdin, sendText, 1)) == 1) {

            lastByte = sendText[0];
            sendEnd++;
            while (lastByte != '\n') {
                if ((bytesRead = read(stdin, sendText + sendEnd, 1)) == -1) {
                    printf("Error : Can't read from stdin. Bye!\n");
                    done = true;
                    break;
                } else {

                    sendEnd += bytesRead;
                    lastByte = sendText[sendEnd - 1];


                    if (sendEnd == MAX_TEXT_SIZE - 1) {
                        sendText[MAX_TEXT_SIZE - 1] = '\n';
                        break;
                    }
                }
            }


            if (sendText[0] == '.' && sendText[1] == '\n') {
                printf("Received exit command. Bye!\n");
                break;
            } else if(sendText[0] != '\n') {
                bytesWrit = write(socket, sendText, sendEnd);


            }
        }


        bytesRead = -1;
        strcpy(receivedText,"");
        bytesRead = read(socket, receivedText, 5);

        if (bytesRead > 0) {
            printf("客户端说：%s\n",receivedText);
        }

    }

    close(socket);

    return 0;//Success
}
