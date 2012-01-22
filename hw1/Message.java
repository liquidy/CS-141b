public class Message {
	
	public enum MessageType {
		REQUEST, TOKEN, TERMINATE
	}
	
	public MessageType type;
	private Client sender;                // Only used for request-type messages
	
	private Message(MessageType type) {
		this.type = type;
	}
	
	public static Message createRequestMessage(Client sender) {
		Message m = new Message(MessageType.REQUEST);
		m.sender = sender;
		return m;
	}
	
	public static Message createToken() {
		return new Message(MessageType.TOKEN);
	}
	
	public static Message createTerminationMessage() {
		return new Message(MessageType.TERMINATE);
	}
	
	public Client getSender() {
		return sender;
	}
	
	public String toString() {
		return type + ", sender: " + sender;
	}
}
