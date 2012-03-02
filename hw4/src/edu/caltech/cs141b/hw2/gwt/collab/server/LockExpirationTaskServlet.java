package edu.caltech.cs141b.hw2.gwt.collab.server;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class LockExpirationTaskServlet extends HttpServlet {
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		resp.setContentType("text/plain");
		String docKey = req.getParameter("docKey");
		
		CollaboratorServiceCommon collabCommon = CollaboratorServiceCommon.getInstance();
		collabCommon.pollDocQueue(docKey);
		
		resp.getWriter().write("Success!");
	}
}
