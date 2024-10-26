package com.marketplace.platform.service.storage;

import com.marketplace.platform.service.storage.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {
    private final Path fileStorageLocation;

    public FileStorageServiceImpl(@Value("${app.file-storage.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir)
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("Created file storage directory: {}", this.fileStorageLocation);
        } catch (IOException ex) {
            log.error("Could not create file storage directory", ex);
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String fileName = UUID.randomUUID().toString() + extension;

            // Store the file
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("Successfully stored file: {}", fileName);
            return fileName;
        } catch (IOException ex) {
            log.error("Failed to store file", ex);
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    @Override
    public void deleteFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            log.warn("Attempted to delete file with null or empty filename");
            return;
        }

        try {
            Path filePath = this.fileStorageLocation.resolve(fileName);
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Successfully deleted file: {}", fileName);
            } else {
                log.warn("File not found for deletion: {}", fileName);
            }
        } catch (IOException ex) {
            log.error("Error deleting file: {}", fileName, ex);
        }
    }
}