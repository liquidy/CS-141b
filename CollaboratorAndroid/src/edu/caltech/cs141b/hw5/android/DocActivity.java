package edu.caltech.cs141b.hw5.android;

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
	private LockedDocument lockedDoc;
	private UnlockedDocument unlockedDoc;
	
	
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
		
		Button lockButton = (Button) findViewById(R.id.Lock);
		lockButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				lockDocument();
				statusPane.setText(docKey);
			}
		}
			);
		
		Button saveButton = (Button) findViewById(R.id.Save);
		saveButton.setBackgroundColor(Color.GRAY);
		saveButton.setEnabled(false);
		saveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
//				CollabServiceWrapper service = new CollabServiceWrapper(); 
//				lockedDoc.setContents(contents.getText().toString());
//				lockedDoc.setTitle(contents.getText().toString());
//				try {
//					service.saveDocument(lockedDoc);
//				} catch (LockExpired e) {
//					statusPane.setText("Lock expired");
//					e.printStackTrace();
//				} catch (InvalidRequest e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
			}
		});
		
		
		switch (status){
		case CollaboratorAndroidActivity.LOAD_DOC:
			loadDocument();
			break;
		case CollaboratorAndroidActivity.NEW_DOC:
//			LockedDocument lockedDoc = new LockedDocument(null, null, null,
//					"Enter the document title.",
//					"New Doc not implemented yet");
//			service.saveDoc(lockedDoc.get)
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
			CollabServiceWrapper service = new CollabServiceWrapper(); 
			unlockedDoc = service.getDocument(docKey);
			title.setText(unlockedDoc.getTitle());
			contents.setText(unlockedDoc.getContents());
			statusPane.setText("Document loaded successfully");
			title.setEnabled(false);
			contents.setEnabled(false);
		} catch (InvalidRequest e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void lockDocument(){
		try {
			CollabServiceWrapper service = new CollabServiceWrapper(); 
			title.setText(docKey);
			lockedDoc = service.lockDocument(docKey);
//			LockedDocument lockedDocument = service.lockDocument(docKey);
//			title.setEnabled(true);
//			contents.setEnabled(true);
//			statusPane.setText(unlockedDoc.getKey());
		} catch (LockUnavailable e) {
			statusPane.setText("Could not acquire lock");
			e.printStackTrace();
		} catch (InvalidRequest e) {
			statusPane.setText("Error");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
