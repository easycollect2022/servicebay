package com.servicebay.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.StorageClient;
import com.servicebay.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.servicebay.config.FirebaseAppHolder;
import java.util.Optional;
import java.util.UUID;

/** Saves images to the Firebase Storage bucket and returns a tokenised download URL. */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "firebase")
public class FirebaseStorageService implements StorageService {
    private final AppProperties props;
    private final FirebaseApp app;

    public FirebaseStorageService(AppProperties props, FirebaseAppHolder holder) {
        this.props = props;
        this.app = holder.get().orElseThrow(() -> new IllegalStateException("app.storage.type=firebase needs Firebase configured"));
    }

    @Override
    public String storeImage(MultipartFile file, String folder) {
        String ext = ImageValidator.validate(file, props.storage().maxBytes());
        String path = folder + "/" + UUID.randomUUID() + "." + ext;
        String token = UUID.randomUUID().toString();
        Bucket bucket = StorageClient.getInstance(app).bucket();
        try {
            Blob blob = bucket.create(path, file.getBytes(), file.getContentType());
            blob.toBuilder().setMetadata(Map.of("firebaseStorageDownloadTokens", token)).build().update();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return "https://firebasestorage.googleapis.com/v0/b/" + bucket.getName() + "/o/"
                + URLEncoder.encode(path, StandardCharsets.UTF_8) + "?alt=media&token=" + token;
    }
}
