/* chat.c */

#include "syscall.h"
#include "stdio.h"

#define PORT 12
#define ADDRESS 0
#define BUFFER_SIZE 80

int main(int argc, char *argv[]) {
    bool isRunning = true;
    
    char chatMessage[BUFFER_SIZE];
    char messageBuffer[BUFFER_SIZE];

    int chatMessage_index = 0;
    int msg_index = 0;
    
    int fd = connect(ADDRESS, PORT);
    
    while(true) 
    {
        
        int charsRead = read(1, messageBuffer + msg_index, BUFFER_SIZE);
        if (charsRead != -1) 
        {
            if (charsRead == 0)
                continue;
            
            msg_index += charsRead;
            
            if (msg_index == 2) 
            {
                if (messageBuffer[0] == '.' && messageBuffer[1] == '\n') 
                {
                    return 0;
                }
            }
            
            if (messageBuffer[msg_index-1] == '\n') 
            {
                write(fd, messageBuffer, msg_index);
                msg_index = 0;
            }
            
        } 
        else
        {
            return 0;
        }
        
        charsRead = read(fd, chatMessage + chatMessage_index, BUFFER_SIZE);
        if (charsRead != -1) 
        {
            if (charsRead == 0)
            {
                continue;
            }
            
            chatMessage_index += charsRead;
            
            if (chatMessage_index == 2) 
            {
                if (chatMessage[0] == '.' && chatMessage[1] == '\n') 
                {
                    return 0;
                }
            }
            
            if (messageBuffer[msg_index-1] == '\n') 
            {
                write(1, chatMessage, chatMessage_index);
                chatMessage_index = 0;
            }
            
        }
        else
        {
            return 0;
        }
        
    }
    
    return 0;
}
