package edu.caltech.cs141b.hw2.gwt.collab.server;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withCountdownMillis;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import edu.caltech.cs141b.hw2.gwt.collab.client.CollaboratorService;
import edu.caltech.cs141b.hw2.gwt.collab.shared.DocRequestorResult;
import edu.caltech.cs141b.hw2.gwt.collab.shared.DocumentMetadata;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockExpired;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockUnavailable;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;
import edu.caltech.cs141b.hw2.gwt.collab.shared.Messages;
import edu.caltech.cs141b.hw2.gwt.collab.shared.UnlockedDocument;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class CollaboratorServiceImpl extends RemoteServiceServlet
                                     implements CollaboratorService {
	
	public static final int LOCK_TIMEOUT = 30;     // Seconds
	public static final String DELIMITER = "~";
	
	private Hashtable<String, String> tokenToClient = 
			new Hashtable<String, String>();
	private Hashtable<String, Queue<String>> docToQueue =
			new Hashtable<String, Queue<String>>();    // Queues are of clientIds
	private Hashtable<String, Object> docToQueueLocks = 
			new Hashtable<String, Object>();
	private Hashtable<String, String> docToTaskNames =
			new Hashtable<String, String>();
	private Object instantiationLock = new Object();
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(CollaboratorServiceImpl.class.toString());
	
	public CollaboratorServiceImpl() {
		CollaboratorServiceCommon.setService(this);
	}
	
	@Override
	public String setUpChannel() {
    String clientId = UUID.randomUUID().toString();
		String token = ChannelServiceFactory.getChannelService()
				.createChannel(clientId);
		tokenToClient.put(token, clientId);
		return token;
	}
	
	@Override
	public List<DocumentMetadata> getDocumentList() {
		List<DocumentMetadata> docMetaList = new ArrayList<DocumentMetadata>();
		
		// Get documents by querying all Document.class types
		List<Document> documents = null;
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Query q = pm.newQuery(Document.class);
			
			@SuppressWarnings("unchecked")
			List<Document> documentsTemp = (List<Document>) q.execute();
			documents = documentsTemp;
			
			// Convert answer into list of metadata and return it.
			for (Document doc : documents) {
				String docKey = KeyFactory.keyToString(doc.getKey());
				String docTitle = doc.getTitle();
				docMetaList.add(new DocumentMetadata(docKey, docTitle));
			}
			return docMetaList;
		} finally {
			pm.close();
		}
	}
	
	@Override
	public DocRequestorResult requestDocument(String documentKey, String token) {
		int numPeopleInFront = -1;
		String clientId = tokenToClient.get(token);
		lazyInstantiationsForDoc(documentKey);
		Object queueLock = docToQueueLocks.get(documentKey);
		synchronized (queueLock) {
			Queue<String> clientQueue = docToQueue.get(documentKey);
			numPeopleInFront = clientQueue.size();
			clientQueue.add(clientId);
		}
		if (numPeopleInFront == 0) {
			sendMessage(clientId, Messages.CODE_LOCK_READY + documentKey);
		}
		return new DocRequestorResult(documentKey, numPeopleInFront);
	}
	
	@Override
	public String unrequestDocument(String documentKey, String token) {
		// Remove the client from the timeout queue.
		removeFromTaskQueue(documentKey);
		
		// Remove clientId from the document queue.
		String clientToRemove = tokenToClient.get(token);
		Object queueLock = docToQueueLocks.get(documentKey);
		synchronized (queueLock) {
			Queue<String> clientIds = docToQueue.get(documentKey);
			Iterator<String> clientsItr = clientIds.iterator();
			int i = 0;
			while (clientsItr.hasNext()) {
				String clientId = clientsItr.next();
				if (clientId.equals(clientToRemove)) {
					clientsItr.remove();
					break;
				}
				i++;
			}
			// Notify the rest of the people that they have moved up one position.
			while (clientsItr.hasNext()) {
				String clientId = clientsItr.next();
				sendMessage(clientId, Messages.CODE_LOCK_NOT_READY + 
						String.valueOf(i) + DELIMITER + documentKey);
				i++;
			}
		}
		
		return documentKey;
	}

	@Override
	public LockedDocument lockDocument(String documentKey, String token)
			throws LockUnavailable {
		
		// Check the queue to see that we're not cutting someone in line.
		Object queueLock = docToQueueLocks.get(documentKey);
		synchronized (queueLock) {
			Queue<String> clientIds = docToQueue.get(documentKey);
			if (clientIds != null && clientIds.size() != 0 &&
					!clientIds.peek().equals(tokenToClient.get(token))) {
				throw new LockUnavailable(true, null, null, clientIds.peek());
			}
		}
		
		// Lock the document.
		Key key = KeyFactory.stringToKey(documentKey);
		Document persistedDoc = null;
		PersistenceManager pm = PMF.get().getPersistenceManager();
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
			boolean lockAvailable;
			Date currentDate = new Date();
			Date lockedUntil = persistedDoc.getLockedUntil();
			String lockedBy = persistedDoc.getLockedBy();
			if (lockedUntil == null || lockedBy == null) {
				lockAvailable = true;
			} else {
				lockAvailable = lockedUntil.before(currentDate) || 
						token.equals(lockedBy);
			}
			if (!lockAvailable) {
				// Determine if exception was thrown because of timestamps or creds.
				throw new LockUnavailable(!token.equals(lockedBy),
						persistedDoc.getLockedUntil(), 
						KeyFactory.keyToString(persistedDoc.getKey()),
						lockedBy); 
			} else {
				persistedDoc.setLockedBy(token);
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.SECOND, LOCK_TIMEOUT);
				persistedDoc.setLockedUntil(cal.getTime());
				pm.makePersistent(persistedDoc);
			}
			
			txn.commit();
			
			String keyStr = KeyFactory.keyToString(persistedDoc.getKey());
			// Set up scheduled task so that after a timeout period, we just
			// move onto the next person in the queue.
			String taskName = UUID.randomUUID().toString();
			QueueFactory.getDefaultQueue().add(
					withCountdownMillis(LOCK_TIMEOUT * 1000)
					.url("/task/lockExpiration")
					.param("docKey", keyStr)
					.taskName(taskName));
			
			return new LockedDocument(
	        persistedDoc.getLockedBy(), 
	        persistedDoc.getLockedUntil(), 
	        keyStr,
	        persistedDoc.getTitle(),
	        persistedDoc.getContents().getValue());
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
			pm.close();
		}
	}
	
	@Override
	public UnlockedDocument getDocument(String documentKey) {
		Document persistedDoc = null;
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Key key = KeyFactory.stringToKey(documentKey);
			persistedDoc = pm.getObjectById(Document.class, key);
			
			if (persistedDoc == null) {
				return null;
			} else {
				return new UnlockedDocument(
				        documentKey,
				        persistedDoc.getTitle(),
				        persistedDoc.getContents().getValue());
			}
		} finally {
			pm.close();
		}
	}

	@Override
	public UnlockedDocument saveDocument(LockedDocument doc, String token)
			throws LockExpired {
		
		// Find key from doc.
		String keyStr = doc.getKey();
		Key key = null;
		if (keyStr != null) {
			key = KeyFactory.stringToKey(keyStr);
			
			// Remove the client from the timeout queue.
			removeFromTaskQueue(keyStr);
		}
		
		// Persist Document JDO.
		PersistenceManager pm = PMF.get().getPersistenceManager();
		Transaction txn = pm.currentTransaction();
		try {
			txn.begin();
			
			// Get persisted document.
			Document persistedDoc = null;
			if (key != null) {
				persistedDoc = pm.getObjectById(Document.class, key);
			}
			
			// If persistedDoc is null, then the Document object should be persisted,
			// so that a key will automatically be generated. Otherwise, take the
			// object, check credentials, modify some fields, and persist again.
			if (persistedDoc == null) {
				persistedDoc = new Document(
						doc.getTitle(), 
						new Text(doc.getContents()),
						token,
						null);
			} else {
				Date currentDate = new Date();
				Date lockedUntil = persistedDoc.getLockedUntil();
				String lockedBy = persistedDoc.getLockedBy();
				if (lockedUntil != null && lockedUntil.before(currentDate)) {
					throw new LockExpired(false, lockedUntil, 
							KeyFactory.keyToString(persistedDoc.getKey()), lockedBy); 
				}
				if (lockedBy != null && !lockedBy.equals(token)) {
					throw new LockExpired(true, lockedUntil, 
							KeyFactory.keyToString(persistedDoc.getKey()), lockedBy); 
				}
				
				persistedDoc.setTitle(doc.getTitle());
				persistedDoc.setContents(new Text(doc.getContents()));
				persistedDoc.setLockedBy(null);
				persistedDoc.setLockedUntil(null);
			}
			pm.makePersistent(persistedDoc);

			txn.commit();
			
			keyStr = KeyFactory.keyToString(persistedDoc.getKey());
			// Update queue for the doc and notify the person next in line.
			lazyInstantiationsForDoc(keyStr);
			pollDocQueue(keyStr);
			
			// Pack up UnlockedDocument to return.
			return new UnlockedDocument(
					keyStr,
					persistedDoc.getTitle(), 
					persistedDoc.getContents().getValue());
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
			pm.close();
		}
	}
	
	@Override
	public String releaseLock(LockedDocument doc, String token) throws LockExpired {
		String keyStr = doc.getKey();
		if (keyStr == null) {
			return keyStr;
		}
		
		// Remove the client from the timeout queue.
		removeFromTaskQueue(keyStr);
		
		PersistenceManager pm = PMF.get().getPersistenceManager();
		Transaction txn = pm.currentTransaction();
		try {
			txn.begin();
			
			// Get persisted document.
			Key key = KeyFactory.stringToKey(keyStr);
			Document persistedDoc = pm.getObjectById(Document.class, key);
			// Checking null in case key provided into the method was invalid.
			if (persistedDoc == null) {
				return keyStr;
			}
			
			// We quickly check if the lock has expired, else continue on with
			// saving and unlocking the document.
			Date lockedUntil = persistedDoc.getLockedUntil();
			Date currentDate = new Date();
			String lockedBy = doc.getLockedBy();
			if (lockedUntil != null && lockedUntil.before(currentDate)) {
				throw new LockExpired(false, lockedUntil, 
						KeyFactory.keyToString(persistedDoc.getKey()), lockedBy); 
			} else if (lockedBy != null && !lockedBy.equals(token)) {
				throw new LockExpired(true, lockedUntil, 
						KeyFactory.keyToString(persistedDoc.getKey()), lockedBy); 
			} else {
				// Release the lock on the document; update lockedBy and lockedUntil.
				persistedDoc.setLockedBy(null);
				persistedDoc.setLockedUntil(null);
				pm.makePersistent(persistedDoc);
			}
			
			// Update queue for the doc and notify the person next in line.
			pollDocQueue(KeyFactory.keyToString(persistedDoc.getKey()));
			
			txn.commit();
			
			return keyStr;
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
			pm.close();
		}
	}
	
	protected void pollDocQueue(String documentKey) {
		// Update queue for the doc.
		Object queueLock = docToQueueLocks.get(documentKey);
		synchronized (queueLock) {
			Queue<String> clientIds = docToQueue.get(documentKey);
			String clientId = clientIds.poll();
			if (clientId != null) {
				sendMessage(clientId, Messages.CODE_LOCK_EXPIRED + documentKey);
				if (!clientIds.isEmpty()) {
					Iterator<String> clientsItr = clientIds.iterator();
					// Notify the first person that doc is ready to be locked.
					clientId = clientsItr.next();
					sendMessage(clientId, Messages.CODE_LOCK_READY + documentKey);
					// Notify the rest that the number of people left has changed.
					int i = 1;
					while (clientsItr.hasNext()) {
						clientId = clientsItr.next();
						sendMessage(clientId, Messages.CODE_LOCK_NOT_READY + 
								String.valueOf(i) + DELIMITER + documentKey);
						i++;
					}
				}
			}
		}
	}
	
	private void sendMessage(String clientId, String message) {
		ChannelService channelService = ChannelServiceFactory.getChannelService();
		channelService.sendMessage(new ChannelMessage(clientId, message));
	}
	
	private void lazyInstantiationsForDoc(String documentKey) {
		if (documentKey == null || documentKey.isEmpty())
			return;
		
		// Lazy instantiation for the queues and corresponding locks.
		if (!docToQueue.containsKey(documentKey)) {
			synchronized (instantiationLock) {
				if (!docToQueue.containsKey(documentKey)) {
					docToQueue.put(documentKey, new ArrayDeque<String>());
					docToQueueLocks.put(documentKey, new Object());
				}
			}
		}
	}
	
	private void removeFromTaskQueue(String documentKey) {
		String taskName = docToTaskNames.get(documentKey);
		if (taskName != null) {
			QueueFactory.getDefaultQueue().deleteTask(taskName);
		}
	}
}
