package nachos.network;///NEW/////

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.MalformedPacketException;
import nachos.userprog.UserKernel;

/**
 * A kernel with network support.
 */
public class NetKernel extends UserKernel {
	/**
	 * Allocate a new networking kernel.
	 */
	public NetKernel() {
		super();
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);

		postOffice = new PostOffice();
	}

	/**
	 * Test the network. Create a server thread that listens for pings on port
	 * 1 and sends replies. Then ping one or two hosts. Note that this test
	 * assumes that the network is reliable (i.e. that the network's
	 * reliability is 1.0).
	 */
	public void selfTest() {
		super.selfTest();
//		Lib.enableDebugFlags("n");
//		KThread serverThread = new KThread(new Runnable() {
//			public void run() { pingServer(); }
//		});
//
//		serverThread.fork();
//
//		System.out.println("Press any key to start the network test...");
//		console.readByte(true);
//
//		int local = Machine.networkLink().getLinkAddress();
//
//		// ping this machine first
//		ping(local);
//
//		// if we're 0 or 1, ping the opposite
//		if (local <= 1)
//			ping(1-local);
	}

//	private void ping(int dstLink) {
//		int srcLink = Machine.networkLink().getLinkAddress();
//
//		System.out.println("PING " + dstLink + " from " + srcLink);
//
//		long startTime = Machine.timer().getTime();
//
//		//MailMessage ping;
//		UdpPacket ping;
//
//		byte[] barr = {'q','a','a','a','a','a','a','a','a','a','a','a'};
//
//		try {
//			ping = new UdpPacket(dstLink,0, Machine.networkLink().getLinkAddress(),
//					1,UdpPacket.DATA, 0,
//					barr);
//		}
//		catch (MalformedPacketException e) {
//			Lib.assertNotReached();
//			return;
//		}
//
//		postOffice.send(ping);
//
//		//MailMessage ack = postOffice.receive(0);
//		UdpPacket ack = postOffice.receive(0);
//
//		System.out.println(ack);
//
//		long endTime = Machine.timer().getTime();
//
//		System.out.println("time=" + (endTime-startTime) + " ticks");
//	}
//
//	private void pingServer() {
//		while (true) {
//			//MailMessage ping = postOffice.receive(1);
//			UdpPacket ping = postOffice.receive(1);
//
//			UdpPacket ack;
//
//			try {
//		/*ack = new MailMessage(ping.packet.srcLink, ping.srcPort,
//				      ping.packet.dstLink, ping.dstPort,
//				      ping.contents);*/
//				ack = new UdpPacket(ping.packet.dstLink, ping.destPort, ping.packet.srcLink, ping.srcPort, UdpPacket.DATA,0, ping.payload);
//			}
//			catch (MalformedPacketException e) {
//				// should never happen...
//				continue;
//			}
//
//			postOffice.send(ack);
//		}
//	}
//
	/**
	 * Start running user programs.
	 */
	public void run() {
		super.run();
	/*
		NetProcess process = NetProcess.newNetProcess();

		String shellProgram = Machine.getShellProgramName();
		Lib.assertTrue(process.execute(shellProgram, new String[] { }));

		KThread.currentThread().finish();*/
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
	}

	//Had to make it available to use the send/receive functions
	public static PostOffice postOffice;

	// dummy variables to make javac smarter
	private static NetProcess dummy1 = null;
}
