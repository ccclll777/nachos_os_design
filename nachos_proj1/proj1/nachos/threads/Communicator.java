package nachos.threads;

import nachos.machine.Machine;

import java.util.ArrayList;
import java.util.List;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
//用条件变量实现信息的 send和receive
//speak方法 一直原子的等待 等到 对同一个对象的listen调用  send后阻塞  等待调用同一个对象的receive  在唤醒
//如果 有线程调用listen   则会阻塞  当有线程speak时才会 唤醒
//注意：这相当于零长度有界缓冲区；因为缓冲区没有空间，生产者和消费者必须直接交互，
//只需要一个锁
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {

        this.speakerSending = new Condition2(this.conditionLock);
        this.listenerReceiving = new Condition2(this.conditionLock);
        this.numOfListener = 0;
        this.numOfSpeaker = 0;
        this.message = 0;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {

        conditionLock.acquire();
        //如果有说者  或者没有听者  则挂起
        while (numOfSpeaker !=0)
        {
            speakerSending.sleep();
        }
        this.message = word;
//        System.out.println("写者"+KThread.currentThread().getName()+"写了"+this.message);
        numOfSpeaker++;
        while (numOfListener == 0)
        {
            speakerSending.sleep();

        }
        listenerReceiving.wake();
        numOfSpeaker --;
        speakerSending.wake();
        conditionLock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {

        conditionLock.acquire();
        numOfListener++;
        if(numOfListener == 1 && numOfSpeaker !=0 )
        {
            speakerSending.wake();
        }
//        System.out.println("读者"+KThread.currentThread().getName()+"读取了"+this.message);
        listenerReceiving.sleep();
        numOfListener--;
        conditionLock.release();
        return this.message;
    }

    private Lock conditionLock = new Lock();
    //表示有无speaker在等待
    private int numOfSpeaker;
    //表示有无listener在等待
    private int numOfListener;
    //是否已经send
    private Condition2 speakerSending;
    //是否已经receive
    private Condition2 listenerReceiving;
    private int message;
    public static void selfTest() {
       CommunicatorTest communicatorTest = new CommunicatorTest();
       communicatorTest.commTest(1);
    }
}
