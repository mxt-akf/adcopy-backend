#!/bin/bash
set -e

echo "📦 拉取最新代码..."
git pull

echo "🔨 构建镜像..."
docker build -t adcopy-backend .

echo "🔄 重启容器..."
docker stop adcopy-backend 2>/dev/null || true
docker rm adcopy-backend 2>/dev/null || true

docker run -d \
  --name adcopy-backend \
  -p 8080:8080 \
  --restart unless-stopped \
  --env-file .env \
  adcopy-backend

echo "✅ 部署完成！"
docker logs adcopy-backend --tail 20