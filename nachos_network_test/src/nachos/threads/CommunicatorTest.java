package nachos.threads;

import nachos.machine.Machine;

import java.util.Random;

/**
 * A Tester for the Communicator class
 */
public class CommunicatorTest {

   public  static int max  = 245;
   private  int message;
   private  Communicator communicatorc;
   private  int numOfSpeaker;
   private   int numOfListener;
   public CommunicatorTest()
   {
       this.message = 1;
       this.numOfListener = 5;
       this.numOfSpeaker = 5;
       communicatorc = new Communicator();
   }
   public  void commTest(int num)
   {
       System.out.println("开始");
   for(int  i = 0 ; i<num ; i++)
   {
       createSpeaker(numOfSpeaker);
       createListener(numOfListener);
       print("\n演讲者" +numOfSpeaker);
       print("收听者"+numOfListener+"\n");
       sleep(numOfListener+numOfSpeaker);
       print("\n回话创建完毕");


   }
   }

   public  void sleep(int num)
   {
       ThreadedKernel.alarm.waitUntil(num*100);
   }
   public   class Listener implements Runnable{
       @Override
       public void run() {
           int a = communicatorc.listen();
           print("  " +KThread.currentThread().getName() +"收到消息" +a);
       }
   }
    public   class Speaker implements Runnable{
        @Override
        public void run() {
           communicatorc.speak(i);
            System.out.println("xxxx"+i);
            print("  " +KThread.currentThread().getName() +"发出消息" +i);
        }
        Speaker(int i)
        {
            this.i = i;
        }
        int i;
    }
    public  void  createSpeaker (int numOfSpeaker)
    {
        int j;
        for (j = 1 ; j<=numOfSpeaker ; j++)
        {
            KThread speakerKthread  = new KThread(new Speaker(j));
            speakerKthread.setName("speaker"+j);
            speakerKthread.fork();
        }
    }
    public void  createListener (int numOfListener)
    {
        int j;
        for (j = 1 ; j<=numOfListener ; j++)
        {
            KThread listener  = new KThread(new Listener());
            listener.setName("listener"+j);
            listener.fork();
        }
    }
   public static void print(String message)
   {
       System.out.println(message);
   }

//
//    private  class  speak implements  Runnable{
//        @Override
//        public void run() {
//            communicator.speak(word);
//        }
//        speak(Communicator communicator,int word)
//        {
//            this.communicator = communicator;
//            this.word = word;
//
//        }
//        Communicator communicator;
//        int word;
//    }
//    private  class  listen implements  Runnable{
//        @Override
//        public void run() {
//           int word ;
//           word = communicator.listen();
//           System.out.println(KThread.currentThread().getName() +"listen"+word);
//        }
//        listen(Communicator communicator)
//        {
//            this.communicator = communicator;
//
//        }
//        Communicator communicator;
//    }
//    public  void test()
//    {
//        System.out.println("test333");
//        Communicator communicator = new Communicator();
//        KThread kThread1 = new KThread(new speak(communicator,1111)).setName("speak1");
//        KThread kThread2 = new KThread(new speak(communicator,2222)).setName("speak2");
//        KThread kThread3 = new KThread(new listen(communicator)).setName("listen1");
//        KThread kThread4 = new KThread(new listen(communicator)).setName("listen2");
//
//        kThread1.fork();
//        kThread2.fork();
//        kThread3.fork();
//        kThread4.fork();
//
//
//
//    }
}
