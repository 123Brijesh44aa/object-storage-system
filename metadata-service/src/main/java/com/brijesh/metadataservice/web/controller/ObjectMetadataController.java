package com.brijesh.metadataservice.web.controller;

import com.brijesh.metadataservice.service.ObjectMetadataService;
import com.brijesh.metadataservice.web.dto.ObjectMetadataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/metadata/buckets")
@RequiredArgsConstructor
public class ObjectMetadataController {

    private final ObjectMetadataService objectService;

    @GetMapping("/{bucketName}/objects")
    public ResponseEntity<List<ObjectMetadataResponse>> listObjects(
            @PathVariable String bucketName,
            @RequestParam(required = false) String prefix) {
        return ResponseEntity.ok(
                objectService.listObjects(bucketName, prefix));
    }

    @GetMapping("/{bucketName}/objects/{objectKey}")
    public ResponseEntity<ObjectMetadataResponse> getObjectMetadata(
            @PathVariable String bucketName,
            @PathVariable String objectKey) {
        return ResponseEntity.ok(
                objectService.getObjectMetadata(bucketName, objectKey));
    }
}