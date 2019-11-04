package nachos.network;

/**
 * Contains all connection related info, including addresses, ports, and its state.
 * NetProcess and NetCommandCenter can pass in this class to have the reference to
 * the connection, making sending packets (or MailMessage in this case) easier
 * 包含所有与连接相关的信息，包括地址、端口及其状态。
 * netprocess和netcommandcenter可以传入这个类以获得对连接的引用，从而使发送数据包（在本例中是mailmessage）更容易
 */
public class Connection {
	public int srcLink, dstLink;
	public int srcPort, dstPort;
	public int state;
	
	/**
	 * Connection states that are declared as "global" used by any class that
	 * deals with Connection states (NetCommandCenter, ConnectionMap)
	 * 声明为“全局”的连接状态，由
	 * 处理连接状态（netcommandcenter、connectionmap）
	 */
	public static final int CLOSED = -1, SYN_SENT = 0, SYN_RCVD = 1,
			ESTABLISHED = 2, STP_SENT = 3, STP_RCVD = 4, CLOSING = 5;

	/**
	 * Takes in literal address and port of the both end of connection. A de facto
	 * function for creating a Connection class
	 */
	public Connection(int srcLink, int dstLink, int srcPort, int dstPort, int state) {
		this.srcLink = srcLink;
		this.dstLink = dstLink;
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.state = state;
	}

	/**
	 * Takes in mail as the main argument. mail is assumed to be the message that
	 * was received, not sent, so Links and Ports are reversed. This function 
	 * makes creating Connection easier by doing so.
	 */
	public Connection(MailMessage mail, int state) {
		srcLink = mail.packet.dstLink; 
		dstLink = mail.packet.srcLink;
		srcPort = mail.dstPort;
		dstPort = mail.srcPort;
		this.state = state;
	}
	
	/**
	 * Determine if the current state is valid.
	 * 确定当前状态是否有效。
	 * @return true if valid, false otherwise
	 */
	public boolean validState() {
		return state == CLOSED || state == SYN_SENT || state == SYN_RCVD ||
				state == ESTABLISHED || state == STP_SENT || state == STP_RCVD ||
				state == CLOSING;
	}
	
	/**
	 * Determine if the given state is valid.
	 * 
	 * @return true if valid, false otherwise
	 */
	public static boolean validState(int state) {
		return state == CLOSED || state == SYN_SENT || state == SYN_RCVD ||
				state == ESTABLISHED || state == STP_SENT || state == STP_RCVD ||
				state == CLOSING;
	}

	@Override
	/**
	 * Search helper function
	 */
	public boolean equals(Object o) {
		Connection c = (Connection) o;
		return (srcLink == c.srcLink && dstLink == c.dstLink && srcPort == c.srcPort
				&& dstPort == c.dstPort&&state == c.state);
	}

	@Override
	/**
	 * Debug print helper
	 */
	public String toString() {
		return "("+srcLink+","+srcPort+") -> ("+dstLink+","+dstPort+")";
	}

}
