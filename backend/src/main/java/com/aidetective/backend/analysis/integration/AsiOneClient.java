package com.aidetective.backend.analysis.integration;

import com.aidetective.backend.analysis.service.PromptFactory;
import com.aidetective.backend.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;

/**
 * Legacy compatibility wrapper kept to avoid breaking old imports while the codebase
 * migrates to GradientAiClient naming.
 */
@Deprecated(forRemoval = true)
public class AsiOneClient extends GradientAiClient {

    public AsiOneClient(
        RestClient aiRestClient,
        ObjectMapper objectMapper,
        AppProperties appProperties,
        PromptFactory promptFactory
    ) {
        super(aiRestClient, objectMapper, appProperties, promptFactory);
    }
}
