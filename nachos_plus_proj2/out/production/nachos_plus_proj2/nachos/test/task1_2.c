#include "stdio.h"
#include "stdlib.h"
#include "syscall.h"


int mystrlength(char *buffer)
{
int i;
for(i = 0 ; i<500 ; i++)
{
if(buffer[i] == 0)
{
return i;
}
}
return -1;
}

int main(){
int fd = 0;
char * filename = "aa.txt";
int ByteNum;
char * buffer = "Hello! This is the test for Task2.1\n";
char buffersize = mystrlength(buffer);
char buf[40];

creat(filename);

printf("调用creat");
printf("done!\n");


fd = open(filename);
printf("调用 open(filename); done！");
printf("return value  fd = ");

printf("\n");
write(fd,buffer,buffersize);
close(fd);

printf("调用write(fd,buffer,buffersize); done！");


fd = open(filename)
int i;
ByteNum = read(fd,buf,40);


printf("调用read(fd,buf,40); done！");

printf("打印读出的文字");

printf(buf);


close(fd);
halt();


}


