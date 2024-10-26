package com.ecm.upload.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecm.upload.entity.FileChunk;

@Repository
public interface FileChunkRepository extends JpaRepository<FileChunk, Long> {
    // You can add custom query methods here if needed
	
	List<FileChunk> findByFileId(Long fileId);
}