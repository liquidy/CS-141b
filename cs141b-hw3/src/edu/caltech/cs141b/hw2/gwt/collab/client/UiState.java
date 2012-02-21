package edu.caltech.cs141b.hw2.gwt.collab.client;

/**
 * Stores the state of a commonly used UI configuration (per doc); for each
 * component there's a boolean value that is true if enabled and false if 
 * disabled.
 */
public enum UiState {
	/**
	 * Format is (refreshDoc, lockButtonUnlocked, lockButtonLocked, saveButton, 
	 * closeButton, title, contents, lockButtonState).
	 */
	NOT_VIEWING (false, false, false, false, false, false, false, LockState.UNLOCKED),
	VIEWING (true, true, false, false, true, false, false, LockState.UNLOCKED),
	LOCKED (false, false, true, true, true, true, true, LockState.LOCKED),
	LOCKING (false, false, false, false, false, false, false, LockState.UNLOCKED),
	REQUESTING (false, false, false, false, true, false, false, LockState.REQUESTING),
	SAVING (false, false, false, false, false, false, false, LockState.LOCKED);
	
	public boolean refreshDocEnabled;
	public boolean lockButtonUnlockedEnabled;
	public boolean lockButtonLockedEnabled;
	public boolean saveButtonEnabled;
	public boolean closeButtonEnabled;
	public boolean titleEnabled;
	public boolean contentsEnabled;
	public LockState lockState;
	
	UiState(boolean refreshDocEnabled,
			boolean lockButtonUnlockedEnabled,
			boolean lockButtonLockedEnabled,
			boolean saveButtonEnabled,
			boolean closeButtonEnabled,
			boolean titleEnabled,
			boolean contentsEnabled,
			LockState lockState) {
		
		this.refreshDocEnabled = refreshDocEnabled;
		this.lockButtonUnlockedEnabled = lockButtonUnlockedEnabled;
		this.lockButtonLockedEnabled = lockButtonLockedEnabled;
		this.saveButtonEnabled = saveButtonEnabled;
		this.closeButtonEnabled = closeButtonEnabled;
		this.titleEnabled = titleEnabled;
		this.contentsEnabled = contentsEnabled;
		this.lockState = lockState;
	}
	
	public enum LockState {
		LOCKED,
		UNLOCKED,
		REQUESTING;
	}
}
