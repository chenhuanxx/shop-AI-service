<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { TablePaginationConfig, TableProps, UploadFile } from 'ant-design-vue'
import HomeHeader from '../../components/HomeHeader.vue'
import { http } from '../../services/http'
import { useAuthStore } from '../../stores/auth'
import { formatDateTime } from '../../utils/date'
import { getUploadUrl, getFileUrl } from '../../utils/env'

type CategoryItem = { id: number; name: string }
type ProductItem = {
  id: number
  name: string
  thumbnail: string
  sort: number
  status: boolean
  description: string
  poster: string
  detailImages: string
  officialPrice: number
  discount: number
  actualPrice: number
  category?: CategoryItem
  createdAt: string
}

type ProductPage = {
  content: ProductItem[]
  totalElements: number
  size: number
  number: number
}

const authStore = useAuthStore()
const router = useRouter()

const loading = ref(false)
const items = ref<ProductItem[]>([])
const categories = ref<CategoryItem[]>([])
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

function handleThumbnailChange(info: any) {
  if (info.file.status === 'done') {
    formState.thumbnail = info.file.response.url
    message.success('缩略图上传成功')
  } else if (info.file.status === 'error') {
    message.error('缩略图上传失败')
  }
}

function handlePosterChange(info: any) {
  if (info.file.status === 'done') {
    formState.poster = info.file.response.url
    message.success('宣传图上传成功')
  } else if (info.file.status === 'error') {
    message.error('宣传图上传失败')
  }
}

function handleDetailImageChange({ file }: any) {
  if (file.status === 'done') {
    const url = file.response?.url
    if (url) {
      if (!formState.detailImages) {
        formState.detailImages = url
      } else {
        formState.detailImages = formState.detailImages + ',' + url
      }
      message.success('详情图上传成功')
    }
  } else if (file.status === 'error') {
    message.error('详情图上传失败')
  } else if (file.status === 'removed') {
    // 处理删除
    const url = file.url || file.response?.url
    if (url && formState.detailImages) {
      const urls = formState.detailImages.split(',').filter((u: string) => u !== url)
      formState.detailImages = urls.join(',')
    }
  }
}

const detailImageList = ref<any[]>([])

function beforeUpload(file: File) {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/jpg'
  if (!isJpgOrPng) {
    message.error('只能上传 JPG/PNG/JPEG 文件!')
  }
  const isLt2M = file.size / 1024 / 1024 < 10
  if (!isLt2M) {
    message.error('图片大小不能超过 10 MB!')
  }
  return isJpgOrPng && isLt2M
}

const formState = reactive({
  name: '',
  thumbnail: '',
  sort: 0,
  status: true,
  description: '',
  poster: '',
  detailImages: '',
  officialPrice: 0,
  discount: 0,
  categoryId: undefined as number | undefined,
})

const actualPrice = computed(() => {
  return (formState.officialPrice || 0) - (formState.discount || 0)
})

const rangeText = computed(() => {
  if (total.value <= 0) return '0 - 0'
  const start = (page.value - 1) * pageSize.value + 1
  const end = Math.min(total.value, page.value * pageSize.value)
  return `${start} - ${end}`
})

const columns: NonNullable<TableProps['columns']> = [
  { title: '产品名称', dataIndex: 'name', key: 'name', width: 200, ellipsis: true },
  { title: '缩略图', dataIndex: 'thumbnail', key: 'thumbnail', width: 100 },
  { title: '宣传图', dataIndex: 'poster', key: 'poster', width: 100 },
  { title: '类型', dataIndex: ['category', 'name'], key: 'category' },
  { title: '价格 (官方/实际)', key: 'price', width: 150 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    key: 'createdAt',
    width: 180,
    customRender: ({ text }) => formatDateTime(text),
  },
  { title: '操作', key: 'action', width: 150, fixed: 'right' },
]

async function onLogout() {
  authStore.logout()
  await router.replace('/login')
}

async function fetchCategories() {
  try {
    const res = await http.get<CategoryItem[]>('/products/categories')
    categories.value = res.data || []
  } catch {}
}

async function fetchList() {
  loading.value = true
  try {
    const res = await http.get<ProductPage>('/products', {
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
    name: '',
    thumbnail: '',
    sort: 0,
    status: true,
    description: '',
    poster: '',
    detailImages: '',
    officialPrice: 0,
    discount: 0,
    categoryId: undefined,
  })
  detailImageList.value = []
  modalOpen.value = true
}

function openEdit(record: ProductItem) {
  modalMode.value = 'edit'
  editingId.value = record.id
  Object.assign(formState, {
    name: record.name,
    thumbnail: record.thumbnail,
    sort: record.sort,
    status: record.status,
    description: record.description,
    poster: record.poster,
    detailImages: record.detailImages || '',
    officialPrice: record.officialPrice,
    discount: record.discount,
    categoryId: record.category?.id,
  })
  // 同步详情图列表
  const images = record.detailImages || ''
  detailImageList.value = images ? images.split(',').filter((u: string) => u).map((url: string, index: number) => ({
    uid: -index - 1,
    name: `image-${index}.png`,
    status: 'done',
    url: import.meta.env.VITE_API_BASE_URL + url,
  })) : []
  modalOpen.value = true
}

async function onModalSubmit() {
  if (!formState.name) {
    message.warning('请填写产品名称')
    return
  }

  modalSubmitting.value = true
  try {
    if (modalMode.value === 'create') {
      await http.post('/products', formState)
      message.success('新增成功')
    } else {
      await http.put(`/products/${editingId.value}`, formState)
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
    await http.delete(`/products/${id}`)
    message.success('删除成功')
    void fetchList()
  } catch (e: any) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '删除失败')
  }
}

const openImageViewer = (file: UploadFile)=>{
   { window.open(file.url || file.response?.url, '_blank') }
}

onMounted(() => {
  void fetchCategories()
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
      title="产品管理"
      subtitle="管理产品列表、价格与状态 · /products"
      :loading="loading"
      @refresh="fetchList"
      @logout="onLogout"
    />

    <main class="pa-list-wrap">
      <a-card class="pa-list-card" :bordered="false">
        <template #title>
          <div class="pa-list-card-title">
            <span>产品列表</span>
            <span class="pa-list-card-sub">共 {{ total }} 条 · 显示 {{ rangeText }}</span>
          </div>
        </template>

          <template #extra>
          <a-space wrap>
            <a-input-search
              v-model:value="keyword"
              placeholder="搜索产品名称"
              style="width: 240px"
              allow-clear
              @search="onSearch"
            />
            <a-button @click="onSearch" :loading="loading">查询</a-button>
            <a-button @click="onReset" :disabled="loading">重置</a-button>
            <a-button type="primary" @click="openCreate">新增产品</a-button>
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
            <template v-if="column.key === 'thumbnail'">
              <a-image :src="getFileUrl(record.thumbnail)" :width="40" fallback="/placeholder.png" />
            </template>
            <template v-else-if="column.key === 'poster'">
              <a-image :src="getFileUrl(record.poster)" :width="40" fallback="/placeholder.png" />
            </template>
            <template v-else-if="column.key === 'price'">
              <div>官方: ¥{{ record.officialPrice }}</div>
              <div style="color: #ff4d4f; font-weight: bold">实际: ¥{{ record.actualPrice }}</div>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="record.status ? 'green' : 'red'">
                {{ record.status ? '已上架' : '已下架' }}
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

    <a-modal
      v-model:open="modalOpen"
      :title="modalMode === 'create' ? '新增产品' : '编辑产品'"
      :confirm-loading="modalSubmitting"
      width="700px"
      @ok="onModalSubmit"
      ok-text="保存"
    >
      <a-form layout="vertical" style="padding-top: 12px">
        <a-row :gutter="16">
          <a-col :span="16">
            <a-form-item label="产品名称" required>
              <a-input v-model:value="formState.name" placeholder="请输入产品名称" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="产品类型">
              <a-select v-model:value="formState.categoryId" placeholder="选择类型" allow-clear>
                <a-select-option v-for="c in categories" :key="c.id" :value="c.id">
                  {{ c.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="产品缩略图">
              <a-upload
                name="file"
                list-type="picture-card"
                class="avatar-uploader"
                :show-upload-list="false"
                :action="uploadUrl"
                :headers="uploadHeaders"
                :before-upload="beforeUpload"
                @change="handleThumbnailChange"
              >
                <img v-if="formState.thumbnail" :src="getFileUrl(formState.thumbnail)" alt="thumbnail" style="width: 100%" />
                <div v-else>
                  <plus-outlined />
                  <div style="margin-top: 8px">上传缩略图</div>
                </div>
              </a-upload>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="宣传大图">
              <a-upload
                name="file"
                list-type="picture-card"
                class="avatar-uploader"
                :show-upload-list="false"
                :action="uploadUrl"
                :headers="uploadHeaders"
                :before-upload="beforeUpload"
                @change="handlePosterChange"
              >
                <img v-if="formState.poster" :src="getFileUrl(formState.poster)" alt="poster" style="width: 100%" />
                <div v-else>
                  <plus-outlined />
                  <div style="margin-top: 8px">上传宣传图</div>
                </div>
              </a-upload>
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="产品详情图（可上传多张）"> 
          <a-upload
            name="file"
            list-type="picture-card"
            v-model:file-list="detailImageList"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :before-upload="beforeUpload"
            multiple
            @change="handleDetailImageChange"
            @preview="(file: any) => openImageViewer(file)"
          >
            <div v-if="detailImageList.length < 10">
              <plus-outlined />
              <div style="margin-top: 8px">上传详情图</div>
            </div>
          </a-upload>
          <div style="color: #999; font-size: 12px; margin-top: 4px">最多可上传10张详情图，点击图片可预览</div>
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="官方价格">
              <a-input-number v-model:value="formState.officialPrice" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="折扣金额">
              <a-input-number v-model:value="formState.discount" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="实际价格 (计算)">
              <a-input :value="actualPrice" disabled style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="排序">
              <a-input-number v-model:value="formState.sort" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="上架状态">
              <a-switch v-model:checked="formState.status" checked-children="上架" un-checked-children="下架" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="产品描述">
          <a-textarea v-model:value="formState.description" :rows="4" placeholder="请输入产品描述" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped lang="less">
.avatar-uploader {
  :deep(.ant-upload-select-picture-card) {
    width: 128px;
    height: 128px;
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
      height: 98px;
    }
  }
}
</style>
