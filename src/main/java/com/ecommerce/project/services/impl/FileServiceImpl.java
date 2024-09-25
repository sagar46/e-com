package com.ecommerce.project.services.impl;

import com.ecommerce.project.services.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Override
    public String uploadImage(String path, MultipartFile file) throws IOException {
        log.debug("FileServiceImpl.uploadImage call started...");
        String originalFilename = file.getOriginalFilename();
        String newFileName = UUID.randomUUID().toString()
                .concat(originalFilename.substring(originalFilename.lastIndexOf(".")));
        String filepath = path + File.separator + newFileName;

        File folder = new File(path);

        if (!folder.exists()) {
            folder.mkdirs();
        }
        Files.copy(file.getInputStream(), Paths.get(filepath));
        log.debug("FileServiceImpl.uploadImage call completed...");
        return newFileName;
    }
}
