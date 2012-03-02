package edu.caltech.cs141b.hw2.gwt.collab.shared;

import java.util.Date;

/**
 * Thrown when the provided lock objects cannot be used any longer.
 */
public class LockExpired extends Exception {

	private boolean wrongCredentials = false;
	private Date lockedUntil = null;
	private String key = null;
	private String credentials = null;
	
	private static final long serialVersionUID = 7796506690276524937L;
	
	public LockExpired() {
		
	}

	public LockExpired(String message) {
		super(message);
	}
	
	public LockExpired(boolean wrongCredentials, Date lockedUntil, String key,
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

