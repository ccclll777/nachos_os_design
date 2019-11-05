#include "stdio.h"
#include "stdlib.h"
#include "syscall.h"
int main(int argc, char** argv)
{
   int fd = 0;
   char * filename = "aa.txt";
   int ByteNum;
   char buffersize = 37;
   char buf2[40];
    printf("调用read \n");
   fd = open(filename);
   printf("fd=%d" ,fd);
   ByteNum = read(fd,buf2,20);


   printf("调用read 成功！\n");

   printf("打印读出的文字");

   printf(buf2);
   close(fd);
    return 0;
}