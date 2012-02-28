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
	private ArrayAdapter<String> listAdapter;
	private CollabServiceWrapper service = new CollabServiceWrapper();  

	public static final int DOC_CHANGED = 1;
	
	public static final int LOAD_DOC = 1;
	public static final int NEW_DOC = 2;
	/** Called when the activity is first created. */
	@Override

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Test getting the document list and print it out on screen    
		List<DocumentMetadata> metas = service.getDocumentList();

		docTitles.clear();
		docKeys.clear();
		for (DocumentMetadata meta : metas) {
			//docsInfo += meta.getKey() + ": " + meta.getTitle() + "\n"; 
			docTitles.add(meta.getTitle());
			docKeys.add(meta.getKey());
		}

		setContentView(R.layout.main);

		ListView list = (ListView) findViewById(R.id.list);
		list.setTextFilterEnabled(true);
		listAdapter = new ArrayAdapter<String>(this,
				R.layout.list_item, docTitles);
		list.setAdapter(listAdapter);

		list.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent newPage = new Intent(view.getContext(), DocActivity.class);
				Bundle b = new Bundle();
				b.putString("document key", docKeys.get(position));
				b.putInt("status code", LOAD_DOC);
				newPage.putExtras(b);
				startActivityForResult(newPage, 0);
			}
		});

		TextView instructions = (TextView) findViewById(R.id.Instructions);
		instructions.setText("Select a document to begin.");
		
		Button refresh = (Button) findViewById(R.id.Refresh);
		Button newDoc = (Button) findViewById(R.id.NewDoc);

		refresh.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				refreshDocumentList();	
			}

		});

		newDoc.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent newPage = new Intent(view.getContext(), DocActivity.class);
				Bundle b = new Bundle();
				b.putInt("status code", NEW_DOC);
				newPage.putExtras(b);
				startActivityForResult(newPage, 0);
			}
		});
	}

	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		refreshDocumentList();
	}

	public void refreshDocumentList(){
		// Test getting the document list and print it out on screen
		List<DocumentMetadata> metas = service.getDocumentList();

		docTitles.clear();
		docKeys.clear();
		for (DocumentMetadata meta : metas) {
			docTitles.add(meta.getTitle());
			docKeys.add(meta.getKey());
		}

		listAdapter.notifyDataSetChanged();
	}
}