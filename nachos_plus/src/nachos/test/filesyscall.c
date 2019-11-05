#include "stdio.h"
#include "stdlib.h"
#include "syscall.h"



int flag = true;
int fd;
int ret;

int main(){

/* Test 0 */

printf("测试文件创建调用 create\n");
flag = true;
fd = creat("0.txt");
if (fd == -1) flag = false;
if (flag) printf("create文件成功\n\n");
else printf("Test 0 failed\n\n");

/* Test 1 */
printf("测试调用 unlink \n");
flag = true;
fd = creat("1.txt");

if(fd != -1)
{
printf("创建文件1.TXT成功 \n");
}
if (fd == -1) flag = false;
close(fd);
ret = unlink("1.txt");

if (ret == -1) flag = false;
if(ret != -1)
{
printf("文件1.TXT  unlink成功 \n");
}
ret = unlink("1.txt");

if(ret != -1)
{
printf("文件1.TXT  不能多次unlink \n");
}
if (ret != -1) flag = false;
if (flag) printf("测试调用 unlink 成功\n\n");
else printf("Test 1 failed\n\n");

/* Test 2 */

printf("测试文件的 创建 打开 关闭\n");
flag = true;
fd = creat("2.txt");

if (fd == -1) flag = false;

if(fd != -1)
{
printf("2.TXT创建成功\n");
}
ret = open("2.txt");

if(ret != -1)
{
printf("2.TXT打开成功\n");
}
if (ret == -1) flag = false;
ret = unlink("2.txt");

if(ret != -1)
{
printf("2.TXT关闭成功\n");
}
if (ret == -1) flag = false;
close(fd);
if (flag) printf("测试文件的 创建 打开 关闭——成功\n\n");
else printf("Test 2 failed\n\n");

/* Test 3 */

printf("Test 3 started\n");
printf("由于只能有16的 文件描述符 0-15  测试 如果打开文件较多 是否可以提示打开失败 返回-1 \n");
flag = true;
int fds[14];
int i = 0;
fd = creat("3.txt");
printf("fd: %d\n", fd);
for (i = 0; i < 15; i++)
{
	fds[i] = open("3.txt");
	if (fds[i] == -1) flag = false;
	printf("flag: %d, i: %d, fd: %d\n", flag, i, fds[i]);
}
printf("flag: %d\n", flag);
fds[13] = open("3.txt");
printf("%d", fds[13]);
if(fds[13] != -1) flag = false;
for (i = 0; i < 14; i++) close(fds[i]);
if (flag) printf("测试完成\n\n");
else printf("Test 3 failed\n\n");

int fd = 0;
char * filename = "aa.txt";
int ByteNum;
char * buffer = "Hello! This is the test for Task2.1\n";
char buffersize = 37;
char buf2[40];

printf("调用creat\n");
creat(filename);


printf("creat aa.txt 成功\n");


printf("调用 open(filename); \n");
fd = open(filename);

printf("调用 open(filename) 成功 ");

printf("\n");
printf("调用write(fd,buffer,buffersize) \n");
write(fd,buffer,buffersize);
close(fd);

printf("调用write(fd,buffer,buffersize); 成功\n");


printf("调用read(fd,buf2,40); \n");
fd = open(filename);
ByteNum = read(fd,buf2,40);


printf("调用read(fd,buf,40); 成功！\n");

printf("打印读出的文字");

printf(buf2);


close(fd);
halt();
}
