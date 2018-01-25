package tutorial;

import java.net.*;
import java.io.*;

public class GreetingServer extends Thread {
	private ServerSocket serverSocket;

	public GreetingServer(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		serverSocket.setSoTimeout(10000);
	}

	@Override
	public void run() {
		while (true) {
			System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");
			try {
				Socket server = serverSocket.accept();

				System.out.println("Just connected to " + server.getRemoteSocketAddress());
				DataInputStream in = new DataInputStream(server.getInputStream());

				System.out.println(in.readUTF());

				DataOutputStream out = new DataOutputStream(server.getOutputStream());
				out.writeUTF("Thank you bye");

				server.close();

			} catch (Exception e) {
				break;
			}
		}

	}

	public static void main(String[] args) {
		int port = 8887;
		try {
			Thread t = new GreetingServer(port);
			t.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
