package com.scoutgg.riotapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RiotApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(RiotApiApplication.class, args);
    }
}
