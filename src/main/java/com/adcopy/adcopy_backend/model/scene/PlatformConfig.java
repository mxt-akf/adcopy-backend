package com.adcopy.adcopy_backend.model.scene;

import java.util.List;

public class PlatformConfig {

  private String id;
  private String name;
  private List<SceneConfig> scenes;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<SceneConfig> getScenes() {
    return scenes;
  }

  public void setScenes(List<SceneConfig> scenes) {
    this.scenes = scenes;
  }
}
