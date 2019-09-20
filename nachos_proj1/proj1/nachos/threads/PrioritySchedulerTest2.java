package nachos.threads;

public class PrioritySchedulerTest2 {

    private static class p implements Runnable
    {
        @Override
        public void run() {
            for(int i = 0 ; i < 7 ; i++)
            {
                System.out.println("thread  +"+ KThread.currentThread().getName()+" is running Priority" +KThread.currentThread().getPriority());
                if(i == 4 &&KThread.currentThread().getName().equals("kt4"))
                {
                    KThread k = new KThread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("thread  +"+ KThread.currentThread().getName()+" is running Priority" +KThread.currentThread().getPriority());
                        }
                    });
                    k.setName("k");
                    k.fork();
                    k.join();
                }
            }

        }
    }

    public void Test()
    {
        Lock lock = new Lock();
        KThread kt1 = new KThread(new p()).setName("kt1");
        kt1.setPriority(2);
        System.out.println("kt1.priority"+kt1.getPriority());
        KThread kt2 = new KThread(new p()).setName("kt2");
        kt2.setPriority(4);
        System.out.println("kt1.priority"+kt2.getPriority());
        KThread kt3 = new KThread(new p()).setName("kt3");
        kt3.setPriority(5);
        System.out.println("kt1.priority"+kt3.getPriority());
        KThread kt4 = new KThread(new p()).setName("kt4");
        kt4.setPriority(7);
        System.out.println("kt1.priority"+kt4.getPriority());
        kt1.fork();
        kt2.fork();
        kt3.fork();
        kt4.fork();

    }
}
