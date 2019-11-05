// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

/**
 * A serial console can be used to send and receive characters. Only one
 * character may be sent at a time, and only one character may be received at a
 * time.
 */

//控制台系列
//
//包含方法：
//readbyte（）返回一个字节（或--1）当返回更多时，等待中断
//writeByte（）接受一个字节，并等待其准备好更多字节时中断


public interface SerialConsole {
    /**
     * Set this console's receive and send interrupt handlers.
     *
     * <p>
     * The receive interrupt handler is called every time another byte arrives
     * and can be read using <tt>readByte()</tt>.
     *
     * <p>
     * The send interrupt handler is called every time a byte sent with
     * <tt>writeByte()</tt> is finished being sent. This means that another
     * byte can be sent.
     *
     * @param	receiveInterruptHandler	the callback to call when a byte
     *					arrives.
     * @param	sendInterruptHandler	the callback to call when another byte
     *					can be sent.
     */
    //当接收数据或完成发生数据时，反馈给工作台
    public void setInterruptHandlers(Runnable receiveInterruptHandler,
                                     Runnable sendInterruptHandler);

    /**
     * Return the next unsigned byte received (in the range <tt>0</tt> through
     * <tt>255</tt>).
     *
     * @return	the next byte read, or -1 if no byte is available.
     */
    //读取操作测试数据字节是否准备好返回。如果是，则立即返回字节，否则返回-1。
    // 当接收到另一字节的数据时，接收中断发生。一次只能对一个字节进行排队，因此如果没有中间的读取操作，就不可能发生两个接收中断。
    public int	readByte();

    /**
     * Send another byte. If a byte is already being sent, the result is not
     * defined.
     *
     * @param	value	the byte to be sent (the upper 24 bits are ignored).
     */
    public void writeByte(int value);
}
