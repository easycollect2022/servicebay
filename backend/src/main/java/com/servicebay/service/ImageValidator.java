package com.servicebay.service;

import com.servicebay.web.ApiException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

final class ImageValidator {
    private ImageValidator() {}

    static final Map<String, String> EXT = Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");

    /** @return file extension for the validated image. */
    static String validate(MultipartFile file, long maxBytes) {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("NO_FILE", "Choose an image to upload");
        if (file.getSize() > maxBytes) throw ApiException.badRequest("FILE_TOO_LARGE", "Image must be under " + (maxBytes / (1024 * 1024)) + " MB");
        String ext = EXT.get(file.getContentType());
        if (ext == null) throw ApiException.badRequest("BAD_FILE_TYPE", "Only JPG, PNG or WebP images are allowed");
        try {
            byte[] head = new byte[12];
            int n = file.getInputStream().readNBytes(head, 0, 12);
            if (!magicMatches(ext, head, n)) throw ApiException.badRequest("BAD_FILE_TYPE", "File content is not a valid image");
        } catch (java.io.IOException e) {
            throw ApiException.badRequest("BAD_FILE", "Could not read the file");
        }
        return ext;
    }

    private static boolean magicMatches(String ext, byte[] h, int n) {
        return switch (ext) {
            case "jpg" -> n >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            case "png" -> n >= 4 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
            case "webp" -> n >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
            default -> false;
        };
    }
}
