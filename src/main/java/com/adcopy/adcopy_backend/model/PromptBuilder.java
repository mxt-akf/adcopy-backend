package com.adcopy.adcopy_backend.service;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class PromptBuilder {

    public String build(String platName, String scene,
                        int count, String tone, Map<String, String> extraFields) {
        String scenePrompt = buildScenePrompt(platName, scene, extraFields);
        return scenePrompt + String.format(
                "\n\n【输出要求】\n" +
                        "- 生成 %d 条独立变体\n" +
                        "- 每条变体之间必须用单独一行「---」分隔，即上一条结束后换行写---再换行写下一条\n" +
                        "- 不加编号，不加多余符号\n" +
                        "- 语气：%s",
                count, tone);
    }

    private String get(Map<String, String> extra, String key) {
        String val = extra == null ? null : extra.get(key);
        return (val != null && !val.isBlank()) ? val : null; // 改为返回 null
    }

    private String buildScenePrompt(String platName, String scene, Map<String, String> extra) {
        String base = String.format("你是一位资深跨境广告文案专家，平台：%s。\n", platName);

        String originalCopy = get(extra, "originalCopy");
        String copyPart = originalCopy != null
                ? "原始文案：" + originalCopy
                : "请从零生成全新文案，不要参考任何已有内容。";

        return base + switch (scene) {

            // ── Amazon ──────────────────────────────────────────
            case "产品标题" -> String.format("""
                    任务：为以下商品生成适合 %s 平台的产品标题，需包含核心关键词，控制在200字符以内。
                    产品品类：%s
                    核心关键词：%s
                    原始文案：%s""",
                    platName, get(extra, "category"), get(extra, "keywords"), get(extra, "originalCopy"));

            case "5点描述 Bullet Points" -> String.format("""
                    任务：基于以下内容生成5条 Bullet Points，每条突出一个核心卖点，以大写词开头。
                    核心卖点：%s
                    目标受众：%s
                    原始文案：%s""",
                    get(extra, "sellingPoints"), get(extra, "targetAudience"), get(extra, "originalCopy"));

            case "产品描述（段落式）" -> String.format("""
                    任务：生成一段完整的产品描述，讲述使用场景和情感价值，200-300词。
                    目标受众：%s
                    使用场景：%s
                    品牌调性：%s
                    原始文案：%s""",
                    get(extra, "targetAudience"), get(extra, "useScene"),
                    get(extra, "brandTone"), get(extra, "originalCopy"));

            case "A+ 内容标题" -> String.format("""
                    任务：生成适合 Amazon A+ 页面的模块标题，简洁有力，突出内容主题。
                    内容主题方向：%s
                    原始文案：%s""",
                    get(extra, "contentTheme"), get(extra, "originalCopy"));

            case "A+ 模块文案" -> String.format("""
                    任务：生成 Amazon A+ 模块配套文案，与图片场景呼应，突出卖点。
                    模块类型：%s
                    主打卖点：%s
                    原始文案：%s""",
                    get(extra, "moduleType"), get(extra, "sellingPoints"), get(extra, "originalCopy"));

            case "Search Terms 关键词" -> String.format("""
                    任务：生成 Search Terms 关键词字符串，不重复，不超过249字符，词之间用空格分隔。
                    相关词 / 竞品词：%s
                    产品用途：%s
                    原始文案：%s""",
                    get(extra, "keywords"), get(extra, "useScene"), get(extra, "originalCopy"));

            case "Sponsored 广告标题" -> String.format("""
                    任务：生成 Sponsored 广告标题，吸引点击，控制在150字符以内。
                    推广重点：%s
                    核心关键词：%s
                    原始文案：%s""",
                    get(extra, "promoFocus"), get(extra, "keywords"), get(extra, "originalCopy"));

            case "品牌推广文案" -> String.format("""
                    任务：生成品牌推广文案，传递品牌价值观，建立用户信任感。
                    品牌理念：%s
                    差异化优势：%s
                    原始文案：%s""",
                    get(extra, "brandConcept"), get(extra, "advantage"), get(extra, "originalCopy"));

            case "买家评论回复" -> String.format("""
                    任务：针对以下买家评论生成专业、友好的回复，体现品牌温度。
                    评论类型：%s
                    买家评论原文：%s""",
                    get(extra, "reviewType"), get(extra, "reviewContent"));

            case "Q&A 问答" -> String.format("""
                    任务：基于以下买家问题生成专业准确的产品问答回复，简洁易懂。
                    买家问题：%s""",
                    get(extra, "question"));

            case "Deal/促销说明" -> String.format("""
                    任务：生成促销活动文案，突出优惠力度和紧迫感，引导下单。
                    折扣力度：%s
                    活动时限：%s
                    适用商品范围：%s
                    原始文案：%s""",
                    get(extra, "dealInfo"), get(extra, "dealDeadline"),
                    get(extra, "dealScope"), get(extra, "originalCopy"));

            // ── TikTok Shop ──────────────────────────────────────
            case "视频脚本" -> String.format("""
                    任务：生成适合 TikTok 短视频的脚本文案，节奏感强，前3秒抓住注意力。
                    视频主题：%s
                    目标受众：%s
                    原始文案：%s""",
                    get(extra, "videoTheme"), get(extra, "targetAudience"), get(extra, "originalCopy"));

            case "直播话术" -> String.format("""
                    任务：生成 TikTok 直播带货话术，口语化，有感染力，包含互动引导。
                    直播阶段：%s
                    原始文案：%s""",
                    get(extra, "liveStage"), get(extra, "originalCopy"));

            case "商品卡标题" -> String.format("""
                    任务：生成 TikTok Shop 商品卡标题，突出卖点，含核心关键词，控制在60字符以内。
                    核心关键词：%s
                    原始文案：%s""",
                    get(extra, "keywords"), get(extra, "originalCopy"));

            case "商品描述" -> String.format("""
                    任务：生成 TikTok Shop 商品描述，简洁生动，突出使用价值。
                    核心卖点：%s
                    目标受众：%s
                    原始文案：%s""",
                    get(extra, "sellingPoints"), get(extra, "targetAudience"), get(extra, "originalCopy"));

            case "Hashtag 文案" -> String.format("""
                    任务：生成适合 TikTok 的 Hashtag 组合，包含热门话题标签和垂直领域标签。
                    话题方向：%s
                    原始文案：%s""",
                    get(extra, "topic"), get(extra, "originalCopy"));

            case "评论区互动" -> String.format("""
                    任务：针对以下评论生成自然、有亲和力的互动回复，提升品牌好感度。
                    评论类型：%s
                    评论内容：%s""",
                    get(extra, "reviewType"), get(extra, "reviewContent"));

            case "达人合作 Brief" -> String.format("""
                    任务：生成达人合作 Brief 文案，说明合作要求、内容方向和核心卖点。
                    达人类型：%s
                    目标受众：%s
                    原始文案：%s""",
                    get(extra, "creatorType"), get(extra, "targetAudience"), get(extra, "originalCopy"));

            case "Shop 广告文案" -> String.format("""
                    任务：生成 TikTok Shop 广告投放文案，直接、有力，引导点击购买。
                    推广重点：%s
                    原始文案：%s""",
                    get(extra, "promoFocus"), get(extra, "originalCopy"));

            case "促销 Banner" -> String.format("""
                    任务：生成促销 Banner 文案，简短有冲击力，突出优惠信息。
                    促销信息：%s
                    原始文案：%s""",
                    get(extra, "dealInfo"), get(extra, "originalCopy"));

            // ── Facebook / Instagram ─────────────────────────────
            case "Feed 广告主文案" -> String.format("""
                    任务：生成 Facebook/Instagram Feed 广告文案，先戳痛点再给解决方案，结尾引导行动。
                    用户痛点：%s
                    目标受众：%s
                    原始文案：%s""",
                    get(extra, "painPoint"), get(extra, "targetAudience"), get(extra, "originalCopy"));

            case "Stories 广告文案" -> String.format("""
                    任务：生成 Stories 广告文案，极简风格，3秒内传达核心信息，结尾配行动号召。
                    行动号召：%s
                    原始文案：%s""",
                    get(extra, "cta"), get(extra, "originalCopy"));

            case "Reels 脚本" -> String.format("""
                    任务：生成 Reels 短视频脚本，视觉感强，节奏紧凑，适合竖屏展示。
                    视频主题：%s
                    目标受众：%s
                    原始文案：%s""",
                    get(extra, "videoTheme"), get(extra, "targetAudience"), get(extra, "originalCopy"));

            case "Post 有机内容" -> String.format("""
                    任务：生成适合自然流量的 Post 内容，有话题性，鼓励互动评论。
                    内容主题：%s
                    原始文案：%s""",
                    get(extra, "contentTheme"), get(extra, "originalCopy"));

            case "广告标题 Headline" -> String.format("""
                    任务：生成 Facebook 广告标题，简洁有力，突出核心价值主张，控制在40字符以内。
                    核心卖点：%s
                    原始文案：%s""",
                    get(extra, "sellingPoints"), get(extra, "originalCopy"));

            case "落地页文案" -> String.format("""
                    任务：生成落地页文案，结构清晰（标题→痛点→解决方案→CTA），转化导向。
                    行动号召：%s
                    目标受众：%s
                    原始文案：%s""",
                    get(extra, "cta"), get(extra, "targetAudience"), get(extra, "originalCopy"));

            case "DM 私信话术" -> String.format("""
                    任务：生成 DM 私信话术，自然不生硬，根据私信目的设计沟通策略。
                    私信目的：%s
                    原始文案：%s""",
                    get(extra, "purpose"), get(extra, "originalCopy"));

            case "评论回复" -> String.format("""
                    任务：针对以下评论生成专业友好的回复，维护品牌形象。
                    评论类型：%s
                    评论内容：%s""",
                    get(extra, "reviewType"), get(extra, "reviewContent"));

            case "品牌故事" -> String.format("""
                    任务：生成品牌故事文案，真实感人，传递品牌温度和差异化价值。
                    品牌理念：%s
                    差异化优势：%s
                    原始文案：%s""",
                    get(extra, "brandConcept"), get(extra, "advantage"), get(extra, "originalCopy"));

            case "促销活动文案" -> String.format("""
                    任务：生成促销活动文案，营造紧迫感，明确优惠利益点，引导立即行动。
                    促销信息：%s
                    活动时限：%s
                    原始文案：%s""",
                    get(extra, "dealInfo"), get(extra, "dealDeadline"), get(extra, "originalCopy"));

            // ── Shopify 独立站 ────────────────────────────────────
            case "产品页标题" -> String.format("""
                    任务：生成 Shopify 产品页标题，SEO 友好，包含核心关键词，吸引点击。
                    产品品类：%s
                    核心关键词：%s
                    原始文案：%s""",
                    get(extra, "category"), get(extra, "keywords"), get(extra, "originalCopy"));

            case "产品描述" -> String.format("""
                    任务：生成 Shopify 产品描述页文案，场景化叙述，突出使用价值，引导加购。
                    目标受众：%s
                    使用场景：%s
                    原始文案：%s""",
                    get(extra, "targetAudience"), get(extra, "useScene"), get(extra, "originalCopy"));

            case "SEO Meta 描述" -> String.format("""
                    任务：生成 SEO Meta 描述，包含核心关键词，控制在155字符以内，自然流畅。
                    核心关键词：%s
                    原始文案：%s""",
                    get(extra, "keywords"), get(extra, "originalCopy"));

            case "首页 Banner 文案" -> String.format("""
                    任务：生成首页 Banner 文案，大标题+副标题+CTA 按钮文字，简洁有力。
                    品牌主张：%s
                    行动号召：%s
                    原始文案：%s""",
                    get(extra, "brandConcept"), get(extra, "cta"), get(extra, "originalCopy"));

            case "Email 营销文案" -> String.format("""
                    任务：生成 Email 营销文案，包含邮件主题行和正文，个性化开头，明确 CTA。
                    邮件类型：%s
                    目标受众：%s
                    原始文案：%s""",
                    get(extra, "emailType"), get(extra, "targetAudience"), get(extra, "originalCopy"));

            case "弃购挽回邮件" -> String.format("""
                    任务：生成弃购挽回邮件，唤起购买欲望，提供专属优惠，制造紧迫感。
                    挽回优惠：%s
                    原始文案：%s""",
                    get(extra, "dealInfo"), get(extra, "originalCopy"));

            case "Blog 文章" -> String.format("""
                    任务：生成 Blog 文章大纲及引言段落，SEO 友好，有实用价值，自然植入产品。
                    文章主题：%s
                    SEO 关键词：%s
                    原始文案：%s""",
                    get(extra, "topic"), get(extra, "keywords"), get(extra, "originalCopy"));

            case "促销 Popup 文案" -> String.format("""
                    任务：生成促销弹窗文案，标题吸引注意，优惠信息清晰，CTA 按钮文字有力。
                    促销信息：%s
                    行动号召：%s
                    原始文案：%s""",
                    get(extra, "dealInfo"), get(extra, "cta"), get(extra, "originalCopy"));

            case "客服话术" -> String.format("""
                    任务：生成客服回复话术，专业友好，解答客户疑虑，维护购买信心。
                    客户问题：%s""",
                    get(extra, "question"));

            case "品牌 Slogan" -> String.format("""
                    任务：生成品牌 Slogan，朗朗上口，传递核心价值，易于记忆。
                    品牌理念：%s
                    差异化优势：%s
                    原始文案：%s""",
                    get(extra, "brandConcept"), get(extra, "advantage"), get(extra, "originalCopy"));

            // ── 兜底 ─────────────────────────────────────────────
            default -> String.format("""
                    任务：针对「%s」场景，基于以下原始文案生成变体。
                    原始文案：%s""",
                    scene, get(extra, "originalCopy"));
        };
    }
}