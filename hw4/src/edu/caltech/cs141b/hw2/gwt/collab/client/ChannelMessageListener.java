package edu.caltech.cs141b.hw2.gwt.collab.client;

import com.google.gwt.appengine.channel.client.SocketError;
import com.google.gwt.appengine.channel.client.SocketListener;

import edu.caltech.cs141b.hw2.gwt.collab.server.CollaboratorServer;
import edu.caltech.cs141b.hw2.gwt.collab.shared.Messages;

class ChannelMessageListener implements SocketListener {

	private Collaborator collab;

	public ChannelMessageListener(Collaborator collab) {
		this.collab = collab;
	}

	@Override
	public void onOpen() {
		collab.statusUpdate("Channel successfully opened!");
		collab.channelIsSetup = true;
	}

	@Override
	public void onMessage(String message) {
		// Channel adds whitespace at the end, so strip message before processing.
		String strippedMessage = message.replaceAll("\\s", "");
		
		switch (strippedMessage.charAt(0)) {
		case Messages.LOCK_READY:
			handleLockReady(strippedMessage);
			break;

		case Messages.LOCK_NOT_READY:
			handleLockNotReady(strippedMessage);
			break;

		case Messages.LOCK_EXPIRED:
			handleLockExpired(strippedMessage);
			break;

		case Messages.DOC_SAVED:
			handleDocSaved(strippedMessage);
			break;

		default:
			handleUnrecognizedMessage(strippedMessage);
		}
	}

	@Override
	public void onError(SocketError error) {
		collab.statusUpdate("Channel error:" + error.getDescription());
	}

	@Override
	public void onClose() {
		collab.statusUpdate("Channel closed!");
	}

	private void handleLockReady(String message) {
		// Doc is locked. The rest of the string is doc ID.
		String docKey = message.substring(1);
		int indOfDoc = collab.tabKeys.indexOf(docKey);
		if (collab.tabIsSelected() && indOfDoc != -1) {
			collab.statusUpdate("Update: " + docKey + "'s lock was acquired.");
			collab.tabQueueLengths.set(indOfDoc, 0);
			collab.queueStatus.setHTML("<br />Position 0 in line");
			// Because the doc is locked, go ahead and just fetch the document.
			collab.lockedReader.lockDocument(docKey);
		}
	}

	private void handleLockNotReady(String message) {
		// Doc is not ready to be locked. The rest of the string is
		// the number of people in front of us in the queue.
		String restOfString = message.substring(1);
		int delimiter = restOfString.indexOf(
				CollaboratorServer.DELIMITER);
		int numPeopleLeft = Integer.parseInt(
				restOfString.substring(0, delimiter));
		String docId = restOfString.substring(delimiter + 1);
		int indOfDoc = collab.tabKeys.indexOf(docId);
		if (collab.tabIsSelected() && indOfDoc != -1) {
			collab.statusUpdate("Update: " + numPeopleLeft + " people are now" +
					" ahead of you for document " + docId + ".");
			collab.tabQueueLengths.set(indOfDoc, numPeopleLeft);
			collab.queueStatus.setHTML("<br />Position " +
					numPeopleLeft + " in line");
		}
	}

	private void handleLockExpired(String message) {
		collab.statusUpdate("Timeout occurred: document lock released.");
		String docKey = message.substring(1);
		collab.queueStatus.setHTML("<br />No lock");
		collab.updateVarsAndUi(docKey, UiState.VIEWING);
		
		if (collab.simulating) {
			collab.simulateThinking();
		}
	}

	private void handleDocSaved(String message) {
		String docKey = message.substring(1);
		collab.statusUpdate("Document '" + docKey
				+ "' successfully saved.");
		// Refresh list and document in case title or contents was changed.
		collab.lister.getDocumentList();
		collab.reader.getDocument(docKey);
		
		if (collab.simulating) {
			collab.simulateThinking();
		}
	}

	private void handleUnrecognizedMessage(String message) {
		collab.statusUpdate("Message type not recognized for message: " + message);
	}

}
