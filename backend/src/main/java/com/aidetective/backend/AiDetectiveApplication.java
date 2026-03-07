package com.aidetective.backend;

import com.aidetective.backend.config.AppProperties;
import com.aidetective.backend.config.ExtractionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AppProperties.class, ExtractionProperties.class})
public class AiDetectiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiDetectiveApplication.class, args);
    }
}
