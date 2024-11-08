package com.example.s3storage;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class S3storageApplication {

	public static void main(String[] args) {
		SpringApplication.run(S3storageApplication.class, args);
	}

}
