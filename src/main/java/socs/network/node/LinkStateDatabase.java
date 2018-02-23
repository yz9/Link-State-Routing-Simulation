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
		// initialize the LSA
		LSA lsa = new LSA();
		lsa.linkStateID = rd.simulatedIPAddress;
		lsa.lsaSeqNumber = Integer.MIN_VALUE;
		LinkDescription ld = new LinkDescription();
		ld.linkID = rd.simulatedIPAddress;
		ld.portNum = -1;
		ld.tosMetrics = 0;
		lsa.links.add(ld);
		_store.put(lsa.linkStateID, lsa);
	}

	/**
	 * output the shortest path from this router to the destination with the given
	 * IP address
	 * http://www.vogella.com/tutorials/JavaAlgorithmsDijkstra/article.html
	 */
	String getShortestPath(String destinationIP) {

		// init list for the algorithm
		Set<LSA> settledRouter = new HashSet<>();
		Set<LSA> unsettledRouter = new HashSet<>();
		Map<LSA, Integer> distance = new HashMap<>();
		Map<LSA, LSA> predecessors = new HashMap<>();

		// the root node
		LSA root = this._store.get(rd.simulatedIPAddress);
		// add the initial root distance
		distance.put(root, 0);
		// add the root to the temp path
		unsettledRouter.add(root);

		// while there is still unconfirmed path, keep running the algorithm
		while (unsettledRouter.size() > 0) {
			LSA node = getMinimum(unsettledRouter, distance);
			// add the node to the confirmed path and remove it from temp path
			settledRouter.add(node);
			unsettledRouter.remove(node);
			findMinimalDistances(node, predecessors, distance, unsettledRouter);
		}

		LinkedList<LSA> path = getPath(this._store.get(destinationIP), predecessors);

		// process the path to get the proper output
		String output = "";

		ListIterator<LSA> listIterator = path.listIterator();

		while (listIterator.hasNext()) {
			LSA current = listIterator.next();

			String ip = current.linkStateID;

			if (listIterator.hasNext()) {
				String arrow = " -> ";

				String neighbor = listIterator.next().linkStateID;
				listIterator.previous();

				int weight = -1;

				for (LinkDescription ld : current.links) {
					if (ld.linkID.equals(neighbor)) {
						weight = ld.tosMetrics;
					}
				}

				String w = " (" + weight + ") ";

				output = output + ip + " " + arrow + w;
			} else {
				output = output + " " + ip;
			}
		}

		return output;
	}

	private LinkedList<LSA> getPath(LSA target, Map<LSA, LSA> predecessors) {
		// actually finding the path from dest to source....
		LinkedList<LSA> path = new LinkedList<LSA>();
		LSA step = target;
		// check if a path exists
		if (predecessors.get(step) == null) {
			return null;
		}
		path.add(step);
		while (predecessors.get(step) != null) {
			step = predecessors.get(step);
			path.add(step);
		}
		// reverse to make source to dest order
		Collections.reverse(path);
		return path;
	}

	private void findMinimalDistances(LSA node, Map<LSA, LSA> predecessors, Map<LSA, Integer> distance,
			Set<LSA> tempPath) {
		// grab all the neighbors of the current node
		LinkedList<LSA> adjacentNodes = getNeighbors(node);

		// look at each neighbor node
		for (LSA target : adjacentNodes) {

			// if the current distance to the target (an arbitrary neighbor) is greater than
			// the distance to the current node + the target
			if (getShortestDistance(target, distance) > getShortestDistance(node, distance)
					+ getDistance(node, target)) {

				if (!predecessors.containsKey(target)) {
					// add the neighbor to the distance, predecessors, and tentative list
					distance.put(target, getShortestDistance(node, distance) + getDistance(node, target));
					predecessors.put(target, node);
					tempPath.add(target);
				}

			}
		}
	}

	// find the node with the minimal distance
	private LSA getMinimum(Set<LSA> tentative, Map<LSA, Integer> distance) {
		LSA minNode = null;

		// for all routers in the tenative list..
		for (LSA router : tentative) {
			if (minNode == null) {
				minNode = router;
				continue;
			}
			// if the current candidate rotuer's distance is smaller than the minNode
			// rotuer, replace it
			if (getShortestDistance(router, distance) < getShortestDistance(minNode, distance)) {
				minNode = router;
			}
		}
		return minNode;
	}

	private int getShortestDistance(LSA router, Map<LSA, Integer> distance) {
		// if the router does not exist, means the path is unavailable, return a
		// arbitary large int....
		if (distance.get(router) == null) {
			return Integer.MAX_VALUE;
		} else {
			return distance.get(router);
		}
	}

	// <--------------------helper functions----------------------->

	private int getDistance(LSA start, LSA target) {
		for (LinkDescription ld : start.links) {
			if (ld.linkID.equals(target.linkStateID)) {
				return ld.tosMetrics;
			}
		}
		return -1;
	}

	private LinkedList<LSA> getNeighbors(LSA node) {
		LinkedList<LSA> neighbors = new LinkedList<LSA>();
		for (LinkDescription ld : node.links) {
			LSA neighbor = _store.get(ld.linkID);
			neighbors.add(neighbor);
		}
		return neighbors;
	}

	public void addNewLinkToDB(Link link) {
		LSA newLsa = new LSA(rd.simulatedIPAddress, getSeqNumber(rd.simulatedIPAddress) + 1);

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

	public synchronized void add(String simulatedIPAddress, LSA lsa) {
		_store.put(simulatedIPAddress, lsa);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (LSA lsa : _store.values()) {
			sb.append(lsa.linkStateID).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
			for (LinkDescription ld : lsa.links) {
				sb.append(ld.linkID).append(",").append(ld.portNum).append(",").append(ld.tosMetrics).append("\t");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

}
