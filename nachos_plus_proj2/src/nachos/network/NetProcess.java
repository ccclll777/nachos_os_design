package nachos.network; ///NEW/////

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.MalformedPacketException;
import nachos.machine.Packet;
import nachos.userprog.FileDescriptor;
import nachos.userprog.UserProcess;

/**
 * A <tt>VMProcess</tt> that supports networking syscalls.
 */
public class NetProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    private static final int RETRANSMIT_INTERVAL = 20000;
    private final int MAX_SOCKETS = 16;
    private Socket[] socketList;
    public NetProcess() {
        super();
        socketList = new Socket[MAX_SOCKETS];
    }
    protected int getAvailIndex() {
        for(int i = 2; i < MAX_SOCKETS; i++)
            if(socketList[i] == null)
                return i;
        return -1;
    }
    /**
     * *这是连接功能。通常是客户打电话来的。
     * *它接受主机IP地址和要连接的端口。
     * *此函数发送一个syn包，并等待服务器用synack应答。
     * *一旦它得到synack，它就会用ack进行第三次握手。
     *
     * @param host
     * @param port
     * @return
     */
    private int handleConnect(int host, int port) {
        int srcLink = Machine.networkLink().getLinkAddress();
//        if(NetKernel.postOffice.availPorts.isEmpty())
//        {
//            return -1;
//        }
//        int srcPort = NetKernel.postOffice.availPorts.first();
        int srcPort = NetKernel.postOffice.getUnusedPort();
//        NetKernel.postOffice.availPorts.remove(NetKernel.postOffice.availPorts.first());
        int res;
        Socket socket = null;
        //检查是否存在相同的 文件描述符
        if ((res = checkExistingConnection(host, srcLink, port, port)) == -1) {
            //如果不存在则新建
            socket = new Socket(host, port, srcLink, srcPort);
            int i = findEmptyFileDescriptor();
            FileDescriptors[i].setFile(socket);
            socketList[i] = socket;
            FileDescriptors[i].setFileName("connect");
            res = i;
        }

         //如果存在寻找之前旧的文件描述符
        if (socket == null) socket = (Socket) FileDescriptors[res].getFile();
        srcPort = socket.sourcePort;

        try {
            //SYN表示此数据包是启动后的第一个数据包
            UdpPacket packet = new UdpPacket(host, port, srcLink, srcPort, UdpPacket.SYN, 0, new byte[0]);
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
            while (SynAckPack == null)
            {
                NetKernel.alarm.waitUntil(RETRANSMIT_INTERVAL);
                SynAckPack = NetKernel.postOffice.receive(srcPort);
                if(SynAckPack != null)
                {
                    break;
                }
            }
            System.out.println("SYN包以收到回复");
            //收到确认的数据包
            if (SynAckPack.status == UdpPacket.SYNACK && Machine.networkLink().getLinkAddress() == SynAckPack.packet.dstLink) {
                System.out.print("SYNACK已经收到: ");
                System.out.println(SynAckPack);
                //发回ack。
                UdpPacket ackPack = new UdpPacket(host, port, srcLink, srcPort, UdpPacket.ACK, SynAckPack.seqNum + 1, new byte[0]);
                NetKernel.postOffice.send(ackPack);
                //确认发送此时可以发送数据。 连接已经建立
            }


        } catch (MalformedPacketException e) {
            return -1;
        }
        return res;
    }

    /**
     * 尝试接受指定本地端口上的单个连接，并返回引用该连接的文件描述符。如果没有挂起的连接请求，
     * 则立即返回-1。在这两种情况下，accept（）返回而不等待远程主机。
     * 返回引用连接的新文件描述符，如果发生错误，则返回-1。
     *
     * @param port
     * @return
     */
    private int handleAccept(int port) {
     Lib.assertTrue(port >= 0 && port < Packet.linkAddressLimit);

        UdpPacket mail = NetKernel.postOffice.receive(port);
        while (mail == null)
        {
            NetKernel.alarm.waitUntil(RETRANSMIT_INTERVAL);
            mail = NetKernel.postOffice.receive(port);
            if(mail != null)
            {
                break;
            }
        }
        if (mail == null) {
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
        if ((res = checkExistingConnection(destinationLink, sourceLink, srcPort, destinationPort)) == -1) {
            Socket conn = new Socket(destinationLink, destinationPort, sourceLink, srcPort);
            int i = -1;

            i = findEmptyFileDescriptor();
            FileDescriptors[i].setFile(conn);
            socketList[i] = conn;
            FileDescriptors[i].setFileName("handleAccept");
            res = i;
        }

        try {
            //确定他是请求连接的数据包  同时确保它被发送给正确的人
            UdpPacket ackPacket = null;
//            System.out.println(mail.toString());
            if (mail.status == UdpPacket.SYN && Machine.networkLink().getLinkAddress() == mail.packet.dstLink) {
                ackPacket = new UdpPacket(destinationLink, destinationPort, sourceLink, srcPort, UdpPacket.SYNACK, seq, new byte[0]);
            }

            if (ackPacket == null)
                Lib.assertNotReached();
            //回复确认收到
            NetKernel.postOffice.send(ackPacket);
            UdpPacket ackPack = NetKernel.postOffice.receive(port);
            while (ackPack == null)
            {
                NetKernel.alarm.waitUntil(RETRANSMIT_INTERVAL);
                ackPack = NetKernel.postOffice.receive(port);
                if(ackPack != null)
                {
                    break;
                }
            }
            //当收到回复是 表示确认连接
            if (ackPack.status == UdpPacket.ACK && Machine.networkLink().getLinkAddress() == mail.packet.dstLink) {
                System.out.print("连接建立：");
            }

        } catch (MalformedPacketException e) {

            return -1;
        }

        return res;
    }


    public int checkExistingConnection(int dest, int src, int srcport, int desport) {

        for (int i = 2; i < FileDescriptors.length; i++) {
            //看是否有相同的文件描述符
            if (FileDescriptors[i].getFile() != null && (FileDescriptors[i].getFile() instanceof Socket)) {
                Socket con = (Socket) FileDescriptors[i].getFile();
                //如意已经存在 则返回索引号
                if (con.destinationLink == dest &&
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

    private int handleRead(int fileDescriptor, int vaddr, int size) {
        // Check if the read wants UserProcess's handleRead instead (used for C code
        // statements like printf)
        if (fileDescriptor == 0 || fileDescriptor == 1) {
            return super.handleSyscall(syscallRead, fileDescriptor, vaddr, size, 0);
        }

        // Return -1 if the input is invalid
        if (size < 0 || (fileDescriptor >= MAX_SOCKETS || fileDescriptor < 0)
                || socketList[fileDescriptor] == null) {
            return -1;
        }
        FileDescriptor fd = FileDescriptors[fileDescriptor];
        if (fd.getFile() == null)
            return -1;
        //需要写入主存的内容
        byte[] buffer = new byte[size];
        int readSize = fd.getFile().read(buffer, 0, size);

        if (readSize <= 0)
            return 0;

        //从内存中读出数据  写入 虚拟内存（主存） 然后返回字节数
        int writeSize = writeVirtualMemory(vaddr, buffer, 0, readSize);
        return writeSize;
//        // Receive buffers from Socket
//        int bytesRead = 0;
//        UdpPacket udpPacket = NetKernel.postOffice.receive(socketList[fileDescriptor].sourcePort);
//
//        while (udpPacket == null)
//        {
//            NetKernel.alarm.waitUntil(RETRANSMIT_INTERVAL);
//            udpPacket = NetKernel.postOffice.receive(socketList[fileDescriptor].sourcePort);
//            if(udpPacket != null)
//            {
//                break;
//            }
//        }
//        byte[] receivedata = udpPacket.payload;
//        byte[] readBuffer;
//
//        if (receivedata == null) {
//            readBuffer = null;
//        } else {
//
//            readBuffer = new byte[receivedata.length];
//            System.arraycopy(receivedata, 0, readBuffer, 0, readBuffer.length);
//            if (readBuffer != null) {
//                // Print the read buffer and update number of bytes read
//                bytesRead += FileDescriptors[1].getFile().write(readBuffer, 0, readBuffer.length);
//            }
//            // Return error if nothing was written to the memory
//            if (bytesRead == 0)
//                return -1;
//
//            // Update the number of bytes read
//            socketList[fileDescriptor].bytesRead += bytesRead;
//            return bytesRead;
//        }
//        return 0;
    }
    private int handleWrite(int fileDescriptor, int vaddr, int size) {
        // Check if the read wants UserProcess's handleWrite instead
        if(fileDescriptor == 0 || fileDescriptor == 1) {
            return super.handleSyscall(syscallWrite, fileDescriptor, vaddr, size, 0);
        }

        // Return -1 if the input is invalid
        if(size < 0 || (fileDescriptor >= MAX_SOCKETS || fileDescriptor < 0)
                || socketList[fileDescriptor] == null) {
            return -1;
        }

//        // Count number of buffers to write and get the write buffer
//        byte[] writeBuffer = new byte[size];
//        int bytesToSend = readVirtualMemory(vaddr, writeBuffer, 0, size);
//
//        // Send the buffer and update the number of bytes sent
//        UdpPacket packet = null;
//        try {
//            packet = new UdpPacket(socketList[fileDescriptor].destinationLink, socketList[fileDescriptor].destinationPort, socketList[fileDescriptor].sourceLink, socketList[fileDescriptor].sourcePort, UdpPacket.DATA ,1, writeBuffer);
//            NetKernel.postOffice.send(packet);
//            socketList[fileDescriptor].bytesSent = bytesToSend;
//        } catch (MalformedPacketException e) {
//            e.printStackTrace();
//        }
//        return bytesToSend;
        FileDescriptor fd = FileDescriptors[fileDescriptor];
        if (fd.getFile() == null)
            return -1;
        byte[] buffer = new byte[size];
        //读取主存中的信息  虚拟内存
        int readSize = readVirtualMemory(vaddr, buffer);
        if (readSize == -1)
            return -1;

        //写入文件
        int returnValue = fd.getFile().write(buffer, 0, readSize);
        if (returnValue == -1)
            return -1;

        return returnValue;
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
     * @param syscall the syscall number.
     * @param a0      the first syscall argument.
     * @param a1      the second syscall argument.
     * @param a2      the third syscall argument.
     * @param a3      the fourth syscall argument.
     * @return the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        switch (syscall) {
            case syscallConnect:
                return handleConnect(a0, a1);
            case syscallAccept:
                return handleAccept(a0);
            case syscallRead:
                return handleRead(a0, a1, a2);
            case syscallWrite:
                return handleWrite(a0, a1, a2);

            default:
                return super.handleSyscall(syscall, a0, a1, a2, a3);
        }
    }


}
