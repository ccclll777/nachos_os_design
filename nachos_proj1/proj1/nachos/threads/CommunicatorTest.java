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
           communicatorc.speak(message++);
            System.out.println("xxxx"+message);
            print("  " +KThread.currentThread().getName() +"发出消息" +(message-1));
        }
    }
    public  void  createSpeaker (int numOfSpeaker)
    {
        int j;
        for (j = 1 ; j<=numOfSpeaker ; j++)
        {
            KThread speakerKthread  = new KThread(new Speaker());
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

}
