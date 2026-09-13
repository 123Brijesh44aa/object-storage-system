package com.brijesh.storageservice.web.controller;

import com.brijesh.storageservice.service.BucketService;
import com.brijesh.storageservice.web.dto.BucketResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/storage/buckets")
@RequiredArgsConstructor
public class BucketController {

    private final BucketService bucketService;


    @PostMapping("/{bucketName}")
    public ResponseEntity<BucketResponse> createBucket(
            @PathVariable @NotBlank String bucketName,
            @RequestParam(defaultValue = "false") Boolean isPublic,
            @RequestParam(defaultValue = "us-east-1") String region,
            @RequestHeader("X-User-Uuid") String ownerUuid) {

        bucketService.createBucket(bucketName, ownerUuid, isPublic, region);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BucketResponse.builder()
                        .bucketName(bucketName)
                        .ownerUuid(ownerUuid)
                        .isPublic(isPublic)
                        .region(region)
                        .message("Bucket created successfully")
                        .build());
    }

    @DeleteMapping("/{bucketName}")
    public ResponseEntity<Map<String, String>> deleteBucket(
            @PathVariable String bucketName,
            @RequestHeader("X-User-Uuid") String ownerUuid) {

        bucketService.deleteBucket(bucketName, ownerUuid);

        return ResponseEntity.ok(Map.of(
                "message", "Bucket deleted successfully",
                "bucketName", bucketName
        ));
    }
}
