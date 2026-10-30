package com.arjun.crm.controller;

import com.arjun.crm.entity.StoredFile;
import com.arjun.crm.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Serves uploaded files (chat files, task attachments) from the database.
 * URL: GET /api/files/{id}
 *
 * No authentication required for download, so files work in <img> and <a> tags;
 * the random UUID in the URL is the secret.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String id) {
        StoredFile file = fileStorageService.load(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(file.getFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .header("X-Content-Type-Options", "nosniff")
                // Uploaded SVG/HTML must never run scripts on the API origin
                .header("Content-Security-Policy", "default-src 'none'; img-src 'self'; style-src 'unsafe-inline'; sandbox")
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                .body(file.getData());
    }
}
