# AdCopy AI — 后端 README

> **定位**：跨境广告文案裂变工具的后端服务，基于 Spring Boot 3.4.4 构建，负责接收前端请求、加载 YAML 场景配置、构建 Prompt、调用 LLM API 生成文案，并进行敏感词检测后返回结构化结果。支持限流、Docker 容器化部署。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 框架 | Spring Boot 3.4.4 |
| 语言 | Java 17 |
| HTTP 客户端 | OkHttp3 4.12.0（调用 LLM API） |
| JSON/YAML 处理 | Jackson + jackson-dataformat-yaml |
| 限流 | Bucket4j 8.10.1（令牌桶，按 IP） |
| 配置管理 | `application.yml` + 环境变量 |
| 构建工具 | Maven |
| 容器化 | Docker 多阶段构建（JRE 17） |

---

## 目录结构

```
src/main/java/com/adcopy/adcopy_backend/
├── AdcopyBackendApplication.java
├── config/
│   ├── CorsConfig.java             # 跨域配置（环境变量驱动 allowed-origins）
│   ├── OkHttpConfig.java           # OkHttpClient Bean（连接池 5 个，超时可配）
│   ├── RateLimitInterceptor.java   # 限流拦截器（Bucket4j，按 IP 令牌桶）
│   └── WebConfig.java              # 注册拦截器，应用到 /api/copy/**
├── controller/
│   ├── CopyController.java         # 文案生成与敏感词检测接口
│   └── SceneController.java        # 场景配置查询接口
├── model/
│   ├── GenerateRequest.java        # 生成文案请求 DTO
│   ├── GenerateResponse.java       # 响应体 DTO（含嵌套类）
│   ├── DetectRequest.java          # 敏感词检测请求 DTO
│   └── scene/
│       ├── PlatformConfig.java     # 平台配置（id, name, scenes）
│       ├── SceneConfig.java        # 场景配置（name, riskLevel, format, template, fields）
│       └── FieldConfig.java        # 场景字段配置（key, label, type, options…）
└── service/
    ├── AiService.java              # 文案生成：构建 Prompt → 调用 LLM → 分割结果
    ├── PromptBuilder.java          # Prompt 工厂：模板变量替换 + 语言约束 + 输出格式
    ├── SceneConfigLoader.java      # 从 scenes.yml 加载平台/场景/模板配置
    ├── SensitiveWordService.java   # 敏感词批量检测（LLM 驱动）
    └── SensitiveWordRules.java     # 各平台违禁规则常量（静态工具类）

src/main/resources/
├── application.yml                 # 服务配置（端口、LLM、CORS、日志）
└── scenes.yml                      # 平台场景模板配置（Prompt 模板 + 字段定义）
```

---

## REST API

### POST `/api/copy/generate` — 生成广告文案

**请求体**：
```json
{
  "count": 10,
  "tone": "专业的",
  "language": "中文",
  "platName": "Amazon",
  "scene": "产品标题",
  "extraFields": {
    "productName": "无线蓝牙耳机",
    "keywords": "降噪, 长续航"
  }
}
```

**响应体**：
```json
{
  "code": 200,
  "data": {
    "items": [
      {
        "index": 1,
        "text": "生成的文案内容",
        "sensitiveWords": [{ "word": "最好" }]
      }
    ],
    "totalSensitiveCount": 1
  }
}
```

> **限流**：每 IP 每分钟最多 **10 次**（AI 调用成本高）

---

### POST `/api/copy/detect` — 敏感词检测

**请求体**：
```json
{
  "texts": ["文案A", "文案B"],
  "platName": "Amazon"
}
```

**响应体**：
```json
{
  "code": 200,
  "data": {
    "items": [
      { "index": 1, "sensitiveWords": [{ "word": "最好" }] },
      { "index": 2, "sensitiveWords": [] }
    ],
    "totalSensitiveCount": 1
  }
}
```

> **限流**：每 IP 每分钟最多 **30 次**

---

### GET `/api/scenes` — 获取场景配置

无需请求体，返回所有平台及其场景配置（前端用于动态渲染表单）。

```json
{
  "code": 200,
  "data": [
    {
      "id": "amazon",
      "name": "Amazon",
      "scenes": [
        {
          "name": "产品标题",
          "riskLevel": "high",
          "format": "default",
          "fields": [
            { "key": "productName", "label": "产品名称", "type": "input", "required": true }
          ]
        }
      ]
    }
  ]
}
```

> `template` 字段（Prompt 原文）在响应中被隐藏，仅用于服务端构建 Prompt。

---

## 核心业务流程

```
POST /api/copy/generate
        │
        ▼ RateLimitInterceptor（10 次/IP/分钟）
        ▼
CopyController.generate(GenerateRequest)
        │
        ├─► AiService.generateCopy()
        │       │
        │       ├─► SceneConfigLoader.getTemplate(scene) + getFormat(scene)
        │       ├─► PromptBuilder.build()   # 变量替换 + 语言约束 + 输出格式
        │       ├─► OkHttp POST → OpenRouter /v1/chat/completions
        │       └─► 按独立一行 "---" 分割 → List<String>（截断到 count 条）
        │
        ├─► SensitiveWordService.detect(texts, platName)
        │       │
        │       ├─► 构建编号列表 Prompt（一次 LLM 调用批量检测）
        │       ├─► OkHttp POST → OpenRouter /v1/chat/completions
        │       └─► 解析 JSON → List<List<String>>（每条文案的违禁词）
        │
        └─► 组装响应体返回
```

---

## 场景配置（`scenes.yml`）

场景通过 YAML 文件动态配置，无需修改 Java 代码即可新增场景。每个场景包含：

| 字段 | 说明 |
|------|------|
| `name` | 场景名称（与前端请求中的 `scene` 字段严格匹配） |
| `riskLevel` | 违禁风险等级：`high` / `medium` / `low` |
| `format` | 输出格式：`bullets`（5点描述等列表类）/ `default` |
| `template` | Prompt 模板（含 `{变量}` 占位符，服务端内部使用） |
| `fields` | 前端表单字段配置（input / textarea / radio） |

**已支持平台与场景**：

| 平台 | 场景（共约 40+） |
|------|------|
| Amazon | 产品标题、5点描述、产品描述、A+内容标题、A+模块文案、Search Terms、Sponsored广告标题、品牌推广文案、买家评论回复、Q&A问答、Deal促销说明 |
| TikTok Shop | 视频脚本、直播话术、商品卡标题、商品描述、Hashtag文案、评论区互动、达人合作Brief、Shop广告文案、促销Banner |
| Facebook / Instagram | Feed广告主文案、Stories广告、Reels脚本、Post有机内容、广告标题、落地页文案、DM私信话术、评论回复、品牌故事、促销活动文案 |
| Shopify 独立站 | 产品页标题、产品描述、SEO Meta描述、首页Banner、Email营销、弃购挽回邮件、Blog文章、促销Popup、客服话术、品牌Slogan |

---

## 限流配置（`RateLimitInterceptor`）

基于 Bucket4j 令牌桶算法，按客户端 IP 独立计数：

| 接口 | 限制 | 说明 |
|------|------|------|
| `/api/copy/generate` | 10 次/IP/分钟 | AI 调用成本较高 |
| `/api/copy/detect` | 30 次/IP/分钟 | 相对轻量 |

超限返回 HTTP 429：
```json
{ "code": 429, "message": "请求过于频繁，请稍后再试" }
```

> 支持 Nginx 反向代理：优先读取 `X-Forwarded-For` 头获取真实 IP。

---

## 配置项（`application.yml`）

```yaml
server:
  port: 8080

openai:
  base-url: https://openrouter.ai/api   # 任意兼容 OpenAI Chat Completions 格式的服务商
  model: deepseek/deepseek-chat         # 或 gpt-4o、claude-3-5-sonnet 等
  api-key: ${OPENAI_API_KEY}            # 通过环境变量注入
  timeout: 60                           # 秒，建议 ≥ 60

app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}  # 多个用逗号分隔

logging:
  level:
    com.adcopy: DEBUG
```

> **兼容性**：任何支持 OpenAI Chat Completions 格式的服务商（OpenRouter、DeepSeek、Azure OpenAI、Qwen 等）均可直接通过修改 `base-url`、`model`、`api-key` 接入。

---

## Docker 部署

**多阶段构建**（Maven 构建 + JRE 17 运行时）：

```bash
# 构建镜像
docker build -t adcopy-backend .

# 运行容器
docker run -p 8080:8080 \
  -e OPENAI_API_KEY=your-api-key \
  -e CORS_ALLOWED_ORIGINS=https://yourdomain.com \
  adcopy-backend
```

---

## 本地开发启动

```bash
# 1. 设置环境变量（或在 application.yml 中直接填写）
export OPENAI_API_KEY=sk-xxxx

# 2. 启动
mvn spring-boot:run

# 服务运行在 http://localhost:8080
```

---

## 扩展指南

### 新增场景

在 `src/main/resources/scenes.yml` 中对应平台下添加新场景配置（name、riskLevel、format、template、fields），**无需修改任何 Java 代码**。前端 `scenes.js` 中的场景 `name` 必须与 YAML 完全一致。

### 替换 LLM 服务商

修改 `application.yml` 或对应环境变量：
```yaml
openai:
  base-url: https://api.deepseek.com
  api-key: your-deepseek-key
  model: deepseek-chat
```

### 持久化 / 历史记录

当前无数据库，所有生成结果为无状态响应。如需持久化，可引入 Spring Data JPA + MySQL，在 `CopyController` 中保存生成结果。

### 性能优化建议

- `OkHttpClient` 已声明为 `@Bean` 单例（连接池 5 个），无需每次重建
- 文案生成与敏感词检测目前串行；如需并行可改为 `CompletableFuture`（注意两者存在数据依赖）
- 大量并发时可调整 Bucket4j 的令牌桶容量与补充速率
