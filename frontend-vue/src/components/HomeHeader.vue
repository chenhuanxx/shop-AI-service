<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'

type Props = {
  title?: string
  subtitle?: string
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: 'python-admin',
  subtitle: 'Vue3 · TS · Vite · Pinia · Axios · Router',
  loading: false,
})

const emit = defineEmits<{
  (e: 'refresh'): void
  (e: 'logout'): void
}>()

const route = useRoute()
const authStore = useAuthStore()

const activePath = computed(() => route.path)
const isAdmin = computed(() => (authStore.user?.role || authStore.user?.username || '').toLowerCase() === 'admin')

const courseOpen = ref(false)
const productOpen = ref(false)
const systemOpen = ref(false)
const contentOpen = ref(false)

const isCourseActive = computed(
  () =>
    activePath.value.startsWith('/booking-center') ||
    activePath.value.startsWith('/public-course-apply') ||
    activePath.value.startsWith('/open-courses') ||
    activePath.value.startsWith('/open-publish'),
)

const isProductActive = computed(
  () => activePath.value.startsWith('/products') || activePath.value.startsWith('/product-categories') || activePath.value.startsWith('/orders'),
)

const isSystemActive = computed(() => activePath.value.startsWith('/roles') || activePath.value.startsWith('/users'))

const isContentActive = computed(() => activePath.value.startsWith('/banners'))

watch(
  () => activePath.value,
  () => {
    if (isCourseActive.value) courseOpen.value = true
    if (isProductActive.value) productOpen.value = true
    if (isSystemActive.value) systemOpen.value = true
    if (isContentActive.value) contentOpen.value = true
  },
  { immediate: true },
)

function onRefresh() {
  emit('refresh')
}

function onLogout() {
  emit('logout')
}
</script>

<template>
  <header class="top">
    <div class="brand">
      <div class="logo">PA</div>
      <div class="brand-text">
        <div class="title">{{ props.title }}</div>
        <div class="subtitle">{{ props.subtitle }}</div>
      </div>
    </div>

    <nav class="nav">
      <router-link class="nav-link" :class="{ active: activePath === '/' }" to="/">首页</router-link>
      <router-link
        class="nav-link"
        :class="{ active: activePath.startsWith('/dashboard') }"
        to="/dashboard"
      >
        仪表盘
      </router-link>

      <button
        type="button"
        class="nav-group"
        :class="{ active: isCourseActive }"
        @click="courseOpen = !courseOpen"
      >
        <span>课程管理</span>
        <span class="chev" :class="{ open: courseOpen }">▾</span>
      </button>
      <div v-show="courseOpen" class="nav-sub">
        <router-link
          class="nav-link sub"
          :class="{ active: activePath.startsWith('/booking-center') }"
          to="/booking-center"
        >
          预约中心
        </router-link>
        <router-link
          class="nav-link sub"
          :class="{ active: activePath.startsWith('/public-course-apply') }"
          to="/public-course-apply"
        >
          公开课申请
        </router-link>
        <router-link
          v-if="isAdmin"
          class="nav-link sub"
          :class="{ active: activePath.startsWith('/open-courses') }"
          to="/open-courses"
        >
          公开课审批
        </router-link>
        <router-link
          v-if="isAdmin"
          class="nav-link sub"
          :class="{ active: activePath.startsWith('/open-publish') }"
          to="/open-publish"
        >
          公开课发布
        </router-link>
      </div>

      <button
        v-if="isAdmin"
        type="button"
        class="nav-group"
        :class="{ active: isProductActive }"
        @click="productOpen = !productOpen"
      >
        <span>产品管理</span>
        <span class="chev" :class="{ open: productOpen }">▾</span>
      </button>
      <div v-if="isAdmin" v-show="productOpen" class="nav-sub">
        <router-link
          class="nav-link sub"
          :class="{ active: activePath === '/product-categories' }"
          to="/product-categories"
        >
          产品类型
        </router-link>
        <router-link
          class="nav-link sub"
          :class="{ active: activePath === '/products' }"
          to="/products"
        >
          产品列表
        </router-link>
        <router-link
          class="nav-link sub"
          :class="{ active: activePath === '/orders' }"
          to="/orders"
        >
          订单管理
        </router-link>
      </div>

      <button
        v-if="isAdmin"
        type="button"
        class="nav-group"
        :class="{ active: isSystemActive }"
        @click="systemOpen = !systemOpen"
      >
        <span>系统设置</span>
        <span class="chev" :class="{ open: systemOpen }">▾</span>
      </button>
      <div v-if="isAdmin" v-show="systemOpen" class="nav-sub">
        <router-link class="nav-link sub" :class="{ active: activePath.startsWith('/roles') }" to="/roles">
          角色管理
        </router-link>
        <router-link class="nav-link sub" :class="{ active: activePath.startsWith('/users') }" to="/users">
          用户管理
        </router-link>
      </div>

      <button
        v-if="isAdmin"
        type="button"
        class="nav-group"
        :class="{ active: isContentActive }"
        @click="contentOpen = !contentOpen"
      >
        <span>内容管理</span>
        <span class="chev" :class="{ open: contentOpen }">▾</span>
      </button>
      <div v-if="isAdmin" v-show="contentOpen" class="nav-sub">
        <router-link class="nav-link sub" :class="{ active: activePath.startsWith('/banners') }" to="/banners">
          Banner 管理
        </router-link>
      </div>
    </nav>

    <div class="actions">
      <button type="button" class="btn btn-ghost" @click="onRefresh" :disabled="props.loading">
        {{ props.loading ? '刷新中…' : '刷新数据' }}
      </button>
      <button type="button" class="btn btn-danger" @click="onLogout">退出</button>
    </div>
  </header>
</template>

<style scoped lang="less">
.top {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 240px;
  z-index: 100;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-start;
  padding: 32px 20px;
  backdrop-filter: blur(12px);
  background: rgba(10, 12, 18, 0.68);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  gap: 32px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-weight: 800;
  letter-spacing: 0.8px;
  color: #0b0c10;
  background: linear-gradient(135deg, #a78bfa, #60a5fa, #34d399);
  box-shadow: 0 10px 28px rgba(96, 165, 250, 0.25);
  flex-shrink: 0;
}

.brand-text {
  overflow: hidden;
}

.brand-text .title {
  font-weight: 700;
  line-height: 1.1;
  color: #e5e7eb;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.brand-text .subtitle {
  font-size: 12px;
  color: rgba(229, 231, 235, 0.7);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 4px;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: auto;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: rgba(96, 165, 250, 0.4) transparent;
}

.nav::-webkit-scrollbar {
  width: 6px;
}

.nav::-webkit-scrollbar-track {
  background: transparent;
  border-radius: 3px;
}

.nav::-webkit-scrollbar-thumb {
  background: rgba(96, 165, 250, 0.4);
  border-radius: 3px;
  transition: background 0.2s ease;
}

.nav::-webkit-scrollbar-thumb:hover {
  background: rgba(96, 165, 250, 0.6);
}

.nav-link {
  display: flex;
  align-items: center;
  height: 40px;
  min-height: 40px;
  padding: 0 16px;
  border-radius: 10px;
  border: 1px solid transparent;
  color: rgba(229, 231, 235, 0.78);
  text-decoration: none;
  background: transparent;
  transition: all 0.15s ease;
}

.nav-link.sub {
  height: 36px;
  min-height: 36px;
  padding-left: 28px;
  font-size: 13px;
}

.nav-group {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 40px;
  min-height: 40px;
  padding: 0 16px;
  border-radius: 10px;
  border: 1px solid transparent;
  color: rgba(229, 231, 235, 0.78);
  background: rgba(255, 255, 255, 0.02);
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: left;
}

.nav-group:hover {
  background: rgba(255, 255, 255, 0.04);
  color: rgba(229, 231, 235, 0.95);
}

.nav-group.active {
  color: rgba(229, 231, 235, 0.95);
  border-color: rgba(96, 165, 250, 0.2);
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.15), rgba(56, 189, 248, 0.05));
  font-weight: 500;
}

.nav-sub {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-left: 6px;
}

.chev {
  opacity: 0.7;
  transition: transform 0.15s ease, opacity 0.15s ease;
}

.chev.open {
  transform: rotate(180deg);
  opacity: 0.9;
}

.nav-link:hover {
  background: rgba(255, 255, 255, 0.04);
  color: rgba(229, 231, 235, 0.95);
}

.nav-link.active {
  color: rgba(229, 231, 235, 0.95);
  border-color: rgba(96, 165, 250, 0.2);
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.15), rgba(56, 189, 248, 0.05));
  font-weight: 500;
}

.btn {
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  padding: 10px 14px;
  color: #e5e7eb;
  background: rgba(255, 255, 255, 0.06);
  cursor: pointer;
  transition: transform 0.12s ease, background 0.12s ease;
  width: 100%;
  text-align: center;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn:hover:not(:disabled) {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.1);
}

.btn-ghost {
  background: rgba(255, 255, 255, 0.04);
}

.btn-danger {
  border-color: rgba(248, 113, 113, 0.3);
  background: rgba(248, 113, 113, 0.12);
}
</style>
