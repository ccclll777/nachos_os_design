package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

/**
 * A UThread is KThread that can execute user program code inside a user
 * process, in addition to Nachos kernel code.
 */
//能够执行用户mips代码的线程。
public class UThread extends KThread {
    /**
     * Allocate a new UThread.
     */
    public UThread(UserProcess process) {
	super();

	setTarget(new Runnable() {
		public void run() {
		    runProgram();
		}
	    });

	this.process = process;
    }

    private void runProgram() {
	process.initRegisters();
	process.restoreState();

	Machine.processor().run();
	
	Lib.assertNotReached();
    }
    
    /**
     * Save state before giving up the processor to another thread.
     */
    //在将处理器放弃给另一个线程之前保存状态。
    protected void saveState() {
	process.saveState();

	for (int i=0; i<Processor.numUserRegisters; i++)
	    userRegisters[i] = Machine.processor().readRegister(i);

	super.saveState();
    }

    /**
     * Restore state before receiving the processor again.
     */
    //在再次接收处理器之前还原状态。
    protected void restoreState() {
	super.restoreState();
	
	for (int i=0; i<Processor.numUserRegisters; i++)
	    Machine.processor().writeRegister(i, userRegisters[i]);
	
	process.restoreState();
    }

    /**
     * Storage for the user register set.
     *
     * <p>
     * A thread capable of running user code actually has <i>two</i> sets of
     * CPU registers: one for its state while executing user code, and one for
     * its state while executing kernel code. While this thread is not running,
     * its user state is stored here.
     */
    //一个能够运行用户代码的线程实际上有两组cpu寄存器：
	// 一组用于执行用户代码时的状态，另一组用于执行内核代码时的状态。
	// 此线程未运行时，其用户状态存储在此处。
    //存储用户寄存器的集合
    public int userRegisters[] = new int[Processor.numUserRegisters];

    /**
     * The process to which this thread belongs.
     */
    //此线程所属的进程
    public UserProcess process;
}
