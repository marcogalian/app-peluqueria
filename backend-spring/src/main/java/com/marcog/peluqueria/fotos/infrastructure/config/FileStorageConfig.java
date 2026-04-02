package com.marcog.peluqueria.fotos.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class FileStorageConfig {

    @Value("${uploads.dir:uploads}")
    private String uploadsDir;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(uploadsDir));
    }

    public String getUploadsDir() { return uploadsDir; }
}
