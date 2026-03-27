package com.adcopy.adcopy_backend.model;

import java.util.Map;

public class GenerateRequest {
  private Integer count = 10;
  private String tone = "专业的";
  private String language = "中文"; // 新增
  private String platName;
  private String scene;
  private Map<String, String> extraFields;

  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }

  public String getTone() {
    return tone;
  }

  public void setTone(String tone) {
    this.tone = tone;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getPlatName() {
    return platName;
  }

  public void setPlatName(String platName) {
    this.platName = platName;
  }

  public String getScene() {
    return scene;
  }

  public void setScene(String scene) {
    this.scene = scene;
  }

  public Map<String, String> getExtraFields() {
    return extraFields;
  }

  public void setExtraFields(Map<String, String> extraFields) {
    this.extraFields = extraFields;
  }
}
