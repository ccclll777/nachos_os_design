package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */

//优先级调度的⼀一个问题是优先级反转。如果⾼高优先级线程需要等待低优先级线程
// (例例如，低优先级线程持 有的锁)，⽽而另⼀一个⾼高优先级线程在就绪列列表中，
// 则⾼高优先级线程将永远⽆无法获得CPU，因为低优先级线程 将不不会获得任何CPU时间。
// 此问题的部分解决⽅方法是让等待线程在保持锁的同时将其优先级捐赠给低优先级 线程。
// 实现优先级调度程序，以便便在可能的情况下提供优先级。请确保实现scheduler.geteffectivepriority()，
// 它 在考虑线程接收的所有捐赠后返回线程的优先级。
//如果有相同 优先权的在队列列中 需要寻找⼀一个 等待时间最久的

/**
 * task5 *当A直接拥有B所需的资源是时，将线程A看作线程B的⽗父线程，则⽤用holdThread表示⽗父线程 * waitThread表示⼦子线程
 * waitQueue表示祖先
 * holdQueue代表后代
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }

    /**
     * Allocate a new priority thread queue.
     *
     * @param    transferPriority    <tt>true</tt> if this queue should
     * transfer priority from waiting threads
     * to the owning thread.
     * @return a new priority thread queue.
     */
    //构造⼀一个优先级队列列 如果transferPriority为true则表示可以传输优先级
    public ThreadQueue newThreadQueue(boolean transferPriority) {

        return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());

        Lib.assertTrue(priority >= priorityMinimum &&
                priority <= priorityMaximum);

        getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMaximum)
            return false;

        setPriority(thread, priority + 1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMinimum)
            return false;

        setPriority(thread, priority - 1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param    thread    the thread whose scheduling state to return.
     * @return the scheduling state of the specified thread.
     */

    // 返回线程的调度状态
    protected ThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new ThreadState(thread);

        return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    //按优先级对线程排序的线程队列列。
    protected class PriorityQueue extends ThreadQueue {
        PriorityQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
        }

        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).waitForAccess(this);
        }

        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).acquire(this);
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me
            ThreadState temp = pickNextThread();
            if (temp == null)
                return null;
            else {
                return temp.thread;
            }
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return the next thread that <tt>nextThread()</tt> would
         * return.
         */
        //返回下⼀一个<tt>nextThread()</tt>将返回的线程，⽽而不不修改此队列列的状态。
        protected ThreadState pickNextThread() {
            // implement me
            //取出⼀一个优先级最	高的线程
            ThreadState res = NextThread();
            if (holdThread != null) {
                holdThread.WaitResourceQueues.remove(this);
                holdThread.getEffectivePriority();
                holdThread = res;
            }
            if (res != null)
                res.holdResourceQueue = null;
            return res;
        }

        protected ThreadState NextThread() { // //将等待此线程资源的其他线程返回⼀一个 优先级最⾼高的
            ThreadState res;
            res = waitResourceThreadsSet.pollLast();
            return res;
        }

        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me (if you want)
        }

        /**
         * <tt>true</tt> if this queue should transfer priority from waiting
         * threads to the owning thread.
         */
        //添加 等待(此线程拥有资源) 的线程
        public void add(ThreadState thread) {

            waitResourceThreadsSet.add(thread);
        }

        public boolean isEmpty() { //
            return waitResourceThreadsSet.isEmpty();
        }

        //<tt>true</tt>如果此队列列应将优先级从等待线程传输到所属线程
        public boolean transferPriority;
        protected long cnt = 0; //
        protected TreeSet<ThreadState> waitResourceThreadsSet = new TreeSet<ThreadState>(); //等待线程资源的 其他线程?
        protected ThreadState holdThread = null;//拥有资源的线程  代表持有他资源的线程

    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see    nachos.threads.KThread#schedulingState
     */
    protected class ThreadState implements Comparable<ThreadState> {
        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param    thread    the thread this state belongs to.
         */
        public ThreadState(KThread thread) {
            this.thread = thread;

            setPriority(priorityDefault);
            getEffectivePriority();//获取有效优先级
        }

        /**
         * Return the priority of the associated thread.
         *
         * @return the priority of the associated thread.
         */
        public int getPriority() {
            return priority;
        }

        /**
         * Return the effective priority of the associated thread.
         *
         * @return the effective priority of the associated thread.
         */
        public int getEffectivePriority() {
            // implement me
            int temp1 = this.priority;
            if (!WaitResourceQueues.isEmpty()) {
                Iterator<PriorityQueue> priorityQueueIterable = WaitResourceQueues.iterator();
                while (priorityQueueIterable.hasNext()) {
                    PriorityQueue priorityQueue = priorityQueueIterable.next();
                    //如果等待它拥有的资源的线程中 有优先级⽐比较⼤大的线程
                    int maxPriority = 0;
                    if (priorityQueue.waitResourceThreadsSet.isEmpty() == false) {
                        maxPriority = priorityQueue.waitResourceThreadsSet.last().effectivePriority;
                    }
                    if (maxPriority > temp1) {
                        temp1 = maxPriority;
                    }
                }
            }
            if (holdResourceQueue != null && temp1 != effectivePriority) {
                //修改 拥有资源线程中原有的优先级
                holdResourceQueue.waitResourceThreadsSet.remove(this);
                this.effectivePriority = temp1;
                holdResourceQueue.waitResourceThreadsSet.add(this);
            }
            if (holdThread != null)
                holdThread.getEffectivePriority();
            return effectivePriority;
        }

        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param    priority    the new priority.
         */
        public void setPriority(int priority) {
            if (this.priority == priority)
                return;

            this.priority = priority;

            // implement me
            getEffectivePriority();
        }

        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified priority queue.
         * The associated thread is therefore waiting for access to the
         * resource guarded by <tt>waitQueue</tt>. This method is only called
         * if the associated thread cannot immediately obtain access.
         *
         * @param    waitQueue    the queue that the associated thread is
         * now waiting on.
         * @see    nachos.threads.ThreadQueue#waitForAccess
         */
        //在指定的优先级队列列上调⽤用waitforaccess(thread)</tt>时调⽤用(其中<tt>thread</tt>是关联的线 程)。
        // 因此，关联的线程正在等待对由waitqueue保护的资源的访问。仅当关联的线程⽆无法⽴立即获得访问权限时才调⽤用此⽅方法。
        //此线程队列列指定的线程正在等待调⽤用 将需要等待获得资源de 线程j加⼊入等待队列列等待调度
        public void waitForAccess(PriorityQueue waitQueue) {
            // implement me
            Lib.assertTrue(Machine.interrupt().disabled());
            //waitQueue拥有资源 等待调⽤用 优先级可能不不够⾼高
            time = ++waitQueue.cnt;
            this.holdResourceQueue = waitQueue;
            //this 此线程 等待资源 但是有优先级 所以添加到waitQueue的等待资源的列列表中 waitQueue.add(this);
//此线程需要的资源所属的线程 变成了了waitQueue需要的资源所属的线程 由于要进⾏行行优先权的转让
            holdThread = waitQueue.holdThread;
            getEffectivePriority();//进⾏行行优先权的交换
        }

        /**
         * Called when the associated thread has acquired access to whatever is
         * guarded by <tt>waitQueue</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
         *
         * @see    nachos.threads.ThreadQueue#acquire
         * @see    nachos.threads.ThreadQueue#nextThread
         */
        //线程队列列某个线程已接收到访问 但是还不不能执⾏行行 由于没有资源
        public void acquire(PriorityQueue waitQueue) {
            // implement me
            Lib.assertTrue(Machine.interrupt().disabled());
            //如果waitQueue可以转让优先权
            if (waitQueue.transferPriority) {
                //将它加⼊入 等待此线程资源的队列列中
                WaitResourceQueues.add(waitQueue);
            }
            Lib.assertTrue(waitQueue.isEmpty());
        }

        /**
         * The thread with which this object is associated.
         */
        //关联线程
        protected KThread thread;
        /**
         * The priority of the associated thread.
         */
        //关联线程的优先级
        protected int priority;

        //有效优先级
        protected int effectivePriority;
        protected Long time;//等待时间 ()
        protected PriorityQueue holdResourceQueue = null; //表示 祖先线程
        protected ArrayList<PriorityQueue> WaitResourceQueues = new ArrayList<PriorityQueue>();// 表示 ⼦子孙线程 等待 它拥有资源的线程队列
        protected ThreadState holdThread = null; //⽗父线程 代表持有资源的线程 拥有它需要资源的线程

        //实现Comparator接⼝口，并重写compare()⽅方法， @Override
        public int compareTo(ThreadState target) {
            if (this.effectivePriority > target.effectivePriority)
                return 1;
            else if (this.effectivePriority < target.effectivePriority)
                return -1;
            else {
//当优先级相同时 选择⼀一个等待时间最久的
                if (this.time > target.time)
                    return 1;
                if (this.time < target.time)
                    return -1;
                return 0;
            }
        }

    }
}
