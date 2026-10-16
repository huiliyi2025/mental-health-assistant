# 心理健康助手

基于 Spring AI + Vue 3 的心理健康助手，包含前端与后端两个子项目。

## 项目结构

```
mental-health/
├── backend/    # Spring Boot 3 后端（Java 17 + Spring AI + MyBatis-Plus + MySQL）
│   ├── src/
│   ├── sql/init.sql   # 数据库初始化脚本
│   └── Dockerfile
├── frontend/   # Vue 3 + Vite + Element Plus 前端
│   ├── src/
│   ├── nginx.conf
│   └── Dockerfile
└── docker-compose.yml # 一键部署（MySQL + 后端 + 前端）
```

## 技术栈

- 后端：Spring Boot 3.5、Spring AI（兼容 OpenAI 协议，默认硅基流动 SiliconFlow）、MyBatis-Plus、MySQL 8、Spring Security + JWT
- 前端：Vue 3、Vite、Element Plus、Pinia、Vue Router、Axios、ECharts、wangEditor

## 功能

- 用户：注册、登录、JWT 鉴权、退出
- AI 心理对话：新建/历史会话、流式回复（SSE）、会话删除
- 情绪日志：用户记录情绪日记，管理端查看/删除
- 知识文章：分类、分页列表、详情、后台增删改与发布/下线
- 文件上传：文章封面等图片上传与访问
- 数据分析：后台仪表盘概览（用户/会话/情绪趋势）


## 快速开始

### 1. 初始化数据库

```bash
mysql -uroot -p < backend/sql/init.sql
```

### 2. 启动后端

先配置环境变量（或修改 `backend/src/main/resources/application.yml`）：

| 变量 | 说明 | 示例 |
| --- | --- | --- |
| `DB_URL` | MySQL 连接地址 | `jdbc:mysql://localhost:3306/mental_health_assistant?...` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | `your-db-password` |
| `AI_API_KEY` | 大模型 API Key（硅基流动等） | `sk-xxx` |
| `AI_BASE_URL` | 模型服务地址 | `https://api.siliconflow.cn` |
| `AI_MODEL` | 模型名称 | `deepseek-ai/DeepSeek-V3` |
| `JWT_SECRET` | JWT 签名密钥 | 随机字符串 |
| `file.upload-dir` | 上传文件存储目录（可选） | 默认 `./upload` |

```bash
cd backend
./mvnw spring-boot:run          # Windows 用 mvnw.cmd spring-boot:run
```

后端默认端口：`1236`

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器默认把 `/api` 请求代理到**本机后端** `http://localhost:1236`
（见 `frontend/vite.config.js`，可用环境变量 `VITE_API_TARGET` 覆盖，例如指向远程服务器）。

浏览器打开 `http://localhost:5173` 即可访问。

## Docker 一键部署

```bash
cp .env.example .env     # 填 AI_API_KEY / MYSQL_ROOT_PASSWORD / JWT_SECRET
docker compose up -d --build
```

启动后：前端 `http://localhost`，后端 `http://localhost:1236`。

## 接口文档（Swagger）

后端启动后访问 `http://localhost:1236/swagger-ui.html`。

## 本地 / 远程后端切换

`frontend/vite.config.js` 中的代理目标默认为本机后端：

```js
const API_TARGET = process.env.VITE_API_TARGET || 'http://localhost:1236'
```

- 想连远程服务器时（PowerShell）：`$env:VITE_API_TARGET="http://你的服务器:1235"; npm run dev`
- 想连本机后端时：直接 `npm run dev`

⚠️ 注意：连到远程共享服务器时，你的数据会存到服务器上、也可能看到别人的数据。

## 安全提示

- `application.yml` 中所有敏感配置（数据库密码、JWT 密钥、AI API Key）均已改为环境变量占位符，请勿提交真实密钥。
- 生产环境务必设置强随机 `JWT_SECRET`。

## 测试账号

- 用户名：`admin`　密码：`123456`（由 `sql/init.sql` 初始化，BCrypt 加密存储）
