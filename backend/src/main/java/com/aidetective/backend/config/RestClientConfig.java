package com.aidetective.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient asiRestClient(RestClient.Builder builder, AppProperties appProperties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(15000);
        requestFactory.setReadTimeout(60000);

        return builder
            .baseUrl(appProperties.getAsi().getBaseUrl())
            .requestFactory(requestFactory)
            .build();
    }
}
