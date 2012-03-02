package edu.caltech.cs141b.hw2.gwt.collab.shared;

import java.util.Date;

/**
 * Thrown when a lock cannot currently be retrieved from the server for the
 * specified document.  The server should return when the lock is available
 * in the exception message.
 */
public class LockUnavailable extends Exception {

	private boolean frontOfQueue = false;
	private Date lockedUntil = null;
	private String key = null;
	private String lockedBy = null;
	
	private static final long serialVersionUID = -8039330302911776861L;
	
	public LockUnavailable() {
	}
	
	public LockUnavailable(String message) {
		super(message);
	}
	
	public LockUnavailable(boolean frontOfQueue,
			String key,
			Date lockedUntil,
			String lockedBy) {
		
		this.frontOfQueue = frontOfQueue;
		this.lockedUntil = lockedUntil;
		this.key = key;
		this.lockedBy = lockedBy;
	}
	
	public boolean frontOfQueue() {
		return frontOfQueue;
	}
	
	public Date getLockedUntil() {
		return lockedUntil;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getLockedBy() {
		return lockedBy;
	}
}

