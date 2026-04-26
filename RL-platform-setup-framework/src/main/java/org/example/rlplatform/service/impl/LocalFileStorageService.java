package org.example.rlplatform.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class LocalFileStorageService {

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );

    @Value("${platform.upload.base-dir:uploads}")
    private String baseDir;

    public String storeImage(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new RuntimeException("仅支持 jpg、jpeg、png、gif、webp、bmp 图片格式");
        }

        try {
            Path categoryDir = Paths.get(baseDir, category).toAbsolutePath().normalize();
            Files.createDirectories(categoryDir);

            String filename = UUID.randomUUID().toString().replace("-", "") + extension;
            Path targetPath = categoryDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + category + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败", e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new RuntimeException("文件缺少后缀名");
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
