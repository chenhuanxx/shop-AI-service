<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('admin')
const password = ref('admin123')
const errorText = ref('')
const showPassword = ref(false) 
const backendError = ref('')
const shaking = ref(false)

async function onSubmit() {
  errorText.value = ''
  try {
    await authStore.login(username.value, password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (e) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    errorText.value = err.response?.data?.detail || err.message || '登录失败'
    shaking.value = true
    window.setTimeout(() => {
      shaking.value = false
    }, 420)
  }
}

</script>

<template>
  <div class="page"> 
    <div class="pa-list-bg">
      <div class="pa-list-orb pa-list-orb-a"></div>
      <div class="pa-list-orb pa-list-orb-b"></div>
      <div class="pa-list-orb pa-list-orb-c"></div>
      <div class="pa-list-grid"></div>
    </div>

    <main class="wrap">
      <section class="brand">
        <div class="logo">PA</div>
        <div class="brand-text">
          <div class="title">python-admin</div>
          <div class="subtitle">登录以进入控制台</div>
        </div>
      </section>

      <section class="panel" :class="{ shake: shaking }">
        <div class="panel-top">
          <div class="panel-title">登录</div>
          <div class="kicker"></div>
        </div>

        <form class="form" @submit.prevent="onSubmit">
          <label class="field">
            <span class="label">用户名/手机号</span>
            <div class="input-wrap">
              <span class="icon">U</span>
              <input v-model="username" autocomplete="username" placeholder="请输入用户名或手机号" />
            </div>
          </label>

          <label class="field">
            <span class="label">密码</span>
            <div class="input-wrap">
              <span class="icon">P</span>
              <input
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="current-password"
                placeholder="请输入密码"
              />
              <button
                type="button"
                class="eye"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                @click="showPassword = !showPassword"
              >
                {{ showPassword ? '隐藏' : '显示' }}
              </button>
            </div>
          </label>

          <button type="submit" class="primary" :disabled="authStore.loading">
            <span class="primary-text">{{ authStore.loading ? '登录中…' : '登录' }}</span>
            <span class="primary-glow"></span>
          </button>

          <div class="hint">
            <p v-if="errorText" class="error">{{ errorText }}</p>
            <p v-else class="muted">默认账号：admin / admin123</p>
            <p v-if="backendError" class="warn">后端错误：{{ backendError }}</p>
          </div>
        </form>
      </section>

      <footer class="footer">
        <span class="muted">忘记密码</span>
        
      </footer>
    </main>
  </div>
</template>

<style scoped lang="less">
.page {
  min-height: 100vh;
  color: #e5e7eb;
  background: #06070a;
  position: relative;
  overflow: hidden;
}
 
 

.wrap {
  position: relative;
  max-width: 980px;
  margin: 0 auto;
  padding: 48px 16px 32px;
  display: grid;
  justify-items: center;
  gap: 18px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: center;
}

.logo {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-weight: 900;
  letter-spacing: 0.8px;
  color: #0b0c10;
  background: linear-gradient(135deg, #a78bfa, #60a5fa, #34d399);
  box-shadow: 0 18px 60px rgba(96, 165, 250, 0.25);
}

.brand-text .title {
  font-weight: 800;
  line-height: 1.05;
  font-size: 18px;
}

.brand-text .subtitle {
  font-size: 12px;
  margin-top: 5px;
  color: rgba(229, 231, 235, 0.72);
}

.panel {
  width: min(520px, 100%);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.48);
  backdrop-filter: blur(14px);
  padding: 18px;
  position: relative;
  overflow: hidden;
}

.panel::before {
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
  opacity: 0.14;
  filter: blur(16px);
}

.panel-top,
.form {
  position: relative;
  z-index: 1;
}

.panel-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.panel-title {
  font-size: 30px;
  letter-spacing: 2px;
  color: rgba(229, 231, 235, 0.78); 
}

.kicker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.form {
  display: grid;
  gap: 12px;
}

.field {
  display: grid;
  gap: 8px;
}

.label {
  font-size: 12px;
  color: rgba(229, 231, 235, 0.78);
}

.input-wrap {
  display: grid;
  grid-template-columns: 30px 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(3, 4, 8, 0.35);
  transition: border-color 0.12s ease, transform 0.12s ease;
}

.input-wrap:focus-within {
  border-color: rgba(96, 165, 250, 0.45);
  transform: translateY(-1px);
}

.icon {
  opacity: 0.9;
  font-size: 14px;
}

input {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  color: rgba(229, 231, 235, 0.92);
  font-size: 14px;
}

input::placeholder {
  color: rgba(229, 231, 235, 0.45);
}

.eye {
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(229, 231, 235, 0.85);
  border-radius: 12px;
  padding: 8px 10px;
  cursor: pointer;
}

.primary {
  position: relative;
  border-radius: 14px;
  border: 1px solid rgba(99, 102, 241, 0.35);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.35), rgba(56, 189, 248, 0.22));
  color: rgba(229, 231, 235, 0.95);
  padding: 12px 14px;
  cursor: pointer;
  overflow: hidden;
}

.primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.primary-text {
  position: relative;
  z-index: 1;
  font-weight: 800;
  letter-spacing: 0.2px;
}

.primary-glow {
  position: absolute;
  inset: -60px;
  background: radial-gradient(circle at 30% 40%, rgba(56, 189, 248, 0.55), transparent 60%),
    radial-gradient(circle at 70% 60%, rgba(167, 139, 250, 0.55), transparent 60%);
  filter: blur(22px);
  opacity: 0.85;
  animation: pulse 6s ease-in-out infinite;
}

.hint {
  display: grid;
  gap: 6px;
  padding-top: 2px;
}

.muted {
  margin: 0;
  font-size: 12px;
  color: rgba(229, 231, 235, 0.72);
}

.warn {
  margin: 0;
  font-size: 12px;
  color: rgba(251, 191, 36, 0.92);
}

.error {
  margin: 0;
  font-size: 12px;
  color: rgba(248, 113, 113, 0.92);
}

.footer {
  width: min(520px, 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.shake {
  animation: shake 0.42s ease-in-out;
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

@keyframes shake {
  0%,
  100% {
    transform: translateX(0);
  }
  20% {
    transform: translateX(-6px);
  }
  40% {
    transform: translateX(6px);
  }
  60% {
    transform: translateX(-4px);
  }
  80% {
    transform: translateX(4px);
  }
}
</style>
