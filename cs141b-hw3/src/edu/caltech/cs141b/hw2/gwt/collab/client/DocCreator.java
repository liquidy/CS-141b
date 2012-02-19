package edu.caltech.cs141b.hw2.gwt.collab.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.TextBox;

import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;
import edu.caltech.cs141b.hw2.gwt.collab.shared.UnlockedDocument;

public class DocCreator implements AsyncCallback<UnlockedDocument> {
	
	private Collaborator collaborator;
	
	public DocCreator(Collaborator collaborator) {
		this.collaborator = collaborator;
	}
	
	public void createDocument() {
		collaborator.statusUpdate("Attemping to create a new document.");
		collaborator.createNew.setEnabled(false);
		LockedDocument lockedDoc = new LockedDocument(null, null, null, 
				"Untitled document", "");
		collaborator.collabService.saveDocument(
				lockedDoc, collaborator.channelToken, this);
	}

	@Override
	public void onFailure(Throwable caught) {
		collaborator.statusUpdate("Error saving document"
				+ "; caught exception " + caught.getClass()
				+ " with message: " + caught.getMessage());
		GWT.log("Error saving document.", caught);
		
		collaborator.createNew.setEnabled(true);
	}

	@Override
	public void onSuccess(UnlockedDocument result) {
		collaborator.statusUpdate("Document '" + result.getTitle()
				+ "' successfully created.");
		
		// Add tab to UI.
		RichTextArea contents = new RichTextArea();
		contents.setHTML(result.getContents());
		TextBox title = new TextBox();
		title.setText(result.getTitle());
		collaborator.addNewTab(result.getKey(), contents, title);
		
		// Refresh list in case title was changed.
		collaborator.lister.getDocumentList();
		
		collaborator.createNew.setEnabled(true);
	}
}
