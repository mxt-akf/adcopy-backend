package com.adcopy.adcopy_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SensitiveWordService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<List<String>> detect(List<String> texts) throws Exception {
        if (texts == null || texts.isEmpty()) return Collections.emptyList();

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        // 构建待检测文案列表
        StringBuilder textList = new StringBuilder();
        for (int i = 0; i < texts.size(); i++) {
            textList.append((i + 1)).append(". ").append(texts.get(i)).append("\n");
        }

        String prompt = "以下是一批广告文案，请检测每条是否包含违禁内容，包括：\n" +
                "- 绝对化用语（最好、第一、最低价、best、#1、guaranteed 等）\n" +
                "- 夸大宣传（100%有效、史上最、全网最 等）\n" +
                "- 虚假承诺（永久保修、无条件退款 等）\n" +
                "- 各主流电商平台（Amazon、Facebook、TikTok）常见违禁词\n\n" +
                "文案列表：\n" + textList +
                "\n只返回 JSON 数组，格式：\n" +
                "[{\"index\":1,\"violations\":[\"违禁词1\",\"违禁词2\"]},{\"index\":2,\"violations\":[]},...]\n" +
                "没有违禁词的条目 violations 返回空数组，不要返回任何其他内容。";

        String requestBody = objectMapper.writeValueAsString(new HashMap<String, Object>() {{
            put("model", model);
            put("messages", List.of(new HashMap<String, String>() {{
                put("role", "user");
                put("content", prompt);
            }}));
        }});

        Request request = new Request.Builder()
                .url(baseUrl + "/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body().string();
            if (!response.isSuccessful()) {
                throw new RuntimeException("敏感词检测失败: " + response.code());
            }

            String content = objectMapper.readTree(body)
                    .path("choices").get(0)
                    .path("message").path("content").asText();

            // 清理 markdown 代码块格式
            content = content.replaceAll("```json", "").replaceAll("```", "").trim();

            JsonNode arr = objectMapper.readTree(content);
            List<List<String>> result = new ArrayList<>();
            // 初始化每条文案的结果为空列表
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