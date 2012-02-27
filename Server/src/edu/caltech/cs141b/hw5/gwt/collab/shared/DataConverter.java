package edu.caltech.cs141b.hw5.gwt.collab.shared;

import java.util.Date;

import edu.caltech.cs141b.hw5.data.CollabMessages.DocumentMetaInfo;
import edu.caltech.cs141b.hw5.data.CollabMessages.LockedDocumentInfo;
import edu.caltech.cs141b.hw5.data.CollabMessages.UnlockedDocumentInfo;

/**
 * Helper class for converting between protocol buffer and native data type.
 * 
 * 
 * @author aliu
 *
 */
public class DataConverter {

	/**
	 * Convert from LockedDocumentInfo to LockedDocument
	 * 
	 * @param protoc data type
	 * @return native object type
	 */
	public static LockedDocument buildLockedDocument(LockedDocumentInfo docInfo) {
		return new LockedDocument(docInfo.getLockedBy(), new Date(docInfo.getLockedUntil()), 
				docInfo.getKey(), docInfo.getTitle(), docInfo.getContents());
	}
	
	/**
	 * Convert from LockedDocument to LockedDocumentInfo
	 * 
	 * @param native object type
	 * @return protoc data type
	 */
	public static LockedDocumentInfo buildLockedDocumentInfo(LockedDocument doc) {
		return LockedDocumentInfo.newBuilder()
				.setKey(doc.getKey())
				.setTitle(doc.getTitle())
				.setLockedBy(doc.getLockedBy())
				.setLockedUntil(doc.getLockedUntil().getTime())
				.setContents(doc.getContents())
				.build();
	}
	
	/**
	 * Convert from UnlockedDocumentInfo to UnlockedDocument
	 * 
	 * @param protoc data type
	 * @return native object type
	 */
	public static UnlockedDocument buildUnlockedDocument(UnlockedDocumentInfo doc) {
		return new UnlockedDocument(doc.getKey(), doc.getTitle(), doc.getContents());
	}
	
	/**
	 * Convert from UnlockedDocument to UnlockedDocumentInfo
	 * 
	 * @param native object type
	 * @return protoc data type
	 */
	public static UnlockedDocumentInfo buildUnlockedDocumentInfo(UnlockedDocument doc) {
		return UnlockedDocumentInfo.newBuilder()
				.setKey(doc.getKey())
				.setTitle(doc.getTitle())
				.setContents(doc.getContents())
				.build();
	}
	
	/**
	 * Convert from DocumentMetaInfo to DocumentMetadata
	 * 
	 * @param protoc data type
	 * @return native object type
	 */
	public static DocumentMetadata buildDocumentMetadata(DocumentMetaInfo meta) {
		return new DocumentMetadata(meta.getKey(), meta.getTitle());
	}
	
	/**
	 * Convert from DocumentMetadata to DocumentMetaInfo
	 * 
	 * @param native object type
	 * @return protoc data type
	 */
	public static DocumentMetaInfo buildDocumentMetaInfo(DocumentMetadata meta) {
		return DocumentMetaInfo.newBuilder()
				.setKey(meta.getKey())
				.setTitle(meta.getTitle())
				.build();
	}
}
