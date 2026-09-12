package com.brijesh.metadataservice.web.controller;

import com.brijesh.metadataservice.service.BucketMetadataService;
import com.brijesh.metadataservice.web.dto.BucketMetadataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/metadata/buckets")
@RequiredArgsConstructor
public class BucketMetadataController {

    private final BucketMetadataService bucketService;

    @GetMapping
    public ResponseEntity<List<BucketMetadataResponse>> listBuckets(
            @RequestHeader("X-User-Uuid") String ownerUuid) {
        return ResponseEntity.ok(bucketService.getBucketsForUser(ownerUuid));
    }

    @GetMapping("/{bucketName}")
    public ResponseEntity<BucketMetadataResponse> getBucket(
            @PathVariable String bucketName) {
        return ResponseEntity.ok(bucketService.getBucket(bucketName));
    }
}