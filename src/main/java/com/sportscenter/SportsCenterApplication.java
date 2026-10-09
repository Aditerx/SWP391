package com.sportscenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SportsCenterApplication {
    public static void main(String[] args) {
        SpringApplication.run(SportsCenterApplication.class, args);
    }
}
