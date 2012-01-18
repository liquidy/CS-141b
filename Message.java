public class Message {
	
	public enum MessageType {
		REQUEST, TOKEN, TERMINATE
	}
	
	public MessageType type;
	public Client sender;                // Only used for request-type messages
	
	public Message(MessageType type) {
		this.type = type;
	}
	
	public void setSender(Client sender) {
		this.sender = sender;
	}
	
	public String toString() {
		return type + ", sender: " + sender;
	}
}
