package edu.caltech.cs141b.hw5.android;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import edu.caltech.cs141b.hw5.android.data.DocumentMetadata;
import edu.caltech.cs141b.hw5.android.proto.CollabServiceWrapper;

public class CollaboratorAndroidActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d("activity", "starting activity");
        String docsInfo = "";
        
        // Test getting the document list and print it out on screen
        CollabServiceWrapper service = new CollabServiceWrapper();      
        List<DocumentMetadata> metas = service.getDocumentList();
      
        for (DocumentMetadata meta : metas) {
        	docsInfo += meta.getKey() + ": " + meta.getTitle() + "\n"; 
        }
        
        TextView tv = new TextView(this);
        tv.setText(docsInfo);
        setContentView(tv);
        //setContentView(R.layout.main);
    }
}