package nachos.threads;

import java.util.Random;

/**
 * A Tester for the Condition2 class
 */
public class Condition2Test {

    /**
     * ProdConsBuffer class, which implements a producer/consumer buffer. 
     * This implementation uses several condition variables, and some 
     * "tricks" to synchronize everything at the end of the execution. This
     * could be done better with a join() call to wait for threads to 
     * finish. But at the moment, the main thread waits to be signaled 
     * that all producers and consumers have exited. This demonstrate that  
     * synchronization primitives can be built on top of others, or each other.
     */
    private static class ProdConsBuffer {

      /**
       * Constructor: takes as parameter the maximum number
       *   of actions (i.e., producing or consuming an item)
       *   before all producers and consumers call it quit. This
       *   is to avoid an infinite execution.
       */
      public ProdConsBuffer(int maxNumActions) {

	/* Initially no actions have been performed */
        this.numActions = 0;
	this.maxNumActions = maxNumActions;
	this.isDone = false;

	/* Initially no threads are done */
	this.numFinishedThreads = 0;

	/* Initially the buffer is empty */
        this.lastItemIndex = -1;
    	this.buffer = new int[ProdConsBuffer.maxNumItems];
        for (int i = 0; i < ProdConsBuffer.maxNumItems; i++)
          this.buffer[i] = -1;
	this.isEmpty = true;
        this.isFull = false;

	/* Create the mutex and the two condition variables */
        this.mutex = new Lock();
        this.isNotEmptyCond = new Condition2(this.mutex);
        this.isNotFullCond = new Condition2(this.mutex);
        this.isOverCond = new Condition2(this.mutex);

	/* Create the RNG to generate random items */
	this.rng = new Random();
      }

      /**
       *  Method to consume an item. This method is NOT thread-safe
       *  and should by called within a critical section via this.mutex.
       *  This function should never be called on an empty buffer.
       */
      public int consumeItem() {
        int item;

	/* Is the buffer empty??? */
  	if (lastItemIndex == -1) {
          System.out.println("Error: Can't consume item because buffer is empty!!");
          return -1;
        }

	/* Consume the item */
        lastItemIndex--;
        item = buffer[lastItemIndex+1];
        buffer[lastItemIndex+1] = -1;

        /* Sanity check */
        if (item == -1) {
          System.out.println("Error: Consumed an invalid item!!");
          return -1;
        }

	/* Update isEmpty and isFull */
        isEmpty = (lastItemIndex == -1);
    	isFull = false;

	/* One additional action was performed */
	isDone = (++numActions >= maxNumActions);
	
	/* if we're done, wake up all sleepers */
        if (isDone) {
	  this.isNotFullCond.wakeAll();
	  this.isNotEmptyCond.wakeAll();
        }

	return item;
      }

      /**
       *  Method to produce an item. This method is NOT thread-safe
       *  and should by called within a critical section via this.mutex.
       *  This function should never be called on a full buffer.
       */
      public void produceItem(int item) {

	/* Is the buffer full?? */
  	if (lastItemIndex == maxNumItems - 1) {
          System.out.println("Error: Can't produce item because buffer is full!!");
          return;
        }
	
        /* Sanity check */
        if (buffer[lastItemIndex+1] != -1) {
          System.out.println("Error: Produced item at an invalid position!!");
          return;
        }

	/* Produce an item */
        lastItemIndex++;
        buffer[lastItemIndex] = item;

	/* Update isFull and isEmpty */
        isFull = (lastItemIndex == maxNumItems-1);
	isEmpty = false;

      	/* One additional action was performed */
	isDone = (++numActions >= maxNumActions);

	/* if we're done, wake up all sleepers */
        if (isDone) {
	  this.isNotFullCond.wakeAll();
	  this.isNotEmptyCond.wakeAll();
        }

        return;
      }

      /** Method to generate a random number 
       */
      public int generateRandomItem() {
        return rng.nextInt(50); /* between 0 and 50 */
      }

      /* Lock for mutual exclusion and condition variables */
      public Lock mutex;

      /* Condition variables */
      public Condition2 isNotEmptyCond; /* signaled when the buffer becomes non-empty */
      public Condition2 isNotFullCond;  /* signaled when the buffer becomes non-full */
      public Condition2 isOverCond;     /* signaled by each finishing prod or cons */

      /* Booleans indicating buffer state */
      public boolean isEmpty;
      public boolean isFull;

      /* The buffer of elements */
      private static final int maxNumItems = 10;
      private int lastItemIndex;
      private int buffer[];

      /* The global counter of actions and flag */
      private int maxNumActions;
      private int numActions;
      public boolean isDone;

      /* The number of threads that are finished */
      public int numFinishedThreads;

      /* Random number generator */
      private Random rng;
    }

    /**
     * Producer class, which implements a producer thread
     * that puts data in a buffer.
     */
    private static class Producer implements Runnable {

	/* Constructor */
	Producer(int who, ProdConsBuffer buffer) {
	    this.buffer = buffer;
	    this.who = who;
	}
	
	public void run() {

          System.out.println("** Producer #"+who+" begins");
	  /* Loop  */
	  while (true) {
	    /* Acquire the mutex */
            buffer.mutex.acquire();
	    /* If the buffer is full, wait on isNotFullCond. This is
             * in a while loop to avoid spurious wake-ups */
            while (!buffer.isDone && buffer.isFull) {
              System.out.println("** Producer #"+who+" waits for the buffer to not be full");
 	      buffer.isNotFullCond.sleep();  /* releases the mutex and reacquires 
                                              * it when it wakes up */
            }

            /* I just woke up, and perhaps it's because it's all over 
             * in which case I exit from my main loop */
	    if (buffer.isDone) {
              buffer.mutex.release();
              break;
            }
	    /* Produce an item */
 	    int producedItem = buffer.generateRandomItem();
            System.out.println("** Producer #"+who+" produces "+producedItem);
	    buffer.produceItem(producedItem);
	    /* Wake up potential consumers */
	    buffer.isNotEmptyCond.wake();
	    /* Release the mutex */
            buffer.mutex.release();
	    /* Yield so that somebody else has a chance to run */
	    KThread.yield();
          }
          System.out.println("** Producer #"+who+" exits");

	  /* Signal that the thread is finished */
	  buffer.mutex.acquire();
	  buffer.numFinishedThreads++;
	  buffer.isOverCond.wake();
	  buffer.mutex.release();
	}

	/* The Prod/Cons buffer */
	private ProdConsBuffer buffer;
	/* An ID for printing out information */
	private int who;
    }

    private static class Consumer implements Runnable {
	Consumer(int who, ProdConsBuffer buffer) {
	    this.buffer = buffer;
	    this.who = who;
	}
	
	public void run() {
	
          System.out.println("** Consumer #"+who+" begins");
	  /* Loop */
	  while (true) {

	    /* Acquire the mutex */
            buffer.mutex.acquire();
	    /* If the buffer is empty, wait on isNotEmptyCond. This is
             * in a while loop to avoid spurious wake-ups */
            while (!buffer.isDone && buffer.isEmpty) {
              System.out.println("** Consumer #"+who+" waits for the buffer to not be empty");
 	      buffer.isNotEmptyCond.sleep(); /* releases the mutex and reacquires 
                                              * it when it wakes up */
            }

            /* I just woke up, and perhaps it's because it's all over 
             * in which case I exit from my main loop */
	    if (buffer.isDone) {
              buffer.mutex.release();
              break;
            }

	    /* Consume an item */
 	    int consumedItem = buffer.consumeItem();
            System.out.println("** Consumer #"+who+" consumes item "+consumedItem);
	    /* Wake up potential producers */
	    buffer.isNotFullCond.wake();
	    /* Release the mutex */
            buffer.mutex.release();
	    /* Yield so that somebody else has a chance to run */
	    KThread.yield();
          }
          System.out.println("** Consumer #"+who+" exits");

	  /* Signal that the thread is finished */
	  buffer.mutex.acquire();
	  buffer.numFinishedThreads++;
	  buffer.isOverCond.wake();
	  buffer.mutex.release();
	}

	/* The Prod/Cons buffer */
	private ProdConsBuffer buffer;
	/* An ID for printing out information */
	private int who;
    }

    /**
     * Tests whether this module is working.
     */
    public static void runTest() {

	System.out.println("**** Condition testing begins ****");

    	/* Create the buffer, with a specified max # of actions */
    	ProdConsBuffer buffer = new ProdConsBuffer(maxNumActions);

        /* Create producer threads and fork them*/
        KThread producers[] = new KThread[numProducers];
	for (int i=0; i < numProducers; i++) {
	  producers[i] = new KThread(new Producer(i,buffer)).setName("producer thread #"+i);
          producers[i].fork();
 	}	

        /* Create consumer threads and fork them*/
        KThread consumers[] = new KThread[numConsumers];
	for (int i=0; i < numConsumers; i++) {
	  consumers[i] = new KThread(new Consumer(i,buffer)).setName("consumer thread #"+i);
          consumers[i].fork();
 	}	

	/* Wait for the prod/cons execution to be over */
	buffer.mutex.acquire();
	while (buffer.numFinishedThreads != numConsumers + numProducers) {
	  buffer.isOverCond.sleep();
        }
        buffer.mutex.release();

	System.out.println("**** Condition testing ends ****");
	
    }

    private static final int maxNumActions = 100000;
    private static final int numProducers = 10;
    private static final int numConsumers = 100;

}
