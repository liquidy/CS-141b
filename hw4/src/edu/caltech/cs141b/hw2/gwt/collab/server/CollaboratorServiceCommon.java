package edu.caltech.cs141b.hw2.gwt.collab.server;

public class CollaboratorServiceCommon {
	
	private static final CollaboratorServiceCommon instance = new CollaboratorServiceCommon();
	private static CollaboratorServiceImpl service = null;
	
  private CollaboratorServiceCommon() { }

  protected static CollaboratorServiceCommon getInstance() {
  	if (service == null) {
  		return null;
  	} else {
  		return instance;
  	}
  }
  
  protected static void setService(CollaboratorServiceImpl service) {
  	CollaboratorServiceCommon.service = service;
  }
  
  protected void pollDocQueue(String documentKey) {
  	service.pollDocQueue(documentKey, true);
  }
}
