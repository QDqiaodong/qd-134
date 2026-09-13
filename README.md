# qd-134 水下潜水基地装备深度区间绑定系统

## 项目简介

水下潜水基地装备、潜水小组与深度区间绑定管理系统。项目包含 Vue/Vite 前端、Spring Boot 后端、MySQL 与 Redis，已按依赖层缓存和固定端口交付链路规范整理。

## 访问地址

- 前端地址: [http://localhost:8195](http://localhost:8195)
- 127.0.0.1 地址: [http://127.0.0.1:8195](http://127.0.0.1:8195)
- 后端 API: http://localhost:8184/api

## 端口

- 前端: 8195
- 后端: 8184
- MySQL: 3360
- Redis: 6433

## 编译与启动

```bash
cd backend
mvn compile -q

cd ../frontend
npm ci
npm run build

cd ..
docker compose up -d --build
```

Docker Compose 端口均绑定到 `127.0.0.1`，镜像基础地址通过 `.env` 中的 `DOCKER_REGISTRY` 统一控制。
