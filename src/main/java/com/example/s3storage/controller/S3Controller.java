package com.example.s3storage.controller;

import com.example.s3storage.exception.FileStorageException;
import com.example.s3storage.service.S3Service;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/s3")
public class S3Controller {
  private final S3Service s3Service;

  // Constructor injection
  public S3Controller(S3Service s3Service) {
    this.s3Service = s3Service;
  }

  // Endpoint to upload a file for a user
  @PostMapping("/upload")
  public ResponseEntity<String> uploadFile(
    @RequestParam("userName") String userName,
    @RequestParam("file") MultipartFile file) {
    try {
      s3Service.uploadFile(userName, file);
      return ResponseEntity.ok("File uploaded successfully!");
    } catch (IOException | FileStorageException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("Error uploading file: " + e.getMessage());
    }
  }

  @GetMapping("/download")
  public ResponseEntity<byte[]> downloadFile(
    @RequestParam("userName") String userName,
    @RequestParam("fileName") String fileName) {
    try {
      // Call the service to handle the download
      return s3Service.downloadFile(userName, fileName);
    } catch (FileStorageException | IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(("Error: " + e.getMessage()).getBytes());
    }
  }

  @GetMapping("/connection")
  public List<String> testS3Connection() {
    return s3Service.listBuckets();
  }
}
