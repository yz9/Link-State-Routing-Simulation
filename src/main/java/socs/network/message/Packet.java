package socs.network.message;

import java.io.*;
import java.util.Vector;

public class Packet implements Serializable {

	// for inter-process communication
	public String srcProcessIP;
	public short srcProcessPort;

	// simulated IP address
	public String srcIP;
	public String dstIP;

	// common header
	public short sospfType; // 0 - HELLO, 1 - LinkState Update, 2 - Attach, 3 - Disconnect  
	public String routerID;

	// simulated IP address
	public String neighborID; // neighbor's simulated IP address

	// used by LSAUPDATE
	public Vector<LSA> lsaArray = null;

	// used for attach (link establishment)
	public int weight;

	public Packet(String simulatedSrcIP, String simulatedDstIP, short packetType) {
		this.srcIP = simulatedSrcIP;
		this.dstIP = simulatedDstIP;
		this.sospfType = packetType;
	}

	public Packet() {
	}

	// create a LinkState Update package
	public static Packet LSAUPDATE(String simulatedSrcIP, String simulatedDstIP, Vector<LSA> lsaArray) {
		Packet packet = new Packet(simulatedSrcIP, simulatedDstIP, (short) 1);
		packet.lsaArray = lsaArray;
		return packet;
	}

	// create a AttachLinkRequest packet used in attach()
	public static Packet AttachLinkRequest(String simulatedSrcIP, String simulatedDstIP, int weight) {
		Packet packet = new Packet(simulatedSrcIP, simulatedDstIP, (short) 2);
		packet.weight = weight;
		return packet;
	}
}
