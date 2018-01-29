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
     *            the ip adderss of the destination simulated router
     */
    private void processDetect(String destinationIP) {

    }

    /**
     * disconnect with the router identified by the given destination ip address
     * Notice: this command should trigger the synchronization of database
     *
     * @param portNumber
     *            the port number which the link attaches at
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
            System.err.println("error: no available port");
            return;
        }

        // case 2: if router is trying to connect to itself
        if (simulatedIP.equals(rd.simulatedIPAddress)) {
            System.err.println("error");
            return;
        }

        // case 3: check if the requested router has already connected with our router.
        if (containsIP(simulatedIP)) {
            System.err.println("error");
            return;
        }

        RouterDescription remoteRouter = new RouterDescription();
        remoteRouter.processIPAddress = processIP;
        remoteRouter.processPortNumber = processPort;
        remoteRouter.simulatedIPAddress = simulatedIP;

        // attach the link to the remote router
        // TODO socket stuff
        try {
        	// create 
            Socket remoteSocket = new Socket(processIP, processPort);
            ObjectOutputStream output = new ObjectOutputStream(remoteSocket.getOutputStream());
            ObjectInputStream input = new ObjectInputStream(remoteSocket.getInputStream());
            Packet linkeRequest = Packet.AttachLinkRequest(this.rd.simulatedIPAddress, simulatedIP, (short) 0);
            output.writeObject(linkeRequest);
            try {
                String message = (String) input.readObject();
                if (message.equals("Success")) {
                    // if all goes well, assign the new router link to the available port
                    ports[linkIndex] = new Link(rd, remoteRouter);
                    // close the stream and socket
                    System.out.println("--- attached with " + linkeRequest.dstIP + " ---");
                    input.close();
                    output.close();
                    remoteSocket.close();
                } else {
                	System.out.println("error");
                }
            } catch (ClassNotFoundException e) {
                System.err.println(e);
            } catch (NullPointerException e) {
                System.err.println(e);
            }
            
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
        // TODO test
        for (int i = 0; i < 4; i++) {
            if (ports[i] != null) {
                try {
                    // TODO test
                    Socket client = new Socket(ports[i].router2.processIPAddress, ports[i].router2.processPortNumber);
                    ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(client.getInputStream());

                    Packet packet = new Packet(rd.simulatedIPAddress, ports[i].router2.simulatedIPAddress, (short) 0);
                    out.writeObject(packet);
                    System.out.println("--- first msg ---");
                    Packet recv = (Packet) in .readObject();

                    if (recv == null) {
                        System.err.println("missing packet");
                        break;
                    }

                    if (recv.sospfType != 0) {
                        System.err.println("wrong packet type");
                        break;
                    } else {
                        System.out.println("received HELLO from " + recv.srcIP + ";");
                        ports[i].router1.status = RouterStatus.TWO_WAY;
                        System.out.println("set " + recv.srcIP + " state to TWO_WAY");
                        packet = new Packet(rd.simulatedIPAddress, ports[i].router2.simulatedIPAddress, (short) 0);
                        out.writeObject(packet);
                        System.out.println("--- second msg ---");
                    }
                    // clean up
                    out.close();
                    in.close();
                    client.close();

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
            if (ports[i] != null) {
                System.out.println("IP address of neighbor- " + (i + 1) + ": " + ports[i].router2.simulatedIPAddress);
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
