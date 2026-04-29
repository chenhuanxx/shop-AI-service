<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { TablePaginationConfig, TableProps } from 'ant-design-vue'
import HomeHeader from '../../components/HomeHeader.vue'
import { http } from '../../services/http'
import { useAuthStore } from '../../stores/auth'
import { getUploadUrl, getFileUrl } from '../../utils/env'

type BannerItem = {
  id: number
  title: string
  imageUrl: string
  linkUrl: string
  sortOrder: number
  enabled: boolean
  createdAt: string
  updatedAt: string
}

type BannerPage = {
  content: BannerItem[]
  totalElements: number
  size: number
  number: number
}

const authStore = useAuthStore()
const router = useRouter()

const loading = ref(false)
const items = ref<BannerItem[]>([])
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const uploadUrl = getUploadUrl()
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('access_token')}`,
}))

const modalOpen = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const modalSubmitting = ref(false)
const editingId = ref<number | null>(null)
const previewVisible = ref(false)
const previewUrl = ref('')

const formState = reactive({
  title: '',
  imageUrl: '',
  linkUrl: '',
  sortOrder: 0,
  enabled: true,
})

const rangeText = computed(() => {
  if (total.value <= 0) return '0 - 0'
  const start = (page.value - 1) * pageSize.value + 1
  const end = Math.min(total.value, page.value * pageSize.value)
  return `${start} - ${end}`
})

const columns: NonNullable<TableProps['columns']> = [
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '图片', dataIndex: 'imageUrl', key: 'imageUrl', width: 120 },
  { title: '标题', dataIndex: 'title', key: 'title', width: 180, ellipsis: true },
  { title: '链接', dataIndex: 'linkUrl', key: 'linkUrl', width: 200, ellipsis: true },
  { title: '状态', dataIndex: 'enabled', key: 'enabled', width: 100 },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    key: 'createdAt',
    width: 180,
    customRender: ({ text }) => {
      if (!text) return '-'
      return new Date(text).toLocaleString('zh-CN')
    },
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
    const res = await http.get<BannerPage>('/banners', {
      params: {
        page: page.value,
        page_size: pageSize.value,
        keyword: keyword.value.trim() || undefined,
      },
    })
    const data = res.data as any
    items.value = data.content || []
    total.value = data.totalElements || 0
  } catch (e: any) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '获取列表失败')
  } finally {
    loading.value = false
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
  page.value = p.current || 1
  pageSize.value = p.pageSize || 10
  void fetchList()
}

function openCreate() {
  modalMode.value = 'create'
  editingId.value = null
  Object.assign(formState, {
    title: '',
    imageUrl: '',
    linkUrl: '',
    sortOrder: 0,
    enabled: true,
  })
  modalOpen.value = true
}

function openEdit(record: BannerItem) {
  modalMode.value = 'edit'
  editingId.value = record.id
  Object.assign(formState, {
    title: record.title || '',
    imageUrl: record.imageUrl || '',
    linkUrl: record.linkUrl || '',
    sortOrder: record.sortOrder || 0,
    enabled: record.enabled ?? true,
  })
  modalOpen.value = true
}

async function onModalSubmit() {
  if (!formState.imageUrl) {
    message.warning('请上传 Banner 图片')
    return
  }

  modalSubmitting.value = true
  try {
    if (modalMode.value === 'create') {
      await http.post('/banners', formState)
      message.success('新增成功')
    } else {
      await http.put(`/banners/${editingId.value}`, formState)
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
    await http.delete(`/banners/${id}`)
    message.success('删除成功')
    void fetchList()
  } catch (e: any) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '删除失败')
  }
}

function handleImageChange(info: any) {
  if (info.file.status === 'done') {
    formState.imageUrl = info.file.response.url
    message.success('图片上传成功')
  } else if (info.file.status === 'error') {
    message.error('图片上传失败')
  }
}

function handlePreview(file: any) {
  previewUrl.value = file.url || file.response?.url || formState.imageUrl
  previewVisible.value = true
}

function beforeUpload(file: File) {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/jpg' || file.type === 'image/gif'
  if (!isImage) {
    message.error('只能上传 JPG/PNG/GIF 文件!')
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    message.error('图片大小不能超过 5 MB!')
  }
  return isImage && isLt5M
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
      title="Banner 管理"
      subtitle="管理首页轮播图 · /banners"
      :loading="loading"
      @refresh="fetchList"
      @logout="onLogout"
    />

    <main class="pa-list-wrap">
      <a-card class="pa-list-card" :bordered="false">
        <template #title>
          <div class="pa-list-card-title">
            <span>Banner 列表</span>
            <span class="pa-list-card-sub">共 {{ total }} 条 · 显示 {{ rangeText }}</span>
          </div>
        </template>

        <template #extra>
          <a-space wrap>
            <a-input-search
              v-model:value="keyword"
              placeholder="搜索标题"
              style="width: 200px"
              allow-clear
              @search="onSearch"
            />
            <a-button @click="onSearch" :loading="loading">查询</a-button>
            <a-button @click="onReset" :disabled="loading">重置</a-button>
            <a-button type="primary" @click="openCreate">
              <template #icon><PlusOutlined /></template>
              新增 Banner
            </a-button>
          </a-space>
        </template>

        <a-table
          :columns="columns"
          :data-source="items"
          :loading="loading"
          :pagination="{
            current: page,
            pageSize: pageSize,
            total: total,
            showSizeChanger: true,
            showQuickJumper: true,
          }"
          row-key="id"
          :scroll="{ x: '100%' }"
          @change="onTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'imageUrl'">
              <a-image
                v-if="record.imageUrl"
                :src="getFileUrl(record.imageUrl)"
                :width="80"
                :height="40"
                style="object-fit: cover; border-radius: 4px; cursor: pointer"
                :preview="{
                  src: getFileUrl(record.imageUrl),
                }"
                fallback="/placeholder.png"
              />
              <span v-else style="color: #999">暂无图片</span>
            </template>
            <template v-else-if="column.key === 'linkUrl'">
              <a-tooltip v-if="record.linkUrl" :title="record.linkUrl">
                <a :href="record.linkUrl" target="_blank" style="color: #1890ff">
                  {{ record.linkUrl.substring(0, 30) }}{{ record.linkUrl.length > 30 ? '...' : '' }}
                </a>
              </a-tooltip>
              <span v-else style="color: #999">-</span>
            </template>
            <template v-else-if="column.key === 'enabled'">
              <a-tag :color="record.enabled ? 'green' : 'red'">
                {{ record.enabled ? '启用' : '禁用' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'action'">
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

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalOpen"
      :title="modalMode === 'create' ? '新增 Banner' : '编辑 Banner'"
      :confirm-loading="modalSubmitting"
      width="600px"
      @ok="onModalSubmit"
      ok-text="保存"
    >
      <a-form layout="vertical" style="padding-top: 12px">
        <a-form-item label="Banner 标题" help="选填，用于后台识别">
          <a-input v-model:value="formState.title" placeholder="请输入 Banner 标题" />
        </a-form-item>

        <a-form-item label="Banner 图片" required>
          <a-upload
            name="file"
            list-type="picture-card"
            class="banner-uploader"
            :show-upload-list="false"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :before-upload="beforeUpload"
            @change="handleImageChange"
            @preview="handlePreview"
          >
            <div v-if="formState.imageUrl">
              <img :src="getFileUrl(formState.imageUrl)" alt="banner" style="width: 100%; height: 100%; object-fit: cover" />
            </div>
            <div v-else>
              <plus-outlined />
              <div style="margin-top: 8px">上传图片</div>
            </div>
          </a-upload>
          <div style="color: #999; font-size: 12px; margin-top: 4px">建议尺寸 750x350 像素，支持 JPG/PNG/GIF 格式</div>
        </a-form-item>

        <a-form-item label="跳转链接" help="选填，点击 Banner 后跳转的页面地址">
          <a-input v-model:value="formState.linkUrl" placeholder="请输入跳转链接，如：/pages/product/detail?id=1" />
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="排序">
              <a-input-number v-model:value="formState.sortOrder" :min="0" :max="9999" style="width: 100%" />
              <div style="color: #999; font-size: 12px; margin-top: 4px">数字越小越靠前</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="启用状态">
              <a-switch v-model:checked="formState.enabled" checked-children="启用" un-checked-children="禁用" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 图片预览 -->
    <a-modal :open="previewVisible" :footer="null" @cancel="previewVisible = false">
      <img v-if="previewUrl" :src="previewUrl" alt="preview" style="width: 100%" />
    </a-modal>
  </div>
</template>

<style scoped lang="less">
.banner-uploader {
  :deep(.ant-upload-select-picture-card) {
    width: 300px;
    height: 150px;
    margin-bottom: 8px;
    background-color: #fafafa;
    border: 1px dashed #d9d9d9;
    border-radius: 8px;
    cursor: pointer;
    transition: border-color 0.3s;

    &:hover {
      border-color: #1890ff;
    }

    img {
      object-fit: cover;
      border-radius: 8px;
    }
  }
}
</style>
