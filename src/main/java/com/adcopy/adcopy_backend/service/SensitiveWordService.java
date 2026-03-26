package com.adcopy.adcopy_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SensitiveWordService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model}")
    private String model;

    private final OkHttpClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SensitiveWordService(OkHttpClient client) {
        this.client = client;
    }

    public List<List<String>> detect(List<String> texts, String platName) throws Exception {
        if (texts == null || texts.isEmpty()) return Collections.emptyList();

        StringBuilder textList = new StringBuilder();
        for (int i = 0; i < texts.size(); i++) {
            textList.append((i + 1)).append(". ").append(texts.get(i)).append("\n");
        }

        // 动态获取该平台的检测规则
        String rules = SensitiveWordRules.getRules(platName);

        String prompt = String.format("""
            你是一位跨境电商合规专家，平台：%s。
            请检测以下每条广告文案是否包含违禁内容：
            %s
            
            文案列表：
            %s
            只返回 JSON 数组，格式：
            [{"index":1,"violations":["违禁词1"]},{"index":2,"violations":[]}]
            没有违禁词的 violations 返回空数组，不要返回任何其他内容。
            """, platName, rules, textList);

        log.debug("=== [SensitiveWordService] 敏感词检测 Prompt ===\n{}", prompt);

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
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                throw new RuntimeException("敏感词检测失败: " + response.code());
            }

            String content = objectMapper.readTree(responseBody)
                    .path("choices").get(0)
                    .path("message").path("content").asText();

            content = content.replaceAll("```json", "").replaceAll("```", "").trim();

            JsonNode arr = objectMapper.readTree(content);
            List<List<String>> result = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                result.add(new ArrayList<>());
            }
            for (JsonNode item : arr) {
                int idx = item.path("index").asInt() - 1;
                if (idx >= 0 && idx < texts.size()) {
                    List<String> violations = new ArrayList<>();
                    for (JsonNode v : item.path("violations")) {
                        violations.add(v.asText());
                    }
                    result.set(idx, violations);
                }
            }
            return result;
        }
    }
}