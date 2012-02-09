package edu.caltech.cs141b.hw2.gwt.collab.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.caltech.cs141b.hw2.gwt.collab.shared.LockUnavailable;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;

/**
 * Used in conjunction with <code>CollaboratorService.lockDocument()</code>.
 */
public class DocLocker implements AsyncCallback<LockedDocument> {
	
	private Collaborator collaborator;
	
	public DocLocker(Collaborator collaborator) {
		this.collaborator = collaborator;
	}
	
	public void lockDocument(String key) {
		collaborator.statusUpdate("Attempting to lock document.");
		collaborator.updateVarsAndUi(key, UiState.LOCKING);
		collaborator.collabService.lockDocument(key, this);
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
	public void onSuccess(LockedDocument result) {
		collaborator.statusUpdate("Lock retrieved for document.");
		gotDoc(result);
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

