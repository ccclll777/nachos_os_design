package nachos.network;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.OpenFile;
import nachos.userprog.UserProcess;

import java.util.Deque;
import java.util.LinkedList;

/**
 * A <tt>VMProcess</tt> that supports networking syscalls.
 */
public class NetProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public NetProcess() {
		super();
		netManager = new NetCommandCenter();
		socketList = new Socket[MAX_SOCKETS];
	}
	
	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static NetProcess newNetProcess() {
		return (NetProcess) Lib.constructObject(Machine.getProcessClassName());
	}

	/**
	 * Attempt to initiate a new connection to the specified port on the specified
	 * remote host, and return a new file descriptor referring to the connection.
	 * connect() does not give up if the remote host does not respond immediately.
	 * 尝试启动到指定端口的新连接
	 * 远程主机，并返回一个引用连接的新文件描述符。
	 * 如果远程主机不立即响应，则connect（）不会放弃。
	 * @param host: remote machine address
	 * @param port: remote port number
	 * @return
	 */
	private int handleConnect(int host, int port) {
		// Return -1 if the list is full
		int fileDescriptor = getAvailIndex();
		if(fileDescriptor == -1)
			return -1;

		// Create a socket
		socketList[fileDescriptor] = new Socket();

		// Attempt to accept
		Connection connection =  netManager.connect(host, port);

		// Failed to accept. Return error
		if(connection == null) {
			// Revert
			socketList[fileDescriptor] = null;
			return -1;
		}

		// Successfully accepted. Return the new file descriptor
		socketList[fileDescriptor] = new Socket(connection);
		return fileDescriptor;
	}

	/**
	 * Attempt to accept a single connection on the specified local port and return
	 * a file descriptor referring to the connection.
	 *
	 * If any connection requests are pending on the port, one request is dequeued
	 * and an acknowledgement is sent to the remote host (so that its connect()
	 * call can return). Since the remote host will never cancel a connection
	 * request, there is no need for accept() to wait for the remote host to
	 * confirm the connection (i.e. a 2-way handshake is sufficient; TCP's 3-way
	 * handshake is unnecessary).
	 *
	 * If no connection requests are pending, returns -1 immediately.
	 *
	 * In either case, accept() returns without waiting for a remote host.
	 *
	 * Returns a new file descriptor referring to the connection, or -1 if an error
	 * occurred.
	 * 尝试接受指定本地端口上的单个连接，并返回引用该连接的文件描述符。
	 * 如果端口上有任何连接请求挂起，则一个请求将退出队列，并向远程主机发送确认（以便其connect（）调用可以返回）。由于远程主机永远不会取消连接请求，因此不需要accept（）等待远程主机确认连接（即，双向握手就足够了；不需要TCP的3向握手）。
	 * 如果没有挂起的连接请求，则立即返回-1。
	 * 在这两种情况下，accept（）返回而不等待远程主机。
	 * 返回引用连接的新文件描述符，如果出现错误，则返回-1
	 * 发生。
	 * @param port: local port on this machine
	 * @return a new file descriptor referring to the connection, or -1 if an error
	 * occurred.
	 */
	private int handleAccept(int port) {
		// Return -1 if the list is full
		int fileDescriptor = getAvailIndex();
		if(fileDescriptor == -1)
			return -1;
		
		// Create a socket
		socketList[fileDescriptor] = new Socket();
		
		// Attempt to accept
		Connection connection =  netManager.accept(port);
		
		// Failed to accept. Return error
		if(connection == null) {
			// Revert
			socketList[fileDescriptor] = null;
			return -1;
		}
		
		// Successfully accepted. Return the new file descriptor
		socketList[fileDescriptor] = new Socket(connection);
		return fileDescriptor;
	}
	
	/**
	 * Attempt to read up to size bytes into buffer from the file or stream
	 * referred to by fileDescriptor.
	 *
	 * On success, the number of bytes read is returned. If the file descriptor
	 * refers to a file on disk, the file position is advanced by this number.
	 *
	 * It is not necessarily an error if this number is smaller than the number of
	 * bytes requested. If the file descriptor refers to a file on disk, this
	 * indicates that the end of the file has been reached. If the file descriptor
	 * refers to a stream, this indicates that the fewer bytes are actually
	 * available right now than were requested, but more bytes may become available
	 * in the future. Note that read() never waits for a stream to have more data;
	 * it always returns as much as possible immediately.
	 *
	 * On error, -1 is returned, and the new file position is undefined. This can
	 * happen if fileDescriptor is invalid, if part of the buffer is read-only or
	 * invalid, or if a network stream has been terminated by the remote host and
	 * no more data is available.
	 * 
	 * @param fileDescriptor
	 * @param vaddr
	 * @param size
	 * @return On success, the number of bytes read is returned. On error, -1 is 
	 * returned, and the new file position is undefined.
	 * 
	 */
	private int handleRead(int fileDescriptor, int vaddr, int size) {
		// Check if the read wants UserProcess's handleRead instead (used for C code
		// statements like printf)
		if(fileDescriptor == 0 || fileDescriptor == 1) {
			return super.handleSyscall(syscallRead, fileDescriptor, vaddr, size, 0);
		}
		
		// Return -1 if the input is invalid
		if(size < 0 || (fileDescriptor >= MAX_SOCKETS || fileDescriptor < 0)
				|| socketList[fileDescriptor] == null) {
			return -1;
		}
		
		// Receive buffers from Socket
		int bytesRead = 0;
		byte[] readBuffer = netManager.extractBuffer(netManager.receiveData(socketList[fileDescriptor].connection));
		if(readBuffer != null) {
			// Print the read buffer and update number of bytes read
			bytesRead +=FileDescriptors[1].getFile().write(readBuffer, 0, readBuffer.length);
		}
		// Return error if nothing was written to the memory
		if(bytesRead == 0)
			return -1;
		
		// Update the number of bytes read
		socketList[fileDescriptor].bytesRead += bytesRead;
		return bytesRead;
	}
	
	/**
	 * Attempt to write up to count bytes from buffer to the file or stream
	 * referred to by fileDescriptor. write() can return before the bytes are
	 * actually flushed to the file or stream. A write to a stream can block,
	 * however, if kernel queues are temporarily full.
	 *
	 * On success, the number of bytes written is returned (zero indicates nothing
	 * was written), and the file position is advanced by this number. It IS an
	 * error if this number is smaller than the number of bytes requested. For
	 * disk files, this indicates that the disk is full. For streams, this
	 * indicates the stream was terminated by the remote host before all the data
	 * was transferred.
	 *
	 * On error, -1 is returned, and the new file position is undefined. This can
	 * happen if fileDescriptor is invalid, if part of the buffer is invalid, or
	 * if a network stream has already been terminated by the remote host.
	 * 
	 * @param fileDescriptor
	 * @param vaddr
	 * @param size
	 * @return On success, the number of bytes written is returned (zero indicates nothing
	 * was written), and the file position is advanced by this number. On error, -1 is 
	 * returned, and the new file position is undefined.
	 * 
	 */
	private int handleWrite(int fileDescriptor, int vaddr, int size) {
		// Check if the read wants UserProcess's handleWrite instead
		if(fileDescriptor == 0 || fileDescriptor == 1) {
			return super.handleSyscall(syscallWrite, fileDescriptor, vaddr, size, 0);
		}
		
		// Return -1 if the input is invalid
		if(size < 0 || (fileDescriptor >= MAX_SOCKETS || fileDescriptor < 0)
				|| socketList[fileDescriptor] == null) {
			return -1;
		}
		
		// Count number of buffers to write and get the write buffer
		byte[] writeBuffer = new byte[size];
		int bytesToSend = readVirtualMemory(vaddr, writeBuffer, 0, size);
		
		// Send the buffer and update the number of bytes sent
		socketList[fileDescriptor].bytesSent = netManager.sendData(socketList[fileDescriptor].connection, writeBuffer, 
				bytesToSend, socketList[fileDescriptor].bytesSent);
		return bytesToSend;
	}
	
	/** 
	 * Get the next available index for fileList
	 * 
	 * @return available "file descriptor" or -1 if not found 
	 */
	protected int getAvailIndex() {
		for(int i = 2; i < MAX_SOCKETS; i++)
			if(socketList[i] == null)
				return i;
		return -1;
	}

	/**
	 * syscall "enums"
	 */
	private static final int
	syscallConnect = 11,
	syscallAccept = 12;

	// Centralized command for networking protocol
	private NetCommandCenter netManager;
	
	// number of max sockets and socket list (just like in UserProcess with OpenFiles)
	private final int MAX_SOCKETS = 16;
	private Socket[] socketList;
	
	/**
	 * A class that has all Connection related variables
	 * 一个具有所有连接相关变量的类
	 */
	private class Socket extends OpenFile {
		public Connection connection;
		public int bytesSent;
		public int bytesRead;
		public Deque<byte[]> readBuffer;
		
		// Default constructor
		public Socket() {
			readBuffer = new LinkedList<byte[]>();
			connection = null;
			bytesSent = 0;
			bytesRead = 0;
		}
		
		// Constructor used in connect/accept
		public Socket(Connection connection) {
			readBuffer = new LinkedList<byte[]>();
			this.connection = connection;
			bytesSent = 0;
			bytesRead = 0;
		}
	}
	
	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 *
	 * <table>
	 * <tr><td>syscall#</td><td>syscall prototype</td></tr>
	 * <tr><td>11</td><td><tt>int  connect(int host, int port);</tt></td></tr>
	 * <tr><td>12</td><td><tt>int  accept(int port);</tt></td></tr>
	 * </table>
	 * 
	 * @param	syscall	the syscall number.
	 * @param	a0	the first syscall argument.
	 * @param	a1	the second syscall argument.
	 * @param	a2	the third syscall argument.
	 * @param	a3	the fourth syscall argument.
	 * @return	the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallConnect:
			return handleConnect(a0, a1);
		case syscallAccept:
			return handleAccept(a0);
		case syscallRead:
			return handleRead(a0, a1, a2);
		case syscallWrite:
			return handleWrite(a0, a1, a2);
		case syscallClose:
//			Lib.assertNotReached("Unsupported Syscall");	// close is not implemented yet
			 return handleClose(a0);
		default:
			return super.handleSyscall(syscall, a0, a1, a2, a3);
		}
	}
}
