package com.servicebay.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@ConditionalOnProperty(name = "app.storage.type", havingValue = "db")
public class ImageController {
    private final JdbcTemplate jdbc;

    public ImageController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<byte[]> image(@PathVariable UUID id) {
        List<ResponseEntity<byte[]>> rows = jdbc.query("select content_type, data from stored_image where id = ?",
                (rs, n) -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(rs.getString(1)))
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                        .header("X-Content-Type-Options", "nosniff")
                        .body(rs.getBytes(2)), id);
        return rows.isEmpty() ? ResponseEntity.notFound().build() : rows.get(0);
    }
}
