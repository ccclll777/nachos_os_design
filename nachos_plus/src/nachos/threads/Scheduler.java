package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;

/**
 * Coordinates a group of thread queues of the same kind.
 *
 * @see    ThreadQueue
 */

//创建决定下一个运行哪个线程的 threadqueue对象。

    //抽象基类nachos.threads.scheduler的一个子类（在nachos.conf中指定）负责为所有被限制的资源调度线程，
// 无论是CPU、锁之类的同步构造，甚至是线程连接操作。对于每个资源，scheduler.newthreadqueue（）
// 将创建一个nachos.threads.threadqueue。资源的实现
//信号量类）负责将kthreads添加到threadqueue（threadqueue.waitforaccess（））中，
// 并请求threadqueue返回下一个线程（threadqueue.next thread（））。
// 因此，所有的调度决策（包括那些关于cpu的1就绪队列的决策）减少到thread queue对象选择下一个线程。


    //项目的各个阶段都需要修改调度器基类。默认情况下是nachos.threads.roundrobinscheduler，它实现了一个全功能（尽管很幼稚）的fifo调度程序。
// 项目的第一阶段要求学生完成nachos.threads.priorityscheduler；第二阶段要求学生完成nachos.threads.lotteryscheduler。
public abstract class Scheduler {
    /**
     * Allocate a new scheduler.
     */
    public Scheduler() {
    }
    
    /**
     * Allocate a new thread queue. If <i>transferPriority</i> is
     * <tt>true</tt>, then threads waiting on the new queue will transfer their
     * "priority" to the thread that has access to whatever is being guarded by
     * the queue. This is the mechanism used to partially solve priority
     * inversion.
     *
     * <p>
     * If there is no definite thread that can be said to have "access" (as in
     * the case of semaphores and condition variables), this parameter should
     * be <tt>false</tt>, indicating that no priority should be transferred.
     *
     * <p>
     * The processor is a special case. There is clearly no purpose to donating
     * priority to a thread that already has the processor. When the processor
     * wait queue is created, this parameter should be <tt>false</tt>.
     *
     * <p>
     * Otherwise, it is beneficial to donate priority. For example, a lock has
     * a definite owner (the thread that holds the lock), and a lock is always
     * released by the same thread that acquired it, so it is possible to help
     * a high priority thread waiting for a lock by donating its priority to
     * the thread holding the lock. Therefore, a queue for a lock should be
     * created with this parameter set to <tt>true</tt>.
     *
     * <p>
     * Similarly, when a thread is asleep in <tt>join()</tt> waiting for the
     * target thread to finish, the sleeping thread should donate its priority
     * to the target thread. Therefore, a join queue should be created with
     * this parameter set to <tt>true</tt>.
     *
     * @param	transferPriority	<tt>true</tt> if the thread that has
     *					access should receive priority from the
     *					threads that are waiting on this queue.
     * @return	a new thread queue.
     */
    //分配新的线程队列。如果<i>transferPriority</i>是
    //<tt>true</tt>，
    //等待新队列的线程将把它们的“优先级”转移到有权访问队列保护的任何内容的线程。  这是用于部分解决优先级反转的机制。

    //如果没有可以说具有“访问”权限的确定线程（如信号量和条件变量的情况），则此参数应为<tt>false</tt>，表示不应传输优先级。

    //process是一个特例   显然，将优先级捐赠给已经拥有处理器的线程是没有意义的。创建处理器等待队列时，此参数应为<tt>false</tt>。

    //如果一个持有锁的线程 并且锁总是
    //由获取锁的同一线程释放，因此可以通过将其优先级donate给持有锁的线程来帮助等待锁的高优先级线程
    //因此，应该使用此参数设置为<tt>true</tt>来创建锁队列。
    public abstract ThreadQueue newThreadQueue(boolean transferPriority);

    /**
     * Get the priority of the specified thread. Must be called with
     * interrupts disabled.
     *
     * @param	thread	the thread to get the priority of.
     * @return	the thread's priority.
     */
    //获取指定线程的优先级。必须在中断被禁用的情况下调用。
    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
	return 0;
    }

    /**
     * Get the priority of the current thread. Equivalent to
     * <tt>getPriority(KThread.currentThread())</tt>.
     *
     * @return	the current thread's priority.
     */
    //获取当前线程的优先级。相当于<tt>getPriority（kthread.currentThread（））</tt>。
    public int getPriority() {
	return getPriority(KThread.currentThread());
    }

    /**
     * Get the effective priority of the specified thread. Must be called with
     * interrupts disabled.
     *
     * <p>
     * The effective priority of a thread is the priority of a thread after
     * taking into account priority donations.
     *
     * <p>
     * For a priority scheduler, this is the maximum of the thread's priority
     * and the priorities of all other threads waiting for the thread through a
     * lock or a join.
     *
     * <p>
     * For a lottery scheduler, this is the sum of the thread's tickets and the
     * tickets of all other threads waiting for the thread through a lock or a
     * join.
     *
     * @param	thread	the thread to get the effective priority of.
     * @return	the thread's effective priority.
     */
    //获取指定线程的有效优先级。必须在中断被禁用的情况下调用。
    //线程的有效优先级是线程在考虑优先级donate之后的优先级。
    //对于优先级调度程序，这是线程优先级的最大值
    //以及通过锁或连接等待线程的所有其他线程的优先级。


    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
	return 0;
    }

    /**
     * Get the effective priority of the current thread. Equivalent to
     * <tt>getEffectivePriority(KThread.currentThread())</tt>.
     *
     * @return	the current thread's priority.
     */

    //获取线程的有效优先级
    public int getEffectivePriority() {
	return getEffectivePriority(KThread.currentThread());
    }

    /**
     * Set the priority of the specified thread. Must be called with interrupts
     * disabled.
     *
     * @param	thread	the thread to set the priority of.
     * @param	priority	the new priority.
     */

    //设置指定线程的优先级。必须在中断被禁用的情况下调用。
    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());
    }

    /**
     * Set the priority of the current thread. Equivalent to
     * <tt>setPriority(KThread.currentThread(), priority)</tt>.
     *
     * @param	priority	the new priority.
     */
    //设置线程的优先级 <tt>setPriority(KThread.currentThread(), priority)</tt>.
    public void setPriority(int priority) {
	setPriority(KThread.currentThread(), priority);
    }

    /**
     * If possible, raise the priority of the current thread in some
     * scheduler-dependent way.
     *
     * @return	<tt>true</tt> if the scheduler was able to increase the current
     *		thread's
     *		priority.
     */
    //如果可能，请以某种依赖于调度程序的方式提高当前线程的优先级。
    public boolean increasePriority() {
	return false;
    }

    /**
     * If possible, lower the priority of the current thread user in some
     * scheduler-dependent way, preferably by the same amount as would a call
     * to <tt>increasePriority()</tt>.
     *
     * @return	<tt>true</tt> if the scheduler was able to decrease the current
     *		thread's priority.
     */
    //如果可能的话，以某种依赖于调度程序的方式降低当前线程用户的优先级，最好与调用<tt>increasepriority（）</tt>的优先级相同。
    public boolean decreasePriority() {
	return false;
    }
}
