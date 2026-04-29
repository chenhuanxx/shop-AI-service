<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import HomeHeader from '../../components/HomeHeader.vue'
import WorkHoursBarChart from '../../components/WorkHoursBarChart.vue'
import { http } from '../../services/http'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const now = ref(new Date())
const backendOk = ref<boolean | null>(null)
const backendLatencyMs = ref<number | null>(null)
const backendError = ref('')
const loading = ref(false)
const toast = ref('')
const debugJson = ref('')

const apiDocsUrl = import.meta.env.VITE_API_DOCS_URL || 'http://localhost:8814/docs'

const username = computed(() => authStore.user?.username || 'admin')
const isAdmin = computed(() => (authStore.user?.role || authStore.user?.username || '').toLowerCase() === 'admin')
const token = computed(() => authStore.token || localStorage.getItem('access_token') || '')
const tokenShort = computed(() => (token.value ? `${token.value.slice(0, 16)}…${token.value.slice(-10)}` : ''))
const dayPart = computed(() => {
  const hour = now.value.getHours()
  if (hour < 6) return '凌晨'
  if (hour < 12) return '上午'
  if (hour < 18) return '下午'
  return '晚上'
})
const clockText = computed(() => {
  const d = now.value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
})

async function onLogout() {
  authStore.logout()
  await router.replace('/login')
}

async function fetchHealth() {
  backendError.value = ''
  const start = performance.now()
  try {
    const res = await http.get('/health')
    backendOk.value = res.data?.status === 'ok'
    backendLatencyMs.value = Math.round(performance.now() - start)
    return { ok: backendOk.value, latencyMs: backendLatencyMs.value }
  } catch (e) {
    backendOk.value = false
    backendLatencyMs.value = Math.round(performance.now() - start)
    const err = e as { message?: string }
    backendError.value = err.message || '请求失败'
    return { ok: backendOk.value, latencyMs: backendLatencyMs.value }
  }
}

async function refreshAll() {
  loading.value = true
  try {
    const health = await fetchHealth()
    debugJson.value = JSON.stringify({ me: authStore.user, health }, null, 2)
  } catch {
    // 保持会话，不再因 /auth/me 失败而强制退出
  } finally {
    loading.value = false
  }
}

async function copyToken() {
  if (!token.value) return
  try {
    await navigator.clipboard.writeText(token.value)
    toast.value = 'Token 已复制'
  } catch {
    toast.value = '复制失败'
  }
  window.setTimeout(() => {
    toast.value = ''
  }, 1800)
}

const timer = window.setInterval(() => {
  now.value = new Date()
}, 1000)

onUnmounted(() => {
  window.clearInterval(timer)
})

onMounted(async () => {
  now.value = new Date()
  await refreshAll()
})
</script>

<template>
  <div class="pa-list-page">
    <div class="pa-list-bg">
      <div class="pa-list-orb pa-list-orb-a"></div>
      <div class="pa-list-orb pa-list-orb-b"></div>
      <div class="pa-list-orb pa-list-orb-c"></div>
      <div class="pa-list-grid"></div>
    </div>

    <HomeHeader :loading="loading" @refresh="refreshAll" @logout="onLogout" />

    <main class="wrap">
      <section class="hero cardx">
        <div class="hero-left">
          <div class="kicker">
            <span class="chip">{{ dayPart }}好，{{ username }}</span>
            <span class="chip chip-ghost">{{ clockText }}</span>
            <span
              class="chip"
              :class="backendOk === true ? 'chip-ok' : backendOk === false ? 'chip-bad' : 'chip-ghost'"
            >
              后端：{{
                backendOk === true ? '在线' : backendOk === false ? '离线' : '检测中'
              }}
              <template v-if="backendLatencyMs !== null"> · {{ backendLatencyMs }}ms</template>
            </span>
          </div>

          <h1 class="hero-title">
            你的控制台，
            <span class="shine">一眼即得</span>
          </h1>
          <p class="hero-desc">
            已接入 Python 登录接口，路由已拦截。这里是一个“炫酷但不花哨”的首页：玻璃拟态、动态光晕、卡片仪表盘。
          </p>

          <div class="hero-actions">
            <button type="button" class="primary" @click="copyToken" :disabled="!token">
              复制 Token
            </button>
            <router-link v-if="isAdmin" class="primary action-link" to="/open-courses">公开课审批</router-link>
            <router-link v-if="isAdmin" class="primary action-link" to="/open-publish">公开课发布</router-link>
            <router-link v-if="isAdmin" class="primary action-link" to="/orders">订单管理</router-link>
            <a class="link" :href="apiDocsUrl" target="_blank" rel="noreferrer">
              打开 API 文档
            </a>
          </div>

          <p v-if="backendError" class="warn">后端错误：{{ backendError }}</p>
          <p v-if="toast" class="toast">{{ toast }}</p>
        </div>

        <div class="hero-right">
          <div class="panel">
            <div class="panel-top">
              <div class="panel-title">SESSION</div>
              <div class="badge" :class="token ? 'badge-ok' : 'badge-bad'">
                {{ token ? 'AUTHED' : 'NO TOKEN' }}
              </div>
            </div>
            <div class="mono">
              <div class="mono-line">
                <span class="mono-key">user</span>
                <span class="mono-val">{{ username }}</span>
              </div>
              <div class="mono-line">
                <span class="mono-key">token</span>
                <span class="mono-val">{{ tokenShort || '—' }}</span>
              </div>
              <div class="mono-line">
                <span class="mono-key">backend</span>
                <span class="mono-val">{{
                  backendOk === true ? 'ok' : backendOk === false ? 'down' : 'checking'
                }}</span>
              </div>
            </div>
            <div class="spark"></div>
          </div>
        </div>
      </section>

      <section class="dash">
        <div class="cardx metric">
          <div class="metric-top">
            <div class="metric-name">当前用户</div>
            <div class="metric-pill">/auth/me</div>
          </div>
          <div class="metric-value">{{ authStore.user?.username || '—' }}</div>
          <div class="metric-sub">登录后自动获取</div>
        </div>

        <div class="cardx metric">
          <div class="metric-top">
            <div class="metric-name">后端状态</div>
            <div class="metric-pill">/health</div>
          </div>
          <div class="metric-value">
            {{ backendOk === true ? 'ONLINE' : backendOk === false ? 'OFFLINE' : 'CHECK' }}
          </div>
          <div class="metric-sub">
            <template v-if="backendLatencyMs !== null">延迟 {{ backendLatencyMs }}ms</template>
            <template v-else>等待检测</template>
          </div>
        </div>

        <div class="cardx metric">
          <div class="metric-top">
            <div class="metric-name">权限令牌</div>
            <div class="metric-pill">JWT</div>
          </div>
          <div class="metric-value">{{ token ? 'READY' : 'EMPTY' }}</div>
          <div class="metric-sub">请求时自动注入 Authorization</div>
        </div>
      </section>

      <WorkHoursBarChart />

      <section class="cardx debug">
        <div class="debug-head">
          <div class="debug-title">调试信息</div>
          <button type="button" class="ghost" @click="refreshAll" :disabled="loading">
            {{ loading ? '同步中…' : '重新同步' }}
          </button>
        </div>
        <pre class="debug-pre">{{ debugJson || '—' }}</pre>
      </section>
    </main>
  </div>
</template>

<style scoped lang="less">
 
.wrap {
  height: 100vh;
  overflow: auto;
  margin-left: 240px;
  padding: 22px 22px 36px;
  box-sizing: border-box;
}

button {
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  padding: 10px 14px;
  color: #e5e7eb;
  background: rgba(255, 255, 255, 0.06);
  cursor: pointer;
  transition: transform 0.12s ease, background 0.12s ease;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

button:hover:not(:disabled) {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.1);
}

.ghost {
  background: rgba(255, 255, 255, 0.04);
}

.danger {
  border-color: rgba(248, 113, 113, 0.3);
  background: rgba(248, 113, 113, 0.12);
}

.primary {
  border-color: rgba(99, 102, 241, 0.35);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.35), rgba(56, 189, 248, 0.22));
}

.link {
  color: rgba(229, 231, 235, 0.85);
  text-decoration: none;
  padding: 10px 0;
}

.link:hover {
  color: #ffffff;
}

.cardx {
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(14px);
}

.hero {
  display: grid;
  grid-template-columns: 1.25fr 0.75fr;
  gap: 18px;
  padding: 22px;
  position: relative;
  overflow: hidden;
}

.hero::after {
  content: '';
  position: absolute;
  inset: -2px;
  background: conic-gradient(
    from 120deg,
    rgba(99, 102, 241, 0.55),
    rgba(56, 189, 248, 0.45),
    rgba(34, 197, 94, 0.35),
    rgba(244, 114, 182, 0.45),
    rgba(99, 102, 241, 0.55)
  );
  opacity: 0.15;
  filter: blur(16px);
}

.hero-left,
.hero-right {
  position: relative;
  z-index: 1;
}

.kicker {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.06);
}

.chip-ghost {
  color: rgba(229, 231, 235, 0.8);
}

.chip-ok {
  border-color: rgba(34, 197, 94, 0.3);
  background: rgba(34, 197, 94, 0.12);
}

.chip-bad {
  border-color: rgba(248, 113, 113, 0.35);
  background: rgba(248, 113, 113, 0.12);
}

.hero-title {
  margin: 0;
  font-size: 44px;
  letter-spacing: -1.2px;
  line-height: 1.05;
}

.shine {
  background: linear-gradient(90deg, #a78bfa, #60a5fa, #34d399);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.hero-desc {
  margin: 14px 0 18px;
  color: rgba(229, 231, 235, 0.78);
  line-height: 1.55;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.action-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  padding: 10px 14px;
  color: #e5e7eb;
  cursor: pointer;
  text-decoration: none;
  transition: transform 0.12s ease, background 0.12s ease;
}

.action-link:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.1);
}

.warn {
  margin: 12px 0 0;
  color: rgba(251, 191, 36, 0.92);
  font-size: 13px;
}

.toast {
  margin: 10px 0 0;
  font-size: 13px;
  color: rgba(167, 243, 208, 0.95);
}

.panel {
  position: relative;
  border-radius: 16px;
  padding: 16px;
  background: rgba(3, 4, 8, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.12);
  overflow: hidden;
}

.panel-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-title {
  font-size: 12px;
  letter-spacing: 1.6px;
  color: rgba(229, 231, 235, 0.78);
}

.badge {
  font-size: 11px;
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.06);
}

.badge-ok {
  border-color: rgba(34, 197, 94, 0.35);
  background: rgba(34, 197, 94, 0.12);
}

.badge-bad {
  border-color: rgba(248, 113, 113, 0.35);
  background: rgba(248, 113, 113, 0.12);
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono',
    'Courier New', monospace;
  font-size: 12px;
  color: rgba(229, 231, 235, 0.86);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mono-line {
  display: grid;
  grid-template-columns: 70px 1fr;
  gap: 10px;
  align-items: center;
}

.mono-key {
  color: rgba(229, 231, 235, 0.62);
}

.mono-val {
  color: rgba(229, 231, 235, 0.92);
}

.spark {
  position: absolute;
  inset: -80px;
  background: radial-gradient(circle at 30% 40%, rgba(56, 189, 248, 0.4), transparent 60%),
    radial-gradient(circle at 70% 60%, rgba(167, 139, 250, 0.38), transparent 60%);
  filter: blur(24px);
  opacity: 0.8;
  animation: pulse 6s ease-in-out infinite;
}

.dash {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.metric {
  padding: 16px;
  position: relative;
  overflow: hidden;
}

.metric::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 20% 20%, rgba(99, 102, 241, 0.25), transparent 55%),
    radial-gradient(circle at 70% 60%, rgba(34, 197, 94, 0.18), transparent 55%);
  opacity: 0.9;
}

.metric > * {
  position: relative;
  z-index: 1;
}

.metric-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.metric-name {
  font-size: 13px;
  color: rgba(229, 231, 235, 0.78);
}

.metric-pill {
  font-size: 11px;
  padding: 4px 8px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(229, 231, 235, 0.78);
}

.metric-value {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.6px;
}

.metric-sub {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(229, 231, 235, 0.68);
}

.debug {
  margin-top: 14px;
  padding: 16px;
}

.debug-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.debug-title {
  font-weight: 700;
}

.debug-pre {
  margin: 0;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(3, 4, 8, 0.45);
  color: rgba(229, 231, 235, 0.86);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono',
    'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
  overflow: auto;
}

@media (max-width: 980px) {
  .hero {
    grid-template-columns: 1fr;
  }
  .dash {
    grid-template-columns: 1fr;
  }
  .hero-title {
    font-size: 36px;
  }
}

@keyframes float {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(0, 18px, 0);
  }
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
    opacity: 0.75;
  }
  50% {
    transform: scale(1.08);
    opacity: 0.95;
  }
}
</style>

