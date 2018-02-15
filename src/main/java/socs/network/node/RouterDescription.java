package socs.network.node;

public class RouterDescription {
	// used to socket communication
	String processIPAddress;
	short processPortNumber;
	// used to identify the router in the simulated network space
	String simulatedIPAddress;
	// status of the router
	RouterStatus status;

	public RouterDescription() {
	}

	public RouterDescription(String processIPAddress, short processPortNumber, String simulatedIPAddress) {
		this.processIPAddress = processIPAddress;
		this.processPortNumber = processPortNumber;
		this.simulatedIPAddress = simulatedIPAddress;
	}
}
