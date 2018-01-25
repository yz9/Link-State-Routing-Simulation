package tutorial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class GreetingClient {
	public static void main(String[] args) {
		String serverName = "localhost";
		int port = 8887;
		
		try {
			System.out.println("connecting to " + serverName + "on port " + port);
			Socket client = new Socket(serverName, port);
			
			System.out.println("Just Connected to " + client.getRemoteSocketAddress());
			OutputStream outToServer = client.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			out.writeUTF("Hello from " + client.getLocalSocketAddress());
			
			InputStream inFromServer = client.getInputStream();
			DataInputStream in = new DataInputStream(inFromServer);
			
			System.out.println("Server response: " + in.readUTF());
			client.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
