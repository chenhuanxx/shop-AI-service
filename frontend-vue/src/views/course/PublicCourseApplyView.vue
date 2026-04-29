<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { message } from 'ant-design-vue'
import type { TablePaginationConfig, TableProps } from 'ant-design-vue'

import HomeHeader from '../../components/HomeHeader.vue'
import { http } from '../../services/http'
import { useAuthStore } from '../../stores/auth'
import { formatDateTime } from '../../utils/date'

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
  userName: string
  auditOpinion: string
  auditedAt: string
}

const authStore = useAuthStore()
const router = useRouter()

const isAdmin = computed(() => (authStore.user?.role || authStore.user?.username || '').toLowerCase() === 'admin')

function normalizeState(s: string) {
  const raw = (s || '').trim()
  if (raw === 'pending') return '待审批'
  if (raw === 'approve') return '已通过'
  if (raw === 'reject') return '已拒绝'
  return raw
}

function canEditApplication(s: string) {
  const st = normalizeState(s)
  return st === '已通过' || st === '已拒绝'
}

const categoryOptions = [
  { label: '示范课', value: '示范课' },
  { label: '教学观摩', value: '教学观摩' },
  { label: '研讨课', value: '研讨课' },
  { label: '公开课', value: '公开课' },
]

const yearOptions = computed(() => {
  const y = new Date().getFullYear()
  const start = y - 1
  return [start, start + 1, start + 2].map((x) => ({
    label: `${x}-${x + 1}`,
    value: `${x}-${x + 1}`,
  }))
})

const departmentOptions = [
  { label: '教育学院', value: '教育学院' },
  { label: '信息工程学院', value: '信息工程学院' },
  { label: '外国语学院', value: '外国语学院' },
  { label: '数学与统计学院', value: '数学与统计学院' },
]

const classOptionsByDepartment: Record<string, { label: string; value: string }[]> = {
  教育学院: [
    { label: '2024 级 教育学 1 班', value: '2024 级 教育学 1 班' },
    { label: '2024 级 教育学 2 班', value: '2024 级 教育学 2 班' },
    { label: '2023 级 学前教育 1 班', value: '2023 级 学前教育 1 班' },
  ],
  信息工程学院: [
    { label: '2023 级 计算机 1 班', value: '2023 级 计算机 1 班' },
    { label: '2023 级 计算机 2 班', value: '2023 级 计算机 2 班' },
    { label: '2024 级 软件工程 1 班', value: '2024 级 软件工程 1 班' },
  ],
  外国语学院: [
    { label: '2024 级 英语 1 班', value: '2024 级 英语 1 班' },
    { label: '2024 级 日语 1 班', value: '2024 级 日语 1 班' },
  ],
  数学与统计学院: [
    { label: '2024 级 数学 1 班', value: '2024 级 数学 1 班' },
    { label: '2024 级 统计 1 班', value: '2024 级 统计 1 班' },
  ],
}

const weekOptions = Array.from({ length: 30 }).map((_, i) => {
  const no = i + 1
  return { label: `第 ${no} 周`, value: no }
})

const weekdayOptions = [
  { label: '星期一', value: '星期一' },
  { label: '星期二', value: '星期二' },
  { label: '星期三', value: '星期三' },
  { label: '星期四', value: '星期四' },
  { label: '星期五', value: '星期五' },
  { label: '星期六', value: '星期六' },
  { label: '星期日', value: '星期日' },
]

const sectionOptions = [
  { label: '第一节', value: '第一节' },
  { label: '第二节', value: '第二节' },
  { label: '第三节', value: '第三节' },
  { label: '第四节', value: '第四节' },
  { label: '第五节', value: '第五节' },
  { label: '第六节', value: '第六节' },
]

const semesterStartByKey: Record<string, string> = {
  '2025-2026|第一学期': '2025-09-01',
  '2025-2026|第二学期': '2026-02-23',
  '2024-2025|第一学期': '2024-09-02',
  '2024-2025|第二学期': '2025-02-24',
  '2026-2027|第一学期': '2026-09-01',
  '2026-2027|第二学期': '2027-02-22',
}

const formState = reactive({
  category: '',
  academicYear: yearOptions.value[0]?.value || '',
  term: '第一学期',
  department: '',
  className: '',
  office: '',
  location: '',
  courseName: '',
  topic: '',
  startDate: '',
  weekNo: null as number | null,
  weekday: '',
  section: '',
  applyReason: '',
})

const classOptions = computed(() => classOptionsByDepartment[formState.department] || [])

function resetForm() {
  formState.category = ''
  formState.department = ''
  formState.className = ''
  formState.office = ''
  formState.location = ''
  formState.courseName = ''
  formState.topic = ''
  formState.startDate = ''
  formState.weekNo = null
  formState.weekday = ''
  formState.section = ''
  formState.applyReason = ''
}

function formatWeekday(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(`${dateStr}T00:00:00`)
  const map = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  const w = map[d.getDay()] || ''
  return w
}

function calcWeekNo(dateStr: string, academicYear: string, term: string) {
  if (!dateStr || !academicYear || !term) return null
  const key = `${academicYear}|${term}`
  const start = semesterStartByKey[key]
  if (!start) return null
  const startDate = new Date(`${start}T00:00:00`)
  const cur = new Date(`${dateStr}T00:00:00`)
  const diffDays = Math.floor((cur.getTime() - startDate.getTime()) / 86400000)
  if (Number.isNaN(diffDays) || diffDays < 0) return null
  return Math.floor(diffDays / 7) + 1
}

watch(
  () => formState.department,
  () => {
    const list = classOptions.value
    if (!list.some((x) => x.value === formState.className)) {
      formState.className = ''
    }
  }
)

watch(
  () => [formState.startDate, formState.academicYear, formState.term] as const,
  ([dateStr, year, term]) => {
    if (!dateStr) return
    const wd = formatWeekday(dateStr)
    if (wd) formState.weekday = wd
    const wk = calcWeekNo(dateStr, year, term)
    if (typeof wk === 'number' && wk > 0 && wk <= 30) {
      if (!formState.weekNo) {
        formState.weekNo = wk
      }
    }
  }
)

const formMode = ref<'create' | 'edit'>('create')
const editingId = ref<string | null>(null)
const applyOpen = ref(false)
const submitting = ref(false)

function openApply() {
  formMode.value = 'create'
  editingId.value = null
  resetForm()
  applyOpen.value = true
}

function openEdit(item: ApplicationItem) {
  formMode.value = 'edit'
  editingId.value = item.id
  formState.category = item.category
  formState.academicYear = item.academicYear
  formState.term = item.term
  formState.department = item.department
  formState.className = item.className
  formState.office = item.office
  formState.location = item.location
  formState.courseName = item.courseName
  formState.topic = item.topic
  formState.startDate = item.startDate
  formState.weekNo = item.weekNo
  formState.weekday = item.weekday
  formState.section = item.section
  formState.applyReason = item.applyReason
  applyOpen.value = true
}

const detailOpen = ref(false)
const currentDetail = ref<ApplicationItem | null>(null)

function openDetail(item: ApplicationItem) {
  currentDetail.value = item
  detailOpen.value = true
}

async function onLogout() {
  authStore.logout()
  await router.replace('/login')
}

function validateForm() {
  if (!formState.category) return '请选择公开课类别'
  if (!formState.academicYear) return '请选择开课学年'
  if (!formState.term) return '请选择开课学期'
  if (!formState.department) return '请选择开课系部'
  if (!formState.className) return '请选择开课班级'
  if (!formState.office.trim()) return '请输入教研室'
  if (!formState.location.trim()) return '请输入开课地点'
  if (!formState.courseName.trim()) return '请输入课程名称'
  if (!formState.topic.trim()) return '请输入课题'
  if (!formState.startDate) return '请选择开课时间'
  if (!formState.weekNo) return '请选择周次'
  if (!formState.weekday) return '请选择星期'
  if (!formState.section) return '请选择节次'
  if (!formState.applyReason.trim()) return '请输入申报理由'
  return ''
}

async function submitApply() {
  const errText = validateForm()
  if (errText) {
    message.warning(errText)
    return
  }
  submitting.value = true
  try {
    const payload = {
      category: formState.category,
      academicYear: formState.academicYear,
      term: formState.term,
      department: formState.department,
      className: formState.className,
      office: formState.office.trim(),
      location: formState.location.trim(),
      courseName: formState.courseName.trim(),
      topic: formState.topic.trim(),
      startDate: formState.startDate,
      weekNo: formState.weekNo,
      weekday: formState.weekday,
      section: formState.section,
      applyReason: formState.applyReason.trim(),
    }
    
    if (formMode.value === 'edit' && editingId.value) {
      const res = await http.post<ApiResponse<string>>('/public-course/applications/update', {
        id: editingId.value,
        ...payload
      })
      if (res.data.code !== 0) {
        message.error(res.data.message || '修改失败')
        return
      }
      message.success('修改成功')
    } else {
      const res = await http.post<ApiResponse<string>>('/public-course/applications', payload)
      if (res.data.code !== 0) {
        message.error(res.data.message || '提交失败')
        return
      }
      message.success('提交成功')
    }
    
    resetForm()
    applyOpen.value = false
    await fetchList()
  } catch (e) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const keyword = ref('')
const statusFilter = ref<'all' | '待审批' | '已通过' | '已拒绝'>('all')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const items = ref<ApplicationItem[]>([])
const loading = ref(false)

const rangeText = computed(() => {
  if (total.value <= 0) return '0 - 0'
  const start = (page.value - 1) * pageSize.value + 1
  const end = Math.min(total.value, page.value * pageSize.value)
  return `${start} - ${end}`
})

const columns = computed<NonNullable<TableProps['columns']>>(() => {
  const base: NonNullable<TableProps['columns']> = [
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
      customRender: ({ record }) => `${record.startDate} · 第${record.weekNo}周 · ${record.weekday} · ${record.section}`,
    },
    { 
      title: '状态', 
      dataIndex: 'state', 
      key: 'state', 
      width: 110,
      customRender: ({ text }) => normalizeState(text)
    },
    {
      title: '操作',
      key: 'action',
      width: 250,
      fixed: 'right',
      customRender: ({ record }) => record,
    },
  ]

  if (isAdmin.value) {
    base.splice(1, 0, { title: '申请人', dataIndex: 'userName', key: 'userName', width: 160, ellipsis: true })
  }
  return base
})

async function fetchList() {
  loading.value = true
  try {
    const endpoint = isAdmin.value ? '/public-course/applications/page' : '/public-course/my-applications/page'
    const params: any = {
      page: page.value,
      page_size: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      state: statusFilter.value === 'all' ? undefined : statusFilter.value,
    }
    if (isAdmin.value && selectedUserId.value) {
      params.userId = selectedUserId.value
    }
    const res = await http.get<ApiResponse<{ list: ApplicationItem[]; total: number }>>(endpoint, { params })
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

const selectedUserId = ref<number | undefined>(undefined)
const users = ref<{ id: number; username: string }[]>([])

async function fetchUsers() {
  if (!isAdmin.value) return
  try {
    const res = await http.get<{ items: { id: number; username: string }[] }>('/users', {
      params: { page: 1, page_size: 200 }
    })
    users.value = res.data.items || []
  } catch (e) {
    // ignore
  }
}

onMounted(async () => {
  await fetchUsers()
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
      title="公开课申请"
      subtitle="申请提交 · 列表管理 · /public-course-apply"
      :loading="loading || submitting"
      @refresh="fetchList"
      @logout="onLogout"
    />

    <main class="pa-list-wrap">
      <a-card class="pa-list-card" :bordered="false">
        <template #title>
      <div class="pa-list-card-title">
        <span>申请列表</span>
        <span class="pa-list-card-sub">共 {{ total }} 条 · 显示 {{ rangeText }}</span>
      </div>
        </template>

        <template #extra>
          <a-space wrap>
            <a-button type="primary" @click="openApply">申请</a-button>
            <a-select
              v-if="isAdmin"
              v-model:value="selectedUserId"
              placeholder="按用户查询"
              allow-clear
              style="width: 160px"
              :options="users.map(u => ({ label: u.username, value: u.id }))"
              @change="fetchList"
            />
            <a-segmented
              v-model:value="statusFilter"
              :options="[
                { label: '全部', value: 'all' },
                { label: '待审批', value: '待审批' },
                { label: '已通过', value: '已通过' },
                { label: '已拒绝', value: '已拒绝' },
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
              :disabled="loading"
              style="width: 280px"
            />
            <a-button @click="onSearch" :loading="loading">查询</a-button>
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
                  <a-button v-if="canEditApplication(record.state)" type="link" @click="openEdit(record)">
                    编辑
                  </a-button>
                  <a-button type="link" @click="openDetail(record)">
                    查看
                  </a-button>
                  <a-tooltip v-if="record.auditOpinion" :title="record.auditOpinion">
                    <a-button type="link">审核意见</a-button>
                  </a-tooltip>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </a-card>

      <a-modal
        v-model:open="applyOpen"
        :title="formMode === 'edit' ? '修改公开课申请' : '公开课申请'"
        :ok-text="formMode === 'edit' ? '保存' : '提交'"
        :confirm-loading="submitting"
        width="900px"
        @ok="submitApply"
        @cancel="applyOpen = false"
      >
        <a-form layout="vertical">
          <a-row :gutter="12">
            <a-col :xs="24" :md="6">
              <a-form-item label="公开课类别" required>
                <a-select
                  v-model:value="formState.category"
                  placeholder="下拉菜单"
                  :options="categoryOptions"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="6">
              <a-form-item label="开课学年" required>
                <a-select
                  v-model:value="formState.academicYear"
                  placeholder="下拉菜单"
                  :options="yearOptions"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="6">
              <a-form-item label="开课学期" required>
                <a-radio-group v-model:value="formState.term" button-style="solid">
                  <a-radio-button value="第一学期">第一学期</a-radio-button>
                  <a-radio-button value="第二学期">第二学期</a-radio-button>
                </a-radio-group>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="6">
              <a-form-item label="开课系部" required>
                <a-select
                  v-model:value="formState.department"
                  placeholder="下拉菜单"
                  :options="departmentOptions"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="6">
              <a-form-item label="开课班级" required>
                <a-select
                  v-model:value="formState.className"
                  placeholder="下拉菜单"
                  :options="classOptions"
                  allow-clear
                  :disabled="!formState.department"
                />
              </a-form-item>
            </a-col> 
            <a-col :xs="24" :md="6">
              <a-form-item label="教研室" required>
                <a-input v-model:value="formState.office" placeholder="请输入" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="6">
              <a-form-item label="开课地点" required>
                <a-input v-model:value="formState.location" placeholder="请输入" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="6">
              <a-form-item label="课程名称" required>
                <a-input v-model:value="formState.courseName" placeholder="请输入" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="12">
            <a-col :xs="24" :md="6">
              <a-form-item label="课题" required>
                <a-input v-model:value="formState.topic" placeholder="请输入" />
              </a-form-item>
            </a-col>

            <a-col :xs="24" :md="6">
              <a-form-item label="开课时间" required>
                <a-date-picker v-model:value="formState.startDate" value-format="YYYY-MM-DD" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="4">
              <a-form-item label="周次" required>
                <a-select
                  v-model:value="formState.weekNo"
                  placeholder="下拉菜单"
                  :options="weekOptions"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="4">
              <a-form-item label="星期" required>
                <a-select
                  v-model:value="formState.weekday"
                  placeholder="下拉菜单"
                  :options="weekdayOptions"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="4">
              <a-form-item label="节次" required>
                <a-select
                  v-model:value="formState.section"
                  placeholder="下拉菜单"
                  :options="sectionOptions"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="12">
            <a-col :xs="24">
              <a-form-item label="申报理由" required>
                <a-textarea v-model:value="formState.applyReason" placeholder="请输入" :rows="5" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </a-modal>

      <a-modal
        v-model:open="detailOpen"
        title="公开课详情"
        width="900px"
        :footer="null"
        @cancel="detailOpen = false"
      >
        <a-descriptions v-if="currentDetail" bordered size="small" :column="{ xs: 1, sm: 2, md: 2 }">
          <a-descriptions-item label="申请ID">{{ currentDetail!.id }}</a-descriptions-item>
          <a-descriptions-item label="申请时间">{{ currentDetail!.createdAt }}</a-descriptions-item>
          <a-descriptions-item label="类别">{{ currentDetail!.category }}</a-descriptions-item>
          <a-descriptions-item label="学年/学期">{{ currentDetail!.academicYear }} / {{ currentDetail!.term }}</a-descriptions-item>
          <a-descriptions-item label="系部">{{ currentDetail!.department }}</a-descriptions-item>
          <a-descriptions-item label="班级">{{ currentDetail!.className }}</a-descriptions-item>
          <a-descriptions-item label="教研室">{{ currentDetail!.office }}</a-descriptions-item>
          <a-descriptions-item label="地点">{{ currentDetail!.location }}</a-descriptions-item>
          <a-descriptions-item label="课程名称">{{ currentDetail!.courseName }}</a-descriptions-item>
          <a-descriptions-item label="课题">{{ currentDetail!.topic }}</a-descriptions-item>
          <a-descriptions-item label="开课安排">
            {{ currentDetail!.startDate }} · 第{{ currentDetail!.weekNo }}周 · {{ currentDetail!.weekday }} · {{ currentDetail!.section }}
          </a-descriptions-item>
          <a-descriptions-item label="状态">{{ currentDetail!.state }}</a-descriptions-item>
          <a-descriptions-item label="申报理由" :span="2">
            <div style="white-space: pre-wrap">{{ currentDetail!.applyReason }}</div>
          </a-descriptions-item>
        </a-descriptions>
        <div style="text-align: right; margin-top: 16px">
          <a-button @click="detailOpen = false">关闭</a-button>
        </div>
      </a-modal>
    </main>
  </div>
</template>
