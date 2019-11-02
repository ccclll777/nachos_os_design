// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

/**
 * A single translation between a virtual page and a physical page.
 */
//虚拟页和物理页之间的单一转换。
public final class TranslationEntry {
    /**
     * Allocate a new invalid translation entry.
     */
    public TranslationEntry() {
	valid = false;
    }

    /**
     * Allocate a new translation entry with the specified initial state.
     *
     * @param	vpn		the virtual page numben.
     * @param	ppn		the physical page number.
     * @param	valid		the valid bit.
     * @param	readOnly	the read-only bit.
     * @param	used		the used bit.
     * @param	dirty		the dirty bit.
     */
    //虚拟页数   物理页数  标记位   只读位    是否被使用》  脏位
    public TranslationEntry(int vpn, int ppn, boolean valid, boolean readOnly,
			    boolean used, boolean dirty) {
	this.vpn = vpn;
	this.ppn = ppn;
	this.valid = valid;
	this.readOnly = readOnly;
	this.used = used;
	this.dirty = dirty;
    }       

    /**
     * Allocate a new translation entry, copying the contents of an existing
     * one.
     *
     * @param	entry	the translation entry to copy.
     */
    //分配新的翻译条目，复制现有翻译条目的内容。
    public TranslationEntry(TranslationEntry entry) {
	vpn = entry.vpn;
	ppn = entry.ppn;
	valid = entry.valid;
	readOnly = entry.readOnly;
	used = entry.used;
	dirty = entry.dirty;
    }

    /** The virtual page number. */
    //虚拟页号
    public int vpn;
    
    /** The physical page number. */
    //物理页号
    public int ppn;

    /**
     * If this flag is <tt>false</tt>, this translation entry is ignored.
     */
    //如果为false 此翻译条目将被忽略。
    public boolean valid;
    
    /**
     * If this flag is <tt>true</tt>, the user pprogram is not allowed to
     * modify the contents of this virtual page.
     */
    //如果此标志为<tt>true</tt>，则不允许用户程序修改此虚拟页的内容。
    public boolean readOnly;
    
    /**
     * This flag is set to <tt>true</tt> every time the page is read or written
     * by a user program.
     */
    //每次用户程序读取或写入页面时，此标志都设置为<tt>true</tt>。
    public boolean used;
    
    /**
     * This flag is set to <tt>true</tt> every time the page is written by a
     * user program.
     */
    //每次用户程序修改页面时，此标志都设置为<tt>true</tt>。
    public boolean dirty;
}
