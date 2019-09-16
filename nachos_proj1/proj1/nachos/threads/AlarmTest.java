package nachos.threads;

import nachos.machine.Machine;

import java.util.Random;


/**
 * A Tester for the Alarm class.
 * We can be sure that this tests the implementation because we create many
 * threads and we can see that they are woken up after the correct amount of
 * time has passed. Please note that it is impossible to wake up the thread at
 * exactly the correct time (since the interrupt handler only runs about every
 * 500 ticks, so instead threads are just woken up sometime after their time has
 * passed.
 */
public class AlarmTest {
    /**
     * Wait thread class
     */
    private static class AThread implements Runnable {
        AThread(String name, Random rng) {
            this.name = name;
            this.rng = rng;
        }

        /** Method to generate a random number of ticks that
         * needs to be spent waiting before attempting a 
         * speak/listen actions. The number of ticks is
         * generated to be between minDelay and maxDelay, inclusive
         */
        private int randomDelay() {
            return minDelay+rng.nextInt(1+maxDelay - minDelay)-1; 
        }

        public void run() {
            long wakeTime = randomDelay();
            System.out.println("** "+name+": Sleeping for "+wakeTime+
                    " (i.e., until time="+
                    (wakeTime+Machine.timer().getTime())+")");
            ThreadedKernel.alarm.waitUntil(wakeTime);
            System.out.println(name + " is Done Sleeping (time="+ Machine.timer().getTime() + ")");
        }

        String name;
        Random rng;
    }


    /**
     * Tests whether this module is working.
     */
    public static void runTest() {
        System.out.println("**** Alarm testing begins ****");

        /* Create a random number generator */
        Random rng = new Random();

        /* Create the threads and fork them*/
        KThread threads[] = new KThread[numAThreads];
        for (int i=0; i < numAThreads; i++) {
            /* Creating a AThread */
            threads[i] = new KThread(new AThread("Thread "+ i, rng));
            threads[i].setName("A-Thread #"+i);
            threads[i].fork();
        }

        ThreadedKernel.alarm.waitUntil(6000);
        System.out.println("**** Alarm testing ends ****");
    }

    private static final int numAThreads = 20;

    /* Bounds on delay for sleep */
    private static final int minDelay = 2;
    private static final int maxDelay = 5000;
}

