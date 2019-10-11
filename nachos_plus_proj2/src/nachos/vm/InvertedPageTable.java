package nachos.vm;


import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.TranslationEntry;

import java.util.HashMap;

//全局的反向页表  保存在真正内存位置的页的虚拟地址以及拥有该页的进程的信息
public class InvertedPageTable {

    private HashMap<PidAndVpn,TranslationEntry> table;

    //全局的一个 页表  存放所有的物理页（对应 进程）
    private TranslationEntryWithPid[] globalTable;

    private static InvertedPageTable instance=null;


    protected static final char dbgVM='v';

    private InvertedPageTable(){
        table=new HashMap<PidAndVpn,TranslationEntry>();
        globalTable=new TranslationEntryWithPid[Machine.processor().getNumPhysPages()];
    }


    //获取反向页表的实例  全局公用同一个反向页表
    public static InvertedPageTable getInstance(){
        if(instance==null)instance=new InvertedPageTable();
        return instance;
    }

    //向反向页表中插入一页
    public boolean insertEntry(int pid,TranslationEntry entry)
    {
        PidAndVpn pidAndVpn = new PidAndVpn(pid,entry.vpn);
        if(table.containsKey(pidAndVpn)){
            Lib.debug(dbgVM, "\t反向页表中已经存在此页");
            return false;
        }
        Lib.debug(dbgVM, "\t插入反向页表:进程 "+pid+"   虚拟页为 "+entry.vpn+
                "    物理页为 "+entry.ppn+"以插入反向页表 ");
        table.put(pidAndVpn,entry);

        //如果则虚拟页在内存中，而tlb可以直接由处理器使用。
        if(entry.valid){

            globalTable[entry.ppn]=new TranslationEntryWithPid(entry,pid);
            Lib.debug(dbgVM, "\t此页已经加载到内存中["+entry.ppn+"]");
        }
        return true;
    }

    //将某进程的某虚拟页 从反向页表中删除
    public TranslationEntry deleteEntry(int pid,int vpn)
    {
        PidAndVpn pidAndVpn = new PidAndVpn(pid,vpn);
        TranslationEntry entry=table.get(pidAndVpn);
        if(entry == null)
        {
            Lib.debug(dbgVM, "\t此进程的此虚拟页不在反向页表中");
            return null;

        }
        //如果则虚拟页在内存中，则将其清空
        if(entry.valid){
            globalTable[entry.ppn]=null;
            Lib.debug(dbgVM, "\t此页已从内存中清空["+entry.ppn+"]");
        }
        return entry;
    }
    //设置某进程的某一个TranslationEntry
    public void setEntry(int pid,TranslationEntry newEntry)
    {
        PidAndVpn pidAndVpn=new PidAndVpn(pid,newEntry.vpn);
        //如果此TranslationEntry本身就不在反向页表中
        if(!table.containsKey(pidAndVpn)){
            Lib.debug(dbgVM, "\t此TranslationEntry不在反向页表中");
            return;
        }

        //从反向页表中取出旧TranslationEntry
        TranslationEntry oldEntry=table.get(pidAndVpn);
        if(oldEntry.valid){
            if(globalTable[oldEntry.ppn]==null){
                Lib.debug(dbgVM, "\t内存中不存在此虚拟页对应的物理页");
                return;
            }
            globalTable[oldEntry.ppn]=null;
            Lib.debug(dbgVM, "\t内存中此虚拟页对应的物理页清空["+oldEntry.ppn+"]");
        }
        if(newEntry.valid){
            if(globalTable[newEntry.ppn]!=null){
                Lib.debug(dbgVM, "\t内存中此虚拟页对应的物理页已被占用");
                return;
            }
            globalTable[newEntry.ppn]=new TranslationEntryWithPid(newEntry,pid);
            Lib.debug(dbgVM, "\t内存中此虚拟页对应的物理页已设置["+newEntry.ppn+"]");
        }
        table.put(pidAndVpn, newEntry);
    }
    public void updateEntry(int pID,TranslationEntry entry){
        PidAndVpn key=new PidAndVpn(pID,entry.vpn);
        if(table.containsKey(key)){
            Lib.debug(dbgVM, "\t此TranslationEntry已经存在在反向页表中");
            return;
        }
        TranslationEntry oldEntry=table.get(key);
        TranslationEntry newEntry=mix(entry,oldEntry);
        if(oldEntry.valid){
            if(globalTable[oldEntry.ppn]==null){
                return;
            }
            globalTable[oldEntry.ppn]=null;
            Lib.debug(dbgVM, "\t内存中此虚拟页对应的物理页清空["+oldEntry.ppn+"]");
        }
        if(newEntry.valid){
            if(globalTable[newEntry.ppn]!=null){
                return;
            }
            globalTable[newEntry.ppn]=new TranslationEntryWithPid(newEntry,pID);
            Lib.debug(dbgVM, "\t内存中此虚拟页对应的物理页已设置["+oldEntry.ppn+"]");
        }
        table.put(key, newEntry);
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
        PidAndVpn pidAndVpn=new PidAndVpn(pid,vpn);
        TranslationEntry entry=null;
        Lib.debug(dbgVM, "\t进程 "+pid+" 使用虚拟页 "+vpn+
                " 从反向页表中获取到对应的TranslationEntry  ");
        if(table.containsKey(pidAndVpn)){
            entry=table.get(pidAndVpn);
        }
        return entry;
    }

    //选取被置换的页
    public TranslationEntryWithPid getVictimPage(){
        TranslationEntryWithPid entry=null;
        do{
            int index=Lib.random(globalTable.length);
            entry=globalTable[index];
        }while(entry==null||!entry.getTranslationEntry().valid);
        return entry;
    }

}
