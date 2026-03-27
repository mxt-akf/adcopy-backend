package com.adcopy.adcopy_backend.controller;

import com.adcopy.adcopy_backend.service.SceneConfigLoader;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scenes")
public class SceneController {

  private final SceneConfigLoader sceneConfigLoader;

  public SceneController(SceneConfigLoader sceneConfigLoader) {
    this.sceneConfigLoader = sceneConfigLoader;
  }

  // 返回所有平台及其场景字段配置（template 已被 @JsonIgnore 过滤）
  @GetMapping
  public ResponseEntity<?> getScenes() {
    return ResponseEntity.ok(Map.of("code", 200, "data", sceneConfigLoader.getPlatforms()));
  }
}
