package socs.network.node;

import socs.network.message.Packet;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client implements Runnable {
	private RouterDescription rd;
	private Socket client;
	private Link[] ports;

	public Client(Socket client, RouterDescription rd, Link[] ports) {
		this.client = client;
		this.rd = rd;
		this.ports = ports;
	}

	// return -1 if not found else return the available index.
	private int findAvailablePort(Link[] ports) {
		for (int i = 0; i < 4; i++) {
			if (ports[i] == null) {
				return i;
			}
		}
		return -1;
	}

	public int getRouter2Index(Link[] ports, String srcIP) {
		for (int i = 0; i < 4; i++) {
			if (ports[i] != null && ports[i].router2.simulatedIPAddress.equals(srcIP)) {
				return i;
			}
		}
		// not found
		return -1;
	}

	public void run() {
		try {
			// System.out.println("Connecting to " + this.rd.simulatedIPAddress + " on port " + this.rd.processPortNumber);

			ObjectInputStream in = new ObjectInputStream(client.getInputStream());
			ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());

			// get the packet client sends
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
				int index = getRouter2Index(ports, packet.srcIP);
				if (packet.dstIP.equals(rd.simulatedIPAddress)) {
					// create a new link if not exists
					if (index == -1) {
						int spot = findAvailablePort(ports);
						if (spot != -1) {
							// add link to ports
							RouterDescription remote = new RouterDescription(packet.srcProcessIP, packet.srcProcessPort, packet.srcIP);
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
        // note that a router can only start with the same router once.
				if (ports[index].router2.status != RouterStatus.TWO_WAY) {
          // received a HELLO msg, set status of srcRouter to INIT
          System.out.println("received HELLO from " + packet.srcIP + ";");
					ports[index].router2.status = RouterStatus.INIT;
					System.out.println("set " + ports[index].router2.simulatedIPAddress + " state to INIT");
					// send response package
					out.writeObject(send);

          // waiting for hello msg
          Packet recv = (Packet) in.readObject();
          if(recv == null) {
            System.err.println("Error: missing packet");
            return;
          }
          else{
            System.out.println("received HELLO from " + recv.srcIP + ";");
            ports[index].router2.status = RouterStatus.TWO_WAY;
            System.out.println("set " + ports[index].router2.simulatedIPAddress + " state to TWO_WAY");
          }
        }
        else{
					System.err.println("Already connected with router " + ports[index].router2.simulatedIPAddress);
        }

			} else if (packet.sospfType == 1) {
				// do something....
			} else if (packet.sospfType == 2) {
				/*
				 * handle attach command.
				 * write a string message back as confirmation
				 */
				// out.writeObject("Success");
				System.out.println("--- attached with " + packet.srcIP + " ---");
			} else {
				System.err.println("Error: Unexpected error");
			}
			// clean up
			client.close();
			in.close();
			out.close();
			System.out.print(">> ");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
