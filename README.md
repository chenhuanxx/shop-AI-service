# java-admin

## 项目结构

- `java-backend/`：后端（Spring Boot + JPA + Security + JWT）
- `frontend-vue/`：前端（Vue3 + TS + Vite + Ant Design Vue）
- `app-web/`：前端（Vue3 + TS + Vite + Vant，可选 Mock）

## 运行环境

- JDK 17
- Maven 3.8+
- Node.js 18+（或 20+） + npm
- MySQL 8（或兼容版本）

## 开发启动流程（推荐）

### 1) 准备数据库

创建数据库与账号（示例）：

```sql
CREATE DATABASE `java-admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'java-admin'@'%' IDENTIFIED BY 'your-password';
GRANT ALL PRIVILEGES ON `java-admin`.* TO 'java-admin'@'%';
FLUSH PRIVILEGES;
```

### 2) 启动后端（java-backend）

后端默认端口：`8813`，可用环境变量覆盖（见下方示例）。

在 PowerShell 中设置环境变量（示例，按你的实际情况修改）：

```powershell
$env:SERVER_PORT="8813"
$env:MYSQL_HOST="127.0.0.1"
$env:MYSQL_PORT="3306"
$env:MYSQL_DATABASE="java-admin"
$env:MYSQL_USER="java-admin"
$env:MYSQL_PASSWORD="your-password"
$env:JWT_SECRET="dev-secret-change-me"
```

启动：

```powershell
cd java-backend
mvn spring-boot:run
```

健康检查：

- `GET http://localhost:8813/health` -> `{"status":"ok"}`

初始化账号（首次启动会自动写入数据库）：

- 管理员：`admin / admin123`
- 普通用户：`user / 123456`

### 3) 启动前端

两个前端可以独立启动，默认都通过 Vite Proxy 把 `/api` 转发到后端。

#### 3.1 启动 frontend-vue（端口 8804）

```powershell
cd frontend-vue
npm install
npm run dev
```

默认访问：

- `http://localhost:8804`

代理目标（后端地址）可通过环境变量 `VITE_PROXY_TARGET` 配置，例如创建 `.env`：

```powershell
Copy-Item .env.example .env
```

然后把 `.env` 内的 `VITE_PROXY_TARGET` 改成你的后端地址（后端默认是 `http://127.0.0.1:8813`）。

#### 3.2 启动 app-web（端口 8803）

```powershell
cd app-web
npm install
npm run dev
```

默认访问：

- `http://localhost:8803`

可选 Mock：

- `app-web/.env.development` 中将 `VITE_USE_MOCK=true`，然后重新执行 `npm run dev`

## 打包与部署

### 后端打包

```powershell
cd java-backend
mvn -DskipTests package
```

运行 jar（示例）：

```powershell
java -jar target/java-backend-0.0.1-SNAPSHOT.jar
```

生产环境建议通过环境变量注入数据库与 JWT 配置，不要在文档或仓库中明文写入真实密码/密钥。

### 前端打包

```powershell
cd frontend-vue
npm install
npm run build
```

```powershell
cd app-web
npm install
npm run build
```
