import java.util.concurrent.ArrayBlockingQueue;

public class Server extends Thread {
	
	public static final boolean PRINT_CSV_VALUES = true;
	
	private ArrayBlockingQueue<Message> requestQueue;
	private ArrayBlockingQueue<Message> tokenQueue;
	private Client[] clients;
	private int numLiveClients;
	
	public Server(int numLiveClients) {
		requestQueue = new ArrayBlockingQueue<Message>(numLiveClients, true);
		tokenQueue = new ArrayBlockingQueue<Message>(1, true);
		clients = new Client[numLiveClients];
		this.numLiveClients = numLiveClients;
		
		// Putting token in tokenQueue
		tokenQueue.add(new Message(Message.MessageType.TOKEN));
	}
	
	public void run() {
		// Start clients
		for (int i = 0; i < numLiveClients; i++) {
			System.out.println("Starting client " + i);
			Client client = new Client(requestQueue, tokenQueue, i);
			clients[i] = client;
			client.start();
		}
		
		// 
		while (numLiveClients > 0) {
			// Wait for token
			Message token = null;
			try {
				token = tokenQueue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Wait for request 
			Message request = null;
			try {
				request = requestQueue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Process request
			switch (request.type) {
				case REQUEST:
					request.sender.inputQueue.add(token);
					break;
			
				case TERMINATE:
					numLiveClients--;
					// Put token back, since termination does not require moving token
					try {
						tokenQueue.put(token);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					break;
					
				default:
					System.out.println("Null request found!");
					// Put token back, since termination does not require moving token
					try {
						tokenQueue.put(token);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					break;
			}
		}
		
		// All clients have died. Print out some stats before server dies.
		System.out.println("All clients have terminated. Done!");
		System.out.println("--- Printing out stats ---");
		for (Client client : clients) {
			long avgThinkingTime = 0;
			long avgHungryTime = 0;
			long avgEatingTime = 0;
			for (int i = 0; i < Client.NUM_ITERATIONS; i++) {
				avgThinkingTime += client.thinkingTimes[i];
				avgHungryTime += client.hungryTimes[i];
				avgEatingTime += client.eatingTimes[i];
			}
			avgThinkingTime /= Client.NUM_ITERATIONS;
			avgHungryTime /= Client.NUM_ITERATIONS;
			avgEatingTime /= Client.NUM_ITERATIONS;
			System.out.println("Client " + client.senderID + 
					"'s avg thinking time: " + avgThinkingTime + " ms");
			System.out.println("Client " + client.senderID + 
					"'s avg hungry time: " + avgHungryTime + " ms");
			System.out.println("Client " + client.senderID + 
					"'s avg eating time: " + avgEatingTime + " ms");
		}
		
		if (PRINT_CSV_VALUES) {
			System.out.println("--- CSV values start here ---");
			System.out.println("Thinking, Hungry, Eating");
			for (Client client : clients) {
				long avgThinkingTime = 0;
				long avgHungryTime = 0;
				long avgEatingTime = 0;
				for (int i = 0; i < Client.NUM_ITERATIONS; i++) {
					avgThinkingTime += client.thinkingTimes[i];
					avgHungryTime += client.hungryTimes[i];
					avgEatingTime += client.eatingTimes[i];
				}
				avgThinkingTime /= Client.NUM_ITERATIONS;
				avgHungryTime /= Client.NUM_ITERATIONS;
				avgEatingTime /= Client.NUM_ITERATIONS;
				System.out.println(avgThinkingTime + "," + avgHungryTime + "," + 
						avgEatingTime);
			}
		}
	}
}
