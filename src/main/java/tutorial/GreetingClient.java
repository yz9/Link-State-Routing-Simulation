package tutorial;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class GreetingClient extends Thread{
	
    @Override
    public void run() {
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
	
	public static void main(String[] args) {
		for(int i = 0; i < 10; i++) {
			new GreetingClient().start();
		}
	}
}
