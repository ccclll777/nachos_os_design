// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

/**
 * An object that maintains Nachos runtime statistics.
 */
//nachos运行是的统计信息
public final class Stats {
    /**
     * Allocate a new statistics object.
     */
    public Stats() {
    }

    /**
     * Print out the statistics in this object.
     */
    //打印各种统计信息
    public void print() {
	System.out.println("Ticks: total " + totalTicks
			   + ", kernel " + kernelTicks
			   + ", user " + userTicks);
	System.out.println("Disk I/O: reads " + numDiskReads
			   + ", writes " + numDiskWrites);
	System.out.println("Console I/O: reads " + numConsoleReads
			   + ", writes " + numConsoleWrites);
	System.out.println("Paging: page faults " + numPageFaults
			   + ", TLB misses " + numTLBMisses);
	System.out.println("Network I/O: received " + numPacketsReceived
			   + ", sent " + numPacketsSent);
    }

    /**
     * The total amount of simulated time that has passed since Nachos
     * started.
     */
    //nachos启动后的总时间
    public long totalTicks = 0;
    /**
     * The total amount of simulated time that Nachos has spent in kernel mode.
     */

    //内核模式运行的总时间数
    public long kernelTicks = 0;
    /**
     * The total amount of simulated time that Nachos has spent in user mode.
     */
    //用户模式运行的总时间数
    public long userTicks = 0;

    /** The total number of sectors Nachos has read from the simulated disk.*/
    //从模拟磁盘读取的扇区总数
    public int numDiskReads = 0;
    /** The total number of sectors Nachos has written to the simulated disk.*/
//已写入模拟磁盘的扇区总数
    public int numDiskWrites = 0;

    /** The total number of characters Nachos has read from the console. */
    //Nachos从控制台读取的字符总数
    public int numConsoleReads = 0;


    /** The total number of characters Nachos has written to the console. */
    //Nachos写入控制台的字符总数。
    public int numConsoleWrites = 0;


    /** The total number of page faults that have occurred. */

    //发生页错误的总数
    public int numPageFaults = 0;

    /** The total number of TLB misses that have occurred. */
//发生TCB未命中总数
    public int numTLBMisses = 0;

    /** The total number of packets Nachos has sent to the network. */

    //nachos发送到网络的数据包总数
    public int numPacketsSent = 0;
    /** The total number of packets Nachos has received from the network. */
    //Nachos从网络接收的数据包总数
    public int numPacketsReceived = 0;

    /**
     * The amount to advance simulated time after each user instructions is
     * executed.
     */
    //执行每个用户指令后提前模拟时间的量。
    public static final int UserTick = 1;
    /**
     * The amount to advance simulated time after each interrupt enable.
     */
    //每次中断启用后提前模拟时间的量。
    public static final int KernelTick = 10;
    /**
     * The amount of simulated time required to rotate the disk 360 degrees.
     */
    //将磁盘旋转360度所需的模拟时间量
    public static final int RotationTime = 500;
    /**
     * The amount of simulated time required for the disk to seek.
     */
    //磁盘搜索所需的模拟时间量
    public static final int SeekTime = 500;
    /**
     * The amount of simulated time required for the console to handle a
     * character.
     */
    //控制台处理字符所需的模拟时间量。
    public static final int ConsoleTime = 100;
    /**
     * The amount of simulated time required for the network to handle a
     * packet.
     */
    //网络处理数据包所需的模拟时间量。
    public static final int NetworkTime = 100;
    /**
     * The mean amount of simulated time between timer interrupts.
     */
    //计时器中断之间的平均模拟时间量。
    public static final int TimerTicks = 500;
    /**
     * The amount of simulated time required for an elevator to move a floor.
     */
    //电梯算法移动一次模拟时间。
    public static final int ElevatorTicks = 2000;
}
