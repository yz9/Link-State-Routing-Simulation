package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.*;

public class LinkStateDatabase {

	// linkID => LSAInstance
	HashMap<String, LSA> _store = new HashMap<String, LSA>();

	private RouterDescription rd = null;

	public LinkStateDatabase(RouterDescription routerDescription) {
		rd = routerDescription;
		LSA l = initLinkStateDatabase();
		_store.put(l.linkStateID, l);
	}

	/**
	 * output the shortest path from this router to the destination with the given
	 * IP address
	 */
	String getShortestPath(String destinationIP) {
		//TODO: fill the implementation here
	    return null;
	}


	// <--------------------helper functions----------------------->

	public void addNewLinkToDB(Link link) {
		LSA newLsa = new LSA(rd.simulatedIPAddress, getSeqNumber(rd.simulatedIPAddress)+1);

		LSA oldLsa = _store.get(rd.simulatedIPAddress);
		System.out.println(oldLsa);
		System.out.println(link);
		newLsa.links.addAll(oldLsa.links);
		LinkDescription new1 = new LinkDescription(link.router2.simulatedIPAddress, link.router2.processPortNumber,
				link.weight);
		newLsa.links.add(new1);
		_store.put(rd.simulatedIPAddress, newLsa);
	}

	public int getSeqNumber(String srcIP) {
		return _store.get(srcIP).lsaSeqNumber;
	}

	public LSA getLSA(String srcIP) {
		return _store.get(srcIP);
	}

	public synchronized void add(String simulatedIPAddress, LSA lsa){
		_store.put(simulatedIPAddress, lsa);
	}

	// initialize the linkstate database by adding an entry about the router itself
	private LSA initLinkStateDatabase() {
		LSA lsa = new LSA();
		lsa.linkStateID = rd.simulatedIPAddress;
		lsa.lsaSeqNumber = Integer.MIN_VALUE;
		LinkDescription ld = new LinkDescription();
		ld.linkID = rd.simulatedIPAddress;
		ld.portNum = -1;
		ld.tosMetrics = 0;
		lsa.links.add(ld);
		return lsa;
	}

	public String toString() {
      StringBuilder sb = new StringBuilder();
      for (LSA lsa: _store.values()) {
        sb.append(lsa.linkStateID).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
        for (LinkDescription ld : lsa.links) {
          sb.append(ld.linkID).append(",").append(ld.portNum).append(",").
                  append(ld.tosMetrics).append("\t");
        }
        sb.append("\n");
      }
      return sb.toString();
    }

}
