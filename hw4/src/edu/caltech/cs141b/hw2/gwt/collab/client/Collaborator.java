package edu.caltech.cs141b.hw2.gwt.collab.client;

import java.util.ArrayList;

import com.google.gwt.appengine.channel.client.Channel;
import com.google.gwt.appengine.channel.client.ChannelFactory;
import com.google.gwt.appengine.channel.client.ChannelFactory.ChannelCreatedCallback;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.TabBar;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;

/**
 * Main class for a single Collaborator widget.
 */
public class Collaborator extends Composite implements ClickHandler, ChangeHandler {

	public static final boolean SHOW_CONSOLE = true;
	public static final int THINKING_RANGE_START = 100;  // ms
	public static final int THINKING_RANGE_END = 2000;
	public static final int EATING_RANGE_START = 100;
	public static final int EATING_RANGE_END = 200;

	CollaboratorServiceAsync collabService;

	// UI elements:
	VerticalPanel statusArea = new VerticalPanel();
	HTML queueStatus = new HTML();
	HorizontalPanel hp = new HorizontalPanel();
	TabPanel tp = new TabPanel();
	TabBar tb = tp.getTabBar();
	ListBox documentList = new ListBox();
	// Buttons for managing available documents.
	PushButton refreshList = new PushButton(
			new Image("images/refresh_small.png"));
	PushButton createNew = new PushButton(
			new Image("images/plus_small.png"));
	// Buttons for displaying document information and editing document content.
	PushButton refreshDoc = new PushButton(
			new Image("images/refresh.png"));
	PushButton lockButtonUnlocked = new PushButton(
			new Image("images/locked.png"));
	PushButton lockButtonLocked = new PushButton(
			new Image("images/unlocked.png"));
	PushButton lockButtonRequesting = new PushButton(
			new Image("images/loading.gif"));
	PushButton saveButton = new PushButton(
			new Image("images/save.png"));
	PushButton closeButton = new PushButton(
			new Image("images/close.png"));
	ToggleButton simulateButton = new ToggleButton(
			new Image("images/play_button.png"),
			new Image("images/pause_button.gif"));

	// Callback objects.
	DocLister lister = new DocLister(this);
	DocReader reader = new DocReader(this);
	DocRequestor requestor = new DocRequestor(this);
	DocUnrequestor unrequestor = new DocUnrequestor(this);
	DocReaderLocked lockedReader = new DocReaderLocked(this);
	DocReleaser releaser = new DocReleaser(this);
	DocSaver saver = new DocSaver(this);
	DocCreator creator = new DocCreator(this);
	ChannelCreator channelCreator = new ChannelCreator(this);

	// Variables for keeping track of current states of the application.
	ArrayList<String> tabKeys = new ArrayList<String>();
	ArrayList<RichTextArea> tabContents = new ArrayList<RichTextArea>();
	ArrayList<TextBox> tabTitles = new ArrayList<TextBox>();
	ArrayList<Integer> tabQueueLengths = new ArrayList<Integer>();
	ArrayList<UiState> uiStates = new ArrayList<UiState>();
	boolean channelIsSetup = false;
	String channelToken = null;
	boolean simulating = false;
	Timer thinkingTimer = null;
	Timer eatingTimer = null;

	/**
	 * UI initialization.
	 * 
	 * @param collabService
	 */
	public Collaborator(CollaboratorServiceAsync collabService) {
		this.collabService = collabService;

		// outerHp is our horizontal panel that includes the majority of the page.
		HorizontalPanel outerHp = new HorizontalPanel();
		outerHp.setWidth("100%");
		outerHp.setHeight("100%");

		// leftColVp holds our document list and console.
		VerticalPanel leftColVp = new VerticalPanel();
		leftColVp.add(new HTML("<h2>Docs</h2>"));

		// docsButtonsHp holds relevant buttons (refresh / create new).
		HorizontalPanel docsButtonsHp = new HorizontalPanel();
		docsButtonsHp.add(refreshList);
		docsButtonsHp.add(createNew);
		leftColVp.add(docsButtonsHp);

		// docsVp holds document list and relevant buttons (refresh / create new).
		documentList.setStyleName("doc-list");
		leftColVp.add(documentList);
		leftColVp.setStyleName("list-column");

		// Add console to leftColVp.
		if (SHOW_CONSOLE) {
			statusArea.setSpacing(10);
			statusArea.add(new HTML("<h2>Console</h2>"));
			leftColVp.add(statusArea);
		}

		// We are done packing leftColVp, so add it to outerHp.
		outerHp.add(leftColVp);

		// Now let's work on the right side of the page, which will include
		// the tabPanel for documents (as well as some relevant buttons)
		VerticalPanel rightColVp = new VerticalPanel();

		// Create horizontal panel that holds the document-specific buttons.
		hp.setSpacing(10);
		hp.add(refreshDoc);
		hp.add(lockButtonUnlocked);
		hp.add(saveButton);
		hp.add(closeButton);
		hp.add(simulateButton);
		hp.add(queueStatus);
		rightColVp.add(hp);

		// Add tab panel to rightColVp.
		tp.setWidth("100%");
		rightColVp.add(tp);
		rightColVp.setWidth("100%");
		rightColVp.setHeight("100%");
		rightColVp.setStyleName("doc-column");

		outerHp.add(rightColVp);

		// Handlers code starts here:
		// Adding selection handler to tab panel. Note that tabTitles
		// and tabContents should be updated before the tab selection occurs.
		tp.addSelectionHandler(new SelectionHandler<Integer>() {
			public void onSelection(SelectionEvent<Integer> event) {
				// Changes UI to update to the current selected tab.
				int currentTabInd = tb.getSelectedTab();
				setUiStateIfNoSim(uiStates.get(currentTabInd));
			}
		});

		refreshList.addClickHandler(this);
		createNew.addClickHandler(this);
		refreshDoc.addClickHandler(this);
		lockButtonUnlocked.addClickHandler(this);
		lockButtonLocked.addClickHandler(this);
		lockButtonRequesting.addClickHandler(this);
		saveButton.addClickHandler(this);
		closeButton.addClickHandler(this);
		simulateButton.addClickHandler(this);

		documentList.addChangeHandler(this);
		documentList.setVisibleItemCount(10);

		setUiStateIfNoSim(UiState.NOT_VIEWING);
		initWidget(outerHp);

		// Make initial necessary calls to server.
		channelCreator.createChannel();
		lister.getDocumentList();
		
		// Initialize timers.
		thinkingTimer = new Timer() {
			public void run() {
				// When time is up, become hungry. That is, request the lock.
				requestor.requestDocument(tabKeys.get(tb.getSelectedTab()));
			}
		};
		eatingTimer = new Timer() {
			public void run() {
				// When time is up, go back to thinking. That is, save the document.
				int currentTabInd = tb.getSelectedTab();
				LockedDocument lockedDoc = new LockedDocument(null, null,
						tabKeys.get(currentTabInd), tabTitles.get(currentTabInd).getValue(), 
						tabContents.get(currentTabInd).getHTML());
				saver.saveDocument(lockedDoc);
			}
		};
	}

	/* (non-Javadoc)
	 * Receives button events.
	 * @see com.google.gwt.event.dom.client.ClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
	 */
	@Override
	public void onClick(ClickEvent event) {
		// Channel is not set up yet, so don't let the user do anything.
		if (!channelIsSetup) {
			Window.alert("Please wait while the channel is established. " +
					"If this takes more than a few seconds, try refreshing the page.");
			return;
		}
		
		Object source = event.getSource();
		if (source.equals(refreshList)) {
			lister.getDocumentList();
		} else if (source.equals(createNew)) {
			creator.createDocument();
		} else if (source.equals(refreshDoc)) {
			reader.getDocument(tabKeys.get(tb.getSelectedTab()));
		} else if (source.equals(lockButtonUnlocked)) {
			requestor.requestDocument(tabKeys.get(tb.getSelectedTab()));
		} else if (source.equals(lockButtonLocked)) {
			int currentTabInd = tb.getSelectedTab();
			LockedDocument lockedDoc = new LockedDocument(null, null,
					tabKeys.get(currentTabInd),
					tabTitles.get(currentTabInd).getValue(),
					tabContents.get(currentTabInd).getHTML());
			releaser.releaseLock(lockedDoc);
		} else if (source.equals(lockButtonRequesting)) {
			int currentTabInd = tb.getSelectedTab();
			unrequestor.unrequestDocument(tabKeys.get(currentTabInd));
		} else if (source.equals(saveButton)) {
			// Make async call to save the document (also updates UI).
			int currentTabInd = tb.getSelectedTab();
			LockedDocument lockedDoc = new LockedDocument(null, null,
					tabKeys.get(currentTabInd), tabTitles.get(currentTabInd).getValue(), 
					tabContents.get(currentTabInd).getHTML());
			saver.saveDocument(lockedDoc);
		} else if (source.equals(closeButton)) {
			// Release locks according to state.
			int currentTabInd = tb.getSelectedTab();
			UiState state = uiStates.get(currentTabInd);
			if (state == UiState.LOCKED || state == UiState.LOCKING) {
				LockedDocument lockedDoc = new LockedDocument(null, null,
						tabKeys.get(currentTabInd),
						tabTitles.get(currentTabInd).getValue(),
						tabContents.get(currentTabInd).getHTML());
				releaser.releaseLock(lockedDoc);
			} else if (state == UiState.REQUESTING) {
				unrequestor.unrequestDocument(tabKeys.get(currentTabInd));
			}
			// Update UI and corresponding variables.
			removeTabAtInd(currentTabInd);
		} else if (source.equals(simulateButton)) {
			simulating = !simulating;
			if (simulating) {
				statusUpdate("Simulating...");
				setUiState(UiState.SIMULATING);
				// Disable additional UI elements like the doc list, etc.
				documentList.setEnabled(false);
				for (int i = 0; i < tb.getTabCount(); i++) {
					tb.setTabEnabled(i, false);
				}

				simulateThinking();
			} else {
				thinkingTimer.cancel();
				eatingTimer.cancel();
				
				statusUpdate("Stopped simulating.");
				int currentTabInd = tb.getSelectedTab();
				setUiState(uiStates.get(currentTabInd));
				// Enable UI elements.
				documentList.setEnabled(true);
				for (int i = 0; i < tb.getTabCount(); i++) {
					tb.setTabEnabled(i, true);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * Intercepts events from the list box.
	 * @see com.google.gwt.event.dom.client.ChangeHandler#onChange(com.google.gwt.event.dom.client.ChangeEvent)
	 */
	@Override
	public void onChange(ChangeEvent event) {
		if (event.getSource().equals(documentList)) {
			String key = documentList.getValue(documentList.getSelectedIndex());
			loadDoc(key);
		}
	}

	void loadDoc(String key) {
		// If it's already open in a tab, save the location of the tab
		// and retrieve the title and contents from that one.
		int savedLoc = tabKeys.indexOf(key);
		if (savedLoc != -1) {
			// Select the appropriate tab; this should fire the SelectionHandler.
			tp.selectTab(savedLoc);
		} else {
			addNewTab(key);
			reader.getDocument(key);
		}
	}

	void setUpChannel() {
		// Establish the channel handlers with our given channel token.
		ChannelFactory.createChannel(channelToken, new ChannelCreatedCallback() {
			@Override
			public void onChannelCreated(Channel channel) {
				channel.open(new ChannelMessageListener(Collaborator.this));
			}
		});
	}

	/**
	 * Updates relevant state-capturing variables and updates UI
	 * if necessary.
	 */
	void updateVarsAndUi(String key, String title, 
			String contents, UiState state) {
		// Update local data structures.
		int indResult = tabKeys.indexOf(key);
		if (indResult == -1) {
			return;
		}
		tabTitles.get(indResult).setValue(title);
		tabContents.get(indResult).setHTML(contents);
		uiStates.set(indResult, state);
		int currentTabInd = tb.getSelectedTab();
		if (key.equals(tabKeys.get(currentTabInd))) {
			setUiStateIfNoSim(state);
		}
	}

	/**
	 * Just update the state corresponding to the key.
	 */
	void updateVarsAndUi(String key, UiState state) {
		// Update local data structures.
		int indResult = tabKeys.indexOf(key);
		if (indResult == -1) {
			return;
		}
		uiStates.set(indResult, state);
		int currentTabInd = tb.getSelectedTab();
		if (key.equals(tabKeys.get(currentTabInd))) {
			setUiStateIfNoSim(state);
		}
	}

	/**
	 * Sets the UI state if the program is not in a simulating state.
	 * 
	 * @param state the UI state to switch to (as defined in UiState.java)
	 */
	void setUiStateIfNoSim(UiState state) {
		if (simulating) {
			return;
		}
		setUiState(state);
	}

	/**
	 * Resets the state of the buttons and edit objects to the specified state.
	 * The state of these objects is modified by requesting or obtaining locks
	 * and trying to or successfully saving.
	 * 
	 * @param state the UI state to switch to (as defined in UiState.java)
	 */
	void setUiState(UiState state) {
		refreshDoc.setEnabled(state.refreshDocEnabled);
		lockButtonUnlocked.setEnabled(state.lockButtonUnlockedEnabled);
		lockButtonLocked.setEnabled(state.lockButtonLockedEnabled);
		saveButton.setEnabled(state.saveButtonEnabled);
		closeButton.setEnabled(state.closeButtonEnabled);
		simulateButton.setEnabled(state.simulateButtonEnabled);
		if (tabIsSelected()) {
			int currentTabInd = tb.getSelectedTab();
			tabTitles.get(currentTabInd).setEnabled(state.titleEnabled);
			tabContents.get(currentTabInd).setEnabled(state.contentsEnabled);
		}

		// Handle UI changes for the queue status panel.
		String statusString = "";
		if (state.lockState == UiState.LockButton.LOCKED) {
			statusString = "<br />Lock obtained";
			hp.remove(lockButtonUnlocked);
			hp.remove(lockButtonRequesting);
			hp.insert(lockButtonLocked, 1);
		} else if (state.lockState == UiState.LockButton.UNLOCKED) {
			statusString = "<br />No lock";
			hp.remove(lockButtonLocked);
			hp.remove(lockButtonRequesting);
			hp.insert(lockButtonUnlocked, 1);
		} else if (state.lockState == UiState.LockButton.REQUESTING) {
			int currentTabInd = tb.getSelectedTab();
			int numPeopleLeft = tabQueueLengths.get(currentTabInd);
			if (currentTabInd != -1 && numPeopleLeft != -1) {
				statusString = "<br />Position " + numPeopleLeft + " in line";
			}
			hp.remove(lockButtonLocked);
			hp.remove(lockButtonUnlocked);
			hp.insert(lockButtonRequesting, 1);
		}
		queueStatus.setHTML(statusString);
	}

	/**
	 * Adds status lines to the console window to enable transparency of the
	 * underlying processes.
	 * 
	 * @param status the status to add to the console window
	 */
	void statusUpdate(String status) {
		while (statusArea.getWidgetCount() > 6) {
			statusArea.remove(1);
		}
		final HTML statusUpd = new HTML(status);
		statusArea.add(statusUpd);
	}

	void simulateThinking() {
		int sleepRange = THINKING_RANGE_END - THINKING_RANGE_START + 1;
		int waitingTime = 
				(int) (THINKING_RANGE_START + sleepRange * Math.random()) + 1;
		thinkingTimer.schedule(waitingTime);
	}

	void simulateEating() {
		int currentInd = tb.getSelectedTab();
		// Write client's token into the document.
		tabContents.get(currentInd).setHTML(
				tabContents.get(currentInd).getHTML() + channelToken + "<br />");

		int eatRange = EATING_RANGE_END - EATING_RANGE_START + 1;
		int waitingTime = (int) (EATING_RANGE_START + eatRange * Math.random()) + 1;
		eatingTimer.schedule(waitingTime);
	}
	
	boolean tabIsSelected() {
		int currentTabInd = tb.getSelectedTab();
		return currentTabInd >= 0 && currentTabInd < tabKeys.size();
	}

	void addNewTab(String key) {
		TextBox title = new TextBox();
		RichTextArea contents = new RichTextArea();

		// Update local variables.
		tabKeys.add(key);
		tabTitles.add(title);
		tabContents.add(contents);
		tabQueueLengths.add(-1);
		uiStates.add(UiState.VIEWING);
		setUiStateIfNoSim(UiState.VIEWING);

		// Update TabPanel's UI:
		HorizontalPanel tabHeader = new HorizontalPanel();
		tabHeader.add(title);
		tp.add(contents, tabHeader);
		// Select the last tab for the user.
		tp.selectTab(tb.getTabCount() - 1);
	}

	private void removeTabAtInd(int i) {
		// Update local data structures
		tabKeys.remove(i);
		tabTitles.remove(i);
		tabContents.remove(i);
		tabQueueLengths.remove(i);
		uiStates.remove(i);

		// Update tab panel
		tp.remove(i);
		int tabCount = tb.getTabCount();
		if (tabCount > 0) {
			if (i > tabCount - 1) {
				tp.selectTab(tabCount - 1);
			} else {
				tb.selectTab(i);
			}
		} else {
			setUiStateIfNoSim(UiState.NOT_VIEWING);
		}
	}
}
