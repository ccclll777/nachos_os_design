package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;

/**
 * A <tt>Lock</tt> is a synchronization primitive that has two states,
 * <i>busy</i> and <i>free</i>. There are only two operations allowed on a
 * lock:
 *
 * <ul>
 * <li><tt>acquire()</tt>: atomically wait until the lock is <i>free</i> and
 * then set it to <i>busy</i>.
 * <li><tt>release()</tt>: set the lock to be <i>free</i>, waking up one
 * waiting thread if possible.
 * </ul>
 *
 * <p>
 * Also, only the thread that acquired a lock may release it. As with
 * semaphores, the API does not allow you to read the lock state (because the
 * value could change immediately after you read it).
 */

//a<tt>lock</tt>是一个同步原语，它有两种状态<i>busy</i>和<i>free</i>。一个锁只允许两个操作：

//<li><tt>acquire（）</tt>：自动等待锁被释放，然后将其设置为<i>busy</i>。
// <li><tt>release（）</tt>：将锁设置为<i>free</i>，尽可能唤醒一个等待线程


//另外，只有获取锁的线程才能释放它。与信号量一样，api不允许您读取锁状态（因为该值可能在您读取后立即更改）。
public class Lock {
    /**
     * Allocate a new lock. The lock will initially be <i>free</i>.
     */
    public Lock() {
    }

    /**
     * Atomically acquire this lock. The current thread must not already hold
     * this lock.
     */
    //原子地获得这个锁。当前线程不能已经持有此锁。
    public void acquire() {
	Lib.assertTrue(!isHeldByCurrentThread());

	boolean intStatus = Machine.interrupt().disable();
	KThread thread = KThread.currentThread();

	if (lockHolder != null) {
		//等待锁
	    waitQueue.waitForAccess(thread);
	    KThread.sleep();
	}
	else {
		//获得锁
	    waitQueue.acquire(thread);
	    lockHolder = thread;
	}

	Lib.assertTrue(lockHolder == thread);

	Machine.interrupt().restore(intStatus);
    }

    /**
     * Atomically release this lock, allowing other threads to acquire it.
     */

    //释放锁
    public void release() {
	Lib.assertTrue(isHeldByCurrentThread());

	boolean intStatus = Machine.interrupt().disable();

	if ((lockHolder = waitQueue.nextThread()) != null)
	    lockHolder.ready();
	
	Machine.interrupt().restore(intStatus);
    }

    /**
     * Test if the current thread holds this lock.
     *
     * @return	true if the current thread holds this lock.
     */

    //测试当前线程是否持有此锁
    public boolean isHeldByCurrentThread() {
	return (lockHolder == KThread.currentThread());
    }

    private KThread lockHolder = null;
    private ThreadQueue waitQueue =
	ThreadedKernel.scheduler.newThreadQueue(true);
}
