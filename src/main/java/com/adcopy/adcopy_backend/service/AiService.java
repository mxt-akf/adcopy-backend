package com.adcopy.adcopy_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model}")
    private String model;

    private final OkHttpClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PromptBuilder promptBuilder;

    public AiService(OkHttpClient client, PromptBuilder promptBuilder) {
        this.client = client;
        this.promptBuilder = promptBuilder;
    }

    public List<String> generateCopy(int count, String tone,String language,
                                     String platName, String scene,
                                     Map<String, String> extraFields) throws Exception {

        String prompt = promptBuilder.build(platName, scene, count, tone, language, extraFields);

        log.debug("=== [AiService] 文案生成 Prompt ===\n{}", prompt);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );
        String requestBody = objectMapper.writeValueAsString(body);

        Request request = new Request.Builder()
                .url(baseUrl + "/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("API 调用失败: " + response.code() + " " + response.body().string());
            }
            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            content = content.replace("\\n", "\n");

            List<String> result = new ArrayList<>();
            for (String block : content.split("(?m)^\\s*---\\s*$")) {
                String trimmed = block.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
            if (result.size() > count) {
                result = result.subList(0, count);
            }
            return result;
        }
    }
}