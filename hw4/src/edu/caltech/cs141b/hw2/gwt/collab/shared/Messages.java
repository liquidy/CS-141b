package edu.caltech.cs141b.hw2.gwt.collab.shared;

public class Messages {

	/**
	 * Doc is ready to be locked. The rest of the string is doc ID.
	 */
	public static final char LOCK_READY = 'A';
	
	/**
	 * Doc is not ready to be locked. The rest of the string is structured as
	 * follows: numPeopleLeft + ":" + docId.
	 */
	public static final char LOCK_NOT_READY = 'B';
	
	/**
	 * The user has taken too long and the lock has expired.
	 */
	public static final char LOCK_EXPIRED = 'C';
	
	/**
	 * The document has successfully been saved.
	 */
	public static final char DOC_SAVED = 'D';
}
