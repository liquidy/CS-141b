package edu.caltech.cs141b.hw2.gwt.collab.client;

/**
 * Stores the state of a commonly used UI configuration (per doc); for each
 * component there's a boolean value that is true if enabled and false if 
 * disabled.
 */
public enum UiState {
	/**
	 * Format is (refreshDoc, lockButton, saveButton, closeButton, title, contents).
	 */
	NOT_VIEWING (false, false, false, false, false, false),
	VIEWING (true, true, false, true, false, false),
	LOCKED (false, false, true, true, true, true),
	LOCKING (false, false, false, false, false, false),
	REQUESTING (false, false, false, true, false, false),
	SAVING (false, false, false, false, false, false);
	
	public boolean refreshDocEnabled;
	public boolean lockButtonEnabled;
	public boolean saveButtonEnabled;
	public boolean closeButtonEnabled;
	public boolean titleEnabled;
	public boolean contentsEnabled;
	
	UiState(boolean refreshDocEnabled,
			boolean lockButtonEnabled,
			boolean saveButtonEnabled,
			boolean closeButtonEnabled,
			boolean titleEnabled,
			boolean contentsEnabled) {
		
		this.refreshDocEnabled = refreshDocEnabled;
		this.lockButtonEnabled = lockButtonEnabled;
		this.saveButtonEnabled = saveButtonEnabled;
		this.closeButtonEnabled = closeButtonEnabled;
		this.titleEnabled = titleEnabled;
		this.contentsEnabled = contentsEnabled;
	}
	
}
