package com.ecm.upload.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "FileChunk")
public class FileChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chunkId;

    @Column(nullable = false)
    private Long fileId;

    private String mongoChunkId;

    @Column(nullable = false)
    private Integer chunkIndex;

    // Getters and Setters
    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getMongoChunkId() {
        return mongoChunkId;
    }

    public void setMongoChunkId(String mongoChunkId) {
        this.mongoChunkId = mongoChunkId;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

	@Override
	public String toString() {
		return "FileChunk [chunkId=" + chunkId + ", fileId=" + fileId + ", mongoChunkId=" + mongoChunkId
				+ ", chunkIndex=" + chunkIndex + "]";
	}
    
    
}