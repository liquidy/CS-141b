package edu.caltech.cs141b.hw5.android;

import edu.caltech.cs141b.hw5.android.data.LockExpired;
import edu.caltech.cs141b.hw5.android.data.LockedDocument;
import edu.caltech.cs141b.hw5.android.data.InvalidRequest;
import edu.caltech.cs141b.hw5.android.data.LockUnavailable;
import edu.caltech.cs141b.hw5.android.proto.CollabServiceWrapper;
import edu.caltech.cs141b.hw5.android.data.UnlockedDocument;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.util.Log;
import java.util.Date;

public class DocActivity extends Activity{
	private EditText title;
	private EditText contents;
	private TextView statusPane;
	private String docKey;
	private LockedDocument lDoc;
	private UnlockedDocument uDoc;
	private CollabServiceWrapper service = new CollabServiceWrapper(); 

	private Button saveButton;
	private Button lockButton;
	private Button reloadButton;

	private boolean lockReleasable = false;
	private int status;

	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.doclayout);

		Bundle b = getIntent().getExtras();
		status = b.getInt("status code");

		title = (EditText) findViewById(R.id.Title);
		contents = (EditText) findViewById(R.id.Contents);
		statusPane = (TextView) findViewById(R.id.Status);

		Button closeButton = (Button) findViewById(R.id.Close);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				releaseIfLocked();
				Intent intent = new Intent();
				setResult(RESULT_OK, intent);
				finish();
			}
		});

		reloadButton = (Button) findViewById(R.id.Reload);
		reloadButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				releaseIfLocked();
				loadDocument();
				statusPane.setText("Document reloaded successfully.");
			}
		});

		lockButton = (Button) findViewById(R.id.Lock);
		lockButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if (lockReleasable){
					releaseIfLocked();
				} else{
					lockDocument();
				}
			}
		}
				);

		saveButton = (Button) findViewById(R.id.Save);
		saveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				lDoc.setContents(contents.getText().toString());
				lDoc.setTitle(title.getText().toString());
				try {
					uDoc = service.saveDocument(lDoc);
					docKey = uDoc.getKey();
					statusPane.setText("Save successful.");
					disableEditing();
					lockReleasable = false;
					if (status == CollaboratorAndroidActivity.NEW_DOC){
						reloadButton.setEnabled(true);
						reloadButton.setBackgroundColor(0xFF90E890);	
						lockButton.setEnabled(true);
						lockButton.setBackgroundColor(0xFF90E890);
						status = CollaboratorAndroidActivity.LOAD_DOC;
					}
				} catch (LockExpired e) {
					statusPane.setText("Lock expired.");
					e.printStackTrace();
				} catch (InvalidRequest e) {
					statusPane.setText("Error");
					e.printStackTrace();
				}
			}
		});

		switch (status){
		case CollaboratorAndroidActivity.LOAD_DOC:
			this.docKey = b.getString("document key");
			loadDocument();
			break;
		case CollaboratorAndroidActivity.NEW_DOC:
			lDoc = new LockedDocument(null, null, null,
					null,
					null);
			title.setText("Enter the document title.");
			contents.setText("Enter document contents.");
			statusPane.setText("Save to create new document.");
			reloadButton.setEnabled(false);
			reloadButton.setBackgroundColor(Color.GRAY);
			enableEditing();
			lockButton.setEnabled(false);
			lockButton.setBackgroundColor(Color.GRAY);
			this.lockReleasable = false;
			break;
		}
	}

	private void loadDocument(){
		try {
			uDoc = service.getDocument(docKey);
			title.setText(uDoc.getTitle());
			contents.setText(uDoc.getContents());
			statusPane.setText("Document loaded successfully");
			disableEditing();
			lockReleasable = false;
		} catch (InvalidRequest e) {
			statusPane.setText("Error loading document");
			e.printStackTrace();
		}
	}

	private void lockDocument(){
		try {
			lDoc = service.lockDocument(uDoc.getKey());
			statusPane.setText("Lock successfully acquired");
			enableEditing();
			lockReleasable = true;
		} catch (LockUnavailable e) {
			statusPane.setText("Could not acquire lock");
			e.printStackTrace();
		} catch (InvalidRequest e) {
			statusPane.setText("Error acquiring lock");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// when editing is enabled, have lock.
	private void enableEditing(){
		title.setEnabled(true);
		contents.setEnabled(true);

		// re-enable save
		saveButton.setBackgroundColor(0xFF90E890);
		saveButton.setEnabled(true);

		//disable lock button
		lockButton.setText("Unlock");
	}

	private void disableEditing(){
		title.setEnabled(false);
		contents.setEnabled(false);

		// can't save
		saveButton.setBackgroundColor(Color.GRAY);
		saveButton.setEnabled(false);

		// enable lock
		lockButton.setBackgroundColor(0xFF90E890);
		lockButton.setText("Lock");
	}

	private void releaseIfLocked(){
		// release the lock if there is one
		if (lockReleasable){
			try {
				service.releaseLock(lDoc);
				disableEditing();
				statusPane.setText("Lock released");
			} catch (LockExpired e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidRequest e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
