package com.arjun.crm.service.impl;

import com.arjun.crm.entity.StoredFile;
import com.arjun.crm.exception.ResourceNotFoundException;
import com.arjun.crm.repository.StoredFileRepository;
import com.arjun.crm.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private final StoredFileRepository storedFileRepository;

    @Override
    @Transactional
    public String store(MultipartFile file, Long uploadedById) {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        try {
            StoredFile stored = storedFileRepository.save(StoredFile.builder()
                    .id(UUID.randomUUID().toString())
                    .fileName(originalName)
                    .contentType(contentType)
                    .fileSize(file.getSize())
                    .data(file.getBytes())
                    .uploadedById(uploadedById)
                    .build());
            log.info("Stored file {} ({} bytes)", stored.getId(), stored.getFileSize());
            return URL_PREFIX + stored.getId();
        } catch (IOException e) {
            log.error("Failed to read uploaded file", e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFile load(String id) {
        return storedFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
    }

    @Override
    @Transactional
    public void deleteByUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(URL_PREFIX)) {
            return;
        }
        storedFileRepository.deleteById(fileUrl.substring(URL_PREFIX.length()));
    }
}
