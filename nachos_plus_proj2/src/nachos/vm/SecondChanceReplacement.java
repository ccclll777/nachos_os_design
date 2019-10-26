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

            return replacePhysicalPage;
        }
        else
        {

            while (InvertedPageTable.getInstance().PhysicalPageCopy[currentPhysicalPage].getTranslationEntry().used)
            {

                InvertedPageTable.getInstance().PhysicalPageCopy[currentPhysicalPage].getTranslationEntry().used = false;

                currentPhysicalPage = ++currentPhysicalPage % Machine.processor().getNumPhysPages();
            }

            replacePhysicalPage = currentPhysicalPage;
            currentPhysicalPage++;
            currentPhysicalPage %= Machine.processor().getNumPhysPages();

                return replacePhysicalPage;
        }
    }





}
