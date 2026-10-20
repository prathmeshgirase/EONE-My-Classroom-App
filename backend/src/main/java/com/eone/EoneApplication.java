package com.eone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EoneApplication {
    public static void main(String[] args) {
        DatabaseTlsDiagnostic.run();
        SpringApplication.run(EoneApplication.class, args);
    }
}
