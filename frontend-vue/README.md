# 前端（Vue3 + TS + Vite + Less + Pinia + Axios + Router）

## 启动

```bash
npm install
npm run dev
```

默认后端地址：

- `http://localhost:8814`

可通过 `.env` 覆盖：

```bash
cp .env.example .env
```

## 页面

- `/login`：登录页（默认填充 admin / admin123）
- `/`：首页（路由拦截：未登录自动跳转到 /login）
