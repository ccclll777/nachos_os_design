package nachos.threads;

import nachos.machine.Kernel;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.TCB;

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
//优先级调度的⼀一个问题是优先级反转。如果⾼优先级线程需要等待低优先级线程
// (例如，低优先级线程持 有的锁)，而另⼀一个⾼高优先级线程在就绪列列表中，
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

        setPriority(thread, priority+1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMinimum)
            return false;

        setPriority(thread, priority-1);

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
            cnt = 0;
            this.transferPriority = transferPriority;
            wait = new TreeSet[priorityMaximum + 1];
            for (int i = 0; i <= priorityMaximum; i++)
                wait[i] = new TreeSet<ThreadState>();

        }

        //将线程加入  等待队列中
        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            //表示此线程 在等待  此队列上的锁
            getThreadState(thread).waitForAccess(this);
        }


        //表示此线程  已经获得锁 可以开始执行
        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).acquire(this);
            if(transferPriority)
                lockholder=getThreadState(thread);
        }


        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            ThreadState res=pickNextThread();

            return res==null?null:res.thread;
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return the next thread that <tt>nextThread()</tt> would
         * return.
         */
        //返回下一个<tt>nextThread（）</tt>将返回的线程，而不修改此队列的状态。
        protected ThreadState pickNextThread() {
            //取出⼀一个优先级最	高的线程;
            ThreadState res=NextThread();

            if(lockholder!=null)
            {
                //将此队列 移除  刚才 拥有此队列锁的线程  的WaitResourceQueues  因为要更换 一个新的拥有锁的线程
                lockholder.holdQueues.remove(this);
                lockholder.getEffectivePriority();
                //此队列现在拥有锁的线程 应该是当前执行的线程
                lockholder=res;
            }
            if(res!=null) res.waitQueue=null;
            return res;
        }


        //执行队列上下一个 优先级最高的线程
        protected ThreadState NextThread() {
            ThreadState res=null;

            for(int i=priorityMaximum;i>=priorityMinimum;i--)
                if((res=wait[i].pollFirst())!=null) break;

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
            wait[thread.effectivePriority].add(thread);
        }

        public boolean isEmpty() { //
            for(int i=0;i<=priorityMaximum;i++)
                if(!wait[i].isEmpty()) return false;
            return true;
        }

        //<tt>true</tt>如果此队列列应将优先级从等待线程传输到所属线程
        protected long cnt = 0;
        public boolean transferPriority;

        protected TreeSet<ThreadState>[] wait; //等待此队列锁的其他线程
        protected ThreadState lockholder=null;//此队列中 拥有锁的线程

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
            holdQueues=new LinkedList<PriorityQueue>();
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
            int res=priority;
            //遍历等待它 资源的 线程队列 （这些线程可以让出自己的优先级）
            if(!holdQueues.isEmpty()) {
                Iterator it=holdQueues.iterator();
                while(it.hasNext())
                {
                    PriorityQueue holdQueue=(PriorityQueue)it.next();
                    //如果等待它拥有的资源的线程中 有优先级⽐比较⼤大的线程
                    for(int i=priorityMaximum;i>res;i--)
                        if(!holdQueue.wait[i].isEmpty()) { res=i;break;}
                }
            }
            //重新计算此线程正在等待的线程 修改自己所处等待队列中 自己的优先级
            if(waitQueue!=null&&res!=effectivePriority)
            {
                ((PriorityQueue)waitQueue).wait[effectivePriority].remove(this);
                ((PriorityQueue)waitQueue).wait[res].add(this);
            }
            effectivePriority=res;
            if(lockholder!=null)
                //让拥有锁的线程  获取自己的有效优先级 看能不能提前执行
                lockholder.getEffectivePriority();
            return res;
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
            Lib.assertTrue(Machine.interrupt().disabled());
            //waitQueue拥有资源 等待调⽤ 优先级可能不够高
            time=++waitQueue.cnt;

            this.waitQueue=waitQueue;
            //将此线程 加入 等待队列的   等待资源的线程的 数据结构中
            waitQueue.add(this);
            //此线程需要的资源  被等待队列中那个 拥有锁的线程 拿着
            lockholder=waitQueue.lockholder;
            //获取自己的有效优先权
            getEffectivePriority();//进⾏优先权的交换
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
        //当前线程已经获得到waitQueue上的锁
        public void acquire(PriorityQueue waitQueue) {
            Lib.assertTrue(Machine.interrupt().disabled());

            //所以将waitQueue加入  此线程的 等待资源的队列中
            if(waitQueue.transferPriority) holdQueues.add(waitQueue);
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
        protected int effectivePriority ;

        protected Long time ;
        ;//等待时间
        protected ThreadQueue waitQueue=null; //表示 所处的等待队列
        protected LinkedList holdQueues;// 表示 ⼦子孙线程 等待它拥有资源的线程队列  等待队列
        protected ThreadState lockholder=null; //该线程等待队列中 拥有锁的线程

        //实现Comparator接⼝口，并重写compare()⽅方法， @Override
        public int compareTo(ThreadState target) {
            if(time==target.time) return 0;
            return time>target.time?1:-1;
        }

    }
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
            System.out.println("Thread " + name + " has EffectivePriority " + ThreadedKernel.scheduler.getEffectivePriority() + ".\n"+"Thread " + name + " has priority " + ThreadedKernel.scheduler.getPriority() + ".");
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
//            System.out.println("线程"+((KThread) it.next()).getName()+"当前的优先级为"+((KThread) it.next()).getPriority());
            Machine.interrupt().restore(intStatus);
            pp = (pp + 1) % 6 + 1;
        }
    }


}
