package com.servicebay.service;

import com.servicebay.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/** Saves images in the stored_image table; served by ImageController at /images/{id}. */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "db")
public class DbStorageService implements StorageService {
    private final AppProperties props;
    private final JdbcTemplate jdbc;

    public DbStorageService(AppProperties props, JdbcTemplate jdbc) {
        this.props = props;
        this.jdbc = jdbc;
    }

    @Override
    public String storeImage(MultipartFile file, String folder) {
        ImageValidator.validate(file, props.storage().maxBytes());
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("insert into stored_image(id, content_type, data) values (?, ?, ?)",
                    id, file.getContentType(), file.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String base = props.publicBaseUrl() == null ? "" : props.publicBaseUrl();
        return base + "/images/" + id;
    }
}
