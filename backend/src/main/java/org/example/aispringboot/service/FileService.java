package org.example.aispringboot.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.example.aispringboot.entity.SysFile;
import org.example.aispringboot.exception.BusinessException;
import org.example.aispringboot.mapper.SysFileMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 文件服务：保存上传文件并落库。
 */
@Service
@RequiredArgsConstructor
public class FileService {

    /** 单文件最大 5MB */
    private static final long MAX_SIZE = 5 * 1024 * 1024L;

    /** 允许的图片扩展名 */
    private static final List<String> ALLOWED_EXTENSIONS =
            List.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    private final SysFileMapper fileMapper;

    /** 文件存储根目录，可通过 file.upload-dir 配置 */
    @Value("${file.upload-dir:./upload}")
    private String uploadDir;

    public SysFile upload(MultipartFile file, String businessType, String businessId, String businessField) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("文件大小不能超过 5MB");
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (StrUtil.isNotBlank(originalName) && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        }
        if (StrUtil.isBlank(ext) || !ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("不支持的文件类型，仅允许图片：jpg/jpeg/png/gif/webp/bmp");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("仅支持上传图片文件");
        }

        String type = StrUtil.isBlank(businessType) ? "common" : businessType;
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String relativeDir = "files/bussiness/" + type;

        try {
            Path dir = Paths.get(uploadDir, relativeDir);
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName).toAbsolutePath().toFile());
        } catch (IOException e) {
            throw new BusinessException("文件上传失败");
        }

        String filePath = "/" + relativeDir + "/" + fileName;
        SysFile sysFile = SysFile.builder()
                .businessType(type)
                .businessId(businessId)
                .businessField(businessField)
                .fileName(originalName)
                .filePath(filePath)
                .fileSize(file.getSize())
                .createdAt(LocalDateTime.now())
                .build();
        fileMapper.insert(sysFile);
        return sysFile;
    }
}
