package com.adcopy.adcopy_backend.model;

import java.util.List;

public class DetectRequest {

  private List<String> texts;
  private String platName;

  public List<String> getTexts() {
    return texts;
  }

  public void setTexts(List<String> texts) {
    this.texts = texts;
  }

  public String getPlatName() {
    return platName;
  }

  public void setPlatName(String platName) {
    this.platName = platName;
  }
}
