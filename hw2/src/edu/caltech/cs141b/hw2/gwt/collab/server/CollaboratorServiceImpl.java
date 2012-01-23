package edu.caltech.cs141b.hw2.gwt.collab.server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import edu.caltech.cs141b.hw2.gwt.collab.client.CollaboratorService;
import edu.caltech.cs141b.hw2.gwt.collab.shared.DocumentMetadata;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockExpired;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockUnavailable;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;
import edu.caltech.cs141b.hw2.gwt.collab.shared.UnlockedDocument;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class CollaboratorServiceImpl extends RemoteServiceServlet implements
		CollaboratorService {
	
	public static final String DOCUMENT_KIND_NAME = "Document";
	public static final String DOCUMENT_CONTENT_PNAME = "documentContent";
	public static final String LOCKED_BY_PNAME = "lockedBy";
	public static final String LOCKED_UNTIL_PNAME = "lockedUntil";
	public static final int LOCK_TIMEOUT = 100;                       // Seconds
	private static final Logger log = Logger.getLogger(CollaboratorServiceImpl.class.toString());
	
	@Override
	public List<DocumentMetadata> getDocumentList() {
		List<DocumentMetadata> docsList = new ArrayList<DocumentMetadata>();
		Query docsQuery = new Query(DOCUMENT_KIND_NAME);
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		List<Entity> results = datastore.prepare(docsQuery).asList(
				FetchOptions.Builder.withDefaults());
		for (Entity en : results) {
			String docKey = KeyFactory.keyToString(en.getKey());
			String docTitle = 
					((UnlockedDocument) en.getProperty(DOCUMENT_CONTENT_PNAME)).getTitle();
			docsList.add(new DocumentMetadata(docKey, docTitle));
		}
		return docsList;
	}

	@Override
	public LockedDocument lockDocument(String documentKey)
			throws LockUnavailable {
		
		// Generate hash for the client by using MD5 on the current time
		Calendar currentCalendar = Calendar.getInstance();
		Date currentDate = currentCalendar.getTime();
		MessageDigest digest = null;
		try {
			digest = java.security.MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}
		digest.update(currentDate.toString().getBytes()); 
		String clientHash = digest.digest().toString();
		
		UnlockedDocument document = null;
		String lockedBy = null;
		Date lockedUntil = null;
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = datastore.beginTransaction();
		try {
			Entity docEntity = datastore.get(KeyFactory.stringToKey(documentKey));
			document = (UnlockedDocument) docEntity.getProperty(DOCUMENT_CONTENT_PNAME);
			lockedBy = (String) docEntity.getProperty(LOCKED_BY_PNAME);
			lockedUntil = (Date) docEntity.getProperty(LOCKED_UNTIL_PNAME);
			if (lockedUntil != null) {
				if (lockedUntil.before(currentDate)) {
					// Document is available for locking, since timestamp has passed.
					lockDocEntity(docEntity, currentCalendar, clientHash);
				} else {
					throw new LockUnavailable();
				}
			} else {
				// Document is available for locking, since no timestamp has been set.
				lockDocEntity(docEntity, currentCalendar, clientHash);
			}
			
	    txn.commit();
		} catch (EntityNotFoundException e) {
			return null;
		} finally {
	    if (txn.isActive()) {
        txn.rollback();
	    }
		}
		
		return new LockedDocument(lockedBy, lockedUntil,
				document.getKey(), document.getTitle(), document.getContents());
	}
	
	private void lockDocEntity(Entity docEntity, Calendar currentCalendar, String clientHash) {
		docEntity.setProperty(LOCKED_BY_PNAME, clientHash);
		currentCalendar.add(Calendar.SECOND, LOCK_TIMEOUT);
		docEntity.setProperty(LOCKED_UNTIL_PNAME, currentCalendar.getTime());
	}
	
	@Override
	public UnlockedDocument getDocument(String documentKey) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Entity document = null;
		try {
			document = datastore.get(KeyFactory.stringToKey(documentKey));
		} catch (EntityNotFoundException e) {
			return null;
		}
		return (UnlockedDocument) document.getProperty(DOCUMENT_CONTENT_PNAME);
	}

	@Override
	public UnlockedDocument saveDocument(LockedDocument doc)
			throws LockExpired {
		
		// This method should throw LockExpired exception if lock has expired.
		releaseLock(doc);
		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = datastore.beginTransaction();
		try {
	    Entity document = new Entity(DOCUMENT_KIND_NAME, doc.getKey());
	    document.setProperty(DOCUMENT_CONTENT_PNAME, doc);
	    datastore.put(document);

	    txn.commit();
		} finally {
	    if (txn.isActive()) {
        txn.rollback();
	    }
		}
		
		return doc.unlock();
	}
	
	@Override
	public void releaseLock(LockedDocument doc) throws LockExpired {
		Date currentDate = new Date();
		if (doc.getLockedUntil().before(currentDate)) {
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Transaction txn = datastore.beginTransaction();
			try {
				// Unlock the doc by setting the appropriate DS fields for this doc.
				Entity docEntity = new Entity(DOCUMENT_KIND_NAME, doc.getKey());
				docEntity.setProperty(LOCKED_BY_PNAME, "");
				docEntity.setProperty(LOCKED_UNTIL_PNAME, currentDate);
				datastore.put(docEntity);
				
				txn.commit();
			} finally {
		    if (txn.isActive()) {
	        txn.rollback();
		    }
			}
		} else {
			throw new LockExpired();
		}
	}

}

