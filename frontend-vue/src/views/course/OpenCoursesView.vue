<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { message } from 'ant-design-vue'
import type { TablePaginationConfig, TableProps } from 'ant-design-vue'

import HomeHeader from '../../components/HomeHeader.vue'
import { http } from '../../services/http'
import { useAuthStore } from '../../stores/auth'
import { formatDateTime, formatDate } from '../../utils/date'

type ApiResponse<T> = {
  code: number
  message: string
  data: T
}

type ApplicationItem = {
  id: string
  category: string
  academicYear: string
  term: string
  department: string
  className: string
  office: string
  location: string
  courseName: string
  topic: string
  startDate: string
  weekNo: number
  weekday: string
  section: string
  applyReason: string
  state: string
  createdAt: string
  userId: number
  auditOpinion: string
  auditedAt: string
  enrolledCount?: number
  checkedInCount?: number
}

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const keyword = ref('')
const statusFilter = ref<'all' | '待审批' | '同意' | '驳回'>('all')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const items = ref<ApplicationItem[]>([])
const loading = ref(false)

const isAdmin = computed(() => (authStore.user?.role || authStore.user?.username || '').toLowerCase() === 'admin')
const isPublishList = computed(() => route.path === '/open-courses-publish')
const pageTitle = computed(() => (isPublishList.value ? '公开课发布' : '公开课审批'))
const pageSubtitle = computed(() =>
  isPublishList.value ? '发布列表（已通过） · /open-courses-publish' : '审批列表 · /public-course/applications'
)

const rangeText = computed(() => {
  if (total.value <= 0) return '0 - 0'
  const start = (page.value - 1) * pageSize.value + 1
  const end = Math.min(total.value, page.value * pageSize.value)
  return `${start} - ${end}`
})

const stateText = (s: string) => {
  const raw = (s || '').trim()
  if (raw === '已通过') return '同意'
  if (raw === '已拒绝') return '驳回'
  if (raw === 'approve') return '同意'
  if (raw === 'reject') return '驳回'
  if (raw === '待审批' || raw === 'pending') return '待审批'
  return raw || '待审批'
}

const columns = computed<NonNullable<TableProps['columns']>>(() => {
  const cols: NonNullable<TableProps['columns']> = [
    { title: '课程名称', dataIndex: 'courseName', key: 'courseName', width: 160, ellipsis: true },
    {
      title: '申请时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 210,
      customRender: ({ text }) => formatDateTime(text),
    },
    { title: '学年', dataIndex: 'academicYear', key: 'academicYear', width: 120 },
    { title: '学期', dataIndex: 'term', key: 'term', width: 110 },
    { title: '类别', dataIndex: 'category', key: 'category', width: 110 },
    { title: '系部', dataIndex: 'department', key: 'department', width: 140, ellipsis: true },
    { title: '班级', dataIndex: 'className', key: 'className', width: 170, ellipsis: true },
    { title: '课题', dataIndex: 'topic', key: 'topic', width: 160, ellipsis: true },
    {
      title: '时间',
      key: 'time',
      width: 220,
      customRender: ({ record }) => `${formatDate(record.startDate)} · 第${record.weekNo}周 · ${record.weekday} · ${record.section}`,
    },
    {
      title: '状态',
      key: 'state',
      width: 110,
      customRender: ({ record }) => stateText(record.state),
    },
    {
      title: '审批时间',
      key: 'auditedAt',
      width: 170,
      customRender: ({ record }) => (stateText(record.state) === '待审批' ? '—' : record.auditedAt || '—'),
    },
    {
      title: '审批意见',
      dataIndex: 'auditOpinion',
      key: 'auditOpinion',
      width: 220,
      ellipsis: true,
      customRender: ({ record }) => (stateText(record.state) === '待审批' ? '—' : record.auditOpinion || '—'),
    },
    {
      title: '操作',
      key: 'action',
      width: 190,
      fixed: 'right',
      customRender: ({ record }) => record,
    },
  ]
  if (isAdmin.value) {
    cols.splice(1, 0, { title: '申请人ID', dataIndex: 'userId', key: 'userId', width: 110 })
  }
  return cols
})

async function onLogout() {
  authStore.logout()
  await router.replace('/login')
}

async function fetchList() {
  loading.value = true
  try {
    const stateParam =
      statusFilter.value === '待审批'
        ? '待审批'
        : statusFilter.value === '同意'
          ? '已通过'
          : statusFilter.value === '驳回'
            ? '已拒绝'
            : undefined
    const res = await http.get<ApiResponse<{ list: ApplicationItem[]; total: number }>>('/public-course/applications/page', {
      params: {
        scope: isPublishList.value ? 'publish' : undefined,
        page: page.value,
        page_size: pageSize.value,
        keyword: keyword.value.trim() || undefined,
        state: stateParam,
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

function onSearch() {
  page.value = 1
  void fetchList()
}

function onReset() {
  keyword.value = ''
  statusFilter.value = isPublishList.value ? '同意' : 'all'
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

const detailOpen = ref(false)
const detailLoading = ref(false)
const current = ref<ApplicationItem | null>(null)
const opinion = ref('')

function openPublish(item: ApplicationItem) {
  current.value = item
  opinion.value = ''
  detailOpen.value = true
}

function openDetail(item: ApplicationItem) {
  current.value = item
  opinion.value = item.auditOpinion || ''
  detailOpen.value = true
}

async function submitAudit(action: 'approve' | 'reject') {
  if (!current.value) return
  detailLoading.value = true
  try {
    const res = await http.post<ApiResponse<boolean>>('/public-course/applications/audit', {
      id: current.value.id,
      action,
      opinion: opinion.value.trim() || '',
    })
    if (res.data.code !== 0) {
      message.error(res.data.message || '操作失败')
      return
    }
    message.success(action === 'approve' ? '已同意' : '已驳回')
    detailOpen.value = false
    await fetchList()
  } catch (e) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '操作失败')
  } finally {
    detailLoading.value = false
  }
}

onMounted(async () => {
  if (isPublishList.value) {
    statusFilter.value = '同意'
  }
  await fetchList()
})

onUnmounted(() => {
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
      :title="pageTitle"
      :subtitle="pageSubtitle"
      :loading="loading"
      @refresh="fetchList"
      @logout="onLogout"
    />

    <main class="pa-list-wrap">
      <a-card class="pa-list-card" :bordered="false">
        <template #title>
          <div class="pa-list-card-title">
            <span>公开课审批列表</span>
            <span class="pa-list-card-sub">共 {{ total }} 条 · 显示 {{ rangeText }}</span>
          </div>
        </template>

        <template #extra>
          <a-space wrap>
            <a-segmented
              v-model:value="statusFilter"
              :options="[
                { label: '全部', value: 'all' },
                { label: '待审批', value: '待审批' },
                { label: '同意', value: '同意' },
                { label: '驳回', value: '驳回' },
              ]"
              :disabled="isPublishList"
              @change="
                () => {
                  page = 1
                  fetchList()
                }
              "
            />
            <a-input
              v-model:value="keyword"
              placeholder="按课程名称/课题/班级搜索"
              allow-clear
              :loading="loading"
              style="width: 280px"
            />
            <a-button @click="onSearch" :disabled="loading">刷新</a-button>
            <a-button @click="onReset" :disabled="loading">重置</a-button>
          </a-space>
        </template>

        <div class="pa-table-scroll">
          <a-table
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
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" @click="openDetail(record)">详情</a-button>
                  <a-button
                    v-if="isAdmin && stateText(record.state) === '待审批'"
                    type="link"
                    @click="openPublish(record)"
                  >
                    审批
                  </a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </a-card>

      <a-modal
        v-model:open="detailOpen"
        title="公开课详情"
        :confirm-loading="detailLoading"
        width="900px"
        @cancel="detailOpen = false"
      >
        <a-descriptions v-if="current" bordered size="small" :column="{ xs: 1, sm: 2, md: 2 }">
          <a-descriptions-item label="申请ID">{{ current.id }}</a-descriptions-item>
          <a-descriptions-item label="申请时间">{{ formatDateTime(current.createdAt) }}</a-descriptions-item>
          <a-descriptions-item label="类别">{{ current.category }}</a-descriptions-item>
          <a-descriptions-item label="学年/学期">{{ current.academicYear }} / {{ current.term }}</a-descriptions-item>
          <a-descriptions-item label="系部">{{ current.department }}</a-descriptions-item>
          <a-descriptions-item label="班级">{{ current.className }}</a-descriptions-item>
          <a-descriptions-item label="教研室">{{ current.office }}</a-descriptions-item>
          <a-descriptions-item label="地点">{{ current.location }}</a-descriptions-item>
          <a-descriptions-item label="课程名称">{{ current.courseName }}</a-descriptions-item>
          <a-descriptions-item label="课题">{{ current.topic }}</a-descriptions-item>
          <a-descriptions-item label="开课安排">
            {{ formatDate(current.startDate) }} · 第{{ current.weekNo }}周 · {{ current.weekday }} · {{ current.section }}
          </a-descriptions-item>
          <a-descriptions-item label="预约/签到">
            <span style="color: #1890ff; font-weight: 600;">{{ current.enrolledCount || 0 }}</span>
            /
            <span style="color: #52c41a; font-weight: 600;">{{ current.checkedInCount || 0 }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="状态">{{ stateText(current.state) }}</a-descriptions-item>
          <a-descriptions-item label="审批时间">{{ formatDateTime(current.auditedAt) || '—' }}</a-descriptions-item>
          <a-descriptions-item label="申报理由" :span="2">
            <div style="white-space: pre-wrap">{{ current.applyReason }}</div>
          </a-descriptions-item>
          <a-descriptions-item label="审批意见" :span="2">
            <div style="white-space: pre-wrap">{{ current.auditOpinion || '—' }}</div>
          </a-descriptions-item>
        </a-descriptions>

        <a-divider v-if="isAdmin && current && stateText(current.state) === '待审批'" />

        <a-form v-if="isAdmin && current && stateText(current.state) === '待审批'" layout="vertical">
          <a-form-item label="审批意见">
            <a-textarea v-model:value="opinion" :rows="4" placeholder="请输入审批意见" />
          </a-form-item>
        </a-form>

        <template #footer>
          <a-space>
            <a-button @click="detailOpen = false">关闭</a-button>
            <a-button
              v-if="isAdmin && current && stateText(current.state) === '待审批'"
              :loading="detailLoading"
              danger
              @click="submitAudit('reject')"
            >
              驳回
            </a-button>
            <a-button
              v-if="isAdmin && current && stateText(current.state) === '待审批'"
              type="primary"
              :loading="detailLoading"
              @click="submitAudit('approve')"
            >
              同意
            </a-button>
          </a-space>
        </template>
      </a-modal>
    </main>
  </div>
</template>
