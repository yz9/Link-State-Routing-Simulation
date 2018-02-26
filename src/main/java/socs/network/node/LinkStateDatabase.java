package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.util.WeightedGraph;

import java.util.*;

public class LinkStateDatabase {

	// linkID => LSAInstance
	HashMap<String, LSA> storage = new HashMap<String, LSA>();

	private RouterDescription rd = null;

	public LinkStateDatabase(RouterDescription routerDescription) {
		rd = routerDescription;
		// initialize the LSA
		LSA lsa = new LSA();
		lsa.linkStateID = rd.simulatedIPAddress;
		lsa.lsaSeqNumber = Integer.MIN_VALUE;
		LinkDescription ld = new LinkDescription();
		ld.linkID = rd.simulatedIPAddress;
		ld.portNum = -1;
		ld.weight = 0;
		lsa.links.add(ld);
		storage.put(lsa.linkStateID, lsa);
	}

	/**
	 * output the shortest path from this router to the destination with the given
	 * IP address
	 * http://www.vogella.com/tutorials/JavaAlgorithmsDijkstra/article.html
	 */
	String getShortestPath(String destinationIP) {
		
		// construct the graph 
		WeightedGraph topology = new WeightedGraph(storage);
		String path = topology.getShortestPath(rd.simulatedIPAddress, destinationIP);
		return path;
	}

	public void addNewLinkToDB(Link link) {
		LSA newLsa = new LSA(rd.simulatedIPAddress, getSeqNumber(rd.simulatedIPAddress) + 1);

		LSA oldLsa = storage.get(rd.simulatedIPAddress);
		System.out.println(oldLsa);
		System.out.println(link);
		newLsa.links.addAll(oldLsa.links);
		LinkDescription new1 = new LinkDescription(link.router2.simulatedIPAddress, link.router2.processPortNumber,
				link.weight);
		newLsa.links.add(new1);
		storage.put(rd.simulatedIPAddress, newLsa);
	}

	public int getSeqNumber(String srcIP) {
		return storage.get(srcIP).lsaSeqNumber;
	}

	public LSA getLSA(String srcIP) {
		return storage.get(srcIP);
	}

	public synchronized void add(String simulatedIPAddress, LSA lsa) {
		storage.put(simulatedIPAddress, lsa);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (LSA lsa : storage.values()) {
			sb.append(lsa.linkStateID).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
			for (LinkDescription ld : lsa.links) {
				sb.append(ld.linkID).append(",").append(ld.portNum).append(",").append(ld.weight).append("\t");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
