package edu.caltech.cs141b.hw2.gwt.collab.server;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
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
	
	private DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
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
		
		// Convert answer into metadata and return it.
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
		Document persistedDoc = getDocument(key);
		if (persistedDoc == null)
			return null;
		
		Date lockedUntil = persistedDoc.getLockedUntil();
		Date currentDate = new Date();
		if (currentDate.before(lockedUntil)) {
			throw new LockUnavailable();
		} else {
			// Document is available for locking.
			Transaction txn = pm.currentTransaction();
			try {
				txn.begin();
				
		    persistedDoc.setLockedBy(UUID.randomUUID().toString());
		    Calendar cal = Calendar.getInstance();
		    cal.add(Calendar.SECOND, LOCK_TIMEOUT);
		    persistedDoc.setLockedUntil(cal.getTime());
		    pm.makePersistent(persistedDoc);
		    
				txn.commit();
			} finally {
		    if (txn.isActive()) {
	        txn.rollback();
		    }
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
		
		return new UnlockedDocument(
				documentKey, persistedDoc.getTitle(), persistedDoc.getContents());
	}

	@Override
	public UnlockedDocument saveDocument(LockedDocument doc)
			throws LockExpired {
		
		UnlockedDocument unlockedDoc = doc.unlock();
		
		// Before returning the unlocked document, we need to lock via DS.
		Date currentDate = new Date();
		Date lockedUntil = doc.getLockedUntil();
		
		// We throw LockExpired only if lockedUntil is set and it's an expired date.
		// In all other cases, we just save the document via DS.
		if (lockedUntil != null && lockedUntil.before(currentDate)) {
			throw new LockExpired();
		} else {
			Key key = null;
			if (doc.getKey() != null) {
				key = KeyFactory.stringToKey(doc.getKey());
			}
			Document document = new Document(
					unlockedDoc.getTitle(), 
					unlockedDoc.getContents(),
					UUID.randomUUID().toString(),
					currentDate);
			document.setKey(key);
			
			// Save the document.
			Transaction txn = pm.currentTransaction();
			try {
				txn.begin();
		    pm.makePersistent(document);
				txn.commit();
			} finally {
		    if (txn.isActive()) {
	        txn.rollback();
		    }
			}
			unlockedDoc = new UnlockedDocument(
					KeyFactory.keyToString(document.getKey()), 
					doc.getTitle(), 
					doc.getContents());
		}
		
		return unlockedDoc;
	}
	
	@Override
	public void releaseLock(LockedDocument doc) throws LockExpired {
		if (doc.getKey() == null)
			return;
		
		Key key = KeyFactory.stringToKey(doc.getKey());
		Document persistedDoc = getDocument(key);
		if (persistedDoc == null)
			return;
		
		Date lockedUntil = persistedDoc.getLockedUntil();
		Date currentDate = new Date();
		if (lockedUntil.before(currentDate)) {
			throw new LockExpired();
		} else {
			// Release the lock on the document; update lockedBy and lockedUntil.
			Transaction txn = pm.currentTransaction();
			try {
				txn.begin();
				
		    persistedDoc.setLockedBy(UUID.randomUUID().toString());
		    persistedDoc.setLockedUntil(currentDate);
		    pm.makePersistent(persistedDoc);
		    
				txn.commit();
			} finally {
		    if (txn.isActive()) {
	        txn.rollback();
		    }
			}
		}
	}
	
	private Document getDocument(Key key) {
		// Get the persisted document and check the timestamp.
		Transaction txn = pm.currentTransaction();
		Document persistedDoc = null;
		try {
			txn.begin();
			persistedDoc = pm.getObjectById(Document.class, key);
			txn.commit();
		} finally {
			if (txn.isActive()) {
        txn.rollback();
	    }
		}
		return persistedDoc;
	}

}

