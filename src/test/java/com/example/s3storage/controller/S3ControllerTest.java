package com.example.s3storage.controller;

import com.example.s3storage.exception.FileStorageException;
import com.example.s3storage.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class S3ControllerTest {

  @Mock
  private S3Service s3Service;

  @InjectMocks
  private S3Controller s3Controller;

  private MockMvc mockMvc;

  private static final String USER_NAME = "testUser";
  private static final String FILE_NAME = "test.txt";
  private static final String FILE_CONTENT = "Hello, World!";

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(s3Controller).build();
  }

  @Test
  void uploadFile_Success() throws Exception {
    // Arrange
    MockMultipartFile file = new MockMultipartFile(
      "file",
      FILE_NAME,
      MediaType.TEXT_PLAIN_VALUE,
      FILE_CONTENT.getBytes()
    );

    doNothing().when(s3Service).uploadFile(eq(USER_NAME), any(MockMultipartFile.class));

    // Act & Assert
    mockMvc.perform(multipart("/api/s3/upload")
        .file(file)
        .param("userName", USER_NAME))
      .andExpect(status().isOk())
      .andExpect(content().string("File uploaded successfully!"));

    verify(s3Service).uploadFile(eq(USER_NAME), any(MockMultipartFile.class));
  }

  @Test
  void uploadFile_WhenServiceThrowsException() throws Exception {
    // Arrange
    MockMultipartFile file = new MockMultipartFile(
      "file",
      FILE_NAME,
      MediaType.TEXT_PLAIN_VALUE,
      FILE_CONTENT.getBytes()
    );

    doThrow(new FileStorageException("Upload failed"))
      .when(s3Service)
      .uploadFile(eq(USER_NAME), any(MockMultipartFile.class));

    // Act & Assert
    mockMvc.perform(multipart("/api/s3/upload")
        .file(file)
        .param("userName", USER_NAME))
      .andExpect(status().isInternalServerError())
      .andExpect(content().string("Error uploading file: Upload failed"));
  }

  @Test
  void downloadFile_Success() throws Exception {
    // Arrange
    byte[] fileContent = FILE_CONTENT.getBytes();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);

    when(s3Service.downloadFile(USER_NAME, FILE_NAME))
      .thenReturn(new ResponseEntity<>(fileContent, headers, HttpStatus.OK));

    // Act & Assert
    mockMvc.perform(get("/api/s3/download")
        .param("userName", USER_NAME)
        .param("fileName", FILE_NAME))
      .andExpect(status().isOk())
      .andExpect(content().bytes(fileContent));

    verify(s3Service).downloadFile(USER_NAME, FILE_NAME);
  }

  @Test
  void downloadFile_WhenFileNotFound() throws Exception {
    // Arrange
    when(s3Service.downloadFile(USER_NAME, FILE_NAME))
      .thenThrow(new FileStorageException("File not found"));

    // Act & Assert
    mockMvc.perform(get("/api/s3/download")
        .param("userName", USER_NAME)
        .param("fileName", FILE_NAME))
      .andExpect(status().isInternalServerError())
      .andExpect(content().string("Error: File not found"));
  }
}
