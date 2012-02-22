package edu.caltech.cs141b.hw2.gwt.collab.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class DocUnrequestor implements AsyncCallback<String> {
	
	private Collaborator collaborator;
	
	public DocUnrequestor(Collaborator collaborator) {
		this.collaborator = collaborator;
	}
	
	public void unrequestDocument(String docKey) {
		collaborator.statusUpdate("Unrequesting document: " + docKey);
		collaborator.updateVarsAndUi(docKey, UiState.LOCKING);
		collaborator.collabService.unrequestDocument(
				docKey, collaborator.channelToken, this);
	}

	@Override
	public void onFailure(Throwable caught) {
		collaborator.statusUpdate("Error leaving queue.");
		GWT.log("Error leaving queue.", caught);
		// TODO: Implement a general Exception class for this project so that
		// all exceptions coming back will have the documentKey in them.
	}

	@Override
	public void onSuccess(String docKey) {
		collaborator.statusUpdate("Left queue for document.");
		collaborator.updateVarsAndUi(docKey, UiState.VIEWING);
	}
}

