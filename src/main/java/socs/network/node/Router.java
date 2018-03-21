package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.Packet;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Vector;

public class Router {

	protected LinkStateDatabase lsd;

	RouterDescription rd = new RouterDescription();

	// assuming that all routers are with ports.length ports
	volatile Link[] ports = new Link[4];
	private boolean usedStart = false;

	public Router(Configuration config) {
		rd.simulatedIPAddress = config.getString("socs.network.router.ip");
		rd.processPortNumber = config.getShort("socs.network.router.port");
		// set processIPAddress to localhost
		rd.processIPAddress = "127.0.0.1";

		System.out.println("Process IP - " + rd.processIPAddress);
		System.out.println("Port Number - " + rd.processPortNumber);
		System.out.println("Simulated IP - " + rd.simulatedIPAddress);
		lsd = new LinkStateDatabase(rd);

		// now create the server socket
		Thread server = new Thread(new Server(rd, ports, lsd));
		server.start();
	}

	/**
	 * output the shortest path to the given destination ip format: source ip
	 * address -> ip address -> ... -> destination ip
	 *
	 * @param destinationIP
	 *            the ip adderss of the destination simulated router
	 */
	private void processDetect(String destinationIP) {
		System.out.println(lsd.getShortestPath(destinationIP));
	}

	/**
	 * disconnect with the router identified by the given destination ip address
	 * Notice: this command should trigger the synchronization of database
	 *
	 * @param portNumber
	 *            the port number which the link attaches at
	 */
	 private void processDisconnect(short portNumber) {
	     int curPort = getCurrentPortSize();
	     if (portNumber < 0 || portNumber > curPort){
	         System.err.println("Invalid port number.");
	         return;
	     }

	     RouterDescription router2 = this.ports[portNumber].router2;

	     try {
	         Socket client = new Socket(router2.processIPAddress, router2.processPortNumber);
	         ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
	         ObjectInputStream in = new ObjectInputStream(client.getInputStream());

	         // initiate a new packet
			 // NOTE! here:
	         Packet packet = new Packet(rd.simulatedIPAddress, router2.simulatedIPAddress, (short) 3);
	         out.writeObject(packet);

	         this.ports[portNumber] = null;
	         LSA lsa = createLSA();
	         broadcastUpdate(lsa);

	         // clean up
	         in.close();
	         out.close();
	         client.close();
	     } catch (UnknownHostException e) {
	         // TODO Auto-generated catch block
	         e.printStackTrace();
	     } catch (IOException e) {
	         // TODO Auto-generated catch block
	         e.printStackTrace();
	     }
	 }

	/**
	 * attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to indentify
	 * the process IP and process Port; additionally, weight is the cost to
	 * transmitting data through the link
	 * <p/>
	 * NOTE: this command should not trigger link database synchronization
	 */
	private void processAttach(String processIP, short processPort, String simulatedIP, short weight) {
		// check if there exists at least one available ports
		int linkIndex = findAvailablePort();
		// case 1: all ports are used
		if (linkIndex == -1) {
			System.err.println("Error: All ports has been used.");
			return;
		}

		// case 2: if router is trying to connect to itself
		if (simulatedIP.equals(rd.simulatedIPAddress)) {
			System.err.println("Error: Router cannot attach to itself");
			return;
		}

		// case 3: check if the requested router has already connected with our router.
		if (containsIP(simulatedIP)) {
			System.err.println("Error: Router has already connected with requested router");
			return;
		}

		RouterDescription remoteRouter = new RouterDescription(processIP, processPort, simulatedIP);
		// attach the link to the remote router
		try {
			// create
			Socket remoteSocket = new Socket(processIP, processPort);
			ObjectOutputStream output = new ObjectOutputStream(remoteSocket.getOutputStream());
			ObjectInputStream input = new ObjectInputStream(remoteSocket.getInputStream());
			Packet linkRequest = Packet.AttachLinkRequest(this.rd.simulatedIPAddress, simulatedIP, weight);
			output.writeObject(linkRequest);

			ports[linkIndex] = new Link(rd, remoteRouter, weight);
			System.out.println("--- attached with " + linkRequest.dstIP + " ---");
			input.close();
			output.close();
			remoteSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * broadcast Hello to neighbors
	 */
	private void processStart() {

		if (ports[0] == null) {
			System.err.println("You haven't attached to any other router yet.");
			return;
		}

		this.usedStart = true;

		for (int i = 0; i < ports.length; i++) {
			if (ports[i] != null) {
				try {
					Socket client = new Socket(ports[i].router2.processIPAddress, ports[i].router2.processPortNumber);
					ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
					ObjectInputStream in = new ObjectInputStream(client.getInputStream());

					String destIP = ports[i].router2.simulatedIPAddress;
					short destPort = ports[i].router2.processPortNumber;
					// initate a new packet
					Packet packet = new Packet(this.rd.simulatedIPAddress, destIP, (short) 0);
					packet.srcProcessIP = this.rd.processIPAddress;
					packet.srcProcessPort = this.rd.processPortNumber;
					// stail back hello packet
					out.writeObject(packet);

					// note that a router can only head with the same router once.
					if (ports[i].router2.status != RouterStatus.TWO_WAY) {
						Packet recv = (Packet) in.readObject();

						if (recv == null) {
							System.err.println("missing packet");
							break;
						}

						// check if received packet has sospfType "hello"
						if (recv.sospfType == (short) 0) {
							System.out.println("received HELLO from " + recv.srcIP + ";");
							ports[i].router2.status = RouterStatus.TWO_WAY;
							System.out.println("set " + ports[i].router2.simulatedIPAddress + " state to TWO_WAY");
							out.writeObject(packet);
						}

						// construct lsa
						LSA lsa = createLSA();
						// broadcast LSAUPDATE
						broadcastUpdate(lsa);

						// iterative
					} else {
						System.err.println("Already connected with router " + ports[i].router2.simulatedIPAddress);
					}

					// clean up
					out.close();
					in.close();
					client.close();
				} catch (UnknownHostException e) {
					System.err.println("Trying to connect to unknown host: " + e);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * attach the link to the remote router, which is identified by the given
	 * simulated ip; to establish the connection via socket, you need to indentify
	 * the process IP and process Port; additionally, weight is the cost to
	 * transmitting data through the link
	 * <p/>
	 * This command does trigger the link database synchronization
	 */
	private void processConnect(String processIP, short processPort, String simulatedIP, short weight) {
		// init with start first
		if (!usedStart){
			System.err.println("Must use start command first");
			return;
		}
		
		// if the router is connected already, don't do anything
		for(Link link: ports) {
			if(link.router2.simulatedIPAddress.equals(simulatedIP)) {
				return;
			}
		}
		
		// now connect the new router
		System.out.println("--- connecting to " + simulatedIP + " ---");
		processAttach(processIP, processPort, simulatedIP, weight);
		processStart();
		System.out.println("--- conencted with " + simulatedIP + " ---");
	}

	/**
	 * output the neighbors of the routers
	 */
	private void processNeighbors() {
		for (int i = 0; i < ports.length; i++) {
			if (ports[i] != null && ports[i].router2.status == RouterStatus.TWO_WAY) {
				System.out.println("IP address of neighbor " + (i + 1) + ": " + ports[i].router2.simulatedIPAddress);
			}
		}
	}

	/**
	 * disconnect with all neighbors and quit the program
	 */
	private void processQuit() {
		for (int i = 0; i < ports.length; i++){
			// disconnect with all neighbors
			if (ports[i] != null && ports[i].router2.status == RouterStatus.TWO_WAY){
				this.processDisconnect((short) i);
			}
		}
		// quit the program
		System.out.println("Quit successfully");
		System.exit(0);
	}

	// <--------------------helper functions----------------------->

	// return -1 if not found else return the available index.
	private int findAvailablePort() {
		for (int i = 0; i < ports.length; i++) {
			if (ports[i] == null) {
				return i;
			}
		}
		return -1;
	}

	private boolean containsIP(String simulatedIP) {
		for (int i = 0; i < ports.length; i++) {
			if (ports[i] != null) {
				if (ports[i].router2.simulatedIPAddress.equals(simulatedIP)) {
					return true;
				}
			}
		}
		return false;
	}

	// create a LSA
	private LSA createLSA() {
		LSA lsa = new LSA();
		// retrive the latest seqNum from db
		int currentSeq = lsd.storage.get(rd.simulatedIPAddress).lsaSeqNumber;
		if (currentSeq == Integer.MIN_VALUE) {
			// initialize seqNum(version) to 0
			lsa.lsaSeqNumber = 0;
		} else {
			lsa.lsaSeqNumber = currentSeq + 1;
		}

		lsa.linkStateID = rd.simulatedIPAddress;

		lsa.links = new LinkedList<LinkDescription>();
		for (int i = 0; i < ports.length; i++) {
			if (ports[i] != null && ports[i].router2.status != null) { // valid connection
				LinkDescription ld = new LinkDescription(ports[i].router2.simulatedIPAddress,
						ports[i].router2.processPortNumber, ports[i].weight);
				lsa.links.add(ld);
			}
		}

		// update lsd
		lsd.add(rd.simulatedIPAddress, lsa);
		return lsa;
	}

	// broadcasts LSAUPDATE whiche conatins the latest info of the link sate to all neighbors
	private void broadcastUpdate(LSA lsa) {
		Vector<LSA> links = new Vector<LSA>();
		// construct the LSAUPDATE packet
		for (int i = 0; i < ports.length; i++) {
			if (ports[i] != null) {
				Socket client;
				try {
					client = new Socket(ports[i].router2.processIPAddress, ports[i].router2.processPortNumber);
					ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
					Packet LSAUPDATE = new Packet(rd.simulatedIPAddress, ports[i].router2.simulatedIPAddress, (short) 1);
					links.add(lsa);
					LSAUPDATE.lsaArray = links;
					out.writeObject(LSAUPDATE);
				} catch (UnknownHostException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

		}
	}

	private short getCurrentPortSize(){
		short size = 0;
		for(int i = 0; i < ports.length; i++){
			if (ports[i] != null && ports[i].router2 != null){
				size++;
			}
		}
		return size;
	}

	public void terminal() {
		try {
			InputStreamReader isReader = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(isReader);
			System.out.print(">> ");
			String command = br.readLine();
			while (true) {
				if (command.startsWith("detect ")) {
					String[] cmdLine = command.split(" ");
					processDetect(cmdLine[1]);
				} else if (command.startsWith("disconnect ")) {
					String[] cmdLine = command.split(" ");
					processDisconnect(Short.parseShort(cmdLine[1]));
				} else if (command.startsWith("quit")) {
					processQuit();
				} else if (command.startsWith("attach ")) {
					String[] cmdLine = command.split(" ");
					processAttach(cmdLine[1], Short.parseShort(cmdLine[2]), cmdLine[3], Short.parseShort(cmdLine[4]));
				} else if (command.equals("start")) {
					processStart();
				} else if (command.equals("connect ")) {
					String[] cmdLine = command.split(" ");
					processConnect(cmdLine[1], Short.parseShort(cmdLine[2]), cmdLine[3], Short.parseShort(cmdLine[4]));
				} else if (command.equals("neighbors")) {
					// output neighbors
					processNeighbors();
				} else {
					// invalid command
					break;
				}
				System.out.print(">> ");
				command = br.readLine();
			}
			isReader.close();
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
