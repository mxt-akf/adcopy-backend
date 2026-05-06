# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Local dev (requires OPENAI_API_KEY env var)
export OPENAI_API_KEY=sk-xxxx
mvn spring-boot:run

# Build JAR
mvn package

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=AdcopyBackendApplicationTests

# Docker
docker build -t adcopy-backend .
docker run -p 8080:8080 -e OPENAI_API_KEY=your-key -e CORS_ALLOWED_ORIGINS=https://yourdomain.com adcopy-backend
```

**Code formatter**: `fmt-maven-plugin` (Google Java Format) runs automatically during `mvn package` / `mvn spring-boot:run`. Run `mvn fmt:format` to format manually.

## Architecture

Spring Boot 3.4.4 / Java 17 service that generates ad copy via LLM APIs.

### Request flow

```
POST /api/copy/generate
  → RateLimitInterceptor (Bucket4j, 10 req/IP/min)
  → CopyController
      → AiService.generateCopy()
          → SceneConfigLoader.getTemplate/getFormat(scene)   # reads scenes.yml
          → PromptBuilder.build()                            # template var substitution + language + output format
          → OkHttp POST → OpenRouter /v1/chat/completions
          → split LLM response on standalone "---" lines → List<String>
      → SensitiveWordService.detect()
          → one LLM call, batch all texts, returns JSON array of violations
      → assemble GenerateResponse
```

`POST /api/copy/detect` is a standalone sensitive-word check endpoint (30 req/IP/min).  
`GET /api/scenes` returns all platform/scene configs (hides `template` field from response).

### Key design decisions

- **`scenes.yml`** is the single source of truth for all platform/scene configuration. Adding a new scene requires only editing this file — no Java changes needed. The `name` field in YAML must exactly match the `scene` field in API requests.
- **`format` field** in a scene config controls LLM output shape: `bullets` = 5-bullet-point sets, `default` = single copy variants. Both formats use `---` as the inter-item separator.
- **LLM provider is swappable**: `application.yml` `openai.*` properties point to any OpenAI-compatible endpoint. Current default is OpenRouter → DeepSeek.
- **`OkHttpClient`** in `AiService` is constructed inline (not injected from `OkHttpConfig`); `SensitiveWordService` also builds its own client. Both use the `openai.timeout` value.
- Rate-limit buckets are in-memory `ConcurrentHashMap`; they reset on restart and are not shared across instances.

### Environment variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `OPENAI_API_KEY` | ✅ | — | LLM API key |
| `CORS_ALLOWED_ORIGINS` | ❌ | `http://localhost:5173` | Comma-separated allowed origins |
