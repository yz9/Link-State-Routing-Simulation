package socs.network.node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
	private RouterDescription rd;
	private ServerSocket socket;
	private Link[] ports;
	private LinkStateDatabase lsd;

	public Server(RouterDescription rd, Link[] ports, LinkStateDatabase lsd) {
		this.rd = rd;
		this.ports = ports;
		this.lsd = lsd;
	}

	public void run() {
		// System.out.println("server side");
		try {
			socket = new ServerSocket(this.rd.processPortNumber);
			while (true) {
				Socket connection = socket.accept();
				// System.out.println("Just connected to " +
				// connection.getRemoteSocketAddress());
				Thread client = new Thread(new TaskManager(connection, this.rd, this.ports, lsd));
				client.start();
				// client.connect();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
