package edu.caltech.cs141b.hw2.gwt.collab.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.caltech.cs141b.hw2.gwt.collab.shared.LockExpired;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;

public class DocReleaser implements AsyncCallback<String> {
	
	private Collaborator collaborator;
	
	public DocReleaser(Collaborator collaborator) {
		this.collaborator = collaborator;
	}
	
	public void releaseLock(LockedDocument lockedDoc) {
		collaborator.statusUpdate("Releasing lock on '" + 
				lockedDoc.getTitle() + "'.");
		collaborator.updateVarsAndUi(lockedDoc.getKey(), UiState.RELEASING);
		collaborator.collabService.releaseLock(
				lockedDoc, collaborator.channelToken, this);
	}

	@Override
	public void onFailure(Throwable caught) {
		if (caught instanceof LockExpired) {
			LockExpired caughtEx = (LockExpired) caught;
			collaborator.statusUpdate("Lock had already expired; release failed.");
			collaborator.updateVarsAndUi(caughtEx.getKey(), UiState.VIEWING);
		} else {
			collaborator.statusUpdate("Error releasing document"
					+ "; caught exception " + caught.getClass()
					+ " with message: " + caught.getMessage());
			GWT.log("Error releasing document.", caught);
		}
	}

	@Override
	public void onSuccess(String docKey) {
		collaborator.statusUpdate("Document lock released.");
		collaborator.updateVarsAndUi(docKey, UiState.VIEWING);
		collaborator.reader.getDocument(docKey);
	}
	
}

