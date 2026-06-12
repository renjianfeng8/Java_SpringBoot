# 生产部署说明

## 推荐方式

使用云服务器 + Docker Compose 部署 MySQL、后端和前端服务。

## 准备环境

- Docker 24+
- Docker Compose v2+
- 开放端口：80、9090；MySQL 映射到 3307 时应仅限内网或安全组访问

## 配置环境变量

```bash
cp .env.example .env
```

修改 `.env`：

- `BACKEND_PORT` — 后端对宿主机暴露的端口
- `DB_PASSWORD` — 数据库密码
- `JWT_SECRET` — JWT 签名密钥（至少 32 位随机字符）
- `FILE_UPLOAD_DIR` — 文件上传目录
- `FILE_ACCESS_PREFIX` — 上传文件公开访问前缀，默认 `/files/`
- `VITE_API_BASE_URL` — 前端 API 基础地址，Docker Compose 默认使用同源 `/`

Compose 会为后端启用 `prod` profile，并把数据库连接、JWT、上传目录和 SQL 日志级别通过环境变量注入。
首次启动时，MySQL 会按 `01-schema.sql`、`02-data.sql` 顺序初始化数据库。

## 启动

```bash
docker compose up -d --build
```

## 健康检查

```bash
curl http://localhost:9090/api/v1/health
curl http://localhost/
```

后端返回 `code=200` 且 `data.status=UP`，前端返回 HTML，表示两个容器入口可用。数据库和反向代理仍应通过登录、查询等业务请求验证。

## 演示前检查

- 管理员账号可以登录
- 影院管理员账号可以登录
- 用户账号可以登录
- 用户端首页可以打开
- 用户可以进入电影详情并选择影院/排片
- 用户订单页可以看到订单状态
- 影院端只能看到本影院相关数据
- 后端健康检查返回 UP
- GitHub Actions 最近一次通过

## 查看日志

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
```

## 回滚

```bash
git checkout <last-good-commit>
docker compose up -d --build
```
