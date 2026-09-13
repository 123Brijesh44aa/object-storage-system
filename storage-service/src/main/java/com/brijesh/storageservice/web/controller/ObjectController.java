package com.brijesh.storageservice.web.controller;


import com.brijesh.storageservice.service.ObjectService;
import com.brijesh.storageservice.web.dto.ObjectUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/storage/buckets")
@RequiredArgsConstructor
public class ObjectController {

    private final ObjectService objectService;


    // ── Upload ────────────────────────────────────────────────────

    @PutMapping("/{bucketName}/objects/{objectKey}")
    public ResponseEntity<ObjectUploadResponse> uploadObject(
            @PathVariable String bucketName,
            @PathVariable String objectKey,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Uuid") String ownerUuid) {

        String etag = objectService.uploadObject(
                bucketName, objectKey, file, ownerUuid);

        return ResponseEntity.ok(ObjectUploadResponse.builder()
                .bucketName(bucketName)
                .objectKey(objectKey)
                .etag(etag)
                .sizeBytes(file.getSize())
                .contentType(file.getContentType())
                .message("Object uploaded successfully")
                .build());
    }

    // ── Download ──────────────────────────────────────────────────

    @GetMapping("/{bucketName}/objects/{objectKey}")
    public ResponseEntity<InputStreamResource> downloadObject(
            @PathVariable String bucketName,
            @PathVariable String objectKey) {

        ObjectService.DownloadResult result =
                objectService.downloadObject(bucketName, objectKey);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.fileName() + "\"")
                .body(new InputStreamResource(result.stream()));
    }

    // ── Delete ────────────────────────────────────────────────────

    @DeleteMapping("/{bucketName}/objects/{objectKey}")
    public ResponseEntity<Map<String, String>> deleteObject(
            @PathVariable String bucketName,
            @PathVariable String objectKey,
            @RequestHeader("X-User-Uuid") String ownerUuid) {

        objectService.deleteObject(bucketName, objectKey, ownerUuid);

        return ResponseEntity.ok(Map.of(
                "message", "Object deleted successfully",
                "objectKey", objectKey
        ));
    }
}
