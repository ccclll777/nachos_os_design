package nachos.threads;

import nachos.machine.*;

/**
 * A Tester for the KThread class. 
 * Really, this tests the join() implementation.
 */
public class KThreadTest {

    /**
     * LoopThread class, which implements a KThread
     * that simply prints out numbers in sequence.
     */
    private static class LoopThread implements Runnable {
	LoopThread(String name, int upTo) {
	    this.name = name;
	    this.upTo = upTo;
	}
	
	public void run() {
	    for (int i=0; i<upTo; i++) {
		//System.out.println("*** " + name + " looped " + i + " times");
		KThread.yield();
	    }
	    System.out.println("*** " + name + " done");
	}

	/* An ID for output purposes */
	private String name;
	/* The maximum number of iterations */
	private int upTo;
    }

    /**
     * JoinThread class, which implements a KThread
     * that attempts to join with one or two threads, in sequence.
     */
    private static class JoinThread implements Runnable {
	JoinThread(String name, KThread thread1, KThread thread2) {
	    this.name = name;
	    this.thread1 = thread1;
	    this.thread2 = thread2;
	}
	
	public void run() {
            /* Joining with the first thread, if non-null */
	    if (thread1 != null) {
   	      System.out.println("*** "+name+" joining with "+thread1.toString());
	      thread1.join();
   	      System.out.println("*** "+name+" joined with "+thread1.toString());
            }
            /* Joining with the second thread, if non-null */
	    if (thread2 != null) {
   	      System.out.println("*** "+name+" joining with "+thread2.toString());
	      thread2.join();
   	      System.out.println("*** "+name+" joined with "+thread2.toString());
            }
   	    System.out.println("*** "+name+" done.");
	}

	/* An ID for output purposes */
	private String name;
	/* The maximum number of iterations */
	private KThread thread1;
	private KThread thread2;
    }

    /**
     * Tests whether this module is working.
     */
    public static void runTest() {

	System.out.println("**** KThread testing begins ****");
	
        /* Create 4 LoopThread, each one looping 3*(i+1) times, so
         * that the last create thread loops longer  */
	KThread loopThreads[] = new KThread[5];
        for (int i=0; i < 5; i++) {
          loopThreads[i] = new KThread(new LoopThread("loopThread"+3*(i+1),3*(i+1)));
          loopThreads[i].setName("loopThread"+3*(i+1));
          loopThreads[i].fork();
        }

        /* Create a JoinThread that waits for loopThread #1 
         * and then for loopThread #3 */
	KThread joinThread1 = new KThread(new JoinThread(
                   "joinThread #1",loopThreads[1],loopThreads[3]));
        joinThread1.setName("joinThread #1");
        joinThread1.fork();

        /* Create a JoinThread that waits for loopThread #4 
         * and then for loopThread #2 */
	KThread joinThread2 = new KThread(new JoinThread(
                   "joinThread #2",loopThreads[4],loopThreads[2]));
        joinThread2.setName("joinThread #2");
        joinThread2.fork();

        /* Create a JoinThread that waits for joinThread #1 
         * and then for loopThread #4 */
	KThread joinThread3 = new KThread(new JoinThread(
                   "joinThread #3",joinThread1,loopThreads[4]));
        joinThread3.setName("joinThread #3");
        joinThread3.fork();

	/* Join with all the above to wait for the end of the testing */
        for (int i=0; i < 5; i++) {
	  loopThreads[i].join();
	}
	joinThread1.join();
	joinThread2.join();
	joinThread3.join();

	System.out.println("**** KThread testing ends ****");
    }

}
