package com.example.minitasks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MiniTasksApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniTasksApplication.class, args);
    }
}
