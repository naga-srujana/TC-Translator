package com.student.tctranslator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// main entry point - just the standard spring boot thing
@SpringBootApplication
public class TcTranslatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TcTranslatorApplication.class, args);
        System.out.println("\n🚀 T&C Translator is running at http://localhost:8080\n");
    }
}
