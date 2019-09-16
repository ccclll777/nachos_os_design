// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.security;

import nachos.machine.Config;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.TCB;

import java.awt.*;
import java.io.File;
import java.io.FilePermission;
import java.net.NetPermission;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.PropertyPermission;

/**
 * Protects the environment from malicious Nachos code.
 */
//保护环境免受恶意nachos代码的攻击
	//安全管理器是一个允许应用程序实现安全策略的类。它允许应用程序在执行一个可能不安全或敏感的操作前确定该操作是什么，以及是否是在允许执行该操作的安全上下文中执行它。应用程序可以允许或不允许该操作
public class NachosSecurityManager extends SecurityManager {
    /**
     * Allocate a new Nachos security manager.
     *
     * @param	testDirectory	the directory usable by the stub file system.
     */
    //构造函数  新文件
    public NachosSecurityManager(File testDirectory) {
	this.testDirectory = testDirectory;

	//读取配置文件信息
	fullySecure = Config.getBoolean("NachosSecurityManager.fullySecure");
    }
	
    /**
     * Return a privilege object for this security manager. This security
     * manager must not be the active security manager.
     *
     * @return	a privilege object for this security manager.
     */
    //返回此安全管理器的特权对象。
	//此安全管理器不能是活动的安全管理器。
	//
    public Privilege getPrivilege() {
	Lib.assertTrue(this != System.getSecurityManager());

	return new PrivilegeProvider();
    }

    /**
     * Install this security manager.
     */
    //安装安全管理器
    public void enable() {
	Lib.assertTrue(this != System.getSecurityManager());
	
	doPrivileged(new Runnable() {
	    public void run() {
		System.setSecurityManager(NachosSecurityManager.this);
	    }
	});
    }

    //特权提供者  java的安全模型

    private class PrivilegeProvider extends Privilege {
    	//开启某个线程 给某个线程更大的安全权限
	public void doPrivileged(Runnable action) {
	    NachosSecurityManager.this.doPrivileged(action);
	}

	//
	public Object doPrivileged(PrivilegedAction action) {
	    return NachosSecurityManager.this.doPrivileged(action);
	}

	public Object doPrivileged(PrivilegedExceptionAction action)
	    throws PrivilegedActionException {
	    return NachosSecurityManager.this.doPrivileged(action);
	}

	// //调用每一个 执行exit的通知程序  观察者模式？？
	public void exit(int exitStatus) {
	    invokeExitNotificationHandlers();
	    NachosSecurityManager.this.exit(exitStatus);
	}
    }

    //确定权限？
    private void enablePrivilege() {
	if (privilegeCount == 0) {
	    Lib.assertTrue(privileged == null);
	    //返回当前代码执行的线程引用
	    privileged = Thread.currentThread();
	    privilegeCount++;
	}
	else {
	    Lib.assertTrue(privileged == Thread.currentThread());
	    privilegeCount++;
	}
    }

    //如果 有错误发生 则减少一个特权？？
    private void rethrow(Throwable e) {
	disablePrivilege();
	
	if (e instanceof RuntimeException)
	    throw (RuntimeException) e;
	else if (e instanceof Error)
	    throw (Error) e;
	else
	    Lib.assertNotReached();	
    }

    //较少特权数量？
    private void disablePrivilege() {
	Lib.assertTrue(privileged != null && privilegeCount > 0);
	privilegeCount--;
	if (privilegeCount == 0)
	    privileged = null;
    }

    //将当前的线程 变为 特权线程？
    private void forcePrivilege() {
	privileged = Thread.currentThread();
	privilegeCount = 1;
    }

    private void exit(int exitStatus) {
	forcePrivilege();
	System.exit(exitStatus);
    }

    private boolean isPrivileged() {
	// the autograder does not allow non-Nachos threads to be created, so..
		//自动加载器不允许创建非nachos线程，因此…  把它变成特权线程
	if (!TCB.isNachosThread())
	    return true;
	
	return (privileged == Thread.currentThread());
    }

    private void doPrivileged(final Runnable action) {
	doPrivileged(new PrivilegedAction() {
	    public Object run() { action.run(); return null; }
	});
    }

    private Object doPrivileged(PrivilegedAction action) {
	Object result = null;
	enablePrivilege();
	try {
	    result = action.run();
	}
	catch (Throwable e) {
	    rethrow(e);
	}
	disablePrivilege();
	return result;
    }

    private Object doPrivileged(PrivilegedExceptionAction action)
	throws PrivilegedActionException {
	Object result = null;
	enablePrivilege();
	try {
	    result = action.run();
	}
	catch (Exception e) {
	    throw new PrivilegedActionException(e);
	}
	catch (Throwable e) {
	    rethrow(e);
	}
	disablePrivilege();
	return result;
    }
    
    private void no() {
	throw new SecurityException();
    }

    //抛出未经许可的错误
    private void no(Permission perm) {
	System.err.println("\n\nLacked permission: " + perm);
	throw new SecurityException();
    }

    /**
     * Check the specified permission. Some operations are permissible while
     * not grading. These operations are regulated here.
     *
     * @param	perm	the permission to check.
     */
    //检查指定的权限
	//某些操作在不分级的情况下是允许的。这些操作在这里受到管制
    public void checkPermission(Permission perm) {
	String name = perm.getName();
	
	// some permissions are strictly forbidden
		//某些权限被严格禁止
		//如果是RuntimePermission的子类
	if (perm instanceof RuntimePermission) {
	    // no creating class loaders
	    if (name.equals("createClassLoader"))
			//抛出未经许可的错误
		no(perm);
	}
	
	// allow the AWT mess when not grading
		//不分级时允许AWT混乱
	if (!fullySecure) {
	    if (perm instanceof NetPermission) {
		// might be needed to load awt stuff
		if (name.equals("specifyStreamHandler"))
		    return;
	    }

	    if (perm instanceof RuntimePermission) {
		// might need to load libawt
		if (name.startsWith("loadLibrary.")) {
		    String lib = name.substring("loadLibrary.".length());
		    if (lib.equals("awt")) {
			Lib.debug(dbgSecurity, "\tdynamically linking " + lib);
			return;
		    }
		}
	    }

	    if (perm instanceof AWTPermission) {
		// permit AWT stuff 运行awt xxx
		if (name.equals("accessEventQueue"))
		    return;
	    }
	}

	// some are always allowed
		//某些一直被允许
	if (perm instanceof PropertyPermission) {
	    // allowed to read properties
	    if (perm.getActions().equals("read"))
		return;
	}

	// some require some more checking
		//某些需要检查
	if (perm instanceof FilePermission) {
	    if (perm.getActions().equals("read")) {
		// the test directory can only be read with privilege
		if (isPrivileged())
		    return;

		enablePrivilege();

		// not allowed to read test directory directly w/out privilege
			//不允许使用w/out权限读取测试目录
		try {
		    File f = new File(name);
		    if (f.isFile()) {
			File p = f.getParentFile();
			if (p != null) {
			    if (p.equals(testDirectory))
				no(perm);
			}
		    }
		}
		catch (Throwable e) {
		    rethrow(e);
		}

		//删除某个权限
		disablePrivilege();
		return;
	    }
	    else if (perm.getActions().equals("write") ||
		     perm.getActions().equals("delete")) {
		// only allowed to write test diretory, and only with privilege
			//允许有特权的线程 写测试文件
		verifyPrivilege();

		try {
		    File f = new File(name);
		    if (f.isFile()) {
			File p = f.getParentFile();
			if (p != null && p.equals(testDirectory))
			    return;
		    }
		}
		catch (Throwable e) {
		    no(perm);
		}		    
	    }
	    else if (perm.getActions().equals("execute")) {
		// only allowed to execute with privilege, and if there's a net
			//有网络  允许特权指令执行
		verifyPrivilege();

		if (Machine.networkLink() == null)
		    no(perm);
	    }
	    else {
		no(perm);
	    }
	}

	// default to requiring privilege
	verifyPrivilege(perm);
    }

    /**
     * Called by the <tt>java.lang.Thread</tt> constructor to determine a
     * thread group for a child thread of the current thread. The caller must
     * be privileged in order to successfully create the thread.
     *
     * @return	a thread group for the new thread, or <tt>null</tt> to use the
     *	        current	thread's thread group.
     */
    //由java.lang.Thread构造函数调用 以确定一个当前线程的子线程的线程组
	//调用者必须有特权才能成个创建线程
    public ThreadGroup getThreadGroup() {
	verifyPrivilege();
	return null;
    }

    /**
     * Verify that the caller is privileged.
     */
    //检查是否有特权
    public void verifyPrivilege() {
	if (!isPrivileged())
	    no();
    }

    /**
     * Verify that the caller is privileged, so as to check the specified
     * permission.
     *
     * @param	perm	the permission being checked.
     */
    //验证调用方是否具有特权
	//以便检查指定的权限
    public void verifyPrivilege(Permission perm) {
	if (!isPrivileged())
	    no(perm);
    }

    private File testDirectory;
    private boolean fullySecure;

    private Thread privileged = null;
    private int privilegeCount = 0;
    
    private static final char dbgSecurity = 'S';
}
