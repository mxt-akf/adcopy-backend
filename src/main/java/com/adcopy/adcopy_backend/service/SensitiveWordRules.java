package com.adcopy.adcopy_backend.service;

import java.util.Map;

public class SensitiveWordRules {

  // 通用规则（所有平台都检测）
  private static final String COMMON_RULES =
      "- 绝对化用语（最好、第一、最低价、best、#1、guaranteed、唯一）\n"
          + "- 夸大宣传（100%有效、史上最、全网最、秒杀一切）\n"
          + "- 虚假承诺（永久保修、无条件退款、永不过期）\n"
          + "- 涉及医疗疗效的表述（治愈、根治、临床证明）";

  // 各平台专属规则
  private static final Map<String, String> PLATFORM_RULES =
      Map.of(
          "Amazon",
          "- 与竞品直接对比的贬低性描述\n"
              + "- #1 Best Seller（未经亚马逊认证的自称）\n"
              + "- 含诱导好评的表述（leave a 5-star review）\n"
              + "- 含价格承诺（lowest price、price match）",
          "TikTok Shop",
          "- 夸大功效（一用就瘦、立竿见影）\n" + "- 不实用户证言（所有人都说好）\n" + "- 含诱导关注/点赞的表述\n" + "- 敏感品类词（减肥、丰胸、壮阳）",
          "Facebook / Instagram",
          "- 误导性的财务回报承诺（每天躺赚）\n"
              + "- 歧视性定向表述\n"
              + "- 含 '点击' 诱导词（click now、点我）的强迫性表述\n"
              + "- before/after 对比用于健康类产品",
          "Shopify 独立站",
          "- 未经认证的权威背书（FDA approved、临床认证）\n" + "- 夸大退货政策（永久退款）\n" + "- 虚假紧迫感（仅剩1件、今天必须下单）");

  public static String getRules(String platName) {
    String platformSpecific = PLATFORM_RULES.getOrDefault(platName, "");
    if (platformSpecific.isBlank()) {
      return COMMON_RULES;
    }
    return COMMON_RULES + "\n" + "- 【" + platName + " 专属】\n" + platformSpecific;
  }
}
