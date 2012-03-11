package edu.caltech.cs141b.hw2.gwt.collab.server;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.logging.Logger;

import javax.jdo.JDODataStoreException;
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
import com.google.appengine.api.taskqueue.TaskOptions;

import edu.caltech.cs141b.hw2.gwt.collab.shared.DocRequestorResult;
import edu.caltech.cs141b.hw2.gwt.collab.shared.DocumentMetadata;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockExpired;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;
import edu.caltech.cs141b.hw2.gwt.collab.shared.Messages;
import edu.caltech.cs141b.hw2.gwt.collab.shared.UnlockedDocument;


/**
 * Holds Collaborator server code that backs other classes (e.g. CollaboratorServiceImpl, CollaboratorServerTaskServlet)
 */
public class CollaboratorServer {

	public static final int LOCK_TIMEOUT = 30;     // Seconds
	public static final int NUM_RETRIES = 5;
	public static final String DELIMITER = "~";

	private static final Logger log = Logger.getLogger(CollaboratorServer.class.getName());
	
	private CollaboratorServer() {}

	public static String setUpChannel() {
		String clientId = UUID.randomUUID().toString();
		String token = ChannelServiceFactory.getChannelService()
				.createChannel(clientId);
		ChannelInfo channelInfo = new ChannelInfo(token, clientId);

		PersistenceManager pm = PMF.get().getPersistenceManager();
		Transaction txn = pm.currentTransaction();
		try {
			txn.begin();
			pm.makePersistent(channelInfo);
			txn.commit();

			return token;
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
			pm.close();
		}
	}

	public static List<DocumentMetadata> getDocumentList() {
		List<DocumentMetadata> docMetaList = new ArrayList<DocumentMetadata>();

		// Get documents by querying all Document.class types
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Query q = pm.newQuery(Document.class);
			@SuppressWarnings("unchecked")
			List<Document> documents = (List<Document>) q.execute();

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

	public static DocRequestorResult requestDocument(String docKey, String token) {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		String clientId = pm.getObjectById(ChannelInfo.class, token).getClient();
		Hashtable<String, String> messages = new Hashtable<String, String>();
		ArrayList<TaskOptions> tasks = new ArrayList<TaskOptions>();
		Transaction txn = pm.currentTransaction();
		try {
			txn.begin();

			Document doc = pm.getObjectById(Document.class, docKey);
			Queue<String> clientQueue = doc.getClientQueue();
			// If the client is already in the queue, throw an exception.
			if (clientQueue.contains(clientId)) {
				throw new RuntimeException("requestDocument was called, and the document " +
						docKey + " queue already includes client " + clientId);
			}
			// Add the client to the queue. Also, be sure to lock if the queue is empty.
			int numPeopleInFront = clientQueue.size();
			if (numPeopleInFront == 0) {
				// Lock the document for this client.
				doc = lockDocument(doc, token, pm, tasks);
				messages.put(clientId, Messages.LOCK_READY + docKey);
			}
			clientQueue.add(clientId);
			pm.makePersistent(doc);

			txn.commit();

			return new DocRequestorResult(docKey, numPeopleInFront);
		} catch (ConcurrentModificationException e) {
			tasks.add(TaskOptions.Builder.withUrl("/task/collaboratorServer")
					.param("methodName", "requestDocument")
					.param("docKey", docKey)
					.param("token", token));
			log.warning("Enqueuing requestDocument job, docKey: " + docKey + ", token: " + token);
			return null;
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			} else {
				// Send out any relevant messages if the transaction went through.
				for (String client : messages.keySet()) {
					sendMessage(client, messages.get(client));
				}
				// Enqueue any relevant tasks if the transaction went through.
				for (TaskOptions task : tasks) {
					QueueFactory.getDefaultQueue().add(task);
				}
			}
			pm.close();
		}
	}

	public static String unrequestDocument(String docKey, String token) {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		String clientToRemove = pm.getObjectById(ChannelInfo.class, token).getClient();
		int retries = NUM_RETRIES;
		while (true) {
			Hashtable<String, String> messages = new Hashtable<String, String>();
			Transaction txn = pm.currentTransaction();
			try {
				txn.begin();

				Document doc = pm.getObjectById(Document.class, docKey);
				Queue<String> clientQueue = doc.getClientQueue();
				// If the client is not in the queue, throw an exception.
				if (!clientQueue.contains(clientToRemove)) {
					throw new RuntimeException("unrequestDocument was called, and the document " +
							docKey + " queue does not include client " + clientToRemove);
				}
				// Remove this client from the queue.
				Iterator<String> clientsItr = clientQueue.iterator();
				int i = 0;
				while (clientsItr.hasNext()) {
					String clientId = clientsItr.next();
					if (clientId.equals(clientToRemove)) {
						clientsItr.remove();
						break;
					}
					i++;
				}
				// Notify the rest of the people behind him that they have moved up one position.
				while (clientsItr.hasNext()) {
					String clientId = clientsItr.next();
					messages.put(clientId, Messages.LOCK_NOT_READY + 
							String.valueOf(i) + DELIMITER + docKey);
					i++;
				}
				pm.makePersistent(doc);

				txn.commit();

				return docKey;
			} catch (ConcurrentModificationException e) {
				if (retries == 0) {
					throw e;
				}
				retries--;
			} finally {
				if (txn.isActive()) {
					txn.rollback();
				} else {
					// Send out any relevant messages if the transaction went through.
					for (String client : messages.keySet()) {
						sendMessage(client, messages.get(client));
					}
				}
				pm.close();
			}
		}
	}

	public static UnlockedDocument getDocument(String documentKey) {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			Key key = KeyFactory.stringToKey(documentKey);
			Document persistedDoc = pm.getObjectById(Document.class, key);

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

	public static LockedDocument getLockedDocument(String docKey, String token) throws LockExpired {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		try {
			// Get persisted doc.
			Key key = KeyFactory.stringToKey(docKey);
			Document persistedDoc = pm.getObjectById(Document.class, key);

			// Figure out if a document is actually locked. If it is,
			// return it; otherwise, throw an exception.
			Date currentDate = new Date();
			Date lockedUntil = persistedDoc.getLockedUntil();
			String lockedBy = persistedDoc.getLockedBy();
			if (!((lockedUntil != null && currentDate.before(lockedUntil)) &&
					(lockedBy != null && lockedBy.equals(token)))) {
				throw new LockExpired(docKey);
			}

			return new LockedDocument(
					persistedDoc.getLockedBy(), 
					persistedDoc.getLockedUntil(), 
					docKey,
					persistedDoc.getTitle(),
					persistedDoc.getContents().getValue());
		} finally {
			pm.close();
		}
	}

	public static UnlockedDocument saveDocument(LockedDocument lockedDoc, String token) throws LockExpired {
		// Find key from doc.
		String docKey = lockedDoc.getKey();
		Key key = null;
		if (docKey != null) {
			key = KeyFactory.stringToKey(docKey);
		}

		// Persist Document JDO.
		PersistenceManager pm = PMF.get().getPersistenceManager();
		Hashtable<String, String> messages = new Hashtable<String, String>();
		ArrayList<TaskOptions> tasks = new ArrayList<TaskOptions>();
		Transaction txn = pm.currentTransaction();
		try {
			txn.begin();

			// Remove the client from the timeout queue.
			removeClientFromTimeoutQueue(docKey, pm);

			// Get persisted document.
			Document doc = null;
			if (key != null) {
				doc = pm.getObjectById(Document.class, key);
			}

			// If persistedDoc is null, then the Document object should be persisted,
			// so that a key will automatically be generated. Otherwise, take the
			// object, check credentials, modify some fields, and persist again.
			if (doc == null) {
				doc = new Document(lockedDoc.getTitle(), new Text(lockedDoc.getContents()));
			} else {
				Date currentDate = new Date();
				Date lockedUntil = doc.getLockedUntil();
				String lockedBy = doc.getLockedBy();
				// A lock is not expired if: 
				// 1) lockedUntil is set AND after now, AND
				// 2) lockedBy is set AND is this user
				if (!((lockedUntil != null && currentDate.before(lockedUntil)) &&
						(lockedBy != null && lockedBy.equals(token)))) {
					throw new LockExpired(docKey);
				}
				// Release doc and update contents.
				doc.setTitle(lockedDoc.getTitle());
				doc.setContents(new Text(lockedDoc.getContents()));
				doc.setLockedBy(null);
				doc.setLockedUntil(null);
				// Also poll the doc queue because this client is done.
				pollDocQueue(doc, false, pm, messages, tasks);
			}
			pm.makePersistent(doc);
			docKey = KeyFactory.keyToString(doc.getKey());

			txn.commit();

			// Pack up UnlockedDocument to return.
			return new UnlockedDocument(
					docKey,
					doc.getTitle(), 
					doc.getContents().getValue());
		} catch (JDODataStoreException e) {
			// NOTE: ConcurrentModificationException here is thrown as JDODataStoreException for some reason...
			// TODO: Figure out why?
			
			tasks.add(TaskOptions.Builder.withUrl("/task/collaboratorServer")
					.param("methodName", "saveDocument")
					.param("docKey", docKey)
					.param("token", token));
			log.warning("Enqueuing saveDocument job, docKey: " + docKey + ", token: " + token);
			return null;
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			} else {
				// Send out any relevant messages if the transaction went through.
				for (String client : messages.keySet()) {
					sendMessage(client, messages.get(client));
				}
				// Enqueue any relevant tasks if the transaction went through.
				for (TaskOptions task : tasks) {
					QueueFactory.getDefaultQueue().add(task);
				}
			}
			pm.close();
		}
	}

	public static String releaseLock(LockedDocument doc, String token) throws LockExpired {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		int retries = NUM_RETRIES;
		while (true) {
			Hashtable<String, String> messages = new Hashtable<String, String>();
			ArrayList<TaskOptions> tasks = new ArrayList<TaskOptions>();
			Transaction txn = pm.currentTransaction();
			try {
				txn.begin();

				// Remove the client from the timeout queue.
				String docKey = doc.getKey();
				removeClientFromTimeoutQueue(docKey, pm);

				// Get persisted document.
				Key key = KeyFactory.stringToKey(docKey);
				Document persistedDoc = pm.getObjectById(Document.class, key);

				// We quickly check if the lock has expired, else continue on with
				// saving and unlocking the document.
				Date currentDate = new Date();
				Date lockedUntil = persistedDoc.getLockedUntil();
				String lockedBy = persistedDoc.getLockedBy();
				// A lock is not expired if: 
				// 1) lockedUntil is set AND after now, AND
				// 2) lockedBy is set AND is this user
				if (!((lockedUntil != null && currentDate.before(lockedUntil)) &&
						(lockedBy != null && lockedBy.equals(token)))) {
					throw new LockExpired(docKey);
				}

				// Release the lock on the document; update lockedBy and lockedUntil.
				persistedDoc.setLockedBy(null);
				persistedDoc.setLockedUntil(null);

				// Update queue for the doc and notify the person next in line.
				pollDocQueue(persistedDoc, false, pm, messages, tasks);

				pm.makePersistent(persistedDoc);

				txn.commit();

				return docKey;
			} catch (ConcurrentModificationException e) {
				if (retries == 0) {
					throw e;
				}
				retries--;
			} finally {
				if (txn.isActive()) {
					txn.rollback();
				} else {
					// Send out any relevant messages if the transaction went through.
					for (String client : messages.keySet()) {
						sendMessage(client, messages.get(client));
					}
				}
				pm.close();
			}
		}
	}

	static void pollDocQueue(Document doc, boolean lockExpired, PersistenceManager pm,
			Hashtable<String, String> messages, ArrayList<TaskOptions> tasks) {

		String docKey = KeyFactory.keyToString(doc.getKey());

		// Update queue for the doc.
		LinkedList<String> clientIds = doc.getClientQueue();
		if (!clientIds.isEmpty()) {
			String clientId = clientIds.remove(0);
			if (lockExpired) {
				messages.put(clientId, Messages.LOCK_EXPIRED + docKey);
			}
			if (!clientIds.isEmpty()) {
				Iterator<String> clientsItr = clientIds.iterator();

				// Notify the first person that doc is locked.
				clientId = clientsItr.next();
				// First need to get token.
				Query q = pm.newQuery(ChannelInfo.class);
				q.setFilter("client == clientParam");
				q.declareParameters("String clientParam");
				@SuppressWarnings("unchecked")
				List<ChannelInfo> cInfo = (List<ChannelInfo>) q.execute(clientId);
				String token = cInfo.get(0).getToken();
				// Lock document, persist it, and notify the relevant client.
				doc = lockDocument(doc, token, pm, tasks);
				messages.put(clientId, Messages.LOCK_READY + docKey);

				// Notify the rest that the number of people left has changed.
				int i = 1;
				while (clientsItr.hasNext()) {
					clientId = clientsItr.next();
					messages.put(clientId, Messages.LOCK_NOT_READY + 
							String.valueOf(i) + DELIMITER + docKey);
					i++;
				}
			}
		}
	}

	private static Document lockDocument(Document doc, String token,
			PersistenceManager pm, ArrayList<TaskOptions> tasks) {

		// Lock document by setting the lockedUntil and lockedBy fields.
		doc.setLockedBy(token);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, LOCK_TIMEOUT);
		doc.setLockedUntil(cal.getTime());

		// Set up scheduled task so that after a timeout period, we just
		// move onto the next person in the queue.
		String taskName = UUID.randomUUID().toString();
		doc.setLockExpirationTaskName(taskName);
		tasks.add(TaskOptions.Builder.withCountdownMillis(LOCK_TIMEOUT * 1000)
				.url("/task/lockExpiration")
				.param("docKey", KeyFactory.keyToString(doc.getKey()))
				.taskName(taskName));

		return doc;
	}

	static void sendMessage(String clientId, String message) {
		ChannelService channelService = ChannelServiceFactory.getChannelService();
		channelService.sendMessage(new ChannelMessage(clientId, message));
	}

	static void sendMessageByToken(String token, String message) {
		PersistenceManager pm = PMF.get().getPersistenceManager();
		String clientId = pm.getObjectById(ChannelInfo.class, token).getClient();
		sendMessage(clientId, message);
	}

	private static void removeClientFromTimeoutQueue(String docKey, PersistenceManager pm) {
		if (docKey != null) {
			String taskName = pm.getObjectById(Document.class, docKey)
					.getLockExpirationTaskName();
			if (taskName != null) {
				QueueFactory.getDefaultQueue().deleteTask(taskName);
			}
		}
	}
}
