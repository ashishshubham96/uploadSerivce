package com.ecm.upload.controller;


import com.ecm.upload.entity.File;
import com.ecm.upload.service.FileUploadService;
import com.ecm.upload.util.EncryptionUtil;

import jakarta.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

import javax.crypto.SecretKey;

@RestController
@RequestMapping("/ecm/files")
public class FileController {

    @Autowired
    private FileUploadService fileUploadService;
    
    
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam String filePath,
            @RequestParam String createdBy) {

        // Call the service layer to process the file
    	File file = new File();
		try {
			file = fileUploadService.uploadFile(Path.of(filePath), createdBy);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return ResponseEntity.ok("File uploaded successfully" + file.getFileId());
    }
    
    private static final String TARGET_APP_BASE_URL = "http://localhost:8081/api/files/upload"; // Port for Application 2

    @GetMapping("/download/{fileId}")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable Long fileId) {
        try {
            // Call the service to get the file as a byte array
            byte[] fileData = fileUploadService.downloadFile(fileId);

            // Get file metadata
            File fileMetadata = fileUploadService.getFileMetadata(fileId);
            String fileName = fileMetadata.getFileName();

            // Create a resource from the byte array
            ByteArrayResource resource = new ByteArrayResource(fileData);
            
         // Set the response headers
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileMetadata.getFileName());
            headers.add(HttpHeaders.CONTENT_TYPE, fileMetadata.getFileType());

            // Return the response entity
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

            // Set headers to initiate file download in the browser
//            return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
//                .contentLength(fileData.length)
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/syndicate/{fileId}")
    public ResponseEntity<String> syndicateFile(@PathVariable Long fileId) {
        String response = fileUploadService.syndicateFileToRemoteApp(fileId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/receive")

    public ResponseEntity<String> receiveFile(@RequestBody Map<String, Object> fileDataMap) {

        try {

            // Retrieve data from request

        	String fileName = (String) fileDataMap.get("fileName");

        	String createdBy = (String) fileDataMap.get("createBy");

        	String encodedFileData = (String) fileDataMap.get("encryptedData"); // Expecting this to be a base64 encoded string

            byte[] fileData = Base64.getDecoder().decode(encodedFileData); 

            String encodedKey = (String) fileDataMap.get("keyBytes"); // Securely receive the key bytes

            byte[] keyData = Base64.getDecoder().decode(encodedKey); 

            

            // Convert byte array back to SecretKey

            SecretKey secretKey = EncryptionUtil.bytesToKey(keyData);



            // Decrypt the file data

            byte[] decryptedData = EncryptionUtil.decrypt(fileData, secretKey);



            // Save the decrypted file

            Path tempFile = Files.write(Path.of("decrypted_" + fileName), decryptedData);



            fileUploadService.uploadFile(tempFile, createdBy);



            return ResponseEntity.ok("File syndicated successfully");

        } catch (IOException e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)

                                 .body("Failed to receive file data");

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest().body("Invalid file data format");

        } catch (Exception e) {

			// TODO Auto-generated catch block

			e.printStackTrace();

		}

        return ResponseEntity.ok("File Unsuccessful");

    }
    
}
