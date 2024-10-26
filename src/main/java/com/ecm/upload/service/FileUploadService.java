package com.ecm.upload.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.ecm.upload.entity.File;
import com.ecm.upload.entity.FileChunk;
import com.ecm.upload.model.MongoFileChunk;
import com.ecm.upload.repository.FileChunkRepository;
import com.ecm.upload.repository.FileRepository;
import com.ecm.upload.repository.MongoFileChunkRepository;
import com.ecm.upload.util.EncryptionUtil;

import org.bson.types.ObjectId;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

@Service
public class FileUploadService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileChunkRepository fileChunkRepository;

    @Autowired
    private MongoFileChunkRepository mongoFileRepository; // Assuming you have a MongoDB repository


    @Autowired
    private RestTemplate restTemplate;

    @Value("${remote.app.url}")
    private String remoteAppUrl;
    
    @Transactional
    public File uploadFile(Path filePath, String createdBy) throws IOException {
            // Read the file and get the metadata
            String fileName = filePath.getFileName().toString();
            long fileSize = Files.size(filePath);
            String fileType = Files.probeContentType(filePath);
            
            // Step 1: Save the file metadata in the File table
            File fileMetadata = new File();
            fileMetadata.setFileName(fileName);
            fileMetadata.setFileSize(fileSize);
            fileMetadata.setFileType(fileType);
            fileMetadata.setCreatedBy(createdBy);
            fileMetadata.setCreatedAt(java.time.LocalDateTime.now());

            fileMetadata = fileRepository.save(fileMetadata); // Save to get fileId
            
            // Step 2: Split file into chunks of 10KB and save them in the FileChunk table
            try {
				divideAndStoreChunks(filePath, fileMetadata.getFileId());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

        
        
        return fileMetadata;
    }

    private void divideAndStoreChunks(Path filePath, Long fileId) throws IOException {
        byte[] fileBytes = Files.readAllBytes(filePath);
        int chunkSize = 1024 * 1024; // 10 KB
        int totalChunks = (int) Math.ceil((double) fileBytes.length / chunkSize);

        for (int i = 0; i < totalChunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, fileBytes.length);

            byte[] chunkData = java.util.Arrays.copyOfRange(fileBytes, start, end);

            // Step 3: Store chunk data in MongoDB and get the mongoChunkId
            MongoFileChunk mongoChunk = new MongoFileChunk();
            mongoChunk.setData(chunkData);
            mongoChunk.setFileId(fileId);
            mongoChunk = mongoFileRepository.save(mongoChunk); // MongoDB save

            // Step 4: Save chunk metadata in the FileChunk table
            FileChunk fileChunk = new FileChunk();
            fileChunk.setFileId(fileId);
            fileChunk.setMongoChunkId(mongoChunk.getId().toString());
            fileChunk.setChunkIndex(i);

            fileChunkRepository.save(fileChunk);
        }
        
        
    }
    
    @Transactional(readOnly = true)
    public byte[] downloadFile(Long fileId) {
        // Step 1: Get all the file chunks based on fileId from the FileChunk table
        List<FileChunk> fileChunks = fileChunkRepository.findByFileId(fileId);
        
        if (fileChunks.isEmpty()) {
            throw new RuntimeException("No chunks found for file ID: " + fileId);
        }

        // Step 2: Reconstruct the file from its chunks
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for (FileChunk fileChunk : fileChunks) {
            // Fetch each chunk from MongoDB using the mongoChunkId
        	MongoFileChunk mongoChunk = mongoFileRepository.findById(fileChunk.getMongoChunkId())
        		    .orElseThrow(() -> new RuntimeException("Chunk not found in MongoDB for chunk ID: " + fileChunk.getMongoChunkId()));

            // Write chunk data to the output stream
            try {
				outputStream.write(mongoChunk.getData());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        // Return the complete file as a byte array
        return outputStream.toByteArray();
    }

    // Get the file metadata for setting the filename and other properties in the response
    @Transactional(readOnly = true)
    public File getFileMetadata(Long fileId) {
        return fileRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));
    }

    public String syndicateFileToRemoteApp(Long fileId) {
        try {
            // Step 1: Fetch the file metadata
            File fileMetadata = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));
            
            // Step 2: Fetch the file content by reconstructing chunks
            byte[] fileData = downloadFile(fileId);
            

            // Step 3: Generate AES key and encrypt file data
            SecretKey secretKey = EncryptionUtil.generateKey();
            byte[] encryptedData = EncryptionUtil.encrypt(fileData, secretKey);
            
            // Step 4: Prepare data to send, including the encrypted file data and key
            Map<String, Object> fileDataMap = new HashMap<>();
            fileDataMap.put("fileName", fileMetadata.getFileName());
            fileDataMap.put("createdBy", fileMetadata.getCreatedBy());
            fileDataMap.put("encryptedData", encryptedData);
            fileDataMap.put("keyBytes", EncryptionUtil.keyToBytes(secretKey)); // Convert key to bytes


            // Step 4: Send data to remote application
            String endpoint = remoteAppUrl + "/receive";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(fileDataMap, headers);

            ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, request, String.class);
            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return "File syndication failed";
        }
    }
    
}
