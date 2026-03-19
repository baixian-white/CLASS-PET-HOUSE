# 腾讯云 Ubuntu 22.04 部署说明（class-pet-house）

本文档基于当前仓库实际代码结构编写，适用于在腾讯云 Ubuntu 22.04 上部署本项目（前端 Vite + 后端 Node/Express + SQLite）。

重点提醒：
1. 当前后端使用 **SQLite**（见 `backend/src/config/database.js`），不是 MySQL。
1. 后端依赖 `canvas` 和 `sqlite3`，需要系统编译依赖。
1. 生产环境不会自动建表，需先执行一次 `backend/db-init.js`。

---

## 1. 服务器准备
1. 腾讯云安全组放行端口：`22`（SSH）、`80`（HTTP）、`443`（HTTPS）。
1. 安装系统依赖（建议先更新）：
```bash
sudo apt update
sudo apt install -y git curl wget tar \
  build-essential python3 pkg-config \
  libcairo2-dev libpango1.0-dev libjpeg-dev libgif-dev librsvg2-dev libpng-dev \
  sqlite3 libsqlite3-dev
```

说明：
1. `canvas` 依赖图形库，缺失会导致 `npm install` 失败。
1. `sqlite3` 需要 `libsqlite3-dev` 与编译工具链。

---

## 2. 安装 Node.js（要求 18+，推荐 20）

### 方案 A：NodeSource（推荐）
```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
node -v
npm -v
```

### 方案 B：nvm（系统受限时）
```bash
curl -fsSL https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
source ~/.bashrc
nvm install 20
nvm use 20
```

---

## 3. 拉取项目代码

推荐部署目录：`/opt/CLASS-PET-HOUSE`
```bash
sudo mkdir -p /opt
sudo chown -R $USER:$USER /opt

cd /opt
git clone <你的仓库地址> CLASS-PET-HOUSE
cd /opt/CLASS-PET-HOUSE
```

---

## 4. 配置后端环境变量

后端读取 `backend/.env`（`backend/.env.example` 可作模板）：
```bash
cp backend/.env.example backend/.env
```

建议修改 `backend/.env`（示例）：
```env
# SQLite 数据库文件路径（建议用绝对路径）
DB_STORAGE=/opt/CLASS-PET-HOUSE/backend/class_pets.db

# JWT
JWT_SECRET=请改成复杂随机串
JWT_EXPIRES_IN=7d

# 服务端口
PORT=3000

# 管理后台账号
ADMIN_USERNAME=admin
ADMIN_PASSWORD=请修改
# AI 功能（可选）
AI_API_KEY=你的API_KEY
AI_API_URL=https://api.deepseek.com/v1/chat/completions
AI_MODEL=deepseek-chat
```

注意：
1. `.env.example` 中的 MySQL 字段当前未使用。
1. `AI_*` 变量未写在 `.env.example`，但 `/api/ai/*` 需要。

---

## 5. 安装依赖并构建前端
```bash
cd /opt/CLASS-PET-HOUSE

# 后端依赖（生产环境）
cd backend
npm ci --omit=dev
cd ..

# 前端依赖 + 构建
cd frontend
npm ci
npm run build
cd ..
```

构建产物在 `frontend/dist`，Nginx 可直接访问该目录。

---

## 6. 初始化数据库

生产环境默认不自动建表，需先初始化一次：
```bash
cd /opt/CLASS-PET-HOUSE
node backend/db-init.js
```

执行成功后会生成 `backend/class_pets.db`（或 `DB_STORAGE` 指定路径）。

---

## 7. 使用 PM2 启动后端

安装并启动：
```bash
sudo npm i -g pm2

cd /opt/CLASS-PET-HOUSE
pm2 start deploy/ecosystem.config.json
pm2 status
```

`deploy/ecosystem.config.json` 固定了 `cwd=/opt/CLASS-PET-HOUSE/backend`，
若部署路径不同，请修改该文件。

设置开机自启：
```bash
pm2 startup systemd -u $USER --hp $HOME
pm2 save
```

查看日志：
```bash
pm2 logs class-pet-house
```

---

## 8. 配置 Nginx

### 8.1 安装 Nginx
```bash
sudo apt install -y nginx
sudo systemctl enable nginx
```

### 8.2 配置站点

复制 `deploy/nginx.conf`：
```bash
sudo cp /opt/CLASS-PET-HOUSE/deploy/nginx.conf /etc/nginx/conf.d/class-pet-house.conf
```

编辑 `/etc/nginx/conf.d/class-pet-house.conf`：
1. 将 `server_name` 改为你的域名或公网 IP
1. 确认 `root` 指向 `/opt/CLASS-PET-HOUSE/frontend/dist`

测试并重启：
```bash
sudo nginx -t
sudo systemctl restart nginx
```

---

## 9. 访问验证

```bash
# 本机检查后端
curl http://127.0.0.1:3000/api/health

# 浏览器访问（替换为你的域名或公网 IP）
http://your-domain.com
```

---

## 10. 更新部署流程（后续版本）

```bash
cd /opt/CLASS-PET-HOUSE
git pull

# 后端更新依赖
cd backend && npm ci --omit=dev && cd ..

# 前端重新构建
cd frontend && npm ci && npm run build && cd ..

# 重启后端
pm2 restart class-pet-house
```

---

## 11. 常见问题

1. `npm install` 失败（canvas / sqlite3 编译错误）
1. 确认已安装 `build-essential python3 pkg-config libcairo2-dev libpango1.0-dev libjpeg-dev libgif-dev librsvg2-dev libpng-dev libsqlite3-dev`

1. 页面能打开但接口 502
1. 检查 `pm2 status`，确认后端在运行
1. Nginx 配置中的 `proxy_pass` 是否正确（默认 127.0.0.1:3000）

1. 访问 `/api/ai/*` 提示未配置
1. 在 `backend/.env` 增加 `AI_API_KEY` 等变量并重启 PM2

---

## 12. 可选：改用 MySQL（需改代码）

当前版本后端强制使用 SQLite（见 `backend/src/config/database.js`）。
若必须使用 MySQL，需要：

1. 修改 `backend/src/config/database.js`，将 `dialect` 改为 `mysql` 并读取 `DB_HOST/DB_PORT/...`
1. 安装并配置 MySQL
1. 重新运行 `node backend/db-init.js`

建议先在测试环境验证。
