package socs.network.message;

import java.io.Serializable;
import java.util.LinkedList;

@SuppressWarnings("serial")
public class LSA implements Serializable {

	// IP address of the router originate this LSA
	public String linkStateID;
	public int lsaSeqNumber = Integer.MIN_VALUE;

	public LinkedList<LinkDescription> links = new LinkedList<LinkDescription>();

	public LSA() {
	}

	public LSA(String srcIP, int lsaSeqNumber) {
		this.linkStateID = srcIP;
		this.lsaSeqNumber = lsaSeqNumber;
	}

	@Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(linkStateID + ":").append(lsaSeqNumber + "\n");
      for (LinkDescription ld : links) {
        sb.append(ld);
      }
      sb.append("\n");
      return sb.toString();
    }
}
