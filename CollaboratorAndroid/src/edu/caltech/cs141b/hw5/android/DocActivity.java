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
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.util.Log;

import java.io.FileNotFoundException;
import java.sql.Time;
import java.util.Date;

public class DocActivity extends Activity{
    public static final int ACTIVE_BUTTON_COLOR = 0xFF90E890;
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
    // lock countdown timer
    // tells the user how much edit time they have left. 
    // at expiration, releases the document lock and deletes changes
    private CountDownTimer timer = new CountDownTimer(30000, 1000) {

        public void onTick(long millisUntilFinished) {
            statusPane.setText("Seconds to lock expiration: " + millisUntilFinished / 1000);
        }

        public void onFinish() {
            statusPane.setText("Lock expired");
            releaseIfLocked();
        }
     };
    
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.doclayout);

        Bundle b = getIntent().getExtras();
        status = b.getInt("status code");

        title = (EditText) findViewById(R.id.Title);
        contents = (EditText) findViewById(R.id.Contents);
        statusPane = (TextView) findViewById(R.id.Status);

        /* when activity is closed, cancel the lock timer, 
         * release any locks, and return to the list screen
         */
        Button closeButton = (Button) findViewById(R.id.Close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timer.cancel();
                releaseIfLocked();
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        /* refreshes the document. 
         * cancels the locker timer and releases the lock, if any
         */
        reloadButton = (Button) findViewById(R.id.Reload);
        reloadButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                timer.cancel();
                releaseIfLocked();
                loadDocument();
                statusPane.setText("Document reloaded successfully.");
            }
        });

        /* locks if the document is not locked; releases the lock
         * if the document is locked
         */
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

        /*
         * save the document. cancel the timer when the
         * save attempt is made
         */
        saveButton = (Button) findViewById(R.id.Save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                lDoc.setContents(contents.getText().toString());
                lDoc.setTitle(title.getText().toString());
                try {
                    timer.cancel();
                    uDoc = service.saveDocument(lDoc);	
                    if ((uDoc != null) && (docKey == null)){
                        docKey = uDoc.getKey();
                    }
                    /* new document has a different UI set than an
                     * opened existing document
                     */
                    if (status == CollaboratorAndroidActivity.NEW_DOC){
                        docKey = uDoc.getKey();
                        reloadButton.setEnabled(true);
                        reloadButton.setBackgroundColor(ACTIVE_BUTTON_COLOR);   
                        lockButton.setEnabled(true);
                        lockButton.setBackgroundColor(ACTIVE_BUTTON_COLOR);
                        status = CollaboratorAndroidActivity.LOAD_DOC;
                    }
                    statusPane.setText("Save successful.");     
                } catch (LockExpired e) {
                    statusPane.setText("Lock expired; changes deleted.");
                    e.printStackTrace();
                } catch (InvalidRequest e) {
                    statusPane.setText("Error");
                    e.printStackTrace();
                } finally {
                    loadDocument();
                    disableEditing();
                    lockReleasable = false;
                }

            }
        });

        switch (status){
        // start state for loading an existing document
        case CollaboratorAndroidActivity.LOAD_DOC:
            this.docKey = b.getString("document key");
            loadDocument();
            statusPane.setText("Document loaded successfully");
            break;
        // start state for creating a new documents
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

    /* load a document. UI set to no editing allowed */
    private void loadDocument(){
        try {
            uDoc = service.getDocument(docKey);
        } catch (InvalidRequest e) {
            statusPane.setText("Error loading document");
            e.printStackTrace();
        } finally {
            title.setText(uDoc.getTitle());
            contents.setText(uDoc.getContents());
            disableEditing();
            lockReleasable = false;
        }
    }

    /* try to lock the document. adjust the UI depending
     * on lock success.
     */
    private void lockDocument(){
        try {
            lDoc = service.lockDocument(uDoc.getKey());
            statusPane.setText("Successfully locked until " +
              new Time(lDoc.getLockedUntil().getTime()));
            enableEditing();
            lockReleasable = true;
            timer.start();
        } catch (LockUnavailable e) {
        	statusPane.setText(e.getMessage());
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
        saveButton.setBackgroundColor(ACTIVE_BUTTON_COLOR);
        saveButton.setEnabled(true);

        //disable lock button
        lockButton.setText("Release");
    }

    private void disableEditing(){
        title.setEnabled(false);
        contents.setEnabled(false);

        // can't save
        saveButton.setBackgroundColor(Color.GRAY);
        saveButton.setEnabled(false);

        // enable lock
        lockButton.setBackgroundColor(ACTIVE_BUTTON_COLOR);
        lockButton.setText("Lock");
    }

    private void releaseIfLocked(){
        // release the lock if there is one
        boolean success = true;
        if (lockReleasable){
            try {
                service.releaseLock(lDoc);
            } catch (LockExpired e) {
                statusPane.setText("Lock expired; changes deleted.");
                success = false;
                e.printStackTrace();
            } catch (InvalidRequest e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                disableEditing();
                loadDocument();
                lockReleasable = false;
            }
            if (success){
                statusPane.setText("Lock released; changes deleted.");
            } 
        }
    }
}
