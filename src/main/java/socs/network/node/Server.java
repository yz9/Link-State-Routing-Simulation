package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
  private RouterDescription rd;
  private ServerSocket server;

  public Server(RouterDescription rd){
    this.rd = rd;
  }

  public void run(){
    System.out.println("server side" );
    try {
      server = new ServerSocket(this.rd.processPortNumber);
      System.out.println("Waiting for client on port: " + this.rd.processPortNumber);
      while (true) {
        Socket connection = server.accept();
        System.out.println("Just connected to " + connection.getRemoteSocketAddress());
        Client client = new Client(connection, this.rd);
        //client.connect();
      }
    } catch (IOException e) {
       e.printStackTrace();
    }
  }
}
