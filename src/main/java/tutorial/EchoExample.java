package tutorial;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoExample {
	public static void main(String args[]) {
		ServerSocket echoServer = null;
		String line;
		DataInputStream inputStream;
		PrintStream os;
		Socket clientSocket = null;
		
		// create the server
		try {
			echoServer = new ServerSocket(9999);
		} catch (Exception e) {
			System.out.println(e);
		}
		
		try {
			clientSocket = echoServer.accept();
		}
		
		
	}
	
	
}
