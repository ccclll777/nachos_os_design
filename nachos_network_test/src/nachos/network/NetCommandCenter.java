package nachos.network;


import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.MalformedPacketException;
import nachos.machine.Packet;
import nachos.threads.KThread;
import nachos.threads.Lock;
import nachos.threads.Semaphore;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * PostOffice with some additional features.
 * 	- provides connect/accept protocol between two ports
 *	- provides assurance that the packet is resent upon failure during delivery
 *
 * 邮局有一些附加功能。
 * -提供两个端口之间的连接/接受协议
 * -提供在传输期间失败时重新发送数据包的保证
 */
public class NetCommandCenter extends PostOffice {
	public NetCommandCenter() {

		// NetCommunicator variables
		connections = new ConnectionMap();
		unackMessages = new HashSet<MailMessage>();//未确认消息
		waitingDataMessages = new Deque[MailMessage.portLimit];
		availPorts = new TreeSet<Integer>();
		for (int i = 0; i < MailMessage.portLimit; i++) {
			availPorts.add(i);
			waitingDataMessages[i] = new LinkedList<MailMessage>();
		}
		KThread ra = new KThread(new Runnable() {
			public void run() { resendAll(); }
		});
		
		
		// PostOffice variables (Reinitializing variables)
		messageReceived = new Semaphore(0);
		messageSent = new Semaphore(0);
		sendLock = new Lock();

		Runnable receiveHandler = new Runnable() {
			public void run() { receiveInterrupt(); }
		};
		Runnable sendHandler = new Runnable() {
			public void run() { sendInterrupt(); }
		};
		Machine.networkLink().setInterruptHandlers(receiveHandler,
				sendHandler);

		KThread pd = new KThread(new Runnable() {
			public void run() { postalDelivery(); }
		});
		
		// This automates postalDelivery(), which handles MailMessage receiving
		// procedures, and resendAll(), which handles resending unACKed MailMessages
		//这将自动化postalDelivery（），它处理邮件接收
		////procedures和resendall（），它处理重新发送未锁定的邮件
		ra.fork();
		pd.fork();
		Lib.debug(dbgNet, "Constructor finished");
	}

	/**
	 * Modified version of PostOffice's postalDelivery(). Instead of inserting 
	 * arrived message at the SynchList, it calls a helper method, handlePacket(),
	 * to insert it into the correct data structure.
	 */
	//邮局PostalDelivery（）的修改版本。而不是插入
	//到达SynchList的消息调用一个helper方法handlePacket（），将其插入到正确的数据结构中。
	protected void postalDelivery() {
		while(true) {
			// Wait until a Packet arrives at NetworkLink
			messageReceived.P();

			// A Packet is received. Checking whether the arrival was successful.
			//A Packet is received. Checking whether the arrival was successful.
			Packet p = Machine.networkLink().receive();
			try {
				MailMessage mail = new MailMessage(p);
				Lib.debug(dbgNet, "receiving mail: " + mail);

				// The arrival was successful. Calling the helper method to handle MailMessage
				//抵达成功调用helper方法来处理mailmessage
				handleMessage(mail);
			}
			catch (MalformedPacketException e) {
				Lib.assertNotReached("Packet is null at postalDelivery()");
				// continue;
			}
		}
	}

	/**
	 * Look for the MailMessage's corresponding Connection state and calls the relevant function.
	 * If no Connection found, then it's treated as CLOSED.
	 * //查找邮件的相应连接状态并调用相关函数。如果找不到连接，则视为已关闭
	 */
	private void handleMessage(MailMessage mail) {
		// Get the connection state
		int connectionState = connections.getConnectionState(mail.packet.srcLink, mail.srcPort, mail.packet.dstLink, mail.dstPort);

		// Handle specified port's all possible Connection states
		//处理指定端口的所有可能连接状态
		switch(connectionState) {
		case Connection.CLOSED:
			handleClosed(mail);
			break;
		case Connection.SYN_SENT:
			handleSYNSent(mail);
			break;
		case Connection.SYN_RCVD:
			handleSYNRcvd(mail);
			break;
		case Connection.ESTABLISHED:
			handleEstab(mail);
			break;
		case Connection.STP_SENT:
			handleSTPSent(mail);
			break;
		case Connection.STP_RCVD:
			handleSTPRcvd(mail);
			break;
		case Connection.CLOSING:
			handleClosing(mail);
			break;
		default:
			//handleMessage（）中不支持的连接状态
			Lib.assertNotReached("Unsupported connection state in handleMessage(): " + connectionState);
		}
	}

	/**
	 * Handles how CLOSED connection handles the message
	 * 处理关闭连接处理消息的方式
	 */
	private void handleClosed(MailMessage mail) {
		// Extract tag bits from mail
		//从邮件中提取标记位
		int tag = extractTag(mail);

		switch(tag) {
		case SYN:
			Lib.debug(dbgConn, "(Network" + Machine.networkLink().getLinkAddress() + ") SYN packet is received in CLOSED");
			// Insert a new Connection(SYN_RCVD or waiting state)
			Lib.debug(dbgConn, "Inserting Connection["+new Connection(mail, Connection.SYN_RCVD)+"] to SYN_RCVD connections");
			connections.add(new Connection(mail, Connection.SYN_RCVD));
			break;
		case FIN:
			Lib.assertNotReached("FIN is not supported yet in handleClosed()");	// FIN is not implemented yet
			break;
		default:
			Lib.assertNotReached("Unsupported invalid Packet tag bits in handleClosed()" + tag);	// protocol error
		}
	}

	/**
	 * Handles how SYN_SENT connection handles the message. Also checks for deadlock.
	 * 处理syn_sent connection处理消息的方式。同时检查死锁。
	 */
	private void handleSYNSent(MailMessage mail) {
		// Extract tag bits from mail
		int tag = extractTag(mail);

		switch(tag) {
		case SYN:
			// protocol error
			Lib.debug(dbgConn, "(Network" + Machine.networkLink().getLinkAddress() + ") SYN packet is received in SYN_SENT");
			Lib.assertNotReached("(Network" + Machine.networkLink().getLinkAddress() + "protocol deadlock");
			break;

		case SYNACK:
			Lib.debug(dbgConn, "(Network" + Machine.networkLink().getLinkAddress() + ") SYNACK packet is received in SYN_SENT");
			// Connection is confirmed. Change SYN_SENT state to ESTABLISHED (this will stop the "blocking" state for connect
			// function
			Lib.debug(dbgConn, "Inserting Connection["+new Connection(mail, Connection.SYN_RCVD)+"] to ESTABLISHED connections");
			connections.switchConnection(Connection.ESTABLISHED, new Connection(mail, Connection.SYN_SENT));
			break;
		
			// Do nothing or print error for the following
		case DATA:
			break;
			
		case STP:
			break;
			
		case FIN:
			break;
		default:
			Lib.assertNotReached("Unsupported invalid Packet tag bits in handleSYNSent()");	// protocol error
		}
	}

	/**
	 * Handles how SYN_RCVD connection handles the message
	 */
	private void handleSYNRcvd(MailMessage mail) {
		// Do nothing
	}

	/**
	 * Handles how ESTABLISHED connection handles the message
	 * 处理已建立的连接如何处理消息
	 */
	private void handleEstab(MailMessage mail) {
		// Extract tag bits from mail
		int tag = extractTag(mail);

		switch(tag) {
		case SYN:
			Lib.debug(dbgConn, "(Network" + Machine.networkLink().getLinkAddress() + ") SYN packet is received in ESTABLISHED");

			// Insert to waiting list until it's established (There is a chance of SYN/ACK Packet drop). 
			if(!isEstablished(new Connection(mail, Connection.SYN_RCVD))) {
				Lib.debug(dbgConn, "Inserting Connection["+new Connection(mail, Connection.SYN_RCVD)+"] to SYN_RCVD connections");
				connections.add(new Connection(mail, Connection.SYN_RCVD));
			}
			else {
				Lib.debug(dbgConn, "Connection["+new Connection(mail, Connection.SYN_RCVD)+"] already exists");
			}
			break;
		case DATA:
			// Add to the waiting data message list (this will be used when NetProcess is trying to read)
			//添加到等待数据消息列表（当NetProcess试图读取时将使用此消息）
			waitingDataMessages[mail.dstPort].add(mail);
			mail.contents[MBZ_TAGS] = ACK;
			
			// Send ACK Packet
			try {
				MailMessage ack = new MailMessage(
						mail.packet.srcLink,
						mail.srcPort,
						mail.packet.dstLink,
						mail.dstPort,
						mail.contents);
				send(ack);
			}
			catch(MalformedPacketException e) {
				// continue;
			}
			break;
			
		case ACK:
			Lib.debug(dbgConn, "(Network" + Machine.networkLink().getLinkAddress() + ") ACK packet is received in ESTABLISHED");
			
			// Remove the mail from the resend list (or "shifting" if window protocol was implemented)
			mail.contents[MBZ_TAGS] = DATA;
			try {
				unackMessages.remove(new MailMessage(mail.packet.srcLink, mail.srcPort, mail.packet.dstLink, mail.dstPort, mail.contents));
			}
			catch(MalformedPacketException e) {
				// continue;
			}
			break;
		default:
//			Lib.assertNotReached("Unsupported invalid Packet tag bits in handleEstab()");
		}
	}

	/**
	 * Handles how STP_SENT connection handles the message
	 */
	private void handleSTPSent(MailMessage mail) {
		// not supported yet
		Lib.assertNotReached("Not ready to support STP_SENT state");
	}

	/**
	 * Handles how STP_RCVD connection handles the message
	 */
	private void handleSTPRcvd(MailMessage mail) {
		// not supported yet
		Lib.assertNotReached("Not ready to support STP_RCVD state");
	}

	/**
	 * Handles how CLOSING connection handles the message
	 */
	private void handleClosing(MailMessage mail) {
		Lib.assertNotReached("Not ready to support CLOSING state");
	}


	/**
	 * Extracts tag components in MailMessage.
	 */
	private int extractTag(MailMessage mail) {
		return mail.contents[MBZ_TAGS];
	}

	/**
	 * Connects to a remote/local host. Returns the corresponding connection
	 * or null if error occurs
	 * 连接到远程/本地主机。返回相应的连接
	 * *如果发生错误，则为空
	 */
	public Connection connect(int dstLink, int dstPort) {
		// Find an available port and pop it out of the available port list
		//找到可用端口并将其从可用端口列表中弹出
		if(availPorts.isEmpty())
			return null;
		
		// source port
		int srcPort = availPorts.first();
		availPorts.remove(availPorts.first());
		
		try {
			// source address
			int srcLink = Machine.networkLink().getLinkAddress();
			
			// Tag specifications
			byte[] contents = new byte[2];
			contents[MBZ] = 0;
			contents[MBZ_TAGS] = SYN;
			
			// Prepare MailMessage
			MailMessage synMail = new MailMessage(
					dstLink, 
					dstPort, 
					srcLink, 
					srcPort,
					contents);
			
			// Send SYN MailMessage
			send(synMail);

			// Insert SYN MailMessage into resend list
			//在重新发送列表中插入syn mailmessage
			unackMessages.add(synMail);
			
			// Goto "SYN_SENT" state
			Connection connection = new Connection(
					srcLink, 
					dstLink, 
					srcPort,
					dstPort,
					Connection.SYN_SENT);
			
			// Insert the Connection to the map
			//成功建立链接
			connections.add(connection);
			
			Lib.debug(dbgConn, "Waiting for Connection["+connection+"]");
			// Wait until the connection is established (Triggered in SYN_SENT's SYNACK)
			while(!isEstablished(connection))
				NetKernel.alarm.waitUntil(RETRANSMIT_INTERVAL);
			
			// Connection is established. Removing the message from resend list
			//连接已建立从重新发送列表中删除邮件
			unackMessages.remove(synMail);
			
			// Return the established Connection
			//返回已建立的连接
			Lib.debug(dbgConn, "Connection["+connection+"] finished");
			connection.state = Connection.ESTABLISHED;
			return connection;
		}
		catch (MalformedPacketException e) {
			Lib.assertNotReached("Packet is null at connect()");
			// continue;
		}
		return null;
	}

	/**
	 * Accepts a waiting connection of the particular port. Returns the corresponding connection
	 * or null if error occurs
	 * 接受特定端口的等待连接。返回相应的连接，如果发生错误则返回空
	 */
	public Connection accept(int srcPort) {
		// Get the next waiting Connection (or SYN_RCVD Connection) if exists
		//如果存在，则获得下一个等待连接（或SythRCVD连接）
		Connection connectMe = connections.findWaitingConnection(Machine.networkLink().getLinkAddress(), srcPort);
		
		// Return null if not found
		//如果找不到，则返回null
		if(connectMe == null)
			return null;

		Lib.debug(dbgConn, "Accepting Connection["+connectMe+"]");
		
		// Source address
		int srcLink = Machine.networkLink().getLinkAddress();
		
		// Tag specifications
		byte[] contents = new byte[2];
		contents[MBZ] = 0;
		contents[MBZ_TAGS] = SYNACK;
		try {
			// Prepare MailMessage
			MailMessage synackMail = new MailMessage(
					connectMe.dstLink,  
					connectMe.dstPort, 
					srcLink,
					srcPort,
					contents);
			
			// Send SYN/ACK Packet
			send(synackMail);

			// Establish connection and insert into the map
			connectMe.state = Connection.ESTABLISHED;
			connections.add(connectMe);
			
			// Return the Connection
			Lib.debug(dbgConn, "Accepted Connection["+connectMe+"]");
			return connectMe;
		}
		catch (MalformedPacketException e) {
			Lib.assertNotReached("Packet is null at accept()");
			// continue;
		}
		
		// Accept failed
		return null;
	}
	
	/**
	 * Sends data packet(s) depending on the size. For now, it sends only 1 packet,
	 * so this only sends up to the contents.length
	 * 根据大小发送数据包现在，它只发送一个数据包，所以它只发送内容。
	 * @param c - current connection
	 * @param contents - bytes to send
	 * @param size - size of the contents
	 * @param bytesSent - bytes that have been sent so far
	 * @return updated bytesSent (not updated for the current version)
	 */
	public int sendData(Connection c, byte[] contents, int size, int bytesSent) {
		Lib.assertTrue(size >= 0);
		
		// Keep sending until all bytes are sent
		Lock lock = new Lock();
		System.out.println("sendData"+Lib.bytesToString(contents,0,contents.length));
		// Send the packet
		try {
			byte[] sendMe = createData(size, contents);
			MailMessage dataMessage = new MailMessage(
					c.dstLink,  
					c.dstPort, 
					c.srcLink,
					c.srcPort,
					sendMe);
			send(dataMessage);
			
			// Also insert into the resendList
			lock.acquire();
			unackMessages.add(dataMessage);
			lock.release();
		}
		catch(MalformedPacketException e) {
			Lib.assertNotReached("MailMessage is failed at sendData()");
		}
		
		return bytesSent;
	}
	
	/**
	 * Receives a data MailMessage's byte contents if exists. Returns null if not.
	 * 如果存在数据字节的字节内容。如果不是，则返回null。
	 */
	public byte[] receiveData(Connection c) {
		return (waitingDataMessages[c.srcPort].isEmpty()) ? null : waitingDataMessages[c.srcPort].removeFirst().contents;
	}

	/**
	 * Resends all packets that are yet to be ACKed. This function will be keep running
	 * in a thread
	 * 重新发送所有尚未确认的数据包。此函数将继续在线程中运行
	 */
	private void resendAll() {
		while(true) {
			Lock lock = new Lock();
			lock.acquire();
			//通过加锁编程单线程操作

//				for(MailMessage m : unackMessages)
//					send(m);
			Iterator iterator = unackMessages.iterator();
			while (iterator.hasNext()) {
				send((MailMessage) iterator.next());
			}


			lock.release();
			NetKernel.alarm.waitUntil(RETRANSMIT_INTERVAL);
		}
	}
	/**
	 * Check whether the given connection is established (in ESTABLISHED state) in the map
	 * 检查给定的连接是否已在映射中建立（处于已建立状态）
	 */
	private boolean isEstablished(Connection findMe) {
		return Connection.ESTABLISHED == connections.getConnectionState(findMe.dstLink, findMe.dstPort, findMe.srcLink, findMe.srcPort);
	}
	
	/**
	 * Create a content array that contains both sequence number and data
	 */
	//发送数据

	public byte[] createData(int seq, byte[] data) {
		Lib.assertTrue(seq >= 0 && data.length <= CONTENTS);
		
		// Create a content array
		byte[] contents = new byte[HEADERS+SEQNUM+data.length];
		
		// tag specifications
		contents[MBZ] = 0;
		contents[MBZ_TAGS] = DATA;
		System.out.println("createData"+Lib.bytesToString(data,0,data.length));
		// Insert sequence number
		System.arraycopy(ByteBuffer.allocate(SEQNUM).putInt(seq).array(), 0, contents, HEADERS, SEQNUM);
		
		// Insert data
		System.arraycopy(data, 0, contents, HEADERS+SEQNUM, data.length);
		
		return contents;
	}
	
	/**
	 * Extract sequence number from MailMessage (DATA MailMessage only)
	 * 从mailmessage中提取序列号（仅数据mailmessage）
	 */
	public int extractSeq(MailMessage mail) {
		byte[] seq = new byte[SEQNUM];
		System.arraycopy(mail.contents, HEADERS, seq, 0, SEQNUM);
		Lib.debug(dbgConn, "Mail["+mail+"] has sequence number of " + ByteBuffer.wrap(seq).order(ByteOrder.BIG_ENDIAN).getInt());
		return ByteBuffer.wrap(seq).order(ByteOrder.BIG_ENDIAN).getInt();
	}
	
	/**
	 * Extract sequence number from MailMessage content array (DATA only)
	 */
	public int extractSeq(byte[] contents) {
		byte[] seq = new byte[SEQNUM];
		System.arraycopy(contents, HEADERS, seq, 0, SEQNUM);
		return ByteBuffer.wrap(seq).order(ByteOrder.BIG_ENDIAN).getInt();
	}
	
	/**
	 * Extract byte contents from MailMessage content array (DATA only)
	 */
	public byte[] extractBuffer(byte[] contents) {
		if(contents == null)
			return null;
		byte[] buffer = new byte[contents.length-HEADERS-SEQNUM];
		System.arraycopy(contents, HEADERS+SEQNUM, buffer, 0, buffer.length);
		return buffer;
	}

	// MailMessage tag bits
	private static final int DATA = 0, SYN = 1, ACK = 2, SYNACK = 3, STP = 4, FIN = 8, FINACK = 10;

	// MailMessage contents index
	//邮件内容索引
	private static final int MBZ = 0, MBZ_TAGS = 1;
	
	// MailMessage contents headers [MBZ][MBZ_TAG][SEQNUM][CONTENTS]
	//电子邮件内容主机[，mbz[，mbz&u tag[，seqnum[，contents]
	private static final int HEADERS = 2, SEQNUM = 4, CONTENTS = MailMessage.maxContentsLength - HEADERS - SEQNUM;

	// Connection Map
	ConnectionMap connections;

	// Other constants
	private static final int RETRANSMIT_INTERVAL = 20000;

	// Data structures
	private static final char dbgConn = 'c';
	private HashSet<MailMessage> unackMessages;//未确认消息
	private TreeSet<Integer> availPorts;//可分配端口
	private Deque<MailMessage>[] waitingDataMessages;

	// Locks and conditions
}
