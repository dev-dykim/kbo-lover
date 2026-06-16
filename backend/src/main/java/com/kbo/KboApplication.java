package com.kbo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KboApplication {

    public static void main(String[] args) {
        SpringApplication.run(KboApplication.class, args);
    }
}
