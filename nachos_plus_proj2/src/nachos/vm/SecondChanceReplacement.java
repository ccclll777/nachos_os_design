package nachos.vm;

import nachos.machine.Machine;
import nachos.userprog.UserKernel;

import java.util.LinkedList;

/**
 * 二次机会算法
 */
public  class SecondChanceReplacement {


    private int currentPhysicalPage;

    private int numFaults;

    private int replacePhysicalPage;


    public SecondChanceReplacement()
    {
        super();
        currentPhysicalPage = 0;
        numFaults = 0;
        //used_frames = 0;
        replacePhysicalPage = 0;


    }

    public int findSwappedPage()
    {
        numFaults++;

        if (!UserKernel.freePages.isEmpty())
        {
            replacePhysicalPage = UserKernel.freePages.removeFirst();

            //used bit set to true will happen in Processor.translate()
            return replacePhysicalPage;
        }
        else
        {
            /* evict page pointed to by current_frame and if only if its u-bit is false, replace with new page, and increment. */
            while (InvertedPageTable.getInstance().PhysicalPageCopy[currentPhysicalPage].getTranslationEntry().used)//search for used-bit contain 0
            {
                //used bit set to true will happen in Processor.translate()
                InvertedPageTable.getInstance().PhysicalPageCopy[currentPhysicalPage].getTranslationEntry().used = false;

                currentPhysicalPage = ++currentPhysicalPage % Machine.processor().getNumPhysPages();//advance next frame
            }

            replacePhysicalPage = currentPhysicalPage;
            currentPhysicalPage++;
            currentPhysicalPage %= Machine.processor().getNumPhysPages();

            return replacePhysicalPage;
        }
    }

    /*returns the number of faults. */
    public int getNumberPageFault()
    {
        return numFaults;
    }



}
