package nachos.network; ///NEW/////


import nachos.machine.MalformedPacketException;
import nachos.machine.Packet;


public class UdpPacket {

    public Packet packet;
    public int destPort;//目标端口号  附加头的第一位
    public int srcPort;//源端口号     附加头的第二位
    public int status;//状态位
    public int seqNum;//序列号    后四位
    public byte[] payload;//此包的所有内容
    public int headerLength = 4;
    public int maxContentsLength = Packet.maxContentsLength - headerLength;//最大内容长度
    public static int   DATA = (0);
    public static int SYN = (1);
    public static int ACK = (2);
    public static int STP = (4);
    public static int FIN = (8);
    public static int SYNACK = (3);
    public UdpPacket(){}
    public UdpPacket(int dstLink, int destPort, int srcLink, int srcPort, int status, int seqNum, byte[] payload)throws MalformedPacketException {
        if (destPort < 0 || destPort >= maxPortLimit ||
                srcPort < 0 || srcPort >= maxPortLimit ||
                payload.length > maxContentsLength)
            throw new MalformedPacketException();

        this.destPort = (byte)destPort;
        this.srcPort = (byte)srcPort;
        this.status = status;
        this.seqNum = seqNum;
        this.payload = payload;

        byte[] contents =  new byte[headerLength + payload.length];

        contents[0] = (byte)destPort;
        contents[1] = (byte)srcPort;
        contents[2] = (byte)status;
        contents[3] = (byte)seqNum;
        //其他八位存放内容
        System.arraycopy(payload, 0, contents, headerLength, payload.length);
        //然后构造成32位的包
        packet = new Packet(dstLink, srcLink, contents);
    }

    public UdpPacket(Packet packet) throws MalformedPacketException {
        this.packet = packet;
        if(packet.contents.length < headerLength ||
                packet.contents[0] < 0 || packet.contents[0] >= maxPortLimit ||
                packet.contents[1] < 0 || packet.contents[1] >= maxPortLimit)
            throw new MalformedPacketException();

        destPort = packet.contents[0];
        srcPort  = packet.contents[1];
        status = packet.contents[2];
        seqNum = packet.contents[3];

        payload = new byte[packet.contents.length - headerLength];
        System.arraycopy(packet.contents, headerLength, payload, 0, payload.length);
    }

    @Override
    public String toString(){
        return "UdpPack || from: "+ packet.srcLink +":"+srcPort+" to: "+ packet.dstLink+":"+destPort+" "+payload.length+" bytes";
    }





    public static final int maxPortLimit = 128;
}