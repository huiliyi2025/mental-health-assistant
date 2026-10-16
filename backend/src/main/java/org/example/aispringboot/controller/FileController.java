package org.example.aispringboot.controller;

import lombok.RequiredArgsConstructor;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.entity.SysFile;
import org.example.aispringboot.service.FileService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传接口
 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam(required = false) String businessType,
                                              @RequestParam(required = false) String businessId,
                                              @RequestParam(required = false) String businessField) {
        SysFile sysFile = fileService.upload(file, businessType, businessId, businessField);
        Map<String, Object> data = new HashMap<>();
        data.put("id", sysFile.getId());
        data.put("filePath", sysFile.getFilePath());
        data.put("fileName", sysFile.getFileName());
        data.put("fileSize", sysFile.getFileSize());
        return Result.ok(data);
    }
}
