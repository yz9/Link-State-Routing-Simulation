package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Client {
  private RouterDescription rd;
  private Socket client;

  public Client(Socket client, RouterDescription rd) {
    this.client = client;
    this.rd = rd;
  }

  // TODO
  public void connect() {
    System.out.println("Connecting to " + this.rd.simulatedIPAddress + " on port " + this.rd.processPortNumber);
    
  }
}
