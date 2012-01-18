
public class Main {

	public static final int NUM_CLIENTS = 2;
	
	public static void main(String[] args) {
		// Start server
		Server server = new Server(NUM_CLIENTS);
		server.start();
	}
}
