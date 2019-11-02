// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

import java.io.EOFException;
import java.util.Arrays;

/**
 * A <tt>CoffSection</tt> manages a single section within a COFF executable.
 */
//coff section管理coff可执行文件中的单个部分。
public class CoffSection {
    /**
     * Allocate a new COFF section with the specified parameters.
     *
     * @param    coff        the COFF object to which this section belongs. 属于那个coff
     * @param    name        the COFF name of this section.  //此部分的coff名称
     * @param    executable    <tt>true</tt> if this section contains code. //
     * @param    readOnly    <tt>true</tt> if this section is read-only.
     * @param    numPages    the number of virtual pages in this section.//此部分的虚拟页数
     * @param    firstVPN    the first virtual page number used by this.
     */


    protected CoffSection(Coff coff, String name, boolean executable,
                          boolean readOnly, int numPages, int firstVPN) {
        this.coff = coff;
        this.name = name;
        this.executable = executable;
        this.readOnly = readOnly;
        this.numPages = numPages;
        this.firstVPN = firstVPN;

        file = null;
        size = 0;
        contentOffset = 0;
        initialized = true;
    }

    /**
     * Load a COFF section from an executable.
     *
     * @throws EOFException if an error occurs.
     * @param    file        the file containing the executable.   包含可执行文件的文件
     * @param    headerOffset    the offset of the section header in the
     * executable.  可执行文件中头文件的偏移量。
     */
    //从可执行文件加载COFF 部分
    public CoffSection(OpenFile file, Coff coff,
                       int headerOffset) throws EOFException {
        this.file = file;
        this.coff = coff;

        Lib.assertTrue(headerOffset >= 0);
        if (headerOffset + headerLength > file.length()) {
            Lib.debug(dbgCoffSection, "\tsection header truncated");
            throw new EOFException();
        }

        byte[] buf = new byte[headerLength];
        Lib.strictReadFile(file, headerOffset, buf, 0, headerLength);

        name = Lib.bytesToString(buf, 0, 8);
        int vaddr = Lib.bytesToInt(buf, 12);
        size = Lib.bytesToInt(buf, 16);
        contentOffset = Lib.bytesToInt(buf, 20);
        int numRelocations = Lib.bytesToUnsignedShort(buf, 32);
        int flags = Lib.bytesToInt(buf, 36);

        if (numRelocations != 0) {
            Lib.debug(dbgCoffSection, "\tsection needs relocation");
            throw new EOFException();
        }

        //每一部分有不同的 作用  是否只读  是否可执行
        switch (flags & 0x0FFF) {
            case 0x0020:
                executable = true;
                readOnly = true;
                initialized = true;
                break;
            case 0x0040:
                executable = false;
                readOnly = false;
                initialized = true;
                break;
            case 0x0080:
                executable = false;
                readOnly = false;
                initialized = false;
                break;
            case 0x0100:
                executable = false;
                readOnly = true;
                initialized = true;
                break;
            default:
                Lib.debug(dbgCoffSection, "\tinvalid section flags: " + flags);
                throw new EOFException();
        }

        if (vaddr % Processor.pageSize != 0 || size < 0 ||
                initialized && (contentOffset < 0 ||
                        contentOffset + size > file.length())) {
            Lib.debug(dbgCoffSection, "\tinvalid section addresses: " +
                    "vaddr=" + vaddr + " size=" + size +
                    " contentOffset=" + contentOffset);
            throw new EOFException();
        }

        numPages = Lib.divRoundUp(size, Processor.pageSize);
        firstVPN = vaddr / Processor.pageSize;
    }

    /**
     * Return the COFF object used to load this executable instance.
     *
     * @return the COFF object corresponding to this section.
     */
    //返回用于加载此可执行实例的COFF对象。
    public Coff getCoff() {
        return coff;
    }

    /**
     * Return the name of this section.
     *
     * @return the name of this section.
     */
    //返回此部分的名称
    public String getName() {
        return name;
    }

    /**
     * Test whether this section is read-only.
     *
     * @return    <tt>true</tt> if this section should never be written.
     */
    //测试此部分是否为只读。
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Test whether this section is initialized. Loading a page from an
     * initialized section requires a disk access, while loading a page from an
     * uninitialized section requires only zero-filling the page.
     *
     * @return    <tt>true</tt> if this section contains initialized data in the
     * executable.
     */
    //测试此部分是否已初始化。从初始化的section加载页需要磁盘访问权限，而从未初始化的节加载页只需要零填充页。
    public boolean isInitialzed() {
        return initialized;
    }

    /**
     * Return the length of this section in pages.
     *
     * @return the number of pages in this section.
     */
    //返回此部分的页数
    public int getLength() {
        return numPages;
    }

    /**
     * Return the first virtual page number used by this section.
     *
     * @return the first virtual page number used by this section.
     */
    //返回此部分使用的第一个虚拟页数
    public int getFirstVPN() {
        return firstVPN;
    }

    /**
     * Load a page from this segment into physical memory.
     *
     * @param    spn    the page number within this segment.   此段中的页码
     * @param    ppn    the physical page to load into.  要加载到的物理页。
     */
    //将此段中的页加载到物理内存中。
    public void loadPage(int spn, int ppn) {
        Lib.assertTrue(file != null);

        Lib.assertTrue(spn >= 0 && spn < numPages);
        Lib.assertTrue(ppn >= 0 && ppn < Machine.processor().getNumPhysPages());

        int pageSize = Processor.pageSize;
        byte[] memory = Machine.processor().getMemory();
        int paddr = ppn * pageSize;
        int faddr = contentOffset + spn * pageSize;
        int initlen;

        if (!initialized)
            initlen = 0;
        else if (spn == numPages - 1)
        /** initlen = size % pageSize;
         *  Bug identified by Steven Schlansker 3/20/08
         *  Bug fix by Michael Rauser
         */
            initlen = (size == pageSize) ? pageSize : (size % pageSize);
        else
            initlen = pageSize;

        if (initlen > 0)
            Lib.strictReadFile(file, faddr, memory, paddr, initlen);

        Arrays.fill(memory, paddr + initlen, paddr + pageSize, (byte) 0);
    }

    /**
     * The COFF object to which this section belongs.
     */
    //此部分所属的coff文件
    protected Coff coff;
    /**
     * The COFF name of this section.
     */
    //此部分的coff名称
    protected String name;
    /**
     * True if this section contains code.
     */
    //如果此部分包含代码 则为true
    protected boolean executable;

    /**
     * True if this section is read-only.
     */
    //如果此部分 只读  则为真
    protected boolean readOnly;

    /**
     * True if this section contains initialized data.
     */
    //如果此部分包含初始化的数据 则为真
    protected boolean initialized;

    /**
     * The number of virtual pages in this section.
     */
    //此部分的虚拟页数
    protected int numPages;

    /**
     * The first virtual page number used by this section.
     */
    //此部分所需要的第一个虚拟页码
    protected int firstVPN;

    private OpenFile file;
    private int contentOffset, size;

    /**
     * The length of a COFF section header.
     */
    //此coff  section 头的大小
    public static final int headerLength = 40;

    private static final char dbgCoffSection = 'c';
}
