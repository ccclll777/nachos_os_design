package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;

import java.util.LinkedList;

/**
 * A kernel that can support multiple user processes.
 */
//多道程序设计的内核。
public class UserKernel extends ThreadedKernel {
	//空页
	private static LinkedList<Integer> freePages = new LinkedList<Integer>();
	public static Semaphore processIDSem;
	public static Semaphore freePagesSem;

	//保证访问内存互斥的锁
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    //初始化这个内核。创建同步控制台并设置处理器的异常处理程序。
    public void initialize(String[] args) {
    	//获取配置文件信息   创建线程 以及时钟
	super.initialize(args);

	//多个用户程序之间共享控制台
	console = new SynchConsole(Machine.console());

		processIDSem = new Semaphore(1);
		freePagesSem = new Semaphore(1);


		int numPhysPages = Machine.processor().getNumPhysPages();
		//初始化物理页  物理内存
		for (int i = 0; i < numPhysPages; ++i) {
			freePages.add(i);
		}

	Machine.processor().setExceptionHandler(new Runnable() {
		public void run() { exceptionHandler(); }
	    });
    }

    //给进程获取物理页
	public static int getFreePage() {
		int pageNumber = -1;
		boolean interruptStatus = Machine.interrupt().disable();
		freePagesSem.P();
		if (freePages.isEmpty() == false) {
			pageNumber = freePages.removeFirst();
		}
		freePagesSem.V();
		Machine.interrupt().restore(interruptStatus);
		return pageNumber;
	}

	//释放物理内存
	public static void addFreePage(int pageNumber) {
		boolean interruptStatus = Machine.interrupt().disable();
		freePagesSem.P();
		freePages.addFirst(pageNumber);
		freePagesSem.V();
		Machine.interrupt().restore(interruptStatus);
	}

    /**
     * Test the console device.
     */
    public void selfTest() {
	super.selfTest();
		System.out.println("测试调度程序");
	System.out.println("Testing the console device. Typed characters");
	System.out.println("will be echoed until q is typed.");

	char c;

	do {
	    c = (char) console.readByte(true);
	    console.writeByte(c);
	}
	while (c != 'q');

	System.out.println("");
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    //返回当前进程
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;

	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    //异常处理程序。每当用户指令导致处理器异常时，处理器都会调用此处理程序。

	//调用异常处理程序时，将启用中断，并且处理器的原因寄存器包含一个整数，用于标识异常的原因（请参阅<tt>processor</tt>类中的<tt>exceptionzzz</tt>常量）。
	// 如果异常涉及错误的虚拟地址（例如页错误、TLB未命中、只读、总线错误或地址错误），处理器的badvaddr寄存器将标识导致异常的虚拟地址。
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	UserProcess process = ((UThread) KThread.currentThread()).process;
	//返回异常原因寄存器中的内容
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	Machine#getShellProgramName
     */
    //*通过创建进程和运行shell来启动用户程序
	//*程序在里面。它必须运行的shell程序的名称由返回
	//*<tt>machine.getShellProgramName（）</tt>。
    public void run() {
	super.run();

	UserProcess process = UserProcess.newUserProcess();

	String shellProgram = Machine.getShellProgramName();
	Lib.assertTrue(process.execute(shellProgram, new String[] { }));

	KThread.currentThread().finish();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }

    /** Globally accessible reference to the synchronized console. */
    //对同步控制台的全局可访问引用
    public static SynchConsole console;

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
}




