// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.ag;

import nachos.machine.*;
import nachos.security.Privilege;
import nachos.threads.KThread;

import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * The default autograder. Loads the kernel, and then tests it using
 * <tt>Kernel.selfTest()</tt>.
 */
//创建内核 启动程序
public class AutoGrader {
    /**
     * Allocate a new autograder.
     */
    public AutoGrader() {
    }

    /**
     * Start this autograder. Extract the <tt>-#</tt> arguments, call
     * <tt>init()</tt>, load and initialize the kernel, and call
     * <tt>run()</tt>.
     *
     * @param	privilege      	encapsulates privileged access to the Nachos
     * 				machine.
     */
    //启动 调用 <tt>init()</tt>初始化内核  然后<tt>run()</tt>.启动
    public void start(Privilege privilege) {
	Lib.assertTrue(this.privilege == null,
		   "start() called multiple times");
	this.privilege = privilege;

	//复制args重的内容返回
	String[] args = Machine.getCommandLineArguments();

	extractArguments(args);

	System.out.print(" grader");

	init();

	System.out.print("\n");	

	//初始化配置文件中的一个kernel类
	kernel =
	    (Kernel) Lib.constructObject(Config.getString("Kernel.kernel"));
	kernel.initialize(args);

	run();
    }

    //执行 命令行的输入
    private void extractArguments(String[] args) {
	String testArgsString = Config.getString("AutoGrader.testArgs");
	if (testArgsString == null) {
		testArgsString = "";
	}

	//判断有无-#
	for (int i=0; i<args.length; ) {
	    String arg = args[i++];
	    if (arg.length() > 0 && arg.charAt(0) == '-') {
		if (arg.equals("-#")) {
		    Lib.assertTrue(i < args.length,
			       "-# switch missing argument");
		    testArgsString = args[i++];
		}
	    }
	}

	// StringTokenizer(String str, String delim) ：构造一个用来解析 str 的 StringTokenizer 对象，并提供一个指定的分隔符。
	StringTokenizer st = new StringTokenizer(testArgsString, ",\n\t\f\r");

	while (st.hasMoreTokens()) {
	    StringTokenizer pair = new StringTokenizer(st.nextToken(), "=");

	    Lib.assertTrue(pair.hasMoreTokens(),
		       "test argument missing key");
	    String key = pair.nextToken();

	    Lib.assertTrue(pair.hasMoreTokens(),
		       "test argument missing value");
	    String value = pair.nextToken();

	    testArgs.put(key, value);
	}	
    }

    //根据key获取配置文件中的String类型的value？
    String getStringArgument(String key) {
	String value = (String) testArgs.get(key);
	Lib.assertTrue(value != null,
		   "getStringArgument(" + key + ") failed to find key");
	return value;
    }

	//根据key获取配置文件中的int类型的value？
    int getIntegerArgument(String key) {
	try {
	    return Integer.parseInt(getStringArgument(key));
	}
	catch (NumberFormatException e) {
	    Lib.assertNotReached("getIntegerArgument(" + key + ") failed: " +
				 "value is not an integer");
	    return 0;
	}
    }

	//根据key获取配置文件中的boolean类型的value？
    boolean getBooleanArgument(String key) {
	String value = getStringArgument(key);

	if (value.equals("1") || value.toLowerCase().equals("true")) {
	    return true;
	}
	else if (value.equals("0") || value.toLowerCase().equals("false")) {
	    return false;
	}
	else {
	    Lib.assertNotReached("getBooleanArgument(" + key + ") failed: " +
				 "value is not a boolean");
	    return false;
	}	
    }

    //获取当前的时钟数
    long getTime() {
	return privilege.stats.totalTicks;
    }

    void targetLevel(int targetLevel) {
	this.targetLevel = targetLevel;
    }

    void level(int level) {
	this.level++;	
	Lib.assertTrue(level == this.level,
		   "level() advanced more than one step: test jumped ahead");
	
	if (level == targetLevel)
	    done();
    }

    private int level = 0, targetLevel = 0;

    void done() {
	System.out.print("\nsuccess\n");
	privilege.exit(162);
    }

    private Hashtable<String, String> testArgs = 
        new Hashtable<String, String>();

    void init() {
    }
    
    void run() {
    	//执行测试
	kernel.selfTest();
	//运行用户代码
	kernel.run();
	//内核终止
	kernel.terminate();
    }

    Privilege privilege = null;
    Kernel kernel;

    /**
     * Notify the autograder that the specified thread is the idle thread.
     * <tt>KThread.createIdleThread()</tt> <i>must</i> call this method before
     * forking the idle thread.
     *
     * @param	idleThread	the idle thread.
     */
    //通知自动加载器指定的线程是空闲线程。  <tt>KThread.createIdleThread()</tt> 必须在fork空闲线程之前调用此方法。
    public void setIdleThread(KThread idleThread) {
    }

    /**
     * Notify the autograder that the specified thread has moved to the ready
     * state. <tt>KThread.ready()</tt> <i>must</i> call this method before
     * returning.
     *
     * @param	thread	the thread that has been added to the ready set.
     */
    //通知自动加载器指定的线程已移动到就绪状态。<tt>kthread.ready（）</tt><i>必须在返回之前调用此方法。
    public void readyThread(KThread thread) {
    }

    /**
     * Notify the autograder that the specified thread is now running.
     * <tt>KThread.restoreState()</tt> <i>must</i> call this method before
     * returning.
     *
     * @param	thread	the thread that is now running.
     */
    //通知自动加载器指定的线程正在运行   在<tt>KThread.restoreState()</tt>前调用此方法
    public void runningThread(KThread thread) {
	privilege.tcb.associateThread(thread);
	currentThread = thread;
    }

    /**
     * Notify the autograder that the current thread has finished.
     * <tt>KThread.finish()</tt> <i>must</i> call this method before putting
     * the thread to sleep and scheduling its TCB to be destroyed.
     */
    //通知自动加载器当前线程已完成  <tt>kthread.finish（）</tt><i>必须先调用此方法，然后才能使线程进入sleep状态并 计划销毁其tcb。
    public void finishingCurrentThread() {
	privilege.tcb.authorizeDestroy(currentThread);
    }

    /**
     * Notify the autograder that a timer interrupt occurred and was handled by
     * software if a timer interrupt handler was installed. Called by the
     * hardware timer.
     *
     * @param	privilege	proves the authenticity of this call.
     * @param	time	the actual time at which the timer interrupt was
     *			issued.
     */
    //通知自动加载器发生计时器中断，如果安装了计时器中断处理程序，则由软件处理。
	// 由硬件时钟调用。
    public void timerInterrupt(Privilege privilege, long time) {
	Lib.assertTrue(privilege == this.privilege,
		   "security violation");
    }

    /**
     * Notify the autograder that a user program executed a syscall
     * instruction.
     *
     * @param	privilege	proves the authenticity of this call.
     * @return	<tt>true</tt> if the kernel exception handler should be called.
     */
    //通知自动加载器用户程序执行了syscall指令。
    public boolean exceptionHandler(Privilege privilege) {
	Lib.assertTrue(privilege == this.privilege,
		   "security violation");
	return true;
    }

    /**
     * Notify the autograder that <tt>Processor.run()</tt> was invoked. This
     * can be used to simulate user programs.
     *
     * @param	privilege	proves the authenticity of this call.
     */
    //通知自动加载器调用了<tt>processor.run（）</tt>。这可以用来模拟用户程序。
    public void runProcessor(Privilege privilege) {
	Lib.assertTrue(privilege == this.privilege,
		   "security violation");
    }

    /**
     * Notify the autograder that a COFF loader is being constructed for the
     * specified file. The autograder can use this to provide its own COFF
     * loader, or return <tt>null</tt> to use the default loader.
     *
     * @param	file	the executable file being loaded.
     * @return	a loader to use in loading the file, or <tt>null</tt> to use
     *		the default.
     */
    //通知自动加载器正在为指定文件构造COFF加载器。自动加载器可以使用它来提供自己的coff加载器，或者返回<tt>null</tt>来使用默认加载器。
    public Coff createLoader(OpenFile file) {
	return null;
    }

    /**
     * Request permission to send a packet. The autograder can use this to drop
     * packets very selectively.
     *
     * @param	privilege	proves the authenticity of this call.
     * @return	<tt>true</tt> if the packet should be sent.
     */
    //请求允许发送数据包。自动加载器可以使用此选项发送？数据包。
    public boolean canSendPacket(Privilege privilege) {
	Lib.assertTrue(privilege == this.privilege,
		   "security violation");
	return true;
    }
    
    /**
     * Request permission to receive a packet. The autograder can use this to
     * drop packets very selectively.
     *
     * @param	privilege	proves the authenticity of this call.
     * @return	<tt>true</tt> if the packet should be delivered to the kernel.
     */
    //请求允许接收数据包。自动加载器可以使用此选项接受？数据包
    public boolean canReceivePacket(Privilege privilege) {
	Lib.assertTrue(privilege == this.privilege,
		   "security violation");
	return true;
    }
    
    private KThread currentThread;
}
