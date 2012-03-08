package edu.caltech.cs141b.hw2.gwt.collab.server;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class ChannelInfo {
	
	@PrimaryKey
	@Persistent
	private String token = null;
	
	@Persistent
	private String client = null;
	
	public ChannelInfo(String token, String client) {
		this.token = token;
		this.client = client;
	}
	
	public String getToken() {
		return token;
	}
	
	public String getClient() {
		return client;
	}
}
