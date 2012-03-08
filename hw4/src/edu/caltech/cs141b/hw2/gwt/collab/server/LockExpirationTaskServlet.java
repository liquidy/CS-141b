package edu.caltech.cs141b.hw2.gwt.collab.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Hashtable;

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

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		resp.setContentType("text/plain");
		String docKey = req.getParameter("docKey");

		// Poll the relevant doc queue.
		PersistenceManager pm = PMF.get().getPersistenceManager();
		int retries = CollaboratorServiceImpl.NUM_RETRIES;
		while (true) {
			Hashtable<String, String> messages = new Hashtable<String, String>();
			ArrayList<TaskOptions> tasks = new ArrayList<TaskOptions>();
			Transaction txn = pm.currentTransaction();
			try {
				txn.begin();
				
				Key key = KeyFactory.stringToKey(docKey);
				Document persistedDoc = pm.getObjectById(Document.class, key);
				CollaboratorServiceImpl.pollDocQueue(persistedDoc, true, pm, messages, tasks);
				pm.makePersistent(persistedDoc);
				
				txn.commit();
				
				resp.getWriter().write("Success!");
				return;
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
}
