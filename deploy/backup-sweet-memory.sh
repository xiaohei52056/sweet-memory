#!/bin/bash

# ==============================================
# 星空记忆（sweet-memory）数据备份脚本
# 备份内容：照片目录 + env 配置 + sweet_memory 库
#         （数据库全量已由 /root/backup-mysql.sh 覆盖，
#           这里单独导出一份便于独立迁移恢复）
# 保留最近 7 天，日志写入 /var/log/sweet_memory_backup.log
# ==============================================

APP_DIR="/opt/sweet-memory"
ENV_FILE="$APP_DIR/sweet-memory.env"
BACKUP_DIR="/backups/sweet-memory"
DATE=$(date +%F_%H-%M-%S)
LOG_FILE="/var/log/sweet_memory_backup.log"

mkdir -p "$BACKUP_DIR"
log() { echo "[$(date '+%F %T')] $1" >> "$LOG_FILE"; }

log "开始备份 sweet-memory..."

# 从 env 文件读取数据库账号与上传目录（不在脚本中硬编码密码）
DB_USER=$(grep '^DB_USER=' "$ENV_FILE" | cut -d= -f2-)
DB_PASSWORD=$(grep '^DB_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)
UPLOAD_DIR=$(grep '^APP_UPLOAD_DIR=' "$ENV_FILE" | cut -d= -f2-)

# --- 1. 数据库导出（宿主机无 mysqldump，通过 blog-mysql 容器执行） ---
docker exec blog-mysql mysqldump \
    -u"$DB_USER" -p"$DB_PASSWORD" \
    --single-transaction --quick --no-tablespaces sweet_memory \
    > "$BACKUP_DIR/db_$DATE.sql" 2>> "$LOG_FILE"
if [ $? -ne 0 ]; then
    log "数据库导出失败！"
    rm -f "$BACKUP_DIR/db_$DATE.sql"
    exit 1
fi

# --- 2. 照片目录打包（原图不压缩，用 tar 仅归档） ---
tar czf "$BACKUP_DIR/uploads_$DATE.tar.gz" -C "$(dirname "$UPLOAD_DIR")" "$(basename "$UPLOAD_DIR")" 2>> "$LOG_FILE"
if [ $? -ne 0 ]; then
    log "照片打包失败！"
    exit 1
fi

# --- 3. env 配置（含管理员哈希/密码，权限收紧） ---
cp "$ENV_FILE" "$BACKUP_DIR/env_$DATE.bak"
chmod 600 "$BACKUP_DIR/env_$DATE.bak"

# --- 4. 清理 7 天前的旧备份 ---
find "$BACKUP_DIR" -name 'db_*.sql' -mtime +7 -delete
find "$BACKUP_DIR" -name 'uploads_*.tar.gz' -mtime +7 -delete
find "$BACKUP_DIR" -name 'env_*.bak' -mtime +7 -delete

log "备份完成：db_$DATE.sql / uploads_$DATE.tar.gz / env_$DATE.bak"
log "当前占用：$(du -sh "$BACKUP_DIR" | cut -f1)"
echo "-----------------------------------" >> "$LOG_FILE"
