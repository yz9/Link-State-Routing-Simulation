package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
    private RouterDescription rd;
    private ServerSocket server;
    private Link[] ports;

    public Server(RouterDescription rd, Link[] ports){
      this.rd = rd;
      this.ports = ports;
    }

    public void run(){
      System.out.println("server side" );
      try {
        server = new ServerSocket(this.rd.processPortNumber);
      while (true) {
        Socket connection = server.accept();
        //System.out.println("Just connected to " + connection.getRemoteSocketAddress());
        Thread client = new Thread(new Client(connection, this.rd, this.ports));
        client.start();
        //client.connect();
      }
    } catch (IOException e) {
       e.printStackTrace();
    }
  }
}
