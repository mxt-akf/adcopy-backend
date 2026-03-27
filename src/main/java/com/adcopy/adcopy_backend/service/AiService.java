package com.adcopy.adcopy_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiService {

  @Value("${openai.api-key}")
  private String apiKey;

  @Value("${openai.base-url}")
  private String baseUrl;

  @Value("${openai.model}")
  private String model;

  private final OkHttpClient client;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PromptBuilder promptBuilder;
  private final SceneConfigLoader sceneConfigLoader;

  public AiService(
      PromptBuilder promptBuilder,
      SceneConfigLoader sceneConfigLoader,
      @Value("${openai.timeout}") int timeout) {
    this.promptBuilder = promptBuilder;
    this.sceneConfigLoader = sceneConfigLoader;
    this.client =
        new OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .build();
  }

  public List<String> generateCopy(
      int count,
      String tone,
      String language,
      String platName,
      String scene,
      Map<String, String> extraFields)
      throws Exception {
    String format = sceneConfigLoader.getFormat(scene);
    String prompt =
        promptBuilder.build(count, tone, language, platName, scene, extraFields, format);

    String requestBody =
        objectMapper.writeValueAsString(
            Map.of("model", model, "messages", List.of(Map.of("role", "user", "content", prompt))));

    Request request =
        new Request.Builder()
            .url(baseUrl + "/v1/chat/completions")
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(requestBody, MediaType.get("application/json")))
            .build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new RuntimeException("API 调用失败: " + response.code() + " " + response.body().string());
      }
      String content =
          objectMapper
              .readTree(response.body().string())
              .path("choices")
              .get(0)
              .path("message")
              .path("content")
              .asText();

      content = content.replace("\\n", "\n");

      List<String> result = new ArrayList<>();
      for (String block : content.split("(?m)^\\s*---\\s*$")) {
        String trimmed = block.trim();
        if (!trimmed.isEmpty()) result.add(trimmed);
      }

      return result.size() > count ? result.subList(0, count) : result;
    }
  }
}
