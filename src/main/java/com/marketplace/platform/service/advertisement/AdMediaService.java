package com.marketplace.platform.service.advertisement;

import com.marketplace.platform.domain.advertisement.AdMedia;
import com.marketplace.platform.domain.advertisement.Advertisement;
import com.marketplace.platform.domain.advertisement.MediaType;
import com.marketplace.platform.dto.request.MediaRequest;
import com.marketplace.platform.exception.BadRequestException;
import com.marketplace.platform.service.storage.FirebaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Slf4j
@Service
@RequiredArgsConstructor
public class AdMediaService {

    private final FirebaseStorageService firebaseStorageService;
    private static final String BASE_DIRECTORY = "advertisements";

    public Set<AdMedia> uploadMediaBatch(
            Advertisement advertisement,
            Set<MediaRequest> mediaRequests,
            Set<String> uploadedPaths
    ) throws IOException {
        Map<MediaRequest, CompletableFuture<String>> uploadFutures = new HashMap<>();
        String directory = buildDirectory(advertisement);

        // Start concurrent uploads
        for (MediaRequest request : mediaRequests) {
            String filename = generateUniqueFilename(request.getFile());
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    String firebasePath = firebaseStorageService.uploadFile(request.getFile(), directory);
                    uploadedPaths.add(firebasePath);
                    return firebasePath;
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            });
            uploadFutures.put(request, future);
        }

        try {
            CompletableFuture.allOf(uploadFutures.values().toArray(new CompletableFuture[0])).join();
        } catch (CompletionException e) {
            // Rollback tracked uploads
            uploadedPaths.forEach(path -> {
                try {
                    firebaseStorageService.deleteFile(path);
                    uploadedPaths.remove(path);
                } catch (Exception ignored) {
                    log.error("Failed to delete file during rollback: {}", path);
                }
            });
            throw new IOException("Failed to upload media files", e);
        }

        return uploadFutures.entrySet().stream()
                .map(entry -> AdMedia.builder()
                        .advertisement(advertisement)
                        .mediaType(entry.getKey().getMediaType())
                        .firebaseUrl(entry.getValue().join())
                        .originalFilename(entry.getKey().getFile().getOriginalFilename())
                        .mimeType(entry.getKey().getFile().getContentType())
                        .fileSize(entry.getKey().getFile().getSize())
                        .displayOrder(entry.getKey().getDisplayOrder())
                        .build())
                .collect(Collectors.toSet());
    }

    public Set<MediaRequest> createMediaRequests(
            List<MultipartFile> files,
            List<MediaType> mediaTypes,
            List<Integer> displayOrders
    ) {
        log.debug("Creating media requests for {} files", files.size());

        if (files.size() != mediaTypes.size() || files.size() != displayOrders.size()) {
            throw new BadRequestException("Number of files, media types, and display orders must match");
        }

        return IntStream.range(0, files.size())
                .mapToObj(i -> createMediaRequest(files.get(i), mediaTypes.get(i), displayOrders.get(i)))
                .collect(Collectors.toSet());
    }

    private MediaRequest createMediaRequest(
            MultipartFile file,
            MediaType mediaType,
            Integer displayOrder
    ) {
        MediaRequest mediaRequest = new MediaRequest();
        mediaRequest.setFile(file);
        mediaRequest.setMediaType(mediaType);
        mediaRequest.setDisplayOrder(displayOrder);
        return mediaRequest;
    }

    private String buildDirectory(Advertisement advertisement) {
        String sanitizedTitle = sanitizeTitle(advertisement.getTitle());
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s/%d/%s_%s",
                BASE_DIRECTORY,
                advertisement.getUser().getUserId(),
                timestamp,
                sanitizedTitle
        );
    }



    private String generateUniqueFilename(MultipartFile file) {
        String extension = getFileExtension(file.getOriginalFilename());
        return UUID.randomUUID().toString() + extension;
    }


    private String sanitizeTitle(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "")
                .substring(0, Math.min(title.length(), 50));
    }

    private String getFileExtension(String filename) {
        return filename != null ?
                filename.substring(filename.lastIndexOf(".")) : "";
    }



}