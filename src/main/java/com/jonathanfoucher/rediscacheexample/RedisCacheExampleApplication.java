package com.jonathanfoucher.rediscacheexample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class RedisCacheExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedisCacheExampleApplication.class, args);
    }
}
