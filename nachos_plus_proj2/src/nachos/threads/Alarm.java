package nachos.threads;

import nachos.machine.Machine;
import nachos.machine.Timer;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
//使用硬件计时器提供抢占，并允许线程休眠到某个时间。
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    //创建一个新的时钟 将机器的计时器中断处理程序设置为此警报的回调
    //Nachos不能在多个alarm下正常工作。
    public Alarm() {
        wakeQueue = new PriorityQueue(1, new TimeCompare());
        queueLock = new Lock();
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    //计时器中断处理程序。这是由机器的计时器周期性地（大约每500个时钟周期）调用的。使当前线程停止，如果有另一个线程，则强制进行上下文切换  并且运行
    public void timerInterrupt() {
        //如果等待的就绪队列中有 线程的waketime 小于现在的时钟 则寻找 waketime最小的那个线程 进行强制的上下文切换
        boolean intStatus = Machine.interrupt().disable();
        long curTime = Machine.timer().getTime();
        KThreadWakeime kThreadWakeime = wakeQueue.peek();

        if(kThreadWakeime != null)
        {

            if(kThreadWakeime.wakeTime < curTime )
            {
                kThreadWakeime.thread.ready();
                wakeQueue.poll();
            }

        }
        Machine.interrupt().restore(intStatus);
        KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	Timer#getTime()
     */
    //将当前线程至少休眠<i>x</i>个信号，在计时器中断处理程序中将其唤醒。线程必须在第一次计时器中断期间唤醒（放置在调度程序就绪队列中）
    // ，其中(current time) >= (WaitUntil called time)+(x)

    /**
     * task3
     * @param x
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
	long wakeTime = Machine.timer().getTime() + x;

        boolean interruptStatus = Machine.interrupt().disable();

        queueLock.acquire();
        KThreadWakeime kThreadWakeime = new KThreadWakeime(wakeTime,KThread.currentThread());
        this.wakeQueue.add(kThreadWakeime);
        queueLock.release();
//        System.out.println("当前线程"+KThread.currentThread().getName()+"休眠"+x+"个tick，应该在"+wakeTime+"时唤醒");
        KThread.sleep();
        Machine.interrupt().restore(interruptStatus);


    }

//    PriorityQueue

    //每个线程需要停止时间的类
    private   class KThreadWakeime {
    private KThread thread;
    private long wakeTime;

    KThreadWakeime(long wakeTime, KThread thread) {
        this.wakeTime = wakeTime;
        this.thread = thread;
    }

    public String toString() {
        String thing = String.valueOf(this.wakeTime);
        return thing;
    }
}


    public class TimeCompare implements Comparator {
        public int compare(Object o1, Object o2) {
            KThreadWakeime t1 = (KThreadWakeime) o1;
            KThreadWakeime t2 = (KThreadWakeime) o2;
            if (t1.wakeTime > t2.wakeTime)
                return 1;
            else if (t1.wakeTime < t2.wakeTime)
                return -1;
            else return 0;
        }
    }

    //存放需要wait线程的队列
    private PriorityQueue<KThreadWakeime> wakeQueue;
    //保证修改队列原子性的锁
    private Lock queueLock;

    private static class PingAlarmTest implements Runnable {
        PingAlarmTest(int which, Alarm alarm) {
            this.which = which;
            this.alarm = alarm;

        }
        Alarm alarm;

        public void run() {
            System.out.println("thread " + which + " started.");
            alarm.waitUntil(which);
            System.out.println("thread " + which + " ran.");

        }

        private int which;
    }


    public static void selfTest()
    {
        AlarmTest.runTest();

        }

}
