package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.Packet;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Vector;

public class TaskManager implements Runnable {
	private RouterDescription rd;
	private Socket client;
	private Link[] ports;
	private LinkStateDatabase db;

	public TaskManager(Socket client, RouterDescription rd, Link[] ports, LinkStateDatabase lsd) {
		this.client = client;
		this.rd = rd;
		this.ports = ports;
		this.db = lsd;
	}

	public void run() {
		try {
			ObjectInputStream in = new ObjectInputStream(client.getInputStream());
			ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
			// get the packet client
			Packet packet = (Packet) in.readObject();
			// empty packet
			if (packet == null) {
				System.err.println("Error: Empty packet");
				return;
			}
			/*
			 * Handle received packet accordingly based on the packet type
			 */
			if (packet.sospfType == 0) {
				// handle HELLO message
				// System.out.println("--received---");
				Link link = null;
				int index = getRouter2Index(packet.srcIP);
				if (packet.dstIP.equals(rd.simulatedIPAddress)) {
					// create a new link if not exists
					if (index == -1) {
						int spot = findAvailablePort(ports);
						if (spot != -1) {
							// add link to ports
							RouterDescription remote = new RouterDescription(packet.srcProcessIP, packet.srcProcessPort,
									packet.srcIP);
							link = new Link(rd, remote);
							index = spot;
							ports[index] = link;
						} else {
							System.err.println("Error: All ports has been used");
							return;
						}
					} else {
						link = ports[index];
					}
				}

				Packet send = new Packet(rd.simulatedIPAddress, ports[index].router2.simulatedIPAddress, (short) 0);
				// note that a router can only head with the same router once.
				if (ports[index].router2.status != RouterStatus.TWO_WAY) {
					// received a HELLO msg, set status of srcRouter to INIT
					System.out.println("received HELLO from " + packet.srcIP + ";");
					ports[index].router2.status = RouterStatus.INIT;
					System.out.println("set " + ports[index].router2.simulatedIPAddress + " state to INIT");
					// send response package
					out.writeObject(send);

					// waiting for hello msg
					Packet recv = (Packet) in.readObject();
					if (recv == null) {
						System.err.println("Error: missing packet");
						return;
					} else {
						System.out.println("received HELLO from " + recv.srcIP + ";");
						ports[index].router2.status = RouterStatus.TWO_WAY;
						System.out.println("set " + ports[index].router2.simulatedIPAddress + " state to TWO_WAY");
					}

					// construct a new lsa
					LSA lsa = createLSA();
					// broadcast the update to all neighbors
					broadcastLSA(lsa);

				} else {
					System.err.println("Already connected with router " + ports[index].router2.simulatedIPAddress);
				}
				System.out.print(">> ");

			} else if (packet.sospfType == 1) {

				/* When a router receives an LSA, it has to check whether it is already stored in databse
				 * If not, then the router needs to
				 * 1. add/update its database
				 * 2. broadcast to all links except the one which sent the LSAUPDATE.
				 */

				// retrive old LSA from the database
				LSA oldLSA = db.storage.get(packet.srcIP);
				// get the most recent lsa sent from client (stored in lsaArray)
				LSA newLSA = getMostRencentLSA(packet.lsaArray);
				// compare the seqNum, if newLSA has a larger seqNumber then we need to update db
				if (needUpdate(oldLSA, newLSA)) {
					// get where the remote router locates in ports
					int index = getRouter2Index(packet.srcIP);
					// get the corresponding link descriptor of the most recent LSA
					LinkDescription ld = getDescriptionByIP(newLSA.links);
					// if both exists then update the information
					if (index != -1 && ld != null) {
						// info not equal
						if (ld.weight != ports[index].weight && ld.weight > 0) {
							// update weight & LSA
							ports[index].weight = (short) ld.weight;

							LSA curLSA = db.storage.get(rd.simulatedIPAddress);
							curLSA.links = createLinks();
							db.add(rd.simulatedIPAddress, curLSA);
							// broadcast our current LSA to all neighbors
							broadcastLSA(curLSA);
						}
					}
					// put the newLSA to db
					db.add(packet.srcIP, newLSA);

					// forward the packet to all neighbors
					for (int i = 0; i < ports.length; i++) {
						if (ports[i] != null && !ports[i].router2.simulatedIPAddress.equals(packet.srcIP)) {
							// forward LSAUPDATE packet to its neighbors
							forwardPacket(packet);
						}
					}
				}
				else{
					// LSP has already been flooded
				}

			} else if (packet.sospfType == 2) {
				/*
				 * handle attach command. write a string message back as confirmation
				 */
				// out.writeObject("Success");
				System.out.println("--- attached with " + packet.srcIP + " ---");
				System.out.print(">> ");

			} else if (packet.sospfType == 3){
				// TODO disconnect
				int index = getRouter2Index(packet.srcIP);
				System.out.println("disconnect index :" + index);
				ports[index] = null;
				// construct a new lsa
				LSA lsa = createLSA();
				// broadcast the update to all neighbors
				broadcastLSA(lsa);
				System.out.print(">> ");
				
			} else {
				System.err.println("Error: Unexpected error");
			}
			// clean up
			client.close();
			in.close();
			out.close();
			// System.out.print(">> ");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	// <--------------------helper functions----------------------->

	// return -1 if not found else return the available index.
	private int findAvailablePort(Link[] ports) {
		for (int i = 0; i < ports.length; i++) {
			if (ports[i] == null) {
				return i;
			}
		}
		return -1;
	}

	// return -1 if not found else return the available index.
	public int getRouter2Index(String srcIP) {
		for (int i = 0; i < ports.length; i++) {
			if (ports[i] != null && ports[i].router2.simulatedIPAddress.equals(srcIP)) {
				return i;
			}
		}
		// not found
		return -1;
	}

		// create a LSA
	private LSA createLSA() {
		LSA lsa = new LSA();
		// retrive the latest seqNum from db
		int currentSeq = db.storage.get(rd.simulatedIPAddress).lsaSeqNumber;
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

		// update link state database
		db.add(rd.simulatedIPAddress, lsa);
		return lsa;
	}

	// broadcasts LSAUPDATE whiche conatins the latest info of the link sate to all neighbors
	private void broadcastLSA(LSA lsa) {
		Vector<LSA> links = new Vector<LSA>();
		// construct the LSAUPDATE packet
		for (int i = 0; i < ports.length; i++) {
			if (ports[i] != null) {
				Socket client;
				try {
					client = new Socket(ports[i].router2.processIPAddress, ports[i].router2.processPortNumber);
					ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
					Packet lsaUpdate = new Packet(rd.simulatedIPAddress, ports[i].router2.simulatedIPAddress, (short) 1);
					links.add(lsa);
					lsaUpdate.lsaArray = links;
					out.writeObject(lsaUpdate);
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	// forward the LSAUPDATE to all neighbors
	@SuppressWarnings("resource")
	private void forwardPacket(Packet packet) throws IOException {
		for (int i = 0; i < ports.length; i++) {
			if (ports[i] != null) {
				Socket clientSocket = new Socket(ports[i].router2.processIPAddress, ports[i].router2.processPortNumber);
				ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
				// broadcast the LSAUPDATE packet
				output.writeObject(packet);
			}
		}
	}

	private LinkDescription getDescriptionByIP(LinkedList<LinkDescription> list) {
		for (LinkDescription ld : list) {
			if (ld.linkID.equals(rd.simulatedIPAddress)) {
				return ld;
			}
		}
		return null;
	}

	private boolean needUpdate(LSA oldLSA, LSA newLSA) {
		if (oldLSA == null) {
			LSA lsa = createLSA();
			broadcastLSA(lsa);
			return true;
		}
		if (newLSA.lsaSeqNumber > oldLSA.lsaSeqNumber) {
			return true;
		}
		return false;
	}

	private LSA getMostRencentLSA(Vector<LSA> lsaArray) {
		return lsaArray.lastElement();
	}

	private LinkedList<LinkDescription> createLinks() {
		LinkedList<LinkDescription> newLinks = new LinkedList<LinkDescription>();
		for (int i = 0; i < ports.length; i++) {
			if (ports[i] != null && ports[i].router2.status != null) { // valid connection
				LinkDescription ld = new LinkDescription(ports[i].router2.simulatedIPAddress,
						ports[i].router2.processPortNumber, ports[i].weight);
				newLinks.add(ld);
			}
		}
		return newLinks;
	}

}
