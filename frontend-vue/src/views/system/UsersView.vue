<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { message } from 'ant-design-vue'
import type { TablePaginationConfig, TableProps } from 'ant-design-vue'

import HomeHeader from '../../components/HomeHeader.vue'
import { http } from '../../services/http'
import { useAuthStore } from '../../stores/auth'
import { formatDateTime, formatDate } from '../../utils/date'

type UserItem = {
  id: number
  username: string
  role: string
  phone: string
  address: string
  gender: string
  birth_date: string
  is_active: boolean
  created_at: string
}

type UserPage = {
  page: number
  page_size: number
  total: number
  items: UserItem[]
}

const authStore = useAuthStore()
const router = useRouter()

const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const items = ref<UserItem[]>([])
const loading = ref(false)

const rangeText = computed(() => {
  if (total.value <= 0) return '0 - 0'
  const start = (page.value - 1) * pageSize.value + 1
  const end = Math.min(total.value, page.value * pageSize.value)
  return `${start} - ${end}`
})

const modalOpen = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const modalSubmitting = ref(false)
const editingId = ref<number | null>(null)

type RoleItem = { id: string; name: string; description: string; createdAt: string }
type ApiResponse<T> = { code: number; message: string; data: T }
const roles = ref<RoleItem[]>([])
const roleModalOpen = ref(false)
const roleSubmitting = ref(false)
const roleUserId = ref<number | null>(null)
const roleSelected = ref('')

const resetModalOpen = ref(false)
const resetSubmitting = ref(false)
const resetUserId = ref<number | null>(null)
const resetUsername = ref('')
const resetPassword = ref('')

const formState = reactive({
  username: '',
  password: '',
  phone: '',
  address: '',
  gender: '',
  birth_date: '',
  is_active: true,
  role: 'user',
})

const columns: NonNullable<TableProps['columns']> = [ 
  { title: '用户名', dataIndex: 'username', key: 'username', ellipsis: true },
  { title: '电话', dataIndex: 'phone', key: 'phone', width: 140, ellipsis: true },
  { title: '性别', dataIndex: 'gender', key: 'gender', width: 90 },
  {
    title: '出生年月',
    dataIndex: 'birth_date',
    key: 'birth_date',
    width: 130,
    customRender: ({ text }) => formatDate(text),
  },
  { title: '角色', dataIndex: 'role', key: 'role', width: 120 },
  {
    title: '状态',
    dataIndex: 'is_active',
    key: 'is_active',
    width: 110,
    customRender: ({ record }) => (record.is_active ? '启用' : '禁用'),
  },
  {
    title: '创建时间',
    dataIndex: 'created_at',
    key: 'created_at',
    width: 210,
    customRender: ({ text }) => formatDateTime(text),
  },
  {
    title: '操作',
    key: 'action',
    width: 240,
    fixed: 'right',
    customRender: ({ record }) => record,
  },
]

async function onLogout() {
  authStore.logout()
  await router.replace('/login')
}

async function fetchList() {
  loading.value = true
  try {
    const res = await http.get<UserPage>('/users', {
      params: {
        page: page.value,
        page_size: pageSize.value,
        keyword: keyword.value.trim() || undefined,
      },
    })
    items.value = res.data.items || []
    total.value = res.data.total || 0
  } catch (e) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '请求失败')
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function fetchRoles() {
  try {
    const res = await http.get<ApiResponse<{ list: RoleItem[] }>>('/roles')
    if (res.data.code !== 0) {
      roles.value = []
      return
    }
    roles.value = res.data.data?.list || []
  } catch {
    roles.value = []
  }
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

function openCreate() {
  modalMode.value = 'create'
  editingId.value = null
  formState.username = ''
  formState.password = ''
  formState.phone = ''
  formState.address = ''
  formState.gender = ''
  formState.birth_date = ''
  formState.is_active = true
  formState.role = 'user'
  modalOpen.value = true
}

function openEdit(u: UserItem) {
  modalMode.value = 'edit'
  editingId.value = u.id
  formState.username = u.username
  formState.password = ''
  formState.phone = u.phone || ''
  formState.address = u.address || ''
  formState.gender = u.gender || ''
  formState.birth_date = u.birth_date || ''
  formState.is_active = u.is_active
  formState.role = (u.role || '').trim() || 'user'
  modalOpen.value = true
}

function openRole(u: UserItem) {
  roleUserId.value = u.id
  roleSelected.value = (u.role || '').trim() || 'user'
  roleModalOpen.value = true
}

function openResetPassword(u: UserItem) {
  resetUserId.value = u.id
  resetUsername.value = u.username || ''
  resetPassword.value = ''
  resetModalOpen.value = true
}

async function submitRole() {
  if (!roleUserId.value) return
  const role = roleSelected.value.trim()
  if (!role) {
    message.warning('请选择角色')
    return
  }
  roleSubmitting.value = true
  try {
    const res = await http.post<ApiResponse<{ ok: boolean }>>('/roles/assign', { userId: roleUserId.value, role })
    if (res.data.code !== 0) {
      message.error(res.data.message || '操作失败')
      return
    }
    message.success('已更新角色')
    roleModalOpen.value = false
    void fetchList()
  } catch (e) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '操作失败')
  } finally {
    roleSubmitting.value = false
  }
}

async function submitResetPassword() {
  if (!resetUserId.value) return
  const pwd = resetPassword.value.trim()
  if (!pwd) {
    message.warning('请输入新密码')
    return
  }
  if (pwd.length < 6) {
    message.warning('密码至少6位')
    return
  }
  resetSubmitting.value = true
  try {
    await http.post(`/users/${resetUserId.value}/reset-password`, { password: pwd })
    message.success('密码已重置')
    resetModalOpen.value = false
  } catch (e) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '操作失败')
  } finally {
    resetSubmitting.value = false
  }
}

async function submitModal() {
  modalSubmitting.value = true
  try {
    const username = formState.username.trim()
    const password = formState.password.trim()
    if (!username) {
      message.warning('请输入用户名')
      return
    }
    if (modalMode.value === 'create') {
      if (!password) {
        message.warning('请输入密码')
        return
      }
      await http.post('/users', {
        username,
        password,
        phone: formState.phone.trim() || undefined,
        address: formState.address.trim() || undefined,
        gender: formState.gender || undefined,
        birth_date: formState.birth_date || undefined,
        is_active: formState.is_active,
        role: formState.role,
      })
      message.success('新增成功')
    } else {
      if (!editingId.value) return
      const payload: {
        username: string
        is_active: boolean
        password?: string
        role?: string
        phone?: string
        address?: string
        gender?: string
        birth_date?: string
      } = {
        username,
        is_active: formState.is_active,
        phone: formState.phone.trim() || undefined,
        address: formState.address.trim() || undefined,
        gender: formState.gender || undefined,
        birth_date: formState.birth_date || undefined,
      }
      if (formState.role) payload.role = formState.role
      if (password) payload.password = password
      await http.put(`/users/${editingId.value}`, payload)
      message.success('保存成功')
    }
    modalOpen.value = false
    void fetchList()
  } catch (e) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '操作失败')
  } finally {
    modalSubmitting.value = false
  }
}

async function exportUsers() {
  try {
    const res = await http.get('/users/export', {
      params: { keyword: keyword.value.trim() || undefined },
      responseType: 'blob',
    })
    const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'users.csv'
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  } catch (e) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '导出失败')
  }
}

onMounted(async () => {
  await fetchList()
  await fetchRoles()
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
      title="用户管理"
      subtitle="新增 · 编辑 · 导出 · /users"
      :loading="loading"
      @refresh="fetchList"
      @logout="onLogout"
    />

    <main class="pa-list-wrap">
      <a-card class="pa-list-card" :bordered="false">
        <template #title>
          <div class="pa-list-card-title">
            <span>用户列表</span>
            <span class="pa-list-card-sub">共 {{ total }} 条 · 显示 {{ rangeText }}</span>
          </div>
        </template>

        <template #extra>
          <a-space wrap>
            <a-input
              v-model:value="keyword"
              allow-clear
              placeholder="按用户名搜索"
              style="width: 240px"
              @pressEnter="onSearch"
            />
            <a-button @click="onSearch" :loading="loading">查询</a-button>
            <a-button @click="onReset" :disabled="loading">重置</a-button>
            <a-button type="primary" @click="openCreate">新增用户</a-button>
            <a-button @click="exportUsers">导出</a-button>
          </a-space>
        </template>

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
                <a-button type="link" @click="openEdit(record)">编辑</a-button>
                <a-button type="link" @click="openRole(record)">角色</a-button>
                <a-button type="link" danger @click="openResetPassword(record)">重置密码</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>

      <a-modal
        v-model:open="modalOpen"
        :title="modalMode === 'create' ? '新增用户' : '编辑用户'"
        :confirm-loading="modalSubmitting"
        @ok="submitModal"
        @cancel="modalOpen = false"
        ok-text="保存"
      >
        <a-form layout="vertical">
          <a-form-item label="用户名" required>
            <a-input v-model:value="formState.username" placeholder="请输入用户名" />
          </a-form-item>
          <a-form-item :label="modalMode === 'create' ? '密码' : '密码（留空不修改）'" :required="modalMode === 'create'">
            <a-input-password v-model:value="formState.password" placeholder="请输入密码" />
          </a-form-item>
          <a-form-item label="电话号码">
            <a-input v-model:value="formState.phone" placeholder="请输入电话号码" allow-clear />
          </a-form-item>
          <a-form-item label="地址">
            <a-input v-model:value="formState.address" placeholder="请输入地址" allow-clear />
          </a-form-item>
          <a-form-item label="性别">
            <a-select
              v-model:value="formState.gender"
              placeholder="请选择"
              allow-clear
              :options="[
                { label: '男', value: '男' },
                { label: '女', value: '女' },
                { label: '未知', value: '未知' },
              ]"
            />
          </a-form-item>
          <a-form-item label="出生年月">
            <a-date-picker v-model:value="formState.birth_date" value-format="YYYY-MM-DD" style="width: 100%" placeholder="请选择出生日期" />
          </a-form-item>
          <a-form-item label="角色" required>
            <a-select v-model:value="formState.role" placeholder="请选择角色">
              <a-select-option value="user">普通用户</a-select-option>
              <a-select-option value="admin">管理员</a-select-option>
              <a-select-option v-for="role in roles.filter(r => r.name !== 'admin' && r.name !== 'user')" :key="role.name" :value="role.name">
                {{ role.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="启用">
            <a-switch v-model:checked="formState.is_active" checked-children="启用" un-checked-children="禁用" />
          </a-form-item>
        </a-form>
      </a-modal>

      <a-modal
        v-model:open="roleModalOpen"
        title="设置角色"
        ok-text="保存"
        :confirm-loading="roleSubmitting"
        @ok="submitRole"
        @cancel="roleModalOpen = false"
      >
        <a-form layout="vertical">
          <a-form-item label="角色" required>
            <a-select
              v-model:value="roleSelected"
              placeholder="请选择角色"
              :options="roles.map((r) => ({ label: r.name, value: r.name }))"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <a-modal
        v-model:open="resetModalOpen"
        :title="resetUsername ? `重置密码 · ${resetUsername}` : '重置密码'"
        ok-text="确认重置"
        :confirm-loading="resetSubmitting"
        @ok="submitResetPassword"
        @cancel="resetModalOpen = false"
      >
        <a-form layout="vertical">
          <a-form-item label="新密码" required>
            <a-input-password v-model:value="resetPassword" placeholder="请输入新密码（至少6位）" />
          </a-form-item>
        </a-form>
      </a-modal>
    </main>
  </div>
</template>
