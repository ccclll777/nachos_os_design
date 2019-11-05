// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

/**
 * A file that supports reading, writing, and seeking.
 */
//读、写和查找的文件。
public class OpenFile {
    /**
     * Allocate a new <tt>OpenFile</tt> object with the specified name on the
     * specified file system.
     *
     * @param	fileSystem	the file system to which this file belongs.  这个文件所属的文件系统
     * @param	name		the name of the file, on that file system.  文件名称
     */
    //在指定的文件系统上分配具有指定名称的新<tt>openfile</tt>对象。
    public OpenFile(FileSystem fileSystem, String name) {
	this.fileSystem = fileSystem;
	this.name = name;
    }

    /**
     * Allocate a new unnamed <tt>OpenFile</tt> that is not associated with any
     * file system.
     */
    //分配一个新的未命名的未与任何文件系统关联的openfile</tt>。
    public OpenFile() {
	this(null, "unnamed");
    }

    /**
     * Get the file system to which this file belongs.
     *
     * @return	the file system to which this file belongs.
     */
    //获取此文件所属的文件系统。
    public FileSystem getFileSystem() {
	return fileSystem;
    }
    
    /**
     * Get the name of this open file.
     *
     * @return	the name of this open file.
     */
    //获取打开的文件名
    public String getName() {
	return name;
    }
    
    /**
     * Read this file starting at the specified position and return the number
     * of bytes successfully read. If no bytes were read because of a fatal
     * error, returns -1
     *
     * @param	pos	the offset in the file at which to start reading. 文件中开始读取的偏移量。 从哪里开始
     * @param	buf	the buffer to store the bytes in.  BUF缓冲区中存储字节。
     * @param	offset	the offset in the buffer to start storing bytes.  缓冲区中开始存储字节的偏移量。
     * @param	length	the number of bytes to read.  读取多少字节
     * @return	the actual number of bytes successfully read, or -1 on failure.
     */
    //从指定位置开始读取此文件，并返回成功读取的字节数。如果由于致命错误没有读取字节，则返回-1
    public int read(int pos, byte[] buf, int offset, int length) {
	return -1;
    }
    
    /**
     * Write this file starting at the specified position and return the number
     * of bytes successfully written. If no bytes were written because of a
     * fatal error, returns -1.
     *
     * @param	pos	the offset in the file at which to start writing.
     * @param	buf	the buffer to get the bytes from.
     * @param	offset	the offset in the buffer to start getting.
     * @param	length	the number of bytes to write.
     * @return	the actual number of bytes successfully written, or -1 on
     *		failure.
     */
    //从指定位置开始写入此文件，并返回成功写入的字节数。如果由于致命错误没有写入字节，则返回-1。
    public int write(int pos, byte[] buf, int offset, int length) {
	return -1;
    }

    /**
     * Get the length of this file.
     *
     * @return	the length of this file, or -1 if this file has no length.
     */
    //文件长度
    public int length() {
	return -1;
    }

    /**
     * Close this file and release any associated system resources.
     */
    //关闭此文件并释放所有关联的系统资源。
    public void close() {
    }

    /**
     * Set the value of the current file pointer.
     */
    //设置当前文件指针的值。
    public void seek(int pos) {
    }

    /**
     * Get the value of the current file pointer, or -1 if this file has no
     * pointer.
     */
    //获取当前文件指针的值，如果此文件没有指针，则为-1。
    public int tell() {
	return -1;
    }

    /**
     * Read this file starting at the current file pointer and return the
     * number of bytes successfully read. Advances the file pointer by this
     * amount. If no bytes could be* read because of a fatal error, returns -1.
     *
     * @param	buf	the buffer to store the bytes in.
     * @param	offset	the offset in the buffer to start storing bytes.
     * @param	length	the number of bytes to read.
     * @return	the actual number of bytes successfully read, or -1 on failure.
     */
    //从当前文件指针开始读取此文件，并返回成功读取的字节数。将文件指针向前移动此量。
    // 如果由于致命错误而无法*读取字节，则返回-1。
    public int read(byte[] buf, int offset, int length) {
	return -1;
    }

    /**
     * Write this file starting at the current file pointer and return the
     * number of bytes successfully written. Advances the file pointer by this
     * amount. If no bytes could be written because of a fatal error, returns
     * -1.
     *
     * @param	buf	the buffer to get the bytes from.
     * @param	offset	the offset in the buffer to start getting.
     * @param	length	the number of bytes to write.
     * @return	the actual number of bytes successfully written, or -1 on
     *		failure.
     */
    //从当前文件指针开始写入此文件，并返回成功写入的字节数。将文件指针向前移动此量。如果由于致命错误而无法写入字节，则返回-1。
    public int write(byte[] buf, int offset, int length) {
	return -1;
    }

    private FileSystem fileSystem;
    private String name;
}
