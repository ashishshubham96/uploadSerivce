package com.ecm.upload.model;

import java.util.Arrays;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "Upload")
public class MongoFileChunk {

    @Id
    private String id; // MongoDB's automatically generated ID

    private byte[] data; // Chunk of the file data

    private Long fileId; // References the fileId in your SQL table

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

	@Override
	public String toString() {
		return "MongoFileChunk [id=" + id + ", data=" + Arrays.toString(data) + ", fileId=" + fileId + "]";
	}
    
    
}
