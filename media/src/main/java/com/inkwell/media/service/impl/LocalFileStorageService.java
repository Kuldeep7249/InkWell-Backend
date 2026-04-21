package com.inkwell.media.service.impl;

import com.inkwell.media.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    @Value("${app.storage.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.storage.public-base-url:http://localhost:8085/files}")
    private String publicBaseUrl;

    @Override
    public String store(MultipartFile file, String generatedFilename) {
        try {
            Path directory = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            Path target = directory.resolve(generatedFilename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return publicBaseUrl + "/" + generatedFilename;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file: " + ex.getMessage());
        }
    }

    @Override
    public void delete(String fileUrl) {
        try {
            String filename = StringUtils.getFilename(fileUrl);
            if (filename == null) return;
            Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to delete stored file: " + ex.getMessage());
        }
    }
}
