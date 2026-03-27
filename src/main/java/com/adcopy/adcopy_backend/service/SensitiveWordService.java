package com.adcopy.adcopy_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SensitiveWordService {

  @Value("${openai.api-key}")
  private String apiKey;

  @Value("${openai.base-url}")
  private String baseUrl;

  @Value("${openai.model}")
  private String model;

  private final OkHttpClient client;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public SensitiveWordService() {
    this.client = new OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build();
  }

  public List<List<String>> detect(List<String> texts, String platName) throws Exception {
    if (texts == null || texts.isEmpty()) return Collections.emptyList();

    StringBuilder textList = new StringBuilder();
    for (int i = 0; i < texts.size(); i++) {
      textList.append(i + 1).append(". ").append(texts.get(i)).append("\n");
    }

    String systemPrompt =
        "你是一个广告合规审核员。只返回 JSON 数组，格式："
            + "[{\"index\":1,\"violations\":[\"违禁词1\"]},{\"index\":2,\"violations\":[]},...] "
            + "没有违禁词的条目 violations 返回空数组，不要返回任何其他内容。";

    String userPrompt =
        "请检测以下广告文案是否包含违禁内容，包括：\n"
            + "- 绝对化用语（最好、第一、最低价、best、#1、guaranteed 等）\n"
            + "- 夸大宣传（100%有效、史上最、全网最 等）\n"
            + "- 虚假承诺（永久保修、无条件退款 等）\n"
            + "- Amazon、Facebook、TikTok 常见违禁词\n\n"
            + "文案列表：\n"
            + textList;

    String requestBody =
        objectMapper.writeValueAsString(
            Map.of(
                "model",
                model,
                "messages",
                List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt))));

    Request request =
        new Request.Builder()
            .url(baseUrl + "/v1/chat/completions")
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(requestBody, MediaType.get("application/json")))
            .build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new RuntimeException("敏感词检测失败: " + response.code());
      }
      String content =
          objectMapper
              .readTree(response.body().string())
              .path("choices")
              .get(0)
              .path("message")
              .path("content")
              .asText();

      int start = content.indexOf('[');
      int end = content.lastIndexOf(']');
      if (start < 0 || end <= start) {
        throw new RuntimeException("敏感词检测返回格式异常: " + content.substring(0, Math.min(content.length(), 200)));
      }
      content = content.substring(start, end + 1);

      JsonNode arr = objectMapper.readTree(content);
      List<List<String>> result = new ArrayList<>();
      for (int i = 0; i < texts.size(); i++) result.add(new ArrayList<>());

      for (JsonNode item : arr) {
        int idx = item.path("index").asInt() - 1;
        if (idx >= 0 && idx < texts.size()) {
          List<String> violations = new ArrayList<>();
          for (JsonNode v : item.path("violations")) violations.add(v.asText());
          result.set(idx, violations);
        }
      }
      return result;
    }
  }
}
