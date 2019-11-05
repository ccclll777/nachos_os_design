#include "stdio.h"
#include "stdlib.h"
#include "syscall.h"
int main(int argc, char** argv)
{
  int fd = 0;
  char * filename = "04.txt";
  int ByteNum;
  char * buffer = "Hello  test  task2.3\n";
  char buffersize = 20;
  char buf2[20];

  printf("调用creat\n");
  creat(filename);
  printf("creat aa.txt 成功\n");
   printf("调用 open(filename); \n");
  fd = open(filename);
 printf("fd=%d" ,fd);
  printf("调用 open(filename) 成功 ");

  printf("\n");
  printf("调用write(fd,buffer,buffersize) \n");
  write(fd,buffer,buffersize);
  close(fd);

  printf("调用write(fd,buffer,buffersize); 成功\n");
   printf("写入：Hello  test  task2.3");
    return 0;
}