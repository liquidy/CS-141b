package edu.caltech.cs141b.hw2.gwt.collab.server;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import edu.caltech.cs141b.hw2.gwt.collab.client.CollaboratorService;
import edu.caltech.cs141b.hw2.gwt.collab.shared.DocumentMetadata;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockExpired;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockUnavailable;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;
import edu.caltech.cs141b.hw2.gwt.collab.shared.UnlockedDocument;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class CollaboratorServiceImpl extends RemoteServiceServlet implements
		CollaboratorService {
	
	public static final int LOCK_TIMEOUT = 30;     // Seconds
	private static final Logger log = Logger.getLogger(CollaboratorServiceImpl.class.toString());
	
	private PersistenceManager pm = PMF.get().getPersistenceManager();
	
	@Override
	public List<DocumentMetadata> getDocumentList() {
		List<DocumentMetadata> docMetaList = new ArrayList<DocumentMetadata>();
		
		// Get documents by querying all Document.class types
		List<Document> documents = null;
		Transaction txn = pm.currentTransaction();
		try {
			txn.begin();
			
			Query q = pm.newQuery(Document.class);
	    documents = (List<Document>) q.execute();
	    
			txn.commit();
		} finally {
	    if (txn.isActive()) {
        txn.rollback();
	    }
		}
		if (documents == null) {
			return docMetaList;
		}
		
		// Convert answer into list of metadata and return it.
		for (Document doc : documents) {
			String docKey = KeyFactory.keyToString(doc.getKey());
			String docTitle = doc.getTitle();
			docMetaList.add(new DocumentMetadata(docKey, docTitle));
		}
		
		return docMetaList;
	}

	@Override
	public LockedDocument lockDocument(String documentKey)
			throws LockUnavailable {
		
		Key key = KeyFactory.stringToKey(documentKey);
		Document persistedDoc = null;
		Transaction txn = pm.currentTransaction();
		try {
			txn.begin();
			
			// Get persisted doc.
			persistedDoc = pm.getObjectById(Document.class, key);
			if (persistedDoc == null)
				return null;
			
			// Using the identity of the client in conjunction with timestamps,
			// figure out if a document is available to be locked. If it is,
			// lock it and persist the new timestamps; otherwise, throw an exception.
			if (isLockUnavailable(persistedDoc)) {
				throw new LockUnavailable("Document locked until " + persistedDoc.getLockedUntil());
			} else {
		    persistedDoc.setLockedBy(getThreadLocalRequest().getRemoteAddr());
		    Calendar cal = Calendar.getInstance();
		    cal.add(Calendar.SECOND, LOCK_TIMEOUT);
		    persistedDoc.setLockedUntil(cal.getTime());
		    pm.makePersistent(persistedDoc);
			}
			
			txn.commit();
		} finally {
	    if (txn.isActive()) {
        txn.rollback();
	    }
		}
		
		return new LockedDocument(
				persistedDoc.getLockedBy(), 
				persistedDoc.getLockedUntil(), 
				KeyFactory.keyToString(persistedDoc.getKey()),
				persistedDoc.getTitle(),
				persistedDoc.getContents());
	}
	
	@Override
	public UnlockedDocument getDocument(String documentKey) {
		Document persistedDoc = null;
		Transaction txn = pm.currentTransaction();
		try {
			txn.begin();
			
			Key key = KeyFactory.stringToKey(documentKey);
			persistedDoc = pm.getObjectById(Document.class, key);
	    
			txn.commit();
		} finally {
	    if (txn.isActive()) {
        txn.rollback();
	    }
		}
		
		if (persistedDoc == null)
			return null;
		else {
			return new UnlockedDocument(
					documentKey, persistedDoc.getTitle(), persistedDoc.getContents());
		}
	}

	@Override
	public UnlockedDocument saveDocument(LockedDocument doc)
			throws LockExpired {
		
		UnlockedDocument unlockedDoc = doc.unlock();
		
		// Before returning the unlocked document, we need to lock via DS.
		Key key = null;
		if (doc.getKey() != null)
			key = KeyFactory.stringToKey(doc.getKey());
		
		// Persist Document JDO.
		Document persistedDoc = null;
		Transaction txn = pm.currentTransaction();
		try {
			txn.begin();
			
			// We quickly check if the lock has expired, else continue on with
			// saving and unlocking the document.
			Date lockedUntil = null;
			if (key == null) {
				lockedUntil = doc.getLockedUntil();
			} else {
				lockedUntil = pm.getObjectById(Document.class, key).getLockedUntil();
			}
			Date currentDate = new Date();
			String lockedBy = doc.getLockedBy();
			String ipAddress = getThreadLocalRequest().getRemoteAddr();
			if ((lockedUntil != null && lockedUntil.before(currentDate)) || 
					(lockedBy != null && !lockedBy.equals(ipAddress))) {
				throw new LockExpired();
			}
			
			// If key is not set, then the Document object should be persisted,
			// so that a key will automatically be generated. Otherwise, let's
			// just set the document's key and update the old copy.
			persistedDoc = new Document(
					unlockedDoc.getTitle(), 
					unlockedDoc.getContents(),
					getThreadLocalRequest().getRemoteAddr(),
					currentDate);
			if (key != null)
				persistedDoc.setKey(key);
			pm.makePersistent(persistedDoc);
			
			// Pack up UnlockedDocument to return.
			unlockedDoc = new UnlockedDocument(
					KeyFactory.keyToString(persistedDoc.getKey()), 
					persistedDoc.getTitle(), 
					persistedDoc.getContents());
			
			txn.commit();
		} finally {
	    if (txn.isActive()) {
        txn.rollback();
	    }
		}
		
		return unlockedDoc;
	}
	
	@Override
	public void releaseLock(LockedDocument doc) throws LockExpired {
		if (doc.getKey() == null)
			return;
		
		Key key = KeyFactory.stringToKey(doc.getKey());
		Document persistedDoc = null;
		Transaction txn = pm.currentTransaction();
		try {
			txn.begin();
			
			// Get persisted document.
			persistedDoc = pm.getObjectById(Document.class, key);
			if (persistedDoc == null)
				return;
			
			// We quickly check if the lock has expired, else continue on with
			// saving and unlocking the document.
			Date lockedUntil = null;
			if (key == null) {
				lockedUntil = doc.getLockedUntil();
			} else {
				lockedUntil = pm.getObjectById(Document.class, key).getLockedUntil();
			}
			Date currentDate = new Date();
			String lockedBy = doc.getLockedBy();
			String ipAddress = getThreadLocalRequest().getRemoteAddr();
			if ((lockedUntil != null && lockedUntil.before(currentDate)) || 
					(lockedBy != null && !lockedBy.equals(ipAddress))) {
				throw new LockExpired();
			} else {
				// Release the lock on the document; update lockedBy and lockedUntil.
				persistedDoc.setLockedBy(getThreadLocalRequest().getRemoteAddr());
		    persistedDoc.setLockedUntil(currentDate);
		    pm.makePersistent(persistedDoc);
			}
			
			txn.commit();
		} finally {
	    if (txn.isActive()) {
        txn.rollback();
	    }
		}
	}
	
	// This method does a check for whether or not a document is locked based
	// on the timestamps as well as the lockedBy identity.
	private boolean isLockUnavailable(Document doc) {
		Date currentDate = new Date();
		Date lockedUntil = doc.getLockedUntil();
		String lockedBy = doc.getLockedBy();
		if (lockedUntil == null || lockedBy == null)
			return false;
		String ipAddress = getThreadLocalRequest().getRemoteAddr();
		
		return currentDate.before(lockedUntil) && 
				!ipAddress.equals(lockedBy);
	}
}
