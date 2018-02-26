package socs.network.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

public class WeightedGraph {
	private Map<String, Node> nodes = new HashMap<>();
	private List<Edge> edges = new ArrayList<>();

	private Set<Node> settledNodes;
	private Set<Node> unSettledNodes;
	private Map<Node, Node> predecessors;
	private Map<Node, Integer> distance;

	public WeightedGraph(HashMap<String, LSA> storage) {
		// construct the nodes
		for (String key : storage.keySet()) {
			Node vertex = new Node(key);
			nodes.put(key, vertex);
		}
		// construct the edges
		for (String key : storage.keySet()) {
			Node source = nodes.get(key);
			for (LinkDescription description : storage.get(key).links) {
				Edge edge = new Edge(source, nodes.get(description.linkID), description.weight);
				edges.add(edge);
			}
		}
	}

	/*
	 * This method returns the path from the source to the selected target and NULL
	 * if no path exists
	 */
	public String getShortestPath(String sourceIP, String destIP) {
		
		Node source = nodes.get(sourceIP);
		settledNodes = new HashSet<Node>();
		unSettledNodes = new HashSet<Node>();
		distance = new HashMap<Node, Integer>();
		predecessors = new HashMap<Node, Node>();
		distance.put(source, 0);
		unSettledNodes.add(source);
		while (unSettledNodes.size() > 0) {
			Node node = getMinimum(unSettledNodes);
			settledNodes.add(node);
			unSettledNodes.remove(node);
			findMinimalDistances(node);
		}
		
		// find the path
		int totalWeight = 0;
		Node target = nodes.get(destIP);
		LinkedList<Node> path = new LinkedList<Node>();
		Node step = target;
		// check if a path exists
		if (predecessors.get(step) == null) {
			return null;
		}
		path.add(step);
		while (predecessors.get(step) != null) {
			// moving forward
			step = predecessors.get(step);
			path.add(step);
		}
		// Put it into the correct order
		Collections.reverse(path);

		String output = "";
		for (int i = 0; i < path.size() - 1; i++) {
			totalWeight += getDistance(path.get(i), path.get(i + 1));
		}
		for (Node v : path) {
			output += v.simulatedIp + " -> ";
		}
		output += totalWeight;
		return output;
	}

	private void findMinimalDistances(Node node) {
		List<Node> adjacentNodes = getNeighbors(node);
		for (Node target : adjacentNodes) {
			if (getShortestDistance(target) > getShortestDistance(node) + getDistance(node, target)) {
				distance.put(target, getShortestDistance(node) + getDistance(node, target));
				predecessors.put(target, node);
				unSettledNodes.add(target);
			}
		}
	}

	private int getDistance(Node node, Node target) {
		for (Edge edge : edges) {
			if (edge.source.equals(node) && edge.destination.equals(target)) {
				return edge.weight;
			}
		}
		throw new RuntimeException("Should not happen");
	}

	private List<Node> getNeighbors(Node node) {
		List<Node> neighbors = new ArrayList<Node>();
		for (Edge edge : edges) {
			if (edge.source.equals(node) && !isSettled(edge.destination)) {
				neighbors.add(edge.destination);
			}
		}
		return neighbors;
	}

	private Node getMinimum(Set<Node> vertexes) {
		Node minimum = null;
		for (Node vertex : vertexes) {
			if (minimum == null) {
				minimum = vertex;
			} else {
				if (getShortestDistance(vertex) < getShortestDistance(minimum)) {
					minimum = vertex;
				}
			}
		}
		return minimum;
	}

	private boolean isSettled(Node vertex) {
		return settledNodes.contains(vertex);
	}

	private int getShortestDistance(Node destination) {
		Integer d = distance.get(destination);
		if (d == null) {
			return Integer.MAX_VALUE;
		} else {
			return d;
		}
	}


	public void printGraph() {
		System.out.println("=====================");
		String nodesList = "";
		for (Node v : nodes.values()) {
			nodesList += v.simulatedIp + ", ";
		}
		System.out.println(nodesList);
		for (Edge edge : edges) {
			System.out.println(edge);
		}
		System.out.println("=====================");
	}
}

class Node {
	final public String simulatedIp;

	public Node(String pIp) {
		simulatedIp = pIp;
	}

	public boolean equals(Node pVertex) {
		return simulatedIp.equals(pVertex.simulatedIp);
	}
}

class Edge {
	final Node source;
	final Node destination;
	final int weight;

	public Edge(Node source, Node destination, int weight) {
		this.source = source;
		this.destination = destination;
		this.weight = weight;
	}

	@Override
	public String toString() {
		return source.simulatedIp + " -> " + destination.simulatedIp + " (" + weight + ")";
	}
}