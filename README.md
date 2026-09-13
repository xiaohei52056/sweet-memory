# 星河回忆 · Sweet Memory

> 把每一次相遇，折叠成星；当夜空亮起，记忆便有了形状。

一个属于两个人的照片回忆网站：深空星野背景，照片以「明信片」形式呈现，正面是照片、翻到背面是 20 字留言与语音。支持时间线胶卷、后台管理（上传 / 搜索 / 分页 / 首页精选 / 回收站）。

---

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [目录结构](#目录结构)
- [快速开始（本地）](#快速开始本地)
- [API 一览](#api-一览)
- [字体子集工具](#字体子集工具)
- [部署（阿里云 / Nginx）](#部署阿里云--nginx)
- [安全注意事项](#安全注意事项)
- [开发约定](#开发约定)

---

## 功能特性

**前台**
- **星空首页**：three.js 实时渲染星野，滚轮推进星场纵深；标题「星河回忆」逐字墨晕入场，金线分隔与三行诗句依次浮现；底部照片轮播条从左向右无限循环，悬停暂停放大。
- **时间线**：横向电影胶卷，按年份分组，支持拖拽 / 滚轮 / 触摸横移 + 惯性，顶部年份刻度一键跳转。
- **Lightbox**：点击任意照片看大图，轻触卡片翻转到背面查看文字留言与语音播放（仅显示秒数，无进度条）；键盘 ←/→ 切换、ESC 关闭，移动端左右滑动。

**后台**（`/admin`，需登录）
- 右下角悬浮「+」按钮批量上传照片，自动读取 EXIF 拍摄日期。
- 按留言模糊搜索 + 年份 / 月份筛选；网格一行五张，每页 40 张分页。
- 星形按钮标记「首页展示」，被标记照片自动排在最前并只在首页轮播条展示。
- 下载照片（带确认弹窗）；回收站软删除 / 恢复 / 彻底删除。

---

## 技术栈

| 层 | 选型 | 说明 |
|---|---|---|
| 前端 | Vue 3 + Vite + Pinia + Vue Router | 无重型 UI 库，后台为自研轻量组件 |
| 视觉 | three.js（星空）+ GSAP（入场动画） | |
| 后端 | Spring Boot 3（JDK 17）+ MyBatis | 标准 Controller / Service / Mapper 分层 |
| 数据库 | MySQL 8 | 单表 `photo`，复用现有 MySQL 实例 |
| 鉴权 | JWT（jjwt）+ bcrypt（spring-security-crypto） | 账号写死在配置，非数据库 |
| 存储 | 本地磁盘 `uploads/yyyy/MM/` | 原图不压缩，Nginx 直接托管 |

---

## 目录结构

```
sweet-memory/
├── frontend/                 # Vue3 前端
│   ├── src/
│   │   ├── api/index.js      # axios 封装 + JWT 注入 + 401 处理
│   │   ├── views/            # HomeView / TimelineView / AdminView
│   │   ├── components/       # StarfieldCanvas / PhotoStrip / Lightbox
│   │   ├── stores/           # Pinia：auth / photos / ui
│   │   └── styles/           # 全局样式 + 艺术字体
│   └── tools/gen-art-fonts.mjs   # 字体子集生成（见下文）
├── backend/                  # Spring Boot 后端
│   ├── src/main/java/com/sweetmemory/
│   │   ├── web/              # Controller + 拦截器
│   │   ├── service/          # 业务：认证 / JWT / 文件存储
│   │   ├── mapper/           # MyBatis Mapper 接口
│   │   ├── entity/           # Photo 实体
│   │   └── tool/HashGen.java # 生成管理员密码 bcrypt 哈希
│   ├── src/main/resources/
│   │   ├── application.yml       # 开发配置（含默认值）
│   │   ├── application-prod.yml  # 生产配置（全走环境变量）
│   │   └── mapper/PhotoMapper.xml
│   ├── schema.sql            # 建库建表脚本
│   └── smoke-test.ps1        # 接口冒烟测试
├── deploy/                   # 部署包
│   ├── nginx/sweet-memory.conf
│   ├── systemd/sweet-memory.service
│   └── sweet-memory.env.example
└── _archive/                 # 旧版代码归档（不纳入 git）
```

---

## 快速开始（本地）

### 环境要求
- **JDK 17+**、**Maven 3.6+**
- **Node.js 18+**
- **MySQL 8**（本地或远程）

### 1. 建库

```bash
mysql -u root -p < backend/schema.sql
```

创建 `sweet_memory` 库与 `photo` 表。建议建一个专用低权限账号：

```sql
CREATE USER 'sweet'@'localhost' IDENTIFIED BY '你的密码';
GRANT ALL PRIVILEGES ON sweet_memory.* TO 'sweet'@'localhost';
FLUSH PRIVILEGES;
```

### 2. 配置后端

编辑 [application.yml](backend/src/main/resources/application.yml)，把数据源账号密码改成上一步的：

```yaml
spring:
  datasource:
    username: ${DB_USER:sweet}
    password: ${DB_PASSWORD:你的密码}
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run        # 默认监听 8082
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev                # 默认监听 5173
```

Vite 已配置代理：`/api` 与 `/uploads` 转发到 `127.0.0.1:8082`，开发环境无需额外配置跨域。

打开 http://localhost:5173/ 即可访问，后台入口 http://localhost:5173/admin 。

### 5. 登录账号

默认管理员账号（star / river）的**用户名与密码不在此公开**。开发时请：
1. 查看 `backend/src/main/resources/application.yml` 的 `app.admins`；
2. 用 [HashGen](backend/src/main/java/com/sweetmemory/tool/HashGen.java) 生成你自己密码的 bcrypt 哈希并替换：

```bash
cd backend
mvn -q exec:java -Dexec.mainClass=com.sweetmemory.tool.HashGen -Dexec.args="你的新密码"
```

把输出的哈希填进 `password-hash` 字段即可。

---

## API 一览

所有接口前缀 `/api`。除 `login` 与「访客读照片列表」外，均需请求头 `Authorization: Bearer <token>`。

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | `/api/login` | 公开 | 登录，返回 `{username, nickname, token}` |
| GET | `/api/photos` | 公开 | 照片列表（不含回收站），供首页/时间线展示 |
| GET | `/api/photos?trashed=true` | 需登录 | 回收站列表 |
| POST | `/api/photos` | 需登录 | 批量上传，`multipart`：`files[]` + `metas`（JSON 数组，含 takenAt/note/featured） |
| PATCH | `/api/photos/{id}` | 需登录 | 更新留言 / 日期 / 首页标记 / 语音信息 |
| POST | `/api/photos/{id}/trash` | 需登录 | 移入回收站（软删） |
| POST | `/api/photos/{id}/restore` | 需登录 | 从回收站恢复 |
| DELETE | `/api/photos/{id}/final` | 需登录 | 彻底删除（连同磁盘文件） |
| POST | `/api/photos/{id}/audio` | 需登录 | 上传语音（预留给小程序端） |
| GET | `/uploads/**` | 公开 | 照片 / 语音静态文件 |

**Photo 字段**：`id, url, thumb, takenAt, note, audioUrl, audioDuration, featured, deleted, createdAt`

冒烟测试（后端已启动时）：

```powershell
cd backend
./smoke-test.ps1
```

---

## 字体子集工具

首页标题「星河回忆」用**霞鹜文楷**、诗句用**站酷文艺宋**。中文整包字体动辄 2MB+，因此用 [gen-art-fonts.mjs](frontend/tools/gen-art-fonts.mjs) 按实际用字裁剪成子集（标题每字约 0.8KB、诗句约 10KB），本地自托管，无 CDN 依赖。

依赖：`pip install fonttools brotli` + `npm install`（已含 `@fontsource/zcool-xiaowei`、`lxgw-wenkai-webfont`）。

**改动首页标题或诗句文案后，必须重跑：**

```bash
cd frontend
node tools/gen-art-fonts.mjs
```

否则会缺字（浏览器回退字体导致显示不一致）。

---

## 部署（阿里云 / Nginx）

部署包在 `deploy/`，目标：单台低内存服务器（2G），复用已有 MySQL 与 Nginx。

1. **打包后端**：`cd backend && mvn clean package -DskipTests` → `target/sweet-memory.jar`
2. **构建前端**：`cd frontend && npm run build` → `dist/`
3. **服务器目录**：约定 `/opt/sweet-memory/`，放 jar、`web/`（前端 dist）、`data/uploads/`、`logs/`
4. **环境变量**：复制 `deploy/sweet-memory.env.example` 为 `sweet-memory.env`，填入数据库密码、随机 JWT 密钥、管理员哈希，`chmod 600`
5. **systemd**：复制 `deploy/systemd/sweet-memory.service` 到 `/etc/systemd/system/`，`systemctl enable --now sweet-memory`（JVM 已限 `-Xmx384m`，低内存友好）
6. **Nginx**：把 `deploy/nginx/sweet-memory.conf` 三段并入现有 server 块（前端静态、`/api/` 反代 8082、`/uploads/` 直出磁盘）

> 详细逐条命令与排错，视服务器实际环境（尤其 MySQL 暴露方式）单独确认。

---

## 安全注意事项

- **切勿提交** `.env`、`application-prod.yml` 的真实值——`.gitignore` 已排除 `*.env`、`data/`、`target/`。
- 生产环境务必用**随机 JWT 密钥**（≥32 字节）和**独立数据库账号**，不要沿用开发默认值。
- 管理员密码以 **bcrypt 哈希**存储，仓库中不含明文；部署前请替换为你自己的账号与哈希。
- 上传限制：单文件 30MB、单请求 200MB（`application.yml` 的 `spring.servlet.multipart`），Nginx 侧 `client_max_body_size` 需同步放大。

---

## 开发约定

- **字体**：标题/诗句用思源宋与艺术字（子集 woff2 本地打包，勿用 Google Fonts CDN）；Windows 下不开 `-webkit-font-smoothing: antialiased`（会禁用 ClearType 导致中文发虚）；正文小字基准 13–15px。
- **中文子集**：勿用 Google Fonts 的 `text=` 端点裁剪——产物在浏览器中会渲染为方框（tofu）；统一用本地 `pyftsubset`。
- **EXIF**：读取拍摄日期用 `exifr`（不要用已停更、在 ESM 下崩溃的 `exif-js`），并保留 3 秒超时兜底，避免上传流程被卡死。
- **缩略图**：一期不生成，`thumb` 与 `url` 相同；照片量大时再引入服务端缩略图。

---

## 许可

私有项目，仅供个人使用。字体遵循各自开源协议：霞鹜文楷（SIL OFL 1.1）、站酷系列（Apache 2.0 / OFL）。
