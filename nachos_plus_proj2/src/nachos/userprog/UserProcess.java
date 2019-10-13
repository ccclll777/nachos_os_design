package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import java.io.EOFException;
import java.util.Hashtable;
import java.util.LinkedList;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
//用户进程；管理地址空间，并将程序加载到虚拟内存中。

/**
 * 封装未包含在其用户线程（或多个线程）中的用户进程的状态。这包括其地址转换状态、文件表和有关正在执行的程序的信息。
 * <p>
 * 该类由其他类扩展以支持附加功能（例如附加系统调用）。
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    protected int pid;//当前用户进程的id  作为进程的唯一标识
    protected static int processCount = 0;//所有的进程数量
    //用Hashtable来存放 所有的用户进程以及对应的编号  Hashtable的方法是Synchronize的 可以解决并发问题 （1）确保线程互斥的访问同步代码（2）保证共享变量的修改能够及时可见（3）有效解决重排序问题。
    private static Hashtable<Integer, UserProcess> userProcessHashtable = new Hashtable<Integer, UserProcess>();


    protected static final int maxLength = 256;//最大长度
    protected static final int MaxFileDescriptor = 16;//每个用户进程最多拥有16的文件描述符
    protected static final int stdin = 0;//标准输入为0
    protected static final int stdout = 1;//标准输出为1

    private FileDescriptor FileDescriptors[] = new FileDescriptor[MaxFileDescriptor];//此用户进程打开的所有文件列表

    private LinkedList<Integer> childProcesses = new LinkedList<Integer>();//存放子进程进程号的数组


    private UThread thread;//对应的用户线程


    private int exitStatus;//退出状态


    public UserProcess() {
        //确保每个pid对应一个进程
        Machine.interrupt().disable();
        UserKernel.processIDSem.P();
        //给每一个运行的进程一个唯一的编号
        pid = processCount++;
        UserKernel.processIDSem.V();
        Machine.interrupt().enable();

        userProcessHashtable.put(pid, this);


        //初始化 文件描述符数组
        for (int i = 0; i < MaxFileDescriptor; ++i) {
            FileDescriptors[i] = new FileDescriptor();
        }
        //物理页
        int numPhysPages = Machine.processor().getNumPhysPages();
        //页表初始化
        pageTable = new TranslationEntry[numPhysPages];
        for (int i = 0; i < numPhysPages; i++)
            pageTable[i] = new TranslationEntry(i, i, false, false, false, false);

        FileDescriptors[stdin].setFile(UserKernel.console.openForReading());//0  为stdin
        FileDescriptors[stdout].setFile(UserKernel.console.openForWriting()); //1  为stdout的文件描述符
    }

    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return a new process of the correct class.
     */
    //分配并返回正确类的新进程。
    // 类名由<tt>nachos.conf</tt>键<tt>kernel.processclassname</tt>指定。
    public static UserProcess newUserProcess() {
        return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
    }


    //分配页表资源
    protected boolean allocate(int vpn, int acquirePagesNum, boolean readOnly) {
        LinkedList<TranslationEntry> allocated = new LinkedList<TranslationEntry>();

        for (int i = 0; i < acquirePagesNum; ++i) {
            if (vpn >= pageTable.length)
                return false;

            int ppn = UserKernel.getFreePage();
            if (ppn == -1) {
                Lib.debug(dbgProcess, "\t没有物理页可以分配");

                //如果没有物理页进行分配 则从已分配的页中 释放资源
                for (TranslationEntry translationEntry: allocated) {
                    pageTable[translationEntry.vpn] = new TranslationEntry(translationEntry.vpn, 0, false, false, false, false);
                    UserKernel.addFreePage(translationEntry.ppn);
                    numPages--;
                }

                return false;
            } else {
                //否则直接分配资源
                TranslationEntry a = new TranslationEntry(vpn + i,
                        ppn, true, readOnly, false,false);
                allocated.add(a);
                pageTable[vpn + i] = a;
                ++numPages;
            }
        }
        return true;
    }

    //如果 所有的物理页都被分配 则 释放在主存的的那些页
    protected void releaseResource() {
        for (int i = 0; i < pageTable.length; ++i)
            if (pageTable[i].valid) {
                UserKernel.addFreePage(pageTable[i].ppn);
                pageTable[i] = new TranslationEntry(pageTable[i].vpn, 0, false, false, false, false);
            }
        numPages = 0;
    }

    protected TranslationEntry AllocatePageTable(int vpn) {
        if (pageTable == null)
            return null;

        if (vpn >= 0 && vpn < pageTable.length)
            return pageTable[vpn];
        else
            return null;
    }
    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the program was successfully executed.
     */
    //用指定的参数执行指定的程序。尝试加载程序，然后fork线程运行它。
    public boolean execute(String name, String[] args) {
        if (!load(name, args))
            return false;

//        new UThread(this).setName(name).fork();
        thread = new UThread(this);
        thread.setName(name).fork();

        return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    //保存此进程的状态以准备上下文切换。由<tt>uthread.saveState（）</tt>调用。

    /**
     *
     *
     *
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    //在上下文切换后还原此进程的状态。由<tt>uthread.restorestate（）调用
    public void restoreState() {
        Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param vaddr     the starting virtual address of the null-terminated
     *                  string.
     * @param maxLength the maximum number of characters in the string,
     *                  not including the null terminator.
     * @return the string read, or <tt>null</tt> if no null terminator was
     * found.
     */
    //从该进程的虚拟内存中读取以空结尾的字符串。从指定地址最多读取<TT>最大长度+1 </TT>字节，
    // 搜索null终止符，并将其转换为<TT> Java.Lang.Stult</TT>，不包括null终止符。如果未找到空终止符，则返回<tt>null</tt>。
    public String readVirtualMemoryString(int vaddr, int maxLength) {
        Lib.assertTrue(maxLength >= 0);

        byte[] bytes = new byte[maxLength + 1];

        int bytesRead = readVirtualMemory(vaddr, bytes);

        for (int length = 0; length < bytesRead; length++) {
            if (bytes[length] == 0)
                return new String(bytes, 0, length);
        }

        return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to read.
     * @param data  the array where the data will be stored.
     * @return the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
        return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param vaddr  the first byte of virtual memory to read.
     * @param data   the array where the data will be stored.
     * @param offset the first byte to write in the array.
     * @param length the number of bytes to transfer from virtual memory to
     *               the array.
     * @return the number of bytes successfully transferred.
     */
    //将数据从该进程的虚拟内存传输到指定的数组。
    // 此方法处理地址转换详细信息。如果发生错误，此方法必须 <i>not</i> 销毁当前进程，
    // 而是应返回成功复制的字节数（如果无法复制任何数据，则返回零）。

    //读内存时 利用页表 将逻辑地址转化为物理地址  然后将内存复制到数组中
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
                                 int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

//        Lib.assertTrue((pageSize * numPages - vaddr) < length, "地址越界");


        byte[] memory = Machine.processor().getMemory();

        /**
         * 一种实现
         */
        //从地址中提取 虚拟页的页码
//        int virtualPageNum = Machine.processor().pageFromAddress(vaddr);
//
//        //从地址中提取偏移分量。
//        int addrOffset = Machine.processor().offsetFromAddress(vaddr);
//
//
//        if (virtualPageNum >= numPages) {
//            return -1;
//        }
//
//        TranslationEntry entry = pageTable[virtualPageNum];
//
//        if (entry == null)
//            return 0;
//        if (entry.valid == false)
//            return -1;
//
//        entry.used = true;
//
//        if (entry.ppn < 0 || entry.ppn >= Machine.processor().getNumPhysPages()) {
//            return 0;
//        }
//
//        //物理地址 为  物理页号*页表大小 +页偏移
//        int paddr = entry.ppn * pageSize + addrOffset;
//
//        int amount = Math.min(length, memory.length - paddr);
//        System.arraycopy(memory, paddr, data, offset, amount);


        /**
         * 另一种实现  可以防止跨页问题
         */

        int amount = 0;
        do {

            //从地址中提取 虚拟页的页码
            int virtualPageNum = Machine.processor().pageFromAddress(vaddr + amount);

            if (virtualPageNum >= numPages) {
                return -1;
            }

            //从地址中提取偏移分量。
            int addrOffset = Machine.processor().offsetFromAddress(vaddr + amount);

            int bytesLeftInPage = pageSize - addrOffset;

            int bytesToRead = Math.min(bytesLeftInPage, length - amount);


            //从
            if (AllocatePageTable(virtualPageNum) == null)
                return 0;
            if (AllocatePageTable(virtualPageNum).valid == false)
                return -1;

            AllocatePageTable(virtualPageNum).used = true;

            if (AllocatePageTable(virtualPageNum).ppn < 0 || AllocatePageTable(virtualPageNum).ppn >= Machine.processor().getNumPhysPages()) {
                return 0;
            }

            int physicalAddr = Processor.makeAddress(AllocatePageTable(virtualPageNum).ppn, addrOffset);
            System.arraycopy(memory, physicalAddr, data, offset + amount, bytesToRead);
            amount += bytesToRead;

        } while (amount < length);




        // for now, just assume that virtual addresses equal physical addresses
//        if (vaddr < 0 || vaddr >= memory.length)
//            return 0;
//
//        int amount = Math.min(length, memory.length - vaddr);
//        System.arraycopy(memory, vaddr, data, offset, amount);

        return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to write.
     * @param data  the array containing the data to transfer.
     * @return the number of bytes successfully transferred.
     */
    //将指定阵列中的所有数据传输到此进程的虚拟内存。
    //与<tt>writeVirtualMemory（vaddr，data，0，data.length）相同。
    public int writeVirtualMemory(int vaddr, byte[] data) {
        return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param vaddr  the first byte of virtual memory to write.
     * @param data   the array containing the data to transfer.
     * @param offset the first byte to transfer from the array.
     * @param length the number of bytes to transfer from the array to
     *               virtual memory.
     * @return the number of bytes successfully transferred.
     */
    //将数据从指定数组传输到此进程的虚拟内存。

    //写内存时 利用页表 将逻辑地址转化为物理地址  然后将数组中的内容 复制到内存中
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
                                  int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        //返回用户程序的主存储器
        byte[] memory = Machine.processor().getMemory();

        /**
         *
         * 一种实现
         */
//        //从地址中提取 虚拟页的页码
//        int virtualPageNum = Machine.processor().pageFromAddress(vaddr);
//
//        //从地址中提取偏移分量。
//        int addrOffset = Machine.processor().offsetFromAddress(vaddr);
//
//
//        if (virtualPageNum >= numPages) {
//            return -1;
//        }
//        TranslationEntry entry = pageTable[virtualPageNum];
//
//        if (entry == null)
//            return 0;
//        //查看此页是否是只读的
//
//        if (entry.valid == false || entry.readOnly)
//            return -1;
//
//        entry.used = true;
//        entry.dirty = true;
//
//        if (entry.ppn < 0 || entry.ppn >= Machine.processor().getNumPhysPages()) {
//            return 0;
//        }
//
//        int physicalAddr = entry.ppn * pageSize + addrOffset;
//        int amount = Math.min(length, memory.length - physicalAddr);
//        System.arraycopy(data, offset, memory, vaddr, amount);


        /**
         *
         * 另一种实现
         */
        int amount = 0;
        do {
            int virtualPageNum = Processor.pageFromAddress(vaddr + amount);
            int addrOffset = Processor.offsetFromAddress(vaddr + amount);

            if (AllocatePageTable(virtualPageNum) == null)
                return 0;
            //查看此页是否是只读的

            //从页表中选取对应的虚拟页
            if (AllocatePageTable(virtualPageNum).valid == false || AllocatePageTable(virtualPageNum).readOnly)
                return -1;

            AllocatePageTable(virtualPageNum).used = true;
            AllocatePageTable(virtualPageNum).dirty = true;

            if (AllocatePageTable(virtualPageNum).ppn < 0 || AllocatePageTable(virtualPageNum).ppn >= Machine.processor().getNumPhysPages()) {
                return 0;
            }

            int bytesLeftInPage = pageSize - addrOffset;
            int bytesToWrite = Math.min(bytesLeftInPage, length - amount);

            int physicalAddr = Processor.makeAddress(AllocatePageTable(virtualPageNum).ppn, addrOffset);
            System.arraycopy(data, offset + amount, memory, physicalAddr, bytesToWrite);
            amount += bytesToWrite;
        } while (amount < length);



        return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the executable was successfully loaded.
     */
    //将具有指定名称的可执行文件加载到此进程中，并准备将指定参数传递给它。
    // 打开可执行文件，读取其头信息，并将节和参数复制到此进程的虚拟内存中。

    //从磁盘装入进程  需要装入一个coff的对象  包含若干个段  每一段是一个coffsection的对象 包含若干个页，

    protected boolean load(String name, String[] args) {
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if (executable == null) {
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try {
            coff = new Coff(executable);
        } catch (EOFException e) {
            executable.close();
            Lib.debug(dbgProcess, "\tcoff load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages) {
                coff.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            //如果 所有的物理页都被分配 则 释放在主存的的那些页
            if (!allocate(numPages, section.getLength(), section.isReadOnly())) {
                releaseResource();
                return false;
            }
        }

        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i = 0; i < args.length; i++) {
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize) {
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

        // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();

        // 接下来是堆栈；堆栈指针最初指向它的顶部
        if (!allocate(numPages, stackPages, false)) {
            releaseResource();
            return false;
        }
        initialSP = numPages * pageSize;

        // 最后保留1页作为参数
        if (!allocate(numPages, 1, false)) {
            releaseResource();
            return false;
        }

        if (!loadSections())
            return false;

        // store arguments in last page
        int entryOffset = (numPages - 1) * pageSize;
        int stringOffset = entryOffset + args.length * 4;

        this.argc = args.length;
        this.argv = entryOffset;

        for (int i = 0; i < argv.length; i++) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
            stringOffset += argv[i].length;
            Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
            stringOffset += 1;
        }

        return true;
    }
    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return <tt>true</tt> if the sections were successfully loaded.
     */
    //为该进程分配内存，并将COFF部分加载到Run（这是进程初始化的最后一步，可能会失败）。
    protected boolean loadSections() {
        if (numPages > Machine.processor().getNumPhysPages()) {
            coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            return false;
        }

        //在导入 section之前应该先创建一个页表
        pageTable = new TranslationEntry[numPages];
        for (int i = 0; i < numPages; ++i) {
            //分配物理页数
            int ppn = UserKernel.getFreePage();
            // //虚拟页数   物理页数  标记位   只读位    是否被使用》  脏位
            pageTable[i] = new TranslationEntry(i, ppn, true, false, false, false);

        }
        // load sections
        //加载coff的  section
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                    + " section (" + section.getLength() + " pages)");

            for (int i = 0; i < section.getLength(); i++) {
                int virtualPageNum = section.getFirstVPN() + i;

                TranslationEntry entry = pageTable[virtualPageNum];
                //给页表中的一页赋值 是否只读
                entry.readOnly = section.isReadOnly();
                //对应的 物理页号
                int ppn = entry.ppn;

                //将此段中的 第i页 加载到物理内存的第ppn页
                section.loadPage(i, ppn);
                // for now, just assume virtual addresses=physical addresses
                //假设物理地址等于虚拟地址
//                section.loadPage(i, vpn);
            }
        }

        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    //释放由<tt>loadSections（）</tt>分配的任何资源。
    protected void unloadSections() {
        coff.close();

        for (int i = 0; i < numPages; ++i) {
            //释放之前分配的页表
            UserKernel.addFreePage(pageTable[i].ppn);
            pageTable[i] = null;
        }

        //将页表置为空
        pageTable = null;
    }

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    //初始化处理器的寄存器，以准备运行加载到此进程中的程序。将pc寄存器设置为指向起始函数，
    // 将堆栈指针寄存器设置为指向堆栈顶部，将a0和a1寄存器分别设置为argc和argv，并将所有其他寄存器初始化为0。
    public void initRegisters() {
        Processor processor = Machine.processor();

        // by default, everything's 0
        for (int i = 0; i < processor.numUserRegisters; i++)
            processor.writeRegister(i, 0);

        // initialize PC and SP according
        processor.writeRegister(Processor.regPC, initialPC);
        processor.writeRegister(Processor.regSP, initialSP);

        // initialize the first two argument registers to argc and argv
        processor.writeRegister(Processor.regA0, argc);
        processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call.
     */
    private int handleHalt() {
        //只有第一个进程才能调用handleHalt的系统调用  如果其他进程想调用 则会立即返回
        if (this.pid != 0) {
            Lib.debug(dbgProcess, "只有第一个用户进程才能执行 handleHalt()");
            return -1;
        }
        Machine.halt();

        Lib.assertNotReached("Machine.halt() did not halt machine!");
        return 0;
    }

    //nachos在载入一个用户进程执行时，会把程序载入nachos的内存
    //在打开或者新建文件时  会返回一个文件描述符  读写文件要使用文件描述符来指定读写的文件
    //若进程已打开相同的文件描述符 则不创建 返回该文件的文件描述符

    private int handleCreate(int filenameAddress) {
        //使用readVirtualMemoryString方法进行读取    读取虚拟内存中的文件名（根据 初始地址和长度）  根据地址  计算 页号 页偏移   找到 虚拟页对应的物理页
        String filename = readVirtualMemoryString(filenameAddress, maxLength);
        if (filename == null) {
            Lib.debug(dbgProcess, "没有找到文件名");
            return -1;
        }

        //使用文件系统 的open方法  第二个参数为true  表示 如果没有则创建一个新文件
        OpenFile returnValue = UserKernel.fileSystem.open(filename, true);
        if (returnValue == null) {
            Lib.debug(dbgProcess, "创建文件夹失败");
            return -1;
        }

        //查看是否已经打开16个文件 如果没有 返回标记
        int index = findEmptyFileDescriptor();
        if (index == -1) {
            Lib.debug(dbgProcess, "已超出用户进程拥有文件描述符的最大数量");
            return -1;
        }
        //维护  打开文件表
        FileDescriptors[index].setFileName(filename);
        FileDescriptors[index].setFile(returnValue);
        return index;
    }

    //在数组中寻找一个空的文件描述符
    private int findEmptyFileDescriptor() {
        for (int i = 0; i < MaxFileDescriptor; i++) {
            if (FileDescriptors[i].getFile() == null) {
                return i;
            }
        }
        return -1;
    }

    //根据文件名 寻找文件是否已经被打开
    private int findFileDescriptorByName(String filename) {
        for (int i = 0; i < MaxFileDescriptor; ++i) {
            if (FileDescriptors[i].getFileName().equals(filename)) {
                return i;
            }
        }
        return -1;
    }

    private int handleOpen(int filenameAddress) {
        //从虚拟内存中读入文件名
        String filename = readVirtualMemoryString(filenameAddress, maxLength);
        if (filename == null) {
            Lib.debug(dbgProcess, "没有找到文件名");
            return -1;
        }

        OpenFile returnValue = UserKernel.fileSystem.open(filename, false);
        if (returnValue == null) {
            Lib.debug(dbgProcess, "打开文件失败");
            return -1;
        }

        int index = findEmptyFileDescriptor();
        if (index == -1) {
            Lib.debug(dbgProcess, "已超出用户进程拥有文件描述符的最大数量");
            return -1;
        }
        FileDescriptors[index].setFileName(filename);
        FileDescriptors[index].setFile(returnValue);
        return index;

    }

    //参数为 文件描述符   内存地址  和读取的字节数
    //读取文件信息 将其 存续 该用户程序的虚拟内存中
    private int handleRead(int fileDescriptor, int virtualAddress, int bufferSize) {

        if (fileDescriptor < 0 || fileDescriptor >= MaxFileDescriptor || bufferSize < 0 || virtualAddress < 0) {
            Lib.debug(dbgProcess, "输入有误");
            return -1;
        }
        FileDescriptor fd = FileDescriptors[fileDescriptor];
        if (fd.getFile() == null)
            return -1;
        //需要写入主存的内容
        byte[] buffer = new byte[bufferSize];
        int readSize = fd.getFile().read(buffer, 0, bufferSize);

        if (readSize <= 0)
            return 0;

        //从内存中读出数据  写入 虚拟内存（主存） 然后返回字节数
        int writeSize = writeVirtualMemory(virtualAddress, buffer, 0, readSize);
        return writeSize;
    }

    //将信息从主存储写入文件
    private int handleWrite(int fileDescriptor, int virtualAddress, int bufferSize) {

        if (fileDescriptor < 0 || fileDescriptor >= MaxFileDescriptor || bufferSize < 0 || virtualAddress < 0) {
            Lib.debug(dbgProcess, "输入有误");
            return -1;
        }
        FileDescriptor fd = FileDescriptors[fileDescriptor];
        if (fd.getFile() == null)
            return -1;
        byte[] buffer = new byte[bufferSize];
        //读取主存中的信息  虚拟内存
        int readSize = readVirtualMemory(virtualAddress, buffer);
        if (readSize == -1)
            return -1;

        //写入文件
        int returnValue = fd.getFile().write(buffer, 0, readSize);
        if (returnValue == -1)
            return -1;

        return returnValue;
    }

    //从该用户进程的文件描述符数组   移除此文件描述符
    private int handleClose(int fileDescriptor) {

        if (fileDescriptor < 0 || fileDescriptor >= MaxFileDescriptor) {
            return -1;
        }

        FileDescriptor fd = FileDescriptors[fileDescriptor];
        if (fd.getFile() != null)
            fd.getFile().close();
        fd.setFile(null);

        boolean returnValue = true;

        if (fd.isToRemove() == true) {
            returnValue = UserKernel.fileSystem.remove(fd.getFileName());
            fd.setToRemove(false);
        }

        fd.setFileName("");

        if (returnValue == false)
            return -1;
        return 0;
    }

    //删除某个文件  根据传入的文件名内存地址   从虚拟存储中读出  文件名
    private int handleUnlink(int filenameAddress) {
        //从虚拟内存中  取出文件名
        String filename = readVirtualMemoryString(filenameAddress, maxLength);
        boolean returnValue = true;
        int index = findFileDescriptorByName(filename);
        if (index == -1) {
            returnValue = UserKernel.fileSystem.remove(filename);
        } else {
            //先关闭读取 移除文件描述符 在进行删除操作
            handleClose(filenameAddress);
            returnValue = UserKernel.fileSystem.remove(filename);
        }

        if (returnValue == false)
            return -1;
        return 0;
    }


    /**
     * * task 2。3  任务一
     * * （1）获取虚拟文件名
     * * （2）处理参数 首先用第三个参数作为虚拟内存地址 得到参数表数组的首地址  然后用readVirtualMemory读出每个参数
     * * （3）用newUserProcess创建子进程 ，将文件和参数表 加载到子进程
     * * （4）execute执行子进程  同时将子进程的父进程 设置为此进程 在将子进程加入到子进程列表
     * *
     *
     * @param fileAddress 程序地址
     * @param argCount    参数个数
     * @param argAddress  参数地址
     * @return
     */

    private int handleExec(int fileAddress, int argCount, int argAddress) {
        if (argCount < 1)
            return -1;
        //根据文件地址  读取文件名
        String filename = readVirtualMemoryString(fileAddress, maxLength);

        if (filename == null)
            return -1;

        //检测文件后缀名
        String suffix = filename.substring(filename.length() - 5, filename.length());
        if (!suffix.equals(".coff"))
            return -1;

        //读取参数列表
        String args[] = new String[argCount];
        for (int i = 0; i < argCount; ++i) {
            byte arg[] = new byte[4];
            int transferSize = readVirtualMemory(argAddress + i * 4, arg);
            if (transferSize != 4)
                return -1;
            int RealargAddress = Lib.bytesToInt(arg, 0);
            args[i] = readVirtualMemoryString(RealargAddress, maxLength);
        }

        UserProcess childProcess = UserProcess.newUserProcess();
        this.childProcesses.add(childProcess.pid);

        boolean returnValue = childProcess.execute(filename, args);
        if (returnValue == true) {
            return childProcess.pid;
        }
        return -1;
    }

    /**
     * join 阻塞等待某子进程 执行完毕
     * <p>
     * 父进程和子进程 不共享任何内存  文件 以及其他状态
     * 父进程只能对子进程进行join操作  如果 A执行B  B执行C  则A不能join  c
     * <p>
     * （1）首先判断是否是子进程  不是则返回-1  是则继续
     * （2）将当前线程挂起在队列中
     * （3）如果当前进程结束 则返回1  否则返回0
     *
     * @param childPid   子进程编号
     * @param addrStatus 保存子进程编号的地址
     * @return
     */
    private int handleJoin(int childPid, int addrStatus) {
        //检查是否是自己的子线程
        if (!childProcesses.contains(childPid)) {
            return -1;
        }
        //找到子线程 然后将它移除  数据结构

        for (int i = 0; i < childProcesses.size(); i++) {
            if (childProcesses.get(i) == childPid) {
                childProcesses.remove(i);
                break;
            }
        }

        UserProcess childProcess = UserProcess.findProcessByID(childPid);
        if (childProcess == null) {
            return -1;
        }

        //子进程调用join  父进程挂起  等待子进程执行结束
        childProcess.thread.join();
        byte byteStatus[] = new byte[4];
        byteStatus = Lib.bytesFromInt(childProcess.exitStatus);

        //写入 主存的
        int transferSize = writeVirtualMemory(addrStatus, byteStatus);
        if (transferSize == 4) {
            return 1;
        }
        return -1;
    }

    /**
     * （1）首先关闭coff  将所有的打开文件关闭  将退出状态置入，
     * （2）如果该进程有父进程   看是否执行了join方法 如果执行了就将其唤醒  同时将此进程从子进程链表中删除
     * （3）调用unloadsections释放内存，调用kthread。finish结束线程
     * （4）如果是最后一个线程 则停机
     *
     * @param exitStatus
     */
    private void handleExit(int exitStatus) {

        //清除打开文件表  关闭文件
        for (int i = 0; i < MaxFileDescriptor; ++i) {
            if (FileDescriptors[i].getFile() != null)

                handleClose(i);
        }



        this.exitStatus = exitStatus;
        //释放资源
        this.unloadSections();

        if (this.pid == 0) {
            //如果 没有进程执行 则停机
            Kernel.kernel.terminate();
        } else {
            //如果有进程执行 则将当前进程finish掉 调度下一个进程
            KThread.currentThread().finish();
        }
    }

    //根据线程id寻找对应的线程
    private static UserProcess findProcessByID(int id) {
        return userProcessHashtable.get(id);
    }

    /**
     *
     *
     */
    private static final int
            syscallHalt = 0,
            syscallExit = 1,
            syscallExec = 2,
            syscallJoin = 3,
            syscallCreate = 4,
            syscallOpen = 5,
            syscallRead = 6,
            syscallWrite = 7,
            syscallClose = 8,
            syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * </tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     * </tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     * </tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     *
     * @param syscall the syscall number.
     * @param a0      the first syscall argument.
     * @param a1      the second syscall argument.
     * @param a2      the third syscall argument.
     * @param a3      the fourth syscall argument.
     * @return the value to be returned to the user.
     */
    //系统调用
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        switch (syscall) {
            case syscallHalt:
                return handleHalt();
            case syscallCreate:
                return handleCreate(a0);
            case syscallOpen:
                return handleOpen(a0);
            case syscallWrite:
                return handleWrite(a0, a1, a2);
            case syscallRead:
                return handleRead(a0, a1, a2);
            case syscallClose:
                return handleClose(a0);
            case syscallUnlink:
                return handleUnlink(a0);
            case syscallExit:
                handleExit(a0);
                return 0;
            case syscallJoin:
                return handleJoin(a0, a1);
            case syscallExec:
                return handleExec(a0, a1, a2);


            default:
                Lib.debug(dbgProcess, "Unknown syscall " + syscall);
                Lib.assertNotReached("Unknown system call!");
        }
        return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param cause the user exception that occurred.
     */
    //处理用户异常。由<tt>userkernel.exceptionhandler（）调用。
    // <i>cause</i>参数标识发生的异常；请参阅<tt>processor.exceptionzzz</tt>常量。
    public void handleException(int cause) {
        Processor processor = Machine.processor();

        switch (cause) {
            case Processor.exceptionSyscall:
                int result = handleSyscall(processor.readRegister(Processor.regV0),
                        processor.readRegister(Processor.regA0),
                        processor.readRegister(Processor.regA1),
                        processor.readRegister(Processor.regA2),
                        processor.readRegister(Processor.regA3)
                );
                processor.writeRegister(Processor.regV0, result);
                processor.advancePC();
                break;

            default:
                Lib.debug(dbgProcess, "Unexpected exception: " +
                        Processor.exceptionNames[cause]);
                Lib.assertNotReached("Unexpected exception");
        }
    }

    /**
     * The program being run by this process.
     */


    //此进程运行的程序
    protected Coff coff;

    /**
     * This process's page table.
     */
    //页表
    protected TranslationEntry[] pageTable;
    /**
     * The number of contiguous pages occupied by the program.
     */
    //程序占用的连续页数
    protected int numPages;

    /**
     * The number of pages in the program's stack.
     */
    //程序堆栈中的页数
    protected final int stackPages = 8;

    private int initialPC, initialSP;
    private int argc, argv;

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';


}
