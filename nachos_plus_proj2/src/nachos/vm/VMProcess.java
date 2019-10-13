package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public VMProcess() {
        super();
        allocatedPages=new LinkedList<Integer>();
        lazyLoadPages = new HashMap<Integer, CoffSectionAddress>();
        tlbBackUp = new TranslationEntry[Machine.processor().getTLBSize()];
        for (int i = 0; i < tlbBackUp.length; i++) {
            tlbBackUp[i] = new TranslationEntry(0, 0, false, false, false, false);
        }
    }



    protected int getFreePage() {
        //获取一个物理页
        int ppn = VMKernel.getFreePage();

        if (ppn == -1) {
            //如果没有物理页了  需要选择一页牺牲掉
            TranslationEntryWithPid victim = InvertedPageTable.getInstance().getVictimPage();
            ppn = victim.getTranslationEntry().ppn;
            swapOut(victim.getPid(), victim.getTranslationEntry().vpn);
        }

        return ppn;
    }

    //重写readVirtualMemory方法
    public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        pageLock.acquire();

        int vpn = Machine.processor().pageFromAddress(vaddr);
        //从反向页表读出对应的TranslationEntry
        TranslationEntry entry = InvertedPageTable.getInstance().getEntry(pid, vpn);
        if (entry == null) {
            Lib.debug(dbgVM, "\t没有找到对应的页");
        }
        if (!entry.valid) {
            //表示 此页还没有被加载到物理页中   需要重新分配物理页
            int ppn = getFreePage();
            swapIn(ppn, vpn);
        }
        entry.used = true;
        //在反向页表中 更新对应的页
        InvertedPageTable.getInstance().setEntry(pid, entry);

        pageLock.release();

        return super.readVirtualMemory(vaddr, data, offset, length);
    }

    public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        pageLock.acquire();

        int vpn = Processor.pageFromAddress(vaddr);
        //由于进程只能看到TLB 所以需要将需要的页换入
        swap(vpn);
        TranslationEntry entry = InvertedPageTable.getInstance().getEntry(pid , vpn);
        if (entry == null) {
            Lib.debug(dbgVM, "\t没有找到对应的页");
        }
        if (!entry.valid) {
            Lib.debug(dbgVM, "\t此页没有被载入物理页");
        }
        entry.dirty = true;
        entry.used = true;
        InvertedPageTable.getInstance().setEntry(pid, entry);

        pageLock.release();

        return super.writeVirtualMemory(vaddr, data, offset, length);
    }

    protected int swap(int vpn) {
        TranslationEntry entry = InvertedPageTable.getInstance().getEntry(pid, vpn);

        if (entry.valid)
            return entry.ppn;
        int ppn = getFreePage();
        swapIn(ppn, vpn);
        return ppn;
    }
    protected TranslationEntry AllocatePageTable(int vpn) {
        return InvertedPageTable.getInstance().getEntry(pid ,vpn);
    }


    protected void lazyLoad(int vpn, int ppn) {

        CoffSectionAddress coffSectionAddress = lazyLoadPages.remove(vpn);
        if (coffSectionAddress == null) {
            Lib.debug(dbgVM, "\t没有找到此coffsection");
            return;
        }
        Lib.debug(dbgVM, "\t将此逻辑页 " + vpn + " 对应到物理页 " + ppn);
        CoffSection section = coff.getSection(coffSectionAddress.getSectionNumber());
        section.loadPage(coffSectionAddress.getPageOffset(), ppn);

    }
    //将某一页从  反向页表中置换出来  这时需要 写入交换文件

    protected void swapOut(int pid, int vpn) {
        TranslationEntry entry = InvertedPageTable.getInstance().getEntry(pid, vpn);
        if (entry == null) {
            Lib.debug(dbgVM, "\t反向页表中 没有此进程的此虚拟页对应的TranslationEntry");
            return;
        }
        if (!entry.valid) {
            Lib.debug(dbgVM, "\t此页不在物理内存中 ");
            return;
        }

        Lib.debug(dbgVM, "\t进程  " + pid +
                " 的虚拟页 " + vpn + " 在物理页的 " + entry.ppn);

        for (int i = 0; i < Machine.processor().getTLBSize(); i++) {

            TranslationEntry tlbEntry = Machine.processor().readTLBEntry(i);
            //遍历tbl  置换出对应的页
            if (tlbEntry.vpn == entry.vpn && tlbEntry.ppn == entry.ppn && tlbEntry.valid) {
                //将反向页表中旧的页换掉
                InvertedPageTable.getInstance().updateEntry(pid, tlbEntry);
                //读取反向页表中对应的新页
                entry = InvertedPageTable.getInstance().getEntry(pid, vpn);
                tlbEntry.valid = false;//表示 不在内存中
                //写入tlb
                Machine.processor().writeTLBEntry(i, tlbEntry);
                break;
            }
        }
        if (entry.dirty) {
            byte[] memory = Machine.processor().getMemory();
            SwapperController.getInstance(getSwapFileName()).writeToSwapFile(pid, vpn, memory, entry.ppn * pageSize);
        }
    }

    protected String getSwapFileName() {
        return "Swap";
    }

    //取得一个物理页 然后将 发生页错误的虚拟页 装到对应的物理页中
    protected void swapIn(int ppn, int vpn) {
        TranslationEntry entry = InvertedPageTable.getInstance().getEntry(pid, vpn);
        if (entry == null) {
            Lib.debug(dbgVM, "\t反向页表中没有对应的页");
            return;
        }
        if (entry.valid) {
            Lib.debug(dbgVM, "\t此页已经装入物理内存");
            return;
        }

        Lib.debug(dbgVM, "\t进程 " + pid +
                "虚拟页 " + vpn + "被换入物理页 " + ppn);

        boolean dirty, used;
        //如果是首次加载此 coff文件 需要设置dirty
        if (lazyLoadPages.containsKey(vpn)) {
            lazyLoad(vpn, ppn);
            dirty = true;
            used = true;
        } else {
            //如果不是首次加载此coff  则将此物理页 从交换文件 复制到主存中
            byte[] memory = Machine.processor().getMemory();
            byte[] page = SwapperController.getInstance(getSwapFileName()).readFromSwapFile(pid, vpn);
            System.arraycopy(page, 0, memory, ppn * pageSize, pageSize);
            dirty = false;
            used = false;
        }
        TranslationEntry newEntry = new TranslationEntry(vpn, ppn, true, false, used, dirty);
        //将此页转入反向页表
        InvertedPageTable.getInstance().setEntry(pid, newEntry);
    }
    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    /**
     * 为了解决这个问题，您必须保证至少一个进程在上下文切换之前保证有2个TLB翻译以及内存中的2个对应页
     * 。这样，至少有一个进程能够在双误指令上取得进展。通过在上下文切换中保存和恢复TLB的状态，可以减少Live锁的效果，
     * 但是同样的问题可能发生在很少的物理内存页上。在这种情况下，在加载指令页和数据页之前，2个进程可能会以类似的方式被卡住。
     */
    public void saveState() {
//	super.saveState();
        Lib.debug(dbgVM, "\t进程 " + pid + " 保存上下文状态");

        for (int i = 0; i < Machine.processor().getTLBSize(); i++) {
            //保存此进程当前的TLB信息
            tlbBackUp[i] = Machine.processor().readTLBEntry(i);
            //保存此时TLB的状态到 反向页表中
            if (tlbBackUp[i].valid) {
                InvertedPageTable.getInstance().updateEntry(pid, tlbBackUp[i]);
            }
        }
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    //在上下文切换后还原此进程的状态
    public void restoreState() {
//	super.restoreState();
        Lib.debug(dbgVM, "\t进程 " + pid + " 在上下文切换后还原进程状态");
        for (int i = 0; i < tlbBackUp.length; i++) {
            //如果此进程之前的TLB中有页在内存中  则
            if (tlbBackUp[i].valid) {
                //还原TLB信息
                Machine.processor().writeTLBEntry(i, tlbBackUp[i]);
                //可以被其他进程替换
                TranslationEntry entry = InvertedPageTable.getInstance().getEntry(pid, tlbBackUp[i].vpn);
                if (entry != null && entry.valid) {
                    Machine.processor().writeTLBEntry(i, entry);
                } else {
                    Machine.processor().writeTLBEntry(i, new TranslationEntry(0, 0, false, false, false, false));
                }
            } else {
                Machine.processor().writeTLBEntry(i, new TranslationEntry(0, 0, false, false, false, false));
            }
        }
    }

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return    <tt>true</tt> if successful.
     */
    protected boolean loadSections() {

        // load sections
        //加载coff的  section
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);

            for (int i = 0; i < section.getLength(); i++) {
                int virtualPageNum = section.getFirstVPN() + i;

                //将coffsection的加载变为懒加载
                CoffSectionAddress coffSectionAddress = new CoffSectionAddress(s, i);
                lazyLoadPages.put(virtualPageNum, coffSectionAddress);
            }
        }

        return true;

    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        super.unloadSections();
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param    cause    the user exception that occurred.
     */
    public void handleException(int cause) {
        Processor processor = Machine.processor();

        switch (cause) {
            case Processor.exceptionTLBMiss:
                //导致TLB缺失的虚拟地址是通过调用processor.readRegister(processor.regBadVAddr);获得的
                int address = processor.readRegister(processor.regBadVAddr);
                int vpn = Machine.processor().pageFromAddress(address);
                Lib.debug(dbgVM, "\tTLB失效，地址为 " + address + " 虚拟页号为 " + vpn);
                pageLock.acquire();
                //处理TLB缺页
                boolean isSuccessful = handleTLBFault(address);
                if (isSuccessful) {
                    Lib.debug(dbgVM, "\tTLB未命中处理成功");
                } else {
                    UThread.finish();
                }
                pageLock.release();
                break;
            default:
                super.handleException(cause);
                break;
        }
    }

    //TLB错误  说明没有在TLB中找到对应的 虚拟页
    protected boolean handleTLBFault(int vaddr) {
        Lib.debug(dbgVM, "\tTLB出错");
        //虚拟页数
        int vpn = Processor.pageFromAddress(vaddr);
        //获取到 页错误发生的  反向页表中对应的TranslationEntry
        TranslationEntry entry = InvertedPageTable.getInstance().getEntry(pid, Processor.pageFromAddress(vaddr));
        if (entry == null) {
            Lib.debug(dbgVM, "\t没有在反向页表中找到对应的页");
            return false;
        }
        //如果  页不在内存中  需要取一个物理页  （将页装入内存中） 然后转入elb
        if (!entry.valid) {
            Lib.debug(dbgVM, "\t页错误");
            int ppn = getFreePage();
            swapIn(ppn, vpn);
            entry = InvertedPageTable.getInstance().getEntry(pid, Processor.pageFromAddress(vaddr));
        }
        //否则直接牺牲TLB中的页 然后 置换
        int victim = getTLBVictim();
        replaceTLBEntry(victim, entry);
        return true;
    }


    protected void replaceTLBEntry(int index, TranslationEntry newEntry) {
        TranslationEntry oldEntry = Machine.processor().readTLBEntry(index);
        if (oldEntry.valid) {
            InvertedPageTable.getInstance().updateEntry(pid, oldEntry);
        }
        Lib.debug(dbgVM, "\t将TLB中第 " + index + " 个的虚拟页 "
                + oldEntry.vpn + "物理页 " + oldEntry.ppn + " 替换为 虚拟页" + newEntry.vpn +
                "物理页 " + newEntry.ppn);
        Machine.processor().writeTLBEntry(index, newEntry);
    }

    //选择一个TLB中的页牺牲掉
    protected int getTLBVictim() {
        //如果
        for (int i = 0; i < Machine.processor().getTLBSize(); i++) {
            //如果此页现在已经不在主存中  则可以将它直接置换掉
            if (!Machine.processor().readTLBEntry(i).valid) {
                return i;
            }
        }
        //否则随机置换一个
        return Lib.random(Machine.processor().getTLBSize());
    }
    //给进程分配物理页
    protected boolean allocate(int vpn, int acquirePagesNum, boolean readOnly) {

        for (int i = 0; i < acquirePagesNum; ++i) {
            InvertedPageTable.getInstance().insertEntry(pid, new TranslationEntry(vpn + i, 0, false, readOnly, false, false));
            SwapperController.getInstance(getSwapFileName()).insertUnallocatedPage(pid, vpn + i);
            allocatedPages.add(vpn + i);
        }

        numPages += acquirePagesNum;

        return true;
    }
    protected void releaseResource() {
        for (int vpn: allocatedPages) {
            pageLock.acquire();

            TranslationEntry entry = InvertedPageTable.getInstance().deleteEntry(pid, vpn);
            if (entry.valid)
                VMKernel.addFreePage(entry.ppn);

            SwapperController.getInstance(getSwapFileName()).deletePosition(pid, vpn);

            pageLock.release();
        }
    }


    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';


    //处理器只能看到TLB
    protected TranslationEntry[] tlbBackUp;

    protected static Lock pageLock = new Lock();
    protected LinkedList<Integer> allocatedPages;
    //实现coffsection的懒加载
    protected HashMap<Integer, CoffSectionAddress> lazyLoadPages;

}
