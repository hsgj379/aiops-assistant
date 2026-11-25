# 数据库设计评审与演进规划

> 范围：`aiops-server/src/main/resources/sql/schema.sql` 中的 8 张表。
> 原则：本阶段只记录问题与规划，不修改既有表结构，不删除任何字段或数据。

## 一、现存 8 张表

| 表 | 定位 |
|---|---|
| `users` | 用户与角色 |
| `alerts` | 告警事件 |
| `metrics` | 指标采样 |
| `logs` | 结构化日志 |
| `tickets` | 工单，关联 users / alerts |
| `knowledge_bases` | 知识库文档主体 |
| `models` | LLM / Embedding 模型配置 |
| `tasks` | AI 任务与后台作业 |

## 二、已知不足

1. **知识库缺少分块维度**
   `knowledge_bases` 以整篇文档为一行，`content` 为 MEDIUMTEXT。RAG 需要按 chunk 检索，当前没有 chunk 表，无法记录分块顺序、token 数、向量 ID 与检索命中情况。

2. **向量 ID 与业务表的耦合过弱**
   `embedding_id` / `embedding_qdrant_id` 是自由字符串，没有唯一约束，也没有与 chunk 的对应关系。文档更新后旧向量无法可靠清理，容易产生脏向量。

3. **缺少会话与多轮上下文**
   只有 `tasks`，没有会话概念。无法表达"一次对话包含多轮问答"、上下文裁剪、消息角色（user/assistant/system）等，前端无法做会话列表。

4. **告警与事件未分层**
   `alerts` 同时承担"原始告警"与"事件聚合"两种语义。同一故障重复触发会产生大量同质记录，缺少去重键与聚合组概念。

5. **指标表无法承受真实数据量**
   `metrics` 单表按行存储，缺少时间分区与降采样策略。生产量级下（百万级/天）查询与清理都会成为瓶颈，后续应转向专用时序库，或至少按月分区 + TTL 归档。

6. **日志表容量无治理**
   `logs` 存 `MEDIUMTEXT` 堆栈且无分区、无归档策略，长期会拖垮实例。检索维度也只有 trace_id / service / level，缺少全文索引。

7. **JSON 字段缺少结构约束**
   `labels_json`、`tags_json`、`config_json`、`input_json`、`output_json` 均为裸 JSON，MySQL 8 未使用生成列索引，复杂条件查询会全表扫描。

8. **工单缺少流转记录**
   `tickets` 只有当前状态，没有状态变更历史（谁在何时改了什么），无法审计 SLA 与处理时长。

9. **缺少软删除与审计字段一致性**
   仅 `users.status` 表达禁用；其余表无 `deleted` 标记，删除即物理删除。也缺少统一的 `created_by / updated_by`。

10. **枚举以字符串裸存**
    `severity`、`status`、`type`、`priority` 等均为 VARCHAR，无字典表或 CHECK 约束，容易出现拼写漂移（`FIRING` / `firing`）。

## 三、后续演进规划（暂不实现）

### 1. `knowledge_chunks`

用途：支撑 RAG 的检索单元。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT PK | 主键 |
| `knowledge_id` | BIGINT | 关联 `knowledge_bases.id` |
| `chunk_index` | INT | 文档内顺序，从 0 开始 |
| `content` | TEXT | 分块正文 |
| `token_count` | INT | 估算 token 数 |
| `embedding_model_id` | BIGINT | 关联 `models.id` |
| `vector_id` | VARCHAR(64) | Qdrant point id，需唯一索引 |
| `vector_status` | VARCHAR(16) | PENDING / INDEXED / FAILED |
| `created_at` / `updated_at` | DATETIME | 审计 |

要点：`(knowledge_id, chunk_index)` 唯一；`vector_id` 唯一；文档更新时按 `knowledge_id` 批量重建并清理旧 point。

### 2. `incident`（事件聚合）

用途：把多条告警聚合为一次故障事件，作为工单与复盘的锚点。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT PK | 主键 |
| `title` | VARCHAR(256) | 事件标题 |
| `severity` | VARCHAR(16) | 事件级别 |
| `status` | VARCHAR(32) | OPEN / MITIGATING / RESOLVED / CLOSED |
| `dedup_key` | VARCHAR(128) | 去重键，如 `service+metric` |
| `alert_count` | INT | 聚合的告警数 |
| `started_at` / `resolved_at` | DATETIME | 起止时间 |
| `owner_id` | BIGINT | 负责人，关联 users |
| `summary` | TEXT | AI 或人工生成的摘要 |

要点：`dedup_key + status` 建索引；`alerts` 后续可增 `incident_id` 外键，与既有字段兼容。

### 3. `ai_session`（会话与消息）

用途：承载多轮对话，区分会话元信息与消息明细。

`ai_session`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT PK | 主键 |
| `session_key` | VARCHAR(64) | 对外暴露的会话标识，唯一 |
| `title` | VARCHAR(256) | 会话标题（可由首轮问题生成） |
| `user_id` | BIGINT | 关联 users |
| `model_id` | BIGINT | 关联 models |
| `message_count` | INT | 消息数 |
| `last_active_at` | DATETIME | 最近活跃时间 |
| `created_at` | DATETIME | 创建时间 |

`ai_message`（建议同批引入）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT PK | 主键 |
| `session_id` | BIGINT | 关联 ai_session |
| `role` | VARCHAR(16) | user / assistant / system |
| `content` | MEDIUMTEXT | 消息正文 |
| `token_count` | INT | token 消耗 |
| `latency_ms` | INT | 响应耗时 |
| `task_id` | BIGINT | 关联 tasks，便于追踪异步任务 |
| `created_at` | DATETIME | 创建时间 |

要点：`session_id + created_at` 建索引；消息量大时可按会话归档。

## 四、落地顺序建议

1. 先补 `ai_session` / `ai_message`（对话功能上线前置）。
2. 再补 `knowledge_chunks`（RAG 检索前置），同时统一向量 ID 的唯一约束。
3. `incident` 与 `tickets.incident_id` 随后引入，保证与现有告警字段兼容。
4. `metrics` / `logs` 的容量治理放到可观测性阶段，按需引入分区或外部存储。
