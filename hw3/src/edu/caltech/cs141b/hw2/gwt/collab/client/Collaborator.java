package edu.caltech.cs141b.hw2.gwt.collab.client;

import java.util.ArrayList;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
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
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;

/**
 * Main class for a single Collaborator widget.
 */
public class Collaborator extends Composite implements ClickHandler, ChangeHandler {
	
	public static final boolean DEBUG = false;
	
	protected CollaboratorServiceAsync collabService;
	
	// Managing available documents.
	protected ListBox documentList = new ListBox();
	protected PushButton refreshList = new PushButton(
			new Image("images/refresh_small.png"));
	protected PushButton createNew = new PushButton(
			new Image("images/plus_small.png"));
	
	// For displaying document information and editing document content.
	protected PushButton refreshDoc = new PushButton(
			new Image("images/refresh.png"));
	protected PushButton lockButton = new PushButton(
			new Image("images/locked.png"));
	protected PushButton saveButton = new PushButton(
			new Image("images/save.png"));
	protected PushButton closeButton = new PushButton(
			new Image("images/close.png"));
	
	// Callback objects.
	protected DocLister lister = new DocLister(this);
	protected DocReader reader = new DocReader(this);
	private DocLocker locker = new DocLocker(this);
	protected DocReleaser releaser = new DocReleaser(this);
	private DocSaver saver = new DocSaver(this);
	private DocCreator creator = new DocCreator(this);
	
	// Status tracking.
	private VerticalPanel statusArea = new VerticalPanel();
	
	protected HTML statusDisplay = new HTML();
	private TabPanel tp = new TabPanel();
	private TabBar tb = tp.getTabBar();
	
	// Variables for keeping track of current states of the application.
	protected int currentTabInd = -1;
	protected ArrayList<String> tabKeys = new ArrayList<String>();
	protected ArrayList<RichTextArea> tabContents = new ArrayList<RichTextArea>();
	protected ArrayList<TextBox> tabTitles = new ArrayList<TextBox>();
	protected ArrayList<UiState> uiStates = new ArrayList<UiState>();
	
	// to control tab overflow
	private boolean tooManyTabs = false;
	
	private String changedKey = null;
	
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
		documentList.setWidth("400");
		leftColVp.setStyleName("list-column");
		
		// Add console to leftColVp.
		if (DEBUG) {
				statusArea.setSpacing(10);
				statusArea.add(new HTML("<h2>Console</h2>"));
				leftColVp.add(statusArea);
		}
		
		// We are done packing leftColVp, so add it to outerHp.
		outerHp.add(leftColVp);
		
		// Now let's work on the right side of the page, which will include
		// the tabPanel for documents (as well as some relevant buttons)
		VerticalPanel rightColVp = new VerticalPanel();

		//horizontal panel to show display
		HorizontalPanel statusDisplayPanel = new HorizontalPanel();
		statusDisplayPanel.add(statusDisplay);
		rightColVp.add(statusDisplayPanel);
		// Create horizontal panel that holds the document-specific buttons.
		HorizontalPanel hp = new HorizontalPanel();
		hp.setSpacing(10);
		hp.add(refreshDoc);
		hp.add(lockButton);
		hp.add(saveButton);
		hp.add(closeButton);
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
				currentTabInd = event.getSelectedItem();
				setUiToState(uiStates.get(currentTabInd));
			}
		});
		
		refreshList.addClickHandler(this);
		createNew.addClickHandler(this);
		refreshDoc.addClickHandler(this);
		lockButton.addClickHandler(this);
		saveButton.addClickHandler(this);
		closeButton.addClickHandler(this);
		
		documentList.addChangeHandler(this);
		documentList.setVisibleItemCount(10);
		
		setUiToState(UiState.NOT_VIEWING);
		initWidget(outerHp);
		
		lister.getDocumentList();
	}
	
	/* (non-Javadoc)
	 * Receives button events.
	 * @see com.google.gwt.event.dom.client.ClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
	 */
	@Override
	public void onClick(ClickEvent event) {
		if (event.getSource().equals(refreshList)) {
			lister.getDocumentList();
		} else if (event.getSource().equals(createNew)) {
			if (tooManyTabs){
				statusDisplay.setText("Too many tabs open.");
			} else {
				creator.createDocument();
			}
		} else if (event.getSource().equals(refreshDoc)) {
			if (tabIsSelected()) {
				reader.getDocument(tabKeys.get(tb.getSelectedTab()));
			}
		} else if (event.getSource().equals(lockButton)) {
			if (tabIsSelected()) {
				locker.lockDocument(tabKeys.get(tb.getSelectedTab()));
			}
		} else if (event.getSource().equals(saveButton)) {
			if (tabIsSelected()) {
				// Make async call to save the document (also updates UI).
				LockedDocument lockedDoc = new LockedDocument(null, null,
						tabKeys.get(currentTabInd), tabTitles.get(currentTabInd).getValue(), 
						tabContents.get(currentTabInd).getHTML());
				saver.saveDocument(lockedDoc);
			}
		} else if (event.getSource().equals(closeButton)) {
			int indOfTab = tb.getSelectedTab();
			if (indOfTab != -1) {
				// Release locks if we were locked or locking.
				if (uiStates.get(indOfTab) == UiState.LOCKED ||
						uiStates.get(indOfTab) == UiState.LOCKING) {
					
					LockedDocument lockedDoc = new LockedDocument(null, null,
							tabKeys.get(indOfTab),
							tabTitles.get(indOfTab).getValue(),
							tabContents.get(indOfTab).getHTML());
					releaser.releaseLock(lockedDoc);
				}
				// Update UI and corresponding variables.
				removeTabAtInd(indOfTab);
				documentList.setSelectedIndex(-1);
			}
		} else if (event.getSource().equals(documentList)){
		    // only use this one if the select document is the one
		    // just closed
		    
		    String key = documentList.getValue(documentList.getSelectedIndex());
		    if (key == changedKey){
		        // if it was just closed, then there shouldn't be
		        // too many tabs, but just in case
		        if (tooManyTabs && !(tabKeys.contains(key))){
	                statusDisplay.setText("Too many tabs open.");
	            } else {
	                loadDoc(key);
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
			changedKey = documentList.getValue(documentList.getSelectedIndex());
			if (tooManyTabs && !(tabKeys.contains(changedKey))){
				statusDisplay.setText("Too many tabs open.");
			} else {
				loadDoc(changedKey);
			}
		}
	}
	
	protected void loadDoc(String key) {
		// If it's already open in a tab, save the location of the tab
		// and retrieve the title and contents from that one.
		int savedLoc = tabKeys.indexOf(key);
		if (savedLoc != -1) {
			// Select the appropriate tab; this should fire the SelectionHandler.
			tp.selectTab(savedLoc);
			statusDisplay.setText("Document loaded successfully");
		} else {
			addNewTab(key, new RichTextArea(), new TextBox());
			reader.getDocument(key);
		}
	}
	
	/**
	 * Updates relevant state-capturing variables and updates UI
	 * if necessary.
	 */
	protected void updateVarsAndUi(String key, String title, 
			String contents, UiState state) {
		// Update local data structures.
		int indResult = tabKeys.indexOf(key);
		if (indResult != -1) {
			tabTitles.get(indResult).setValue(title);
			tabContents.get(indResult).setHTML(contents);
			uiStates.set(indResult, state);
			if (key.equals(tabKeys.get(currentTabInd))) {
				setUiToState(state);
			}
		}
	}
	
	/**
	 * Just update the state corresponding to the key.
	 */
	protected void updateVarsAndUi(String key, UiState state) {
		// Update local data structures.
		int indResult = tabKeys.indexOf(key);
		if (indResult != -1) {
			uiStates.set(indResult, state);
			if (key.equals(tabKeys.get(currentTabInd))) {
				setUiToState(state);
			}
		}
	}
	
	/**
	 * Resets the state of the buttons and edit objects to the specified state.
	 * The state of these objects is modified by requesting or obtaining locks
	 * and trying to or successfully saving.
	 * 
	 * @param state the UI state to switch to (as defined in UiState.java)
	 */
	protected void setUiToState(UiState state) {
		refreshDoc.setEnabled(state.refreshDocEnabled);
		lockButton.setEnabled(state.lockButtonEnabled);
		saveButton.setEnabled(state.saveButtonEnabled);
		closeButton.setEnabled(state.closeButtonEnabled);
		if (tabIsSelected()) {
			tabTitles.get(currentTabInd).setEnabled(state.titleEnabled);
			tabContents.get(currentTabInd).setEnabled(state.contentsEnabled);
		}
	}
	
	/**
	 * Adds status lines to the console window to enable transparency of the
	 * underlying processes.
	 * 
	 * @param status the status to add to the console window
	 */
	protected void statusUpdate(String status) {
		while (statusArea.getWidgetCount() > 6) {
			statusArea.remove(1);
		}
		final HTML statusUpd = new HTML(status);
		statusArea.add(statusUpd);
		statusDisplay.setText(status);
	}

	protected void addNewTab(String key, RichTextArea contents, 
			TextBox title) {
		// Update local variables.
		currentTabInd = tb.getTabCount();
		tabKeys.add(key);
		tabTitles.add(title);
		tabContents.add(contents);
		uiStates.add(UiState.VIEWING);
		setUiToState(UiState.VIEWING);
		
		// Update TabPanel's UI:
		HorizontalPanel tabHeader = new HorizontalPanel();
		tabHeader.add(title);
		tp.add(contents, tabHeader);
		// Select the last tab for the user.
		tp.selectTab(tb.getTabCount() - 1);
		
		// set tab overflow boolean
		if (tb.getTabCount() == 4){
			tooManyTabs = true;
		}
	}
	
	private void removeTabAtInd(int i) {
		// Update local data structures
		tabKeys.remove(i);
		tabTitles.remove(i);
		tabContents.remove(i);
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
			currentTabInd = -1;
			setUiToState(UiState.NOT_VIEWING);
		}
		if (tooManyTabs){
			tooManyTabs = false;
			statusDisplay.setText("");
		}
	}
	
	private boolean tabIsSelected() {
		return currentTabInd >= 0 && currentTabInd < tabKeys.size();
	}
}
