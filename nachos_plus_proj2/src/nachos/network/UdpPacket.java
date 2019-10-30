//package nachos.network;
//
//import nachos.machine.Lib;
//import nachos.machine.MalformedPacketException;
//import nachos.machine.Packet;
//
//import java.util.Arrays;
//
//public class UdpPacket {
//    public Packet packet;
//    public int dstPort;//目标端口号  附加头的第一位
//    public int srcPort;//源端口号     附加头的第二位
//    public int status;//状态位
//    public int seqNum;//序列号    后四位
//    public byte[] contents;//此包的所有内容
//    public static final int maxStatus = 16;//最大的状态码
//    public static final int portLimit = 128;//最大的端口号
//    public static final int maxSeqNum = java.lang.Integer.MAX_VALUE;
//    //传输协议添加了一个附加的8字节头
//    public static final int headerLength = 8;
//    //content长度为16
//    public static final int maxContentsLength =
//            Packet.maxContentsLength - headerLength;//最大内容长度
//
//    public static int DATA = (0);
//    public static int SYN = (1);
//    public static int ACK = (2);
//    public static int STP = (4);
//    public static int FIN = (8);
//    public static int SYNACK = (3);
//    public UdpPacket(int dstLink, int dstPort, int srcLink, int srcPort, int status, int seqNum, byte[] contents) throws MalformedPacketException {
////        Lib.assertTrue(dstPort < 0 || dstPort >= portLimit ||
////                srcPort < 0 || srcPort >= portLimit ||
////                status < 0 || status >= maxStatus ||
////                seqNum > 0 || seqNum <= maxSeqNum ||
////                contents.length > maxContentsLength);
//
//
//        this.dstPort = (byte) dstPort;
//        this.srcPort = (byte) srcPort;
//        this.status = (byte) status;
//        this.seqNum = seqNum;
//        this.contents = contents;
//
//        byte[] packetContents = new byte[headerLength + contents.length];
//
//        packetContents[0] = (byte) dstPort;
//        packetContents[1] = (byte) srcPort;
//        packetContents[3] = (byte) status;
//        byte[] seqNumAsByte = Lib.bytesFromInt(seqNum);
//        //后四位存放虚拟号
//        System.arraycopy(seqNumAsByte, 0, packetContents, 4, seqNumAsByte.length);
//
//        //其他八位存放内容
//        System.arraycopy(contents, 0, packetContents, headerLength,
//                contents.length);
//        //然后构造成32位的包
//        this.packet = new Packet(dstLink, srcLink, packetContents);
//    }
//
//    public UdpPacket(Packet packet) throws MalformedPacketException{
//        this.packet = packet;
////        Lib.assertTrue(packet.contents.length < headerLength ||
////                packet.contents[0] < 0 || packet.contents[0] >= portLimit ||
////                packet.contents[1] < 0 || packet.contents[1] >= portLimit ||
////                packet.contents[3] < 0 || packet.contents[3] >= maxStatus );
//
//
//        this.dstPort = packet.contents[0];
//        this.srcPort  = packet.contents[1];
//        this.status = packet.contents[3];
//        this.seqNum = Lib.bytesToInt(Arrays.copyOfRange(packet.contents, 4, 8), 0);
//
//        contents = new byte[packet.contents.length - 4];
//        System.arraycopy(packet.contents, 4, contents, 0, contents.length);
//    }
//    public String toString() {
//        return "from (" + packet.srcLink + ":" + srcPort +
//                ") to (" + packet.dstLink + ":" + dstPort +
//                "), " + contents.length + " bytes";
//    }
//}
package nachos.network; ///NEW/////


import nachos.machine.MalformedPacketException;
import nachos.machine.Packet;


public class UdpPacket {

    public Packet packet;

    public int destPort;
    public int srcPort;
    int flag;
    public int seq;

    public byte[] payload;

    public int HEADER_LENGTH = 4;
    public int MAX_PAYLOAD_LENGTH = Packet.maxContentsLength - HEADER_LENGTH;

    public UdpPacket(){}

    public UdpPacket(int dstLink, int destPort, int srcLink, int srcPort, int flag, int seq, byte[] payload)throws MalformedPacketException{
        //Make sure we have a valid port range
        if (destPort < 0 || destPort >= maxPortLimit ||
                srcPort < 0 || srcPort >= maxPortLimit ||
                payload.length > MAX_PAYLOAD_LENGTH)
            throw new MalformedPacketException();

        this.destPort = (byte)destPort;
        this.srcPort = (byte)srcPort;
        this.flag = flag;
        this.seq = seq;
        this.payload = payload;

        byte[] contents =  new byte[HEADER_LENGTH + payload.length];

        contents[0] = (byte)destPort;
        contents[1] = (byte)srcPort;
        contents[2] = (byte)flag;
        contents[3] = (byte)seq;

        System.arraycopy(payload, 0, contents, HEADER_LENGTH, payload.length);

        packet = new Packet(dstLink, srcLink, contents);
    }

    public UdpPacket(Packet packet) throws MalformedPacketException{
        this.packet = packet;
        //check port range again and form packet
        if(packet.contents.length < HEADER_LENGTH ||
                packet.contents[0] < 0 || packet.contents[0] >= maxPortLimit ||
                packet.contents[1] < 0 || packet.contents[1] >= maxPortLimit)
            throw new MalformedPacketException();

        destPort = packet.contents[0];
        srcPort  = packet.contents[1];
        flag = packet.contents[2];
        seq = packet.contents[3];

        payload = new byte[packet.contents.length - HEADER_LENGTH];
        System.arraycopy(packet.contents, HEADER_LENGTH, payload, 0, payload.length);
    }

    @Override
    public String toString(){
        return "UdpPack || from: "+ packet.srcLink +":"+srcPort+" to: "+ packet.dstLink+":"+destPort+" "+payload.length+" bytes";
    }


    public static int   DATA = (0);
    public static int SYN = (1);
    public static int ACK = (2);
    public static int STP = (4);
    public static int FIN = (8);
    public static int SYNACK = (3);



    //public static final int newHeaderLength = 2;
    //public static final int maxUdpLength = Packet.maxContentsLength - newHeaderLength;
    public static final int maxPortLimit = 128;
}