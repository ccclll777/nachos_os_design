package nachos.network;

import nachos.machine.*;
import nachos.vm.*;

/**
 * A <tt>VMProcess</tt> that supports networking syscalls.
 */
public class NetProcess extends VMProcess {
    /**
     * Allocate a new process.
     */
    public NetProcess() {
        super();
    }

    private static final int
            syscallConnect = 11,
            syscallAccept = 12;

    private int handleAccept(int port) {
        Lib.assertTrue(port >= 0 && port < Packet.linkAddressLimit);

        UdpPacket updPacket = NetKernel.postOffice.receive(port);
        if(updPacket == null) {
            return -1;
        }
        int srcPort = updPacket.destPort;
        int sourceLink = updPacket.packet.dstLink;
        int destinationLink = updPacket.packet.srcLink;
        int destinationPort = updPacket.srcPort;
        int seqNum = updPacket.seq + 1;
        int index = findEmptyFileDescriptor();
        if (index == -1) {
            Lib.debug(dbgProcess, "已超出用户进程拥有文件描述符的最大数量");
            return -1;
        }
        Socket conn = new Socket(destinationLink, destinationPort, sourceLink, srcPort);
        FileDescriptors[index].setFileName(String.valueOf(pid + index) + "connection");
        FileDescriptors[index].setFile(conn);

        try {
            //确定他是请求连接的数据包  同时确保它被发送给正确的人
            UdpPacket ackPacket = null;
            if(updPacket.flag == UdpPacket.SYN && Machine.networkLink().getLinkAddress() == updPacket.packet.dstLink) {
                //回复确认
                ackPacket = new UdpPacket(destinationLink, destinationPort, sourceLink, srcPort, UdpPacket.SYNACK, seqNum, new byte[0]);
            }
            NetKernel.postOffice.send(ackPacket);
            //收到确认的回复
            UdpPacket ackPack = NetKernel.postOffice.receive(port);
            //当收到回复是 表示确认连接
            if(ackPack.flag == UdpPacket.ACK && Machine.networkLink().getLinkAddress() == updPacket.packet.dstLink){
                System.out.print("连接建立");
            }

        } catch (MalformedPacketException e) {
            Lib.assertNotReached("确认数据包格式错误");
            return -1;
        }

        return index;

    }

    /**
     * 尝试启动到指定远程主机上指定端口的新连接，
     * 并返回引用该连接的新文件描述符。如果远程主机不立即响应，
     * 则connect（）不会放弃。返回新的文件描述符，如果发生错误，则返回-1。
     *
     * @param host
     * @param port
     * @return
     */
    private int handleConnect(int host, int port) {
        int srcLink = Machine.networkLink().getLinkAddress();
        int srcPort = NetKernel.postOffice.PortAvailable();

        Socket connection = new Socket(host, port, srcLink, srcPort);


        //打开一个文件 需要返回一个文件描述符
        int index = findEmptyFileDescriptor();
        if (index == -1) {
            Lib.debug(dbgProcess, "已超出用户进程拥有文件描述符的最大数量");
            return -1;
        }
        FileDescriptors[index].setFileName(String.valueOf(pid + index) + "connectionfile");
        FileDescriptors[index].setFile(connection);


        try {
            //SYN表示此数据包是启动后的第一个数据包
            UdpPacket packet = new UdpPacket(host, port, srcLink, srcPort, UdpPacket.SYN, 0, new byte[0]);
            //发送packet
            NetKernel.postOffice.send(packet);
            /**
             * 当用户进程调用connect（）系统调用时，活动端点发送同步数据包（syn）。
             * 这将导致创建挂起的连接。在被动端点的用户进程调用accept（）系统调用之前，
             * 此挂起连接的状态必须存储在接收器上。调用accept（）时，
             * 被动端向主动端发送一个syn确认数据包（syn/ack），并建立连接。
             */

            UdpPacket SynAckPack = NetKernel.postOffice.receive(srcPort);
            //收到确认的数据包
            if (SynAckPack.flag == UdpPacket.SYNACK && Machine.networkLink().getLinkAddress() == SynAckPack.packet.dstLink) {
                System.out.println("收到确认链接数据包: ");
                System.out.print(SynAckPack);
                //发送确认收到确认的数据包
                UdpPacket ackPack = new UdpPacket(host, port, srcLink, srcPort, UdpPacket.ACK, SynAckPack.seq + 1, new byte[0]);
                NetKernel.postOffice.send(ackPack);
                //ack sent. At this point its okay to send data.
            }

        } catch (MalformedPacketException e) {
            System.out.println("Malformed packet exception");
            Lib.assertNotReached();
            return -1;
        }


        return index;
    }




    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>11</td><td><tt>int  connect(int host, int port);</tt></td></tr>
     * <tr><td>12</td><td><tt>int  accept(int port);</tt></td></tr>
     * </table>
     *
     * @param syscall the syscall number.
     * @param a0      the first syscall argument.
     * @param a1      the second syscall argument.
     * @param a2      the third syscall argument.
     * @param a3      the fourth syscall argument.
     * @return the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        switch (syscall) {
            case syscallAccept:
                return handleAccept(a0);
            case syscallConnect:
                return handleConnect(a0, a1);
            default:
                return super.handleSyscall(syscall, a0, a1, a2, a3);
        }
    }
}
