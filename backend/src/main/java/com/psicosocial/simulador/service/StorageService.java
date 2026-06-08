package com.psicosocial.simulador.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class StorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private static final List<String> ALLOWED = List.of("image/png", "image/jpeg", "image/jpg", "image/webp", "image/gif");

    private Path root;

    @PostConstruct
    public void init() {
        try {
            root = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de subidas", e);
        }
    }

    public String storeAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Archivo vacío");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType.toLowerCase())) {
            throw new RuntimeException("Formato no permitido. Usa PNG, JPG, WEBP o GIF.");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("La imagen supera los 5MB.");
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "avatar" : file.getOriginalFilename());
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot).toLowerCase();
        String filename = "avatar_" + UUID.randomUUID().toString().replace("-", "") + ext;

        try {
            Path target = root.resolve(filename).normalize();
            if (!target.getParent().equals(root)) {
                throw new RuntimeException("Ruta inválida");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el archivo", e);
        }

        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(filename)
                .toUriString();
    }
}
