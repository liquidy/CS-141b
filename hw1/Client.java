import java.util.concurrent.ArrayBlockingQueue;

public class Client extends Thread {

	public static final int NUM_ITERATIONS = 100;

	// Sleep between 195 - 205 ms
	public static final int THINKING_RANGE_START = 195;
	public static final int THINKING_RANGE_END = 205;

	// Sleep between 15 - 25 ms
	public static final int EATING_RANGE_START = 15;
	public static final int EATING_RANGE_END = 25;

	public ArrayBlockingQueue<Message> inputQueue;     // Holds token for client
	public int senderID;
	public long[] thinkingTimes;
	public long[] hungryTimes;
	public long[] eatingTimes;

	private ArrayBlockingQueue<Message> requestQueue;   // Belongs to server
	private ArrayBlockingQueue<Message> tokenQueue;     // Belongs to server


	public Client(ArrayBlockingQueue<Message> requestQueue,
	              ArrayBlockingQueue<Message> tokenQueue,
	              int senderID) {

		inputQueue = new ArrayBlockingQueue<Message>(1, true);
		this.requestQueue = requestQueue;
		this.tokenQueue = tokenQueue;
		this.senderID = senderID;
		this.thinkingTimes = new long[NUM_ITERATIONS];
		this.hungryTimes = new long[NUM_ITERATIONS];
		this.eatingTimes = new long[NUM_ITERATIONS];
	}

	public void run() {
		for (int i = 0; i < NUM_ITERATIONS; i++) {
			System.out.println("Client " + senderID + ": iteration " + i);

			// Thinking stage
			int sleepRange = THINKING_RANGE_END - THINKING_RANGE_START + 1;
			long waitingTime = 
			        (long) (THINKING_RANGE_START + sleepRange * Math.random());
			System.out.println("Client " + senderID + ": wait " + waitingTime);
			thinkingTimes[i] = waitingTime;
			try {
				sleep(waitingTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Hungry stage
			System.out.println("Client " + senderID + ": hungry for token ");
			long currentTime = System.currentTimeMillis();
			requestQueue.add(Message.createRequestMessage(this));
			Message token = null;
			try {
				token = inputQueue.take();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			hungryTimes[i] = System.currentTimeMillis() - currentTime;
			
			// Eating stage
			int eatRange = EATING_RANGE_END - EATING_RANGE_START + 1;
			waitingTime = (long) (EATING_RANGE_START + eatRange * Math.random());
			System.out.println("Client " + senderID + ": eating for " + waitingTime);
			eatingTimes[i] = waitingTime;
			try {
				sleep(waitingTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			tokenQueue.add(token);
		}

		// Thread is done, so send out terminate message and finish.
		System.out.println("Client " + senderID + ": terminate");
		try {
			requestQueue.put(Message.createTerminationMessage());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
