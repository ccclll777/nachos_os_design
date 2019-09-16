// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

/**
 * An OS kernel.
 */
//每个阶段都有自己的内核子类
public abstract class Kernel {
    /** Globally accessible reference to the kernel. */
    //对内核的全局可访问引用
    public static Kernel kernel = null;

    /**
     * Allocate a new kernel.
     */
    public Kernel() {
	// make sure only one kernel is created
	Lib.assertTrue(kernel == null);	
	kernel = this;
    }

    /**
     * Initialize this kernel.
     */
    //初始化内核
    public abstract void initialize(String[] args);
    
    /**
     * Test that this module works.
     *
     * <b>Warning:</b> this method will not be invoked by the autograder when
     * we grade your projects. You should perform all initialization in
     * <tt>initialize()</tt>.
     */
    //执行测试  ag不使用
    public abstract void selfTest();
    
    /**
     * Begin executing user programs, if applicable.
     */

    //运行用户代码
    public abstract void run();

    /**
     * Terminate this kernel. Never returns.
     */

    //内核结束
    public abstract void terminate();
}

