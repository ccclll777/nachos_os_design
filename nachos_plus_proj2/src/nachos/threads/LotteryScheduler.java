package nachos.threads;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */

/**
 * 使用抽签选择线程的调度程序。
 *
 * 彩票调度程序将多张彩票与每个线程关联起来。当一个线程需要出列时，在所有等待出列的线程的所有票中，会举行随机抽签。选择持有中奖票的线程。
 *
 * 请注意，彩票调度程序必须能够处理大量彩票（有时是数十亿张），因此不能为每个彩票维护状态。
 *
 * 彩票调度程序必须部分解决优先级反转问题；在
 * 特别是，票必须通过锁和连接进行传输。
 * 与优先级调度程序不同，这些票据会添加（而不是只获取最大值）。
 *
 */

/**
 * 在调度过程中  并不是选择彩票数最多的线程运行   而是随机抽取一张彩票  让彩票的主人运行   这样 彩票越多 下次运行的机会越大
 */

import nachos.machine.Lib;
import nachos.machine.Machine;

import java.util.*;

/**
 * 流程：  统计总的彩票数  生成合理中奖彩票   找出持有者  调度
 */
public class LotteryScheduler extends Scheduler {
    public LotteryScheduler() {
    }

    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new LotteryQueue(transferPriority);
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

    public static final int priorityDefault = 1;

    public static final int priorityMinimum = 0;

    public static final int priorityMaximum = Integer.MAX_VALUE;

    protected ThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new ThreadState(thread);

        return (ThreadState) thread.schedulingState;
    }

    protected class LotteryQueue extends ThreadQueue {
        LotteryQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;

            waitThreadsSet = new TreeSet<ThreadState>();
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
            if(transferPriority) //
                //当前队列持有锁的线程 是此线程
                holdThread = getThreadState(thread);
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me
            ThreadState temp = pickNextThread(); //
            if(temp == null) //
                return null;
            else
                return temp.thread;
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return	the next thread that <tt>nextThread()</tt> would
         *		return.
         */

        protected ThreadState NextThread() { //

            int sum = 0;
            if (waitThreadsSet.isEmpty()) return null;
            for (ThreadState threadState : waitThreadsSet) {
                //获取彩票总数
                sum += threadState.getEffectivePriority();
            }

            int tmp = 0;
            //生成彩票随机数
            int lotteryValue = (new Random()).nextInt(sum) + 1;

            //寻找被随机到的  进程
            for (ThreadState threadState : waitThreadsSet) {
                tmp += threadState.effectivePriority;
                if (tmp >= lotteryValue) {
                    waitThreadsSet.remove(threadState);
                    return threadState;
                }
            }
            return null;
        }

        protected ThreadState pickNextThread() {
            // implement me
            //取出⼀一个被彩票 抽中的线程
            ThreadState res = NextThread(); //
            if(holdThread!=null) //
            {
                //将此队列 移除  刚才 拥有此队列锁的线程  的WaitResourceQueues  因为要更换 一个新的拥有锁的线程
                holdThread.holdQueues.remove(this);
                holdThread.getEffectivePriority();
                //此队列现在拥有锁的线程 应该是当前执行的线程
                holdThread=res;
            }
            if(res!=null) //
                res.waitQueue = null;
            return res;
        }

        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());
        }

        public void add(ThreadState thread) { //
            waitThreadsSet.add(thread);
        }

        public boolean isEmpty() { //
            return waitThreadsSet.isEmpty();

        }

        protected long cnt = 0; //
        public boolean transferPriority;
        //protected TreeSet<ThreadState> waitThreads[] = new TreeSet[priorityMaximum+1];
        protected TreeSet<ThreadState> waitThreadsSet ;
        protected ThreadState holdThread = null; //

    }

    protected class ThreadState /**/implements Comparable<ThreadState>/**/{
        public ThreadState(KThread thread) {
            this.thread = thread;
            setPriority(priorityDefault);
            getEffectivePriority(); //
        }

        public int compareTo(ThreadState target) {
            if (this.effectivePriority > target.effectivePriority)
                return 1;
            else if (this.effectivePriority < target.effectivePriority)
                return -1;
            else {
                if (this.time > target.time)
                    return 1;
                if (this.time < target.time)
                    return -1;
                return 0;
            }
        }

        public int getPriority() {
            return priority;
        }

        public int getEffectivePriority() { //
            int temp = priority;

            if(!holdQueues.isEmpty())
            {
                Iterator<LotteryQueue> iterator=holdQueues.iterator();
                while(iterator.hasNext())
                {
                    LotteryQueue holdQueue = (LotteryQueue)iterator.next();

                    //获得某个线程的 有效彩票数  可以叠加
                    for (ThreadState threadState : holdQueue.waitThreadsSet) {
                        temp += threadState.effectivePriority;
                    }

                }
            }
            //重新计算自己拥有的 彩票数
            if(waitQueue!=null&&temp!=effectivePriority)
            {
                ((LotteryQueue) waitQueue).waitThreadsSet.remove(this);
                this.effectivePriority = temp;
                ((LotteryQueue) waitQueue).waitThreadsSet.add(this);
            }
            if(holdThread!=null)

                holdThread.getEffectivePriority();
            return (effectivePriority=temp);
        }

        public void setPriority(int priority) {
            if (this.priority == priority)
                return;

            this.priority = priority;

            // implement me
            getEffectivePriority(); //
        }

        public void waitForAccess(LotteryQueue waitQueue) { //
            // implement me
            Lib.assertTrue(Machine.interrupt().disabled());
            //waitQueue拥有资源 等待调⽤ 优先级可能不够高
            time=++waitQueue.cnt;
            this.waitQueue=waitQueue;
            //将此线程 加入 等待队列的   等待资源的线程的 数据结构中
            waitQueue.add(this);
            //此线程需要的资源  被等待队列中那个 拥有锁的线程 拿着
            holdThread=waitQueue.holdThread;
            //获取自己的有效优先权
            getEffectivePriority();
        }


        public void acquire(LotteryQueue waitQueue) { //
            // implement me
            Lib.assertTrue(Machine.interrupt().disabled());
            if(waitQueue.transferPriority)
                //所以将waitQueue加入  此线程的 等待资源的队列中
                holdQueues.add(waitQueue);
            Lib.assertTrue(waitQueue.isEmpty());
        }

        protected KThread thread;
        protected int priority/**/, effectivePriority;
        protected long time; //
        protected ThreadQueue waitQueue=null; //表示 所处的等待队列
        protected LinkedList<LotteryQueue> holdQueues = new LinkedList<LotteryQueue>(); //表示 ⼦子孙线程 等待它拥有资源的线程队列  等待队列
        protected ThreadState holdThread=null; // //该线程等待队列中 拥有锁的线程
    }

    public static void selfTest() {
        System.out.println("---------LotteryScheduler test---------------------");
        LotteryScheduler s = new LotteryScheduler();
        ThreadQueue queue = s.newThreadQueue(true);
        ThreadQueue queue2 = s.newThreadQueue(true);

        KThread thread1 = new KThread();
        KThread thread2 = new KThread();
        KThread thread3 = new KThread();
        KThread thread4 = new KThread();
        KThread thread5 = new KThread();
        thread1.setName("thread1");
        thread2.setName("thread2");
        thread3.setName("thread3");
        thread4.setName("thread4");
        thread5.setName("thread5");
        thread1.setPriority(10);
        thread2.setPriority(15);
        thread3.setPriority(20);
        thread4.setPriority(25);
        thread5.setPriority(30);
        System.out.println("thread1 EffectivePriority="+s.getThreadState(thread1).getEffectivePriority());
        System.out.println("thread1 Priority="+s.getThreadState(thread1).getPriority());
        System.out.println("thread2 EffectivePriority="+s.getThreadState(thread2).getEffectivePriority());
        System.out.println("thread2 Priority="+s.getThreadState(thread2).getPriority());
        System.out.println("thread3 EffectivePriority="+s.getThreadState(thread3).getEffectivePriority());
        System.out.println("thread3 Priority="+s.getThreadState(thread3).getPriority());
        System.out.println("thread4 EffectivePriority="+s.getThreadState(thread4).getEffectivePriority());
        System.out.println("thread4 Priority="+s.getThreadState(thread4).getPriority());
        System.out.println("thread5 EffectivePriority="+s.getThreadState(thread5).getEffectivePriority());
        System.out.println("thread5 Priority="+s.getThreadState(thread5).getPriority());
        boolean intStatus = Machine.interrupt().disable();
        System.out.println();
        System.out.println("~~~~~~~~Thread1 aquires queue thread2 thread3 waits~~~~~~~~~`");
        queue.acquire(thread1);
        queue.waitForAccess(thread2);
        queue.waitForAccess(thread3);
        System.out.println("thread1 EffectivePriority="+s.getThreadState(thread1).getEffectivePriority());
        System.out.println("thread1 Priority="+s.getThreadState(thread1).getPriority());
        System.out.println("thread2 EffectivePriority="+s.getThreadState(thread2).getEffectivePriority());
        System.out.println("thread2 Priority="+s.getThreadState(thread2).getPriority());
        System.out.println("thread3 EffectivePriority="+s.getThreadState(thread3).getEffectivePriority());
        System.out.println("thread3 Priority="+s.getThreadState(thread3).getPriority());
        System.out.println("thread4 EffectivePriority="+s.getThreadState(thread4).getEffectivePriority());
        System.out.println("thread4 Priority="+s.getThreadState(thread4).getPriority());
        System.out.println("thread5 EffectivePriority="+s.getThreadState(thread5).getEffectivePriority());
        System.out.println("thread5 Priority="+s.getThreadState(thread5).getPriority());

        System.out.println("~~~~~~~~Thread4 aquires queue2 thread1 waits~~~~~~~~~`");
        System.out.println();
        queue2.acquire(thread4);
        queue2.waitForAccess(thread1);
        System.out.println("thread1 EffectivePriority="+s.getThreadState(thread1).getEffectivePriority());
        System.out.println("thread1 Priority="+s.getThreadState(thread1).getPriority());
        System.out.println("thread2 EffectivePriority="+s.getThreadState(thread2).getEffectivePriority());
        System.out.println("thread2 Priority="+s.getThreadState(thread2).getPriority());
        System.out.println("thread3 EffectivePriority="+s.getThreadState(thread3).getEffectivePriority());
        System.out.println("thread3 Priority="+s.getThreadState(thread3).getPriority());
        System.out.println("thread4 EffectivePriority="+s.getThreadState(thread4).getEffectivePriority());
        System.out.println("thread4 Priority="+s.getThreadState(thread4).getPriority());
        System.out.println("thread5 EffectivePriority="+s.getThreadState(thread5).getEffectivePriority());
        System.out.println("thread5 Priority="+s.getThreadState(thread5).getPriority());
        System.out.println("~~~~~~~~thread2 priority changed to 25~~~~~~~~~`");
        System.out.println();
        s.getThreadState(thread2).setPriority(25);

        System.out.println("thread1 EffectivePriority="+s.getThreadState(thread1).getEffectivePriority());
        System.out.println("thread1 Priority="+s.getThreadState(thread1).getPriority());
        System.out.println("thread2 EffectivePriority="+s.getThreadState(thread2).getEffectivePriority());
        System.out.println("thread2 Priority="+s.getThreadState(thread2).getPriority());
        System.out.println("thread3 EffectivePriority="+s.getThreadState(thread3).getEffectivePriority());
        System.out.println("thread3 Priority="+s.getThreadState(thread3).getPriority());
        System.out.println("thread4 EffectivePriority="+s.getThreadState(thread4).getEffectivePriority());
        System.out.println("thread4 Priority="+s.getThreadState(thread4).getPriority());
        System.out.println("thread5 EffectivePriority="+s.getThreadState(thread5).getEffectivePriority());
        System.out.println("thread5 Priority="+s.getThreadState(thread5).getPriority());
        System.out.println();
        System.out.println("~~~~~~~~thread2 priority changed to 15~~~~~~~~~`");
        s.getThreadState(thread2).setPriority(15);

        System.out.println("thread1 EffectivePriority="+s.getThreadState(thread1).getEffectivePriority());
        System.out.println("thread1 Priority="+s.getThreadState(thread1).getPriority());
        System.out.println("thread2 EffectivePriority="+s.getThreadState(thread2).getEffectivePriority());
        System.out.println("thread2 Priority="+s.getThreadState(thread2).getPriority());
        System.out.println("thread3 EffectivePriority="+s.getThreadState(thread3).getEffectivePriority());
        System.out.println("thread3 Priority="+s.getThreadState(thread3).getPriority());
        System.out.println("thread4 EffectivePriority="+s.getThreadState(thread4).getEffectivePriority());
        System.out.println("thread4 Priority="+s.getThreadState(thread4).getPriority());
        System.out.println("thread5 EffectivePriority="+s.getThreadState(thread5).getEffectivePriority());
        System.out.println("thread5 Priority="+s.getThreadState(thread5).getPriority());
        System.out.println();
        System.out.println("~~~~~~~~Thread5 waits on queue1~~~~~~~~~`");
        queue.waitForAccess(thread5);

        System.out.println("thread1 EffectivePriority="+s.getThreadState(thread1).getEffectivePriority());
        System.out.println("thread1 Priority="+s.getThreadState(thread1).getPriority());
        System.out.println("thread2 EffectivePriority="+s.getThreadState(thread2).getEffectivePriority());
        System.out.println("thread2 Priority="+s.getThreadState(thread2).getPriority());
        System.out.println("thread3 EffectivePriority="+s.getThreadState(thread3).getEffectivePriority());
        System.out.println("thread3 Priority="+s.getThreadState(thread3).getPriority());
        System.out.println("thread4 EffectivePriority="+s.getThreadState(thread4).getEffectivePriority());
        System.out.println("thread4 Priority="+s.getThreadState(thread4).getPriority());
        System.out.println("thread5 EffectivePriority="+s.getThreadState(thread5).getEffectivePriority());
        System.out.println("thread5 Priority="+s.getThreadState(thread5).getPriority());
        System.out.println();
        System.out.println("~~~~~~~~thread2 priority changed to 30~~~~~~~~~`");
        s.getThreadState(thread2).setPriority(30);
        System.out.println("thread1 EffectivePriority="+s.getThreadState(thread1).getEffectivePriority());
        System.out.println("thread1 Priority="+s.getThreadState(thread1).getPriority());
        System.out.println("thread2 EffectivePriority="+s.getThreadState(thread2).getEffectivePriority());
        System.out.println("thread2 Priority="+s.getThreadState(thread2).getPriority());
        System.out.println("thread3 EffectivePriority="+s.getThreadState(thread3).getEffectivePriority());
        System.out.println("thread3 Priority="+s.getThreadState(thread3).getPriority());
        System.out.println("thread4 EffectivePriority="+s.getThreadState(thread4).getEffectivePriority());
        System.out.println("thread4 Priority="+s.getThreadState(thread4).getPriority());
        System.out.println("thread5 EffectivePriority="+s.getThreadState(thread5).getEffectivePriority());
        System.out.println("thread5 Priority="+s.getThreadState(thread5).getPriority());
    }



}

