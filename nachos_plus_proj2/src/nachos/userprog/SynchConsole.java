package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

/**
 * Provides a simple, synchronized interface to the machine's console. The
 * interface can also be accessed through <tt>OpenFile</tt> objects.
 */
//一个同步的控制台；使得在多个线程之间共享机器的串行控制台成为可能。
public class SynchConsole {
    /**
     * Allocate a new <tt>SynchConsole</tt>.
     *
     * @param	console	the underlying serial console to use.
     */
    public SynchConsole(SerialConsole console) {
	this.console = console;

	//开两个线程  接受 发送中断
	Runnable receiveHandler = new Runnable() {
	    public void run() { receiveInterrupt(); }
	};
	Runnable sendHandler = new Runnable() {
	    public void run() { sendInterrupt(); }
	};
	console.setInterruptHandlers(receiveHandler, sendHandler);
    }

    /**
	 *
     * Return the next unsigned byte received (in the range <tt>0</tt> through
     * <tt>255</tt>). If a byte has not arrived at, blocks until a byte
     * arrives, or returns immediately, depending on the value of <i>block</i>.
     *
     * @param	block	<tt>true</tt> if <tt>readByte()</tt> should wait for a
     *			byte if none is available.
     * @return	the next byte read, or -1 if <tt>block</tt> was <tt>false</tt>
     *		and no byte was available.
     */
    //true为 读取是 如果没有则会等待
    public int readByte(boolean block) {
	int value;
	boolean intStatus = Machine.interrupt().disable();	
	readLock.acquire();

	if (block || charAvailable) {
	    charAvailable = false;
	    //用信号量控制
	    readWait.P();

	    value = console.readByte();
	    Lib.assertTrue(value != -1);
	}
	else {
	    value = -1;
	}

	readLock.release();
	Machine.interrupt().restore(intStatus);
	return value;
    }

    /**
     * Return an <tt>OpenFile</tt> that can be used to read this as a file.
     *
     * @return	a file that can read this console.
     */
    //打开文件读取
    public OpenFile openForReading() {
	return new File(true, false);
    }

    //读取完成？？发送信号量
    private void receiveInterrupt() {
	charAvailable = true;
	readWait.V();
    }

    /**
     * Send a byte. Blocks until the send is complete.
     *
     * @param	value	the byte to be sent (the upper 24 bits are ignored).
     */
    //发送一个字节。直到发送完成。
    public void writeByte(int value) {
	writeLock.acquire();
	console.writeByte(value);
	writeWait.P();
	writeLock.release();
    }

    /**
     * Return an <tt>OpenFile</tt> that can be used to write this as a file.
     *
     * @return	a file that can write this console.
     */
    public OpenFile openForWriting() {
	return new File(false, true);
    }

    //写入完成？？
    private void sendInterrupt() {
	writeWait.V();
    }

    private boolean charAvailable = false;

    private SerialConsole console;
    //读写锁
    private Lock readLock = new Lock();
    private Lock writeLock = new Lock();
    //有关读写的信号量
    private Semaphore readWait = new Semaphore(0);
    private Semaphore writeWait = new Semaphore(0);

    private class File extends OpenFile {
	File(boolean canRead, boolean canWrite) {
	    super(null, "SynchConsole");
	    
	    this.canRead = canRead;
	    this.canWrite = canWrite;
	}
	
	public void close() {
	    canRead = canWrite = false;
	}

	public int read(byte[] buf, int offset, int length) {
	    if (!canRead)
		return 0;

	    int i;
	    for (i=0; i<length; i++) {
		int value = SynchConsole.this.readByte(false);
		if (value == -1)
		    break;
		
		buf[offset+i] = (byte) value;
	    }

	    return i;
	}

	public int write(byte[] buf, int offset, int length) {
	    if (!canWrite)
		return 0;
	    
	    for (int i=0; i<length; i++)
		SynchConsole.this.writeByte(buf[offset+i]);
	    
	    return length;
	}

	private boolean canRead, canWrite;
    }
}
