-- =====================================================================
-- AI DevOps Assistant
-- AIOps 核心表初始化 DDL（MySQL 8 / InnoDB / utf8mb4）
--
-- 运行前提：Docker Compose 会自动执行本脚本（数据卷首次为空时）
--   docker compose up -d            首次启动自动建库建表
--   docker compose down -v          连同数据卷清空后再次 up 会重新执行
--
-- Hibernate 配置 ddl-auto=validate —— 启动时只校验 schema，
--   不会自动修改/删除已有表。如需重建：
--     方法 A：docker compose down -v && docker compose up -d
--     方法 B：手动 DROP 单表后重新执行本脚本
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `aiops_assistant`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `aiops_assistant`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 1. 用户表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`        VARCHAR(64)  NOT NULL                COMMENT '登录用户名',
    `password_hash`   VARCHAR(128) NOT NULL                COMMENT 'BCrypt 哈希',
    `email`           VARCHAR(128) DEFAULT NULL             COMMENT '邮箱',
    `phone`           VARCHAR(32)  DEFAULT NULL             COMMENT '手机号',
    `role`            VARCHAR(32)  NOT NULL DEFAULT 'USER'  COMMENT '角色 ADMIN/USER',
    `status`          TINYINT      NOT NULL DEFAULT 1       COMMENT '1 启用 0 禁用',
    `avatar`          VARCHAR(256) DEFAULT NULL             COMMENT '头像 URL',
    `last_login_at`   DATETIME     DEFAULT NULL             COMMENT '最后登录时间',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_email` (`email`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ---------------------------------------------------------------------
-- 2. 告警表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `alerts` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `rule_id`          VARCHAR(64)   DEFAULT NULL            COMMENT '触发的规则 ID',
    `name`             VARCHAR(256)  NOT NULL                COMMENT '告警名称',
    `severity`         VARCHAR(16)   NOT NULL DEFAULT 'WARN' COMMENT 'CRITICAL/MAJOR/MINOR/WARN/INFO',
    `status`           VARCHAR(16)   NOT NULL DEFAULT 'FIRING' COMMENT 'FIRING/ACKED/RESOLVED/CLOSED',
    `source`           VARCHAR(64)   DEFAULT NULL            COMMENT '告警来源（Prometheus/自建等）',
    `target`           VARCHAR(256)  DEFAULT NULL            COMMENT '告警目标（host/service）',
    `message`          VARCHAR(1024) DEFAULT NULL            COMMENT '告警消息',
    `trace_id`         VARCHAR(64)   DEFAULT NULL            COMMENT '分布式追踪 ID',
    `labels_json`      JSON          DEFAULT NULL            COMMENT 'Prometheus labels 或自定义扩展',
    `started_at`       DATETIME      DEFAULT NULL            COMMENT '首次触发时间',
    `fired_at`         DATETIME      DEFAULT NULL            COMMENT '最近一次触发时间',
    `recovered_at`     DATETIME      DEFAULT NULL            COMMENT '恢复时间',
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_severity_status` (`severity`, `status`),
    KEY `idx_target` (`target`(128)),
    KEY `idx_trace_id` (`trace_id`),
    KEY `idx_started_at` (`started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警表';

-- ---------------------------------------------------------------------
-- 3. 指标表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `metrics` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(128)  NOT NULL                COMMENT '指标名',
    `value`       DOUBLE        NOT NULL                COMMENT '值',
    `unit`        VARCHAR(16)   DEFAULT NULL            COMMENT '单位',
    `tags_json`   JSON          DEFAULT NULL            COMMENT '维度标签（host/service/endpoint/...）',
    `timestamp`   DATETIME      NOT NULL                COMMENT '采样时间',
    `source`      VARCHAR(64)   DEFAULT NULL            COMMENT '采集来源',
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_name_ts` (`name`, `timestamp`),
    KEY `idx_ts` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标表';

-- ---------------------------------------------------------------------
-- 4. 日志表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `logs` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `trace_id`         VARCHAR(64)   DEFAULT NULL            COMMENT '分布式追踪 ID',
    `span_id`          VARCHAR(32)   DEFAULT NULL            COMMENT 'Span ID',
    `level`            VARCHAR(16)   NOT NULL                COMMENT 'TRACE/DEBUG/INFO/WARN/ERROR/FATAL',
    `service`          VARCHAR(64)   DEFAULT NULL            COMMENT '服务名',
    `host`             VARCHAR(128)  DEFAULT NULL            COMMENT '主机名/IP',
    `thread_name`      VARCHAR(128)  DEFAULT NULL,
    `logger`           VARCHAR(256)  DEFAULT NULL,
    `message`          TEXT          NOT NULL                COMMENT '日志消息',
    `exception_stack`  MEDIUMTEXT    DEFAULT NULL            COMMENT '异常堆栈',
    `timestamp`        DATETIME      NOT NULL                COMMENT '日志产生时间',
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_trace_id` (`trace_id`),
    KEY `idx_service_ts` (`service`, `timestamp`),
    KEY `idx_level_ts` (`level`, `timestamp`),
    KEY `idx_ts` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日志表';

-- ---------------------------------------------------------------------
-- 5. 工单表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tickets` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `title`            VARCHAR(256)  NOT NULL,
    `description`      TEXT          DEFAULT NULL,
    `type`             VARCHAR(32)   NOT NULL DEFAULT 'INCIDENT' COMMENT 'INCIDENT/PROBLEM/CHANGE/REQUEST',
    `priority`         VARCHAR(16)   NOT NULL DEFAULT 'P3'      COMMENT 'P0-P3',
    `status`           VARCHAR(32)   NOT NULL DEFAULT 'OPEN'    COMMENT 'OPEN/IN_PROGRESS/PENDING/RESOLVED/CLOSED',
    `creator_id`       BIGINT        DEFAULT NULL,
    `assignee_id`      BIGINT        DEFAULT NULL,
    `related_alert_id` BIGINT        DEFAULT NULL,
    `channel`          VARCHAR(32)   DEFAULT NULL              COMMENT '来源渠道 LARK/WEB/CLI/AGENT',
    `tags_json`        JSON          DEFAULT NULL,
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `resolved_at`      DATETIME      DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_status_priority` (`status`, `priority`),
    KEY `idx_creator` (`creator_id`),
    KEY `idx_assignee` (`assignee_id`),
    KEY `idx_related_alert` (`related_alert_id`),
    CONSTRAINT `fk_tickets_creator`  FOREIGN KEY (`creator_id`)       REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_tickets_assignee` FOREIGN KEY (`assignee_id`)      REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_tickets_alert`    FOREIGN KEY (`related_alert_id`) REFERENCES `alerts` (`id`)  ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单表';

-- ---------------------------------------------------------------------
-- 6. 知识库表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `knowledge_bases` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `title`          VARCHAR(256)  NOT NULL,
    `category`       VARCHAR(64)   DEFAULT NULL            COMMENT '分类：runbook/postmortem/faq/...',
    `content`        MEDIUMTEXT    NOT NULL                COMMENT '正文（支持 Markdown）',
    `content_type`   VARCHAR(16)   NOT NULL DEFAULT 'MARKDOWN',
    `source`         VARCHAR(64)   DEFAULT NULL            COMMENT '来源',
    `source_url`     VARCHAR(512)  DEFAULT NULL            COMMENT '原始链接',
    `tags_json`      JSON          DEFAULT NULL,
    `embedding_id`   VARCHAR(128)  DEFAULT NULL            COMMENT '向量数据库文档 ID',
    `embedding_qdrant_id` VARCHAR(64) DEFAULT NULL        COMMENT 'Qdrant point id',
    `creator_id`     BIGINT        DEFAULT NULL,
    `is_public`      TINYINT       NOT NULL DEFAULT 1,
    `view_count`     INT           NOT NULL DEFAULT 0,
    `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`),
    KEY `idx_embedding_id` (`embedding_id`),
    KEY `idx_embedding_qdrant` (`embedding_qdrant_id`),
    KEY `idx_creator` (`creator_id`),
    CONSTRAINT `fk_knowledge_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- ---------------------------------------------------------------------
-- 7. 模型配置表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `models` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `name`         VARCHAR(128) NOT NULL                COMMENT '显示名',
    `type`         VARCHAR(32)  NOT NULL DEFAULT 'LLM'   COMMENT 'LLM/EMBEDDING/RERANK',
    `provider`     VARCHAR(32)  NOT NULL DEFAULT 'ALIYUN_DASHSCOPE' COMMENT 'ALIYUN_DASHSCOPE/OPENAI/ANTHROPIC/SELF_HOSTED',
    `model_id`     VARCHAR(128) NOT NULL                COMMENT '平台上的真实模型 ID',
    `endpoint`     VARCHAR(256) DEFAULT NULL            COMMENT 'API Base URL（OpenAI 兼容）',
    `api_key_ref`  VARCHAR(256) DEFAULT NULL            COMMENT '引用的环境变量名（如 DASHSCOPE_API_KEY）',
    `config_json`  JSON         DEFAULT NULL            COMMENT '温度/top_p/最大 token 等',
    `status`       TINYINT      NOT NULL DEFAULT 1       COMMENT '1 启用 0 禁用',
    `creator_id`   BIGINT       DEFAULT NULL,
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_type_provider` (`type`, `provider`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_models_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型配置表';

-- ---------------------------------------------------------------------
-- 8. 任务表（AI 执行任务 + 后台作业）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tasks` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT,
    `name`          VARCHAR(256)  NOT NULL,
    `task_type`     VARCHAR(32)   NOT NULL               COMMENT 'CHAT/SUMMARIZE/RUNBOOK_AID/ETL/VECTORIZE/...',
    `status`        VARCHAR(32)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED/CANCELLED',
    `creator_id`    BIGINT        DEFAULT NULL,
    `related_id`    BIGINT        DEFAULT NULL            COMMENT '关联业务对象 ID（工单/告警/知识库等）',
    `related_type`  VARCHAR(32)   DEFAULT NULL,
    `model_id`      BIGINT        DEFAULT NULL            COMMENT '使用的 LLM 模型',
    `input_json`    JSON          DEFAULT NULL            COMMENT '输入参数/提示词',
    `output_json`   JSON          DEFAULT NULL            COMMENT '输出结果',
    `error_message` MEDIUMTEXT    DEFAULT NULL,
    `queue_name`    VARCHAR(64)   DEFAULT NULL            COMMENT 'RabbitMQ 队列名',
    `started_at`    DATETIME      DEFAULT NULL,
    `ended_at`      DATETIME      DEFAULT NULL,
    `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status_type` (`status`, `task_type`),
    KEY `idx_creator` (`creator_id`),
    KEY `idx_related` (`related_type`, `related_id`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_tasks_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_tasks_model`   FOREIGN KEY (`model_id`)   REFERENCES `models` (`id`)   ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 任务/后台作业表';

SET FOREIGN_KEY_CHECKS = 1;
