package edu.caltech.cs141b.hw2.gwt.collab.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.caltech.cs141b.hw2.gwt.collab.shared.DocumentMetadata;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;
import edu.caltech.cs141b.hw2.gwt.collab.shared.UnlockedDocument;

/**
 * The async counterpart of <code>CollaboratorService</code>.
 */
public interface CollaboratorServiceAsync {

	void setUpChannel(AsyncCallback<String> callback);
	
	void getDocumentList(AsyncCallback<List<DocumentMetadata>> callback);

	void requestDocument(String documentKey, String token,
			AsyncCallback<Integer> callback);
	
	void unrequestDocument(String documentKey, String token,
			AsyncCallback<Void> callback);
	
	void lockDocument(String documentKey, String token,
			AsyncCallback<LockedDocument> callback);

	void getDocument(String documentKey,
			AsyncCallback<UnlockedDocument> callback);

	void saveDocument(LockedDocument doc, String token,
			AsyncCallback<UnlockedDocument> callback);

	void releaseLock(LockedDocument doc, String token,
			AsyncCallback<Void> callback);

}

