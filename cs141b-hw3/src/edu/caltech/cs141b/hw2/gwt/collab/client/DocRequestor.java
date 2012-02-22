package edu.caltech.cs141b.hw2.gwt.collab.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.caltech.cs141b.hw2.gwt.collab.shared.DocRequestorResult;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockUnavailable;

public class DocRequestor implements AsyncCallback<DocRequestorResult> {
	
	private Collaborator collaborator;
	
	public DocRequestor(Collaborator collaborator) {
		this.collaborator = collaborator;
	}
	
	public void requestDocument(String key) {
		collaborator.statusUpdate("Requesting document: " + key);
		collaborator.updateVarsAndUi(key, UiState.REQUESTING);
		collaborator.collabService.requestDocument(
				key, collaborator.channelToken, this);
	}

	@Override
	public void onFailure(Throwable caught) {
		if (caught instanceof LockUnavailable) {
			LockUnavailable caughtEx = ((LockUnavailable) caught);
			if (caughtEx.getWrongCredentials()) {
				collaborator.statusUpdate("Lock is available, but you have the" +
						"wrong credentials; save failed. It's locked by: " +
						caughtEx.getCredentials());
			} else {
				collaborator.statusUpdate("Lock is unavailable; save failed. " +
						"It's locked until " + caughtEx.getLockedUntil());
			}
			collaborator.updateVarsAndUi(caughtEx.getKey(), UiState.VIEWING);
		} else {
			collaborator.statusUpdate("Error retrieving lock"
					+ "; caught exception " + caught.getClass()
					+ " with message: " + caught.getMessage());
			GWT.log("Error getting document lock.", caught);
		}
	}

	@Override
	public void onSuccess(DocRequestorResult result) {
		int numPeopleLeft = result.getNumPeopleLeft();
		if (numPeopleLeft > 0) {
			collaborator.statusUpdate("There are " + numPeopleLeft + " people ahead of" +
					" you waiting to lock the document.");
		}
		
		// Update data structures + queue status panel.
		String docKey = result.getDocKey();
		int indOfDoc = collaborator.tabKeys.indexOf(docKey);
		collaborator.tabQueueLengths.set(indOfDoc, numPeopleLeft);
		if (collaborator.tabIsSelected() && indOfDoc != -1) {
			collaborator.tabQueueLengths.set(indOfDoc, numPeopleLeft);
			collaborator.queueStatus.setHTML("<br />Position " +
					numPeopleLeft + " in line");
		}
	}
}
