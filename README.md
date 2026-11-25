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
│       │   ├── config/        # Web / CORS
│       │   ├── controller/    # REST API
│       │   ├── service/       # 业务逻辑
│       │   ├── entity/        # JPA 实体（待建）
│       │   └── repository/    # Spring Data JPA（待建）
│       └── resources/
│           ├── application.yml            # 公共配置 + 默认 profile=local
│           ├── application-local.yml      # 本地开发：全部 127.0.0.1
│           ├── application-docker.yml     # 容器内运行：Compose 服务名
│           └── sql/schema.sql             # 8 张核心表 DDL
├── aiops-web/             # Vue 3 + Vite 前端（待开发）
├── aiops-mcp/             # Python MCP Server（待开发）
├── docker-data/           # Docker Compose 数据卷（已 gitignore）
├── deploy/                # 部署相关
├── docs/                  # 设计文档（含 database-review.md）
├── sql/                   # 额外 SQL 脚本
├── test-data/             # 测试数据
├── docker-compose.yml     # 仅 Redis / RabbitMQ（MySQL 不由 Compose 管理）
├── .env.example           # Compose 环境变量示例（无真实密码）
└── .gitignore
```

## 技术栈

| 组件 | 版本 | 说明 |
|---|---|---|
| 后端 | Spring Boot 2.7.18 | Java 8，JPA（ddl-auto=validate）+ Redis + AMQP + Actuator |
| 数据库 | MySQL 8.0.26 | **本机安装，不由 Docker Compose 管理**；utf8mb4，应用账户 aiops |
| 缓存 | Redis 7.2 | Docker 运行，AOF 持久化，仅绑定 127.0.0.1 |
| 消息队列 | RabbitMQ 3.13 | Docker 运行，management 插件，独立 aiops 用户，仅绑定 127.0.0.1 |
| 向量库 | Qdrant | 后续通过 Docker 启用 |
| LLM | 阿里云百炼 DashScope | OpenAI 兼容 API |
| 前端 | Vue 3 + Vite | 待开发 |
| MCP | Python 3.14 + mcp 2.3.0 | 待开发 |

## 本地开发启动步骤

### 1. 准备本地 MySQL

MySQL 使用本机已安装的实例（`127.0.0.1:3306`），Compose 不接管。首次需要建库、建应用账户、导入表结构：

```powershell
# 以本机 MySQL 管理员账号登录
mysql -u root -p

-- 建库
CREATE DATABASE IF NOT EXISTS aiops_assistant
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- 建应用账户（密码自行设定，不要使用弱口令，也不要提交到 Git）
CREATE USER IF NOT EXISTS 'aiops'@'%' IDENTIFIED BY '<你的密码>';
GRANT ALL PRIVILEGES ON aiops_assistant.* TO 'aiops'@'%';
FLUSH PRIVILEGES;
```

导入 8 张核心表：

```powershell
cd G:\projects\code\aiops-assistant
mysql -u aiops -p aiops_assistant < aiops-server/src/main/resources/sql/schema.sql

# 校验
mysql -u aiops -p -e "USE aiops_assistant; SHOW TABLES;"
```

> 脚本使用 `CREATE TABLE IF NOT EXISTS`，可重复执行；`ddl-auto=validate` 只校验结构，不会自动改表或删表。

### 2. 启动 Docker 中间件（Redis / RabbitMQ）

要求：Docker Desktop 已运行、WSL2 已启用。密码必须通过环境变量提供，仓库内没有默认弱口令。

```powershell
cd G:\projects\code\aiops-assistant

# 方式 A：使用 .env（推荐，一次性配置）
Copy-Item .env.example .env      # 然后编辑 .env 填写 RABBITMQ_PASSWORD
docker compose up -d

# 方式 B：当前会话注入
$env:RABBITMQ_PASSWORD = "<你的MQ密码>"
docker compose up -d

# 查看状态与日志
docker compose ps
docker compose logs -f rabbitmq
```

- Redis：`127.0.0.1:6379`，无密码，数据在 `./docker-data/redis`
- RabbitMQ：`127.0.0.1:5672`（AMQP）、`127.0.0.1:15672`（管理台），用户 `aiops`，数据在 `./docker-data/rabbitmq`

### 3. 配置应用环境变量

应用不会读取 `.env`，本地启动前需在 shell 中注入（密码不落盘）：

```powershell
$env:MYSQL_PASSWORD   = "<你的MySQL密码>"
$env:RABBITMQ_PASSWORD = "<你的MQ密码>"
# 可选：阿里云百炼
$env:DASHSCOPE_API_KEY = "sk-xxxxxxxxxxxxxxxx"
$env:DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
$env:DASHSCOPE_MODEL = "qwen-plus"
```

> ⚠️ **禁止**把密码 / API Key 写入任何源码或配置文件。所有配置项均为 `${VAR:默认值}` 占位符，未设置时为空值。

### 4. 启动后端（默认 local Profile）

```powershell
cd G:\projects\code\aiops-assistant\aiops-server
mvn spring-boot:run
```

打包运行：

```powershell
mvn clean package -DskipTests
java -jar target\aiops-server.jar
```

### 5. 健康检查

```powershell
curl.exe http://127.0.0.1:8080/api/health
# {"code":200,"message":"OK","data":{"status":"UP","service":"aiops-server","time":"..."}}

curl.exe http://127.0.0.1:8080/actuator/health
# 仅返回 {"status":"UP"}，不包含详情与环境信息
```

## Profile 切换说明

| Profile | 适用场景 | MySQL | Redis / RabbitMQ |
|---|---|---|---|
| `local`（默认） | Windows 本机 `mvn spring-boot:run` | `127.0.0.1:3306` | `127.0.0.1:6379` / `127.0.0.1:5672` |
| `docker` | 后端自身也运行在容器内 | `host.docker.internal:3306`（访问宿主机 MySQL） | `redis:6379` / `rabbitmq:5672`（Compose 服务名） |

切换方式：

```powershell
# 方式 A：环境变量
$env:SPRING_PROFILES_ACTIVE = "docker"
java -jar target\aiops-server.jar

# 方式 B：启动参数
mvn spring-boot:run "-Dspring-boot.run.profiles=docker"
# 或
java -jar target\aiops-server.jar --spring.profiles.active=docker
```

所有 profile 的 host / port / 账号 / 密码都可用环境变量覆盖（见下表）。

## 可用端口

| 服务 | 端口 | 绑定 | 管理方式 |
|---|---|---|---|
| aiops-server | 8080 | 0.0.0.0 | 本机进程 |
| MySQL | 3306 | 0.0.0.0 | **本机安装，非 Compose** |
| Redis | 6379 | 127.0.0.1 | Compose |
| RabbitMQ AMQP | 5672 | 127.0.0.1 | Compose |
| RabbitMQ 管理台 | 15672 | 127.0.0.1 | Compose |

## 环境变量

| 变量 | local 默认 | docker 默认 | 说明 |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | local | docker（需显式指定） | 激活的 profile |
| `MYSQL_HOST` | 127.0.0.1 | host.docker.internal | MySQL 主机 |
| `MYSQL_PORT` | 3306 | 3306 | MySQL 端口 |
| `MYSQL_DB` | aiops_assistant | aiops_assistant | 数据库名 |
| `MYSQL_USER` | aiops | aiops | 应用账户 |
| `MYSQL_PASSWORD` | （空，必填） | （空，必填） | 应用账户密码 |
| `REDIS_HOST` | 127.0.0.1 | redis | Redis 主机 |
| `REDIS_PORT` | 6379 | 6379 | Redis 端口 |
| `REDIS_PASSWORD` | （空） | （空） | Redis 密码，未启用认证时留空 |
| `RABBITMQ_HOST` | 127.0.0.1 | rabbitmq | RabbitMQ 主机 |
| `RABBITMQ_PORT` | 5672 | 5672 | AMQP 端口 |
| `RABBITMQ_USER` | aiops | aiops | RabbitMQ 用户 |
| `RABBITMQ_PASSWORD` | （空，必填） | （空，必填） | RabbitMQ 密码；Compose 侧同样必填 |
| `DASHSCOPE_API_KEY` | （空） | （空） | 阿里云百炼 API Key |
| `DASHSCOPE_BASE_URL` | DashScope 兼容端点 | 同左 | OpenAI 兼容 API Base URL |
| `DASHSCOPE_MODEL` | qwen-plus | qwen-plus | 默认模型 |

Compose 侧变量（写入 `.env` 或当前 shell）：

| 变量 | 说明 |
|---|---|
| `RABBITMQ_USER` | RabbitMQ 用户，默认 aiops |
| `RABBITMQ_PASSWORD` | **必填**，缺失时 `docker compose up` 直接报错 |

## 数据库

核心表：`users`、`alerts`、`metrics`、`logs`、`tickets`、`knowledge_bases`、`models`、`tasks`。

设计不足与后续演进（`knowledge_chunks` / `incident` / `ai_session`）见 [docs/database-review.md](docs/database-review.md)。

> 禁止使用 `docker compose down -v` 清理数据；MySQL 数据在本机实例中，与 Compose 无关。

## 开发规范

- Java 版本固定 **1.8**，不升级。
- 编码统一 **UTF-8**（`pom.xml` / `application*.yml` / MySQL / JSON）。
- 密码、API Key、Token **一律环境变量管理**，配置文件只允许 `${VAR:默认值}` 空占位符。
- MySQL 使用本机实例，不纳入 Compose；Redis / RabbitMQ 由 Compose 管理且只绑定 `127.0.0.1`。
- 依赖版本由 Spring Boot 2.7.18 BOM 管理；MySQL 驱动坐标用 `com.mysql:mysql-connector-j`。
- JPA 仓库层使用 **`repository`** 包（Spring Data JPA），不引入 MyBatis / MyBatis-Plus。
- `ddl-auto=validate`：启动只校验结构，不自动改表 / 删表；结构变更走 `schema.sql` + 手工执行。
- Actuator 只暴露 `health`、`info`，`show-details=never`；不得为展示健康状态而打开 `env` 等端点。
- `/api/health` 只反映应用自身存活，不混入中间件连接状态。

## 下一阶段

- [x] 阶段 1 — 环境检查与骨架
- [x] 阶段 2 — 后端骨架 + 中间件编排 + 核心 DDL
- [x] 阶段 2.5 — 配置修复：Profile 拆分、Compose 精简、Actuator 收敛
- [ ] 阶段 3 — 用户/认证 + 基础 CRUD（users/alerts/tickets）
- [ ] 阶段 4 — AI 集成：阿里云百炼 + 知识库检索
- [ ] 阶段 5 — Vue 3 前端骨架 + 页面
- [ ] 阶段 6 — Python MCP Server
- [ ] 阶段 7 — Docker 化后端 + 全链路 Compose + CI/CD
