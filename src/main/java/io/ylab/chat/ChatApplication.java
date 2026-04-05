package io.ylab.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the Chat Application.
 */
@SpringBootApplication
@EnableAspectJAutoProxy
@EnableAsync
public class ChatApplication {

    /**
     * Launches the Spring Boot chat application.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }
}