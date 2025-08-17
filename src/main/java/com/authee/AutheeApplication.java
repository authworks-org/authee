package com.authee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AutheeApplication {
	public static void main(String[] args) {
		SpringApplication.run(AutheeApplication.class, args);
		System.out.println("Authee authentication server started!");
	}
}
