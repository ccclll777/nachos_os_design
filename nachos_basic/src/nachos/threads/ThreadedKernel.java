package nachos.threads;

import nachos.machine.*;

/**
 *
 * 创建一个线程
 * //到创建内核的时候，引导过程是相当容易遵循的——NACHOS只是制作Java对象，与任何其他Java程序一样。与任何其他单线程Java程序一样，
 *     // NACHOS代码也在Java创建的初始Java线程上执行。threadedkernel.initialize（）的任务是启动线程：
 *     发生特殊情况的第一个线索应该是，创建的新kthread对象没有存储在initialize（）中的变量中。kthread的构造函数在第一次调用时遵循以下过程：
 *     （1）创建就绪队列（threadedkernel.scheduler.newthreadqueue（））。
 *     （2）将cpu分配给正在创建的新kthread对象（readyqueue.acquire（this））
 *     （3）将kthread.currentthread设置为正在生成的新kthread。
 *     （4）将新kthread的tcb对象设置为tcb.currenttcb（）。这样做时，当前运行的Java线程被分配给正在创建的新的kthread对象。
 *     （5）将新kthread的状态从默认（status new）更改为statusrunning。这将绕过statusready状态
 *     （6）创建空闲线程：
 *     创建另一个新的kthread，目标设置为无限的yield（）循环
 *     从主线程fork一个空闲线程
 *
 *
 *     在这个过程之后，有两个kthread对象，每个对象都有一个tcb对象（一个用于主线程，一个用于空闲线程）。主线程并不特殊-调度程序将其与其他任何kthread一模一样。
 *     主线程可以创建其他线程，它可以die，它可以block。nachos会话在所有kthread完成之前不会结束，不管主线程是否活动。
 *
 *     //在大多数情况下，空闲线程也是一个普通线程，它可以像任何其他线程一样进行上下文切换。唯一的区别是它永远不会添加到就绪队列（kthread.ready（）对空闲线程进行显式检查）。
 *     相反，如果readyqueue.nextThread（）返回null，则线程系统将切换到空闲线程。
 *
 *     注意：当nachos空闲线程永远只做yield（）时，一些系统使用空闲线程来工作。一个常见的用途是将内存置0以准备重新分配。
 *
 *Creating More Threads
 *     创建后续线程要简单得多。如kthread javadoc中所述，将创建一个新的kthread，向构造函数传递一个runnable对象。然后，fork（）被调用：
 *      KThread newThread = new KThread(myRunnable);
 *          ...
 *          newThread.fork();
 *     此序列导致新线程被放置在就绪队列中。但是，当前运行的线程不会立即yield（）
 *
 *
 *     NACHOS源是相对干净的，只使用基本的Java，除了使用匿名类来复制C++中函数指针的功能外。
 *     下面的代码演示如何使用匿名类创建新的kthread对象，该对象在fork（）时将执行enclosing对象的myfunction（）方法。
 *
 *
       Runnable myRunnable = new Runnable() {
      public void run() {
    myFunction();
   }
      };
     KThread newThread = new KThread(myRunnable);


 *
 *
 * 这段代码在封闭对象的上下文中创建一个runnable类型的新对象
 * 。由于MyRunnFielt没有方法MyFrAuthor（），执行MyRunnaby.Run（）
 * 会导致Java在封闭类中查找MyType（）方法。
 *
 *
 *
 *
 * On Thread Death
 *
 *
 * 所有线程都有一些分配给它们的资源，这些资源是线程运行所必需的（例如tcb对象）
 * 由于线程本身在运行时无法释放这些资源，因此它会留下一个virtua，要求下一个运行的线程释放其资源。
 *
 *
 * 这是在kthread.finish（）中实现的，它将kthread.tobedestroyed设置为当前正在运行的线程。
 * 然后将当前线程的status字段设置为statusFinished并调用sleep（）。
 * 由于线程没有等待threadqueue对象，因此它的sleep将是永久的（也就是说，nachos永远不会试图唤醒线程）
 * 。然而，这个方案确实要求在每次上下文切换之后，新运行的线程必须检查tobedestroyed。
 *
 *
 * 注意：在C++版本的NACHOS中，线程死亡是由显式内存分配所需的复杂的，结合了仍然指向死后线程的悬空引用（例如，大多数线程联接（）实现需要对线程的一些引用）。
 * 在Java中，垃圾收集器负责注意这些引用何时被分离，大大简化了线程整理过程。
 */

/**
 * A multi-threaded OS kernel.
 */

//多线程操作系统内核
    //nachos提供了一个内核线程包，允许多个任务同时运行（请参见nachos.threads.threadedkernel和nachos.threads.kthread）。
// 一旦用户进程被实现（阶段2），一些线程可能正在运行mips处理器模拟。
// 由于调度器和线程包有关，运行MIPS仿真的线程和一个正在运行的内核Java代码之间没有差别。
public class ThreadedKernel extends Kernel {
    /**
     * Allocate a new multi-threaded kernel.
     */
    //构造函数
    public ThreadedKernel() {
	super();
    }

    /**
     * Initialize this kernel. Creates a scheduler, the first thread, and an
     * alarm, and enables interrupts. Creates a file system if necessary.   
     */
    //初始化这个内核。创建调度程序、第一个线程和时钟，并启用中断。
    // 必要时创建文件系统。

    public void initialize(String[] args) {
	// set scheduler
        //获取nachos.conf中的信息
	String schedulerName = Config.getString("ThreadedKernel.scheduler");

	//根据某个名称 获取到某个 调度程序
	scheduler = (Scheduler) Lib.constructObject(schedulerName);

	// set fileSystem
	String fileSystemName = Config.getString("ThreadedKernel.fileSystem");
	if (fileSystemName != null)
	    fileSystem = (FileSystem) Lib.constructObject(fileSystemName);
	else if (Machine.stubFileSystem() != null)
	    fileSystem = Machine.stubFileSystem();
	else
	    fileSystem = null;

	// start threading
	new KThread(null);

	//启动一个时钟类
	alarm  = new Alarm();

	//中断
	Machine.interrupt().enable();
    }

    /**
     * Test this kernel. Test the <tt>KThread</tt>, <tt>Semaphore</tt>,
     * <tt>SynchList</tt>, and <tt>ElevatorBank</tt> classes. Note that the
     * autograder never calls this method, so it is safe to put additional
     * tests here.
     */
    //测试这个内核。测试<tt>KThread</tt>, <tt>Semaphore</tt>,
    //     * <tt>SynchList</tt>类。请注意
    //autograder从不调用此方法，因此可以安全地添加
    //在这里测试。
    public void selfTest() {
	KThread.selfTest();
	Semaphore.selfTest();
	SynchList.selfTest();
	if (Machine.bank() != null) {
	    ElevatorBank.selfTest();
	}
    }
    
    /**
     * A threaded kernel does not run user programs, so this method does
     * nothing.
     */
    //线程内核不允许用户程序
    public void run() {
    }

    /**
     * Terminate this kernel. Never returns.
     */
    //内核终止
    public void terminate() {
	Machine.halt();
    }

    /** Globally accessible reference to the scheduler. */
    //对调度程序的全局可访问引用
    public static Scheduler scheduler = null;
    /** Globally accessible reference to the alarm. */
    //对时钟的全局访问
    public static Alarm alarm = null;
    /** Globally accessible reference to the file system. */
    //对文件系统的全局访问
    public static FileSystem fileSystem = null;

    // dummy variables to make javac smarter
    private static RoundRobinScheduler dummy1 = null;
    private static PriorityScheduler dummy2 = null;
    private static LotteryScheduler dummy3 = null;
    private static Condition2 dummy4 = null;
    private static Communicator dummy5 = null;
    private static Rider dummy6 = null;
    private static ElevatorController dummy7 = null;
}
