package com.adcopy.adcopy_backend.service;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class PromptBuilder {

    public String build(String platName, String scene,
                        int count, String tone, String language, Map<String, String> extraFields) {
        return getPlatformPersona(platName)
                + "\n\n"
                + buildScenePrompt(platName, scene, extraFields)
                + "\n\n"
                + getAntiAiRules(scene)
                + "\n\n"
                + buildOutputRequirements(count, tone, scene, language);
    }

    // ─────────────────────────────────────────────
    // ① 平台人格：让模型代入真实从业者的思维方式
    // ─────────────────────────────────────────────
    private String getPlatformPersona(String platName) {
        return switch (platName) {
            case "Amazon" -> """
                    你是一位在亚马逊从事跨境电商 8 年的资深卖家，
                    深度研究过数百条 BSR 榜单 listing，熟悉 A9 算法对关键词密度的偏好，
                    清楚买家在搜索结果页停留不超过 3 秒的决策习惯。
                    你写的标题能精准命中搜索意图，bullet points 总能在第一条就击中核心痛点。""";

            case "TikTok Shop" -> """
                    你是一个每天刷 TikTok 超过 4 小时、做了 3 年 TikTok Shop 的内容操盘手，
                    深度研究过上千条爆款视频的开头 3 秒结构，
                    清楚什么样的文案能让用户停下来、什么样的文案会被直接划走。
                    你的文案从不像广告，更像是朋友之间的真实分享。""";

            case "Facebook / Instagram" -> """
                    你是一位专注 DTC 品牌的 Facebook 广告优化师，
                    经手过超过 500 万美金的广告预算，深谙 Meta 算法对互动率的权重机制。
                    你清楚 Feed 广告的黄金公式：先共鸣用户处境，再给出解决方案，最后制造行动动力。
                    你写的文案能让目标用户感觉"这说的就是我"。""";

            case "Shopify 独立站" -> """
                    你是一位服务过 50+ 个 Shopify 独立站的品牌文案顾问，
                    深度研究过高转化率产品页的叙事结构，
                    熟悉 SEO 语义搜索的关键词布局逻辑。
                    你写的产品描述既能被 Google 收录，又能让访客读完之后立刻想加购。""";

            default -> "你是一位资深跨境电商文案专家。";
        };
    }

    // ─────────────────────────────────────────────
    // ② 场景专家知识 + 风格样本
    // ─────────────────────────────────────────────
    private String get(Map<String, String> extra, String key) {
        String val = extra == null ? null : extra.get(key);
        return (val != null && !val.isBlank()) ? val : null;
    }

    private String buildScenePrompt(String platName, String scene, Map<String, String> extra) {
        return switch (scene) {

            // ── Amazon ──────────────────────────────────────────────────────

            case "产品标题" -> String.format("""
                    任务：生成 Amazon 产品标题。
                    
                    行业知识：
                    - 亚马逊标题公式：[品牌] + [核心词] + [属性/规格] + [使用场景/人群] + [卖点]
                    - 核心关键词必须出现在前 80 字符（移动端截断位置）
                    - 避免全大写，避免特殊符号（! $ ? 等）
                    - 不要堆砌关键词，要让真实用户读起来通顺
                    
                    参考风格：
                    「Anker USB C Cable [2-Pack, 6ft], 240W Fast Charging, Nylon Braided for MacBook Pro」
                    「COSRX Snail Mucin 96% Power Repairing Essence 100ml, Korean Skincare for Dry Skin」
                    
                    产品品类：%s
                    核心关键词：%s
                    原始文案：%s""",
                    get(extra, "category"), get(extra, "keywords"), get(extra, "originalCopy"));

            case "5点描述 Bullet Points" -> String.format("""
                    任务：生成 Amazon 5条 Bullet Points。
                    
                    行业知识：
                    - 每条以全大写词组开头，紧跟破折号，格式：CORE BENEFIT – 展开说明
                    - 第一条必须是最强卖点，买家扫一眼就能看到
                    - 每条 150-200 字符，不超过两行
                    - 顺序建议：功能卖点 → 材质/工艺 → 适用人群/场景 → 兼容性/规格 → 售后/保障
                    - 用数字和具体参数代替模糊描述（"50 hours" 优于 "long battery life"）
                    
                    参考风格：
                    「ADVANCED NOISE CANCELLATION – 40dB hybrid ANC blocks 99%% of ambient noise...」
                    「PREMIUM MATERIALS – Aircraft-grade aluminum frame withstands 1.5m drops...」
                    
                    核心卖点：%s
                    目标受众：%s
                    原始文案：%s""",
                    get(extra, "sellingPoints"), get(extra, "targetAudience"), get(extra, "originalCopy"));

            case "产品描述（段落式）" -> String.format("""
                    任务：生成 Amazon 产品描述正文段落。
                    
                    行业知识：
                    - 结构：场景代入（1句）→ 痛点共鸣（1-2句）→ 产品解决方案（2-3句）→ 使用细节（1-2句）→ 信任背书（1句）
                    - 用第二人称"You"，让买家代入
                    - 避免公司视角（"我们的产品"），多用用户视角（"你会发现"）
                    - 200-300 词，段落间空行分隔
                    
                    目标受众：%s
                    使用场景：%s
                    品牌调性：%s
                    原始文案：%s""",
                    get(extra, "targetAudience"), get(extra, "useScene"),
                    get(extra, "brandTone"), get(extra, "originalCopy"));

            case "A+ 内容标题" -> String.format("""
                    任务：生成 Amazon A+ 模块标题。
                    
                    行业知识：
                    - A+ 标题要像杂志栏目名，简洁有力，4-8个词为佳
                    - 避免直接描述产品，要描述用户得到的价值或体验
                    - 可以用反问、数字、对比等手法制造好奇
                    
                    参考风格：
                    「Why 2 Million Runners Choose Us」「Sleep Better, Starting Tonight」
                    「The 30-Second Morning Routine」
                    
                    内容主题方向：%s
                    原始文案：%s""",
                    get(extra, "contentTheme"), get(extra, "originalCopy"));

            case "A+ 模块文案" -> String.format("""
                    任务：生成 Amazon A+ 模块配套文案。
                    
                    行业知识：
                    - A+ 文案配合图片，文字只做补充，不要重复图片已呈现的信息
                    - 每个模块聚焦一个主题，不要什么都写
                    - 语言要有画面感，让读者脑海里能浮现使用场景
                    - 避免罗列参数，多写使用感受和情绪价值
                    
                    模块类型：%s
                    主打卖点：%s
                    原始文案：%s""",
                    get(extra, "moduleType"), get(extra, "sellingPoints"), get(extra, "originalCopy"));

            case "Search Terms 关键词" -> String.format("""
                    任务：生成 Amazon Search Terms 后台关键词。
                    
                    行业知识：
                    - 总字符不超过 249（含空格），超出部分亚马逊直接忽略
                    - 不要重复标题和 bullet 中已有的词（亚马逊自动抓取，重复浪费空间）
                    - 优先补充：同义词、俚语叫法、错误拼写、竞品品类词、使用场景词
                    - 词与词之间空格分隔，不需要逗号，不需要引号
                    - 不包含 "Amazon"、竞品品牌名、违禁词
                    
                    相关词 / 竞品词：%s
                    产品用途：%s
                    原始文案：%s""",
                    get(extra, "keywords"), get(extra, "useScene"), get(extra, "originalCopy"));

            case "Sponsored 广告标题" -> String.format("""
                    任务：生成 Amazon Sponsored 广告标题。
                    
                    行业知识：
                    - 字符限制 130（Sponsored Brands）或 150（Sponsored Display）
                    - 黄金公式：[痛点/场景] + [产品核心卖点] + [促销钩子]
                    - 开头词决定点击率，尽量用动词或数字开头
                    - 避免品牌名开头（用户不认识你的品牌，没有吸引力）
                    
                    参考风格：
                    「Tired of Tangled Cables? Try Our 240W USB-C – Ships Today」
                    「#1 Voted Skincare in 2024 – Buy 2 Get 1 Free This Week」
                    
                    推广重点：%s
                    核心关键词：%s
                    原始文案：%s""",
                    get(extra, "promoFocus"), get(extra, "keywords"), get(extra, "originalCopy"));

            case "品牌推广文案" -> String.format("""
                    任务：生成 Amazon 品牌推广文案。
                    
                    行业知识：
                    - 品牌文案不卖产品，卖的是信念和身份认同
                    - 用"我们相信……"或"为……而生"等句式建立价值共鸣
                    - 避免自吹自擂，多用第三方视角（用户故事、数据背书）
                    - 控制在 3-4 句话，每句都要有独立信息量
                    
                    品牌理念：%s
                    差异化优势：%s
                    原始文案：%s""",
                    get(extra, "brandConcept"), get(extra, "advantage"), get(extra, "originalCopy"));

            case "买家评论回复" -> String.format("""
                    任务：生成 Amazon 买家评论回复。
                    
                    行业知识：
                    - 好评回复：表达感谢 → 呼应买家提到的具体点 → 轻描一句未来期待
                    - 差评回复：先共情不辩解 → 给出解决方案 → 留联系方式私下跟进（绝不在公开回复里争论）
                    - 中评回复：肯定正面反馈 → 针对负面点给出说明或补偿 → 邀请再次联系
                    - 全程保持品牌语气，不卑不亢，体现专业度
                    - 回复控制在 3-5 句话，不要写成小作文
                    
                    评论类型：%s
                    买家评论原文：%s""",
                    get(extra, "reviewType"), get(extra, "reviewContent"));

            case "Q&A 问答" -> String.format("""
                    任务：生成 Amazon Q&A 问答回复。
                    
                    行业知识：
                    - 直接回答问题，不要废话开头（"感谢您的提问"这种开头浪费买家时间）
                    - 回答要精准，给出具体参数/数字，不要模糊表述
                    - 如果问题涉及兼容性，列出明确的兼容/不兼容清单
                    - 结尾可以顺带提一个相关卖点，但不要变成广告
                    
                    买家问题：%s""",
                    get(extra, "question"));

            case "Deal/促销说明" -> String.format("""
                    任务：生成 Amazon Deal 促销说明文案。
                    
                    行业知识：
                    - 先说"省了多少"，比说"折扣力度"更有冲击力（"Save $15" > "15%% off"）
                    - 时限要具体（"Ends Sunday midnight" > "Limited time"）
                    - 明确适用范围，避免买家误解后差评
                    - 结尾加一句 social proof（"Join 10,000+ happy customers"）
                    
                    折扣力度：%s
                    活动时限：%s
                    适用商品范围：%s
                    原始文案：%s""",
                    get(extra, "dealInfo"), get(extra, "dealDeadline"),
                    get(extra, "dealScope"), get(extra, "originalCopy"));

            // ── TikTok Shop ─────────────────────────────────────────────────

            case "视频脚本" -> String.format("""
                    任务：生成 TikTok 短视频脚本。
                    
                    行业知识：
                    - 黄金结构：前3秒钩子（反常识/夸张/疑问）→ 中间体验细节 → 自然收尾
                    - 钩子类型参考：「等等这个东西有点离谱」「我不敢相信我用了这么久才发现」「这个冷知识99%%的人不知道」
                    - 细节描写越具体越好，"软烂多汁" 比 "很好吃" 好，"第一口就沦陷" 比 "非常好吃" 好
                    - 字数 80-120 字，读出来约 15-20 秒
                    - 结尾不要强行 CTA，自然收尾反而效果更好
                    
                    参考语感（只借鉴口吻，禁止抄袭内容）：
                    「我发誓这是我今年吃过最离谱的东西，好吃到有点生气」
                    「等等！你们有没有发现这个根本停不下来！！」
                    
                    视频主题：%s
                    目标受众：%s
                    原始文案：%s""",
                    get(extra, "videoTheme"), get(extra, "targetAudience"), get(extra, "originalCopy"));

            case "直播话术" -> String.format("""
                    任务：生成 TikTok 直播带货话术。
                    
                    行业知识：
                    - 直播话术的核心是制造"当下感"，让观众觉得现在不买就亏了
                    - 开场预热：先互动破冰，再引出产品（"刚进来的宝宝扣1，老粉扣2"）
                    - 介绍产品：痛点 → 解决方案 → 使用场景 → 价格锚点 → 限时优惠
                    - 逼单话术：库存数字（"还剩最后38件"）+ 时间压力（"这个价格只有今晚"）
                    - 口语化，要有停顿感，可以用省略号表示停顿节拍
                    
                    参考语感：
                    「停一下！这个我已经回购了三次了，今天给你们争取到了全年最低价……」
                    「好姐妹们听我说，这个东西真的是买到就是赚到，不买就是亏……」
                    
                    直播阶段：%s
                    原始文案：%s""",
                    get(extra, "liveStage"), get(extra, "originalCopy"));

            case "商品卡标题" -> String.format("""
                    任务：生成 TikTok Shop 商品卡标题。
                    
                    行业知识：
                    - TikTok Shop 标题和亚马逊不同，更偏向"种草感"而不是关键词堆砌
                    - 公式：[人群/场景定位] + [核心卖点] + [差异化属性]
                    - 移动端显示约 40 字符，核心信息前置
                    - 可以带一点情绪词（「绝绝子」已过时，用「好用到想囤货」这类真实口语）
                    
                    参考风格：
                    「夏天必备！清爽不黏腻防晒霜 敏感肌也能用」
                    「健身党狂推 高蛋白低卡代餐棒 运动后补能量」
                    
                    核心关键词：%s
                    原始文案：%s""",
                    get(extra, "keywords"), get(extra, "originalCopy"));

            case "商品描述" -> String.format("""
                    任务：生成 TikTok Shop 商品描述。
                    
                    行业知识：
                    - TikTok 用户几乎不看长描述，用换行+短句提高可读性
                    - 第一行必须是最强卖点，之后才是细节
                    - 可以用 emoji 辅助视觉分隔，但不超过每段一个
                    - 结构：一句话总结 → 核心卖点3条（每条一行）→ 适用人群/场景
                    
                    核心卖点：%s
                    目标受众：%s
                    原始文案：%s""",
                    get(extra, "sellingPoints"), get(extra, "targetAudience"), get(extra, "originalCopy"));

            case "Hashtag 文案" -> String.format("""
                    任务：生成 TikTok Hashtag 组合。
                    
                    行业知识：
                    - 黄金组合：1-2个超大流量标签（千亿级）+ 3-4个垂直领域标签（百亿级）+ 2-3个精准长尾标签
                    - 超大流量标签：#foryou #fyp #viral（引流但竞争大）
                    - 垂直领域标签：和产品类目强相关
                    - 精准标签：描述具体使用场景或人群，转化率更高
                    - 总数控制在 8-12 个，太多会稀释权重
                    
                    话题方向：%s
                    原始文案：%s""",
                    get(extra, "topic"), get(extra, "originalCopy"));

            case "评论区互动" -> String.format("""
                    任务：生成 TikTok 评论区互动回复。
                    
                    行业知识：
                    - TikTok 评论区回复风格要活泼、有梗、像真人说话
                    - 好评：要显得真诚惊喜，不能像机器人表情包（"感谢支持！"这种最差）
                    - 差评：先共情，再给解决方案，不要删评，公开处理比私下处理更能建立信任
                    - 可以适当用表情包语气词增加亲切感
                    - 回复要短，1-2句话最佳，太长没人看
                    
                    评论类型：%s
                    评论内容：%s""",
                    get(extra, "reviewType"), get(extra, "reviewContent"));

            case "达人合作 Brief" -> String.format("""
                    任务：生成 TikTok 达人合作 Brief。
                    
                    行业知识：
                    - 好的 Brief 给方向不给脚本，让达人用自己的风格创作（硬塞脚本的视频播放量很差）
                    - 必须包含：产品核心卖点（不超过3条）、必须提及的信息（成分/功效/价格）、禁止提及的内容
                    - 可以提供参考视频链接，比文字描述更直观
                    - 给达人留足创作空间，只锁定底线，不锁定风格
                    
                    达人类型：%s
                    目标受众：%s
                    原始文案：%s""",
                    get(extra, "creatorType"), get(extra, "targetAudience"), get(extra, "originalCopy"));

            case "Shop 广告文案" -> String.format("""
                    任务：生成 TikTok Shop 广告投放文案。
                    
                    行业知识：
                    - TikTok 广告文案要在前 5 个字就让目标用户感觉"这说的是我"
                    - 用痛点/场景开头比用产品名开头点击率高 30%%+
                    - 文案要有强烈的画面感，让用户脑补使用场景
                    - 结尾 CTA 要具体（"点击立即抢" > "了解更多"）
                    
                    推广重点：%s
                    原始文案：%s""",
                    get(extra, "promoFocus"), get(extra, "originalCopy"));

            case "促销 Banner" -> String.format("""
                    任务：生成 TikTok Shop 促销 Banner 文案。
                    
                    行业知识：
                    - Banner 用户停留时间不超过 1 秒，只能传达一个核心信息
                    - 公式：[数字化优惠] + [限时紧迫感] + [行动指令]
                    - 数字要醒目（"5折" 比 "五折" 视觉冲击更强）
                    - 避免堆砌信息，宁可少说一个卖点也要保持视觉干净
                    
                    促销信息：%s
                    原始文案：%s""",
                    get(extra, "dealInfo"), get(extra, "originalCopy"));

            // ── Facebook / Instagram ─────────────────────────────────────────

            case "Feed 广告主文案" -> String.format("""
                    任务：生成 Facebook/Instagram Feed 广告主文案。
                    
                    行业知识：
                    - Meta 广告文案黄金公式：共鸣（你是否曾经……）→ 承诺（我们帮你……）→ 证明（已经有……人）→ 行动（立即……）
                    - 第一行是最重要的，Feed 默认折叠，用户只看第一行决定是否展开
                    - 用问句开头比陈述句开头互动率高约 40%%
                    - 长文案（150词+）适合高决策成本产品，短文案（50词内）适合冲动消费品
                    
                    参考风格：
                    「每天早上起床脖子酸痛？可能不是你睡姿的问题……」
                    「3年前我也觉得护肤品都是智商税，直到我遇到了它」
                    
                    用户痛点：%s
                    目标受众：%s
                    原始文案：%s""",
                    get(extra, "painPoint"), get(extra, "targetAudience"), get(extra, "originalCopy"));

            case "Stories 广告文案" -> String.format("""
                    任务：生成 Instagram/Facebook Stories 广告文案。
                    
                    行业知识：
                    - Stories 广告用户有跳过权，前 2 秒决定生死
                    - 文字要极简，画面才是主角，文案只做辅助
                    - 结构：一句话 hook + 一句话价值 + CTA 按钮文字
                    - CTA 要具体有力（"Shop Now" < "Grab 50%% Off Today"）
                    - 避免在 Stories 用长句，每行不超过 5 个词
                    
                    行动号召：%s
                    原始文案：%s""",
                    get(extra, "cta"), get(extra, "originalCopy"));

            case "Reels 脚本" -> String.format("""
                    任务：生成 Instagram Reels 短视频脚本。
                    
                    行业知识：
                    - Reels 和 TikTok 相似但受众偏大，25-35岁更多，可以稍微正式一点点
                    - 前 3 秒仍然是决定性的，但可以用美学画面吸引而不只靠文字钩子
                    - Instagram 用户对品质感要求更高，文案要精炼不粗糙
                    - 字数控制在 80-100 字，适合 15-30 秒视频节奏
                    
                    视频主题：%s
                    目标受众：%s
                    原始文案：%s""",
                    get(extra, "videoTheme"), get(extra, "targetAudience"), get(extra, "originalCopy"));

            case "Post 有机内容" -> String.format("""
                    任务：生成 Facebook/Instagram 自然流量 Post 内容。
                    
                    行业知识：
                    - 有机内容的目标是互动（评论/分享），不是直接转化
                    - 能引发讨论的内容公式：争议性观点 / 投票 / 测验 / 晒经历 / 提问
                    - 结尾必须有一个明确的互动引导（"你们觉得呢？" / "评论区见"）
                    - 避免过度推销，用户一眼看出是广告就划走
                    - 配文长度：Instagram 150词内最佳，Facebook 可以到 400 词
                    
                    内容主题：%s
                    原始文案：%s""",
                    get(extra, "contentTheme"), get(extra, "originalCopy"));

            case "广告标题 Headline" -> String.format("""
                    任务：生成 Facebook 广告标题（Headline）。
                    
                    行业知识：
                    - Headline 出现在广告图片下方，是第二个被看到的元素
                    - 40字符以内，传达一个完整的价值主张
                    - 最有效的 Headline 类型：利益型（省钱/省时/变美）、好奇型（你不知道的……）、紧迫型（最后X件）
                    - 和主文案形成互补，不要重复主文案已有的信息
                    
                    核心卖点：%s
                    原始文案：%s""",
                    get(extra, "sellingPoints"), get(extra, "originalCopy"));

            case "落地页文案" -> String.format("""
                    任务：生成 Facebook 广告落地页文案。
                    
                    行业知识：
                    - 落地页第一屏（Above the fold）决定跳出率，必须在 5 秒内传达核心价值
                    - 结构：大标题（核心承诺）→ 副标题（如何实现）→ 社会证明 → 详细卖点 → CTA
                    - 大标题聚焦结果，不聚焦产品（"睡醒不再颈椎疼" 比 "人体工学枕头" 转化率高）
                    - CTA 按钮文字要体现价值（"领取专属优惠" > "立即购买"）
                    
                    行动号召：%s
                    目标受众：%s
                    原始文案：%s""",
                    get(extra, "cta"), get(extra, "targetAudience"), get(extra, "originalCopy"));

            case "DM 私信话术" -> String.format("""
                    任务：生成 Facebook/Instagram DM 私信话术。
                    
                    行业知识：
                    - 私信第一条决定对方是否回复，冷启动最难
                    - 开头要让对方感觉这是写给他一个人的，不是群发模板
                    - 找到切入点（对方的帖子/评论/共同兴趣），先建立连接再谈目的
                    - 不要一开始就推产品，先给价值（信息/帮助/赞美），再自然引出
                    - 控制在 3-4 句话，太长没人看完
                    
                    私信目的：%s
                    原始文案：%s""",
                    get(extra, "purpose"), get(extra, "originalCopy"));

            case "评论回复" -> String.format("""
                    任务：生成 Facebook/Instagram 评论回复。
                    
                    行业知识：
                    - 公开评论回复有品牌展示效应，潜在客户都在看你怎么回应
                    - 好评：简短真诚，带出一个品牌关键词，偶尔@用户增加亲近感
                    - 差评：黄金原则——绝不删评，公开处理比私下处理更建立信任
                    - 差评回复格式：理解情绪（不道歉产品）→ 给解决方案 → 转至私信跟进
                    
                    评论类型：%s
                    评论内容：%s""",
                    get(extra, "reviewType"), get(extra, "reviewContent"));

            case "品牌故事" -> String.format("""
                    任务：生成品牌故事文案。
                    
                    行业知识：
                    - 品牌故事要有"创始人时刻"：因为亲身经历的痛点，所以创造了这个产品
                    - 真实的细节比宏大的叙事更打动人（具体的时间地点人物）
                    - 故事结构：世界观建立 → 创始人/品牌遇到的问题 → 解决过程 → 现在的使命
                    - 结尾要升华到用户层面（"我们创造这个，是为了让你……"）
                    - 避免自我吹捧，多用事实和数字说话
                    
                    品牌理念：%s
                    差异化优势：%s
                    原始文案：%s""",
                    get(extra, "brandConcept"), get(extra, "advantage"), get(extra, "originalCopy"));

            case "促销活动文案" -> String.format("""
                    任务：生成 Facebook/Instagram 促销活动文案。
                    
                    行业知识：
                    - 促销文案的两个核心驱动力：贪便宜（省了多少）+ 怕错过（限时限量）
                    - 先说优惠利益点，再说时间限制（顺序很重要）
                    - 用具体数字（"省 $20" 比 "85折" 更直观）
                    - 结尾倒计时或库存数字制造紧迫感
                    - 配一个清晰的 CTA，让用户知道下一步做什么
                    
                    促销信息：%s
                    活动时限：%s
                    原始文案：%s""",
                    get(extra, "dealInfo"), get(extra, "dealDeadline"), get(extra, "originalCopy"));

            // ── Shopify 独立站 ───────────────────────────────────────────────

            case "产品页标题" -> String.format("""
                    任务：生成 Shopify 产品页 SEO 标题。
                    
                    行业知识：
                    - 标题标签（Title Tag）理想长度 50-60 字符，超出部分 Google 截断
                    - 公式：[核心关键词] + [差异化属性] + [品牌名]（品牌名放最后）
                    - 核心关键词要和用户搜索词完全匹配，不要同义词替换
                    - 每个产品页标题必须唯一，不能和其他页面重复
                    
                    参考风格：
                    「Handmade Leather Wallet for Men – Slim Bifold, RFID Blocking | BrandName」
                    「Organic Cotton Baby Blanket – Machine Washable, 4 Colors | StoreName」
                    
                    产品品类：%s
                    核心关键词：%s
                    原始文案：%s""",
                    get(extra, "category"), get(extra, "keywords"), get(extra, "originalCopy"));

            case "产品描述" -> String.format("""
                    任务：生成 Shopify 产品描述页文案。
                    
                    行业知识：
                    - 独立站产品描述比亚马逊有更大的品牌发挥空间，要塑造生活方式而不只是介绍功能
                    - 结构：场景化开头（让用户代入）→ 核心卖点（2-3个，有数据支撑）→ 规格参数 → 信任背书
                    - 用"你"而不是"用户"，用"当你……"开头制造代入感
                    - SEO：自然融入2-3个核心关键词，但不要堆砌
                    - 配合短段落和小标题，移动端扫读体验更好
                    
                    目标受众：%s
                    使用场景：%s
                    原始文案：%s""",
                    get(extra, "targetAudience"), get(extra, "useScene"), get(extra, "originalCopy"));

            case "SEO Meta 描述" -> String.format("""
                    任务：生成 Shopify SEO Meta 描述。
                    
                    行业知识：
                    - Meta 描述不直接影响排名，但影响点击率（CTR 影响排名）
                    - 理想长度 150-155 字符，超出会被截断显示省略号
                    - 必须包含核心关键词（用户在 SERP 看到关键词会加粗，提高点击欲望）
                    - 公式：[核心关键词] + [最强卖点] + [CTA]
                    - 每个页面的 Meta 描述要唯一，不能重复
                    
                    核心关键词：%s
                    原始文案：%s""",
                    get(extra, "keywords"), get(extra, "originalCopy"));

            case "首页 Banner 文案" -> String.format("""
                    任务：生成 Shopify 首页 Hero Banner 文案。
                    
                    行业知识：
                    - Hero Banner 是品牌第一印象，用户决定留下还是离开就在这 3 秒
                    - 结构：大标题（核心承诺，6-10个词）+ 副标题（补充说明，15-20词）+ CTA按钮（3-5词）
                    - 大标题聚焦用户得到什么，而不是产品是什么
                    - CTA 按钮要有具体动作感（"探索系列" 比 "查看更多" 好）
                    - 三者信息互补，不要重复
                    
                    参考风格：
                    大标题：「Sleep Better, Wake Up Stronger」
                    副标题：「Ergonomic pillows designed with orthopedic specialists for deeper, pain-free sleep」
                    CTA：「Find Your Perfect Pillow」
                    
                    品牌主张：%s
                    行动号召：%s
                    原始文案：%s""",
                    get(extra, "brandConcept"), get(extra, "cta"), get(extra, "originalCopy"));

            case "Email 营销文案" -> String.format("""
                    任务：生成 Email 营销文案（含主题行和正文）。
                    
                    行业知识：
                    - 主题行决定打开率，关键词：个人化（[名字]）、好奇心（你不知道的……）、利益（专属优惠）、紧迫感（最后24小时）
                    - 主题行 40-50 字符最佳，手机端不会被截断
                    - 正文开头第一句即 Preview Text，很多用户在收件箱直接看到，要和主题行形成互补
                    - 一封邮件只做一件事，一个 CTA，不要什么都塞进去
                    - 邮件正文段落短，用小标题分割，移动端扫读更舒适
                    
                    邮件类型：%s
                    目标受众：%s
                    原始文案：%s""",
                    get(extra, "emailType"), get(extra, "targetAudience"), get(extra, "originalCopy"));

            case "弃购挽回邮件" -> String.format("""
                    任务：生成弃购挽回邮件文案。
                    
                    行业知识：
                    - 弃购挽回邮件发送时机：1小时内（转化率最高）、24小时、72小时三封序列
                    - 第一封：提醒 + 解答顾虑（不要立刻给折扣，先了解是否有疑问）
                    - 第二封：社会证明 + 轻度优惠（5-10% off）
                    - 第三封：最终报价 + 明确截止时间
                    - 主题行用产品名/买家名字个人化，打开率提升 20%%+
                    
                    挽回优惠：%s
                    原始文案：%s""",
                    get(extra, "dealInfo"), get(extra, "originalCopy"));

            case "Blog 文章" -> String.format("""
                    任务：生成 Blog 文章大纲及引言段落。
                    
                    行业知识：
                    - Blog 的核心目的是 SEO 引流，文章要围绕用户真实搜索问题展开
                    - 标题公式：数字（10个……）/ 疑问（为什么……）/ 方法（如何……）
                    - 引言段落：先提出问题/痛点（用户代入）→ 承诺本文解决什么 → 一句话预告内容
                    - 内容植入产品要自然，放在解决方案环节，不要一开始就推
                    - 理想长度：1500-2500词（SEO效果最佳区间）
                    
                    文章主题：%s
                    SEO 关键词：%s
                    原始文案：%s""",
                    get(extra, "topic"), get(extra, "keywords"), get(extra, "originalCopy"));

            case "促销 Popup 文案" -> String.format("""
                    任务：生成 Shopify 促销弹窗文案。
                    
                    行业知识：
                    - Popup 用户默认排斥，文案要在 0.5 秒内传达价值，否则直接关掉
                    - 结构：标题（一眼看出优惠是什么）+ 副标题（可选，补充限制条件）+ CTA按钮 + 拒绝按钮
                    - 拒绝按钮文字也要设计（"No thanks, I don't like discounts" 比 "Close" 增加愧疚感）
                    - 订阅类 Popup 要说明订阅能得到什么，不只是"订阅我们的邮件"
                    
                    促销信息：%s
                    行动号召：%s
                    原始文案：%s""",
                    get(extra, "dealInfo"), get(extra, "cta"), get(extra, "originalCopy"));

            case "客服话术" -> String.format("""
                    任务：生成 Shopify 独立站客服回复话术。
                    
                    行业知识：
                    - 独立站客服代表品牌形象，回复质量直接影响复购率和口碑
                    - 先理解客户问题再回答，不要用模板化开头（"您好！感谢您联系我们"这种已经让用户关闭页面了）
                    - 复杂问题分步骤回答，用数字列表便于执行
                    - 结尾留下跟进空间（"如果还有其他问题，随时回复这封邮件"）
                    - 语气要像真人，可以有一点点个人温度，不要像机器人
                    
                    客户问题：%s""",
                    get(extra, "question"));

            case "品牌 Slogan" -> String.format("""
                    任务：生成品牌 Slogan。
                    
                    行业知识：
                    - 伟大的 Slogan 有三个特质：简短（3-7词）、好记（音韵/对仗/押韵）、有情绪（而不只是描述）
                    - 不要描述产品是什么，要描述品牌让用户成为什么人
                    - 参考：「Just Do It（你做了什么）」「Think Different（你是什么样的人）」「Belong Anywhere（你在哪里）」
                    - 避免陈词滥调：品质卓越、匠心打造、为您服务……这些词已经失去意义
                    
                    品牌理念：%s
                    差异化优势：%s
                    原始文案：%s""",
                    get(extra, "brandConcept"), get(extra, "advantage"), get(extra, "originalCopy"));

            default -> String.format("""
                    任务：针对「%s」场景生成文案变体。
                    原始文案：%s""",
                    scene, get(extra, "originalCopy"));
        };
    }

    // ─────────────────────────────────────────────
    // ③ 反 AI 禁令：按场景差异化配置
    // ─────────────────────────────────────────────
    private String getAntiAiRules(String scene) {
        String common = """
                【写作禁令】
                - 禁止使用：不容错过、完美之选、卓越品质、匠心打造、引领潮流
                - 禁止句式：让我们一起、为您呈现、我们致力于
                - 禁止开头：作为……、在当今……、随着……的发展
                - 不要生硬地堆砌关键词，关键词要融入自然语境""";

        String sceneSpecific = switch (scene) {
            case "视频脚本", "直播话术", "评论区互动", "评论回复", "买家评论回复" -> """
                - 禁止书面语，只用口语
                - 禁止长句，超过20字的句子拆成两句
                - 禁止"感叹号"连续出现超过2次
                - 每句话读出来要自然，不能有阅读感""";

            case "产品标题", "商品卡标题", "Sponsored 广告标题", "广告标题 Headline" -> """
                - 禁止用空洞形容词（优质、卓越、高端）代替具体描述
                - 禁止英文全大写（除非是规格参数）
                - 禁止在标题里放联系方式或网址""";

            case "品牌故事", "品牌 Slogan", "品牌推广文案" -> """
                - 禁止自我吹捧式表述（我们是最好的、行业领先）
                - 禁止空洞的使命宣言（让世界更美好、改变未来）
                - 故事要有具体的人、时间、地点，不能只有概念""";

            case "SEO Meta 描述", "Search Terms 关键词", "Blog 文章" -> """
                - 关键词必须自然融入，不能生硬堆砌
                - 禁止重复关键词（同一个词在同一段出现超过2次）
                - 语言要流畅，要像人写的而不是机器生成的""";

            default -> "";
        };

        return sceneSpecific.isBlank() ? common : common + "\n" + sceneSpecific;
    }

    // ─────────────────────────────────────────────
    // ④ 输出要求
    // ─────────────────────────────────────────────
    private String buildOutputRequirements(int count, String tone, String scene, String language) {

        boolean isStructuredScene = switch (scene) {
            case "5点描述 Bullet Points" -> true;
            case "A+ 模块文案"           -> true;
            case "首页 Banner 文案"      -> true;
            case "促销 Popup 文案"       -> true;
            default -> false;
        };

        String variantDescription = isStructuredScene
                ? "每条变体 = 一套完整内容（包含所有要求的子项），变体之间风格要有明显差异"
                : "每条都要有明显差异，不能只是换几个词";

        return String.format("""
            【输出要求】
            - 生成 %d 条独立变体，%s
            - 每条变体之间用单独一行「---」分隔
            - 不加编号，不加多余符号
            - 语气：%s
            - 输出语言：%s（无论输入内容是什么语言，输出必须全程使用%s）
            - 输出前在脑海中默读一遍，确认像真人写的而不是 AI 生成的""",count, variantDescription, tone, language, language);
    }
}