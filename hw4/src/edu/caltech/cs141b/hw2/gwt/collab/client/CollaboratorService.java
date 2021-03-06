package edu.caltech.cs141b.hw2.gwt.collab.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import edu.caltech.cs141b.hw2.gwt.collab.shared.DocRequestorResult;
import edu.caltech.cs141b.hw2.gwt.collab.shared.DocumentMetadata;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockExpired;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;
import edu.caltech.cs141b.hw2.gwt.collab.shared.UnlockedDocument;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("collab")
public interface CollaboratorService extends RemoteService {
	
	/**
	 * Sets up a channel for the client that called this method.
	 * 
	 * @return token to uniquely identify the client
	 */
	String setUpChannel();
	
	/**
	 * Used to get a list of the currently available documents.
	 * 
	 * @return a list of the metadata of the currently available documents
	 */
	List<DocumentMetadata> getDocumentList();
	
	/**
	 * Request a particular document to be locked. (Get in a queue.)
	 * 
	 * @param documentKey the key of the document to lock
	 * @param token the token that identifies the client
	 * @return DocRequestorResult with documentKey and numPeopleLeft
	 */
	DocRequestorResult requestDocument(String documentKey, String token);
	
	/**
	 * Used to remove the client from the queue for a particular doc.
	 * 
	 * @param documentKey the key of the document to lock
	 * @param token the token that identifies the client
	 * @return key (String) of the document that was unrequested
	 */
	String unrequestDocument(String documentKey, String token);
	
	/**
	 * Used to retrieve a locked document for editing.
	 * 
	 * @param documentKey the key of the document to lock
	 * @param token the token that identifies the client
	 * @return a LockedDocument object containing the current document state
	 *         and the locking primites necessary to save the document
	 * @throws LockExpired if this lock is not obtained
	 */
	LockedDocument getLockedDocument(String documentKey, String token) throws LockExpired;
	
	/**
	 * Used to retrieve a document in read-only mode.
	 * 
	 * @param documentKey the key of the document to read
	 * @return an UnlockedDocument object which contains the entire document
	 *         but without any locking primitives
	 */
	UnlockedDocument getDocument(String documentKey);
	
	/**
	 * Used to save a currently locked document.
	 * 
	 * @param doc the LockedDocument object returned by lockDocument(), with
	 *         the document properties (but not the locking primitives)
	 *         potentially modified
	 * @param token the token that identifies the client
	 * @throws LockExpired if the locking primitives in the supplied
	 *         LockedDocument object cannot be used to modify the document
	 */
	UnlockedDocument saveDocument(LockedDocument doc, String token) throws LockExpired;
	
	/**
	 * Used to release a lock that is no longer needed without saving.
	 * 
	 * @param doc the LockedDocument object returned by lockDocument(); any
	 *         modifications made to the document properties in this case are
	 *         ignored
	 * @param token the token that identifies the client
	 * @return the key of the document to lock
	 * @throws LockExpired if the locking primitives in the supplied
	 *         LockedDocument object cannot be used to release the lock
	 */
	String releaseLock(LockedDocument doc, String token) throws LockExpired;
}

