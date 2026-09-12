-- 星河回忆 数据库初始化脚本
-- 执行：mysql -u root -p < schema.sql
CREATE DATABASE IF NOT EXISTS sweet_memory
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE sweet_memory;

CREATE TABLE IF NOT EXISTS photo (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  url           VARCHAR(255)    NOT NULL COMMENT '原图相对路径 /uploads/...',
  thumb         VARCHAR(255)    NOT NULL COMMENT '缩略图（一期同 url）',
  taken_at      DATE            NOT NULL COMMENT '拍摄日期（EXIF 或手动）',
  note          VARCHAR(60)     NOT NULL DEFAULT '' COMMENT '文字留言（≤20字）',
  audio_url     VARCHAR(255)    NULL COMMENT '语音留言文件',
  audio_duration DOUBLE         NULL COMMENT '语音时长（秒）',
  featured      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否首页展示',
  deleted       TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否在回收站',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_deleted_taken (deleted, taken_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='照片';
