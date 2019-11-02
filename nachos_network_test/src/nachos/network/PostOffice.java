package nachos.network;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.MalformedPacketException;
import nachos.machine.Packet;
import nachos.threads.KThread;
import nachos.threads.Lock;
import nachos.threads.Semaphore;
import nachos.threads.SynchList;

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

        //queues = new SynchList[MailMessage.portLimit];
        queues = new SynchList[UdpPacket.maxPortLimit];
        for (int i = 0; i < queues.length; i++)
            queues[i] = new SynchList();

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
                postalDelivery();
            }
        });

        t.fork();
    }

    /**
     * Retrieve a message on the specified port, waiting if necessary.
     * 检索指定端口上的消息，必要时等待。
     *
     * @param    port    the port on which to wait for a message.
     * 等待消息的端口。
     * @return the message received.
     * 收到的消息
     */
    //public MailMessage receive(int port) {
    public UdpPacket receive(int port) {

        Lib.assertTrue(port >= 0 && port < queues.length);

        Lib.debug(dbgNet, "waiting for mail on port " + port);

        //MailMessage mail = (MailMessage) queues[port].removeFirst();//Dont want to return mail.Return our thing
        UdpPacket mail = (UdpPacket) queues[port].removeFirst();

        if (Lib.test(dbgNet))
            System.out.println("got mail on port " + port + ": " + mail);

        return mail;
    }

    /**
     * Wait for incoming messages, and then put them in the correct mailbox.
     * 等待收到的邮件，然后将它们放入正确的邮箱。
     */
    private void postalDelivery() {
        while (true) {
            messageReceived.P();

            Packet p = Machine.networkLink().receive();

            UdpPacket mail;

            try {
                mail = new UdpPacket(p);
            } catch (MalformedPacketException e) {
                continue;
            }

            if (Lib.test(dbgNet))
                System.out.println("delivering mail..."
                        + ": " + mail);

            // 自动将邮件添加到邮箱并唤醒等待线程
            queues[mail.destPort].add(mail);
            //queues[mail.destPort].free = false;
            setPortUsed(mail.destPort);
        }
    }

    /**
     * Called when a packet has arrived and can be dequeued from the network
     * link.
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
        Machine.networkLink().send(mail.packet);
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

    private Lock portLock;
    private SynchList[] queues;
    private Semaphore messageReceived;    // 当消息出队列时唤醒
    private Semaphore messageSent;    // 到消息入队列时唤醒
    private Lock sendLock;
    private static final char dbgNet = 'n';
}
