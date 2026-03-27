package com.adcopy.adcopy_backend.service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");

  private final SceneConfigLoader sceneConfigLoader;

  public PromptBuilder(SceneConfigLoader sceneConfigLoader) {
    this.sceneConfigLoader = sceneConfigLoader;
  }

  public String build(
      int count,
      String tone,
      String language,
      String platName,
      String scene,
      Map<String, String> extraFields,
      String format) {
    String template = sceneConfigLoader.getTemplate(scene);

    Map<String, String> vars = new HashMap<>(extraFields != null ? extraFields : Map.of());
    vars.put("platName", platName);

    String originalCopy = vars.get("originalCopy");
    if (originalCopy == null || originalCopy.isBlank()) {
      vars.put("originalCopy", "请从零生成全新文案，不要参考任何已有内容。");
    }

    String languageInstruction =
        String.format("【语言要求 - 最高优先级】所有输出内容必须使用「%s」，严禁使用其他任何语言。\n\n", language);

    String finalPrompt =
        languageInstruction
            + fillTemplate(template, vars)
            + buildOutputRequirements(count, tone, language, format);
    System.out.println(
        "========== PROMPT START ==========\n"
            + finalPrompt
            + "\n========== PROMPT END ==========");
    return finalPrompt;
  }

  private String fillTemplate(String template, Map<String, String> vars) {
    Matcher matcher = PLACEHOLDER.matcher(template);
    return matcher.replaceAll(
        m -> {
          String val = vars.get(m.group(1));
          return Matcher.quoteReplacement((val != null && !val.isBlank()) ? val : "（未提供）");
        });
  }

  private String buildOutputRequirements(int count, String tone, String language, String format) {
    if ("bullets".equals(format)) {
      return String.format(
          """

                【输出要求】
                - 生成 %d 套完整变体，每套固定包含 5 个 Bullet Points
                - 套与套之间用单独一行「---」分隔
                - 每套内部 5 条之间用换行分隔，不加「---」
                - 第一套直接开始输出，不要有任何前置说明
                - 不加编号，不加多余符号
                - 语气：%s
                - 输出语言：%s，所有文案必须使用该语言，不得混入其他语言""",
          count, tone, language);
    }

    return String.format(
        """

            【输出要求】
            - 生成 %d 条独立变体
            - 每条变体之间必须用单独一行「---」分隔，即上一条结束后换行写---再换行写下一条
            - 第一条直接开始输出，不要有任何前置说明
            - 不加编号，不加多余符号
            - 语气：%s
            - 输出语言：%s，所有文案必须使用该语言，不得混入其他语言""",
        count, tone, language);
  }
}
