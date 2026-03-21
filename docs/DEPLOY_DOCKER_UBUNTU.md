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
在部分公网环境 `get.docker.com` 可能无法访问，建议优先用系统源安装。

### 方案 A：Ubuntu 系统源（最稳妥）
```bash
sudo apt update
sudo apt install -y docker.io docker-compose
sudo systemctl enable --now docker

# 可选：避免每次都 sudo（重新登录后生效）
sudo usermod -aG docker $USER
```

验证：
```bash
docker version
docker-compose --version
```

说明：
- 使用系统源时，只有 `docker-compose` 命令，没有 `docker compose` 子命令。
- 如果你坚持用 `docker compose` 语法，需要 Docker 官方仓库的 `docker-compose-plugin`。

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
docker-compose up -d --build
```

首次构建会编译 `canvas/sqlite3`，耗时略长属正常。

---

## 6. 初始化数据库（仅首次）
```bash
docker-compose exec app node db-init.js
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

## 10. 常见问题（本次部署遇到的问题汇总）
1. `curl https://get.docker.com` 连接被重置  
使用 Ubuntu 系统源安装：
```bash
sudo apt update
sudo apt install -y docker.io docker-compose
sudo systemctl enable --now docker
```

1. `docker compose` 报错：`unknown command`  
系统源安装没有 Compose 插件，只能用：
```bash
docker-compose up -d --build
```

1. `docker-compose-plugin` 包找不到  
这是正常的，系统源不提供该包；如果必须用 `docker compose` 语法，需要 Docker 官方仓库。

1. 构建阶段拉取 `node:20-bookworm-slim` 超时  
配置镜像加速：
```bash
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json > /dev/null <<'EOF'
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://hub-mirror.c.163.com",
    "https://docker.mirrors.ustc.edu.cn"
  ]
}
EOF
sudo systemctl restart docker
```
然后重新构建：
```bash
docker-compose build
docker-compose up -d
```

1. `esbuild` 报 `ETXTBSY` 导致 `npm ci` 失败  
已在 `Dockerfile` 中固定安装 `esbuild` 并设置 `ESBUILD_BINARY_PATH`，重新构建即可。

1. 容器启动后反复重启，日志提示 `GLIBC_2.38 not found`（sqlite3）  
已在 `Dockerfile` 中强制 `sqlite3` 从源码编译：
```dockerfile
RUN cd backend && npm ci --omit=dev --build-from-source=sqlite3
```
重新构建镜像即可：
```bash
docker-compose build --no-cache
docker-compose up -d
```

1. `git pull` 提示本地 `Dockerfile` 有修改  
如果不需要本地改动：
```bash
git checkout -- Dockerfile
git pull
```
如果需要保留本地改动：
```bash
git stash -u
git pull
git stash pop
```

1. 访问 502 或页面白屏  
确认容器运行正常：
```bash
docker-compose ps
docker-compose logs -f app
```

1. 容器启动但数据库为空  
确认是否执行过初始化：
```bash
docker-compose exec app node db-init.js
```

1. 需要改端口  
修改 `docker-compose.yml`：
```yaml
ports:
  - "3000:3000"
```

---

如果需要加 HTTPS（域名 + 证书），可以在宿主机加 Nginx/Certbot 做反向代理到 `http://127.0.0.1:3000`。
