// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

/**
 * A set of controls that can be used by a rider controller. Each rider uses a
 * distinct <tt>RiderControls</tt> object.
 */
//一组可由rider controller使用的controller。每个骑手使用一个不同的<tt>ridercontrols</tt>对象。
public interface RiderControls {
    /**
     * Return the number of floors in the elevator bank. If <i>n</i> is the
     * number of floors in the bank, then the floors are numbered <i>0</i>
     * (the ground floor) through <i>n - 1</i> (the top floor).
     *
     * @return	the number of floors in the bank.
     */
    //返回电梯楼层数
    public int getNumFloors();

    /**
     * Return the number of elevators in the elevator bank. If <i>n</i> is the
     * number of elevators in the bank, then the elevators are numbered
     * <i>0</i> through <i>n - 1</i>.
     *
     * @return	the numbe rof elevators in the bank.
     */
    //返回电梯数
    public int getNumElevators();

    /**
     * Set the rider's interrupt handler. This handler will be called when the
     * rider observes an event.
     *
     * @param	handler	the rider's interrupt handler.
     */
    //设置rider的中断处理程序。当rider观察事件时，将调用此处理程序。
    public void setInterruptHandler(Runnable handler);
	
    /**
     * Return the current location of the rider. If the rider is in motion,
     * the returned value will be within one of the exact location.
     *
     * @return	the floor the rider is on.
     */

    //返回rider的当前位置
    public int getFloor();

    /**
     * Return an array specifying the sequence of floors at which this rider
     * has successfully exited an elevator. This array naturally does not
     * count the floor the rider started on, nor does it count floors where
     * the rider is in the elevator and does not exit.
     *
     * @return	an array specifying the floors this rider has visited.
     */
    //返回一个数组，指定该骑手成功进入电梯的楼层顺序。这个数组自然不计算骑手开始的楼层，也不计算骑手在电梯中且不退出的楼层。
    public int[] getFloors();

    /**
     * Return the indicated direction of the specified elevator, set by
     * <tt>ElevatorControls.setDirectionDisplay()</tt>.
     *
     * @param	elevator	the elevator to check the direction of.
     * @return	the displayed direction for the elevator.
     *
     * @see	nachos.machine.ElevatorControls#setDirectionDisplay
     */
    //返回指定电梯的指示方向，由elvatorcontrols.setDirectionDisplay（）设置。
    public int getDirectionDisplay(int elevator);

    /**
     * Press a direction button. If <tt>up</tt> is <tt>true</tt>, invoke
     * <tt>pressUpButton()</tt>; otherwise, invoke <tt>pressDownButton()</tt>.
     *
     * @param	up	<tt>true</tt> to press the up button, <tt>false</tt> to
     *			press the down button.
     * @return	the return value of <tt>pressUpButton()</tt> or of
     *		<tt>pressDownButton()</tt>.
     */
    //按方向键。如果<tt>up</tt>是<tt>true</tt>，则调用<tt>pressUpbutton（）</tt>；否则，调用<tt>pressDownbutton（）</tt>。
    public boolean pressDirectionButton(boolean up);

    /**
     * Press the up button. The rider must not be on the top floor and must not
     * be inside an elevator. If an elevator is on the same floor as this
     * rider, has the doors open, and says it is going up, does nothing and
     * returns <tt>false</tt>.
     *
     * @return	<tt>true</tt> if the button event was sent to the elevator
     *		controller.
     */
    //按向上按钮。rider不得在顶层，也不得在电梯内。如果电梯和这个rider在同一层，门是开着的，并且说它在上升，什么也不做，然后返回<tt>false</tt>。
    public boolean pressUpButton();

    /**
     * Press the down button. The rider must not be on the bottom floor and
     * must not be inside an elevator. If an elevator is on the same floor as
     * as this rider, has the doors open, and says it is going down, does
     * nothing and returns <tt>false</tt>.
     *
     * @return	<tt>true</tt> if the button event was sent to the elevator
     *		controller.
     */
    //按向上按钮。rider不得在顶层，也不得在电梯内。如果电梯和这个rider在同一层，门是开着的，并且说它在下降，什么也不做，然后返回<tt>false</tt>。
    public boolean pressDownButton();

    /**
     * Enter an elevator. A rider cannot enter an elevator if its doors are not
     * open at the same floor, or if the elevator is full. The rider must not
     * already be in an elevator.
     *
     * @param	elevator	the elevator to enter.
     * @return	<tt>true</tt> if the rider successfully entered the elevator.
     */
    //进入电梯如果电梯的门在同一层没有打开，或者电梯已经满了，rider就不能进入电梯。rider不能已经在电梯里了。
    public boolean enterElevator(int elevator);

    /**
     * Press a floor button. The rider must be inside an elevator. If the
     * elevator already has its doors open on <tt>floor</tt>, does nothing and
     * returns <tt>false</tt>.
     *
     * @param	floor	the button to press.
     * @return	<tt>true</tt> if the button event was sent to the elevator
     *		controller.
     */
    //按楼层按钮。rider必须在电梯里。如果电梯已经在<tt>floor</tt>打开了门，则不执行任何操作并返回<tt>FALSE</tt>。
    public boolean pressFloorButton(int floor);

    /**
     * Exit the elevator. A rider cannot exit the elevator if its doors are not
     * open on the requested floor. The rider must already be in an elevator.
     *
     * @param	floor	the floor to exit on.
     * @return	<tt>true</tt> if the rider successfully got off the elevator.
     */
    //离开电梯。如果电梯的门在要求的楼层没有打开，rider就不能离开电梯rider一定已经在电梯里了。
    public boolean exitElevator(int floor);

    /**
     * Call when the rider is finished.
     */
    //结束时调用
    public void finish();

    /**
     * Return the next event in the event queue. Note that there may be
     * multiple events pending when a rider interrupt occurs, so this
     * method should be called repeatedly until it returns <tt>null</tt>.
     *
     * @return	the next event, or <tt>null</tt> if no further events are
     * currently pending.
     */
    //返回事件队列中的下一个事件。注意，当一个附加程序中断发生时，可能有多个事件挂起，因此应该重复调用此方法，直到它返回<tt>null</tt>
    public RiderEvent getNextEvent();
}
