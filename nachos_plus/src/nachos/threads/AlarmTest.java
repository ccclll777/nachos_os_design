package nachos.threads;

import nachos.machine.Machine;

import java.util.Random;

public class AlarmTest {

    private static class AThread implements Runnable {
        AThread(String name, Random rng) {
            this.name = name;
            this.rng = rng;
        }

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
    private static final int minDelay = 2;
    private static final int maxDelay = 5000;
}

