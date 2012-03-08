package edu.caltech.cs141b.hw2.gwt.collab.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.caltech.cs141b.hw2.gwt.collab.shared.LockExpired;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;

/**
 * DocReaderLocked is a descendant of DocLocker; the difference here is that the
 * in HW4, the "lock" method is called only on the server side, and when it's a
 * client's turn to 
 */
public class DocReaderLocked implements AsyncCallback<LockedDocument> {
	
	private Collaborator collaborator;
	
	public DocReaderLocked(Collaborator collaborator) {
		this.collaborator = collaborator;
	}
	
	public void lockDocument(String key) {
		collaborator.statusUpdate("Fetching locked document.");
		collaborator.updateVarsAndUi(key, UiState.REQUESTING);
		collaborator.collabService.getLockedDocument(key, collaborator.channelToken, this);
	}

	@Override
	public void onFailure(Throwable caught) {
		if (caught instanceof LockExpired) {
			LockExpired caughtEx = (LockExpired) caught;
			collaborator.statusUpdate("Lock had already expired; release failed.");
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

