-- ============================================================
-- 心理健康助手（金凤）数据库初始化脚本
-- 适用数据库：MySQL 8.0+
-- 执行方式：mysql -uroot -p < sql/init.sql
-- ============================================================

-- 强制客户端使用 utf8mb4，避免中文写入乱码（Docker 里 mysql 客户端默认可能是 latin1）
SET NAMES utf8mb4;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `mental_health_assistant`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `mental_health_assistant`;

-- ------------------------------------------------------------
-- 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`   VARCHAR(50)  NOT NULL                COMMENT '用户名',
    `email`      VARCHAR(100) NOT NULL                COMMENT '邮箱',
    `phone`      VARCHAR(20)           DEFAULT NULL   COMMENT '手机号',
    `password`   VARCHAR(255) NOT NULL                COMMENT '密码（BCrypt 加密）',
    `nickname`   VARCHAR(50)           DEFAULT NULL   COMMENT '昵称',
    `avatar`     VARCHAR(255)          DEFAULT NULL   COMMENT '头像地址',
    `gender`     TINYINT               DEFAULT NULL   COMMENT '性别：1-男 2-女',
    `birthday`   DATE                  DEFAULT NULL   COMMENT '生日',
    `user_type`  TINYINT      NOT NULL DEFAULT 1      COMMENT '用户类型：1-普通用户 2-管理员',
    `status`     TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：0-禁用 1-正常',
    `created_at` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

-- ------------------------------------------------------------
-- 咨询会话表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `consultation_session`;
CREATE TABLE `consultation_session` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_id`               BIGINT       NOT NULL                COMMENT '用户ID',
    `session_title`         VARCHAR(200)          DEFAULT NULL   COMMENT '会话标题',
    `started_at`            DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    `last_emotion_analysis` TEXT                   DEFAULT NULL  COMMENT '最近一次情绪分析结果(JSON)',
    `last_emotion_updated_at` DATETIME             DEFAULT NULL  COMMENT '情绪分析更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_session_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '咨询会话表';

-- ------------------------------------------------------------
-- 咨询消息表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `consultation_message`;
CREATE TABLE `consultation_message` (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id`   BIGINT      NOT NULL                COMMENT '会话ID',
    `sender_type`  TINYINT     NOT NULL                COMMENT '发送者类型：1-用户 2-AI助手',
    `message_type` TINYINT     NOT NULL DEFAULT 1      COMMENT '消息类型：1-文本',
    `content`      TEXT        NOT NULL                COMMENT '消息内容',
    `emotion_tag`  VARCHAR(50)          DEFAULT NULL   COMMENT '情绪标签',
    `ai_model`     VARCHAR(50)          DEFAULT NULL   COMMENT '使用的AI模型',
    `created_at`   DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    CONSTRAINT `fk_message_session` FOREIGN KEY (`session_id`) REFERENCES `consultation_session` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '咨询消息表';

-- ============================================================
-- 可选：初始化一个测试账号
-- 用户名：admin  密码：123456（BCrypt 加密）
-- 如需自定义，可先注册接口 POST /api/user/add 创建用户
-- ============================================================
INSERT INTO `user` (`username`, `email`, `password`, `nickname`, `user_type`, `status`)
VALUES ('admin', 'admin@example.com', '$2a$10$kAri1DE/Ck7hNITFcEkupuHEUpXXmkffRm2pi/HOnUoWlHPkGyJle', '管理员', 2, 1);

-- ------------------------------------------------------------
-- 知识分类表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `knowledge_category`;
CREATE TABLE `knowledge_category` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `category_name` VARCHAR(50) NOT NULL                COMMENT '分类名称',
    `sort_order`    INT         NOT NULL DEFAULT 0      COMMENT '排序',
    `created_at`    DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识分类表';

-- ------------------------------------------------------------
-- 知识文章表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `knowledge_article`;
CREATE TABLE `knowledge_article` (
    `id`           VARCHAR(64)  NOT NULL                COMMENT '文章ID(UUID)',
    `title`        VARCHAR(200) NOT NULL                COMMENT '标题',
    `category_id`  BIGINT                DEFAULT NULL    COMMENT '分类ID',
    `summary`      TEXT                  DEFAULT NULL   COMMENT '摘要',
    `tags`         VARCHAR(500)          DEFAULT NULL   COMMENT '标签(逗号分隔)',
    `cover_image`  VARCHAR(255)          DEFAULT NULL   COMMENT '封面图片路径',
    `content`      MEDIUMTEXT            DEFAULT NULL   COMMENT '正文内容(HTML)',
    `author_id`    BIGINT                DEFAULT NULL   COMMENT '作者ID',
    `author_name`  VARCHAR(50)           DEFAULT NULL   COMMENT '作者名称',
    `read_count`   INT          NOT NULL DEFAULT 0      COMMENT '阅读量',
    `status`       TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0-草稿 1-已发布 2-已下线',
    `created_at`   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `published_at` DATETIME              DEFAULT NULL   COMMENT '发布时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识文章表';

-- ------------------------------------------------------------
-- 情绪日记表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `emotion_diary`;
CREATE TABLE `emotion_diary` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`             BIGINT       NOT NULL                COMMENT '用户ID',
    `diary_date`          DATE                  DEFAULT NULL   COMMENT '记录日期',
    `mood_score`          INT                   DEFAULT NULL   COMMENT '情绪评分 1-10',
    `dominant_emotion`    VARCHAR(20)           DEFAULT NULL   COMMENT '主要情绪',
    `emotion_triggers`    VARCHAR(1000)         DEFAULT NULL   COMMENT '情绪触发因素',
    `diary_content`       TEXT                  DEFAULT NULL   COMMENT '日记内容',
    `sleep_quality`       INT                   DEFAULT NULL   COMMENT '睡眠质量 1-5',
    `stress_level`        INT                   DEFAULT NULL   COMMENT '压力水平 1-5',
    `ai_emotion_analysis` TEXT                  DEFAULT NULL   COMMENT 'AI情绪分析结果(JSON)',
    `created_at`          DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`          DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_diary_user_id` (`user_id`),
    CONSTRAINT `fk_diary_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '情绪日记表';

-- ------------------------------------------------------------
-- 文件表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `business_type`  VARCHAR(50)           DEFAULT NULL   COMMENT '业务类型',
    `business_id`    VARCHAR(64)           DEFAULT NULL   COMMENT '业务ID',
    `business_field` VARCHAR(50)           DEFAULT NULL   COMMENT '业务字段',
    `file_name`      VARCHAR(255)          DEFAULT NULL   COMMENT '原始文件名',
    `file_path`      VARCHAR(500)          DEFAULT NULL   COMMENT '访问路径',
    `file_size`      BIGINT                DEFAULT NULL   COMMENT '文件大小',
    `created_at`     DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_business` (`business_type`, `business_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文件表';

-- 初始化几个知识分类
INSERT INTO `knowledge_category` (`category_name`, `sort_order`) VALUES
    ('情绪管理', 1), ('压力应对', 2), ('自我成长', 3), ('人际关系', 4), ('睡眠健康', 5);

-- ------------------------------------------------------------
-- 安慰邮件发送记录表（用于限制发送频率）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `emotion_mail_log`;
CREATE TABLE `emotion_mail_log` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`        BIGINT       NOT NULL                COMMENT '接收用户ID',
    `session_id`     BIGINT                DEFAULT NULL   COMMENT '关联会话ID',
    `email`          VARCHAR(100)          DEFAULT NULL   COMMENT '接收邮箱',
    `primary_emotion` VARCHAR(20)          DEFAULT NULL   COMMENT '触发时的主要情绪',
    `emotion_score`  INT                   DEFAULT NULL   COMMENT '触发时的情绪评分',
    `risk_level`     INT                   DEFAULT NULL   COMMENT '触发时的风险等级',
    `sent_at`        DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_mail_user_time` (`user_id`, `sent_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '安慰邮件发送记录表';


