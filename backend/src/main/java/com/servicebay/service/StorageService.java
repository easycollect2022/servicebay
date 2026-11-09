package com.servicebay.service;

import org.springframework.web.multipart.MultipartFile;

/** Stores an uploaded image and returns its public URL. */
public interface StorageService {
    String storeImage(MultipartFile file, String folder);
}
