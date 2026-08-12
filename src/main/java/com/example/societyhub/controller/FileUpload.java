package com.example.societyhub.controller;

import com.example.societyhub.service.ExcelService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;

@RestController
@RequestMapping("/api")
public class FileUpload {

    private static final Logger log = LogManager.getLogger(FileUpload.class);

    private final ExcelService excelService;

    public FileUpload(ExcelService excelService) {
        this.excelService = excelService;
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Test endpoint working");
    }

    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(
            @RequestParam("file") MultipartFile file,
            HttpSession session
    ) {

        Integer sid = (Integer) session.getAttribute("adminSocietyId");

        if (sid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Session expired. Please login again.");
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("No file selected");
        }

        try (InputStream inputStream = file.getInputStream()) {

            log.info("File received: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

            excelService.processExcelStream(inputStream, sid);

            return ResponseEntity.ok("File processed successfully");

        } catch (Exception e) {
            log.error("File upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File processing failed: " + e.getMessage());
        }
    }
}

