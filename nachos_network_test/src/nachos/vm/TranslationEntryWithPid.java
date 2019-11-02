package nachos.vm;

import nachos.machine.TranslationEntry;


//pid与 TranslationEntry的对应
public class TranslationEntryWithPid {

    private TranslationEntry translationEntry;
    private int pid;

    public TranslationEntryWithPid(TranslationEntry translationEntry, int pid) {
        this.translationEntry = translationEntry;
        this.pid = pid;
    }

    public TranslationEntry getTranslationEntry() {
        return translationEntry;
    }

    public void setTranslationEntry(TranslationEntry translationEntry) {
        this.translationEntry = translationEntry;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }
}
