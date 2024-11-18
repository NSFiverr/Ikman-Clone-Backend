package com.marketplace.platform.service.storage;

import com.google.cloud.storage.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseStorageService {

    private final Storage storage;

    @Value("${firebase.storage.bucket}")
    private String bucketName;

    public String uploadFile(MultipartFile file, String directory) throws IOException {
        try {
            String fileName = generateFileName(file.getOriginalFilename());
            String path = directory + "/" + fileName;

            BlobId blobId = BlobId.of(bucketName, path);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();

            // Upload file
            Blob blob = storage.create(blobInfo, file.getBytes());

            // Generate a signed URL that expires in 10 years
            URL signedUrl = storage.signUrl(blobInfo, 3650, TimeUnit.DAYS);

            log.info("File uploaded successfully. Path: {}", path);
            return path; // Return the path instead of URL for easier deletion
        } catch (Exception e) {
            log.error("Failed to upload file", e);
            throw new IOException("Failed to upload file", e);
        }
    }

    public void deleteFile(String path) {
        try {
            log.info("Attempting to delete file at path: {}", path);

            BlobId blobId = BlobId.of(bucketName, path);
            boolean deleted = storage.delete(blobId);

            if (deleted) {
                log.info("File deleted successfully: {}", path);
            } else {
                log.warn("File not found: {}", path);
            }
        } catch (Exception e) {
            log.error("Failed to delete file: {}", path, e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    private String generateFileName(String originalFilename) {
        return UUID.randomUUID().toString() + getFileExtension(originalFilename);
    }

    private String getFileExtension(String filename) {
        return filename.substring(filename.lastIndexOf("."));
    }
}