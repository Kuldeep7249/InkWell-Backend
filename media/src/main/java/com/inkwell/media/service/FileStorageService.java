package com.inkwell.media.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file, String generatedFilename);
    void delete(String fileUrl);
}
