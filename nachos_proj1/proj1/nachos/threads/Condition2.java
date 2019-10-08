package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	Condition
 */

//一种用于同步的条件变量的实现

    //通过使用中断启用和禁用来提供原子性，直接实现条件变量。我们提供了一个使用信号量的示例实现；您的工作是提供一个等效的实现，
// 而不直接使用信号量（当然，您可能仍然使用锁，即使它们间接使用信号量）。
// 完成后，您将有两个替代实现，它们提供完全相同的功能。条件变量的第二个实现必须位于classnachos.threads.condition2中。

/**
 *
 * task2
 *
 *
 *
 * 如果调用weak（）则会有一个等待队列的条件变量被唤醒 获得锁
 *
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {

        this.conditionLock = conditionLock;
        waitQueue = ThreadedKernel.scheduler.newThreadQueue(true);
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    //原子释放关联的锁并在这个条件变量上进入睡眠状态，直到另一个线程使用<tt>wake（）</tt>唤醒它
    // 。当前线程必须持有关联的锁。线程将在返回<tt>sleep（）</tt>之前自动重新获取锁。
    public void sleep() {
        //关中断
        boolean InterruptStatus = Machine.interrupt().disable();
        //如果当前线程没有持有锁
	    Lib.assertTrue(conditionLock.isHeldByCurrentThread());

	//将线程添加到等待队列
        waitQueue.waitForAccess(KThread.currentThread());

        //在条件变量的控制下 进入sleep函数 释放自己的锁 进入睡眠 当有线程wake他时， 在去获得锁
        //阻塞的线程在sleep函数返回前   将继续拥有锁
        //调用这个方法的线程将自己挂起到等待队列  阻塞自己的同时将自己拥有的锁交出去  之后等待唤醒 重新拥有锁
	conditionLock.release();//进程将锁释放
        KThread.sleep();
	conditionLock.acquire();//线程唤醒之后  处于就绪队列 将获得锁
        Machine.interrupt().restore(InterruptStatus);
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    //在这个条件变量上最多唤醒一个线程。当前线程必须持有关联的锁
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        boolean InterruptStatus = Machine.interrupt().disable();

        KThread thread = waitQueue.nextThread();
        if(thread != null) {
            thread.ready();
        }

        Machine.interrupt().restore(InterruptStatus);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    //唤醒在此条件变量上睡眠的所有线程。当前线程必须持有关联的锁
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean InterruptStatus = Machine.interrupt().disable();


        KThread thread = null;
        do {
            thread = waitQueue.nextThread();
            if(thread != null) {
                thread.ready();
            }
        } while(thread != null);
        Machine.interrupt().restore(InterruptStatus);
    }

    private Lock conditionLock;

    // 需要一个等待队列
    private ThreadQueue waitQueue =null;

}
