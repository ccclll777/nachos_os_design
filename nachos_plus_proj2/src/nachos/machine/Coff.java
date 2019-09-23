// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

import java.io.EOFException;

/**
 * A COFF (common object file format) loader.
 */
//COFF（公共对象文件格式）加载程序
public class Coff {
    /**
     * Allocate a new Coff object.
     */

    protected Coff() {
	file = null;
	entryPoint = 0;
	sections = null;
    }
    
    /**
     * Load the COFF executable in the specified file.
     *
     * <p>
     * Notes:
     * <ol>
     * <li>If the constructor returns successfully, the file becomes the
     * property of this loader, and should not be accessed any further.
     * <li>The autograder expects this loader class to be used. Do not load
     * sections through any other mechanism.
     * <li>This loader will verify that the file is backed by a file system,
     * by asserting that read() operations take non-zero simulated time to
     * complete. Do not supply a file backed by a simulated cache (the primary
     * purpose of this restriction is to prevent sections from being loaded
     * instantaneously while handling page faults).
     * </ol>
     *
     * @param	file	the file containing the executable.
     * @exception EOFException    if the executable is corrupt.
     */
    //在指定的文件中加载COFF可执行文件
	//
    public Coff(OpenFile file) throws EOFException {
	this.file = file;

	//加载coff文件
		//如果构造函数成功返回，则该文件将成为此加载程序的属性，并且不应再被访问。
		//自动加载器希望使用此加载器类。不要通过任何其他机构加载截面。<li>此加载程序将通过断言read（）
		// 操作需要非零的模拟时间才能完成，来验证文件是否由文件系统支持。不要提供由模拟缓存支持的文件（
		// 此限制的主要目的是防止在处理页面错误时立即加载节）。
	Coff coff = Machine.autoGrader().createLoader(file);

	if (coff != null) {
		//第一条指令的虚拟地址
	    this.entryPoint = coff.entryPoint;
		//此COFF可执行文件中内容
	    this.sections = coff.sections;
	}
	else {
	    byte[] headers = new byte[headerLength+aoutHeaderLength];

	    if (file.length() < headers.length) {
		Lib.debug(dbgCoff, "\tfile is not executable");
		throw new EOFException();
	    }

	    Lib.strictReadFile(file, 0, headers, 0, headers.length);
	    
	    int magic = Lib.bytesToUnsignedShort(headers, 0);
	    int numSections = Lib.bytesToUnsignedShort(headers, 2);
	    int optionalHeaderLength = Lib.bytesToUnsignedShort(headers, 16);
	    int flags = Lib.bytesToUnsignedShort(headers, 18);
	    entryPoint = Lib.bytesToInt(headers, headerLength+16);

	    if (magic != 0x0162) {
		Lib.debug(dbgCoff, "\tincorrect magic number");
		throw new EOFException();
	    }
	    if (numSections < 2 || numSections > 10) {
		Lib.debug(dbgCoff, "\tbad section count");
		throw new EOFException();
	    }
	    if ((flags & 0x0003) != 0x0003) {
		Lib.debug(dbgCoff, "\tbad header flags");
		throw new EOFException();
	    }

	    int offset = headerLength + optionalHeaderLength;

	    sections = new CoffSection[numSections];
	    for (int s=0; s<numSections; s++) {
		int sectionEntryOffset = offset + s*CoffSection.headerLength;
		try {
		    sections[s] =
			new CoffSection(file, this, sectionEntryOffset);
		}
		catch (EOFException e) {
		    Lib.debug(dbgCoff, "\terror loading section " + s);
		    throw e;
		}
	    }
	}
    }

    /**
     * Return the number of sections in the executable.
     *
     * @return	the number of sections in the executable.
     */
    //返回可执行文件中的大小
    public int getNumSections() {
	return sections.length;
    }

    /**
     * Return an object that can be used to access the specified section. Valid
     * section numbers include <tt>0</tt> through <tt>getNumSections() -
     * 1</tt>.
     *
     * @param	sectionNumber	the section to select.
     * @return	an object that can be used to access the specified section.
     */
    //返回可用于访问指定区域的对象。有效的节编号包括<tt>0</tt>到<tt>getNumSections（）-1</tt>。
	//返回可执行文件的 某个字段
    public CoffSection getSection(int sectionNumber) {
	Lib.assertTrue(sectionNumber >= 0 && sectionNumber < sections.length);

	return sections[sectionNumber];
    }

    /**
     * Return the program entry point. This is the value that to which the PC
     * register should be initialized to before running the program.
     *
     * @return	the program entry point.
     */
    //返回程序入口点。这是在运行程序之前PC寄存器应该初始化到的值。
    public int getEntryPoint() {
	Lib.assertTrue(file != null);
	
	return entryPoint;
    }

    /**
     * Close the executable file and release any resources allocated by this
     * loader.
     */
    //关闭可执行文件并释放此加载程序分配的所有资源。
    public void close() {
	file.close();

	sections = null;
    }

    private OpenFile file;

    /** The virtual address of the first instruction of the program. */
    //程序第一条指令的虚拟地址
    protected int entryPoint;
    /** The sections in this COFF executable. */
    //此COFF可执行文件中内容
    protected CoffSection sections[];

    private static final int headerLength = 20;
    private static final int aoutHeaderLength = 28;

    private static final char dbgCoff = 'c';
}
