package nachos.vm;

public class CoffSectionAddress {
    private int sectionNumber;
    private int pageOffset;

    public int getSectionNumber() {
        return sectionNumber;
    }

    public CoffSectionAddress(int sectionNumber, int pageOffset) {
        this.sectionNumber = sectionNumber;
        this.pageOffset = pageOffset;
    }

    public void setSectionNumber(int sectionNumber) {
        this.sectionNumber = sectionNumber;
    }

    public int getPageOffset() {
        return pageOffset;
    }

    public void setPageOffset(int pageOffset) {
        this.pageOffset = pageOffset;
    }

}
