package nachos.network; ///NEW/////

import nachos.machine.MalformedPacketException;
import nachos.machine.OpenFile;

import java.util.Arrays;

//为网络连接新建的 文件类  每一个文件都会和  接收方和发送方的地址相关联
public class Connection extends OpenFile {
    public int sourceLink;
    public int destinationLink;

    public int sourcePort;
    public int destinationPort;

    public int currentSeqNum;
    public int SeqNum;

    public Connection(int destinationLink, int destinationPort, int sourceLink, int sourcePort) {
        super(null, "Connection");
        this.sourceLink = sourceLink;
        this.sourcePort = sourcePort;
        this.destinationLink = destinationLink;
        this.destinationPort = destinationPort;

        this.currentSeqNum = 0;
        this.SeqNum = 0;
    }

    public int read(byte[] buffer, int offset, int size)
    {
        //获取到指定端口上的数据
        UdpPacket packet = NetKernel.postOffice.receive(sourcePort);

        //如果包为空 则返回-1
        if(packet == null)
        {
            return -1;
        }

        //如果不为空  则增加当前接受到的消息的序列号
        currentSeqNum++;
        //防止读取到的内容不越界
        int bytesRead = Math.min(size, packet.payload.length);

        //将内容复制到目标数组
        System.arraycopy(packet.payload, 0, buffer, offset, bytesRead);

        return bytesRead;
    }

    public int write(byte[] buffer, int offset, int size)
    {
        int amt = Math.min(offset+size, buffer.length);


        byte[] elements = Arrays.copyOfRange(buffer, offset, amt);

        try {
            //写入新的包
            UdpPacket packet = new UdpPacket(destinationLink, destinationPort, sourceLink, sourcePort, UdpPacket.DATA ,SeqNum+1, elements);

            //然后发送
            NetKernel.postOffice.send(packet);

            //当前发送的序列号+1
            SeqNum++;

            return amt;
        }
        catch(MalformedPacketException e)
        {
            return -1;
        }


    }



}
