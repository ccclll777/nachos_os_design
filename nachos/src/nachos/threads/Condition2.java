package nachos.threads;

import nachos.machine.Lib;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see    Condition
 */

//一种用于同步的条件变量的实现
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
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());

	conditionLock.release();

	conditionLock.acquire();
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    //在这个条件变量上最多唤醒一个线程。当前线程必须持有关联的锁
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    //唤醒在此条件变量上睡眠的所有线程。当前线程必须持有关联的锁
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    }

    private Lock conditionLock;
}
