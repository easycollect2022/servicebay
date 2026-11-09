package com.servicebay.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/** Serves locally stored uploads (dev / single-server setups) at /uploads/**. */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AppProperties props;

    public WebConfig(AppProperties props) {
        this.props = props;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (props.storage() != null && "local".equalsIgnoreCase(props.storage().type())) {
            String location = Path.of(props.storage().localDir()).toAbsolutePath().toUri().toString();
            registry.addResourceHandler("/uploads/**").addResourceLocations(location);
        }
        // When the PWA is bundled into the jar (demo build), serve it and fall back to index.html for app routes.
        if (new ClassPathResource("static/index.html").exists()) {
            registry.addResourceHandler("/**")
                    .addResourceLocations("classpath:/static/")
                    .resourceChain(true)
                    .addResolver(new PathResourceResolver() {
                        @Override
                        protected Resource getResource(String path, Resource location) throws IOException {
                            Resource r = location.createRelative(path);
                            if (r.exists() && r.isReadable()) return r;
                            if (path.startsWith("api/") || path.contains(".")) return null;
                            return new ClassPathResource("static/index.html");
                        }
                    });
        }
    }
}
