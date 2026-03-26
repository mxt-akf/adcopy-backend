package com.adcopy.adcopy_backend.model;

import java.util.List;

public class GenerateResponse {
    private Integer code;
    private ResponseData data;

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public ResponseData getData() { return data; }
    public void setData(ResponseData data) { this.data = data; }

    public static class ResponseData {
        private List<CopyItem> items;
        private Integer totalSensitiveCount;

        public List<CopyItem> getItems() { return items; }
        public void setItems(List<CopyItem> items) { this.items = items; }
        public Integer getTotalSensitiveCount() { return totalSensitiveCount; }
        public void setTotalSensitiveCount(Integer totalSensitiveCount) { this.totalSensitiveCount = totalSensitiveCount; }
    }

    public static class CopyItem {
        private Integer index;
        private String text;
        private List<SensitiveWord> sensitiveWords;

        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public List<SensitiveWord> getSensitiveWords() { return sensitiveWords; }
        public void setSensitiveWords(List<SensitiveWord> sensitiveWords) { this.sensitiveWords = sensitiveWords; }
    }

    public static class SensitiveWord {
        private String word;
        private Integer start;
        private Integer end;

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }
        public Integer getStart() { return start; }
        public void setStart(Integer start) { this.start = start; }
        public Integer getEnd() { return end; }
        public void setEnd(Integer end) { this.end = end; }
    }
}