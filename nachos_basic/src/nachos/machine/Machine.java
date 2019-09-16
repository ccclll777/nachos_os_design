// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

import nachos.security.*;
import nachos.ag.*;

import java.io.File;

/**
 * The master class of the simulated machine. Processes command line arguments,
 * constructs all simulated hardware devices, and starts the grader.
 */

//项目入口 初始化 设备中断控制   时钟 mips处理器   控制台 文件系统
//将控制传递给自动加载器 ag文件（autograder）


//启动系统，并提供对各种硬件设备的访问：
//Machine.interrupt() –
//  Machine.timer()
//–  Machine.console()
//–  Machine.networkLink()


	//machine目录中的大多数类是硬件模拟的一部分，而该目录之外的所有类都是nachos操作系统的一部分。

public final class Machine {
    /**
     * Nachos main entry point.
     *
     * @param	args	the command line arguments.
     */
    public static void main(final String[] args) {
	System.out.print("nachos 5.0j initializing...");
	
	Lib.assertTrue(Machine.args == null);
	Machine.args = args;

	processArgs();

	//config加载配置文件名
	Config.load(configFileName);

	// get the current directory (.)
		//基本文件路径  当前文件路径的绝对路径
	baseDirectory = new File(new File("").getAbsolutePath());
	// get the nachos directory (./nachos)
		//nachos操作系统文件  绝对路径+nachos
	nachosDirectory = new File(baseDirectory, "nachos");

	//测试文件名   从配置文件读取
	String testDirectoryName =
	    Config.getString("FileSystem.testDirectory");

	// get the test directory
		//加载测试文件
	if (testDirectoryName != null) {
	    testDirectory = new File(testDirectoryName);
	}
	else {
	    // use ../test 如果没有则新建test 文件
	    testDirectory = new File(baseDirectory.getParentFile(), "test");
	}

	//安全配置文件
	securityManager = new NachosSecurityManager(testDirectory);

	//获取某个特权类 的实例
	privilege = securityManager.getPrivilege();

	///提供 使用  <tt>Machine</tt>中私有方法 的权限
	privilege.machine = new MachinePrivilege();

	//将TCB中的某个权限给privilege
	TCB.givePrivilege(privilege);
	//初始化privilege的状态为
	privilege.stats = stats;

	securityManager.enable();

	createDevices();
	checkUserClasses();

	autoGrader = (AutoGrader) Lib.constructObject(autoGraderClassName);

	//启动程序
	new TCB().start(new Runnable() {
	    public void run() {
	    	//启动测试程序
	    	autoGrader.start(privilege); }
	});
    }

    /**
     * Yield to non-Nachos threads. Use in non-preemptive JVM's to give
     * non-Nachos threads a chance to run.
     */
    //移交非nachos线程    在非抢占式jvm中使用，使非nachos线程有机会运行。
    public static void yield() {
	Thread.yield();
    }

    /**
     * Terminate Nachos. Same as <tt>TCB.die()</tt>.
     */
    //停止nachos
    public static void terminate() {
	TCB.die();
    }

    /**
     * Terminate Nachos as the result of an unhandled exception or error.
     *
     * @param	e	the exception or error.
     */
    //
	//由于未处理的异常或错误而终止nachos。
    public static void terminate(Throwable e) {
	if (e instanceof ThreadDeath)
	    throw (ThreadDeath) e;
	
	e.printStackTrace();
	terminate();
    }

    /**
     * Print stats, and terminate Nachos.
     */
    //打印统计数据并终止nachos。
    public static void halt() {
	System.out.print("Machine halting!\n\n");
	stats.print();
	terminate();
    }

    /**
     * Return an array containing all command line arguments.
     *
     * @return	the command line arguments passed to Nachos.
     */
    //返回包含所有命令行参数的数组。
    public static String[] getCommandLineArguments() {
	String[] result = new String[args.length];

	System.arraycopy(args, 0, result, 0, args.length);

	return result;
    }

    //执行 命令行 输入的某些参数
	//
    private static void processArgs() {
	for (int i=0; i<args.length; ) {
	    String arg = args[i++];

	    //如果第一位是-
	    if (arg.length() > 0 && arg.charAt(0) == '-') {
		if (arg.equals("-d")) {
			//如果两位是-d
		    Lib.assertTrue(i < args.length, "switch without argument");
		    //-d确定debug字符？
		    Lib.enableDebugFlags(args[i++]);
		}
		else if (arg.equals("-h")) {
			// 如果输入-h则打印help菜单
		    System.out.print(help);
		    System.exit(1);
		}
		else if (arg.equals("-m")) {
		    Lib.assertTrue(i < args.length, "switch without argument");
		    try {
		    	//将转化为int -m 设置某个参数 分页大小？
			numPhysPages = Integer.parseInt(args[i++]);
		    }
		    catch (NumberFormatException e) {
			Lib.assertNotReached("bad value for -m switch");
		    }
		}
		else if (arg.equals("-s")) {
		    Lib.assertTrue(i < args.length, "switch without argument");
		    try {
		    	//-s设置随机数种子
			randomSeed = Long.parseLong(args[i++]);
		    }
		    catch (NumberFormatException e) {
			Lib.assertNotReached("bad value for -s switch");
		    }
		}
		else if (arg.equals("-x")) {
		    Lib.assertTrue(i < args.length, "switch without argument");
		    //-x 设置shell名称？
		    shellProgramName = args[i++];		    
		}		    
		else if (arg.equals("-z")) {
			//-z打印  copyright
		    System.out.print(copyright);
		    System.exit(1);
		}
		// these switches are reserved for the autograder
		else if (arg.equals("-[]")) {
		    Lib.assertTrue(i < args.length, "switch without argument");
		    //-【】设置配置文件名？
		    configFileName = args[i++];
		}
		else if (arg.equals("--")) {
		    Lib.assertTrue(i < args.length, "switch without argument");
		    //--设置  nachos.ag.AutoGrader类名
		    autoGraderClassName = args[i++];
		}
	    }
	}

	Lib.seedRandom(randomSeed);
    }

    private static void createDevices() {
    	//传入中断的特权类
	interrupt = new Interrupt(privilege);

	timer = new Timer(privilege);

	if (Config.getBoolean("Machine.bank"))
	    bank = new ElevatorBank(privilege);

	if (Config.getBoolean("Machine.processor")) {
	    if (numPhysPages == -1)
	    	//要附加的物理内存页数。
		numPhysPages = Config.getInteger("Processor.numPhysPages");

	    processor = new Processor(privilege, numPhysPages);
	}				      

	if (Config.getBoolean("Machine.console"))
	    console = new StandardConsole(privilege);

	if (Config.getBoolean("Machine.stubFileSystem"))
	    stubFileSystem = new StubFileSystem(privilege, testDirectory);

	if (Config.getBoolean("Machine.networkLink"))
	    networkLink = new NetworkLink(privilege);
    }

    private static void checkUserClasses() {
	System.out.print(" user-check");
	
	Class aclsInt = (new int[0]).getClass();
	Class clsObject = Lib.loadClass("java.lang.Object");
	Class clsRunnable = Lib.loadClass("java.lang.Runnable");
	Class clsString = Lib.loadClass("java.lang.String");

	Class clsKernel = Lib.loadClass("nachos.machine.Kernel");
	Class clsFileSystem = Lib.loadClass("nachos.machine.FileSystem");
	Class clsRiderControls = Lib.loadClass("nachos.machine.RiderControls");
	Class clsElevatorControls =
	    Lib.loadClass("nachos.machine.ElevatorControls");
	Class clsRiderInterface =
	    Lib.loadClass("nachos.machine.RiderInterface");
	Class clsElevatorControllerInterface =
	    Lib.loadClass("nachos.machine.ElevatorControllerInterface");

	Class clsAlarm = Lib.loadClass("nachos.threads.Alarm");
	Class clsThreadedKernel =
	    Lib.loadClass("nachos.threads.ThreadedKernel");
	Class clsKThread = Lib.loadClass("nachos.threads.KThread");
	Class clsCommunicator = Lib.loadClass("nachos.threads.Communicator");
	Class clsSemaphore = Lib.loadClass("nachos.threads.Semaphore");
	Class clsLock = Lib.loadClass("nachos.threads.Lock");
	Class clsCondition = Lib.loadClass("nachos.threads.Condition");
	Class clsCondition2 = Lib.loadClass("nachos.threads.Condition2");
	Class clsRider = Lib.loadClass("nachos.threads.Rider");
	Class clsElevatorController =
	    Lib.loadClass("nachos.threads.ElevatorController");

	Lib.checkDerivation(clsThreadedKernel, clsKernel);
	
	Lib.checkStaticField(clsThreadedKernel, "alarm", clsAlarm);
	Lib.checkStaticField(clsThreadedKernel, "fileSystem", clsFileSystem);
	
	Lib.checkMethod(clsAlarm, "waitUntil", new Class[] { long.class },
			void.class);
	
	Lib.checkConstructor(clsKThread, new Class[] { });
	Lib.checkConstructor(clsKThread, new Class[] { clsRunnable });

	Lib.checkStaticMethod(clsKThread, "currentThread", new Class[] {},
			      clsKThread);
	Lib.checkStaticMethod(clsKThread, "finish", new Class[] {},
			      void.class);
	Lib.checkStaticMethod(clsKThread, "yield", new Class[] {}, void.class);
	Lib.checkStaticMethod(clsKThread, "sleep", new Class[] {}, void.class);

	Lib.checkMethod(clsKThread, "setTarget", new Class[]{ clsRunnable },
			clsKThread);
	Lib.checkMethod(clsKThread, "setName", new Class[] { clsString },
			clsKThread);
	Lib.checkMethod(clsKThread, "getName", new Class[] { }, clsString);
	Lib.checkMethod(clsKThread, "fork", new Class[] { }, void.class);
	Lib.checkMethod(clsKThread, "ready", new Class[] { }, void.class);
	Lib.checkMethod(clsKThread, "join", new Class[] { }, void.class);

	Lib.checkField(clsKThread, "schedulingState", clsObject);

	Lib.checkConstructor(clsCommunicator, new Class[] {});
	Lib.checkMethod(clsCommunicator, "speak", new Class[] { int.class },
			void.class);
	Lib.checkMethod(clsCommunicator, "listen", new Class[] { }, int.class);
	
	Lib.checkConstructor(clsSemaphore, new Class[] { int.class });
	Lib.checkMethod(clsSemaphore, "P", new Class[] { }, void.class);
	Lib.checkMethod(clsSemaphore, "V", new Class[] { }, void.class);
	
	Lib.checkConstructor(clsLock, new Class[] { });
	Lib.checkMethod(clsLock, "acquire", new Class[] { }, void.class);
	Lib.checkMethod(clsLock, "release", new Class[] { }, void.class);
	Lib.checkMethod(clsLock, "isHeldByCurrentThread", new Class[]{ },
			boolean.class);

	Lib.checkConstructor(clsCondition, new Class[] { clsLock });
	Lib.checkConstructor(clsCondition2, new Class[] { clsLock });

	Lib.checkMethod(clsCondition, "sleep", new Class[] { }, void.class);
	Lib.checkMethod(clsCondition, "wake", new Class[] { }, void.class);
	Lib.checkMethod(clsCondition, "wakeAll", new Class[] { }, void.class);
	Lib.checkMethod(clsCondition2, "sleep", new Class[] { }, void.class);
	Lib.checkMethod(clsCondition2, "wake", new Class[] { }, void.class);
	Lib.checkMethod(clsCondition2, "wakeAll", new Class[] { }, void.class);

	Lib.checkDerivation(clsRider, clsRiderInterface);

	Lib.checkConstructor(clsRider, new Class[] { });
	Lib.checkMethod(clsRider, "initialize",
			new Class[] { clsRiderControls, aclsInt }, void.class);

	Lib.checkDerivation(clsElevatorController,
			    clsElevatorControllerInterface);

	Lib.checkConstructor(clsElevatorController, new Class[] { });
	Lib.checkMethod(clsElevatorController, "initialize",
			new Class[] { clsElevatorControls }, void.class);
    }

    /**
     * Prevent instantiation.
     */
    private Machine() {
    }

    /**
     * Return the hardware interrupt manager.
     *
     * @return	the hardware interrupt manager.
     */
    //返回中断管理的实例
    public static Interrupt interrupt() { return interrupt; }
    
    /**
     * Return the hardware timer.
     *
     * @return	the hardware timer.
     */
    //返回Timer实例
    public static Timer timer() { return timer; }
    
    /**
     * Return the hardware elevator bank.
     *
     * @return	the hardware elevator bank, or <tt>null</tt> if it is not
     * 		present.
     */
    //返回elevator实例
    public static ElevatorBank bank() { return bank; }
    
    /**
     * Return the MIPS processor.
     *
     * @return	the MIPS processor, or <tt>null</tt> if it is not present.
     */     
    public static Processor processor() { return processor; }
    
    /**
     * Return the hardware console.
     *
     * @return	the hardware console, or <tt>null</tt> if it is not present.
     */
    //
    public static SerialConsole console() { return console; }
    
    /**
     * Return the stub filesystem.
     *
     * @return	the stub file system, or <tt>null</tt> if it is not present.
     */
    public static FileSystem stubFileSystem() { return stubFileSystem; }
    
    /**
     * Return the network link.
     *
     * @return	the network link,  or <tt>null</tt> if it is not present.
     */
    public static NetworkLink networkLink() { return networkLink; }
    
    /**
     * Return the autograder.
     *
     * @return	the autograder.
     */
    public static AutoGrader autoGrader() { return autoGrader; }

    private static Interrupt interrupt = null;
    private static Timer timer = null;
    private static ElevatorBank bank = null;
    private static Processor processor = null;
    private static SerialConsole console = null;
    private static FileSystem stubFileSystem = null;
    private static NetworkLink networkLink = null;
    private static AutoGrader autoGrader = null;

    private static String autoGraderClassName = "nachos.ag.AutoGrader";

    /**
     * Return the name of the shell program that a user-programming kernel
     * must run. Make sure <tt>UserKernel.run()</tt> <i>always</i> uses this
     * method to decide which program to run.
     *
     * @return	the name of the shell program to run.
     */
    //返回用户编程内核必须运行的shell程序的名称。确保始终使用此方法来决定要运行的程序。
    public static String getShellProgramName() {
	if (shellProgramName == null)
	    shellProgramName = Config.getString("Kernel.shellProgram");

	Lib.assertTrue(shellProgramName != null);
	return shellProgramName;
    }

    private static String shellProgramName = null;

    /**
     * Return the name of the process class that the kernel should use. In
     * the multi-programming project, returns
     * <tt>nachos.userprog.UserProcess</tt>. In the VM project, returns
     * <tt>nachos.vm.VMProcess</tt>. In the networking project, returns
     * <tt>nachos.network.NetProcess</tt>.
     *
     * @return	the name of the process class that the kernel should use.
     *
     * @see	nachos.userprog.UserKernel#run
     * @see	nachos.userprog.UserProcess
     * @see	nachos.vm.VMProcess
     * @see	nachos.network.NetProcess
     */
    //返回内核应该使用的进程类的名称。在multi-programming项目中，返回<tt>nachos.userprog.userprocess</tt>。
	// 在vm项目中，返回<tt>nachos.vm.vmprocess</tt>。在网络项目中，返回
	//<tt>nachos.network.netprocess</tt>。
    public static String getProcessClassName() {
	if (processClassName == null)
	    processClassName = Config.getString("Kernel.processClassName");

	Lib.assertTrue(processClassName != null);
	return processClassName;
    }

    private static String processClassName = null;

    //nachos的安全管理类
    private static NachosSecurityManager securityManager;
    //特权
    private static Privilege privilege;

    private static String[] args = null;

    private static Stats stats = new Stats();

	//要附加的物理内存页数。
    private static int numPhysPages = -1;
    private static long randomSeed = 0;

    private static File baseDirectory, nachosDirectory, testDirectory;
    private static String configFileName = "nachos.conf";

    private static final String help =
	"\n" +
	"Options:\n" +
	"\n" +
	"\t-d <debug flags>\n" +
	"\t\tEnable some debug flags, e.g. -d ti\n" +
	"\n" +
	"\t-h\n" +
	"\t\tPrint this help message.\n" +
	"\n" +
	"\t-m <pages>\n" +
	"\t\tSpecify how many physical pages of memory to simulate.\n" +
	"\n" +
	"\t-s <seed>\n" +
	"\t\tSpecify the seed for the random number generator (seed is a\n" +
	"\t\tlong).\n" +
	"\n" +
	"\t-x <program>\n" +
	"\t\tSpecify a program that UserKernel.run() should execute,\n" +
	"\t\tinstead of the value of the configuration variable\n" +
	"\t\tKernel.shellProgram\n" +
	"\n" +
	"\t-z\n" +
	"\t\tprint the copyright message\n" +
	"\n" +
	"\t-- <grader class>\n" +
	"\t\tSpecify an autograder class to use, instead of\n" +
	"\t\tnachos.ag.AutoGrader\n" +
	"\n" +
	"\t-# <grader arguments>\n" +
	"\t\tSpecify the argument string to pass to the autograder.\n" +
	"\n" +
	"\t-[] <config file>\n" +
	"\t\tSpecifiy a config file to use, instead of nachos.conf\n" +
	""
	;

    private static final String copyright = "\n"
	+ "Copyright 1992-2001 The Regents of the University of California.\n"
	+ "All rights reserved.\n"
	+ "\n"
	+ "Permission to use, copy, modify, and distribute this software and\n"
	+ "its documentation for any purpose, without fee, and without\n"
	+ "written agreement is hereby granted, provided that the above\n"
	+ "copyright notice and the following two paragraphs appear in all\n"
	+ "copies of this software.\n"
	+ "\n"
	+ "IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY\n"
	+ "PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL\n"
	+ "DAMAGES ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS\n"
	+ "DOCUMENTATION, EVEN IF THE UNIVERSITY OF CALIFORNIA HAS BEEN\n"
	+ "ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n"
	+ "\n"
	+ "THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY\n"
	+ "WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES\n"
	+ "OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  THE\n"
	+ "SOFTWARE PROVIDED HEREUNDER IS ON AN \"AS IS\" BASIS, AND THE\n"
	+ "UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE\n"
	+ "MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.\n"
	;

    private static class MachinePrivilege
	implements Privilege.MachinePrivilege {
	public void setConsole(SerialConsole console) {
	    Machine.console = console;
	}
    }

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
}
