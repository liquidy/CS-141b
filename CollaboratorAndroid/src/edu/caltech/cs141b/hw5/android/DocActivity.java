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
	
	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.doclayout);
		
		Bundle b = getIntent().getExtras();
		this.docKey = b.getString("document key");
		int status = b.getInt("status code");
		
		title = (EditText) findViewById(R.id.Title);
		contents = (EditText) findViewById(R.id.Contents);
		statusPane = (TextView) findViewById(R.id.Status);
		
		Button closeButton = (Button) findViewById(R.id.Close);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent();
				setResult(RESULT_OK, intent);
				finish();
			}
		});
		
		Button reloadButton = (Button) findViewById(R.id.Reload);
		reloadButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				loadDocument();
				statusPane.setText("Document reloaded successfully");
			}
		});
		
		lockButton = (Button) findViewById(R.id.Lock);
		lockButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				lockDocument();
			}
		}
			);
		
		saveButton = (Button) findViewById(R.id.Save);
		saveButton.setBackgroundColor(Color.GRAY);
		saveButton.setEnabled(false);
		saveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				lDoc.setContents(contents.getText().toString());
				lDoc.setTitle(title.getText().toString());
					try {
						service.saveDocument(lDoc);
						statusPane.setText("Save successful.");
						
						saveButton.setBackgroundColor(Color.GRAY);
						saveButton.setEnabled(false);
						
						// enable lock
						lockButton.setBackgroundColor(0xFF90E890);
						lockButton.setEnabled(true);
						
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
			loadDocument();
			break;
		case CollaboratorAndroidActivity.NEW_DOC:
			LockedDocument lockedDoc = new LockedDocument(null, null, null,
					null,
					null);
			try {
				service.saveDocument(new LockedDocument(null, null, null, null, null));
			} catch (LockExpired e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidRequest e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			title.setText("Enter the document title.");
			contents.setText("New Doc not implemented yet");
			statusPane.setText("New Document created.");
//			title.setEnabled(true);
//			contents.setEnabled(true);
			break;
		}
     

	}
	
	private void loadDocument(){
		try {
			uDoc = service.getDocument(docKey);
			title.setText(uDoc.getTitle());
			contents.setText(uDoc.getContents());
			statusPane.setText("Document loaded successfully");
			title.setEnabled(false);
			contents.setEnabled(false);
		} catch (InvalidRequest e) {
			statusPane.setText("Error loading document");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void lockDocument(){
		try {
			lDoc = service.lockDocument(docKey);
			title.setEnabled(true);
			contents.setEnabled(true);
			statusPane.setText("Lock successfully acquired");
			// re-enable save
			//saveButton.getBackground().setColorFilter(Color.parseColor("#90E890"), PorterDuff.Mode.DARKEN);
			saveButton.setBackgroundColor(0xFF90E890);
			saveButton.setEnabled(true);
			
			//disable lock button
			lockButton.setEnabled(false);
			lockButton.setBackgroundColor(Color.GRAY);
		} catch (LockUnavailable e) {
			statusPane.setText("Could not acquire lock");
			e.printStackTrace();
		} catch (InvalidRequest e) {
			statusPane.setText("Error acquiring lock");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
