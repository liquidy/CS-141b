package edu.caltech.cs141b.hw2.gwt.collab.shared;

import java.util.Date;

/**
 * Thrown when a lock cannot currently be retrieved from the server for the
 * specified document.  The server should return when the lock is available
 * in the exception message.
 */
public class LockUnavailable extends Exception {

	private boolean wrongCredentials = false;
	private Date lockedUntil = null;
	private String key = null;
	private String credentials = null;
	
	private static final long serialVersionUID = -8039330302911776861L;
	
	public LockUnavailable() {
		
	}
	
	public LockUnavailable(String message) {
		super(message);
	}
	
	public LockUnavailable(boolean wrongCredentials, Date lockedUntil, String key,
			String credentials) {
		
		super("Doc key: " + key + 
				", Locked until: " + lockedUntil + 
				", Wrong credentials: " + wrongCredentials +
				", Locked by: " + credentials);
		
		this.wrongCredentials = wrongCredentials;
		this.lockedUntil = lockedUntil;
		this.key = key;
		this.credentials = credentials;
	}
	
	public boolean getWrongCredentials() {
		return wrongCredentials;
	}
	
	public Date getLockedUntil() {
		return lockedUntil;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getCredentials() {
		return credentials;
	}
}

