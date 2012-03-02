package edu.caltech.cs141b.hw2.gwt.collab.server;

import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;

@PersistenceCapable
public class Document {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key = null;
	
	@Persistent
	private String title = null;
	
	@Persistent
	private Text contents = null;
	
	@Persistent
	private String lockedBy = null;
	
	@Persistent
	private Date lockedUntil = null;
	
	public Document(String title, Text contents) {
		this.title = title;
		this.contents = contents;
	}
	
	public Key getKey() {
		return key;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}

	public Text getContents() {
		return contents;
	}
	
	public void setContents(Text contents) {
		this.contents = contents;
	}
	
	public String getLockedBy() {
		return lockedBy;
	}
	
	public void setLockedBy(String lockedBy) {
		this.lockedBy = lockedBy;
	}
	
	public Date getLockedUntil() {
		return lockedUntil;
	}
	
	public void setLockedUntil(Date lockedUntil) {
		this.lockedUntil = lockedUntil;
	}
}
