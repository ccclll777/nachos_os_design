#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define MAX_TEXT_SIZE 1000
#define MAX_CLIENT_SOCKETS 16
#define BUFFERSIZE	64

#define MAXARGSIZE	16
#define MAXARGS		16
int clientSockets[MAX_CLIENT_SOCKETS], receivedEnd;
char receivedText[MAX_TEXT_SIZE];

void broadcastFromClient(int clientNum);
static int tokenizeCommand(char* command, int maxTokens, char *tokens[], char* storage) {
	const int quotingCharacter = 0x00000001;
	const int quotingString = 0x00000002;
	const int startedArg = 0x00000004;

	int state = 0;
	int numTokens = 0;

	char c;

	assert(maxTokens > 0);

	while ((c = *(command++)) != '\0') {
		if (state & quotingCharacter) {
			switch (c) {
				case 't':
					c = '\t';
					break;
				case 'n':
					c = '\n';
					break;
			}
			*(storage++) = c;
			state &= ~quotingCharacter;
		}
		else if (state & quotingString) {
			switch (c) {
				case '\\':
					state |= quotingCharacter;
					break;
				case '"':
					state &= ~quotingString;
					break;
				default:
					*(storage++) = c;
					break;
			}
		}
		else {
			switch (c) {
				case ' ':
				case '\t':
				case '\n':
					if (state & startedArg) {
						*(storage++) = '\0';
						state &= ~startedArg;
					}
					break;
				default:
					if (!(state & startedArg)) {
						if (numTokens == maxTokens) {
							return -1;
						}
						tokens[numTokens++] = storage;
						state |= startedArg;
					}

					switch (c) {
						case '\\':
							state |= quotingCharacter;
							break;
						case '"':
							state |= quotingString;
							break;
						default:
							*(storage++) = c;
							break;
					}
			}
		}
	}

	if (state & quotingCharacter) {
		printf("Unmatched \\.\n");
		return -1;
	}

	if (state & quotingString) {
		printf("Unmatched \".\n");
		return -1;
	}

	if (state & startedArg) {
		*(storage++) = '\0';
	}

	return numTokens;
}

int main(int argc, char* argv[]) {
	int newSocket = 0, i;
	char result[1];

	for (i = 0; i < MAX_CLIENT_SOCKETS; i++) { // init clientSockets
		clientSockets[i] = -1;
		printf("初始化\n", clientSockets[i]);
	}
    char buffer[BUFFERSIZE];
	while (1) {
        readline(buffer, BUFFERSIZE);
        char args[BUFFERSIZE];
        char *argv[MAXARGS];

        int argc = tokenizeCommand(buffer, MAXARGS, argv, args);
        if (argc <= 0)
            return;
        if(argc > 0)
        {
            printf("聊天服务器退n");
            break;
        }

		newSocket = accept(15); // Check for new socket
		if (newSocket != -1) { // If new socket, add to client server list
            printf("client %d connected\n", newSocket);
			clientSockets[newSocket] = newSocket;
		}
		for (i = 0; i < MAX_CLIENT_SOCKETS; i++) {
			if (clientSockets[i] != -1) {
				broadcastFromClient(i);
			}
		}
	}

	// We don't need to call close on our sockets. It is done explicitly upon kernel termination.
}

// broadcasts next line from a specified client to all the clients
void broadcastFromClient(int clientNum) {
	int i, bytesWrit, bytesRead;
	char result[1];
    
	// Check for char from client
	bytesRead = read(clientSockets[clientNum], result, 1);

	// If client disconnected, kill it
	if (bytesRead == -1) {
//        printf("disconnecting client %d\n", clientNum);
//        close(clientSockets[clientNum]);
//        clientSockets[clientNum] = -1;
        return;
    }
    // Abort if no text received
	if (bytesRead == 0 ) return;
	
	receivedEnd = 0;
	// Else get all chars from client until next '/n'
	while ((bytesRead > -1) && (receivedEnd < MAX_TEXT_SIZE)) {
		receivedText[receivedEnd++] = result[0];
		if (result[0] == '\n') break;
		bytesRead = read(clientSockets[clientNum], result, 1);
	}
	
    // Abort if no text received
	if (receivedEnd == 0) return;
	
    receivedText[receivedEnd] = '\0';
    printf("broadcast: %s",receivedText);
    
	// If there was any text received from the client, broadcast that line to each client
	for (i = 0; i < MAX_CLIENT_SOCKETS; ++i)
		if (i != clientNum && clientSockets[i] != -1) { // Do not broadcast to client it came from
			bytesWrit = write(clientSockets[i], receivedText, receivedEnd);

			// If did not work (bytesWrit != receivedEnd) disconnect client
			if (bytesWrit != receivedEnd) {
				printf("Unable to write to client %d. Disconnecting client.", i);
				close(clientSockets[i]);
				clientSockets[i] = -1;
            }
		}
}
