package com.adcopy.adcopy_backend.controller;

import com.adcopy.adcopy_backend.model.DetectRequest;
import com.adcopy.adcopy_backend.model.GenerateRequest;
import com.adcopy.adcopy_backend.service.AiService;
import com.adcopy.adcopy_backend.service.SensitiveWordService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/copy")
public class CopyController {

  private final AiService aiService;
  private final SensitiveWordService sensitiveWordService;

  public CopyController(AiService aiService, SensitiveWordService sensitiveWordService) {
    this.aiService = aiService;
    this.sensitiveWordService = sensitiveWordService;
  }

  @PostMapping("/generate")
  public ResponseEntity<?> generate(@RequestBody GenerateRequest req) {
    try {
      List<String> texts =
          aiService.generateCopy(
              req.getCount(),
              req.getTone(),
              req.getLanguage(),
              req.getPlatName(),
              req.getScene(),
              req.getExtraFields());

      List<Map<String, Object>> items = new ArrayList<>();
      for (int i = 0; i < texts.size(); i++) {
        Map<String, Object> item = new HashMap<>();
        item.put("index", i + 1);
        item.put("text", texts.get(i));
        items.add(item);
      }

      return ResponseEntity.ok(Map.of("code", 200, "data", Map.of("items", items)));

    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("code", 500, "message", e.getMessage()));
    }
  }

  @PostMapping("/detect")
  public ResponseEntity<?> detect(@RequestBody DetectRequest req) {
    try {
      List<String> texts = req.getTexts();
      if (texts == null || texts.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "texts 不能为空"));
      }

      List<List<String>> detections = sensitiveWordService.detect(texts, req.getPlatName());

      List<Map<String, Object>> items = new ArrayList<>();
      int totalSensitive = 0;

      for (int i = 0; i < texts.size(); i++) {
        List<String> violations = detections.get(i);
        if (!violations.isEmpty()) totalSensitive++;

        List<Map<String, String>> sensitiveWords =
            violations.stream().map(word -> Map.of("word", word)).toList();

        Map<String, Object> item = new HashMap<>();
        item.put("index", i + 1);
        item.put("sensitiveWords", sensitiveWords);
        items.add(item);
      }

      return ResponseEntity.ok(
          Map.of(
              "code",
              200,
              "data",
              Map.of(
                  "items", items,
                  "totalSensitiveCount", totalSensitive)));

    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("code", 500, "message", e.getMessage()));
    }
  }
}
