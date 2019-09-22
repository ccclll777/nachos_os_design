package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
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
     * @param transferPriority <tt>true</tt> if this queue should
     *                         transfer priority from waiting threads
     *                         to the owning thread.
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
     * @param thread the thread whose scheduling state to return.
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
            waitResourceThreadsSet = new TreeSet[priorityMaximum + 1];
            for (int i = 0; i <= priorityMaximum; i++)
                waitResourceThreadsSet[i] = new TreeSet<ThreadState>();

        }

        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).waitForAccess(this);
        }

        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).acquire(this);
            if (transferPriority) {
                holdThread = getThreadState(thread);
            }
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me
            ThreadState temp = pickNextThread();
            if (temp == null) {
                return null;
            } else {
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
            //取出⼀一个优先级最	高的线程;
            ThreadState res = NextThread();
            if (holdThread != null) {
                holdThread.WaitResourceQueues.remove(this);
                holdThread.getEffectivePriority();
                holdThread = res;
            }
            return res;
        }

        protected ThreadState NextThread() { // //将等待此线程资源的其他线程返回⼀一个 优先级最高的
            ThreadState res = null;
            for (int i = priorityMaximum; i >= priorityMinimum; i--) {
                res = waitResourceThreadsSet[i].pollFirst();
                if (res != null) {
                    break;
                }
            }

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
            waitResourceThreadsSet[thread.effectivePriority].add(thread);
        }

        public boolean isEmpty() { //
            for (int i = 0; i <= priorityMaximum; i++)
                if (!waitResourceThreadsSet[i].isEmpty()) {
                    return false;
                }

            return true;
        }

        //<tt>true</tt>如果此队列列应将优先级从等待线程传输到所属线程
        protected long cnt = 0;
        public boolean transferPriority;
        //        protected TreeSet<ThreadState> waitResourceThreadsSet = new TreeSet<ThreadState>(); //等待线程资源的 其他线程?
        protected TreeSet<ThreadState>[] waitResourceThreadsSet; //等待此队列锁的其他线程 可以让出自己的优先级
        protected ThreadState holdThread = null;//此队列中 拥有锁的线程

    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see KThread#schedulingState
     */
    protected class ThreadState implements Comparable<ThreadState> {
        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param thread the thread this state belongs to.
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
                    for (int i = priorityMaximum; i > temp1; i--)
                        if (!priorityQueue.waitResourceThreadsSet[i].isEmpty()) {
                            temp1 = i;
                            break;
                        }

                }
            }

            //重新计算此线程正在等待的线程 修改自己所处等待队列中 自己的优先级
            if (waitQueue != null && temp1 != effectivePriority) {
                waitQueue.waitResourceThreadsSet[effectivePriority].remove(this);
                waitQueue.waitResourceThreadsSet[temp1].add(this);
            }
            this.effectivePriority = temp1;
            if (holdLockThread != null) {
                //让拥有锁的线程  获取自己的有效优先级 看能不能提前执行
                holdLockThread.getEffectivePriority();
            }

            return temp1;
        }

        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param priority the new priority.
         */
        public void setPriority(int priority) {
            if (this.priority == priority)
                return;

            this.priority = priority;
            // implement me
            this.getEffectivePriority();

        }

        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified priority queue.
         * The associated thread is therefore waiting for access to the
         * resource guarded by <tt>waitQueue</tt>. This method is only called
         * if the associated thread cannot immediately obtain access.
         *
         * @param waitQueue the queue that the associated thread is
         *                  now waiting on.
         * @see ThreadQueue#waitForAccess
         */
        //在指定的优先级队列列上调⽤用waitforaccess(thread)</tt>时调⽤用(其中<tt>thread</tt>是关联的线 程)。
        // 因此，关联的线程正在等待对由waitqueue保护的资源的访问。仅当关联的线程⽆无法⽴立即获得访问权限时才调⽤用此⽅方法。
        //此线程队列列指定的线程正在等待调⽤用 将需要等待获得资源de 线程j加⼊入等待队列列等待调度
        public void waitForAccess(PriorityQueue waitQueue) {
            // implement me
            Lib.assertTrue(Machine.interrupt().disabled());
            //waitQueue拥有资源 等待调⽤用 优先级可能不不够⾼高
            waitQueue.cnt++;
            this.time = waitQueue.cnt;
            //将此线程 加入 自己等待队列的等待资源的 数据结构中
            waitQueue.waitResourceThreadsSet[this.effectivePriority].add(this);
            //此线程需要的资源  被等待队列中那个 拥有锁的线程 拿着
            this.holdLockThread = waitQueue.holdThread;
            //this 此线程 等待资源 但是有优先级 所以添加到waitQueue的等待资源的列列表中 waitQueue.add(this);
//此线程需要的资源所属的线程 变成了了waitQueue需要的资源所属的线程 由于要进⾏行行优先权的转让
            //获取自己的有效优先权
            this.getEffectivePriority();//进⾏优先权的交换
        }

        /**
         * Called when the associated thread has acquired access to whatever is
         * guarded by <tt>waitQueue</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
         *
         * @see ThreadQueue#acquire
         * @see ThreadQueue#nextThread
         */
        //线程队列列某个线程已接收到访问 但是还不不能执⾏行行 由于没有资源
        public void acquire(PriorityQueue waitQueue) {
            // implement me
            if (waitQueue.transferPriority)
            {
                //此线程开始等待waitQueue拥有的资源  想要获取waitQueue中 持有锁的进程的锁
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
        protected int priority = 1;

        //有效优先级
        protected int effectivePriority = 1;
        protected Long time = Machine.timer().getTime();
        ;//等待时间
        protected PriorityQueue waitQueue = null; //表示 所处的等待队列
        protected LinkedList<PriorityQueue> WaitResourceQueues = new LinkedList<PriorityQueue>();// 表示 ⼦子孙线程 等待 它拥有资源的线程队列
        protected ThreadState holdLockThread = null; //该线程等待队列中 拥有锁的线程

        //实现Comparator接⼝口，并重写compare()⽅方法， @Override
        public int compareTo(ThreadState target) {
            if(time==target.time) return 0;
            return time>target.time?1:-1;
        }
//        public int compareTo(ThreadState target) {
//            if (this.effectivePriority > target.effectivePriority)
//                return 1;
//            else if (this.effectivePriority < target.effectivePriority)
//                return -1;
//            else {
////当优先级相同时 选择⼀一个等待时间最久的
//                if (this.time > target.time)
//                    return 1;
//                if (this.time < target.time)
//                    return -1;
//                return 0;
//            }
//        }

    }

    //    public static void selfTest() {
////        PrioritySchedulerTest.runTest();
//        System.out.println("---------PriorityScheduler Test---------");
//        PriorityScheduler ps = new PriorityScheduler();
//        ThreadQueue queue1 = ps.newThreadQueue(false);
//        ThreadQueue queue2 = ps.newThreadQueue(false);
//        ThreadQueue queue3 = ps.newThreadQueue(true);
//        final Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//                System.out.println("111");
//            }
//        };
//        KThread thread1 = new KThread(runnable);
//        KThread thread2 = new KThread(runnable);
//        KThread thread3 = new KThread(runnable);
//
//        thread1.setName("PriorityScheduler-thread1");
//        thread2.setName("PriorityScheduler-thread1");
//        thread3.setName("PriorityScheduler-thread1");
//
//        boolean intStatus = Machine.interrupt().disable();
//        ps.setPriority(thread1, PriorityScheduler.priorityMinimum);
//        ps.setPriority(thread2, PriorityScheduler.priorityDefault);
//        ps.setPriority(thread3, PriorityScheduler.priorityMaximum);
//        Lib.assertTrue(ps.getEffectivePriority(thread1) == PriorityScheduler.priorityMinimum);
//        Lib.assertTrue(ps.getEffectivePriority(thread2) == PriorityScheduler.priorityDefault);
//        Lib.assertTrue(ps.getEffectivePriority(thread3) == PriorityScheduler.priorityMaximum);
//
//        queue1.waitForAccess(thread1);
//        queue1.waitForAccess(thread2);
//        queue2.waitForAccess(thread1);
//        queue2.waitForAccess(thread2);
//        System.out.println(KThread.currentThread().getName());
//        Lib.assertTrue(queue1.nextThread().equals(thread2), "下一个线程应该是2");
//        System.out.println(KThread.currentThread().getName());
//        queue3.acquire(thread1);
//        queue3.waitForAccess(thread3);
//        System.out.println(ps.getEffectivePriority(thread1) + "      " + ps.getEffectivePriority(thread3));
//        Lib.assertTrue(ps.getEffectivePriority(thread1) == PriorityScheduler.priorityMaximum,"高优先级 捐赠给低优先级");
//
//        Lib.assertTrue(ps.getEffectivePriority(thread2) == PriorityScheduler.priorityDefault,"不变");
//
//        Lib.assertTrue(queue2.nextThread().equals(thread1),"提高");
//
//
//
//
//
//
//        Machine.interrupt().restore(intStatus);
//    }
    private static class PingTest implements Runnable {
        Lock a = null, b = null;
        int name;

        PingTest(Lock A, Lock B, int x) {
            a = A;
            b = B;
            name = x;
        }

        public void run() {
            System.out.println("Thread " + name + " starts.");
            if (b != null) {
                System.out.println("Thread " + name + " waits for Lock b.");
                b.acquire();
                System.out.println("Thread " + name + " gets Lock b.");
            }
            if (a != null) {
                System.out.println("Thread " + name + " waits for Lock a.");
                a.acquire();
                System.out.println("Thread " + name + " gets Lock a.");
            }
            KThread.yield();
            boolean intStatus = Machine.interrupt().disable();
            System.out.println("Thread " + name + " has priority " + ThreadedKernel.scheduler.getEffectivePriority() + ".");
            Machine.interrupt().restore(intStatus);
            KThread.yield();
            if (b != null) b.release();
            if (a != null) a.release();
            System.out.println("Thread " + name + " finishs.");

        }
    }

    public static void selfTest() {
        Lock a = new Lock();
        Lock b = new Lock();

        Queue<KThread> qq = new LinkedList<KThread>();
        for (int i = 1; i <= 5; i++) {
            KThread kk = new KThread(new PingTest(null, null, i));
            qq.add(kk);
            kk.setName("Thread-" + i).fork();
        }
        for (int i = 6; i <= 10; i++) {
            KThread kk = new KThread(new PingTest(a, null, i));
            qq.add(kk);
            kk.setName("Thread-" + i).fork();
        }
        for (int i = 11; i <= 15; i++) {
            KThread kk = new KThread(new PingTest(a, b, i));
            qq.add(kk);
            kk.setName("Thread-" + i).fork();
        }
        KThread.yield();
        Iterator it = qq.iterator();
        int pp = 0;
        while (it.hasNext()) {
            boolean intStatus = Machine.interrupt().disable();
            ThreadedKernel.scheduler.setPriority((KThread) it.next(), pp + 1);
            Machine.interrupt().restore(intStatus);
            pp = (pp + 1) % 6 + 1;
        }
    }


}

