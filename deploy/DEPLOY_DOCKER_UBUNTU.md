# Ubuntu 公网服务器 Docker 快速部署（class-pet-house）

适用范围：Ubuntu 20.04/22.04/24.04。  
部署方式：单容器运行后端并托管前端静态资源（前端已打包到 `frontend/dist`）。  
数据库：当前后端默认使用 **SQLite**，数据持久化到宿主机目录 `./data`。

---

## 0. 端口与安全组
需要放行端口：
- `22`（SSH）
- `80`（HTTP）
- `443`（可选，HTTPS）

如果你不想用 80 端口，可以改为 `3000`（见下方 `docker-compose.yml` 端口映射）。

---

## 1. 安装 Docker 与 Compose
```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg
curl -fsSL https://get.docker.com | sudo sh
sudo systemctl enable --now docker

# 可选：避免每次都 sudo（重新登录后生效）
sudo usermod -aG docker $USER
```

验证：
```bash
docker version
docker compose version
```

---

## 2. 安装 Git
```bash
sudo apt update
sudo apt install -y git
git --version
```

---

## 3. 获取代码
```bash
mkdir -p /srv/class-pet-house
cd /srv/class-pet-house
git clone <你的仓库地址> .
```

---

## 4. 配置环境变量
新建文件 `deploy/.env.backend`（示例）：
```env
NODE_ENV=production
PORT=3000

# SQLite 数据库文件（容器内路径）
DB_STORAGE=/data/class_pets.db

# JWT
JWT_SECRET=请改成复杂随机串
JWT_EXPIRES_IN=7d

# 管理后台账号
ADMIN_USERNAME=admin
ADMIN_PASSWORD=请修改

# AI（可选，未使用可留空）
AI_API_KEY=
AI_API_URL=https://api.deepseek.com/v1/chat/completions
AI_MODEL=deepseek-chat
```

说明：
- `DB_STORAGE` 必须是容器内路径，数据会落在宿主机 `./data` 目录。
- `JWT_SECRET`、`ADMIN_PASSWORD` 一定要改。

---

## 5. 构建并启动
项目已包含 `Dockerfile` 与 `docker-compose.yml`，直接构建运行：
```bash
docker compose up -d --build
```

首次构建会编译 `canvas/sqlite3`，耗时略长属正常。

---

## 6. 初始化数据库（仅首次）
```bash
docker compose exec app node db-init.js
```

执行成功后会在宿主机生成 `./data/class_pets.db`。

---

## 7. 访问验证
```bash
curl http://127.0.0.1/api/health
```

浏览器访问：
```
http://你的公网IP
```

---

## 8. 日常维护
查看日志：
```bash
docker compose logs -f app
```

重启：
```bash
docker compose restart app
```

停止：
```bash
docker compose down
```

更新部署：
```bash
git pull
docker compose build
docker compose up -d
```

---

## 9. 备份数据
SQLite 数据在 `./data` 目录，可直接打包备份：
```bash
tar -czf class-pet-house-db-$(date +%F).tgz data
```

---

## 10. 常见问题
1. 访问 502 或页面白屏  
确认容器运行正常：
```bash
docker compose ps
docker compose logs -f app
```

1. 容器启动但数据库为空  
确认是否执行过初始化：
```bash
docker compose exec app node db-init.js
```

1. 需要改端口  
修改 `docker-compose.yml`：
```yaml
ports:
  - "3000:3000"
```

---

如果需要加 HTTPS（域名 + 证书），可以在宿主机加 Nginx/Certbot 做反向代理到 `http://127.0.0.1:3000`。
