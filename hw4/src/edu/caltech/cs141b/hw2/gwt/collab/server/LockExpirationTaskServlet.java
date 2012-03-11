package edu.caltech.cs141b.hw2.gwt.collab.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.jdo.JDODataStoreException;
import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

@SuppressWarnings("serial")
public class LockExpirationTaskServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(LockExpirationTaskServlet.class.getName());
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		String docKey = req.getParameter("docKey");
		boolean successfulExecution = handleLockExpired(docKey);
		if (successfulExecution) {
			resp.setStatus(HttpServletResponse.SC_OK);
		} else {
			resp.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT, "Error occurred. Will retry task later...");
		}
	}

	private static boolean handleLockExpired(String docKey) {
		Hashtable<String, String> messages = new Hashtable<String, String>();
		ArrayList<TaskOptions> tasks = new ArrayList<TaskOptions>();
		PersistenceManager pm = PMF.get().getPersistenceManager();
		Transaction txn = pm.currentTransaction();
		try {
			txn.begin();

			Key key = KeyFactory.stringToKey(docKey);
			Document persistedDoc = pm.getObjectById(Document.class, key);
			CollaboratorServer.pollDocQueue(persistedDoc, true, pm, messages, tasks);
			pm.makePersistent(persistedDoc);

			txn.commit();

			return true;
		} catch (JDODataStoreException e) {
			log.warning("Putting lockExpiration task back into task queue, docKey: " + docKey);
			return false;
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			} else {
				// Send out any relevant messages if the transaction went through.
				for (String client : messages.keySet()) {
					ChannelService channelService = ChannelServiceFactory.getChannelService();
					channelService.sendMessage(new ChannelMessage(client, messages.get(client)));
				}
				// Enqueue any relevant tasks if the transaction went through.
				for (TaskOptions task : tasks) {
					QueueFactory.getDefaultQueue().add(task);
				}
			}
			pm.close();
		}
	}
}
