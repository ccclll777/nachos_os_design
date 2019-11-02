package nachos.threads;

/**
 * Schedules access to some sort of resource with limited access constraints. A
 * thread queue can be used to share this limited access among multiple
 * threads.
 *
 * <p>
 * Examples of limited access in Nachos include:
 *
 * <ol>
 * <li>the right for a thread to use the processor. Only one thread may run on
 * the processor at a time.
 *
 * <li>the right for a thread to acquire a specific lock. A lock may be held by
 * only one thread at a time.
 *
 * <li>the right for a thread to return from <tt>Semaphore.P()</tt> when the
 * semaphore is 0. When another thread calls <tt>Semaphore.V()</tt>, only one
 * thread waiting in <tt>Semaphore.P()</tt> can be awakened.
 *
 * <li>the right for a thread to be woken while sleeping on a condition
 * variable. When another thread calls <tt>Condition.wake()</tt>, only one
 * thread sleeping on the condition variable can be awakened.
 *
 * <li>the right for a thread to return from <tt>KThread.join()</tt>. Threads
 * are not allowed to return from <tt>join()</tt> until the target thread has
 * finished.
 * </ol>
 *
 * All these cases involve limited access because, for each of them, it is not
 * necessarily possible (or correct) for all the threads to have simultaneous
 * access. Some of these cases involve concrete resources (e.g. the processor,
 * or a lock); others are more abstract (e.g. waiting on semaphores, condition
 * variables, or join).
 *
 * <p>
 * All thread queue methods must be invoked with <b>interrupts disabled</b>.
 */
//安排对某种具有有限访问限制的资源的访问。线程队列可用于在多个线程之间共享此有限访问。
//nachos中的有限访问示例包括：
//线程使用处理器的权限————  一次只能在处理器上运行一个线程。
//线程获取特定锁的权利————  一次只能由一个线程持有一个锁
//当信号量为0时，线程从<tt>semaphore.p（）</tt>返回的权限。当另一个线程调用simaphore.v（）</tt>时，只能唤醒在simaphore.p（）</tt>中等待的一个线程。
//在临界区  上sleep时线程被唤醒的权利———— 当另一个线程调用condition.wake（）</tt>时，只能唤醒sleep在 condition 上的一个线程。
//线程从<tt>kthread.join（）</tt>返回的权限 ———— 在目标线程完成之前，不允许线程从<tt>join（）</tt>返回。
public abstract class ThreadQueue {
    /**
     * Notify this thread queue that the specified thread is waiting for
     * access. This method should only be called if the thread cannot
     * immediately obtain access (e.g. if the thread wants to acquire a lock
     * but another thread already holds the lock).
     *
     * <p>
     * A thread must not simultaneously wait for access to multiple resources.
     * For example, a thread waiting for a lock must not also be waiting to run
     * on the processor; if a thread is waiting for a lock it should be
     * sleeping.
     *
     * <p>
     * However, depending on the specific objects, it may be acceptable for a
     * thread to wait for access to one object while having access to another.
     * For example, a thread may attempt to acquire a lock while holding
     * another lock. Note, though, that the processor cannot be held while
     * waiting for access to anything else.
     *
     * @param	thread	the thread waiting for access.
     */
    //此线程队列指定的线程正在等待调用
    //只有当线程无法立即获得访问权限时（例如，如果线程想要获取锁，但另一个线程已经持有锁），才应调用此方法。
    //线程不能同时等待对多个资源的访问。
    //例如，等待锁的线程不能同时等待在处理器上运行；如果线程正在等待锁，则它应该处于sleep状态。
    //但是，根据具体的对象，可以接受
    //线程在访问一个对象时等待对另一个对象的访问  例如，一个线程可能试图在保持的同时获取一个锁
    //另一把锁  但是处理器不能 等待获得其他线程持有锁的线程
    public abstract void waitForAccess(KThread thread);

    /**
     * Notify this thread queue that another thread can receive access. Choose
     * and return the next thread to receive access, or <tt>null</tt> if there
     * are no threads waiting.
     *
     * <p>
     * If the limited access object transfers priority, and if there are other
     * threads waiting for access, then they will donate priority to the
     * returned thread.
     *
     * @return	the next thread to receive access, or <tt>null</tt> if there
     *		are no threads waiting.
     */
    //通知此线程队列另一个线程可以被调用。选择
    //并返回下一个线程以接收访问，如果没有线程在等待，则返回<tt>null</tt>。
    //如果有限制访问的对象转移优先权  and 如果有对象在等待调用  那么那个限制访问的对象将会把自己的优先级给返回的对象
    public abstract KThread nextThread();

    /**
     * Notify this thread queue that a thread has received access, without
     * going through <tt>request()</tt> and <tt>nextThread()</tt>. For example,
     * if a thread acquires a lock that no other threads are waiting for, it
     * should call this method.
     *
     * <p>
     * This method should not be called for a thread returned from
     * <tt>nextThread()</tt>.
     *
     * @param	thread	the thread that has received access, but was not
     * 			returned from <tt>nextThread()</tt>.
     */

    //通知此线程队列某个线程已接收到访问，而没有
    //通过<tt>request（）</tt>和<tt>nextThread（）</tt>。
    //例如，如果一个线程获得了一个没有其他线程等待的锁，那么
    //应该调用此方法。
    //要按顺序返回线程 不应该调用方法
    //返回值为  可以访问临界区 但是没有被<tt>nextThread()</tt>.调用到的线程
    public abstract void acquire(KThread thread);

    /**
     * Print out all the threads waiting for access, in no particular order.
     */
    //打印出所有等待访问的线程，没有特定的顺序。
    public abstract void print();
}
