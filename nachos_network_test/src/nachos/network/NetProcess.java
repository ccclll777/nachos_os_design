package nachos.network; ///NEW/////

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.MalformedPacketException;
import nachos.machine.Packet;
import nachos.userprog.UserProcess;

/**
 * A <tt>VMProcess</tt> that supports networking syscalls.
 */
public class NetProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public NetProcess() {
        super();
    }

    /**
     * *这是连接功能。通常是客户打电话来的。
     * *它接受主机IP地址和要连接的端口。
     * *此函数发送一个syn包，并等待服务器用synack应答。
     * *一旦它得到synack，它就会用ack进行第三次握手。
     * @param host
     * @param port
     * @return
     */
    private int handleConnect(int host, int port)
    {
        int srcLink = Machine.networkLink().getLinkAddress();
        int srcPort = NetKernel.postOffice.getUnusedPort();
        int res;
        Connection connection = null;
        //检查是否存在相同的 文件描述符
        if( (res = checkExistingConnection(host, srcLink, port, port)) == -1) {
            //如果不存在则新建
            connection = new Connection(host, port, srcLink, srcPort);
            int i = findEmptyFileDescriptor();
            FileDescriptors[i].setFile(connection);
            FileDescriptors[i].setFileName("connect");
            res = i;
        }

        //如果存在寻找之前旧的文件描述符
        if(connection == null) connection = (Connection) FileDescriptors[res].getFile();
        srcPort = connection.sourcePort;

        try {
            //SYN表示此数据包是启动后的第一个数据包
            UdpPacket packet = new UdpPacket(host, port, srcLink, srcPort, UdpPacket.SYN,0, new byte[0]);
            //发送packet
            NetKernel.postOffice.send(packet);

            System.out.println("SYN包已发送，挂起等待回复");

            /**
             * 当用户进程调用connect（）系统调用时，活动端点发送同步数据包（syn）。
             * 这将导致创建挂起的连接。在被动端点的用户进程调用accept（）系统调用之前，
             * 此挂起连接的状态必须存储在接收器上。调用accept（）时，
             * 被动端向主动端发送一个syn确认数据包（syn/ack），并建立连接。
             */
            UdpPacket SynAckPack = NetKernel.postOffice.receive(srcPort);

            System.out.println("SYN包以收到回复");
            //收到确认的数据包
            if(SynAckPack.status == UdpPacket.SYNACK && Machine.networkLink().getLinkAddress() == SynAckPack.packet.dstLink){
                System.out.print("SYNACK已经收到: ");
                System.out.println(SynAckPack);
                //发回ack。
                UdpPacket ackPack = new UdpPacket(host, port, srcLink, srcPort, UdpPacket.ACK, SynAckPack.seqNum+1, new byte[0]);
                NetKernel.postOffice.send(ackPack);
                //确认发送此时可以发送数据。 连接已经建立
            }


        }catch (MalformedPacketException e){
            return -1;
        }




        return res;
    }

    /**
     * 尝试接受指定本地端口上的单个连接，并返回引用该连接的文件描述符。如果没有挂起的连接请求，
     * 则立即返回-1。在这两种情况下，accept（）返回而不等待远程主机。
     * 返回引用连接的新文件描述符，如果发生错误，则返回-1。
     * @param port
     * @return
     */
    private int handleAccept(int port) {

        Lib.assertTrue(port >= 0 && port < Packet.linkAddressLimit);

        UdpPacket mail = NetKernel.postOffice.receive(port);
        if(mail == null) {
            return -1;
        }
        //添加端口信息  已经包的序列号

        int srcPort = mail.destPort;
        int sourceLink = mail.packet.dstLink;
        int destinationLink = mail.packet.srcLink;
        int destinationPort = mail.srcPort;
        int seq = mail.seqNum + 1;

        int res;
        //确认文件描述符还不存在
        if( (res = checkExistingConnection(destinationLink, sourceLink, srcPort, destinationPort)) == -1){

            Connection conn = new Connection(destinationLink, destinationPort, sourceLink, srcPort);
            int i = -1;

           i = findEmptyFileDescriptor();
            FileDescriptors[i].setFile(conn);
            FileDescriptors[i].setFileName("handleAccept");
            res = i;
        }



        try {
            //确定他是请求连接的数据包  同时确保它被发送给正确的人
            UdpPacket ackPacket = null;
            if(mail.status == UdpPacket.SYN && Machine.networkLink().getLinkAddress() == mail.packet.dstLink) {
                ackPacket = new UdpPacket(destinationLink, destinationPort, sourceLink, srcPort, UdpPacket.SYNACK, seq, new byte[0]);
            }

            if(ackPacket == null)
                Lib.assertNotReached();
            //回复确认收到
            NetKernel.postOffice.send(ackPacket);

            UdpPacket ackPack = NetKernel.postOffice.receive(port);
            //当收到回复是 表示确认连接
            if(ackPack.status == UdpPacket.ACK && Machine.networkLink().getLinkAddress() == mail.packet.dstLink){
                System.out.print("连接建立：");
            }

        } catch (MalformedPacketException e) {

            return -1;
        }

        return res;
    }


    public int checkExistingConnection(int dest, int src, int srcport, int desport){

        for(int i = 2; i < FileDescriptors.length; i++){
            //看是否有相同的文件描述符
            if(FileDescriptors[i].getFile() != null && (FileDescriptors[i].getFile() instanceof Connection)){
                Connection con = (Connection) FileDescriptors[i].getFile();
                //如意已经存在 则返回索引号
                if(con.destinationLink == dest &&
                        con.sourceLink == src &&
                        con.sourcePort == srcport &&
                        con.destinationPort == desport
                        ) {
                    return i;
                }
            }
        }

        //如果不存在返回-1
        return -1;
    }



    private static final int
            syscallConnect = 11,
            syscallAccept = 12;

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
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        switch (syscall) {
            case syscallConnect:
                return handleConnect(a0,a1);
            case syscallAccept:
                return handleAccept(a0);

            default:
                return super.handleSyscall(syscall, a0, a1, a2, a3);
        }
    }



}
