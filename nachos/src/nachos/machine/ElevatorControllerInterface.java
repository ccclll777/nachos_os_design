// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

/**
 * A controller for all the elevators in an elevator bank. The controller
 * accesses the elevator bank through an instance of <tt>ElevatorControls</tt>.
 */
//电梯组中所有电梯的控制器。控制器通过电梯控制实例访问电梯组。
public interface ElevatorControllerInterface extends Runnable {
    /**
     * Initialize this elevator controller. The controller will access the
     * elevator bank through <i>controls</i>. This constructor should return
     * immediately after this controller is initialized, but not until the
     * interupt handler is set. The controller will start receiving events
     * after this method returns, but potentially before <tt>run()</tt> is
     * called.
     *
     * @param	controls	the controller's interface to the elevator
     *				bank. The controller must not attempt to access
     *				the elevator bank in <i>any</i> other way.
     */
    //初始化这个电梯控制器。控制器将通过<i>controls</i>访问电梯组。
    // 此构造函数应在初始化此控制器后立即返回，但在设置中断处理程序之前不能返回。
    // 控制器将在该方法返回后开始接收事件，但可能在调用<tt>run（）</tt>之前。
    public void initialize(ElevatorControls controls);

    /**
     * Cause the controller to use the provided controls to receive and process
     * requests from riders. This method should not return, but instead should
     * call <tt>controls.finish()</tt> when the controller is finished.
     */
    //使控制器使用提供的控件来接收和处理来自附加程序的请求。此方法不应返回，而是应在控制器完成时调用<tt>controls.finish（）</tt>。
    public void run();

    /** The number of ticks doors should be held open before closing them. */
    public static final int timeDoorsOpen = 500;

    /** Indicates an elevator intends to move down. */
    public static final int dirDown = -1;
    /** Indicates an elevator intends not to move. */
    public static final int dirNeither = 0;
    /** Indicates an elevator intends to move up. */
    public static final int dirUp = 1;
}
