package com.excelr.service.impl;

import com.excelr.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path rootLocation = Paths.get("uploads");

    public FileStorageServiceImpl() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String uploadDir) throws Exception {
        if (file.isEmpty()) {
            throw new Exception("Failed to store empty file");
        }

        // Get original filename and create unique filename
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        
        if (originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // Create directory if it doesn't exist
        Path dirPath = rootLocation.resolve(uploadDir);
        Files.createDirectories(dirPath);

        // Copy file to the target location
        Path destinationFile = dirPath.resolve(uniqueFilename);
        
        try {
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new Exception("Failed to store file", e);
        }

        // Return the URL path
        return "/uploads/" + uploadDir + "/" + uniqueFilename;
    }

    @Override
    public void deleteFile(String fileName, String uploadDir) throws Exception {
        try {
            Path filePath = rootLocation.resolve(uploadDir).resolve(fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new Exception("Failed to delete file", e);
        }
    }
}
