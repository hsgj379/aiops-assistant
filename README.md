# AI DevOps Assistant

> 一个面向 Java AI 应用开发工程师求职的中高级个人项目：**AIOps 智能运维助手**。
> 整合告警、指标、日志、知识库与 LLM（阿里云百炼）能力，提供自然语言运维问答、自动化诊断与工单闭环。

## 目录结构

```
aiops-assistant/
├── aiops-server/          # Spring Boot 后端（模块化单体，Java 8）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/aiops/assistant/
│       │   ├── AiopsAssistantApplication.java
│       │   ├── common/        # 通用类型（Result 等）
│       │   ├── config/        # Web / CORS / Redis / JPA
│       │   ├── controller/    # REST API
│       │   ├── service/       # 业务逻辑
│       │   ├── entity/        # JPA 实体
│       │   └── repository/    # Spring Data JPA
│       └── resources/
│           ├── application.yml
│           └── sql/schema.sql
├── aiops-web/             # Vue 3 + Vite 前端（待开发）
├── aiops-mcp/             # Python MCP Server（待开发）
├── docker-data/           # Docker Compose 数据卷（已在 .gitignore）
├── deploy/                # 部署相关
├── docs/                  # 设计文档
├── sql/                   # 额外 SQL 脚本
├── test-data/             # 测试数据
├── docker-compose.yml     # MySQL / Redis / RabbitMQ
└── .gitignore
```

## 技术栈

| 组件 | 版本 | 说明 |
|---|---|---|
| 后端 | Spring Boot 2.7.18 | Java 8，JPA（ddl-auto=validate）+ Redis + AMQP + Actuator |
| 数据库 | MySQL 8.0.26 | utf8mb4，应用账户 aiops，root 仅用于容器初始化 |
| 缓存 | Redis 7.2 | AOF 持久化，仅绑定 127.0.0.1 |
| 消息队列 | RabbitMQ 3.13 | 带 management 插件，独立 aiops 用户，仅绑定 127.0.0.1 |
| 向量库 | Qdrant | 后续通过 Docker 启用 |
| LLM | 阿里云百炼 DashScope | OpenAI 兼容 API |
| 前端 | Vue 3 + Vite | 待开发 |
| MCP | Python 3.14 + mcp 2.3.0 | 待开发 |

## 快速开始

### 1. 启动中间件（Docker Compose）

要求：Docker Desktop 已运行、WSL2 已启用。

```powershell
# 在项目根目录
docker compose up -d

# 查看状态
docker compose ps

# 查看日志
docker compose logs -f mysql
```

首次启动 MySQL 会自动：
- 创建库 `aiops_assistant`
- 执行 `aiops-server/src/main/resources/sql/schema.sql` 初始化 8 张核心表

### 2. 配置阿里云百炼 API Key

**方式 A — 设置系统环境变量（推荐）**
```powershell
# PowerShell（当前会话）
$env:DASHSCOPE_API_KEY = "sk-xxxxxxxxxxxxxxxx"
$env:DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
$env:DASHSCOPE_MODEL = "qwen-plus"

# 或持久化到用户环境变量
[Environment]::SetEnvironmentVariable("DASHSCOPE_API_KEY", "sk-xxx", "User")
```

**方式 B — 命令行注入**
```powershell
mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-DDASHSCOPE_API_KEY=sk-xxx"
```

> ⚠️ **禁止**把 API Key 写入任何源码文件或 `application.yml`；`application.yml` 中已用 `${DASHSCOPE_API_KEY:}` 占位符，环境变量为空时对应功能自动降级。

### 3. 启动后端

**方式 A — Maven 运行**
```powershell
cd aiops-server
mvn spring-boot:run
```

**方式 B — 打包后运行**
```powershell
cd aiops-server
mvn clean package -DskipTests
java -jar target/aiops-server.jar
```

**方式 C — 走本地 MySQL（跳过 Docker 中间件）**

编辑 `aiops-server/src/main/resources/application.yml`，把 `spring.datasource.url` 等三条 Docker 默认配置**注释掉**，启用下面注释块里的 `localhost` 配置。Redis / RabbitMQ 同理。

### 4. 健康检查

```powershell
# PowerShell
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/health" | ConvertTo-Json

# 或浏览器访问
# http://127.0.0.1:8080/api/health
# http://127.0.0.1:8080/actuator/health
```

### 5. 推送到 GitHub

```powershell
# 首次推送（仓库已初始化）
git status
git add -A
git commit -m "feat: init backend skeleton /w docker-compose and 8 core tables"
git push origin main
```

## 可用端口

| 服务 | 端口 | 绑定 | 账号/密码（Docker 默认值） |
|---|---|---|---|
| aiops-server | 8080 | 0.0.0.0 | 无（HTTP） |
| MySQL | 3306 | 0.0.0.0 | 应用：aiops / aiops_pwd；root 仅容器内 |
| Redis | 6379 | 127.0.0.1 | 无密码 |
| RabbitMQ | 5672 | 127.0.0.1 | aiops / aiops_pwd |
| RabbitMQ 管理 | 15672 | 127.0.0.1 | aiops / aiops_pwd |

## 环境变量

所有敏感配置通过环境变量注入，`application.yml` 使用 `${VAR:默认值}` 占位符。

| 变量 | 默认值 | 说明 |
|---|---|---|
| `MYSQL_HOST` | mysql | MySQL 主机（Docker 服务名） |
| `MYSQL_PORT` | 3306 | MySQL 端口 |
| `MYSQL_DB` | aiops_assistant | 数据库名 |
| `MYSQL_USER` | aiops | 应用运行账户 |
| `MYSQL_PASSWORD` | （空） | 应用账户密码，**必须显式设置** |
| `MYSQL_ROOT_PASSWORD` | root_pwd | root 密码，仅容器初始化时用 |
| `REDIS_HOST` | redis | Redis 主机 |
| `REDIS_PORT` | 6379 | Redis 端口 |
| `RABBITMQ_HOST` | rabbitmq | RabbitMQ 主机 |
| `RABBITMQ_PORT` | 5672 | RabbitMQ AMQP 端口 |
| `RABBITMQ_USER` | aiops | RabbitMQ 用户 |
| `RABBITMQ_PASSWORD` | aiops_pwd | RabbitMQ 密码 |
| `DASHSCOPE_API_KEY` | （空） | 阿里云百炼 API Key |
| `DASHSCOPE_BASE_URL` | https://dashscope.aliyuncs.com/compatible-mode/v1 | OpenAI 兼容端点 |
| `DASHSCOPE_MODEL` | qwen-plus | 默认模型 |

快速设置（PowerShell 当前会话）：
```powershell
$env:MYSQL_PASSWORD = "aiops_pwd"            # 与 docker-compose.yml 保持一致
$env:MYSQL_ROOT_PASSWORD = "root_pwd"
$env:RABBITMQ_PASSWORD = "aiops_pwd"
$env:DASHSCOPE_API_KEY = "sk-xxxxxxxxxxxxxxxx"
```

## 数据库

```sql
USE aiops_assistant;
SHOW TABLES;
```

核心表：`users`, `alerts`, `metrics`, `logs`, `tickets`, `knowledge_bases`, `models`, `tasks`。

## 开发规范

- Java 版本固定 **1.8**，不升级。
- 编码统一 **UTF-8**（`pom.xml` / `application.yml` / MySQL / JSON）。
- API Key、Token、密码**一律环境变量管理**，禁止硬编码。
- 中间件优先走 Docker，必要时切换本地 localhost 需显式注释/启用配置块。
- 新增依赖前先查 BOM 是否已管理版本（Spring Boot 2.7.18 BOM 里已含 mysql-connector-j 8.0.33，不要用旧坐标 `mysql:mysql-connector-java`）。
- JPA 使用 `spring-boot-starter-data-jpa`，仓库层用 **`repository`** 包（Spring Data JPA），不引入 MyBatis / MyBatis-Plus。
- Hibernate `ddl-auto=validate` — 启动时只校验 schema，不自动修改/删除已有表。DDL 通过 `schema.sql` + Docker 初始化脚本**明确可控**地执行。
- **不要用 root 账户作为应用运行账户**。MySQL root 仅用于容器初始化，应用通过独立 `aiops` 账户连接。
- **不要用 RabbitMQ 默认 guest 用户**。Docker Compose 已创建独立 `aiops` 用户。
- Redis / RabbitMQ 端口仅绑定 `127.0.0.1`（容器内网络仍互通），避免暴露到局域网。
- `/api/health` 只检查 Java 应用自身存活，不混入中间件连接状态。中间件健康通过 Actuator 单独端点按需启用。

## 下一阶段

- [x] 阶段 1 — 环境检查与骨架
- [x] 阶段 2 — 后端骨架 + 中间件编排 + 核心 DDL
- [ ] 阶段 3 — 用户/认证 + 基础 CRUD（users/alerts/tickets）
- [ ] 阶段 4 — AI 集成：阿里云百炼 + 知识库检索
- [ ] 阶段 5 — Vue 3 前端骨架 + 页面
- [ ] 阶段 6 — Python MCP Server
- [ ] 阶段 7 — Docker 化后端 + 全链路 Compose + CI/CD
