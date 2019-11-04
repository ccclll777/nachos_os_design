package nachos.network;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.threads.KThread;
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
		ncc = new NetCommandCenter();	// used for selfTest/run
	}

	/**
	 * Test the network. Create a server thread that listens for pings on port
	 * 1 and sends replies. Then ping one or two hosts. Note that this test
	 * assumes that the network is reliable (i.e. that the network's
	 * reliability is 1.0).
	 */
	public void selfTest() {
		super.selfTest();
		
		// stalling to prepare other machines to run
//		System.out.println("Press any key to start the network test...");
//		console.readByte(true);
		
		/**
		 * A simple test to verify NetCommandCenter's connect/accept protocol.
		 * Currently commented out to do other testing
		 */
		
//		int local = Machine.networkLink().getLinkAddress();
//		
//		// send syn packet if network 0
//		if(local == 0) {
//			ncc.connect(1, 1);
//		}
//		// send synack packet if network 1 and syn packet is detected 
//		else {
//			ncc.accept(1);
//		}
//		Machine.halt();
	}

	/**
	 * Start running user programs.
	 * To replicate the testing do the following:
	 *  0) Before running, use "-d n" in run config arguments for better view.
	 * 	1) Create 3 machine instances (3 consoles)
	 *  2) Press any key in Network0
	 *  3) Press any key in Network1
	 *  4) Press any key in Network2
	 *  5) Hope that the output doesn't fail (Current implementation suffers from synchronization
	 *  issues)
	 */
	public void run() {
		super.run();
//		 C code testing for project 3 (copy and paste from project 2)
		int local = Machine.networkLink().getLinkAddress();

		// Network0 testing
		if(local == 0) {
			// Create NetProcess
			NetProcess process = NetProcess.newNetProcess();

			// client = chat.c
			String shellProgram = "client.coff";

			// Pass arguments for coff file
			String[] arguments = { "2", "15", "Tired...", ""+"Tired...".length()};

			// Run the program
			Lib.assertTrue(process.execute(shellProgram, arguments));

			KThread.currentThread().finish();
		}
		// Network 1 testing
		else if(local == 1) {
			// Create NetProcess
			NetProcess process = NetProcess.newNetProcess();

			// client = chat.c
			String shellProgram = "client.coff";

			// Pass arguments for coff file
			String[] arguments = { "2", "15", "I am", ""+"I am".length() };

			// Run the program
			Lib.assertTrue(process.execute(shellProgram, arguments));

			KThread.currentThread().finish();
		}
		// Network 2 testing
		else {
			// Create NetProcess
			NetProcess process = NetProcess.newNetProcess();

			// host = chatserver.c
			String shellProgram = "host.coff";

			// Pass arguments for coff file
			String[] arguments = { "15", ""+"Tired...".length(), ""+"I am".length() };

			// Run the program
			Lib.assertTrue(process.execute(shellProgram, arguments));

			KThread.currentThread().finish();
		}
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
	}

	/*
	private void ping(int dstLink) {
		int srcLink = Machine.networkLink().getLinkAddress();

		System.out.println("PING " + dstLink + " from " + srcLink);

		long startTime = Machine.timer().getTime();

		MailMessage ping;

		try {
			ping = new MailMessage(dstLink, 1,
					Machine.networkLink().getLinkAddress(), 0,
					new byte[0]);
		}
		catch (MalformedPacketException e) {
			Lib.assertNotReached();
			return;
		}

		postOffice.send(ping);

		MailMessage ack = postOffice.receive(0);

		long endTime = Machine.timer().getTime();

		System.out.println("time=" + (endTime-startTime) + " ticks");	
	}

	private void pingServer() {
		while (true) {
			MailMessage ping = postOffice.receive(1);

			MailMessage ack;

			try {
				ack = new MailMessage(ping.packet.srcLink, ping.srcPort,
						ping.packet.dstLink, ping.dstPort,
						ping.contents);
			}
			catch (MalformedPacketException e) {
				// should never happen...
				continue;
			}

			postOffice.send(ack);
		}	
	}
	*/
	// variables for selfTest
	private PostOffice postOffice;
	private NetCommandCenter ncc; // used for selfTest/run

	// dummy variables to make javac smarter
	private static NetProcess dummy1 = null;
}
