package com.sweetmemory.service;

import java.io.IOException;
import java.io.InputStream;
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

import net.coobird.thumbnailator.Thumbnails;

/**
 * 本地磁盘文件存储：uploads/yyyy/MM/uuid.ext，原图不压缩；
 * 图片另存一份 480px 宽 JPEG 缩略图到 uploads/thumbs/yyyy/MM/uuid.jpg（仅用于列表/网格）。
 */
@Service
public class FileStorageService {

    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> AUDIO_EXT = Set.of("mp3", "m4a", "wav", "aac", "ogg", "webm");
    /** ImageIO 可直接解码的格式才生成缩略图；webp/gif 兜底用原图 */
    private static final Set<String> THUMBABLE_EXT = Set.of("jpg", "jpeg", "png");

    private static final int THUMB_WIDTH = 480;

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

    /** 保存原图并尽力生成缩略图；返回 [url, thumb]（thumb 失败时等于 url，不影响上传） */
    public String[] saveImageWithThumb(MultipartFile file) {
        String url = save(file, "image");
        String ext = extension(file.getOriginalFilename());
        if (!THUMBABLE_EXT.contains(ext)) {
            return new String[] { url, url };
        }
        try {
            return new String[] { url, generateThumbFor(resolve(url)) };
        } catch (Exception e) {
            return new String[] { url, url };
        }
    }

    /** 由已落盘的原图生成缩略图，返回其公开 URL */
    public String generateThumbFor(Path original) throws IOException {
        String name = original.getFileName().toString();
        String base = name.substring(0, name.lastIndexOf('.'));
        // 相对路径 yyyy/MM/xxx.ext → thumbs/yyyy/MM/base.jpg
        Path rel = root.relativize(original);
        Path thumbRel = Paths.get("thumbs").resolve(rel).resolveSibling(base + ".jpg");
        Path thumb = root.resolve(thumbRel);
        Files.createDirectories(thumb.getParent());
        try (InputStream in = Files.newInputStream(original)) {
            Thumbnails.of(in)
                    .width(THUMB_WIDTH)
                    .outputFormat("jpg")
                    .outputQuality(0.82)
                    .toFile(thumb.toFile());
        }
        return publicBase + "/" + thumbRel;
    }

    /** 公开 URL → 磁盘路径（越界返回 null） */
    public Path resolve(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(publicBase + "/")) return null;
        Path p = root.resolve(publicUrl.substring(publicBase.length() + 1)).normalize();
        return p.startsWith(root) ? p : null;
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
