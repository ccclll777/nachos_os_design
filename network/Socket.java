//package nachos.network;
//
//import nachos.machine.MalformedPacketException;
//import nachos.machine.OpenFile;
//
//import java.util.Arrays;
//
////
//public class Socket extends OpenFile {
//    public int sourceLink;   //源地址
//    public int destinationLink;//目标地址
//
//    public int sourcePort; // 源端口
//    public int destinationPort;//目标端口
//
//    public int currentReceiveSeqNum;
//    public int currentSendSeqNum;
//    public Socket(int destinationLink, int destinationPort, int sourceLink, int sourcePort) {
//        //传入文件名
//        super(null, "Socket");
//        this.sourceLink = sourceLink;
//        this.destinationLink = destinationLink;
//        this.sourcePort = sourcePort;
//        this.destinationPort = destinationPort;
//        this.currentReceiveSeqNum = 0;
//        this.currentSendSeqNum = 0;
//
//    }
//
//    public int read(byte[] buffer, int offset, int length)
//    {
//        //获取到指定端口上的数据
//        UdpPacket packet = NetKernel.postOffice.receive(sourcePort);
//        //如果包为空 则返回-1
//        if(packet == null)
//        {
//            return -1;
//        }
//        //如果不为空  则增加当前接受到的消息的序列号
//        currentReceiveSeqNum++;
//        //读取的大小是两者的最小值
//        int numBytesRead = Math.min(length, packet.payload.length);
//        //将内容复制到目标数组
//        System.arraycopy(packet.payload, 0, buffer, offset, numBytesRead);
//
//        return numBytesRead;
//    }
//
//    public int write(byte[] buf, int offset, int length)
//    {
//        int amount = Math.min(offset + length, buf.length);
//        byte[] contents = Arrays.copyOfRange(buf, offset, amount);
//
//        try {
//            //写入新的包
//            UdpPacket packet = new UdpPacket(destinationLink, destinationPort, sourceLink, sourcePort, UdpPacket.DATA, currentSendSeqNum + 1, contents);
//            System.out.println("write: " + packet);
//            NetKernel.postOffice.send(packet);
//            //当前发送的序列号+1
//            currentSendSeqNum++;
//            return amount;
//        }
//        catch (MalformedPacketException e) {
//            return -1;
//        }
//    }
//}
package nachos.network; ///NEW/////

import nachos.network.*;
import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.Arrays;
import java.lang.Math;

public class Socket extends OpenFile{
    public int sourceLink;
    public int destinationLink;

    public int sourcePort;
    public int destinationPort;

    public int currentSeqNum;
    public int SeqNum;

    public Socket(int destinationLink, int destinationPort, int sourceLink, int sourcePort) {
        super(null, "Connection");
        this.sourceLink = sourceLink;
        this.sourcePort = sourcePort;
        this.destinationLink = destinationLink;
        this.destinationPort = destinationPort;

        this.currentSeqNum = 0;
        this.SeqNum = 0;
    }

    public int handleRead(byte[] buffer, int offset, int size)
    {
        //create a new packet and receive it on the source port
        UdpPacket packet = NetKernel.postOffice.receive(sourcePort);

        //if the packet is not valid
        if(packet == null)
        {
            return -1;
        }

        //increment the current sequence number
        currentSeqNum++;

        //the amount of bytes read is the minimum of the 2
        int bytesRead = Math.min(size, packet.payload.length);

        //copy the array to the destination
        System.arraycopy(packet.payload, 0, buffer, offset, bytesRead);

        return bytesRead;
    }

    public int handleWrite(byte[] buffer, int offset, int size)
    {
        int amt = Math.min(offset+size, buffer.length);


        byte[] elements = Arrays.copyOfRange(buffer, offset, amt);

        try {
            //write the new packet
            UdpPacket packet = new UdpPacket(destinationLink, destinationPort, sourceLink, sourcePort, UdpPacket.DATA ,SeqNum+1, elements);

            //send the packet
            NetKernel.postOffice.send(packet);

            //increment the sequence number
            SeqNum++;

            return amt;
        }

        //invalid packet
        catch(MalformedPacketException e)
        {
            return -1;
        }//catch (Exception e){ Might have to catch any exception and return -1 if it doesn't work
        //     return -1;
        // }


    }



}
