<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { message } from 'ant-design-vue'
import type { TablePaginationConfig, TableProps } from 'ant-design-vue'

import HomeHeader from '../../components/HomeHeader.vue'
import { http } from '../../services/http'
import { useAuthStore } from '../../stores/auth'
import { formatDateTime } from '../../utils/date'

type ApiResponse<T> = { code: number; message: string; data: T }

type RoleItem = {
  id: string
  name: string
  description: string
  createdAt: string
}

const authStore = useAuthStore()
const router = useRouter()

const keyword = ref('')
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const items = ref<RoleItem[]>([])

const modalOpen = ref(false)
const submitting = ref(false)
const formState = reactive({ name: '', description: '' })

const rangeText = computed(() => {
  if (total.value <= 0) return '0 - 0'
  const start = (page.value - 1) * pageSize.value + 1
  const end = Math.min(total.value, page.value * pageSize.value)
  return `${start} - ${end}`
})

const columns: NonNullable<TableProps['columns']> = [
  { title: '角色名', dataIndex: 'name', key: 'name', width: 160 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    key: 'createdAt',
    width: 210,
    customRender: ({ text }) => formatDateTime(text),
  },
  { title: '操作', key: 'action', width: 120, fixed: 'right', customRender: ({ record }) => record },
]

async function onLogout() {
  authStore.logout()
  await router.replace('/login')
}

async function fetchList() {
  loading.value = true
  try {
    const res = await http.get<ApiResponse<{ list: RoleItem[]; total: number }>>('/roles/page', {
      params: {
        page: page.value,
        page_size: pageSize.value,
        keyword: keyword.value.trim() || undefined,
      },
    })
    if (res.data.code !== 0) {
      message.error(res.data.message || '请求失败')
      items.value = []
      total.value = 0
      return
    }
    items.value = res.data.data?.list || []
    total.value = res.data.data?.total || 0
  } catch (e) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '请求失败')
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  formState.name = ''
  formState.description = ''
  modalOpen.value = true
}

function onSearch() {
  page.value = 1
  void fetchList()
}

function onReset() {
  keyword.value = ''
  page.value = 1
  pageSize.value = 10
  void fetchList()
}

function onTableChange(p: TablePaginationConfig) {
  const nextPage = typeof p.current === 'number' && p.current > 0 ? p.current : 1
  const nextSize = typeof p.pageSize === 'number' && p.pageSize > 0 ? p.pageSize : 10
  const sizeChanged = nextSize !== pageSize.value
  pageSize.value = nextSize
  page.value = sizeChanged ? 1 : nextPage
  void fetchList()
}

async function submitCreate() {
  const name = formState.name.trim()
  if (!name) {
    message.warning('请输入角色名')
    return
  }
  submitting.value = true
  try {
    const res = await http.post<ApiResponse<RoleItem>>('/roles', { name, description: formState.description.trim() })
    if (res.data.code !== 0) {
      message.error(res.data.message || '创建失败')
      return
    }
    message.success('创建成功')
    modalOpen.value = false
    void fetchList()
  } catch (e) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

async function onDelete(r: RoleItem) {
  if (!r?.id) return
  submitting.value = true
  try {
    const res = await http.post<ApiResponse<{ ok: boolean }>>('/roles/delete', { id: r.id })
    if (res.data.code !== 0) {
      message.error(res.data.message || '删除失败')
      return
    }
    message.success('删除成功')
    void fetchList()
  } catch (e) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '删除失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await fetchList()
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

    <HomeHeader
      title="角色管理"
      subtitle="角色列表 · 新增 · 删除 · /roles"
      :loading="loading || submitting"
      @refresh="fetchList"
      @logout="onLogout"
    />

    <main class="pa-list-wrap">
      <a-card class="pa-list-card" :bordered="false">
        <template #title>
          <div class="pa-list-card-title">
            <span>角色列表</span>
            <span class="pa-list-card-sub">共 {{ total }} 条 · 显示 {{ rangeText }}</span>
          </div>
        </template>

        <template #extra>
          <a-space wrap>
            <a-input v-model:value="keyword" allow-clear placeholder="按角色名/描述搜索" style="width: 260px" />
            <a-button @click="onSearch" :disabled="loading">查询</a-button>
            <a-button @click="onReset" :disabled="loading">重置</a-button>
            <a-button @click="fetchList" :disabled="loading">刷新</a-button>
            <a-button type="primary" @click="openCreate">新增角色</a-button>
          </a-space>
        </template>

        <div class="pa-table-scroll">
          <a-table
            row-key="id"
            :columns="columns"
            :data-source="items"
            :loading="loading"
            :pagination="{
              current: page,
              pageSize,
              total,
              showSizeChanger: true,
              showQuickJumper: true,
            }"
            :scroll="{ x: '100%' }"
            @change="onTableChange" 
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-popconfirm title="确认删除该角色？" @confirm="onDelete(record)">
                    <a-button type="link" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </a-card>

      <a-modal
        v-model:open="modalOpen"
        title="新增角色"
        ok-text="创建"
        :confirm-loading="submitting"
        @ok="submitCreate"
        @cancel="modalOpen = false"
      >
        <a-form layout="vertical">
          <a-form-item label="角色名" required>
            <a-input v-model:value="formState.name" placeholder="如：admin、user" />
          </a-form-item>
          <a-form-item label="描述">
            <a-input v-model:value="formState.description" placeholder="可选" />
          </a-form-item>
        </a-form>
      </a-modal>
    </main>
  </div>
</template>
