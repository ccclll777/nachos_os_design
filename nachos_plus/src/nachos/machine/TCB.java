// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

import nachos.security.Privilege;
import nachos.threads.KThread;

import java.util.Vector;

/**
 * A TCB simulates the low-level details necessary to create, context-switch,
 * and destroy Nachos threads. Each TCB controls an underlying JVM Thread
 * object.
 *
 * <p>
 * Do not use any methods in <tt>java.lang.Thread</tt>, as they are not
 * compatible with the TCB API. Most <tt>Thread</tt> methods will either crash
 * Nachos or have no useful effect.
 *
 * <p>
 * Do not use the <i>synchronized</i> keyword <b>anywhere</b> in your code.
 * It's against the rules, <i>and</i> it can easily deadlock nachos.
 */

//TCB模拟了创建、上下文切换和 销毁nachos线程  每个TCB控制一个底层的jvm线程对象
//不要使用任何<tt>java.lang.Thread</tt>中的方法  因为它们与tcb api不兼容  大多数线程方法都会崩溃  nachos将没有作用
//不要使用<i>synchronized</i> keyword <b>anywhere</b> in your code.
public final class TCB {
    /**
     * Allocate a new TCB.
     */
    public TCB() {
    }
     
    /**
     * Give the TCB class the necessary privilege to create threads. This is
     * necessary, because unlike other machine classes that need privilege, we
     * want the kernel to be able to create TCB objects on its own.
     *
     * @param	privilege      	encapsulates privileged access to the Nachos
     *				machine.
     */
    //赋予tcb类需要的特权去创建线程
	//这是必须的 应为他不像其他需要特权的machine类  他们希望内核能自己创建tcb对象
    public static void givePrivilege(Privilege privilege) {
	TCB.privilege = privilege;
	privilege.tcb = new TCBPrivilege();
    }
    
    /**
     * Causes the thread represented by this TCB to begin execution. The
     * specified target is run in the thread.
     */
    //让此TCB表示的线程开始执行
	//这个指定的目标在线程中执行
    public void start(Runnable target) {
	/* We will not use synchronization here, because we're assuming that
	 * either this is the first call to start(), or we're being called in
	 * the context of another TCB. Since we only allow one TCB to run at a
	 * time, no synchronization is necessary.
	 *
	 * The only way this assumption could be broken is if one of our
	 * non-Nachos threads used the TCB code.
	 */
	//我们不会在这里进行同步 因为我们假设 这是对start（）方法的第一次调用，或者我们正在 被调用的另一个tcb的上下文
		//因为我们每次只允许运行一个tcb，所以不需要同步
		//唯一能打破这种假设的方法是在非nachos线程使用这些代码
	/* Make sure this TCB has not already been started. If done is false,
	 * then destroy() has not yet set javaThread back to null, so we can
	 * use javaThread as a reliable indicator of whether or not start() has
	 * already been invoked.
	 */

	//确定此TCB没有被启动 如果结果为false  那么destroy()方法没有将javaThread设置为null
		//所以我们使用javaThread作为一个可靠的标志来表示start（）是否已经被调用
	Lib.assertTrue(javaThread == null && !done);

	/* Make sure there aren't too many running TCBs already. This
	 * limitation exists in an effort to prevent wild thread usage.
	 */
	//确保已经没有太多正在运行的tcb。 保证运行的线程没有大于最大线程数
	Lib.assertTrue(runningThreads.size() < maxThreads);

	isFirstTCB = (currentTCB == null);

	/* Probably unnecessary sanity check: if this is not the first TCB, we
	 * make sure that the current thread is bound to the current TCB. This
	 * check can only fail if non-Nachos threads invoke start().
	 */
	//可能不必要的检查：如果这不是第一个tcb，我们将确保当前线程绑定到当前tcb。只有在非nachos线程调用start（）时，此检查才能失败。
	if (!isFirstTCB)
	    Lib.assertTrue(currentTCB.javaThread == Thread.currentThread());

	/* At this point all checks are complete, so we go ahead and start the
	 * TCB. Whether or not this is the first TCB, it gets added to
	 * runningThreads, and we save the target closure.
	 */

	//在这一点上，所有的检查都完成了，所以我们继续并启动tcb。不管这是否是第一个tcb，
	// 它都会被添加到runningthreads中，然后我们保存目标闭包。
	runningThreads.add(this);

	this.target = target;

	if (!isFirstTCB) {
	    /* If this is not the first TCB, we have to make a new Java thread
	     * to run it. Creating Java threads is a privileged operation.
	     */
	    //如果这不是第一个TCB，我们必须创建一个新的Java线程来运行它。创建Java线程是一种特权操作。
	    tcbTarget = new Runnable() {
		    public void run() { threadroot(); }
		};

	    privilege.doPrivileged(new Runnable() {
		    public void run() { javaThread = new Thread(tcbTarget); }
		});

	    /* The Java thread hasn't yet started, but we need to get it
	     * blocking in yield(). We do this by temporarily turning off the
	     * current TCB, starting the new Java thread, and waiting for it
	     * to wake us up from threadroot(). Once the new TCB wakes us up,
	     * it's safe to context switch to the new TCB.
	     */

	     //Java线程还没有启动，但我们需要将它阻塞在内存中。我们通过暂时关闭当前的TCB，启动新的Java线程，
		//并等待它从threadroot()唤醒
		//一旦新的tcb唤醒我们，就可以安全地将上下文切换到新的tcb。
	    currentTCB.running = false;
	    //此TCB对应的java 线程start
	    this.javaThread.start();
	    //等待中断
	    currentTCB.waitForInterrupt();
	}
	else {
	    /* This is the first TCB, so we don't need to make a new Java
	     * thread to run it; we just steal the current Java thread.
	     */
	    //这是第一个TCB，所以我们不需要用一个新的Java线程来运行它，我们只是窃取当前的Java线程。
	    javaThread = Thread.currentThread();

	    /* All we have to do now is invoke threadroot() directly. */

	    threadroot();
	}
    }

    /**
     * Return the TCB of the currently running thread.
     */
    //返回当前运行线程的TCB
    public static TCB currentTCB() {
	return currentTCB;
    }

    /**
     * Context switch between the current TCB and this TCB. This TCB will
     * become the new current TCB. It is acceptable for this TCB to be the
     * current TCB.
     */

    //上下文切换
    public void contextSwitch() {
	/* Probably unnecessary sanity check: we make sure that the current
	 * thread is bound to the current TCB. This check can only fail if
	 * non-Nachos threads invoke start().
	 */
	//我们确保当前线程绑定到当前tcb。只有在非nachos线程调用start（）时，此检查才能失败。
	Lib.assertTrue(currentTCB.javaThread == Thread.currentThread());

	// make sure AutoGrader.runningThread() called associateThread()
		//确保调用过associateThread()方法
	Lib.assertTrue(currentTCB.associated);
	currentTCB.associated = false;
	
	// can't switch from a TCB to itself
		 //不能进行上下文切换
	if (this == currentTCB)
	    return;

	/* There are some synchronization concerns here. As soon as we wake up
	 * the next thread, we cannot assume anything about static variables,
	 * or about any TCB's state. Therefore, before waking up the next
	 * thread, we must latch the value of currentTCB, and set its running
	 * flag to false (so that, in case we get interrupted before we call
	 * yield(), the interrupt will set the running flag and yield() won't
	 * block).
	 */

	//这里有一些同步问题。一旦我们唤醒下一个线程，我们就不能假设任何关于静态变量或任何tcb状态的事情。
		//因此，在唤醒下一个线程之前，我们必须锁定currenttcb的值，
		// 并将其running标志设置为false（这样，如果在调用yield（）之前中断，中断将设置running标志并且yield（）不会阻塞）
	TCB previous = currentTCB;
	previous.running = false;
	
	this.interrupt();
	previous.yield();
    }
    
    /**
     * Destroy this TCB. This TCB must not be in use by the current thread.
     * This TCB must also have been authorized to be destroyed by the
     * autograder.
     */
    //销毁这个TCB 当前线程不能使用此TCB
    public void destroy() {
	// make sure the current TCB is correct
		//确保是当前线程的TCB
	Lib.assertTrue(currentTCB != null &&
		   currentTCB.javaThread == Thread.currentThread());
	// can't destroy current thread
		//不能销毁当前线程
	Lib.assertTrue(this != currentTCB);

	// thread must have started but not be destroyed yet
		//线程必须已启动但尚未被销毁
	Lib.assertTrue(javaThread != null && !done);

	// ensure AutoGrader.finishingCurrentThread() called authorizeDestroy()
	Lib.assertTrue(nachosThread == toBeDestroyed);
	//
	toBeDestroyed = null;

	this.done = true;
	currentTCB.running = false;

	this.interrupt();
	currentTCB.waitForInterrupt();
	
	this.javaThread = null;
    }

    /**
     * Destroy all TCBs and exit Nachos. Same as <tt>Machine.terminate()</tt>.
     */
    //销毁所有TCB并退出Nachos。与machine.terminate（）相同。
    public static void die() {
	privilege.exit(0);
    }

    /**
     * Test if the current JVM thread belongs to a Nachos TCB. The AWT event
     * dispatcher is an example of a non-Nachos thread.
     *
     * @return	<tt>true</tt> if the current JVM thread is a Nachos thread.
     */
    public static boolean isNachosThread() {
	return (currentTCB != null &&
		Thread.currentThread() == currentTCB.javaThread);
    }

    private void threadroot() {
	// this should be running the current thread
		//必须是当前线程执行他
	Lib.assertTrue(javaThread == Thread.currentThread());

	if (!isFirstTCB) {
	    /* start() is waiting for us to wake it up, signalling that it's OK
	     * to context switch to us. We leave the running flag false so that
	     * we'll still run if a context switch happens before we go to
	     * sleep. All we have to do is wake up the current TCB and then
	     * wait to get woken up by contextSwitch() or destroy().
	     */

	    //start（）等待唤醒  表示可以切换上下文  将running标记变成false 以便 sleep之前仍然可以进行上下文切换 然后运行它
		//我们要做的就是唤醒当前TCB  然后等待被contextSwitch() or destroy().唤醒
	    currentTCB.interrupt();
	    this.yield();
	}
	else {
	    /* start() called us directly, so we just need to initialize
	     * a couple things.
	     */
	    //start（）直接调用了我们，所以我们只需要初始化一些东西。
	    currentTCB = this;
	    running = true;
	}

	try {
	    target.run();

	    // no way out of here without going throw one of the catch blocks
		//如果没有block 不能退出这里
	    Lib.assertNotReached();
	}
	catch (ThreadDeath e) {
	    // make sure this TCB is being destroyed properly
		//确保tcb被正确销毁
	    if (!done) {
		System.out.print("\nTCB terminated improperly!\n");
		privilege.exit(1);
	    }

	    //移除此线程
	    runningThreads.removeElement(this);
	    //如果没有TCB 则停机
	    if (runningThreads.isEmpty())
		privilege.exit(0);
	}
	catch (Throwable e) {
	    System.out.print("\n");
	    e.printStackTrace();

	    runningThreads.removeElement(this);
	    if (runningThreads.isEmpty())
		privilege.exit(1);
	    else
		die();
	}
    }

    /**
     * Invoked by threadroot() and by contextSwitch() when it is necessary to
     * wait for another TCB to context switch to this TCB. Since this TCB
     * might get destroyed instead, we check the <tt>done</tt> flag after
     * waking up. If it is set, the TCB that woke us up is waiting for an
     * acknowledgement in destroy(). Otherwise, we just set the current TCB to
     * this TCB and return.
     */

    //当需要等待另一个TCB到该TCB的上下文切换时， 由 threadroot()和contextSwitch()调用
	//因为这个TCB可能会被破坏，所以我们在唤醒后检查 <tt>done</tt>标记
	//如果它已经设置  如果设置了，唤醒我们的tcb将在destroy（）中等待确认。否则，我们只需将当前tcb设置为此tcb并返回。
    private void yield() {
	waitForInterrupt();
	
	if (done) {
	    currentTCB.interrupt();
	    throw new ThreadDeath();
	}

	currentTCB = this;
    }

    /**
     * Waits on the monitor bound to this TCB until its <tt>running</tt> flag
     * is set to <tt>true</tt>. <tt>waitForInterrupt()</tt> is used whenever a
     * TCB needs to go to wait for its turn to run. This includes the ping-pong
     * process of starting and destroying TCBs, as well as in context switching
     * from this TCB to another. We don't rely on <tt>currentTCB</tt>, since it
     * is updated by <tt>contextSwitch()</tt> before we get called.
     */
    //在绑定到此TCB的监视器上等待，直到其<tt>running</tt>标志设置为<tt>true</tt>。
	//<tt>waitForInterrupt（）</tt>用于TCB需要等待轮到其运行时使用。
	//这包括 启动和销毁  TCB  ping-pong 的过程  以及在上下文中从这个tcb切换到另一个tcb
	//我们不依赖于<t t>currenttcb</tt>，因为它在被调用之前由<tt>contextswitch（）</tt>更新

	/**
	 * 在JAVA多线程编程中，将需要并发执行的代码放在Thread类的run方法里面，然后创建多个Thread类的对象，调用start()方法，线程启动执行。
	 *
	 * 当某段代码需要互斥时，可以用 synchronized 关键字修饰
	 *
	 * synchronized 修饰方法时锁定的是调用该方法的对象。它并不能使调用该方法的多个对象在执行顺序上互斥。
	 *
	 * 只会锁定对象  在对象上进行 互斥  如果是两个对象分别调用 则不会进行互斥的操作
	 */
    private synchronized void waitForInterrupt() {
	while (!running) {
	    try {
	    	//导致线程进入等待状态，直到它被其他线程通过notify()或者notifyAll唤醒
	    	wait();
	    }
	    catch (InterruptedException e) { }
	}
    }

    /**
     * Wake up this TCB by setting its <tt>running</tt> flag to <tt>true</tt>
     * and signalling the monitor bound to it. Used in the ping-pong process of
     * starting and destroying TCBs, as well as in context switching to this
     * TCB.
     */
    //通过将tcb的<tt>running</tt>标志设置为<tt>true</tt>并向绑定到它的监视器发送信号来唤醒tcb
	//用于ping-pong process和销毁tcb的过程，以及在上下文中切换到此tcb。
    private synchronized void interrupt() {
	running = true;
	//随机选择一个在该对象上调用wait方法的线程，解除其阻塞状态。
		// 该方法只能在同步方法或同步块内部调用。如果当前线程不是锁的持有者，该方法抛出一个
	notify();
    }

    //协作线程？ 指定此TCB的KThread？
    private void associateThread(KThread thread) {
	// make sure AutoGrader.runningThread() gets called only once per
	// context switch
		//确保每次切换上下文只调用一次autograder.runningthread（）
	Lib.assertTrue(!associated);
	associated = true;

	Lib.assertTrue(thread != null);

	if (nachosThread != null)
	    Lib.assertTrue(thread == nachosThread);
	else
	    nachosThread = thread;
    }

    //销毁
    private static void authorizeDestroy(KThread thread) {
	// make sure AutoGrader.finishingThread() gets called only once per
	// destroy
		//确保每次销毁只调用autograder.finishingthread（）一次
	Lib.assertTrue(toBeDestroyed == null);
	toBeDestroyed = thread;
    }

    /**
     * The maximum number of started, non-destroyed TCB's that can be in
     * existence.
     */
    //启动 未销毁的最大线程数
    public static final int maxThreads = 250;

    /**
     * A reference to the currently running TCB. It is initialized to
     * <tt>null</tt> when the <tt>TCB</tt> class is loaded, and then the first
     * invocation of <tt>start(Runnable)</tt> assigns <tt>currentTCB</tt> a
     * reference to the first TCB. After that, only <tt>yield()</tt> can
     * change <tt>currentTCB</tt> to the current TCB, and only after
     * <tt>waitForInterrupt()</tt> returns.
     *
     * <p>
     * Note that <tt>currentTCB.javaThread</tt> will not be the current thread
     * if the current thread is not bound to a TCB (this includes the threads
     * created for the hardware simulation).
     */
    //当前运行TCB的引用  当加载tcb类是 被初始化为null  然后加载时第一个调用start（）方法   将 <tt>currentTCB</tt>引用分配给第一个TCB
			//然后只有 <tt>yield()</tt>方法将<tt>currentTCB</tt>改为当前TCB  并且在<tt>waitForInterrupt（）</tt>是返回

			//注意  如果当前线程未绑定到TCB（这包括为硬件模拟创建的线程）。  则<tt>currentTCB.javaThread</tt> 不是当前线程
    private static TCB currentTCB = null;


    /**
     * A vector containing all <i>running</i> TCB objects. It is initialized to
     * an empty vector when the <tt>TCB</tt> class is loaded. TCB objects are
     * added only in <tt>start(Runnable)</tt>, which can only be invoked once
     * on each TCB object. TCB objects are removed only in each of the
     * <tt>catch</tt> clauses of <tt>threadroot()</tt>, one of which is always
     * invoked on thread termination. The maximum number of threads in
     * <tt>runningThreads</tt> is limited to <tt>maxThreads</tt> by
     * <tt>start(Runnable)</tt>. If <tt>threadroot()</tt> drops the number of
     * TCB objects in <tt>runningThreads</tt> to zero, Nachos exits, so once
     * the first TCB is created, this vector is basically never empty.
     */
    //一个Vector包含所有<i>running</i>的TCB对象  他在加载<tt>TCB</tt>被初始化为空向量
	//TCB对象只能在<tt>start(Runnable)</tt>中才能添加  每个TCB对象只能调用一次
	//tcb对象只在<tt>threadroot（）</tt>的每个<tt>catch</tt>子句中被删除，其中一个总是在线程终止时调用。
			//<tt>runningthreads</tt>中的最大线程数被<tt>start（runnable）</tt>限制为<tt>maxthreads</tt>。
	// 如果<tt>threadroot（）</tt>将<tt>runningthreads</tt>中的tcb对象数减少到零，则nachos将退出，
	// 因此，一旦创建了第一个tcb，此向量基本上永远不会为空。
    private static Vector<TCB> runningThreads = new Vector<TCB>();

    //特权
    private static Privilege privilege;

    //
    private static KThread toBeDestroyed = null;

    /**
     * <tt>true</tt> if and only if this TCB is the first TCB to start, the one
     * started in <tt>Machine.main(String[])</tt>. Initialized by
     * <tt>start(Runnable)</tt>, on the basis of whether <tt>currentTCB</tt>
     * has been initialized.
     */
    //如果TCB被第一个初始化(第一个执行<tt>Machine.main(String[])</tt>) 则他为True

    private boolean isFirstTCB;

    /**
     * A reference to the Java thread bound to this TCB. It is initially
     * <tt>null</tt>, assigned to a Java thread in <tt>start(Runnable)</tt>,
     * and set to <tt>null</tt> again in <tt>destroy()</tt>.
     */
    //绑定此TCB线程的JAVA线程引用 刚开始是null
			//<tt>start(Runnable)</tt>,时被分配
			//<tt>destroy()</tt>.时又变成null
    private Thread javaThread = null;

    /**
     * <tt>true</tt> if and only if the Java thread bound to this TCB ought to
     * be running. This is an entirely different condition from membership in
     * <tt>runningThreads</tt>, which contains all TCB objects that have
     * started and have not terminated. <tt>running</tt> is only <tt>true</tt>
     * when the associated Java thread ought to run ASAP. When starting or
     * destroying a TCB, this is temporarily true for a thread other than that
     * of the current TCB.
     */
    //当且仅当java线程绑定到这个tcb并且开始运行时为true，
	//这是与<tt>runningthreads</tt>中的成员身份完全不同的条件，后者包含已启动和未终止的所有tcb对象。
	//当关联的java线程经快运行时<tt>running</tt>为真
	//当启动或销毁TCB时 对于当前TCB以外的线程 暂时为真
    private boolean running = false;

    /**
     * Set to <tt>true</tt> by <tt>destroy()</tt>, so that when
     * <tt>waitForInterrupt()</tt> returns in the doomed TCB, <tt>yield()</tt>
     * will know that the current TCB is doomed.
     */
    //销毁时设置为true 以便在<tt>waitForInterrupt()</tt>时返回此TCB
    // <tt>yield()</tt>的方法可以知道当前TCB的doomed
    private boolean done = false;
    
    private KThread nachosThread = null;
    private boolean associated = false;
    private Runnable target;
    private Runnable tcbTarget;

    private static class TCBPrivilege implements Privilege.TCBPrivilege {
	public void associateThread(KThread thread) {
	    Lib.assertTrue(currentTCB != null);
	    currentTCB.associateThread(thread);
	}
	public void authorizeDestroy(KThread thread) {
	    TCB.authorizeDestroy(thread);
	}
    }
}
