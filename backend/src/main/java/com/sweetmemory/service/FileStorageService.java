package com.sweetmemory.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sweetmemory.config.AppProperties;

/**
 * 本地磁盘文件存储：uploads/yyyy/MM/uuid.ext，原图不压缩
 */
@Service
public class FileStorageService {

    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> AUDIO_EXT = Set.of("mp3", "m4a", "wav", "aac", "ogg");

    private final Path root;
    private final String publicBase;

    public FileStorageService(AppProperties props) {
        this.root = Paths.get(props.getUploadDir()).toAbsolutePath().normalize();
        this.publicBase = props.getPublicBase();
    }

    public String save(MultipartFile file, String kind) {
        String ext = extension(file.getOriginalFilename());
        checkExt(ext, kind);
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM", Locale.ROOT));
        try {
            Path dir = root.resolve(datePart);
            Files.createDirectories(dir);
            Path target = dir.resolve(UUID.randomUUID().toString().replace("-", "") + "." + ext);
            file.transferTo(target);
            return publicBase + "/" + datePart + "/" + target.getFileName();
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败: " + e.getMessage(), e);
        }
    }

    /** 删除照片时清理磁盘文件（容错：失败仅忽略，不影响主流程） */
    public void deleteQuietly(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(publicBase + "/")) return;
        try {
            Path p = root.resolve(publicUrl.substring(publicBase.length() + 1)).normalize();
            if (p.startsWith(root)) Files.deleteIfExists(p);
        } catch (IOException ignored) {
        }
    }

    private static String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static void checkExt(String ext, String kind) {
        Set<String> allowed = "audio".equals(kind) ? AUDIO_EXT : IMAGE_EXT;
        if (!allowed.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件类型: " + ext);
        }
    }
}
