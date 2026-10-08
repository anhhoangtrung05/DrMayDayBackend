package com.drmayday.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.drmayday")
@EntityScan(basePackages = "com.drmayday")
@EnableJpaRepositories(basePackages = "com.drmayday")
public class DrMayDayApplication {

    public static void main(String[] args) {
        SpringApplication.run(DrMayDayApplication.class, args);
    }
}
