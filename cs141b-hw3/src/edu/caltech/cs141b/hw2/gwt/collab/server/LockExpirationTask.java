package edu.caltech.cs141b.hw2.gwt.collab.server;

import com.google.appengine.api.taskqueue.DeferredTask;

public class LockExpirationTask implements DeferredTask {
	
	private String documentKey;
	
	public LockExpirationTask() {}
	
	public LockExpirationTask(String documentKey) {
		this.documentKey = documentKey;
	}

	@Override
	public void run() {
		
	}
}
