package com.marketplace.platform.controller;

import com.marketplace.platform.service.storage.FirebaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FirebaseStorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("directory") String directory) {
        try {
            String path = storageService.uploadFile(file, directory);

            return ResponseEntity.ok(new UploadResponse(
                    "File uploaded successfully",
                    path,
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType()
            ));
        } catch (Exception e) {
            log.error("Upload failed", e);
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "Upload failed: " + e.getMessage(),
                    e.getClass().getSimpleName()
            ));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteFile(@RequestParam String path) {
        try {
            storageService.deleteFile(path);

            return ResponseEntity.ok(new DeleteResponse(
                    "File deleted successfully",
                    path
            ));
        } catch (Exception e) {
            log.error("Delete failed", e);
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "Delete failed: " + e.getMessage(),
                    e.getClass().getSimpleName()
            ));
        }
    }

    record UploadResponse(
            String message,
            String path,
            String originalFilename,
            long size,
            String contentType
    ) {}

    record DeleteResponse(
            String message,
            String path
    ) {}

    record ErrorResponse(
            String message,
            String errorType
    ) {}
}