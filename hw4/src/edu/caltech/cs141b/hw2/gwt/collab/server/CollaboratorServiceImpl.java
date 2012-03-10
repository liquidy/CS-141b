package edu.caltech.cs141b.hw2.gwt.collab.server;

import java.util.List;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import edu.caltech.cs141b.hw2.gwt.collab.client.CollaboratorService;
import edu.caltech.cs141b.hw2.gwt.collab.shared.DocRequestorResult;
import edu.caltech.cs141b.hw2.gwt.collab.shared.DocumentMetadata;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockExpired;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;
import edu.caltech.cs141b.hw2.gwt.collab.shared.UnlockedDocument;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class CollaboratorServiceImpl extends RemoteServiceServlet implements CollaboratorService {

	@Override
	public String setUpChannel() {
		return CollaboratorServer.setUpChannel();
	}

	@Override
	public List<DocumentMetadata> getDocumentList() {
		return CollaboratorServer.getDocumentList();
	}

	@Override
	public DocRequestorResult requestDocument(String docKey, String token) {
		return CollaboratorServer.requestDocument(docKey, token);
	}

	@Override
	public String unrequestDocument(String docKey, String token) {
		return CollaboratorServer.unrequestDocument(docKey, token);
	}

	@Override
	public UnlockedDocument getDocument(String documentKey) {
		return CollaboratorServer.getDocument(documentKey);
	}

	@Override
	public LockedDocument getLockedDocument(String docKey, String token) throws LockExpired {
		return CollaboratorServer.getLockedDocument(docKey, token);
	}

	@Override
	public UnlockedDocument saveDocument(LockedDocument lockedDoc, String token) throws LockExpired {
		return CollaboratorServer.saveDocument(lockedDoc, token);
	}

	@Override
	public String releaseLock(LockedDocument doc, String token) throws LockExpired {
		return CollaboratorServer.releaseLock(doc, token);
	}
}
