<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import QrcodeVue from 'qrcode.vue'

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
const router = useRouter()

const keyword = ref('')
const statusFilter = ref<'all' | '同意' | '已发布'>('all')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const items = ref<ApplicationItem[]>([])
const loading = ref(false)

// 统计数据
const stats = computed(() => {
  const pending = items.value.filter(i => stateText(i.state) === '待发布').length
  const published = items.value.filter(i => stateText(i.state) === '已发布').length
  const totalEnrolled = items.value.reduce((sum, i) => sum + (i.enrolledCount || 0), 0)
  const totalCheckedIn = items.value.reduce((sum, i) => sum + (i.checkedInCount || 0), 0)
  return { total: total.value, pending, published, totalEnrolled, totalCheckedIn }
})

const isAdmin = computed(() => (authStore.user?.role || authStore.user?.username || '').toLowerCase() === 'admin')
const pageTitle = computed(() => '公开课发布')
const pageSubtitle = computed(() => '发布列表（已通过） · /open-publish')

const rangeText = computed(() => {
  if (total.value <= 0) return '0 - 0'
  const start = (page.value - 1) * pageSize.value + 1
  const end = Math.min(total.value, page.value * pageSize.value)
  return `${start} - ${end}`
})

const stateText = (s: string) => {
  const raw = (s || '').trim()
  if (raw === '已通过') return '同意'
  if (raw === '已发布') return '已发布'
  if (raw === '已拒绝') return '驳回'
  if (raw === 'approve') return '同意'
  if (raw === 'published') return '已发布'
  if (raw === 'reject') return '驳回'
  if (raw === '待审批' || raw === 'pending') return '待审批'
  return raw || '待审批'
}

// 批量选择
const selectedRowKeys = ref<string[]>([])
const batchLoading = ref(false)

const columns = computed<NonNullable<TableProps['columns']>>(() => {
  const cols: NonNullable<TableProps['columns']> = [
    { title: '课程名称', dataIndex: 'courseName', key: 'courseName', width: 150, ellipsis: true },
    {
      title: '发布时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      customRender: ({ text }) => formatDateTime(text),
    },
    { title: '学年', dataIndex: 'academicYear', key: 'academicYear', width: 110 },
    { title: '学期', dataIndex: 'term', key: 'term', width: 100 },
    { title: '类别', dataIndex: 'category', key: 'category', width: 100 },
    { title: '系部', dataIndex: 'department', key: 'department', width: 130, ellipsis: true },
    { title: '班级', dataIndex: 'className', key: 'className', width: 160, ellipsis: true },
    { title: '课题', dataIndex: 'topic', key: 'topic', width: 150, ellipsis: true },
    {
      title: '时间',
      key: 'time',
      width: 200,
      customRender: ({ record }) => `${formatDate(record.startDate)} · 第${record.weekNo}周 · ${record.weekday} · ${record.section}`,
    },
    {
      title: '预约/签到',
      key: 'enrolled',
      width: 100,
      align: 'center',
      customRender: ({ record }) => {
        const enrolled = record.enrolledCount || 0
        const checkedIn = record.checkedInCount || 0
        return `${enrolled}/${checkedIn}`
      }
    },
    {
      title: '状态',
      key: 'state',
      width: 100,
      customRender: ({ record }) => stateText(record.state),
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right',
      customRender: ({ record }) => record,
    },
  ]
  if (isAdmin.value) {
    cols.splice(1, 0, { title: '申请人ID', dataIndex: 'userId', key: 'userId', width: 100 })
  }
  return cols
})

const rowSelection = computed(() => isAdmin.value ? {
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: string[]) => { selectedRowKeys.value = keys },
  getCheckboxProps: (record: ApplicationItem) => ({
    disabled: stateText(record.state) !== '同意', // 待发布 = 已通过 = 同意
    name: record.id,
  })
} : undefined)

async function onLogout() {
  authStore.logout()
  await router.replace('/login')
}

async function fetchList() {
  loading.value = true
  try {
    const stateParam =
      statusFilter.value === '同意' ? '已通过' : statusFilter.value === '已发布' ? '已发布' : undefined
    const res = await http.get<ApiResponse<{ list: ApplicationItem[]; total: number }>>('/public-course/applications/page', {
      params: {
        scope: 'publish',
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
    selectedRowKeys.value = []
  } catch (e) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '请求失败')
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 批量发布
async function batchPublish() {
  if (!selectedRowKeys.value.length) {
    message.warning('请先选择要发布的课程')
    return
  }
  batchLoading.value = true
  try {
    const res = await http.post<ApiResponse<{ successCount: number }>>('/public-course/applications/batch-publish', {
      ids: selectedRowKeys.value
    })
    if (res.data.code !== 0) {
      message.error(res.data.message || '批量发布失败')
      return
    }
    message.success(`成功发布 ${res.data.data?.successCount || selectedRowKeys.value.length} 门课程`)
    selectedRowKeys.value = []
    await fetchList()
  } catch (e) {
    message.error('批量发布失败')
  } finally {
    batchLoading.value = false
  }
}

function onSearch() {
  page.value = 1
  void fetchList()
}

function onReset() {
  keyword.value = ''
  statusFilter.value = 'all'
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

const qrModalOpen = ref(false)
const qrLoading = ref(false)
const qrBookingUrl = ref('')
const qrCheckinUrl = ref('')
const qrReviewUrl = ref('')
let checkinTimer: number | null = null

function openDetail(item: ApplicationItem) {
  current.value = item
  opinion.value = item.auditOpinion || ''
  detailOpen.value = true
}

async function fetchQrCodes(id: string) {
  qrLoading.value = true
  try {
    const res = await http.get<ApiResponse<{ bookingUrl: string; checkinUrl: string; reviewUrl: string }>>('/public-course/applications/qrcodes', {
      params: { id },
    })
    if (res.data.code === 0 && res.data.data) {
      qrBookingUrl.value = res.data.data.bookingUrl
      qrCheckinUrl.value = res.data.data.checkinUrl
      qrReviewUrl.value = res.data.data.reviewUrl
    } else {
      message.error(res.data.message || '获取二维码失败')
    }
  } catch (e) {
    message.error('获取二维码失败')
  } finally {
    qrLoading.value = false
  }
}

function openQrCode(item: ApplicationItem) {
  if (stateText(item.state) !== '已发布') {
    message.warning('请先发布后再查看二维码')
    return
  }
  current.value = item
  qrBookingUrl.value = ''
  qrCheckinUrl.value = ''
  qrReviewUrl.value = ''
  qrModalOpen.value = true
  fetchQrCodes(item.id)
  
  // Set up dynamic check-in QR code refresh every 10 seconds
  if (checkinTimer) clearInterval(checkinTimer)
  checkinTimer = window.setInterval(() => {
    if (qrModalOpen.value && current.value) {
      fetchQrCodes(current.value.id)
    }
  }, 10000)
}

function closeQrModal() {
  qrModalOpen.value = false
  if (checkinTimer) {
    clearInterval(checkinTimer)
    checkinTimer = null
  }
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

async function publishCourse(item: ApplicationItem) {
  if (!isAdmin.value) return
  if (stateText(item.state) !== '同意') {
    message.warning('只有已通过的课程才能发布')
    return
  }
  loading.value = true
  try {
    const res = await http.post<ApiResponse<boolean>>('/public-course/applications/publish', { id: item.id })
    if (res.data.code !== 0) {
      message.error(res.data.message || '发布失败')
      return
    }
    message.success('已发布')
    await fetchList()
  } catch (e) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '发布失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchList()
})

onUnmounted(() => {
  if (checkinTimer) {
    clearInterval(checkinTimer)
    checkinTimer = null
  }
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
      <!-- 统计卡片 -->
      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col :xs="12" :sm="6">
          <a-card size="small" :bordered="false" class="stat-card">
            <a-statistic title="课程总数" :value="stats.total" :value-style="{ color: '#1890ff' }" />
          </a-card>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-card size="small" :bordered="false" class="stat-card">
            <a-statistic title="待发布" :value="stats.pending" :value-style="{ color: '#faad14' }">
              <template #suffix><span style="font-size: 14px">/ 待审批</span></template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-card size="small" :bordered="false" class="stat-card">
            <a-statistic title="已发布" :value="stats.published" :value-style="{ color: '#52c41a' }" />
          </a-card>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-card size="small" :bordered="false" class="stat-card">
            <a-statistic title="总预约/签到" :value="stats.totalEnrolled" :value-style="{ color: '#722ed1' }">
              <template #suffix><span style="font-size: 14px">/ {{ stats.totalCheckedIn }}</span></template>
            </a-statistic>
          </a-card>
        </a-col>
      </a-row>

      <a-card class="pa-list-card" :bordered="false">
        <template #title>
          <div class="pa-list-card-title">
            <span>公开课发布列表</span>
            <span class="pa-list-card-sub">共 {{ total }} 条 · 显示 {{ rangeText }}</span>
          </div>
        </template>

        <template #extra>
          <a-space wrap>
            <a-segmented
              v-model:value="statusFilter"
              :options="[
                { label: '全部', value: 'all' },
                { label: '待发布', value: '同意' },
                { label: '已发布', value: '已发布' },
              ]"
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
              @pressEnter="onSearch"
            />
            <a-button @click="onSearch" :disabled="loading">查询</a-button>
            <a-button @click="onReset" :disabled="loading">重置</a-button>
            <a-button
              v-if="isAdmin && selectedRowKeys.length > 0"
              type="primary"
              :loading="batchLoading"
              @click="batchPublish"
            >
              批量发布 ({{ selectedRowKeys.length }})
            </a-button>
          </a-space>
        </template>

        <div class="pa-table-scroll">
          <a-table
            :columns="columns"
            :data-source="items"
            :loading="loading"
            :row-selection="rowSelection"
            :pagination="{
              current: page,
              pageSize,
              total,
              showSizeChanger: true,
              showQuickJumper: true,
            }"
            :scroll="{ x: '100%' }"
            row-key="id"
            @change="onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'state'">
                <a-tag :color="stateText(record.state) === '已发布' ? 'green' : 'orange'">
                  {{ stateText(record.state) }}
                </a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" @click="openDetail(record)">详情</a-button>
                  <a-button
                    v-if="isAdmin && stateText(record.state) === '同意'"
                    type="link"
                    @click="publishCourse(record)"
                  >
                    发布
                  </a-button>
                  <a-button
                    v-if="stateText(record.state) === '已发布'"
                    type="link"
                    @click="openQrCode(record)"
                  >
                    二维码
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
          <a-descriptions-item label="审批时间">{{ current.auditedAt || '—' }}</a-descriptions-item>
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
      <a-modal
        v-model:open="qrModalOpen"
        title="公开课二维码"
        width="800px"
        :footer="null"
        @cancel="closeQrModal"
      >
        <div style="display: flex; justify-content: space-around; padding: 20px 0; text-align: center;">
          <div style="flex: 1;">
            <h3 style="margin-bottom: 16px;">预约二维码</h3>
            <a-spin :spinning="qrLoading && !qrBookingUrl">
              <div v-if="qrBookingUrl" style="padding: 16px; background: #f5f5f5; border-radius: 8px; display: inline-block;">
                <qrcode-vue :value="qrBookingUrl" :size="180" level="M" />
              </div>
            </a-spin>
            <div style="margin-top: 12px; color: #666; font-size: 13px;">扫码进行公开课预约</div>
          </div>
          
          <div style="width: 1px; background: #e8e8e8; margin: 0 20px;"></div>
          
          <div style="flex: 1;">
            <h3 style="margin-bottom: 16px;">动态签到二维码</h3>
            <a-spin :spinning="qrLoading && !qrCheckinUrl">
              <div v-if="qrCheckinUrl" style="padding: 16px; background: #f5f5f5; border-radius: 8px; display: inline-block;">
                <qrcode-vue :value="qrCheckinUrl" :size="180" level="M" />
              </div>
            </a-spin>
            <div style="margin-top: 12px; color: #666; font-size: 13px;">扫码进行签到（自动刷新）</div>
          </div>

          <div style="width: 1px; background: #e8e8e8; margin: 0 20px;"></div>

          <div style="flex: 1;">
            <h3 style="margin-bottom: 16px;">评课二维码</h3>
            <a-spin :spinning="qrLoading && !qrReviewUrl">
              <div v-if="qrReviewUrl" style="padding: 16px; background: #f5f5f5; border-radius: 8px; display: inline-block;">
                <qrcode-vue :value="qrReviewUrl" :size="180" level="M" />
              </div>
            </a-spin>
            <div style="margin-top: 12px; color: #666; font-size: 13px;">扫码进入评课页</div>
          </div>
        </div>
        <div style="text-align: right; margin-top: 16px">
          <a-button @click="closeQrModal">关闭</a-button>
        </div>
      </a-modal>
    </main>
  </div>
</template>
