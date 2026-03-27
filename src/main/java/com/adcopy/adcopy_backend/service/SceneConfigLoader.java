package com.adcopy.adcopy_backend.service;

import com.adcopy.adcopy_backend.model.scene.PlatformConfig;
import com.adcopy.adcopy_backend.model.scene.SceneConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class SceneConfigLoader {

  private List<PlatformConfig> platforms = new ArrayList<>();

  @PostConstruct
  public void load() throws Exception {
    ObjectMapper mapper =
        new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    var resource = new ClassPathResource("scenes.yml");
    ScenesRoot root = mapper.readValue(resource.getInputStream(), ScenesRoot.class);
    this.platforms = root.getPlatforms() != null ? root.getPlatforms() : List.of();
  }

  public List<PlatformConfig> getPlatforms() {
    return platforms;
  }

  public String getTemplate(String sceneName) {
    return platforms.stream()
        .flatMap(p -> p.getScenes().stream())
        .filter(s -> s.getName().equals(sceneName))
        .findFirst()
        .map(SceneConfig::getTemplate)
        .orElseThrow(() -> new IllegalArgumentException("未知场景: " + sceneName));
  }

  public SceneConfig getScene(String sceneName) {
    return platforms.stream()
        .flatMap(p -> p.getScenes().stream())
        .filter(s -> s.getName().equals(sceneName))
        .findFirst()
        .orElse(null);
  }

  public String getFormat(String sceneName) {
    SceneConfig scene = getScene(sceneName);
    return scene != null ? scene.getFormat() : null;
  }

  // YAML 根节点映射类
  private static class ScenesRoot {
    private List<PlatformConfig> platforms;

    public List<PlatformConfig> getPlatforms() {
      return platforms;
    }

    public void setPlatforms(List<PlatformConfig> platforms) {
      this.platforms = platforms;
    }
  }
}
