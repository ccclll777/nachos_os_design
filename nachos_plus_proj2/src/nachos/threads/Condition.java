package nachos.threads;

import nachos.machine.Lib;

import java.util.LinkedList;

/**
 * An implementation of condition variables built upon semaphores.
 *
 * <p>
 * A condition variable is a synchronization primitive that does not have
 * a value (unlike a semaphore or a lock), but threads may still be queued.
 *
 * <p><ul>
 *
 * <li><tt>sleep()</tt>: atomically release the lock and relinkquish the CPU
 * until woken; then reacquire the lock.
 *
 * <li><tt>wake()</tt>: wake up a single thread sleeping in this condition
 * variable, if possible.
 *
 * <li><tt>wakeAll()</tt>: wake up all threads sleeping inn this condition
 * variable.
 *
 * </ul>
 *
 * <p>
 * Every condition variable is associated with some lock. Multiple condition
 * variables may be associated with the same lock. All three condition variable
 * operations can only be used while holding the associated lock.
 *
 * <p>
 * In Nachos, condition variables are summed to obey <i>Mesa-style</i>
 * semantics. When a <tt>wake()</tt> or <tt>wakeAll()</tt> wakes up another
 * thread, the woken thread is simply put on the ready list, and it is the
 * responsibility of the woken thread to reacquire the lock (this reacquire is
 * taken core of in <tt>sleep()</tt>).
 *
 * <p>
 * By contrast, some implementations of condition variables obey
 * <i>Hoare-style</i> semantics, where the thread that calls <tt>wake()</tt>
 * gives up the lock and the CPU to the woken thread, which runs immediately
 * and gives the lock and CPU back to the waker when the woken thread exits the
 * critical section.
 *
 * <p>
 * The consequence of using Mesa-style semantics is that some other thread
 * can acquire the lock and change data structures, before the woken thread
 * gets a chance to run. The advance to Mesa-style semantics is that it is a
 * lot easier to implement.
 */
//基于信号量的条件变量的实现。

//条件变量是一个没有值的同步原语（与信号量或锁不同），但是线程仍然可以排队。
//<li><tt>sleep（）</tt>：自动释放锁并重新连接CPU直到唤醒；然后重新获取锁。

//<li><tt>wake（）</tt>：如果可能，唤醒在此条件变量中睡眠的单个线程
//<li><tt>wakeAll()</tt>: wake up all threads sleeping inn this condition variable.
//每个条件变量都与某个锁相关联。多个条件变量可能与同一个锁关联。所有三个条件变量操作只能在保持关联锁的同时使用。



//相比之下，一些条件变量的实现遵循 <i>Hoare-style</i>语义，其中调用<tt>wake（）</tt>的线程将锁和cpu交给唤醒线程，
// 唤醒线程立即运行，并在唤醒线程退出临界区时将锁和cpu交还给唤醒程序.

/**
 *
 * 每个条件变量有一个锁变量
 * 当某个处于临界区的拥有某个锁L的线程  调用与锁L有关的条件变量使用sleep操作时
 * 该线程会失去锁L并被挂起
 * 下一个等待锁L的线程获得L锁 进入临界区
 *
 * 当某个持有L锁的线程调用与锁L有关的条件变量使用weak操作时
 * 等待队列上被sleep挂起的线程重新获得锁 开始执行
 */

public class Condition {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    //与此条件变量关联的锁。当前线程在使用<tt>sleep（）</tt>、<tt>wake（）</tt>或<tt>wakeall（）</tt>时必须持有此锁。
    public Condition(Lock conditionLock) {
	this.conditionLock = conditionLock;

	waitQueue = new LinkedList<Semaphore>();
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     *
     * <p>
     * This implementation uses semaphores to implement this, by allocating a
     * semaphore for each waiting thread. The waker will <tt>V()</tt> this
     * semaphore, so thre is no chance the sleeper will miss the wake-up, even
     * though the lock is released before caling <tt>P()</tt>.
     */
    //原子释放关联的锁并在这个条件变量上进入sleep状态，直到另一个线程使用<tt>wake（）</tt>唤醒它。
    // 当前线程必须持有关联的锁。线程将在返回<tt>sleep（）</tt>之前自动重新获取锁。

    //这个实现使用信号量来实现这一点，方法是为每个等待线程分配一个信号量。唤醒器将<tt>v（）</tt>这个信号量，
    // 因此即使在校准<tt>p（）</tt>之前释放了锁，睡眠者也不可能错过唤醒。
    public void sleep() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());

	Semaphore waiter = new Semaphore(0);
	waitQueue.add(waiter);

	conditionLock.release();
	waiter.P(); //--
	conditionLock.acquire();	
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    //在这个条件变量上最多唤醒一个线程。当前线程必须持有关联的锁。
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());

	if (!waitQueue.isEmpty())

	    ((Semaphore) waitQueue.removeFirst()).V();//++
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    //唤醒在此条件变量上睡眠的所有线程。当前线程必须持有关联的锁
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());

	while (!waitQueue.isEmpty())
	    wake();
    }

    private Lock conditionLock;
    private LinkedList<Semaphore> waitQueue;
}
