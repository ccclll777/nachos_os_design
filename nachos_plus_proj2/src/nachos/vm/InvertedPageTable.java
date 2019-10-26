package nachos.vm;


import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.TranslationEntry;

import java.util.HashMap;

//全局的反向页表  保存在真正内存位置的页的虚拟地址以及拥有该页的进程的信息
public class InvertedPageTable {

    //全局的反向页表
    private HashMap<VirtualPageFlag,TranslationEntry> GlobalInvertedPageTable;

    //全局的一个 页表  存放所有的物理页（对应 进程） 对应进程的物理页 的一个副本
    public TranslationEntryWithPid[] PhysicalPageCopy;

    private static InvertedPageTable instance=null;

    private  SecondChanceReplacement secondChanceReplacement ;

    protected static final char dbgVM='v';

    private InvertedPageTable(){
        GlobalInvertedPageTable=new HashMap<VirtualPageFlag,TranslationEntry>();
        PhysicalPageCopy=new TranslationEntryWithPid[Machine.processor().getNumPhysPages()];
        secondChanceReplacement = new SecondChanceReplacement();
    }


    //获取反向页表的实例  全局公用同一个反向页表
    public static InvertedPageTable getInstance(){
        if(instance==null)instance=new InvertedPageTable();
        return instance;
    }

    //向反向页表中插入一页
    public boolean insertEntry(int pid,TranslationEntry entry)
    {
        VirtualPageFlag virtualPageFlag = new VirtualPageFlag(pid,entry.vpn);
        if(GlobalInvertedPageTable.containsKey(virtualPageFlag)){
            Lib.debug(dbgVM, "\t反向页表中已经存在此页");
            return false;
        }
        Lib.debug(dbgVM, "\t插入反向页表:进程 "+pid+"   虚拟页为 "+entry.vpn+
                "    物理页为 "+entry.ppn+"以插入反向页表 ");
        GlobalInvertedPageTable.put(virtualPageFlag,entry);

        //如果则虚拟页在物理页中，而tlb可以直接由处理器使用。
        if(entry.valid){

            PhysicalPageCopy[entry.ppn]=new TranslationEntryWithPid(entry,pid);
            Lib.debug(dbgVM, "\t此页已经加载到内存中["+entry.ppn+"]");
        }
        return true;
    }

    //将某进程的某虚拟页 从反向页表中删除
    public TranslationEntry deleteEntry(int pid,int vpn)
    {
        VirtualPageFlag virtualPageFlag = new VirtualPageFlag(pid,vpn);
        TranslationEntry entry=GlobalInvertedPageTable.get(virtualPageFlag);
        if(entry == null)
        {
            Lib.debug(dbgVM, "\t此进程的此虚拟页不在反向页表中");
            return null;

        }
        //如果则虚拟页在内存中，则将其清空
        if(entry.valid){
            PhysicalPageCopy[entry.ppn]=null;
            Lib.debug(dbgVM, "\t此页已从内存中清空["+entry.ppn+"]");
        }
        return entry;
    }
    //设置某进程的某一个TranslationEntry
    public void setEntry(int pid,TranslationEntry newEntry)
    {
        VirtualPageFlag virtualPageFlag =new VirtualPageFlag(pid,newEntry.vpn);
        //如果此TranslationEntry本身就不在反向页表中
        if(!GlobalInvertedPageTable.containsKey(virtualPageFlag)){
            Lib.debug(dbgVM, "\t此TranslationEntry不在反向页表中");
            return;
        }

        //从反向页表中取出旧TranslationEntry
        TranslationEntry oldEntry=GlobalInvertedPageTable.get(virtualPageFlag);
        if(oldEntry.valid){
            if(PhysicalPageCopy[oldEntry.ppn]==null){
                Lib.debug(dbgVM, "\t内存中不存在此虚拟页对应的物理页");
                return;
            }
            PhysicalPageCopy[oldEntry.ppn]=null;
            Lib.debug(dbgVM, "\t内存中此虚拟页对应的物理页清空["+oldEntry.ppn+"]");
        }
        if(newEntry.valid){
            if(PhysicalPageCopy[newEntry.ppn]!=null){
                Lib.debug(dbgVM, "\t内存中此虚拟页对应的物理页已被占用");
                return;
            }
            PhysicalPageCopy[newEntry.ppn]=new TranslationEntryWithPid(newEntry,pid);
            Lib.debug(dbgVM, "\t内存中此虚拟页对应的物理页已设置["+newEntry.ppn+"]");
        }
        GlobalInvertedPageTable.put(virtualPageFlag, newEntry);
    }
    public void updateEntry(int pID,TranslationEntry entry){
        VirtualPageFlag key=new VirtualPageFlag(pID,entry.vpn);
        if(GlobalInvertedPageTable.containsKey(key)){
            Lib.debug(dbgVM, "\t此TranslationEntry已经存在在反向页表中");
            return;
        }
        TranslationEntry oldEntry=GlobalInvertedPageTable.get(key);
        TranslationEntry newEntry=mix(entry,oldEntry);
        if(oldEntry.valid){
            if(PhysicalPageCopy[oldEntry.ppn]==null){
                return;
            }
            PhysicalPageCopy[oldEntry.ppn]=null;
            Lib.debug(dbgVM, "\t内存中此虚拟页对应的物理页清空["+oldEntry.ppn+"]");
        }
        if(newEntry.valid){
            if(PhysicalPageCopy[newEntry.ppn]!=null){
                return;
            }
            PhysicalPageCopy[newEntry.ppn]=new TranslationEntryWithPid(newEntry,pID);
            Lib.debug(dbgVM, "\t内存中此虚拟页对应的物理页已设置["+oldEntry.ppn+"]");
        }
        GlobalInvertedPageTable.put(key, newEntry);
    }

    private TranslationEntry mix(TranslationEntry entry1,TranslationEntry entry2){
        TranslationEntry mixture=entry1;
        if(entry1.dirty||entry2.dirty){
            mixture.dirty=true;
        }
        if(entry1.used||entry1.used){
            mixture.used=true;
        }
        return mixture;
    }



    //获取 某进程 某虚拟页下对应的TranslationEntry
    public TranslationEntry getEntry(int pid,int vpn)
    {
        VirtualPageFlag virtualPageFlag =new VirtualPageFlag(pid,vpn);
        TranslationEntry entry=null;
        Lib.debug(dbgVM, "\t进程 "+pid+" 使用虚拟页 "+vpn+
                " 从反向页表中获取到对应的TranslationEntry  ");
        if(GlobalInvertedPageTable.containsKey(virtualPageFlag)){
            entry=GlobalInvertedPageTable.get(virtualPageFlag);
        }
        return entry;
    }

    //选取被置换的页
    public TranslationEntryWithPid getVictimPage(){
        TranslationEntryWithPid entry=null;
        entry = PhysicalPageCopy[secondChanceReplacement.findSwappedPage()];
//
//        do{
//            int index=Lib.random(PhysicalPageCopy.length);
//            entry=PhysicalPageCopy[index];
//        }while(entry==null||!entry.getTranslationEntry().valid);
        return entry;
    }

}
