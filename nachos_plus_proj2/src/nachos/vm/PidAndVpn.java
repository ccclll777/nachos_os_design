package nachos.vm;


//管理 进程号  以及  虚拟页数 （反向页表                                       ）
public class PidAndVpn {
    private int  pid;
    private int VirtualPageNum;

    public PidAndVpn(int pid, int virtualPageNum) {
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
}
