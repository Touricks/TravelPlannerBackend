package org.laioffer.planner.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String openAiApiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String modelName;

    @Value("${langchain4j.open-ai.chat-model.temperature}")
    private Double temperature;

    @Value("${langchain4j.open-ai.chat-model.max-tokens}")
    private Integer maxTokens;

    @Value("${langchain4j.open-ai.chat-model.timeout}")
    private Duration timeout;

    @Value("${langchain4j.open-ai.chat-model.max-retries}")
    private Integer maxRetries;

    @Value("${langchain4j.open-ai.chat-model.log-requests}")
    private Boolean logRequests;

    @Value("${langchain4j.open-ai.chat-model.log-responses}")
    private Boolean logResponses;

}