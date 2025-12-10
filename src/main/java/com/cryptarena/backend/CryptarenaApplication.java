package com.cryptarena.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CryptarenaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptarenaApplication.class, args);
    }

}

