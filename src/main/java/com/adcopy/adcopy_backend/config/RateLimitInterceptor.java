package com.adcopy.adcopy_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    // generate 接口：每个 IP 每分钟最多 10 次（调 AI，成本高）
    // detect  接口：每个 IP 每分钟最多 30 次
    private final Map<String, Bucket> generateBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> detectBuckets   = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler)
            throws Exception {

        String uri = req.getRequestURI();
        String ip  = resolveClientIp(req);

        // 根据请求路径选择对应的限流桶
        Bucket bucket = uri.contains("/generate")
                ? generateBuckets.computeIfAbsent(ip, k -> buildBucket(10, Duration.ofMinutes(1)))
                : detectBuckets  .computeIfAbsent(ip, k -> buildBucket(30, Duration.ofMinutes(1)));

        // 还有令牌就放行，没有就返回 429
        if (bucket.tryConsume(1)) return true;

        res.setStatus(429);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(
                objectMapper.writeValueAsString(Map.of("code", 429, "message", "请求过于频繁，请稍后再试"))
        );
        return false;
    }

    private Bucket buildBucket(int capacity, Duration duration) {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(capacity).refillIntervally(capacity, duration))
                .build();
    }

    // 优先取 X-Forwarded-For 头，兼容 Nginx 反代后真实 IP 被替换的情况
    private String resolveClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}