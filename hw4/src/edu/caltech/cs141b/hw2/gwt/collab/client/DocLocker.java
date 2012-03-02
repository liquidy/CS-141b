package edu.caltech.cs141b.hw2.gwt.collab.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.caltech.cs141b.hw2.gwt.collab.shared.LockUnavailable;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;

public class DocLocker implements AsyncCallback<LockedDocument> {
	
	private Collaborator collaborator;
	
	public DocLocker(Collaborator collaborator) {
		this.collaborator = collaborator;
	}
	
	public void lockDocument(String key) {
		collaborator.statusUpdate("Attempting to lock document.");
		collaborator.updateVarsAndUi(key, UiState.REQUESTING);
		collaborator.collabService.lockDocument(key, collaborator.channelToken, this);
	}

	@Override
	public void onFailure(Throwable caught) {
		if (caught instanceof LockUnavailable) {
			LockUnavailable caughtEx = ((LockUnavailable) caught);
			collaborator.statusUpdate("Lock is unavailable. Are you at the front " +
					"of the queue: " + caughtEx.frontOfQueue() +
					". Locked until: " + caughtEx.getLockedUntil() +
					" by " + caughtEx.getLockedBy() + ".");
			collaborator.updateVarsAndUi(caughtEx.getKey(), UiState.VIEWING);
		} else {
			collaborator.statusUpdate("Error retrieving lock"
					+ "; caught exception " + caught.getClass()
					+ " with message: " + caught.getMessage());
			GWT.log("Error getting document lock.", caught);
		}
	}

	@Override
	public void onSuccess(LockedDocument result) {
		collaborator.statusUpdate("Lock retrieved for document.");
		collaborator.updateVarsAndUi(result.getKey(),
				result.getTitle(),
				result.getContents(),
				UiState.LOCKED);
		
		if (collaborator.simulating) {
			collaborator.simulateEating();
		}
	}
}
