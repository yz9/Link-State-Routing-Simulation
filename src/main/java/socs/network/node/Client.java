package socs.network.node;

import socs.network.message.SOSPFPacket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Client implements Runnable{
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

  public int getRouter2Index(Link[] ports, String srcIP){
    for(int i = 0; i < 4; i++){
      if(ports[i] != null && ports[i].router2.simulatedIPAddress.equals(srcIP)){
          return i;
      }
    }
    // not found
    return -1;
  }
  // TODO test 
  public void run() {
    try {
      System.out.println("Connecting to " + this.rd.simulatedIPAddress + " on port " + this.rd.processPortNumber);

      ObjectInputStream in = new ObjectInputStream(client.getInputStream());
      ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());

      // get the packet client sends
      SOSPFPacket packet = (SOSPFPacket) in.readObject();

      //empty packet
      if (packet == null) {
        System.err.println("Error: Empty packet");
        return;
      }

      if (packet.dstIP == rd.simulatedIPAddress){
        Link link;
        RouterDescription remote = new RouterDescription();
        remote.processPortNumber = packet.srcProcessPort;
    		remote.simulatedIPAddress = packet.srcIP;

        int index = getRouter2Index(ports, packet.srcIP);
        // create a new link if not exists
        if (index != -1){
          int spot = findAvailablePort(ports);
          if (spot != -1){
            // add link to ports
            link = new Link(rd, remote);
            ports[spot] = link;
          }
          System.err.println("error: no available port");
          return;
        }
        else{
          link = ports[index];
        }

        while (true) {

          // Start
          // if receive hello
          if (packet.sospfType == 0){
            // TODO handles hello part
            System.out.println("received HELLO from " + packet.srcIP + ";");
            if (link.router2.status == null){
              link.router2.status = RouterStatus.INIT;
              System.out.println("set " + packet.srcIP + " state to INIT");
            }
            else{
              link.router2.status = RouterStatus.TWO_WAY;
              System.out.println("set " + packet.srcIP + " state to TWO_WAY");

              SOSPFPacket send = new SOSPFPacket(rd.simulatedIPAddress, ports[index].router2.simulatedIPAddress, (short)0);

              out.writeObject(send);
            }
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
  }
}
