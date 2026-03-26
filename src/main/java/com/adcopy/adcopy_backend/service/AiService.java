package com.adcopy.adcopy_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Map;

@Service
public class AiService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.timeout}")
    private int timeout;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PromptBuilder promptBuilder;

    public AiService(PromptBuilder promptBuilder) {
        this.promptBuilder = promptBuilder;
    }

    public List<String> generateCopy(int count, String tone,
                                     String platName, String scene,
                                     Map<String, String> extraFields) throws Exception {

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build();

        String prompt = promptBuilder.build(platName, scene, count, tone, extraFields);

        String requestBody = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("model", model);
            put("messages", List.of(
                    new java.util.HashMap<String, String>() {{
                        put("role", "user");
                        put("content", prompt);
                    }}
            ));
        }});

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
            // 按独立一行的 --- 分割，精确匹配避免误切段落内容
            List<String> result = new ArrayList<>();
            for (String block : content.split("(?m)^\\s*---\\s*$")) {
                String trimmed = block.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
            // 截断到请求数量，防止模型多输出
            if (result.size() > count) {
                result = result.subList(0, count);
            }
            return result;
        }
    }
}