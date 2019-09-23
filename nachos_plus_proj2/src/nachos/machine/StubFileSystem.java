// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

import nachos.security.Privilege;
import nachos.threads.ThreadedKernel;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * This class implements a file system that redirects all requests to the host
 * operating system's file system.
 */

//指定计算机是否应提供存根文件系统。
//存根文件系统只提供对测试目录的直接访问。
//因为我们不做文件系统项目，所以他的值常常是true
public class StubFileSystem implements FileSystem {
    /**
     * Allocate a new stub file system.
     *
     * @param	privilege      	encapsulates privileged access to the Nachos
     *				machine.
     * @param	directory	the root directory of the stub file system.
     */
    //存根文件系统的根目录。
    public StubFileSystem(Privilege privilege, File directory) {
	this.privilege = privilege;
	this.directory = directory;
    }
    //打开文件  如果不存在 切第二个参数为true  则会创建
    public OpenFile open(String name, boolean truncate) {
	if (!checkName(name))
	    return null;
	
	delay();
	    
	try {
	    return new StubOpenFile(name, truncate);
	}
	catch (IOException e) {
	    return null;
	}
    }

    //移除文件  检查文件合法性后进行操作
    public boolean remove(String name) {
	if (!checkName(name))
	    return false;

	delay();
		//创建一个线程 完成文件的移除？？
	FileRemover fr = new FileRemover(new File(directory, name));
	//可以使用特权指令 执行此操作
	privilege.doPrivileged(fr);
	return fr.successful;
    }

    //文件移除 线程  执行文件的移除操作
    private class FileRemover implements Runnable {
	public FileRemover(File f) {
	    this.f = f;
	}
	
	public void run() {
	    successful = f.delete();
	}
	
	public boolean successful = false;
	private File f;
    }

    //wait 1000个tick？？ 延迟操作 可能是为了协调系统进程的运行？？
    private void delay() {
	long time = Machine.timer().getTime();
	int amount = 1000;
	ThreadedKernel.alarm.waitUntil(amount);
	Lib.assertTrue(Machine.timer().getTime() >= time+amount);
    }

    //打开文件的对象  在内部建立一个file对象  然后通过getRandomAccess打开文件
    private class StubOpenFile extends OpenFileWithPosition {
	StubOpenFile(final String name, final boolean truncate)
	    throws IOException {
	    super(StubFileSystem.this, name);

	    final File f = new File(directory, name);

	    if (openCount == maxOpenFiles)
		throw new IOException();

	    privilege.doPrivileged(new Runnable() {
		public void run() { getRandomAccessFile(f, truncate); }
	    });

	    if (file == null)
		throw new IOException();

	    open = true;
	    openCount++;
	}

	//在可以访问指定文件的情况下（文件不存在 可以创建 或文件已经存在的情况） 然后调用java的RandomAccessFile函数获得文件流
	private void getRandomAccessFile(File f, boolean truncate) {
	    try {
		if (!truncate && !f.exists())
		    return;

		//可以访问文件的任意地方同时支持文件的读和写，并且它支持随机访问
		file = new RandomAccessFile(f, "rw");

		if (truncate)
		    file.setLength(0);
	    }
	    catch (IOException e) {
	    }
	}

	public int read(int pos, byte[] buf, int offset, int length) {
	    if (!open)
		return -1;
	    
	    try {
		delay();

		//通过java的RandomAccess进行寻找
		file.seek(pos);
		return Math.max(0, file.read(buf, offset, length));
	    }
	    catch (IOException e) {
		return -1;
	    }
	}
	
	public int write(int pos, byte[] buf, int offset, int length) {
	    if (!open)
		return -1;
	    
	    try {
		delay();

			//通过java的RandomAccess进行寻找，然后读写操作
		file.seek(pos);
		file.write(buf, offset, length);
		return length;
	    }
	    catch (IOException e) {
		return -1;
	    }
	}

	public int length() {
	    try {
		return (int) file.length();
	    }
	    catch (IOException e) {
		return -1;
	    }
	}

	public void close() {
	    if (open) {
		open = false;
		openCount--;
	    }
		
	    try {
		file.close();
	    }
	    catch (IOException e) {
	    }
	}

	//可以访问文件的任意地方同时支持文件的读和写，并且它支持随机访问
	private RandomAccessFile file = null;
	private boolean open = false;
    }

    private int openCount = 0;
    private static final int maxOpenFiles = 16;
    
    private Privilege privilege;
    private File directory;

    //检查文件名的合法性
    private static boolean checkName(String name) {
	char[] chars = name.toCharArray();

	for (int i=0; i<chars.length; i++) {
	    if (chars[i] < 0 || chars[i] >= allowedFileNameCharacters.length)
		return false;
	    if (!allowedFileNameCharacters[(int) chars[i]])
		return false;
	}
	return true;
    }

    private static boolean[] allowedFileNameCharacters = new boolean[0x80];

    private static void reject(char c) {
	allowedFileNameCharacters[c] = false;
    }

    private static void allow(char c) {
	allowedFileNameCharacters[c] = true;
    }

    private static void reject(char first, char last) {
	for (char c=first; c<=last; c++)
	    allowedFileNameCharacters[c] = false;
    }

    private static void allow(char first, char last) {
	for (char c=first; c<=last; c++)
	    allowedFileNameCharacters[c] = true;
    }

    static {
	reject((char) 0x00, (char) 0x7F);
	
	allow('A', 'Z');
	allow('a', 'z');
	allow('0', '9');
	
	allow('-');
	allow('_');
	allow('.');
	allow(',');
    }
}
