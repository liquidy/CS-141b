package edu.caltech.cs141b.hw2.gwt.collab.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class DocRequestorResult implements IsSerializable {

	private String docKey;
	private int numPeopleLeft;
	
	// Required by GWT serialization.
	public DocRequestorResult() {
		
	}
	
	public DocRequestorResult(String docKey, int numPeopleLeft) {
		this.docKey = docKey;
		this.numPeopleLeft = numPeopleLeft;
	}
	
	public String getDocKey() {
		return docKey;
	}

	public int getNumPeopleLeft() {
		return numPeopleLeft;
	}
}
