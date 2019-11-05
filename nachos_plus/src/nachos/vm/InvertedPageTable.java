package nachos.vm;


import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.TranslationEntry;

import java.util.HashMap;

//全局的反向页表  保存在真正内存位置的页的虚拟地址以及拥有该页的进程的信息
public class InvertedPageTable {

    //全局的反向页表
    private static HashMap<VirtualPageFlag, TranslationEntry> GlobalInvertedPageTable = new HashMap<VirtualPageFlag, TranslationEntry>();
    ;

    //全局的一个 页表  存放所有的物理页（对应 进程） 对应进程的物理页 的一个副本
    public static TranslationEntryWithPid[] PhysicalPageCopy = new TranslationEntryWithPid[Machine.processor().getNumPhysPages()];


    private static SecondChanceReplacement secondChanceReplacement = new SecondChanceReplacement();

    private InvertedPageTable() {

    }


    //向反向页表中插入一页
    public static boolean insertEntry(int pid, TranslationEntry entry) {
        VirtualPageFlag virtualPageFlag = new VirtualPageFlag(pid, entry.vpn);
        if (GlobalInvertedPageTable.containsKey(virtualPageFlag)) {
            return false;
        }
        GlobalInvertedPageTable.put(virtualPageFlag, entry);
        //如果则虚拟页在物理页中，而tlb可以直接由处理器使用。
        if (entry.valid) {
            PhysicalPageCopy[entry.ppn] = new TranslationEntryWithPid(entry, pid);

        }
        return true;
    }

    //将某进程的某虚拟页 从反向页表中删除
    public static TranslationEntry deleteEntry(int pid, int vpn) {
        VirtualPageFlag virtualPageFlag = new VirtualPageFlag(pid, vpn);
        TranslationEntry entry = GlobalInvertedPageTable.get(virtualPageFlag);
        if (entry == null) {
            return null;
        }
        //如果则虚拟页在内存中，则将其清空
        if (entry.valid) {
            PhysicalPageCopy[entry.ppn] = null;
        }
        return entry;
    }

    //设置某进程的某一个TranslationEntry
    public static void setEntry(int pid, TranslationEntry newEntry) {
        VirtualPageFlag virtualPageFlag = new VirtualPageFlag(pid, newEntry.vpn);
        //如果此TranslationEntry本身就不在反向页表中
        if (!GlobalInvertedPageTable.containsKey(virtualPageFlag)) {

            return;
        }
        //从反向页表中取出旧TranslationEntry
        TranslationEntry oldEntry = GlobalInvertedPageTable.get(virtualPageFlag);
        if (oldEntry.valid) {
            if (PhysicalPageCopy[oldEntry.ppn] == null) {

                return;
            }
            PhysicalPageCopy[oldEntry.ppn] = null;

        }
        if (newEntry.valid) {
            if (PhysicalPageCopy[newEntry.ppn] != null) {

                return;
            }
            PhysicalPageCopy[newEntry.ppn] = new TranslationEntryWithPid(newEntry, pid);
        }
        GlobalInvertedPageTable.put(virtualPageFlag, newEntry);
    }

    public static void updateEntry(int pID, TranslationEntry entry) {
        VirtualPageFlag key = new VirtualPageFlag(pID, entry.vpn);
        if (GlobalInvertedPageTable.containsKey(key)) {

            return;
        }
        TranslationEntry oldEntry = GlobalInvertedPageTable.get(key);
        TranslationEntry newEntry = mix(entry, oldEntry);
        if (oldEntry.valid) {
            if (PhysicalPageCopy[oldEntry.ppn] == null) {
                return;
            }
            PhysicalPageCopy[oldEntry.ppn] = null;

        }
        if (newEntry.valid) {
            if (PhysicalPageCopy[newEntry.ppn] != null) {
                return;
            }
            PhysicalPageCopy[newEntry.ppn] = new TranslationEntryWithPid(newEntry, pID);

        }
        GlobalInvertedPageTable.put(key, newEntry);
    }

    private static TranslationEntry mix(TranslationEntry entry1, TranslationEntry entry2) {
        TranslationEntry mixture = entry1;
        if (entry1.dirty || entry2.dirty) {
            mixture.dirty = true;
        }
        if (entry1.used || entry1.used) {
            mixture.used = true;
        }
        return mixture;
    }


    //获取 某进程 某虚拟页下对应的TranslationEntry
    public static TranslationEntry getEntry(int pid, int vpn) {
        VirtualPageFlag virtualPageFlag = new VirtualPageFlag(pid, vpn);
        TranslationEntry entry = null;
        if (GlobalInvertedPageTable.containsKey(virtualPageFlag)) {
            entry = GlobalInvertedPageTable.get(virtualPageFlag);
        }
        return entry;
    }

    //选取被置换的页
    public static TranslationEntryWithPid getVictimPage() {
        TranslationEntryWithPid entry = null;
        int i = secondChanceReplacement.findSwappedPage();
        entry = PhysicalPageCopy[i];
        PhysicalPageCopy[i] = null;

        return entry;
    }

}
