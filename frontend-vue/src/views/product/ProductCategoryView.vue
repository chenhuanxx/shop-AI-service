<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'
import HomeHeader from '../../components/HomeHeader.vue'
import { http } from '../../services/http'
import { useAuthStore } from '../../stores/auth'
import { formatDateTime } from '../../utils/date'

type CategoryItem = {
  id: number
  name: string
  code: string
  createdAt: string
}

const authStore = useAuthStore()
const router = useRouter()

const loading = ref(false)
const items = ref<CategoryItem[]>([])
const modalOpen = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const modalSubmitting = ref(false)
const editingId = ref<number | null>(null)

const formState = reactive({
  name: '',
  code: '',
})

const columns: NonNullable<TableProps['columns']> = [
  { title: '类型名称', dataIndex: 'name', key: 'name' },
  { title: '类型编码', dataIndex: 'code', key: 'code' },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    key: 'createdAt',
    customRender: ({ text }) => formatDateTime(text),
  },
  { title: '操作', key: 'action', width: 150, fixed: 'right' },
]

async function onLogout() {
  authStore.logout()
  await router.replace('/login')
}

async function fetchList() {
  loading.value = true
  try {
    const res = await http.get<CategoryItem[]>('/products/categories')
    items.value = res.data || []
  } catch (e: any) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '获取列表失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  modalMode.value = 'create'
  editingId.value = null
  formState.name = ''
  formState.code = ''
  modalOpen.value = true
}

function openEdit(record: CategoryItem) {
  modalMode.value = 'edit'
  editingId.value = record.id
  formState.name = record.name
  formState.code = record.code
  modalOpen.value = true
}

async function onModalSubmit() {
  if (!formState.name || !formState.code) {
    message.warning('请填写完整信息')
    return
  }

  modalSubmitting.value = true
  try {
    if (modalMode.value === 'create') {
      await http.post('/products/categories', formState)
      message.success('新增成功')
    } else {
      await http.put(`/products/categories/${editingId.value}`, formState)
      message.success('更新成功')
    }
    modalOpen.value = false
    void fetchList()
  } catch (e: any) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '提交失败')
  } finally {
    modalSubmitting.value = false
  }
}

async function onDelete(id: number) {
  try {
    await http.delete(`/products/categories/${id}`)
    message.success('删除成功')
    void fetchList()
  } catch (e: any) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '删除失败')
  }
}

onMounted(() => {
  void fetchList()
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
      title="产品类型管理"
      subtitle="管理产品分类与编码 · /products/categories"
      :loading="loading"
      @refresh="fetchList"
      @logout="onLogout"
    />

    <main class="pa-list-wrap">
      <a-card class="pa-list-card" :bordered="false">
        <template #title>
          <div class="pa-list-card-title">
            <span>类型列表</span>
            <span class="pa-list-card-sub">共 {{ items.length }} 条</span>
          </div>
        </template>

        <template #extra>
          <a-space>
            <a-button type="primary" @click="openCreate">新增类型</a-button>
          </a-space>
        </template>

        <a-table
          :columns="columns"
          :data-source="items"
          :loading="loading"
          :pagination="false"
          row-key="id"
          :scroll="{ x: '100%' }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
                <a-popconfirm title="确定删除吗？" @confirm="onDelete(record.id)">
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </main>

    <a-modal
      v-model:open="modalOpen"
      :title="modalMode === 'create' ? '新增产品类型' : '编辑产品类型'"
      :confirm-loading="modalSubmitting"
      @ok="onModalSubmit"
      ok-text="保存"
    >
      <a-form layout="vertical" style="padding-top: 12px">
        <a-form-item label="类型名称" required>
          <a-input v-model:value="formState.name" placeholder="请输入类型名称" />
        </a-form-item>
        <a-form-item label="类型编码" required>
          <a-input v-model:value="formState.code" placeholder="请输入类型编码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
