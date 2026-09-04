package com.conectatech.sgs_backend;

import io.mongock.runner.springboot.EnableMongock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableMongock
public class SgsBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SgsBackendApplication.class, args);
	}

}
