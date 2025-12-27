package com.rakeshgupta.fastchat_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FastchatApplication {

	public static void main(String[] args) {
		SpringApplication.run(FastchatApplication.class, args);
	}

}
