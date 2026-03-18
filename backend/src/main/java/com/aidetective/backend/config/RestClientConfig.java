package com.aidetective.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient aiRestClient(RestClient.Builder builder, AppProperties appProperties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(15000);
        requestFactory.setReadTimeout(60000);

        String baseUrl = appProperties.getAi().getBaseUrl();
        RestClient.Builder configuredBuilder = builder.requestFactory(requestFactory);

        if (baseUrl == null || baseUrl.isBlank()) {
            return configuredBuilder.build();
        }

        return configuredBuilder
            .baseUrl(baseUrl)
            .build();
    }
}
