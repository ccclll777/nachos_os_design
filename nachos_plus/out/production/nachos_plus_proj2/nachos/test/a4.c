#include "stdio.h"
#include "stdlib.h"
#include "syscall.h"
int main(int argc, char** argv)
{
     int fd = 0;
       char * filename = "04.txt";
       int ByteNum;
       char buffersize = 20;
       char buf2[20];
        printf("调用read(fd,buf2,40); \n");
       fd = open(filename);
        printf("fd=%d" ,fd);
       ByteNum = read(fd,buf2,20);


       printf("调用read(fd,buf,40); 成功！\n");

       printf("打印读出的文字");

       printf(buf2);
       close(fd);
    return 0;
}