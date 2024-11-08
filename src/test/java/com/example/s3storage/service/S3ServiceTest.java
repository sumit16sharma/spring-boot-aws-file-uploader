package com.example.s3storage.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.example.s3storage.exception.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class S3ServiceTest {
  @Mock
  private AmazonS3 amazonS3;

  @InjectMocks
  private S3Service s3Service;

  private static final String BUCKET_NAME = "test-bucket";
  private static final String USER_NAME = "testUser";
  private static final String FILE_NAME = "test.txt";
  private static final String FILE_CONTENT = "Hello, World!";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(s3Service, "bucketName", BUCKET_NAME);
  }

  @Test
  void uploadFile_Success() throws IOException, FileStorageException {
    // Arrange
    MockMultipartFile multipartFile = new MockMultipartFile(
      "file",
      FILE_NAME,
      "text/plain",
      FILE_CONTENT.getBytes()
    );

    // Act
    s3Service.uploadFile(USER_NAME, multipartFile);

    // Assert
    verify(amazonS3).putObject(
      eq(BUCKET_NAME),
      eq(USER_NAME + "/" + FILE_NAME),
      any(InputStream.class),
      any(ObjectMetadata.class)
    );
  }

  @Test
  void uploadFile_WhenS3Throws_ShouldThrowFileStorageException() throws IOException {
    // Arrange
    MockMultipartFile multipartFile = new MockMultipartFile(
      "file",
      FILE_NAME,
      "text/plain",
      FILE_CONTENT.getBytes()
    );

    doThrow(new AmazonS3Exception("Upload failed"))
      .when(amazonS3)
      .putObject(any(String.class), any(String.class), any(InputStream.class), any(ObjectMetadata.class));

    // Act & Assert
    assertThrows(FileStorageException.class, () -> {
      s3Service.uploadFile(USER_NAME, multipartFile);
    });
  }

  @Test
  void downloadFile_Success() throws IOException, FileStorageException {
    // Arrange
    S3Object s3Object = mock(S3Object.class);
    S3ObjectInputStream inputStream = new S3ObjectInputStream(
      new ByteArrayInputStream(FILE_CONTENT.getBytes()),
      null
    );

    when(amazonS3.getObject(BUCKET_NAME, USER_NAME + "/" + FILE_NAME)).thenReturn(s3Object);
    when(s3Object.getObjectContent()).thenReturn(inputStream);

    // Act
    ResponseEntity<byte[]> response = s3Service.downloadFile(USER_NAME, FILE_NAME);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertArrayEquals(FILE_CONTENT.getBytes(), response.getBody());
    assertTrue(response.getHeaders().getContentDisposition().toString().contains(FILE_NAME));
  }

  @Test
  void downloadFile_WhenFileNotFound_ShouldThrowFileStorageException() {
    // Arrange
    when(amazonS3.getObject(any(String.class), any(String.class)))
      .thenThrow(new AmazonS3Exception("File not found"));

    // Act & Assert
    assertThrows(FileStorageException.class, () -> {
      s3Service.downloadFile(USER_NAME, FILE_NAME);
    });
  }
}
