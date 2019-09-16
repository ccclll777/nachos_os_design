// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

import nachos.security.*;

/**
 * A hardware timer generates a CPU timer interrupt approximately every 500
 * clock ticks. This means that it can be used for implementing time-slicing,
 * or for having a thread go to sleep for a specific period of time.
 *
 * The <tt>Timer</tt> class emulates a hardware timer by scheduling a timer
 * interrupt to occur every time approximately 500 clock ticks pass. There is
 * a small degree of randomness here, so interrupts do not occur exactly every
 * 500 ticks.
 */

//每500个周期产生一个中断
//•重要方法：
//–gettime（）获取总时钟数
//–setInterruptHandler（）告诉计时器当它关闭时该做什么
//•提供抢占权

	//每当mips模拟器执行一条指令时，时钟前进一个tick。

	//Nachos提供了一个计时器实例来模拟实时时钟，以固定的间隔生成中断。它是使用上述事件驱动的中断机制实现的。machine.timer（）返回对此计时器的引用。
public final class Timer {
    /**
     * Allocate a new timer.
     *
     * @param	privilege      	encapsulates privileged access to the Nachos
     *				machine.
     */
    public Timer(Privilege privilege) {
	System.out.print(" timer");
	
	this.privilege = privilege;
	
	timerInterrupt = new Runnable() {
		public void run() { timerInterrupt(); }
	    };
	
	autoGraderInterrupt = new Runnable() {
		public void run() {
		    Machine.autoGrader().timerInterrupt(Timer.this.privilege,
							lastTimerInterrupt);
		}
	    };

	scheduleInterrupt();
    }

    /**
     * Set the callback to use as a timer interrupt handler. The timer
     * interrupt handler will be called approximately every 500 clock ticks.
     *
     * @param	handler		the timer interrupt handler.
     */
    //将回调设置为用作计时器中断处理程序。计时器中断处理程序大约每500个时钟周期调用一次。
	//设置计时器中断处理程序，模拟计时器大约在每个stats.timer ticks滴答声时调用该处理程序。
    public void setInterruptHandler(Runnable handler) {
	this.handler = handler;
    }

    /**
     * Get the current time.
     *
     * @return	the number of clock ticks since Nachos started.
     */
    //计时器可用于提供抢占。但是，请注意，计时器中断并不总是以完全相同的间隔发生。不要依赖间隔相等的计时器中断；而是使用getTime（）。
	//获取当前已经进行的时间
    public long getTime() {
	return privilege.stats.totalTicks;
    }


    private void timerInterrupt() {
	scheduleInterrupt();
	scheduleAutoGraderInterrupt();

	//最后一个中断的 时间
	lastTimerInterrupt = getTime();

	if (handler != null)
	    handler.run();
    }

    private void scheduleInterrupt() {
	int delay = Stats.TimerTicks;
	delay += Lib.random(delay/10) - (delay/20);

	privilege.interrupt.schedule(delay, "timer", timerInterrupt);
    }

    private void scheduleAutoGraderInterrupt() {
	privilege.interrupt.schedule(1, "timerAG", autoGraderInterrupt);
    }

    private long lastTimerInterrupt;
    private Runnable timerInterrupt;
    private Runnable autoGraderInterrupt;

    private Privilege privilege;
    private Runnable handler = null;
}
