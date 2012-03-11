package edu.caltech.cs141b.hw2.gwt.collab.server;

import java.io.IOException;
import java.util.logging.Logger;

import javax.jdo.JDODataStoreException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.caltech.cs141b.hw2.gwt.collab.shared.LockExpired;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;
import edu.caltech.cs141b.hw2.gwt.collab.shared.Messages;

@SuppressWarnings("serial")
public class CollaboratorServerTaskServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(CollaboratorServerTaskServlet.class.getName());
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		boolean successfulExecution = false;
		String methodName = req.getParameter("methodName");
		if (methodName.equals("requestDocument")) {
			successfulExecution = handleRequestDocument(req);
		} else if (methodName.equals("saveDocument")) {
			successfulExecution = handleSaveDocument(req);
		}
		
		if (successfulExecution) {
			log.info("Processed task successfuly: methodName: " + methodName);
			resp.setStatus(HttpServletResponse.SC_OK);
		} else {
			log.warning("Failed to process task successfuly: methodName: " + methodName);
			resp.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT, "Error occurred. Will retry task later...");
		}
	}
	
	private static boolean handleRequestDocument(HttpServletRequest req) {
		String docKey = req.getParameter("docKey");
		String token = req.getParameter("token");
		try {
			CollaboratorServer.requestDocument(docKey, token);
			return true;
		} catch (JDODataStoreException e) {
			log.warning("Putting requestDocument task back into task queue, docKey: " + docKey + ", token:" + token);
			return false;
		}
	}
	
	private static boolean handleSaveDocument(HttpServletRequest req) {
		String docKey = req.getParameter("docKey");
		String docTitle = req.getParameter("docTitle");
		String docContents = req.getParameter("docContents");
		String token = req.getParameter("token");
		try {
			CollaboratorServer.saveDocument(new LockedDocument(null, null, docKey, docTitle, docContents), token);
			CollaboratorServer.sendMessageByToken(token, Messages.DOC_SAVED + docKey);
			return true;
		} catch (JDODataStoreException e) {
			log.warning("Putting saveDocument task back into task queue, docKey: " + docKey + ", token:" + token);
			return false;
		} catch (LockExpired ex) {
			CollaboratorServer.sendMessageByToken(token, Messages.LOCK_EXPIRED + docKey);
			return true;
		}
	}
}
