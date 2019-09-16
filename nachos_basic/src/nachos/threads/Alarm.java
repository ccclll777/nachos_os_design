package nachos.threads;

import nachos.machine.*;

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
     * @see	nachos.machine.Timer#getTime()
     */
    //将当前线程至少休眠<i>x</i>个信号，在计时器中断处理程序中将其唤醒。线程必须在第一次计时器中断期间唤醒（放置在调度程序就绪队列中），其中(current time) >= (WaitUntil called time)+(x)
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
	long wakeTime = Machine.timer().getTime() + x;
	while (wakeTime > Machine.timer().getTime())
	    KThread.yield();
    }
}
