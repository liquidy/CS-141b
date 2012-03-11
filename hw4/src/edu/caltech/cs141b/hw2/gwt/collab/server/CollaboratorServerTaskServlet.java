package edu.caltech.cs141b.hw2.gwt.collab.server;

import java.io.IOException;
import java.util.logging.Logger;

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

		resp.setContentType("text/plain");
		
		String methodName = req.getParameter("methodName");
		if (methodName.equals("requestDocument")) {
			log.info("Processing task: methodName: " + methodName);
			String docKey = req.getParameter("docKey");
			String token = req.getParameter("token"); 
			CollaboratorServer.requestDocument(docKey, token);
			resp.getWriter().write("Success!");
		} else if (methodName.equals("saveDocument")) {
			log.info("Processing task: methodName: " + methodName);
			String docKey = req.getParameter("docKey");
			String token = req.getParameter("token");
			try {
				LockedDocument lockedDoc = CollaboratorServer.getLockedDocument(docKey, token);
				CollaboratorServer.saveDocument(lockedDoc, token);
				resp.getWriter().write("Success!");
				CollaboratorServer.sendMessageByToken(token, Messages.DOC_SAVED + docKey);
			} catch (LockExpired ex) {
				resp.getWriter().write("Lock expired");
				CollaboratorServer.sendMessageByToken(token, Messages.LOCK_EXPIRED + docKey);
			}
		}
	}
}
