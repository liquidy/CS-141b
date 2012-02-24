package edu.caltech.cs141b.hw5.android;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import edu.caltech.cs141b.hw5.android.data.DocumentMetadata;
import edu.caltech.cs141b.hw5.android.proto.CollabServiceWrapper;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class CollaboratorAndroidActivity extends Activity {
    private ArrayList<String> docKeys = new ArrayList<String>();
    private ArrayList<String> docTitles = new ArrayList<String>();
    
    /** Called when the activity is first created. */
    @Override

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Test getting the document list and print it out on screen
        CollabServiceWrapper service = new CollabServiceWrapper();      
        List<DocumentMetadata> metas = service.getDocumentList();
      
        for (DocumentMetadata meta : metas) {
        	//docsInfo += meta.getKey() + ": " + meta.getTitle() + "\n"; 
        	docTitles.add(meta.getTitle());
        	docKeys.add(meta.getKey());
        }

		setContentView(R.layout.main);

		Button refresh = (Button) findViewById(R.id.Refresh);
		Button newDoc = (Button) findViewById(R.id.NewDoc);

		ListView list = (ListView) findViewById(R.id.list);
		list.setTextFilterEnabled(true);
		list.setAdapter(new ArrayAdapter<String>(this,
				R.layout.list_item, docTitles));

		list.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent newPage = new Intent(view.getContext(), DocActivity.class);
//				newPage.putExtra("document key", docKeys.get(position));
				startActivityForResult(newPage, 0);
			}
		});
		
		refresh.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
//				??!?!
			}

		});

		newDoc.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent myIntent = new Intent(view.getContext(), DocActivity.class);
				startActivityForResult(myIntent, 0);
			}
		});
		

    }
    
	static final String[] COUNTRIES = new String[] {
		"AfghanistanHOOOOOOOOOOOOHAAAAAAASCROLIINGGGGGGGGGGGERRRYDAAAAAY", 
		"Albania", "Algeria", "American Samoa", "Andorra",
		"Angola", "Anguilla", "Antarctica", "Antigua and Barbuda", "Argentina",
		"Armenia", "Aruba", "Australia", "Austria", "Azerbaijan" };

}