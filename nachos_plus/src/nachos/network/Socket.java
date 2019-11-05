package nachos.network; ///NEW/////

import nachos.machine.Lib;
import nachos.machine.MalformedPacketException;
import nachos.machine.OpenFile;
import nachos.userprog.UserProcess;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

//为网络连接新建的 文件类  每一个文件都会和  接收方和发送方的地址相关联
public class Socket extends OpenFile {
    public int sourceLink;
    public int destinationLink;

    public int sourcePort;
    public int destinationPort;

    public int currentSeqNum;
    public int SeqNum;
    public int bytesSent;
    public int bytesRead;
    public Deque<byte[]> readBuffer;
    public int getSourcePort()
    {
        return sourcePort;
    }
    public Socket(int destinationLink, int destinationPort, int sourceLink, int sourcePort) {
        super(null, "Socket");
        this.sourceLink = sourceLink;
        this.sourcePort = sourcePort;
        this.destinationLink = destinationLink;
        this.destinationPort = destinationPort;

        this.currentSeqNum = 0;
        this.SeqNum = 0;
        readBuffer = new LinkedList<byte[]>();
        bytesSent = 0;
        bytesRead = 0;
    }

    public int read(byte[] buffer, int offset, int size)
    {
        //获取到指定端口上的数据  在这里阻塞
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
//        System.out.println("element内容为"+ Lib.bytesToString(elements,0,elements.length));

        try {
            //写入新的包
            UdpPacket packet = new UdpPacket(destinationLink, destinationPort, sourceLink, sourcePort, UdpPacket.DATA ,SeqNum+1, elements);
//            System.out.println("write中UdpPacket的内容为"+ Lib.bytesToString(packet.payload,0,packet.payload.length));
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
