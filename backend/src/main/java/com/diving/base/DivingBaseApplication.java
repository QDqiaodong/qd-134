package com.diving.base;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class DivingBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(DivingBaseApplication.class, args);
    }
}