package com.sweetmemory.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sweetmemory.entity.Photo;
import com.sweetmemory.mapper.PhotoMapper;
import com.sweetmemory.service.FileStorageService;

@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    private final PhotoMapper photoMapper;
    private final FileStorageService storage;

    public PhotoController(PhotoMapper photoMapper, FileStorageService storage) {
        this.photoMapper = photoMapper;
        this.storage = storage;
    }

    /** 照片列表：trashed=true 返回回收站 */
    @GetMapping
    public List<Photo> list(@RequestParam(defaultValue = "false") boolean trashed) {
        return photoMapper.listByDeleted(trashed);
    }

    /**
     * 批量上传照片（multipart）：
     * files[] 为图片文件；metas 为 JSON 数组字符串，与 files 一一对应，元素形如
     * {"takenAt":"2024-05-01","note":"...","featured":false}
     */
    @PostMapping
    public ResponseEntity<?> add(@RequestParam("files") List<MultipartFile> files,
                                 @RequestParam("metas") String metas) {
        List<Map<String, Object>> metaList;
        try {
            metaList = JsonUtil.parseMapList(metas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "metas 格式错误"));
        }
        if (files.size() != metaList.size()) {
            return ResponseEntity.badRequest().body(Map.of("message", "files 与 metas 数量不一致"));
        }

        List<Photo> created = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile f = files.get(i);
            Map<String, Object> meta = metaList.get(i);

            Photo p = new Photo();
            String url = storage.save(f, "image");
            p.setUrl(url);
            p.setThumb(url); // 一期不生成缩略图，thumb 与 url 相同
            p.setTakenAt(DateUtil.parseDate(meta.get("takenAt")));
            p.setNote(str(meta.get("note")));
            p.setFeatured(Boolean.TRUE.equals(meta.get("featured")));
            p.setDeleted(false);
            photoMapper.insert(p);
            created.add(photoMapper.findById(p.getId()));
        }
        return ResponseEntity.ok(created);
    }

    /** 更新留言/日期/首页标记 */
    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Photo p = photoMapper.findById(id);
        if (p == null) return ResponseEntity.status(404).body(Map.of("message", "照片不存在"));

        Photo patch = new Photo();
        patch.setId(id);
        if (body.containsKey("takenAt")) patch.setTakenAt(DateUtil.parseDate(body.get("takenAt")));
        if (body.containsKey("note")) patch.setNote(str(body.get("note")));
        if (body.containsKey("featured")) patch.setFeatured(Boolean.TRUE.equals(body.get("featured")));
        if (body.containsKey("audioUrl")) patch.setAudioUrl(str(body.get("audioUrl")));
        if (body.containsKey("audioDuration")) patch.setAudioDuration(num(body.get("audioDuration")));
        photoMapper.updateSelective(patch);
        return ResponseEntity.ok(photoMapper.findById(id));
    }

    /** 移入回收站 */
    @PostMapping("/{id}/trash")
    public ResponseEntity<?> trash(@PathVariable long id) {
        if (photoMapper.findById(id) == null) return ResponseEntity.status(404).body(Map.of("message", "照片不存在"));
        photoMapper.updateDeleted(id, true);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 从回收站恢复 */
    @PostMapping("/{id}/restore")
    public ResponseEntity<?> restore(@PathVariable long id) {
        if (photoMapper.findById(id) == null) return ResponseEntity.status(404).body(Map.of("message", "照片不存在"));
        photoMapper.updateDeleted(id, false);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 彻底删除（连同磁盘文件） */
    @DeleteMapping("/{id}/final")
    public ResponseEntity<?> destroy(@PathVariable long id) {
        Photo p = photoMapper.findById(id);
        if (p == null) return ResponseEntity.status(404).body(Map.of("message", "照片不存在"));
        storage.deleteQuietly(p.getUrl());
        storage.deleteQuietly(p.getAudioUrl());
        photoMapper.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 上传语音（二期小程序使用；接口先预留） */
    @PostMapping("/{id}/audio")
    public ResponseEntity<?> uploadAudio(@PathVariable long id,
                                         @RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "duration", required = false) Double duration) {
        Photo p = photoMapper.findById(id);
        if (p == null) return ResponseEntity.status(404).body(Map.of("message", "照片不存在"));
        storage.deleteQuietly(p.getAudioUrl()); // 替换旧语音

        Photo patch = new Photo();
        patch.setId(id);
        patch.setAudioUrl(storage.save(file, "audio"));
        patch.setAudioDuration(duration);
        photoMapper.updateSelective(patch);
        return ResponseEntity.ok(photoMapper.findById(id));
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static Double num(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        try {
            return o == null ? null : Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
