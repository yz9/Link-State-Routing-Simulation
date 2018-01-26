package socs.network.message;

import java.io.*;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class SOSPFPacket implements Serializable {

	// for inter-process communication
	public String srcProcessIP;
	public short srcProcessPort;

	// simulated IP address
	public String srcIP;
	public String dstIP;

	// common header
	public short sospfType; // 0 - HELLO, 1 - LinkState Update
	public String routerID;

	// used by HELLO message to identify the sender of the message
	// e.g. when router A sends HELLO to its neighbor, it has to fill this field
	// with its own
	// simulated IP address
	public String neighborID; // neighbor's simulated IP address

	// used by LSAUPDATE
	public Vector<LSA> lsaArray = null;

	// used for attach (link establishment)
	public int weight;

	public SOSPFPacket(String simulatedSrcIP, String simulatedDstIP, short packetType) {
		this.srcIP = simulatedSrcIP;
		this.dstIP = simulatedDstIP;
		this.sospfType = packetType;
	}

	// create a LinkState Update package
	public static SOSPFPacket LSAUPDATE(String simulatedSrcIP, String simulatedDstIP, Vector<LSA> lsaArray) {
		SOSPFPacket packet = new SOSPFPacket(simulatedSrcIP, simulatedDstIP, (short) 1);
		packet.lsaArray = lsaArray;
		return packet;
	}

	// create a AttachLinkRequest packet used in attach()
	public static SOSPFPacket AttachLinkRequest(String simulatedSrcIP, String simulatedDstIP, int weight) {
		SOSPFPacket packet = new SOSPFPacket(simulatedSrcIP, simulatedDstIP, (short) 2);
		packet.weight = weight;
		return packet;
	}
}
