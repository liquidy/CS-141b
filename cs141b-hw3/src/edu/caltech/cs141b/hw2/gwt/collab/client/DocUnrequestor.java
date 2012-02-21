package edu.caltech.cs141b.hw2.gwt.collab.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class DocUnrequestor implements AsyncCallback<Void> {
	
	private Collaborator collaborator;
	
	public DocUnrequestor(Collaborator collaborator) {
		this.collaborator = collaborator;
	}
	
	public void unrequestDocument(String key) {
		collaborator.statusUpdate("Unrequesting document: " + key);
		collaborator.collabService.unrequestDocument(
				key, collaborator.channelToken, this);
	}

	@Override
	public void onFailure(Throwable caught) {
		collaborator.statusUpdate("Error leaving queue.");
		GWT.log("Error leaving queue.", caught);
	}

	@Override
	public void onSuccess(Void result) {
		collaborator.statusUpdate("Left queue for document.");
	}
}

