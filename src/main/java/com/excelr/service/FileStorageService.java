package com.excelr.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeFile(MultipartFile file, String uploadDir) throws Exception;
    void deleteFile(String fileName, String uploadDir) throws Exception;
}
