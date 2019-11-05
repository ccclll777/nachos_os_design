package nachos.vm;


//管理 进程号  以及  虚拟页数 （反向页表                                       ）
public class VirtualPageFlag {
    private int pid;
    private int VirtualPageNum;

    public VirtualPageFlag(int pid, int virtualPageNum) {
        this.pid = pid;
        VirtualPageNum = virtualPageNum;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public int getVirtualPageNum() {
        return VirtualPageNum;
    }

    public void setVirtualPageNum(int virtualPageNum) {
        VirtualPageNum = virtualPageNum;
    }

    @Override
    public boolean equals(Object that) {
        if (that == null) return false;
        return this.pid == ((VirtualPageFlag) that).pid && this.VirtualPageNum == ((VirtualPageFlag) that).VirtualPageNum;
    }

    @Override
    public int hashCode() {
        return (new Integer(pid).toString() + new Integer(VirtualPageNum).toString()).hashCode();
    }
}
