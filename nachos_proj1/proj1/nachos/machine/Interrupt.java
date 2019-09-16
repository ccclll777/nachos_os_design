// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

import nachos.security.Privilege;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * The <tt>Interrupt</tt> class emulates low-level interrupt hardware. The
 * hardware provides a method (<tt>setStatus()</tt>) to enable or disable
 * interrupts.
 *
 * <p>
 * In order to emulate the hardware, we need to keep track of all pending
 * interrupts the hardware devices would cause, and when they are supposed to
 * occur.
 *
 * <p>
 * This module also keeps track of simulated time. Time advances only when the
 * following occur:
 * <ul>
 * <li>interrupts are enabled, when they were previously disabled
 * <li>a MIPS instruction is executed
 * </ul>
 *
 * <p>
 * As a result, unlike real hardware, interrupts (including time-slice context
 * switches) cannot occur just anywhere in the code where interrupts are
 * enabled, but rather only at those places in the code where simulated time
 * advances (so that it becomes time for the hardware simulation to invoke an
 * interrupt handler).
 *
 * <p>
 * This means that incorrectly synchronized code may work fine on this hardware
 * simulation (even with randomized time slices), but it wouldn't work on real
 * hardware. But even though Nachos can't always detect when your program
 * would fail in real life, you should still write properly synchronized code.
 */

//启动硬件中断
//中断类维护一个事件队列，时钟
//两种情况下的时钟周期：一个周期用于执行，十个周期用于重新启用中断。
//任何一个时钟周期之后，中断检查是否有挂起的中断，并运行它们。
//调用设备事件处理程序，而不是软件中断处理程序

//其他硬件模拟设备可使用的重要方法：
//–schedule（）需要一段时间，handler
//–tick（）接受布尔值（1或10个ticks）–
// checkifdue（）调用到期中断–enable（）
//–禁用（）
//•所有硬件设备都依赖于中断—它们没有线程。



	//中断类还模拟硬件接口来启用和禁用中断

	//nachos中的其余硬件设备取决于中断设备。nachos中没有硬件设备创建线程，
// 因此，设备类中的代码执行的唯一时间是由正在运行的kthread调用函数或由中断对象执行的中断处理程序引起的。
public final class Interrupt {
    /**
     * Allocate a new interrupt controller.
     *
     * @param	privilege      	encapsulates privileged access to the Nachos
     *				machine.
     */
    //初始化   传入特权
    public Interrupt(Privilege privilege) {
	System.out.print(" interrupt");
	
	this.privilege = privilege;
	//中断的特权
	privilege.interrupt = new InterruptPrivilege();

	//禁止中断？
	enabled = false;
	//tree set 有序的集合 保证元素的唯一性
	pending = new TreeSet<PendingInterrupt>();
    }

    /**
     * Enable interrupts. This method has the same effect as
     * <tt>setStatus(true)</tt>.
     */
    //开中断。此方法的效果与<tt>setstatus（true）</tt>相同。
    public void enable() {
	setStatus(true);
    }

    /**
     * Disable interrupts and return the old interrupt state. This method has
     * the same effect as <tt>setStatus(false)</tt>.
     *
     * @return	<tt>true</tt> if interrupts were enabled.
     */
    //关中断并返回旧的中断状态。此方法的效果与<tt>setstatus（false）</tt>相同。
    public boolean disable() {
	return setStatus(false);
    }

    /**
     * Restore interrupts to the specified status. This method has the same
     * effect as <tt>setStatus(<i>status</i>)</tt>.
     *
     * @param	status	<tt>true</tt> to enable interrupts.
     */
    //
	//开中断或者关中断  相当于执行<tt>setStatus(<i>status</i>)</tt>.  传入true为开中断  false为关中断
    public void restore(boolean status) {
	setStatus(status);
    }

    /**
     * Set the interrupt status to be enabled (<tt>true</tt>) or disabled
     * (<tt>false</tt>) and return the previous status. If the interrupt
     * status changes from disabled to enabled, the simulated time is advanced.
     *
     * @param	status		<tt>true</tt> to enable interrupts.
     * @return			<tt>true</tt> if interrupts were enabled.
     */
    //将中断状态设置为启用（<tt>true</tt>）或禁用（<tt>false</tt>）并返回先前的状态。如果中断状态从禁用变为启用，则模拟时间提前。
    public boolean setStatus(boolean status) {
	boolean oldStatus = enabled;
	enabled = status;
	
	if (oldStatus == false && status == true)
	    tick(true);

	return oldStatus;
    }

    /**
     * Tests whether interrupts are enabled.
     *
     * @return	<tt>true</tt> if interrupts are enabled.
     */
    //返回是否启动中断 true为启动
    public boolean enabled() {
	return enabled;
    }

    /**
     * Tests whether interrupts are disabled.
     *
     * @return <tt>true</tt> if interrupts are disabled.
     */
    //返回中断是否停止
    public boolean disabled() {
	return !enabled;
    }

	//线程要发生什么样的中断  when=中断时间  type=中断类型  hander为中断的线程
	//以时间和设备事件处理程序作为参数，并计划在指定时间调用指定的处理程序。
    private void schedule(long when, String type, Runnable handler) {
	Lib.assertTrue(when>0);
	
	long time = privilege.stats.totalTicks + when;
	PendingInterrupt toOccur = new PendingInterrupt(time, type, handler);

	Lib.debug(dbgInt,
		  "Scheduling the " + type +
		  " interrupt handler at time = " + time);

	pending.add(toOccur);
    }

    //将时钟时间增加
	//根据Nachos是处于用户模式还是内核模式，将时间提前1或10个滴答。
	// 每当中断从禁用变为启用时，setStatus（）都会调用它，在执行每个用户指令后，processor.run（）也会调用它。
    private void tick(boolean inKernelMode) {

    	//状态
	Stats stats = privilege.stats;

	if (inKernelMode) {
	    stats.kernelTicks += Stats.KernelTick;
	    stats.totalTicks += Stats.KernelTick;
	}
	else {
	    stats.userTicks += Stats.UserTick;
	    stats.totalTicks += Stats.UserTick;
	}

	if (Lib.test(dbgInt))
	    System.out.println("== Tick " + stats.totalTicks + " ==");

	enabled = false;
	checkIfDue();
	enabled = true;
    }

    //测试 当前中断是否结束  测试 系统时钟总时间是否用完？？
	//为排队事件调用事件处理程序，直到不再发生事件为止。它由tick（）调用。
    private void checkIfDue() {
	long time = privilege.stats.totalTicks;

	Lib.assertTrue(disabled());

	if (Lib.test(dbgInt))
	    print();

	if (pending.isEmpty())
	    return;

	if (((PendingInterrupt) pending.first()).time > time)
	    return;

	Lib.debug(dbgInt, "Invoking interrupt handlers at time = " + time);
	
	while (!pending.isEmpty() &&
	       ((PendingInterrupt) pending.first()).time <= time) {
	    PendingInterrupt next = (PendingInterrupt) pending.first();
	    pending.remove(next);

	    Lib.assertTrue(next.time <= time);

	    if (privilege.processor != null)
		privilege.processor.flushPipe();

	    Lib.debug(dbgInt, "  " + next.type);
			
	    next.handler.run();
	}

	Lib.debug(dbgInt, "  (end of list)");
    }

    private void print() {
	System.out.println("Time: " + privilege.stats.totalTicks
			   + ", interrupts " + (enabled ? "on" : "off"));
	System.out.println("Pending interrupts:");

	for (Iterator i=pending.iterator(); i.hasNext(); ) {
	    PendingInterrupt toOccur = (PendingInterrupt) i.next();
	    System.out.println("  " + toOccur.type +
			       ", scheduled at " + toOccur.time);
	}

	System.out.println("  (end of list)");
    }

    private class PendingInterrupt implements Comparable {
	PendingInterrupt(long time, String type, Runnable handler) {
	    this.time = time;
	    this.type = type;
	    this.handler = handler;
	    this.id = numPendingInterruptsCreated++;
	}

	public int compareTo(Object o) {
	    PendingInterrupt toOccur = (PendingInterrupt) o;

	    // can't return 0 for unequal objects, so check all fields
	    if (time < toOccur.time)
		return -1;
	    else if (time > toOccur.time)
		return 1;
	    else if (id < toOccur.id)
		return -1;
	    else if (id > toOccur.id)
		return 1;
	    else
		return 0;
	}

	long time;
	String type;
	Runnable handler;

	private long id;
    }
    
    private long numPendingInterruptsCreated = 0;

    private Privilege privilege;

    private boolean enabled;
    private TreeSet<PendingInterrupt> pending;

    private static final char dbgInt = 'i';


    private class InterruptPrivilege implements Privilege.InterruptPrivilege {
		//线程要发生什么样的中断  when=中断时间  type=中断类型  hander为中断的线程
	public void schedule(long when, String type, Runnable handler) {
	    Interrupt.this.schedule(when, type, handler);
	}

	public void tick(boolean inKernelMode) {
	    Interrupt.this.tick(inKernelMode);
	}
    }
}
