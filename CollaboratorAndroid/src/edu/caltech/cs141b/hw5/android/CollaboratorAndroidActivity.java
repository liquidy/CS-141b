package edu.caltech.cs141b.hw5.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Button;

public class CollaboratorAndroidActivity extends Activity {
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Button next = (Button) findViewById(R.id.Refresh);
		Button newDoc = (Button) findViewById(R.id.NewDoc);

		ListView list = (ListView) findViewById(R.id.list);
		list.setTextFilterEnabled(true);
		list.setAdapter(new ArrayAdapter<String>(this,
				R.layout.list_item, COUNTRIES));

		list.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent newPage = new Intent(view.getContext(), DocActivity.class);
				startActivityForResult(newPage, 0);
			}
		});

		newDoc.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent myIntent = new Intent(view.getContext(), DocActivity.class);
				startActivityForResult(myIntent, 0);
			}
		});
		
		protobufRequest();
	}
	public void protobufRequest(){
		String url = "http://2manithreads.appspot.com/";
		try {
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response = httpclient.execute(new HttpGet(url));
			HttpPost postRequest = new HttpPost(url);
			
//			String proto = new RequestMessage();
			StatusLine statusLine = response.getStatusLine();
			if(statusLine.getStatusCode() == HttpStatus.SC_OK){
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				response.getEntity().writeTo(out);
				out.close();
				String responseString = out.toString();
			} else{
				//Closes the connection.
				response.getEntity().getContent().close();
				throw new IOException(statusLine.getReasonPhrase());
			}
		}
		catch (Exception e) {
			System.out.println("Nay, did not work");
		}
	}
	static final String[] COUNTRIES = new String[] {
		"AfghanistanHOOOOOOOOOOOOHAAAAAAASCROLIINGGGGGGGGGGGERRRYDAAAAAY", 
		"Albania", "Algeria", "American Samoa", "Andorra",
		"Angola", "Anguilla", "Antarctica", "Antigua and Barbuda", "Argentina",
		"Armenia", "Aruba", "Australia", "Austria", "Azerbaijan" };

}