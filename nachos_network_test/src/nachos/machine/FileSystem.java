// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

/**
 * A file system that allows the user to create, open, and delete files.
 */
//允许用户创建、打开和删除文件的文件系统。
public interface FileSystem {
    /**
     * Atomically open a file, optionally creating it if it does not 
     * already exist. If the file does not
     * already exist and <tt>create</tt> is <tt>false</tt>, returns
     * <tt>null</tt>. If the file does not already exist and <tt>create</tt>
     * is <tt>true</tt>, creates the file with zero length. If the file already
     * exists, opens the file without changing it in any way.
     *
     * @param	name		the name of the file to open.  打开的文件名
     * @param	create		<tt>true</tt> to create the file if it does not
     *				already exist.
     * @return	an <tt>OpenFile</tt> representing a new instance of the opened
     *		file, or <tt>null</tt> if the file could not be opened.
     */
    //原子地打开一个文件，如果它还不存在，可以选择创建它。如果文件不存在且create<tt>is<tt>false<tt>，则返回<tt>null<tt>。
    // 如果文件不存在且<tt>create</tt>为<tt>true</tt>，则创建长度为零的文件。如果该文件已经存在，则在不以任何方式更改它的情况下打开该文件。
    public OpenFile open(String name, boolean create);

    /**
     * Atomically remove an existing file. After a file is removed, it cannot
     * be opened until it is created again with <tt>open</tt>. If the file is
     * already open, it is up to the implementation to decide whether the file
     * can still be accessed or if it is deleted immediately.
     *
     * @param	name	the name of the file to remove.
     * @return	<tt>true</tt> if the file was successfully removed.
     */
    //原子删除现有文件。删除文件后，在用<tt>open</tt>重新创建之前，无法打开该文件。如果文件已经打开，则取决于实现来决定文件是否仍然可以访问或是否立即删除。
    public boolean remove(String name);
}
