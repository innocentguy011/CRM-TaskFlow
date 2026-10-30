package com.arjun.crm.service;

import com.arjun.crm.entity.StoredFile;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /** URL prefix the stored files are served from (see FileController). */
    String URL_PREFIX = "/api/files/";

    /** Stores the file and returns its public URL path, e.g. /api/files/{uuid}. */
    String store(MultipartFile file, Long uploadedById);

    StoredFile load(String id);

    /** Deletes the file behind a URL returned by store(); ignores other URLs. */
    void deleteByUrl(String fileUrl);
}
