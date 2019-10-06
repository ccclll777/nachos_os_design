package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.TCB;

/**
 * A KThread is a thread that can be used to execute Nachos kernel code. Nachos
 * allows multiple threads to run concurrently.
 *
 * To create a new thread of execution, first declare a class that implements
 * the <tt>Runnable</tt> interface. That class then implements the <tt>run</tt>
 * method. An instance of the class can then be allocated, passed as an
 * argument when creating <tt>KThread</tt>, and forked. For example, a thread
 * that computes pi could be written as follows:
 *
 * <p><blockquote><pre>
 * class PiRun implements Runnable {
 *     public void run() {
 *         // compute pi
 *         ...
 *     }
 * }
 * </pre></blockquote>
 * <p>The following code would then create a thread and start it running:
 *
 * <p><blockquote><pre>
 * PiRun p = new PiRun();
 * new KThread(p).fork();
 * </pre></blockquote>
 */

//所有的线程都是KThread的实例 或子类
	//线程有状态  New, Ready, Running, Blocked, Finished

	//每一个 KThread 线程都有一个tcb   内部由java的线程实现

	//所有nachos线程都是nachos.threads.kthread的实例（能够运行用户级mips代码的线程是kthread，nachos.userprog.uthread的一个子类）。
// 每个kthread都包含一个nachos.machine.tcb对象，它为上下文切换、线程创建、线程销毁和线程生成提供低级支持。

/**
 * 在内部，NACHOS使用Java线程为每个TCB实现线程。Java线程由TCB同步，这样就可以在任何给定的时间运行一个Java线程
 * 。这提供了一种错觉，即上下文切换当前线程的保存状态并加载新线程的保存状态。
 * 然而，这个细节对于调试器（它将显示多个Java线程）是非常重要的，因为行为等同于实际处理器上的上下文切换。
 */
public class KThread {
    /**
     * Get the current thread.
     *
     * @return	the current thread.
     */
    //获取当前线程
    public static KThread currentThread() {
	Lib.assertTrue(currentThread != null);
	return currentThread;
    }
    
    /**
     * Allocate a new <tt>KThread</tt>. If this is the first <tt>KThread</tt>,
     * create an idle thread as well.
     */

    //创建一个新的<tt>KThread</tt
	//如果这是第一个线程 则创建一个空闲线程
    public KThread() {
	if (currentThread != null) {
	    tcb = new TCB();
		joinQueue =ThreadedKernel.scheduler.newThreadQueue(false);
	}	    
	else {
		//分配新的线程队列
	    readyQueue = ThreadedKernel.scheduler.newThreadQueue(false);

		joinQueue =ThreadedKernel.scheduler.newThreadQueue(false);
	    //此线程被调用
	    readyQueue.acquire(this);	    

	    //当前运行的线程
	    currentThread = this;
	    //tcb   第一个TCB在machine 的main函数创建好了

	    tcb = TCB.currentTCB();

	    name = "main";


		//准备将线程启动
	    restoreState();


	    createIdleThread();
	}
    }

    /**
     * Allocate a new KThread.
     *
     * @param	target	the object whose <tt>run</tt> method is called.
     */
    //分配一个新的线程
    public KThread(Runnable target) {
	this();
	this.target = target;
    }

    /**
     * Set the target of this thread.
     *
     * @param	target	the object whose <tt>run</tt> method is called.
     * @return	this thread.
     */
    //设置此线程的目标线程（调用了<tt>run</tt> 的那些线程）。
    public KThread setTarget(Runnable target) {
	Lib.assertTrue(status == statusNew);
	
	this.target = target;
	return this;
    }

    /**
     * Set the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @param	name	the name to give to this thread.
     * @return	this thread.
     */

    //设置线程的名称  名称用于调试
    public KThread setName(String name) {
	this.name = name;
	return this;
    }

    /**
     * Get the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @return	the name given to this thread.
     */
    //获取名称
    public String getName() {
	return name;
    }

    /**
     * Get the full name of this thread. This includes its name along with its
     * numerical ID. This name is used for debugging purposes only.
     *
     * @return	the full name given to this thread.
     */
    //获取name和id  id为现成的唯一标识符
    public String toString() {
	return (name + " (#" + id + ")");
    }

    /**
     * Deterministically and consistently compare this thread to another
     * thread.
     */
    //比较两线程是否为同一线程
    public int compareTo(Object o) {
	KThread thread = (KThread) o;

	if (id < thread.id)
	    return -1;
	else if (id > thread.id)
	    return 1;
	else
	    return 0;
    }

    /**
     * Causes this thread to begin execution. The result is that two threads
     * are running concurrently: the current thread (which returns from the
     * call to the <tt>fork</tt> method) and the other thread (which executes
     * its target's <tt>run</tt> method).
     */
    //fork一个子线程    两个线程同时运行  当前线程（执行fork方法的线程） 和另一个线程（执行run方法的线程）
    public void fork() {
	Lib.assertTrue(status == statusNew);
	Lib.assertTrue(target != null);
	
	Lib.debug(dbgThread,
		  "Forking thread: " + toString() + " Runnable: " + target);


	//关中断   并返回旧的中断状态
  	boolean intStatus = Machine.interrupt().disable();

  	//让此TCB表示的线程开始执行
		// 这个指定的目标在线程中执行

	tcb.start(new Runnable() {
		public void run() {
		    runThread();
		}
	    });

	ready();

	//将进程设置为旧的中断状态
	Machine.interrupt().restore(intStatus);
    }

    private void runThread() {
	begin();
	target.run();
	finish();
    }

    private void begin() {
	Lib.debug(dbgThread, "Beginning thread: " + toString());
	
	Lib.assertTrue(this == currentThread);

	restoreState();

	Machine.interrupt().enable();
    }

    /**
     * Finish the current thread and schedule it to be destroyed when it is
     * safe to do so. This method is automatically called when a thread's
     * <tt>run</tt> method returns, but it may also be called directly.
     *
     * The current thread cannot be immediately destroyed because its stack and
     * other execution state are still in use. Instead, this thread will be
     * destroyed automatically by the next thread to run, when it is safe to
     * delete this thread.
     */


	/**
	 *
	 *     //完成当前线程并计划在安全时将其销毁。当线程的<tt>run</tt>方法返回时，会自动调用此方法，但也可以直接调用它。
	 * 	//可能无法销毁线程  因为 堆 和其他执行程序正在使用它   当可以安全销毁此线程时  下一个要运行的线程将自动销毁此线程
	 */
    public static void finish() {
	Lib.debug(dbgThread, "Finishing thread: " + currentThread.toString());

	//关中断
	Machine.interrupt().disable();

	//将当前线程的join队列中的线程加入ready队列
		currentThread().wakeJoiners();
	//当前线程已完成 销毁当前线程的tcb
	Machine.autoGrader().finishingCurrentThread();

	Lib.assertTrue(toBeDestroyed == null);
	//当前线程可以销毁 tcb可以被销毁
	toBeDestroyed = currentThread;


	currentThread.status = statusFinished;
		/**
		 * task1 方法2  使用信号量
		 *
		 * 当A线程 调用B。join时  B执行完毕之后 会执行到这里 将当前线程的信号量+1  可以正常运行 A线程
		 */
//		currentThread.joinSem.V();
	
	sleep();
    }
	/**
	 * task1  获取当前线程程的join队列中的线程
	 */

	public void wakeJoiners() {
		boolean intStatus = Machine.interrupt().disable();
		KThread thread = null;
		do {
				thread =joinQueue.nextThread();

			if(thread != null) {
				thread.ready();
			}
		} while(thread != null);
		Machine.interrupt().restore(intStatus);
	}
	/**
     * Relinquish the CPU if any other thread is ready to run. If so, put the
     * current thread on the ready queue, so that it will eventually be
     * rescheuled.
     *
     * <p>
     * Returns immediately if no other thread is ready to run. Otherwise
     * returns when the current thread is chosen to run again by
     * <tt>readyQueue.nextThread()</tt>.
     *
     * <p>
     * Interrupts are disabled, so that the current thread can atomically add
     * itself to the ready queue and switch to the next thread. On return,
     * restores interrupts to the previous state, in case <tt>yield()</tt> was
     * called with interrupts disabled.
     */

	/**
	 * 如果有其他线程准备运行，放弃CPU。 ，请将当前线程放到就绪队列中，以便它最终被重新调度。
	 *如果没有其他线程准备运行，则立即返回。否则，当通过<tt>readyqueue.nextthread（）</tt>选择当前线程再次运行时返回。
	 *
	 * 中断被禁用，因此当前线程可以原子地将自身添加到就绪队列并切换到下一个线程。返回时，将中断恢复到以前的状态，以防在中断被禁用的情况下调用<tt>yield（）</tt>。
	 */
	//表示让出cpu的使用权
	public static void yield() {
	Lib.debug(dbgThread, "Yielding thread: " + currentThread.toString());
	
	Lib.assertTrue(currentThread.status == statusRunning);

	//关中断
	boolean intStatus = Machine.interrupt().disable();

	//当前线程
	currentThread.ready();

	//运行下一个线程
	runNextThread();
	//设置中断为原来的 状态
	Machine.interrupt().restore(intStatus);
    }

    /**
     * Relinquish the CPU, because the current thread has either finished or it
     * is blocked. This thread must be the current thread.
     *
     * <p>
     * If the current thread is blocked (on a synchronization primitive, i.e.
     * a <tt>Semaphore</tt>, <tt>Lock</tt>, or <tt>Condition</tt>), eventually
     * some thread will wake this thread up, putting it back on the ready queue
     * so that it can be rescheduled. Otherwise, <tt>finish()</tt> should have
     * scheduled this thread to be destroyed by the next thread to run.
     */

	/**
	 * 放弃CPU，因为当前线程已完成或被阻塞。此线程必须是当前线程。
	 *
	 * 如果当前线程被阻塞（在同步原语上，即
	 *  <tt>Semaphore</tt>, <tt>Lo
	 *
	 *  ck</tt>, or <tt>Condition</tt>，
	 * 一些线程将唤醒此线程，并将其放回就绪队列
	 * 以便重新调度
	 * 如果执行<tt>finish（）</tt>应该
	 * 计划此线程被下一个要运行的线程销毁。
	 */
	public static void sleep() {
	Lib.debug(dbgThread, "Sleeping thread: " + currentThread.toString());
	
	Lib.assertTrue(Machine.interrupt().disabled());

	if (currentThread.status != statusFinished)
	    currentThread.status = statusBlocked;

	runNextThread();
    }

    /**
     * Moves this thread to the ready state and adds this to the scheduler's
     * ready queue.
     */
    //将此线程变为到就绪状态  并将其添加到计划程序的就绪队列
    public void ready() {
	Lib.debug(dbgThread, "Ready thread: " + toString());
	
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(status != statusReady);
	
	status = statusReady;
	//添加到就绪队列
	if (this != idleThread)
	    readyQueue.waitForAccess(this);

		//移到就绪状态
	Machine.autoGrader().readyThread(this);
    }

    /**
     * Waits for this thread to finish. If this thread is already finished,
     * return immediately. This method must only be called once; the second
     * call is not guaranteed to return. This thread must not be the current
     * thread.
     */
	/**
	 *
	 * 等待此线程完成。如果此线程已完成，请立即返回。此方法只能调用一次；第二次调用不能保证返回。此线程不能是当前线程。？？？？？？？
	 */
	/**
	 *
	 * join（）方法  另一个线程不必调用join
	 * 但是如果另一个线程调用了join，则只能调用一次。
	 * 第二次调用会返回undefined   即使第二个调用方与第一个调用方不是同一个线程
	 * 无论是否调用join方法  一个线程都必须正常完成执行
	 *
	 * 意思就是 ：如果主线程调用了a.JOIN 则主线程会被阻塞  a线程将执行 直到a线程返回  主线程才会继续执行
	 * 在一个KThread对象上只能调用一次join，且当前线程不能对自身调用join
	 *
	 * 需要修改finish方法 如果有被join挂起的线程  则 finish之后需要重新调用
	 *
	 */
	public void join() {
	Lib.debug(dbgThread, "Joining to thread: " + toString());

	//实现线程只能调用一次join（）
	Lib.assertTrue(this != currentThread);
		/**
		 *
		 * task1   添加一个join队列  如果 在线程A上调用了B.join  则将A加入B的join队列 然后A阻塞  直到B执行结束后 A从join队列 移到就绪队列 可以继续执行
		 *
		 * 方法wakeJoiners()
		 */


		//关中断  并且返回当前中断状态
		boolean InterruptStatus  = Machine.interrupt().disable();

		if(status == statusFinished)
		{
//			System.out.println("调用Join方法的线程" +id+name +"已经finish 无需继续执行");
		}
		else
		{
			joinQueue.waitForAccess(currentThread());

			sleep();
		}

		Machine.interrupt().restore(InterruptStatus);

		/**
		 * task1 方法2  使用信号量?
		 *
		 * A线程中调用B.join()   执行这里  信号量变为-1 阻塞A进程   B进程正常执行  当B 执行完毕时 调用了B.finish
		 */

//		this.joinSem.P();


    }

    /**
     * Create the idle thread. Whenever there are no threads ready to be run,
     * and <tt>runNextThread()</tt> is called, it will run the idle thread. The
     * idle thread must never block, and it will only be allowed to run when
     * all other threads are blocked.
     *
     * <p>
     * Note that <tt>ready()</tt> never adds the idle thread to the ready set.
     */


    /**
	 *
	 * //创建空闲线程  如果没有ready的线程，并且调用了runnextthread（）</tt>   它将运行空闲线程
	 * 	//空闲线程决不能被阻塞，只有当所有其他线程都被阻塞时，才允许它运行。
	 */

    private static void createIdleThread() {
	Lib.assertTrue(idleThread == null);


	//创建空闲线程
	idleThread = new KThread(new Runnable() {
	    public void run() { while (true) yield(); }
	});
	idleThread.setName("idle");

	Machine.autoGrader().setIdleThread(idleThread);
	
	idleThread.fork();
    }
    
    /**
     * Determine the next thread to run, then dispatch the CPU to the thread
     * using <tt>run()</tt>.
     */
    //确定要运行的下一个线程，然后使用<tt>run（）</tt>将CPU分派给该线程
    private static void runNextThread() {
	KThread nextThread = readyQueue.nextThread();
	if (nextThread == null)
	    nextThread = idleThread;

	nextThread.run();
    }

    /**
     * Dispatch the CPU to this thread. Save the state of the current thread,
     * switch to the new thread by calling <tt>TCB.contextSwitch()</tt>, and
     * load the state of the new thread. The new thread becomes the current
     * thread.
     *
     * <p>
     * If the new thread and the old thread are the same, this method must
     * still call <tt>saveState()</tt>, <tt>contextSwitch()</tt>, and
     * <tt>restoreState()</tt>.
     *
     * <p>
     * The state of the previously running thread must already have been
     * changed from running to blocked or ready (depending on whether the
     * thread is sleeping or yielding).
     *
     * @paramfinishing	<tt>true</tt> if the current thread is
     *				finished, and should be destroyed by the new
     *				thread.
     */
	/**
	 *
	 */
	/**
	 * 将CPU分派到此线程。保存当前线程的状态
	 * 通过调用<tt>tcb.contextswitch（）</tt>切换到新线程，并加载新线程的状态。新线程成为当前线程
	 *
	 * 如果新线程和旧线程相同，则此方法仍必须调用<tt>savestate（）</tt>、<tt>contextswitch（）</tt>和<tt>restorestate（）</tt>。
	 *
	 * 以前运行的线程的状态必须已经从running更改为blocked或ready（取决于线程是在sleeping还是yielding）。
	 */

	//cpu执行下一个线程  进行 上下文的切换
	private void run() {
	Lib.assertTrue(Machine.interrupt().disabled());

	//移交非nachos线程    在非抢占式jvm中使用，使非nachos线程有机会运行。
	Machine.yield();

	currentThread.saveState();

	Lib.debug(dbgThread, "Switching from: " + currentThread.toString()
		  + " to: " + toString());

	currentThread = this;

	tcb.contextSwitch();

	currentThread.restoreState();
    }

    /**
     * Prepare this thread to be run. Set <tt>status</tt> to
     * <tt>statusRunning</tt> and check <tt>toBeDestroyed</tt>.
     */
    //准备将线程启动    设置线程状态为<tt>statusRunning</tt>然后检查是否被销毁
    protected void restoreState() {
	Lib.debug(dbgThread, "Running thread: " + currentThread.toString());

	//确保不能中断
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(this == currentThread);
	Lib.assertTrue(tcb == TCB.currentTCB());

		//通知自动加载器指定的线程正在运行
	Machine.autoGrader().runningThread(this);

	//线程状态
	status = statusRunning;

	//销毁准备销毁的线程
	if (toBeDestroyed != null) {
	    toBeDestroyed.tcb.destroy();
	    toBeDestroyed.tcb = null;
	    toBeDestroyed = null;
	}
    }

    /**
     * Prepare this thread to give up the processor. Kernel threads do not
     * need to do anything here.
     */
    //	此线程要放弃处理器  执行中断
    protected void saveState() {
	Lib.assertTrue(Machine.interrupt().disabled());
	Lib.assertTrue(this == currentThread);
    }

    private static class PingTest implements Runnable {
	PingTest(int which) {
	    this.which = which;
	}
	
	public void run() {
	    for (int i=0; i<5; i++) {
		System.out.println("*** thread " + which + " looped "
				   + i + " times");
		currentThread.yield();
	    }
	}

	private int which;
    }
	/**
	 * Sets the priority of a thread()
	 */
	public void setPriority(int priority) {
		/* Disable interrupts */
		boolean intStatus = Machine.interrupt().disable();

		/* Talk to the scheduler */
		ThreadedKernel.scheduler.setPriority(this, priority);

		/* Restore interrupts */
		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Gets the priority of a thread()
	 */
	public int getPriority() {
		/* Disable interrupts */
		boolean intStatus = Machine.interrupt().disable();

		/* Talk to the scheduler */
		int priority = ThreadedKernel.scheduler.getPriority(this);

		/* Restore interrupts */
		Machine.interrupt().restore(intStatus);

		return priority;
	}

    /**
     * Tests whether this module is working.
     */
    //测试此模块是否工作。
    public static void selfTest() {
	Lib.debug(dbgThread, "Enter KThread.selfTest");


//			KThreadTest.runTest();
//	new KThread(new PingTest(1)).setName("forked thread").fork();
//	new PingTest(0).run();

		joinSelfTest();
    }
	public static void joinSelfTest() {
		System.out.println("---------join() Test---------");
		KThread t1 = new KThread(new Runnable() {
			public void run() {
				System.out.println("is going to make t2");
				KThread t2 = new KThread(new Runnable() {
					public void run() {
						System.out.println("Inside t2");
					}
				});
				System.out.println("t2 is going to fork");
				t2.fork();
				System.out.println("t2 forked");
				t2.join();
				System.out.println("t2 joined");
			}
		});//

		System.out.println("t1 is going to fork");
		t1.fork();
		System.out.println("t1 forked");
		t1.join();
		System.out.println("t1 joined");
		System.out.println("---------End join() Test---------\n");
	}
    private static final char dbgThread = 't';

    /**
     * Additional state used by schedulers.
     *
     * @see	PriorityScheduler.ThreadState
     */
    //调度状态 （优先级调度？）
    public Object schedulingState = null;

    //五种 线程状态
	//新创建 还没有执行fork的线程
    private static final int statusNew = 0;
    //等待访问CPU的线程。kthread.ready（）将把线程添加到就绪队列，并将状态设置为statusready。
    private static final int statusReady = 1;

    //当前正在使用CPU的线程。kthread.restorestate（）负责将status设置为statusrunning，并由kthread.runnextthread（）调用。
    private static final int statusRunning = 2;

    //一个正在休眠（由kthread.sleep（）设置）的线程，它正在等待除
	//CPU。
    private static final int statusBlocked = 3;

    //预定要销毁的线程。使用kthread.finish（）设置此状态。
    private static final int statusFinished = 4;

    /**
     * The status of this thread. A thread can either be new (not yet forked),
     * ready (on the ready queue but not running), running, or blocked (not
     * on the ready queue and not running).
     */

    //标志当前现成的状态
    private int status = statusNew;


    private String name = "(unnamed thread)";
    private Runnable target;
    private TCB tcb;

    /**
     * Unique identifer for this thread. Used to deterministically compare
     * threads.
     */
    //此线程的唯一标识符
			//用于确定的比较线程
    private int id = numCreated++;
    /** Number of times the KThread constructor was called. */
    //调用kthread构造函数的次数。
    private static int numCreated = 0;


    private static ThreadQueue readyQueue = null;
    private static KThread currentThread = null;
    private static KThread toBeDestroyed = null;
    //空闲线程   猜测 ： 当cpu没有线程 运行是 会运行这个线程  然后执行yield方法
    private static KThread idleThread = null;

	/**
	 * task1 方法1：为每个线程建一个join队列 在此线程上调用过join方法的其他线程  加入join队列
	 *
	 */

	private ThreadQueue joinQueue = null;

	/**
	 * task1 方法2：使用信号量  java线程是这么实现的
	 *
	 */
	//一个信号量 初始值为0
	private Semaphore joinSem= new Semaphore(0);

}
