# AdCopy AI — 后端 README

> **定位**：跨境广告文案裂变工具的后端服务，基于 Spring Boot 构建，负责接收前端请求、构建 Prompt、调用 LLM API 生成文案，并进行敏感词检测后返回结构化结果。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 框架 | Spring Boot 3.x |
| HTTP 客户端 | OkHttp（调用 LLM API） |
| JSON 处理 | Jackson (`ObjectMapper`) |
| 配置管理 | `@Value` + `application.properties` |
| 构建工具 | Maven |

---

## 目录结构

```
src/main/java/com/adcopy/adcopy_backend/
├── AdcopyBackendApplication.java   # Spring Boot 启动类
├── config/
│   └── CorsConfig.java             # 跨域配置（允许前端 localhost:5173）
├── controller/
│   └── CopyController.java         # REST 接口层（/api/copy/generate）
├── model/
│   ├── GenerateRequest.java        # 请求体 DTO
│   └── GenerateResponse.java       # 响应体 DTO（含嵌套类 ResponseData / CopyItem / SensitiveWord）
└── service/
    ├── AiService.java              # 文案生成：构建请求 → 调用 LLM → 解析分割结果
    ├── PromptBuilder.java          # Prompt 工厂：根据平台+场景拼装专属 Prompt
    └── SensitiveWordService.java   # 敏感词检测：批量检测 → 解析 JSON 结果
```

---

## 核心业务流程

```
POST /api/copy/generate
        │
        ▼
CopyController.generate(GenerateRequest)
        │
        ├─► AiService.generateCopy()
        │       │
        │       ├─► PromptBuilder.build()          # 拼装 Prompt
        │       ├─► OkHttp POST → LLM /v1/chat/completions
        │       └─► 解析响应，按 "---" 分割为 List<String>
        │
        ├─► SensitiveWordService.detect(List<String>)
        │       │
        │       ├─► 构建批量检测 Prompt（含编号列表）
        │       ├─► OkHttp POST → LLM /v1/chat/completions
        │       └─► 解析 JSON 数组 → List<List<String>>（每条文案的违禁词列表）
        │
        └─► 组装响应体返回
                { code: 200, data: { items: [...], totalSensitiveCount } }
```

---

## 各层详解

### Controller — `CopyController.java`

- 路由：`POST /api/copy/generate`
- 职责：调用 Service → 合并文案与检测结果 → 构建响应 Map
- 错误处理：catch `Exception` 返回 `{ code: 500, message: e.getMessage() }`

**响应结构**：
```json
{
  "code": 200,
  "data": {
    "items": [
      {
        "index": 1,
        "text": "生成的文案",
        "sensitiveWords": [{ "word": "最好" }]
      }
    ],
    "totalSensitiveCount": 1
  }
}
```

---

### Service — `AiService.java`

**职责**：生成文案

关键实现细节：

```java
// 1. OkHttp 客户端根据配置的 timeout 创建（每次调用新建，可优化为 Bean 单例）
OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(timeout, TimeUnit.SECONDS)
    .readTimeout(timeout, TimeUnit.SECONDS)
    .build();

// 2. 从响应中提取文本
String content = root.path("choices").get(0).path("message").path("content").asText();

// 3. 按独立一行的 --- 分割（正则精确匹配，避免段落内 --- 误切）
content.split("(?m)^\\s*---\\s*$")

// 4. 截断到请求数量，防止模型多输出
if (result.size() > count) result = result.subList(0, count);
```

---

### Service — `PromptBuilder.java`

**职责**：根据 `platName + scene + extraFields` 拼装专属 Prompt

- 使用 Java `switch` 表达式（Text Block 语法）按场景精确匹配
- 每个 `case` 对应一个场景（如 `"产品标题"`、`"视频脚本"`），注入对应的 `extraFields` 字段值
- `get(extra, key)` 辅助方法：空值/空白字符串均返回 `null`，防止 "null" 字符串污染 Prompt
- 统一追加输出要求：生成数量、分隔符规则（`---`）、语气

**Prompt 结构**（每个场景）：
```
你是一位资深跨境广告文案专家，平台：{platName}。
任务：...（场景专属描述）
{extraFields 中的关键信息}

【输出要求】
- 生成 {count} 条独立变体
- 每条变体之间必须用单独一行「---」分隔
- 不加编号，不加多余符号
- 语气：{tone}
```

**已支持场景总览**：

| 平台 | 场景 |
|------|------|
| Amazon | 产品标题、5点描述、产品描述、A+内容标题、A+模块文案、Search Terms、Sponsored广告标题、品牌推广、买家评论回复、Q&A问答、Deal促销说明 |
| TikTok Shop | 视频脚本、直播话术、商品卡标题、商品描述、Hashtag文案、评论区互动、达人合作Brief、Shop广告文案、促销Banner |
| Facebook/Instagram | Feed广告主文案、Stories广告、Reels脚本、Post有机内容、广告标题、落地页文案、DM私信话术、评论回复、品牌故事、促销活动文案 |
| Shopify | 产品页标题、产品描述、SEO Meta描述、首页Banner、Email营销、弃购挽回邮件、Blog文章、促销Popup、客服话术、品牌Slogan |

---

### Service — `SensitiveWordService.java`

**职责**：批量检测文案中的违禁词

关键实现细节：

```java
// 1. 将所有文案拼接为编号列表，一次 LLM 调用完成批量检测
"1. 文案A\n2. 文案B\n..."

// 2. 要求模型只返回 JSON（防止 markdown 代码块干扰）
content.replaceAll("```json", "").replaceAll("```", "").trim();

// 3. 结果映射：index(1-based) → violations 列表
// 初始化等长空列表，确保无违禁词的条目也有对应空数组
```

**检测维度**（Prompt 内定义）：
- 绝对化用语（最好、第一、best、#1、guaranteed 等）
- 夸大宣传（100%有效、史上最等）
- 虚假承诺（永久保修、无条件退款等）
- 各主流电商平台常见违禁词

**LLM 返回格式**：
```json
[
  { "index": 1, "violations": ["最好", "第一"] },
  { "index": 2, "violations": [] }
]
```

---

## 配置项（`application.properties`）

```properties
# LLM 服务配置（兼容 OpenAI 格式的任意服务商）
openai.api-key=sk-xxxx
openai.base-url=https://api.openai.com    # 可替换为任意兼容端点
openai.model=gpt-4o-mini                   # 或 gpt-4o、claude 等
openai.timeout=60                          # 秒，建议 ≥ 60（生成数量多时耗时较长）

# 服务端口
server.port=8080
```

> **兼容性**：`base-url + /v1/chat/completions` 拼接调用，任何兼容 OpenAI Chat Completions 格式的服务商均可直接替换（如 Azure OpenAI、DeepSeek、Qwen 等）。

---

## 跨域配置（`CorsConfig.java`）

```java
registry.addMapping("/api/**")
    .allowedOrigins("http://localhost:5173")   // 前端开发服务器
    .allowedMethods("GET", "POST", "OPTIONS")
    .allowedHeaders("*");
```

> 生产部署时需将 `allowedOrigins` 改为实际域名，或配置 Nginx 统一处理跨域。

---

## 开发启动

```bash
# 1. 修改 application.properties 填入真实 API Key
# 2. 启动
mvn spring-boot:run

# 服务默认运行在 http://localhost:8080
```

---

## 扩展指南

### 新增场景

在 `PromptBuilder.java` 的 `buildScenePrompt()` 方法的 `switch` 中添加新的 `case`，同时在前端 `src/data/scenes.js` 对应平台的 `scenes` 数组中添加字段配置即可，**后端与前端场景名必须完全一致（`scene` 字段字符串匹配）**。

### 替换 LLM 服务商

只需修改 `application.properties`：
```properties
openai.base-url=https://api.deepseek.com
openai.api-key=your-deepseek-key
openai.model=deepseek-chat
```

### 持久化 / 历史记录

当前无数据库，所有生成结果均为无状态响应。如需持久化，可引入 Spring Data JPA + MySQL，在 `CopyController` 中 save 生成结果。

### 性能优化建议

- `OkHttpClient` 应声明为 `@Bean` 单例，避免每次请求重建
- 文案生成与敏感词检测可改为并行（但需注意两者存在数据依赖，目前为串行）
- 大量并发时可引入线程池限流，防止 LLM API 限速
