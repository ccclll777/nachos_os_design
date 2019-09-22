// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.security;

import nachos.machine.SerialConsole;
import nachos.machine.Stats;
import nachos.threads.KThread;

import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A capability that allows privileged access to the Nachos machine.
 *
 * <p>
 * Some privileged operations are guarded by the Nachos security manager:
 * <ol>
 * <li>creating threads
 * <li>writing/deleting files in the test directory
 * <li>exit with specific status code
 * </ol>
 * These operations can only be performed through <tt>doPrivileged()</tt>.
 *
 * <p>
 * Some privileged operations require a capability:
 * <ol>
 * <li>scheduling interrupts
 * <li>advancing the simulated time
 * <li>accessing machine statistics
 * <li>installing a console
 * <li>flushing the simulated processor's pipeline
 * <li>approving TCB operations
 * </ol>
 * These operations can be directly performed using a <tt>Privilege</tt>
 * object.
 *
 * <p>
 * The Nachos kernel should <i>never</i> be able to directly perform any of
 * these privileged operations. If you have discovered a loophole somewhere,
 * notify someone.
 */

//*允许对Nachos计算机进行特权访问的功能。
//*一些特权操作由NACHOS安全管理器保护：
//*< OL>
//*<li>创建线程
//*<li>在测试目录中写入/删除文件
//*<li>使用特定状态代码退出
//*</OL>
//*这些操作只能通过<tt>doPrivileged（）</tt>执行。
//*
//*< P>
//*某些特权操作需要一种功能：
//*< OL>
//*<li>调度中断
//*<li>提前模拟时间
//*<li>访问计算机统计信息
//*<li>安装控制台
//*<li>刷新模拟处理器的管道
//*<li>批准TCB操作
//*</OL>
//*可以使用<tt>特权直接执行这些操作
//*对象。
//*
//*< P>
//*Nachos内核应该永远不能直接执行  这些特权操作。如果你在某个地方发现了漏洞，应该发出提示
public abstract class Privilege {
    /**
     * Allocate a new <tt>Privilege</tt> object. Note that this object in
     * itself does not encapsulate privileged access until the machine devices
     * fill it in.
     */
    //分配一个新的<tt>特权<tt>对象。注意这个对象
	// 它本身不封装特权访问  until the machine devices   fill it in.
    public Privilege() {
    }
    
    /**
     * Perform the specified action with privilege.
     *
     * @param	action	the action to perform.
     */
    //使用特权指令执行操作
    public abstract void doPrivileged(Runnable action);

    /**
     * Perform the specified <tt>PrivilegedAction</tt> with privilege.
     *
     * @param	action	the action to perform.   要执行的操作
     * @return	the return value of the action.
     */
    //使用特权指令执行PrivilegedAction操作
    public abstract Object doPrivileged(PrivilegedAction action);

    /**
     * Perform the specified <tt>PrivilegedExceptionAction</tt> with privilege.
     *
     * @param	action	the action to perform.
     * @return	the return value of the action.
     */

    //使用特权指令 执行PrivilegedExceptionAction操作
    public abstract Object doPrivileged(PrivilegedExceptionAction action)
	throws PrivilegedActionException;

    /**
     * Exit Nachos with the specified status.
     *
     * @param	exitStatus	the exit status of the Nachos process.
     */

    //退出具有指定状态的nachos
    public abstract void exit(int exitStatus);

    /**
     * Add an <tt>exit()</tt> notification handler. The handler will be invoked
     * by exit().
     *
     * @param	handler	the notification handler.
     */
    //添加一个通知处理程序到 linklist中  这个处理程序将被exit调用
    public void addExitNotificationHandler(Runnable handler) {
	exitNotificationHandlers.add(handler);
    }

    /**
     * Invoke each <tt>exit()</tt> notification handler added by
     * <tt>addExitNotificationHandler()</tt>. Called by <tt>exit()</tt>.
     */

    //调用每一个 exit的通知程序
    protected void invokeExitNotificationHandlers() {
	for (Iterator i = exitNotificationHandlers.iterator(); i.hasNext(); ) {
	    try {
		((Runnable) i.next()).run();
	    }
	    catch (Throwable e) {
		System.out.println("exit() notification handler failed");
	    }
	}
    }

    private LinkedList<Runnable> exitNotificationHandlers =
        new LinkedList<Runnable>();

    /** Nachos runtime statistics. */
    //nachos的运行状态
    public Stats stats = null;

    /** Provides access to some private <tt>Machine</tt> methods. */
    //提供 使用  <tt>Machine</tt>中私有方法 的权限
    public MachinePrivilege machine = null;
    /** Provides access to some private <tt>Interrupt</tt> methods. */

	//提供 使用  <tt>Interrupt</tt>中私有方法 的权限
    public InterruptPrivilege interrupt = null;
    /** Provides access to some private <tt>Processor</tt> methods. */

	//提供 使用  <tt>Processor</tt>中私有方法 的权限
    public ProcessorPrivilege processor = null;
    /** Provides access to some private <tt>TCB</tt> methods. */

	//提供 使用  <tt>TCB</tt>中私有方法 的权限
    public TCBPrivilege tcb = null;

    /**
     * An interface that provides access to some private <tt>Machine</tt>
     * methods.
     */
    //提供一个接口  可以访问 <tt>Machine</tt>中的某些私有方法
    public interface MachinePrivilege {
	/**
	 * Install a hardware console.
	 *
	 * @param	console	the new hardware console.
	 */
	//安装控制台硬件？？
	public void setConsole(SerialConsole console);
    }

    /**
     * An interface that provides access to some private <tt>Interrupt</tt>
     * methods.
     */
	//提供一个接口  可以访问 <tt>Interrupt</tt>中的某些私有方法
    public interface InterruptPrivilege {
	/**
	 * Schedule an interrupt to occur at some time in the future.
	 *
	 * @param	when	the number of ticks until the interrupt should
	 *			occur.
	 * @param	type	a name for the type of interrupt being
	 *			scheduled.
	 * @param	handler	the interrupt handler to call.
	 */
	//线程要发生什么样的中断
	public void schedule(long when, String type, Runnable handler);
	
	/**
	 * Advance the simulated time.
	 *
	 * @param inKernelMode	<tt>true</tt> if the current thread is running kernel
	 *		code, <tt>false</tt> if the current thread is running
	 *		MIPS user code.
	 */
	//时钟 如果运行 kernel代码-- true   如果当先线程正在运行mips用户代码--false
	public void tick(boolean inKernelMode);
    }

    /**
     * An interface that provides access to some private <tt>Processor</tt>
     * methods.
     */

	//提供一个接口  可以访问 <tt>Processor</tt>中的某些私有方法
    public interface ProcessorPrivilege {
	/**
	 * Flush the processor pipeline in preparation for switching to kernel
	 * mode.
	 */
	//刷新处理器管道以准备切换到内核模式。
	public void flushPipe();
    }

    /**
     * An interface that provides access to some private <tt>TCB</tt> methods.
     */

	//提供一个接口  可以访问 <tt>TCB</tt>中的某些私有方法
    public interface TCBPrivilege {
	/**
	 * Associate the current TCB with the specified <tt>KThread</tt>.
	 * <tt>AutoGrader.runningThread()</tt> <i>must</i> call this method
	 * before returning.
	 *
	 * @param	thread	the current thread.
	 */
	//去当前tcb指定的  <tt>KThread</tt>.相关联
	//在返回之前<tt>AutoGrader.runningThread()</tt>必须调用此方法
	//参数是当前线程
	public void associateThread(KThread thread);
	/**
	 * Authorize the TCB associated with the specified thread to be
	 * destroyed.
	 *
	 * @param	thread	the thread whose TCB is about to be destroyed.
	 */
	//授权  销毁某个指定的 TCB
	public void authorizeDestroy(KThread thread);
    }    
}