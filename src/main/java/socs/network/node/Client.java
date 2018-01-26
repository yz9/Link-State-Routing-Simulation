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

  // TODO
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

      }

      // if received hello
      if (packet.sospfType == 0){
        // TODO handles hello part
        // change status INIT etc..
      }

      RouterDescription response = new RouterDescription();
  		response.processPortNumber = packet.srcProcessPort;
  		response.simulatedIPAddress = packet.srcIP;

    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
  }
}
