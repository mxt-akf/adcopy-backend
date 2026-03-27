package com.adcopy.adcopy_backend.model.scene;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SceneConfig {

  private String name;
  private String riskLevel;
  private String format; // 新增

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String template;

  private List<FieldConfig> fields;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRiskLevel() {
    return riskLevel;
  }

  public void setRiskLevel(String riskLevel) {
    this.riskLevel = riskLevel;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  public List<FieldConfig> getFields() {
    return fields;
  }

  public void setFields(List<FieldConfig> fields) {
    this.fields = fields;
  }
}
