package com.beats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BeatsApplication {
    public static void main(String[] args) {
        SpringApplication.run(BeatsApplication.class, args);
    }
}
