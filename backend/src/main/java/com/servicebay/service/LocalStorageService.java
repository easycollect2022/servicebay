package com.servicebay.service;

import com.servicebay.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Saves images on the server's disk and serves them from /uploads (single-server / local dev). */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {
    private final AppProperties props;

    public LocalStorageService(AppProperties props) {
        this.props = props;
    }

    @Override
    public String storeImage(MultipartFile file, String folder) {
        String ext = ImageValidator.validate(file, props.storage().maxBytes());
        String name = UUID.randomUUID() + "." + ext;
        Path dir = Path.of(props.storage().localDir(), folder);
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(name).toAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return props.publicBaseUrl() + "/uploads/" + folder + "/" + name;
    }
}
