// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

import nachos.security.Privilege;
import nachos.threads.KThread;
import nachos.threads.Semaphore;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

/**
 * A bank of elevators.
 */
//模拟机器的一部分
public final class ElevatorBank implements Runnable {
    /** Indicates an elevator intends to move down. */
    public static final int dirDown = -1;
    /** Indicates an elevator intends not to move. */
    public static final int dirNeither = 0;
    /** Indicates an elevator intends to move up. */
    public static final int dirUp = 1;

    /**
     * Allocate a new elevator bank.
     *
     * @param	privilege      	encapsulates privileged access to the Nachos
     *				machine.
     */
    //线程组？？ 封装对nachos的特权访问
    public ElevatorBank(Privilege privilege) {
	System.out.print(" elevators");
	
	this.privilege = privilege;

	simulationStarted = false;
    }

    /**
     * Initialize this elevator bank with the specified number of elevators and
     * the specified number of floors. The software elevator controller must
     * also be specified. This elevator must not already be running a
     * simulation.
     *
     * @param	numElevators	the number of elevators in the bank.
     * @param	numFloors	the number of floors in the bank.
     * @param	controller	the elevator controller.
     */
    //使用指定数量的elevators和指定数量的floors初始化此elevator bank。还必须指定 software elevator controller。这部电梯一定还没有运行模拟
    public void init(int numElevators, int numFloors,
		     ElevatorControllerInterface controller) {
	Lib.assertTrue(!simulationStarted);

	this.numElevators = numElevators;
	this.numFloors = numFloors;

	manager = new ElevatorManager(controller);

		//电梯状态 数组
	elevators = new ElevatorState[numElevators];
	//初始化
	for (int i=0; i<numElevators; i++)
	    elevators[i] = new ElevatorState(0);

	numRiders = 0;
	//乘客列表
	ridersVector = new Vector<RiderControls>();

	enableGui = false;
	gui = null;
    }

    /**
     * Add a rider to the simulation. This method must not be called after
     * <tt>run()</tt> is called.
     *
     * @param	rider	the rider to add.
     * @param	floor	the floor the rider will start on.
     * @param	stops	the array to pass to the rider's <tt>initialize()</tt>
     *			method.
     * @return	the controls that will be given to the rider.
     */
    //添加一个rider  在调用<tt>run（）</tt>之后，不能调用此方法。
    public RiderControls addRider(RiderInterface rider,
                                  int floor, int[] stops) {
	Lib.assertTrue(!simulationStarted);

	RiderControls controls = new RiderState(rider, floor, stops);
	ridersVector.addElement(controls);
	numRiders++;
	return controls;
    }

    /**
     * Create a GUI for this elevator bank.
     */
    // Create a GUI
    public void enableGui() {
	Lib.assertTrue(!simulationStarted);
	Lib.assertTrue(Config.getBoolean("ElevatorBank.allowElevatorGUI"));

	enableGui = true;
    }

    /**
     * Run a simulation. Initialize all elevators and riders, and then
     * fork threads to each of their <tt>run()</tt> methods. Return when the
     * simulation is finished.
     */

    //运行模拟。初始化所有电梯和乘客，然后将fork线程到它们的每个<tt>run（）</tt>方法。模拟完成后返回。
    public void run() {
	Lib.assertTrue(!simulationStarted);
	simulationStarted = true;

	riders = new RiderState[numRiders];
	ridersVector.toArray(riders);

	if (enableGui) {
	    privilege.doPrivileged(new Runnable() {
		public void run() { initGui(); }
	    });
	}

	//初始化riders
	for (int i=0; i<numRiders; i++)
	    riders[i].initialize();
	//初始化
	manager.initialize();

	for (int i=0; i<numRiders; i++)
	    riders[i].run();
	manager.run();

	for (int i=0; i<numRiders; i++)
	    riders[i].join();
	manager.join();

	simulationStarted = false;
    }

    private void initGui() {
	int[] numRidersPerFloor = new int[numFloors];
	for (int floor=0; floor<numFloors; floor++)
	    numRidersPerFloor[floor] = 0;

	for (int rider=0; rider<numRiders; rider++)
	    numRidersPerFloor[riders[rider].floor]++;

	gui = new ElevatorGui(numFloors, numElevators, numRidersPerFloor);
    }

    /**
     * Tests whether this module is working.
     */
    public static void selfTest() {
	new ElevatorTest().run();
    }

    void postRiderEvent(int event, int floor, int elevator) {
	int direction = dirNeither;
	if (elevator != -1) {
	    Lib.assertTrue(elevator >= 0 && elevator < numElevators);
	    direction = elevators[elevator].direction;
	}

	RiderEvent e = new RiderEvent(event, floor, elevator, direction);
	for (int i=0; i<numRiders; i++) {
	    RiderState rider = riders[i];
	    if ((rider.inElevator && rider.elevator == e.elevator) ||
		(!rider.inElevator && rider.floor == e.floor)) {
		rider.events.add(e);
		rider.schedule(1);
	    }
	}
    }

    //电梯控制
    private class ElevatorManager implements ElevatorControls {
	ElevatorManager(ElevatorControllerInterface controller) {
	    this.controller = controller;

	    interrupt = new Runnable() { public void run() { interrupt(); }};
	}

	public int getNumFloors() {
	    return numFloors;
	}

	public int getNumElevators() {
	    return numElevators;
	}

	public void setInterruptHandler(Runnable handler) {
	    this.handler = handler;
	}

	public void openDoors(int elevator) {
	    Lib.assertTrue(elevator >= 0 && elevator < numElevators);
	    postRiderEvent(RiderEvent.eventDoorsOpened,
			   elevators[elevator].openDoors(), elevator);

	    if (gui != null) {
		if (elevators[elevator].direction == dirUp)
		    gui.clearUpButton(elevators[elevator].floor);
		else if (elevators[elevator].direction == dirDown)
		    gui.clearDownButton(elevators[elevator].floor);

		gui.openDoors(elevator);
	    }
	}

        public void closeDoors(int elevator) {
	    Lib.assertTrue(elevator >= 0 && elevator < numElevators);
	    postRiderEvent(RiderEvent.eventDoorsClosed,
			   elevators[elevator].closeDoors(), elevator);

	    if (gui != null)
		gui.closeDoors(elevator);
	}

	public boolean moveTo(int floor, int elevator) {
	    Lib.assertTrue(floor >= 0 && floor < numFloors);
	    Lib.assertTrue(elevator >= 0 && elevator < numElevators);

	    if (!elevators[elevator].moveTo(floor))
		return false;

	    schedule(Stats.ElevatorTicks);
	    return true;
	}

	public int getFloor(int elevator) {
	    Lib.assertTrue(elevator >= 0 && elevator < numElevators);
	    return elevators[elevator].floor;
	}

	public void setDirectionDisplay(int elevator, int direction) {
	    Lib.assertTrue(elevator >= 0 && elevator < numElevators);
	    elevators[elevator].direction = direction;

	    if (elevators[elevator].doorsOpen) {
		postRiderEvent(RiderEvent.eventDirectionChanged,
			       elevators[elevator].floor, elevator);
	    }

	    if (gui != null) {
		if (elevators[elevator].doorsOpen) {
		    if (direction == dirUp)
			gui.clearUpButton(elevators[elevator].floor);
		    else if (direction == dirDown)
			gui.clearDownButton(elevators[elevator].floor);
		}

		gui.setDirectionDisplay(elevator, direction);
	    }
	}

	public void finish() {
	    finished = true;

	    Lib.assertTrue(KThread.currentThread() == thread);

	    done.V();
	    KThread.finish();
	}

	public ElevatorEvent getNextEvent() {
	    if (events.isEmpty())
		return null;
	    else
		return (ElevatorEvent) events.removeFirst();
	}

	void schedule(int when) {
	    privilege.interrupt.schedule(when, "elevator", interrupt);
	}

        void postEvent(int event, int floor, int elevator, boolean schedule) {
	    events.add(new ElevatorEvent(event, floor, elevator));

	    if (schedule)
		schedule(1);
	}

	void interrupt() {
	    for (int i=0; i<numElevators; i++) {
		if (elevators[i].atNextFloor()) {
		    if (gui != null)
			gui.elevatorMoved(elevators[i].floor, i);

		    if (elevators[i].atDestination()) {
			postEvent(ElevatorEvent.eventElevatorArrived,
				  elevators[i].destination, i, false);
		    }
		    else {
			elevators[i].nextETA += Stats.ElevatorTicks;
			privilege.interrupt.schedule(Stats.ElevatorTicks,
						   "elevator",
						   interrupt);
		    }
		}
	    }

	    if (!finished && !events.isEmpty() && handler != null)
		handler.run();
	}

	void initialize() {
	    controller.initialize(this);
	}

	void run() {
	    thread = new KThread(controller);
	    thread.setName("elevator controller");
	    thread.fork();
	}

	void join() {
	    postEvent(ElevatorEvent.eventRidersDone, -1, -1, true);
	    done.P();
	}

	ElevatorControllerInterface controller;
	Runnable interrupt;
	KThread thread;

	Runnable handler = null;
	LinkedList<ElevatorEvent> events = new LinkedList<ElevatorEvent>();
	Semaphore done = new Semaphore(0);
	boolean finished = false;
    }

    //电梯状态
    private class ElevatorState {
	ElevatorState(int floor) {
	    this.floor = floor;
	    destination = floor;
	}

	int openDoors() {
	    Lib.assertTrue(!doorsOpen && !moving);
	    doorsOpen = true;
	    return floor;
	}

	int closeDoors() {
	    Lib.assertTrue(doorsOpen);
	    doorsOpen = false;
	    return floor;
	}

	boolean moveTo(int newDestination) {
	    Lib.assertTrue(!doorsOpen);

	    if (!moving) {
		// can't move to current floor
		if (floor == newDestination)
		    return false;

		destination = newDestination;
		nextETA = Machine.timer().getTime() + Stats.ElevatorTicks;

		moving = true;
		return true;
	    }
	    else {
		// moving, shouldn't be at destination
		Lib.assertTrue(floor != destination);

		// make sure it's ok to stop
		if ((destination > floor && newDestination <= floor) ||
		    (destination < floor && newDestination >= floor))
		    return false;

		destination = newDestination;
		return true;
	    }
	}

	boolean enter(RiderState rider, int onFloor) {
	    Lib.assertTrue(!riders.contains(rider));

	    if (!doorsOpen || moving || onFloor != floor ||
		riders.size() == maxRiders)
		return false;

	    riders.addElement(rider);
	    return true;
	}

	boolean exit(RiderState rider, int onFloor) {
	    Lib.assertTrue(riders.contains(rider));

	    if (!doorsOpen || moving || onFloor != floor)
		return false;

	    riders.removeElement(rider);
	    return true;
	}

	boolean atNextFloor() {
	    if (!moving || Machine.timer().getTime() < nextETA)
		return false;

	    Lib.assertTrue(destination != floor);
	    if (destination > floor)
		floor++;
	    else
		floor--;

	    for (Iterator i=riders.iterator(); i.hasNext(); ) {
		RiderState rider = (RiderState) i.next();

		rider.floor = floor;
	    }

	    return true;
	}

	boolean atDestination() {
	    if (!moving || destination != floor)
		return false;

	    moving = false;
	    return true;
	}

        static final int maxRiders = 4;

	int floor, destination;
	long nextETA;

	boolean doorsOpen = false, moving = false;
	int direction = dirNeither;
	public Vector<RiderState> riders = new Vector<RiderState>();
    }

    private class RiderState implements RiderControls {
	RiderState(RiderInterface rider, int floor, int[] stops) {
	    this.rider = rider;
	    this.floor = floor;
	    this.stops = stops;

	    interrupt = new Runnable() { public void run() { interrupt(); }};
	}

	public int getNumFloors() {
	    return numFloors;
	}

	public int getNumElevators() {
	    return numElevators;
	}

	public void setInterruptHandler(Runnable handler) {
	    this.handler = handler;
	}

	public int getFloor() {
	    return floor;
	}

	public int[] getFloors() {
	    int[] array = new int[floors.size()];
	    for (int i=0; i<array.length; i++)
		array[i] = ((Integer) floors.elementAt(i)).intValue();

	    return array;
	}

	public int getDirectionDisplay(int elevator) {
	    Lib.assertTrue(elevator >= 0 && elevator < numElevators);
	    return elevators[elevator].direction;
	}

	public RiderEvent getNextEvent() {
	    if (events.isEmpty())
		return null;
	    else
		return (RiderEvent) events.removeFirst();
	}

	public boolean pressDirectionButton(boolean up) {
	    if (up)
		return pressUpButton();
	    else
		return pressDownButton();
	}

	public boolean pressUpButton() {
	    Lib.assertTrue(!inElevator && floor < numFloors-1);

	    for (int elevator=0; elevator<numElevators; elevator++) {
		if (elevators[elevator].doorsOpen &&
		    elevators[elevator].direction == ElevatorBank.dirUp &&
		    elevators[elevator].floor == floor)
		    return false;
	    }

	    manager.postEvent(ElevatorEvent.eventUpButtonPressed,
			      floor, -1, true);

	    if (gui != null)
		gui.pressUpButton(floor);

	    return true;
	}

	public boolean pressDownButton() {
	    Lib.assertTrue(!inElevator && floor > 0);

	    for (int elevator=0; elevator<numElevators; elevator++) {
		if (elevators[elevator].doorsOpen &&
		    elevators[elevator].direction == ElevatorBank.dirDown &&
		    elevators[elevator].floor == floor)
		    return false;
	    }

	    manager.postEvent(ElevatorEvent.eventDownButtonPressed,
			      floor, -1, true);

	    if (gui != null)
		gui.pressDownButton(floor);

	    return true;
	}

	public boolean enterElevator(int elevator) {
	    Lib.assertTrue(!inElevator &&
		       elevator >= 0 && elevator < numElevators);
	    if (!elevators[elevator].enter(this, floor))
		return false;

	    if (gui != null)
		gui.enterElevator(floor, elevator);

	    inElevator = true;
	    this.elevator = elevator;
	    return true;
	}

	public boolean pressFloorButton(int floor) {
	    Lib.assertTrue(inElevator && floor >= 0 && floor < numFloors);

	    if (elevators[elevator].doorsOpen &&
		elevators[elevator].floor == floor)
		return false;

	    manager.postEvent(ElevatorEvent.eventFloorButtonPressed,
			      floor, elevator, true);

	    if (gui != null)
		gui.pressFloorButton(floor, elevator);

	    return true;
	}

	public boolean exitElevator(int floor) {
	    Lib.assertTrue(inElevator && floor >= 0 && floor < numFloors);

	    if (!elevators[elevator].exit(this, floor))
		return false;

	    inElevator = false;
	    floors.add(new Integer(floor));

	    if (gui != null)
		gui.exitElevator(floor, elevator);

	    return true;
	}

	public void finish() {
	    finished = true;

	    int[] floors = getFloors();
	    Lib.assertTrue(floors.length == stops.length);
	    for (int i=0; i<floors.length; i++)
		Lib.assertTrue(floors[i] == stops[i]);

	    Lib.assertTrue(KThread.currentThread() == thread);

	    done.V();
	    KThread.finish();
	}

	void schedule(int when) {
	    privilege.interrupt.schedule(when, "rider", interrupt);
	}

	void interrupt() {
	    if (!finished && !events.isEmpty() && handler != null)
		handler.run();
	}

	void initialize() {
	    rider.initialize(this, stops);
	}

	void run() {
	    thread = new KThread(rider);
	    thread.setName("rider");
	    thread.fork();
	}

	void join() {
	    done.P();
	}

    	RiderInterface rider;
	boolean inElevator = false, finished = false;
	int floor, elevator;
	int[] stops;
	Runnable interrupt, handler = null;
	LinkedList<RiderEvent> events = new LinkedList<RiderEvent>();
	Vector<Integer> floors = new Vector<Integer>();
	Semaphore done = new Semaphore(0);
	KThread thread;
    }

    private int numFloors, numElevators;
    private ElevatorManager manager;
    //电梯
    private ElevatorState[] elevators;

    private int numRiders;
    private Vector<RiderControls> ridersVector;
    private RiderState[] riders;
    
    private boolean simulationStarted, enableGui;
    private Privilege privilege;
    private ElevatorGui gui;
}
