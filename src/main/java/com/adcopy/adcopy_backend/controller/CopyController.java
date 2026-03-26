package com.adcopy.adcopy_backend.controller;

import com.adcopy.adcopy_backend.model.GenerateRequest;
import com.adcopy.adcopy_backend.service.AiService;
import com.adcopy.adcopy_backend.service.SensitiveWordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            List<String> texts = aiService.generateCopy(
                    req.getCount(),
                    req.getTone(),
                    req.getLanguage(),
                    req.getPlatName(),
                    req.getScene(),
                    req.getExtraFields()
            );

            // 敏感词检测
            List<List<String>> detections = sensitiveWordService.detect(texts, req.getPlatName());

            List<Map<String, Object>> items = new ArrayList<>();
            int totalSensitive = 0;
            for (int i = 0; i < texts.size(); i++) {
                List<String> violations = detections.get(i);
                if (!violations.isEmpty()) totalSensitive++;

                Map<String, Object> item = new HashMap<>();
                item.put("index", i + 1);
                item.put("text", texts.get(i));

                List<Map<String, String>> sensitiveWords = new ArrayList<>();
                for (String word : violations) {
                    Map<String, String> sw = new HashMap<>();
                    sw.put("word", word);
                    sensitiveWords.add(sw);
                }
                item.put("sensitiveWords", sensitiveWords);
                items.add(item);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("items", items);
            data.put("totalSensitiveCount", totalSensitive);

            return ResponseEntity.ok(Map.of("code", 200, "data", data));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "code", 500,
                    "message", e.getMessage()
            ));
        }
    }
}