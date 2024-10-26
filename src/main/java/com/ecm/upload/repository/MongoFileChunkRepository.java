package com.ecm.upload.repository;

import com.ecm.upload.model.MongoFileChunk;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MongoFileChunkRepository extends MongoRepository<MongoFileChunk, String> {

    List<MongoFileChunk> findByFileId(Long fileId); // Find all chunks for a specific file
}