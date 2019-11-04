package nachos.network;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.MalformedPacketException;
import nachos.machine.Packet;
import nachos.threads.KThread;
import nachos.threads.Lock;
import nachos.threads.Semaphore;
import nachos.threads.SynchList;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeSet;

/**
 * A collection of message queues, one for each local port. A
 * <tt>PostOffice</tt> interacts directly with the network hardware. Because
 * of the network hardware, we are guaranteed that messages will never be
 * corrupted, but they might get lost.
 * * 消息队列的集合，每个本地端口一个。
 * * <tt>PostOffice</tt> 与网络硬件直接交互。由于网络硬件的原因，我们保证消息不会被破坏，但它们可能会丢失。
 * <p>
 * The post office uses a "postal worker" thread to wait for messages to arrive
 * from the network and to place them in the appropriate queues. This cannot
 * be done in the receive interrupt handler because each queue (implemented
 * with a <tt>SynchList</tt>) is protected by a lock.
 * <p>
 * * post office使用“postal worker”线程等待来自网络的messages到达，并将它们放置在适当的队列中。
 * * 无法在接收中断处理程序中执行此操作，
 * * 因为每个队列（使用<tt>synchlist</tt>实现）都受锁保护。
 */
public class PostOffice {
    /**
     * Allocate a new post office, using an array of <tt>SynchList</tt>s.
     * Register the interrupt handlers with the network hardware and start the
     * "postal worker" thread.
     * <p>
     * * 使用<tt>synchlist</tt>s数组分配新 post office,。
     * * <p>
     * * 向网络硬件注册中断处理程序并启动“postal worker”线程。
     */
    public PostOffice() {
        messageReceived = new Semaphore(0);
        messageSent = new Semaphore(0);
        sendLock = new Lock();
        portLock = new Lock();

        queues = new SynchList[MailMessage.portLimit];
        unackMessages = new HashSet<UdpPacket>();//未确认消息
        for (int i = 0; i < queues.length; i++)
            queues[i] = new SynchList();
        waitingDataMessages = new Deque[MailMessage.portLimit];
        availPorts = new TreeSet<Integer>();
        for (int i = 0; i < MailMessage.portLimit; i++) {
            availPorts.add(i);
            waitingDataMessages[i] = new LinkedList<UdpPacket>();
        }
        Runnable receiveHandler = new Runnable() {
            public void run() {
                receiveInterrupt();
            }
        };
        Runnable sendHandler = new Runnable() {
            public void run() {
                sendInterrupt();
            }
        };
        Machine.networkLink().setInterruptHandlers(receiveHandler,
                sendHandler);

        KThread t = new KThread(new Runnable() {
            public void run() {
                try {
                    postalDelivery();
                } catch (MalformedPacketException e) {
                    e.printStackTrace();
                }
            }
        });

        KThread resend = new KThread(new Runnable() {
            public void run() { resendAll(); }
        });
//        resend.fork();
        t.fork();
    }

    /**
     * Retrieve a message on the specified port, waiting if necessary.
     * 检索指定端口上的消息，必要时等待。
     *
     * @param port the port on which to wait for a message.
     *             等待消息的端口。
     * @return the message received.
     * 收到的消息
     */
    //public MailMessage receive(int port) {
    public UdpPacket receive(int port) {

//        Lib.assertTrue(port >= 0 && port < queues.length);

        Lib.debug(dbgNet, "waiting for mail on port " + port);

        //MailMessage mail = (MailMessage) queues[port].removeFirst();//Dont want to return mail.Return our thing
//        UdpPacket mail = (UdpPacket) queues[port].removeFirst();

//        if (Lib.test(dbgNet))
//            System.out.println("got mail on port " + port + ": " + mail);
        return (waitingDataMessages[port].isEmpty()) ? null : waitingDataMessages[port].removeFirst();
//        return mail;
    }

    /**
     * Wait for incoming messages, and then put them in the correct mailbox.
     * 等待收到的邮件，然后将它们放入正确的邮箱。
     */
    private void postalDelivery() throws MalformedPacketException {
        while (true) {
            messageReceived.P();

            Packet p = Machine.networkLink().receive();

            UdpPacket mail;

            try {
                mail = new UdpPacket(p);
            } catch (MalformedPacketException e) {
                continue;
            }

//            //如果是回复包
//            if(mail.status == UdpPacket.ACK )
//            {
//                for(UdpPacket m : unackMessages)
//                {
//                    if(m.destPort == mail.srcPort && m.packet.dstLink == mail.packet.srcLink && m.seqNum == mail.seqNum)
//                    {
//                        unackMessages.remove(m);
//                        break;
//                    }
//                }
//            }
////            如果是数据包  则添加到对应端口的队列
//            if(mail.status == UdpPacket.DATA )
//            {
//                waitingDataMessages[mail.destPort].add(mail);
//                //然后构造返回包
//                UdpPacket ackmail = new UdpPacket(mail.packet.srcLink, mail.destPort,mail.packet.dstLink, mail.srcPort, mail.status,mail.seqNum,mail.payload);
//                send(ackmail);
//            }



            if (Lib.test(dbgNet))
                System.out.println("delivering mail..."
                        + ": " + mail);

//            System.out.println("收到数据：" + Lib.bytesToString(mail.payload, 0, mail.payload.length) + "从" + mail.packet.srcLink+" 的 " +mail.srcPort);
            waitingDataMessages[mail.destPort].add(mail);

//            // 自动将邮件添加到邮箱并唤醒等待线程
            queues[mail.destPort].add(mail);
            //queues[mail.destPort].free = false;
            setPortUsed(mail.destPort);
        }
    }

    /**
     * Called when a packet has arrived and can be dequeued from the network
     * link.
     * 当数据包到达并可以从网络链路中退出队列时调用。
     */
    private void receiveInterrupt() {
        messageReceived.V();
    }

    /**
     * Send a message to a mailbox on a remote machine.
     */
    public void send(UdpPacket mail) {
        if (Lib.test(dbgNet))
            System.out.println("sending mail: " + mail);

        sendLock.acquire();
//        System.out.println("send发出数据：" + Lib.bytesToString(mail.payload,0,mail.payload.length) + "到" + mail.packet.dstLink+" 的 " +mail.destPort);
        Machine.networkLink().send(mail.packet);
        unackMessages.add(mail);
        messageSent.P();

        sendLock.release();
    }

    /**
     * Called when a packet has been sent and another can be queued to the
     * network link. Note that this is called even if the previous packet was
     * dropped.
     */
    private void sendInterrupt() {
        messageSent.V();
    }

    //标记已经使用的端口
    public int getUnusedPort() {
        portLock.acquire();
        int i = 0;
        for (SynchList obj : queues) {
            if (obj.free) {
                obj.free = false;
                return i;
            }
            i++;
        }
        portLock.release();
        return -1;
    }

    public void setPortUsed(int i) {
        portLock.acquire();
        if (queues[i].free)
            queues[i].free = false;
        portLock.release();
    }
    private void resendAll() {
        while(true) {
            Lock lock = new Lock();
            lock.acquire();

            for(UdpPacket m : unackMessages)
                send(m);

            lock.release();
            NetKernel.alarm.waitUntil(RETRANSMIT_INTERVAL);
        }
    }
    private static final int RETRANSMIT_INTERVAL = 20000;
    private Lock portLock;
    private SynchList[] queues;
    private Semaphore messageReceived;    // 当消息出队列时唤醒
    private Semaphore messageSent;    // 到消息入队列时唤醒
    private Lock sendLock;
    private static final char dbgNet = 'n';
    public HashSet<UdpPacket> unackMessages;//未确认消息
    public TreeSet<Integer> availPorts;//可分配端口
    public Deque<UdpPacket>[] waitingDataMessages;

}
