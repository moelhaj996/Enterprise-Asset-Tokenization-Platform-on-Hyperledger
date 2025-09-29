package com.enterprise.tokenization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Enterprise Asset Tokenization Platform
 *
 * @author Enterprise Blockchain Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class TokenizationApplication {

    public static void main(String[] args) {
        SpringApplication.run(TokenizationApplication.class, args);
    }
}