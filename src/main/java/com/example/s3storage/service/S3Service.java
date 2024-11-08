package com.example.s3storage.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.example.s3storage.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3Service {

  private final AmazonS3 amazonS3;

  public S3Service(AmazonS3 amazonS3) {
    this.amazonS3 = amazonS3;
  }

  @Value("${s3.bucket.name}")
  private String bucketName;

  // Uploads a file to S3 under a specific user's folder
  public void uploadFile(String userName, MultipartFile file) throws IOException, FileStorageException {
    String filePath = userName + "/" + file.getOriginalFilename();
    try {
      amazonS3.putObject(bucketName, filePath, file.getInputStream(), new ObjectMetadata());
    } catch (AmazonS3Exception e) {
      throw new FileStorageException("Failed to upload file to S3: " + e.getMessage());
    }
  }

  public ResponseEntity<byte[]> downloadFile(String userName, String fileName) throws IOException, FileStorageException {
    String filePath = userName + "/" + fileName;

    try {
      S3Object s3Object = amazonS3.getObject(bucketName, filePath);
      byte[] fileBytes = IOUtils.toByteArray(s3Object.getObjectContent());

      // Determine content type based on file extension
      String contentType = Files.probeContentType(Paths.get(fileName));
      if (contentType == null) {
        contentType = "application/octet-stream"; // Default to binary data
      }

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.parseMediaType(contentType));
      headers.setContentDisposition(ContentDisposition.builder("attachment").filename(fileName).build());
      headers.setContentLength(fileBytes.length);

      return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    } catch (AmazonS3Exception e) {
      throw new FileStorageException("File not found or unable to download: " + e.getMessage());
    }
  }

  // Method to test the S3 connection
  public List<String> listBuckets() {
    return amazonS3.listBuckets().stream()
      .map(bucket -> bucket.getName())
      .collect(Collectors.toList());
  }
}
