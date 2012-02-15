package edu.caltech.cs141b.hw2.gwt.collab.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.caltech.cs141b.hw2.gwt.collab.shared.UnlockedDocument;

/**
 * Used in conjunction with <code>CollaboratorService.getDocument()</code>.
 */
public class DocReader implements AsyncCallback<UnlockedDocument> {
	
	private Collaborator collaborator;
	
	public DocReader(Collaborator collaborator) {
		this.collaborator = collaborator;
	}
	
	public void getDocument(String key) {
		collaborator.statusUpdate("Fetching document " + key + ".");
		collaborator.collabService.getDocument(key, this);
	}

	@Override
	public void onFailure(Throwable caught) {
		collaborator.statusUpdate("Error retrieving document"
				+ "; caught exception " + caught.getClass()
				+ " with message: " + caught.getMessage());
		GWT.log("Error getting document lock.", caught);
	}

	@Override
	public void onSuccess(UnlockedDocument result) {
		collaborator.statusUpdate("Document fetched successfully.");
		collaborator.updateVarsAndUi(result.getKey(),
				result.getTitle(),
				result.getContents(),
				UiState.VIEWING);
	}
}

