package nachos.vm;

import nachos.machine.Machine;
import nachos.userprog.UserKernel;

import java.util.LinkedList;

/**
 * 二次机会算法
 */
public class SecondChanceReplacement {


    private int currentPhysicalPage;


    private int replacePhysicalPage;


    public SecondChanceReplacement() {
        currentPhysicalPage = 0;
        replacePhysicalPage = 0;


    }

    public int findSwappedPage() {

        if (!UserKernel.freePages.isEmpty()) {
            replacePhysicalPage = UserKernel.freePages.removeFirst();

            return replacePhysicalPage;
        } else {

            while (InvertedPageTable.PhysicalPageCopy[currentPhysicalPage].getTranslationEntry().used) {

                InvertedPageTable.PhysicalPageCopy[currentPhysicalPage].getTranslationEntry().used = false;
                InvertedPageTable.updateEntry( InvertedPageTable.PhysicalPageCopy[currentPhysicalPage].getPid(), InvertedPageTable.PhysicalPageCopy[currentPhysicalPage].getTranslationEntry());
                currentPhysicalPage = ++currentPhysicalPage % Machine.processor().getNumPhysPages();
            }

            replacePhysicalPage = currentPhysicalPage;
            currentPhysicalPage++;
            currentPhysicalPage %= Machine.processor().getNumPhysPages();

            return replacePhysicalPage;
        }
    }


}
