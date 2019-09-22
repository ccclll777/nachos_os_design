package nachos.threads;
import nachos.ag.BoatGrader;


/**
 *
 *
 * 现在您已经拥有了所有这些同步设备，请使用它们来解决此问题。你会发现条件变量是解决这个问题最有用的同步方法。
 * 一些夏威夷成年人和儿童正试图从瓦胡岛到莫洛凯岛。不幸的是，他们只有一艘船，
 * 最多只能载两个孩子或一个成年人（但不能载一个孩子和一个成年人）。这条船可以划回瓦胡岛，但它需要一名驾驶员这样做。
 *
 * 安排一个解决方案，把所有人从瓦胡转移到莫洛凯。你可以假设至少有两个孩子。
 *
 * boat.begin（）方法应该为每个儿童或成人fork一个线程。我们将引用名为boat.begin（）的线程作为父线程。
 * 你的机制不能依赖于预先知道有多少孩子或大人在场，尽管你可以在线程之间自由地尝试确定这个
 * （即，你不能将方法中的参数“大人”和“孩子”传递给线程，但是你可以自由地让每个线程都在crement共享变量以尝试确定此值（如果您愿意）。
 *
 *
 * 为了证明旅行是正确同步的，每次有人通过通道时都要调用相应的BoatGrader方法。当一个孩子驾驶这艘船从瓦胡岛到莫洛凯岛时，就叫childrowtomolokai。
 * 当一个孩子作为乘客从瓦胡到莫洛凯时，叫做childridetomolakai。确保当一艘船上有两个人时，驾驶员call…RowTo…方法在乘客调用…rideto…方法。
 *
 * 你的解决方案必须不busy waiting，而且最终必须结束。当父线程完成运行时，模拟结束。注意，
 *  不需要终止所有fork线程——您可以让它们在等待条件变量时被阻塞。虽然您不能将创建的线程数传递给代表成人和儿童的线程，
 * 但您可以而且可能需要在begin（）中使用此数字，以便确定所有成人和儿童的时间，并且您可以返回。
 *
 * 此任务背后的思想是使用独立线程来解决问题。你要编程的逻辑，如果一个孩子或一个成年人将遵循这种情况下的人。
 * 例如，允许一个人看到有多少儿童或成年人在同一个岛上是合理的。一个人可以看到船是否在他们的岛上。一个人可以知道他们在哪个岛上。
 * 所有这些信息可以与每个单独的线程一起存储，也可以存储在共享变量中。因此，只要能够访问代表oahu上的人的线程，
 * 就允许一个计数器保存oahu上的子线程数。
 *
 *
 * 不允许的是执行“自顶向下”模拟策略的线程。例如，您不能为儿童和成人创建线程，然后让控制器线程通过通信程序向他们发送命令。
 * 线程必须像个人一样工作。这也意味着您不能以任何理由通过显式禁用boat.java中的中断来序列化代码。每个人必须始终独立地思考和行动。
 *
 * 在现实世界中不可能得到的信息也是不允许的。例如，莫洛凯岛上的一个孩子无法神奇地看到瓦胡岛上所有的人。
 * 那孩子可能记得他或她看到离开的人数，但他或她可能看不到瓦胡岛上的人就好像在那里一样。（假设人们除了船之外没有任何技术！）
 *
 * 你将在你的模拟中到达一个点，在这个点上，成人和儿童线程相信每个人都在摩洛凯对面。
 * 此时，您可以执行从成人/子线程到begin（）（父线程）的单向通信，以通知它模拟可能已经结束。
 * 但是，您的成人和儿童线程可能不正确。您的模拟必须处理此情况，而不需要从begin（）（父线程）到成人/子线程的显式或隐式通信。
 */
public class Boat
{
    static BoatGrader bg;
    //需要的变量
    static final int Oahu = 0;//表示O岛
    static final int Molokai = 1;//表示M岛
    static int boatPosition = 0;//表示船的位置 0为O岛 1为M岛
    static int numOfChildrenOnMolokai = 0;//M岛的孩子数
    static int numOfAdultsOnOahu = 0;//O岛的大人数
    static int numOfChildrenOnOahu = 0;//O岛的孩子数
    static int peopleOnBoat = 0;//船上是否有人  0为空  1为一个小孩 2为两个小孩或者一个大人
    static Lock conditionLock = new Lock();//锁  四个条件变量使用同一个锁
    static Condition OahuChildCondition = new Condition(conditionLock);//O岛孩子的条件变量
    static Condition OahuAdultCondition = new Condition(conditionLock);//O岛大人的条件变量
    static Condition MolokaiChildCondition = new Condition(conditionLock);//M岛孩子的条件变量
    static Condition boatCondition = new Condition(conditionLock);//有关船的条件变量
    static Semaphore s1 = new Semaphore(0);
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
//	System.out.println("\n ***Testing Boats with only 2 children***");
//	begin(0, 2, b);

	System.out.println("\n ***Testing Boats with 4 children, 4 adult***");
  	begin(4, 4, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {

	// Store the externally generated autograder in a class
	// variable to be accessible by children.
        //将外部生成的自动加载器存储在一个类变量中，以供子级访问。
	bg = b;

	// Instantiate global variables here
        numOfAdultsOnOahu = adults;
        numOfChildrenOnOahu = children;
        boatPosition = 0;
        peopleOnBoat = 0;
        numOfChildrenOnMolokai = 0;
        for (int i = 0 ; i<adults ; i++)
        {
            Runnable r = new Runnable() {
                public void run() {
                    AdultItinerary();
                }
            };
            KThread t = new KThread(r);
            t.setName("Adult"+i+"thread");
            t.fork();
        }


        for (int i = 0 ; i<children ; i++)
        {
            Runnable r = new Runnable() {
                public void run() {
                    ChildItinerary();
                }
            };
            KThread t = new KThread(r);
            t.setName("Child"+i+"thread");
            t.fork();
        }


        s1.P();
        System.out.println("运送完毕");
    }

    static void AdultItinerary()
    {
	bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 

	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/

	//只需在O岛sleep 等待唤醒  如果能上船则上船 不能上船则sleep
        //到达M岛后 唤醒一个孩子  让孩子划船岛O岛
	conditionLock.acquire();

        OahuAdultCondition.sleep();
        while ((boatPosition != Oahu) || (peopleOnBoat > 0))
        {
            OahuAdultCondition.sleep();
        }
        //否则 可以划船去M岛
        peopleOnBoat = 2;
        bg.AdultRowToMolokai();
        numOfAdultsOnOahu -= 1;
        boatPosition = Molokai;
        peopleOnBoat = 0;
        //唤醒一个孩子 让他能回到O岛
        MolokaiChildCondition.wake();
        if(numOfChildrenOnOahu == 0&&numOfAdultsOnOahu == 0)
        {
            s1.V();
            MolokaiChildCondition.sleep();
        }
        conditionLock.release();
    }

    static void ChildItinerary()
    {
	bg.initializeChild();

        int position = Oahu;
        while (true)
        {
            conditionLock.acquire();
            if (position == Oahu)
            {
                while ((boatPosition != Oahu) || (peopleOnBoat > 1))
                {
                    OahuChildCondition.sleep();
                }
                if(peopleOnBoat == 0)//第一个上船的小孩
                {
                    peopleOnBoat += 1;
                    OahuChildCondition.wakeAll();//唤醒O岛的小孩
                    numOfChildrenOnOahu -= 1;
                    OahuChildCondition.wakeAll();
                    boatCondition.sleep();//如果在O岛 之上一个孩子 那船先不能开
                    position = Molokai;//一个孩子到了M岛
                    numOfChildrenOnMolokai += 1;
                    MolokaiChildCondition.sleep();
                }
                else//第二个小孩上船
                {
                    boatCondition.wake();
                    numOfChildrenOnOahu -= 1;
                    bg.ChildRowToMolokai();//一个小孩驾船
                    bg.ChildRideToMolokai();//一个小孩坐船
                    OahuChildCondition.wakeAll();
                    boatPosition = Molokai;
                    peopleOnBoat = 0;
                    position = Molokai;//两个孩子都到了M岛
                    numOfChildrenOnMolokai++;
                    if(numOfChildrenOnOahu == 0&&numOfAdultsOnOahu == 0)
                    {
                        s1.V();
                        MolokaiChildCondition.sleep();
                    }
                    else
                    {
                        numOfChildrenOnMolokai--;
                        bg.ChildRowToOahu();
                        position = Oahu;
                        boatPosition = Oahu;
                        numOfChildrenOnOahu += 1;
                        OahuAdultCondition.wake();//叫醒一个O岛的大人
                        OahuChildCondition.sleep();
                    }
                }
            }
            else//如果船在M岛  小孩怎么做
            {

                //小孩要驾驶船去M岛
                numOfChildrenOnOahu += 1;
                numOfChildrenOnMolokai -= 1;
                position = Oahu;
                bg.ChildRowToOahu();
                boatPosition = Oahu;
                //然后将O岛的所有小孩唤醒
                OahuChildCondition.wakeAll();
            }
            conditionLock.release();
        }

    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
}
