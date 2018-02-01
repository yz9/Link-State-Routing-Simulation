package socs.network.node;

import socs.network.message.Packet;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Router {

  protected LinkStateDatabase lsd;

  RouterDescription rd = new RouterDescription();

  // assuming that all routers are with 4 ports
  Link[] ports = new Link[4];

  public Router(Configuration config) {
    rd.simulatedIPAddress = config.getString("socs.network.router.ip");
    rd.processPortNumber = config.getShort("socs.network.router.port");

    try {
      rd.processIPAddress = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      System.out.println("No host ip...");
    }
    System.out.println("Process IP - " + rd.processIPAddress);
    System.out.println("Port Number - " + rd.processPortNumber);
    System.out.println("Simulated IP - " + rd.simulatedIPAddress);
    lsd = new LinkStateDatabase(rd);

    // now create the server socket
    Thread server = new Thread(new Server(rd, ports));
    server.start();
  }

  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address -> ip address -> ... -> destination ip
   *
   * @param destinationIP
   *      the ip adderss of the destination simulated router
   */
  private void processDetect(String destinationIP) {

  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber
   *      the port number which the link attaches at
   */
  private void processDisconnect(short portNumber) {

  }

  // return -1 if not found else return the available index.
  private int findAvailablePort() {
    for (int i = 0; i < 4; i++) {
      if (ports[i] == null) {
        return i;
      }
    }
    return -1;
  }

  private boolean containsIP(String simulatedIP) {
    for (int i = 0; i < 4; i++) {
      if (ports[i] != null) {
        if (ports[i].router2.simulatedIPAddress.equals(simulatedIP)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * attach the link to the remote router, which is identified by the given
   * simulated ip; to establish the connection via socket, you need to indentify
   * the process IP and process Port; additionally, weight is the cost to
   * transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, short processPort, String simulatedIP, short weight) {
    // check if there exists at least one available ports
    int linkIndex = findAvailablePort();
    // case 1: all ports are used
    if (linkIndex == -1) {
      System.err.println("Error: All ports has been used.");
      return;
    }

    // case 2: if router is trying to connect to itself
    if (simulatedIP.equals(rd.simulatedIPAddress)) {
      System.err.println("Error: Router cannot attach to itself");
      return;
    }

    // case 3: check if the requested router has already connected with our router.
    if (containsIP(simulatedIP)) {
      System.err.println("Error: Router has already connected with requested router");
      return;
    }

    RouterDescription remoteRouter = new RouterDescription(processIP, processPort, simulatedIP);
    // attach the link to the remote router
    try {
    	// create
      Socket remoteSocket = new Socket(processIP, processPort);
      ObjectOutputStream output = new ObjectOutputStream(remoteSocket.getOutputStream());
      ObjectInputStream input = new ObjectInputStream(remoteSocket.getInputStream());
      Packet linkRequest = Packet.AttachLinkRequest(this.rd.simulatedIPAddress, simulatedIP, (short) 0);
      output.writeObject(linkRequest);

      ports[linkIndex] = new Link(rd, remoteRouter);
      System.out.println("--- attached with " + linkRequest.dstIP + " ---");
      input.close();
      output.close();
      remoteSocket.close();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * broadcast Hello to neighbors
   */
  private void processStart() {

    if (ports[0] == null){
      System.err.println("You haven't attached to any other router yet.");
      return;
    }

    for (int i = 0; i < 4; i++) {
      if (ports[i] != null) {
        try {
          Socket client = new Socket(ports[i].router2.processIPAddress, ports[i].router2.processPortNumber);
          ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
          ObjectInputStream in = new ObjectInputStream(client.getInputStream());
          Packet packet = new Packet(this.rd.simulatedIPAddress, ports[i].router2.simulatedIPAddress, (short) 0);
          packet.srcProcessIP = this.rd.processIPAddress;
          packet.srcProcessPort = this.rd.processPortNumber;
          // send hello packet to clienthandler
          out.writeObject(packet);

          // note that a router can only start with the same router once.
          if(ports[i].router2.status != RouterStatus.TWO_WAY){
            Packet recv = (Packet) in.readObject();

            if (recv == null) {
              System.err.println("missing packet");
              break;
            }

            // check if received packet has sospfType "hello"
            if (recv.sospfType == (short) 0 ) {
              System.out.println("received HELLO from " + recv.srcIP + ";");
              ports[i].router2.status = RouterStatus.TWO_WAY;
              System.out.println("set " +  ports[i].router2.simulatedIPAddress + " state to TWO_WAY");
              out.writeObject(packet);
            }
          }
          else{
            System.err.println("Already connected with router " + ports[i].router2.simulatedIPAddress);
          }

           // clean up
          out.close();
          in.close();
          client.close();
        } catch (UnknownHostException e) {
          System.err.println("Trying to connect to unknown host: " + e);
        } catch (IOException e) {
          e.printStackTrace();
        } catch (ClassNotFoundException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * attach the link to the remote router, which is identified by the given
   * simulated ip; to establish the connection via socket, you need to indentify
   * the process IP and process Port; additionally, weight is the cost to
   * transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
   */
  private void processConnect(String processIP, short processPort, String simulatedIP, short weight) {

  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors() {
    for (int i = 0; i < 4; i++) {
      if (ports[i] != null && ports[i].router2.status == RouterStatus.TWO_WAY) {
        System.out.println("IP address of neighbor " + (i + 1) + ": " + ports[i].router2.simulatedIPAddress);
      }
    }
  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {

  }

  public void terminal() {
    try {
      InputStreamReader isReader = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(isReader);
      System.out.print(">> ");
      String command = br.readLine();
      while (true) {
        if (command.startsWith("detect ")) {
          String[] cmdLine = command.split(" ");
          processDetect(cmdLine[1]);
        } else if (command.startsWith("disconnect ")) {
          String[] cmdLine = command.split(" ");
          processDisconnect(Short.parseShort(cmdLine[1]));
        } else if (command.startsWith("quit")) {
          processQuit();
        } else if (command.startsWith("attach ")) {
          String[] cmdLine = command.split(" ");
          processAttach(cmdLine[1], Short.parseShort(cmdLine[2]), cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("start")) {
          processStart();
        } else if (command.equals("connect ")) {
          String[] cmdLine = command.split(" ");
          processConnect(cmdLine[1], Short.parseShort(cmdLine[2]), cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("neighbors")) {
          // output neighbors
          processNeighbors();
        } else {
          // invalid command
          break;
        }
        System.out.print(">> ");
        command = br.readLine();
      }
      isReader.close();
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
