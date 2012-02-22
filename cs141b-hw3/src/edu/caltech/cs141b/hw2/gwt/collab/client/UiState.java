package edu.caltech.cs141b.hw2.gwt.collab.client;

/**
 * Stores the state of a commonly used UI configuration (per doc); for each
 * component there's a boolean value that is true if enabled and false if 
 * disabled.
 */
public enum UiState {
	/**
	 * Format is (refreshDoc, lockButtonUnlocked, lockButtonLocked,
	 * lockButtonRequesting, saveButton, closeButton, simulateButton,
	 * title, contents, lockButtonState).
	 */
	NOT_VIEWING (false, false, false, false, false, false, false, false, false, LockButton.UNLOCKED),
	VIEWING (true, true, false, false, false, true, true, false, false, LockButton.UNLOCKED),
	REQUESTING (false, false, false, true, false, true, false, false, false, LockButton.REQUESTING),
	LOCKING (false, false, false, false, false, false, false, false, false, LockButton.UNLOCKED),
	LOCKED (false, false, true, false, true, true, false, true, true, LockButton.LOCKED),
	RELEASING (false, false, false, false, false, false, false, false, false, LockButton.LOCKED),
	SAVING (false, false, false, false, false, false, false, false, false, LockButton.LOCKED),
	SIMULATING (false, false, false, false, false, false, true, false, false, LockButton.UNLOCKED);
	
	public boolean refreshDocEnabled;
	public boolean lockButtonUnlockedEnabled;
	public boolean lockButtonLockedEnabled;
	public boolean lockButtonRequestingEnabled;
	public boolean saveButtonEnabled;
	public boolean closeButtonEnabled;
	public boolean simulateButtonEnabled;
	public boolean titleEnabled;
	public boolean contentsEnabled;
	public LockButton lockState;
	
	UiState(boolean refreshDocEnabled,
			boolean lockButtonUnlockedEnabled,
			boolean lockButtonLockedEnabled,
			boolean lockButtonRequestingEnabled,
			boolean saveButtonEnabled,
			boolean closeButtonEnabled,
			boolean simulateButtonEnabled,
			boolean titleEnabled,
			boolean contentsEnabled,
			LockButton lockState) {
		
		this.refreshDocEnabled = refreshDocEnabled;
		this.lockButtonUnlockedEnabled = lockButtonUnlockedEnabled;
		this.lockButtonLockedEnabled = lockButtonLockedEnabled;
		this.lockButtonRequestingEnabled = lockButtonRequestingEnabled;
		this.saveButtonEnabled = saveButtonEnabled;
		this.closeButtonEnabled = closeButtonEnabled;
		this.simulateButtonEnabled = simulateButtonEnabled;
		this.titleEnabled = titleEnabled;
		this.contentsEnabled = contentsEnabled;
		this.lockState = lockState;
	}
	
	public enum LockButton {
		LOCKED,
		UNLOCKED,
		REQUESTING;
	}
}
