package com.chirag.train_management_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrainManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainManagementSystemApplication.class, args);
    }
}
