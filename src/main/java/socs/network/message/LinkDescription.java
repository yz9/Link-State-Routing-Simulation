package socs.network.message;

import java.io.Serializable;

@SuppressWarnings("serial")
public class LinkDescription implements Serializable {
	public String linkID;
	public int portNum;
	public int weight;

	public String toString() {
		return linkID + "," + portNum + "," + weight;
	}

	public LinkDescription() {
	}

	public LinkDescription(String linkID, int portNum, int toMetrics) {
		this.linkID = linkID;
		this.portNum = portNum;
		this.weight = toMetrics;
	}

}
