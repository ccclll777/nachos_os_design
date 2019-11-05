package nachos.threads;

import nachos.machine.Lib;

import java.util.LinkedList;

/**
 * A synchronized queue.
 */
public class SynchList {
    /**
     * Allocate a new synchronized queue.
	 * 分配新的同步队列。
     */
    public SynchList() {
	list = new LinkedList<Object>();
	lock = new Lock();
	listEmpty = new Condition(lock);
		free = true;
    }

    /**
     * Add the specified object to the end of the queue. If another thread is
     * waiting in <tt>removeFirst()</tt>, it is woken up.
     *
	 * 将指定的对象添加到队列的末尾。如果另一个线程在removefirst（）中等待，它将被唤醒。
	 *
     * @param	o	the object to add. Must not be <tt>null</tt>.
     */
    public void add(Object o) {
	Lib.assertTrue(o != null);
	
	lock.acquire();
	list.add(o);
	listEmpty.wake();
	lock.release();
    }

    /**
     * Remove an object from the front of the queue, blocking until the queue
     * is non-empty if necessary.
     *从队列前面移除一个对象，如有必要，阻塞直到队列非空。
     * @return	the element removed from the front of the queue.
	 * 从队列前面移除的元素。
     */
    public Object removeFirst() {
	Object o;

	lock.acquire();
	while (list.isEmpty())
	{
		listEmpty.sleep();


	}

	o = list.removeFirst();
	lock.release();

	return o;
    }

    private static class PingTest implements Runnable {
	PingTest(SynchList ping, SynchList pong) {
	    this.ping = ping;
	    this.pong = pong;
	}
	
	public void run() {
	    for (int i=0; i<10; i++)
		pong.add(ping.removeFirst());
	}

	private SynchList ping;
	private SynchList pong;
    }

    /**
     * Test that this module is working.
     */
    public static void selfTest() {
	SynchList ping = new SynchList();
	SynchList pong = new SynchList();

	new KThread(new PingTest(ping, pong)).setName("ping").fork();

	for (int i=0; i<10; i++) {
	    Integer o = new Integer(i);
	    ping.add(o);
	    Lib.assertTrue(pong.removeFirst() == o);
	}
    }

    private LinkedList<Object> list;
    private Lock lock;
    private Condition listEmpty;

	public boolean free;//判断此端口是否已经被占用
}

