// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

/**
 * A set of controls that can be used by an elevator controller.
 */

//elevator controller可以使用的一组控制器。
public interface ElevatorControls {
    /**
     * Return the number of floors in the elevator bank. If <i>n</i> is the
     * number of floors in the bank, then the floors are numbered <i>0</i>
     * (the ground floor) through <i>n - 1</i> (the top floor).
     *
     * @return	the number of floors in the bank.
     */
    //然后floor数量
    public int getNumFloors();

    /**
     * Return the number of elevators in the elevator bank. If <i>n</i> is the
     * number of elevators in the bank, then the elevators are numbered
     * <i>0</i> through <i>n - 1</i>.
     *
     * @return	the numbe rof elevators in the bank.
     */
    //返回elevators 数量
    public int getNumElevators();

    /**
     * Set the elevator interrupt handler. This handler will be called when an
     * elevator event occurs, and when all the riders have reaced their
     * destinations.
     *
     * @param	handler	the elevator interrupt handler.
     */

    //设置 elevator interrupt handler  当elevator的事件发生后  当所有riders到达目的地是 启动此程序
    public void setInterruptHandler(Runnable handler);
	
    /**
     * Open an elevator's doors.
     *
     * @param	elevator	which elevator's doors to open.
     */
    //开门？？
    public void openDoors(int elevator);

    /**
     * Close an elevator's doors.
     *
     * @param	elevator	which elevator's doors to close.
     */
    //关门？？
    public void closeDoors(int elevator);

    /**
     * Move an elevator to another floor. The elevator's doors must be closed.
     * If the elevator is already moving and cannot safely stop at the
     * specified floor because it has already passed or is about to pass the
     * floor, fails and returns <tt>false</tt>. If the elevator is already
     * stopped at the specified floor, returns <tt>false</tt>.
     *
     * @param	floor		the floor to move to.
     * @param	elevator	the elevator to move.
     * @return	<tt>true</tt> if the elevator's destination was changed.
     */

    //将elevator移动到另一个floor
    //如果电梯已经在移动，并且由于已经通过或即将通过楼层而无法在指定楼层安全停车，则故障并返回<tt>false</tt>
    //如果电梯已在指定楼层停止，则返回<tt>false</tt>。
    public boolean moveTo(int floor, int elevator);

    /**
     * Return the current location of the elevator. If the elevator is in
     * motion, the returned value will be within one of the exact location.
     *
     * @param	elevator	the elevator to locate.
     * @return	the floor the elevator is on.
     */
    //返回电梯的当前位置。如果电梯正在运行，返回值将在一个确切的位置内
    public int getFloor(int elevator);

    /**
     * Set which direction the elevator bank will show for this elevator's
     * display. The <i>direction</i> argument should be one of the <i>dir*</i>
     * constants in the <tt>ElevatorBank</tt> class.
     *
     * @param	elevator	the elevator whose direction display to set.
     * @param	direction	the direction to show (up, down, or neither).
     */
    //设置电梯组将向此电梯显示的方向。<i>direction</i>参数应该是<tt>elevatorbank</tt>类中的<i>dir*</i>常量之一。
    public void setDirectionDisplay(int elevator, int direction);

    /**
     * Call when the elevator controller is finished.
     */
    //停止
    public void finish();

    /**
     * Return the next event in the event queue. Note that there may be
     * multiple events pending when an elevator interrupt occurs, so this
     * method should be called repeatedly until it returns <tt>null</tt>.
     *
     * @return	the next event, or <tt>null</tt> if no further events are
     * currently pending.
     */
    //返回事件队列中的下一个事件。请注意，当电梯中断发生时，可能有多个事件挂起，因此应重复调用此方法，直到它返回<tt>null</tt>。1
    public ElevatorEvent getNextEvent();
}
