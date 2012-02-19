package edu.caltech.cs141b.hw2.gwt.collab.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.caltech.cs141b.hw2.gwt.collab.shared.LockUnavailable;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;

/**
 * Used in conjunction with <code>CollaboratorService.lockDocument()</code>.
 */
public class DocRequestor implements AsyncCallback<Integer> {
	
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
	public void onSuccess(Integer queueSize) {
		if (queueSize > 0) {
			collaborator.statusUpdate("There are " + queueSize + " people ahead of" +
					" you waiting to lock the document.");
		}
		
		// TODO: Update data structures + status panel. 
	}
	
	/**
	 * Generalized so that it can be used elsewhere.  In particular, when
	 * creating a new document, a locked document is simulated by calling this
	 * function with a new LockedDocument object without the lock primitives.
	 * 
	 * @param result
	 */
	protected void gotDoc(LockedDocument result) {
		collaborator.updateVarsAndUi(result.getKey(),
				result.getTitle(),
				result.getContents(),
				UiState.LOCKED);
	}
	
}

