<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { message } from 'ant-design-vue'
import type { TablePaginationConfig, TableProps } from 'ant-design-vue'

import HomeHeader from '../../components/HomeHeader.vue'
import { http } from '../../services/http'
import { useAuthStore } from '../../stores/auth'
import { formatDateTime } from '../../utils/date'

type ApiResponse<T> = { code: number; message: string; data: T }

type BookingCourseListItem = {
  id: string
  name: string
  startAt: string
  teacherName: string
  enrolledCount: number
  capacity: number
  reservationState: string
  courseStatus: string
  canReserve: boolean
  canCancel: boolean
}

type BookingCourseDetailItem = {
  id: string
  name: string
  startAt: string
  teacherName: string
  enrolledCount: number
  capacity: number
  academicYear: string
  term: string
  department: string
  className: string
  office: string
  location: string
  course: string
  topic: string
  reservationState: string
  courseStatus: string
  canReserve: boolean
  canCancel: boolean
}

type ReservedUserItem = {
  userId: number
  username: string
  phone: string
  reserveTime: string
  checkedIn: boolean
}

const authStore = useAuthStore()
const router = useRouter()
const isAdmin = computed(() => (authStore.user?.role || authStore.user?.username || '').toLowerCase() === 'admin')
const loading = ref(false)
const kwCourses = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedUserId = ref<number | undefined>(undefined)
const users = ref<{ id: number; username: string }[]>([])
const courses = ref<BookingCourseListItem[]>([])

const detailOpen = ref(false)
const detailLoading = ref(false)
const current = ref<BookingCourseDetailItem | null>(null)
const actionLoading = ref(false)

// 预约用户列表
const reservedUsers = ref<ReservedUserItem[]>([])
const reservedUsersLoading = ref(false)

const rangeText = computed(() => {
  if (total.value <= 0) return '0 - 0'
  const start = (page.value - 1) * pageSize.value + 1
  const end = Math.min(total.value, page.value * pageSize.value)
  return `${start} - ${end}`
})

const courseColumns: NonNullable<TableProps['columns']> = [
  { title: '课程', dataIndex: 'name', key: 'name', ellipsis: true },
  {
    title: '时间',
    dataIndex: 'startAt',
    key: 'startAt',
    width: 220,
    customRender: ({ text }) => formatDateTime(text),
  },
  { title: '教师', dataIndex: 'teacherName', key: 'teacherName', width: 140, ellipsis: true },
  { title: '人数', key: 'cnt', width: 120, customRender: ({ record }) => `${record.enrolledCount}/${record.capacity}` },
  { title: '预约状态', dataIndex: 'reservationState', key: 'reservationState', width: 110 },
  {
    title: '课程状态',
    dataIndex: 'courseStatus',
    key: 'courseStatus',
    width: 110,  
  },
  { title: '操作', key: 'action', width: 220, fixed: 'right', customRender: ({ record }) => record },
]

const reservedUserColumns: NonNullable<TableProps['columns']> = [
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 100 },
  { title: '用户名', dataIndex: 'username', key: 'username', width: 150 },
  { title: '电话', dataIndex: 'phone', key: 'phone', width: 150 },
  {
    title: '预约时间',
    dataIndex: 'reserveTime',
    key: 'reserveTime',
    width: 180,
    customRender: ({ text }) => formatDateTime(text),
  },
  {
    title: '签到状态',
    dataIndex: 'checkedIn',
    key: 'checkedIn',
    width: 110, 
  },
]

async function onLogout() {
  authStore.logout()
  await router.replace('/login')
}

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

async function fetchAll() {
  loading.value = true
  try {
    const params: any = {
      page: page.value,
      page_size: pageSize.value,
      keyword: kwCourses.value.trim() || undefined,
    }
    if (isAdmin.value && selectedUserId.value) {
      params.userId = selectedUserId.value
    }
    const res1 = await http.get<ApiResponse<{ list: BookingCourseListItem[]; total: number }>>('/booking/courses/page', { params })
    if (res1.data.code !== 0) {
      message.error(res1.data.message || '请求失败')
      courses.value = []
      total.value = 0
      return
    }
    courses.value = res1.data.data?.list || []
    total.value = res1.data.data?.total || 0
  } catch (e) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '请求失败')
    courses.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  void fetchAll()
}

function onTableChange(p: TablePaginationConfig) {
  const nextPage = typeof p.current === 'number' && p.current > 0 ? p.current : 1
  const nextSize = typeof p.pageSize === 'number' && p.pageSize > 0 ? p.pageSize : 10
  const sizeChanged = nextSize !== pageSize.value
  pageSize.value = nextSize
  page.value = sizeChanged ? 1 : nextPage
  void fetchAll()
}

async function openDetail(item: { id: string }) {
  const id = (item?.id || '').trim()
  if (!id) return
  detailOpen.value = true
  detailLoading.value = true
  current.value = null
  reservedUsers.value = []
  try {
    const params: any = { id }
    if (isAdmin.value && selectedUserId.value) {
      params.userId = selectedUserId.value
    }
    const res = await http.get<ApiResponse<BookingCourseDetailItem>>('/booking/course-detail', { params })
    if (res.data.code !== 0) {
      message.error(res.data.message || '请求失败')
      return
    }
    current.value = res.data.data

    // 获取预约用户列表
    reservedUsersLoading.value = true
    try {
      const userRes = await http.get<ApiResponse<{ list: ReservedUserItem[] }>>('/booking/course/reserved-users', {
        params: { courseId: id }
      })
      if (userRes.data.code === 0) {
        reservedUsers.value = userRes.data.data?.list || []
      }
    } catch (e) {
      console.error('获取预约用户失败', e)
    } finally {
      reservedUsersLoading.value = false
    }
  } catch (e) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '请求失败')
  } finally {
    detailLoading.value = false
  }
}

async function reserve(id: string) {
  const cid = (id || '').trim()
  if (!cid) return
  actionLoading.value = true
  try {
    const res = await http.post<ApiResponse<{ ok: boolean }>>('/booking/reserve', { id: cid })
    if (res.data.code !== 0) {
      message.error(res.data.message || '预约失败')
      return
    }
    message.success('预约成功')
    await fetchAll()
    if (detailOpen.value && current.value?.id === cid) {
      await openDetail({ id: cid })
    }
  } catch (e) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '预约失败')
  } finally {
    actionLoading.value = false
  }
}

async function cancelReserve(id: string) {
  const cid = (id || '').trim()
  if (!cid) return
  actionLoading.value = true
  const wasDetailOpen = detailOpen.value
  const wasCurrentId = current.value?.id
  try {
    const res = await http.post<ApiResponse<{ ok: boolean }>>('/booking/cancel', { id: cid })
    if (res.data.code !== 0) {
      message.error(res.data.message || '取消失败')
      return
    }
    message.success('已取消预约')
    await fetchAll()
    // 刷新详情（如果在详情弹窗中操作）
    if (wasDetailOpen && wasCurrentId === cid) {
      await openDetail({ id: cid })
    }
  } catch (e) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || err.message || '取消失败')
  } finally {
    actionLoading.value = false
  }
}

onMounted(async () => {
  await fetchUsers()
  await fetchAll()
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
      title="预约中心"
      subtitle="课程预约 · 统一管理"
      :loading="loading"
      @refresh="fetchAll"
      @logout="onLogout"
    />

    <main class="pa-list-wrap">
      <a-card class="pa-list-card" :bordered="false">
        <template #title>
          <div class="pa-list-card-title">
            <span>全部课程</span>
            <span class="pa-list-card-sub">共 {{ total }} 条 · 显示 {{ rangeText }}</span>
          </div>
        </template>

        <template #extra>
          <a-space wrap>
            <a-select
              v-if="isAdmin"
              v-model:value="selectedUserId"
              placeholder="按用户查询"
              allow-clear
              style="width: 160px"
              :options="users.map(u => ({ label: u.username, value: u.id }))"
              @change="fetchAll"
            />
            <a-input v-model:value="kwCourses" placeholder="按课程/教师搜索" allow-clear style="width: 260px" />
            <a-button @click="onSearch" :loading="loading">查询</a-button>
            <a-button @click="fetchAll" :loading="loading">刷新</a-button>
          </a-space>
        </template>

        <div class="pa-table-scroll">
          <a-table
            :columns="courseColumns"
            :data-source="courses"
            :loading="loading"
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
              <template v-if="column.key === 'reservationState'">
                <a-tag :color="record.reservationState === '已预约' ? 'success' : (record.reservationState === '未预约' ? 'default' : 'warning')">
                  {{ record.reservationState }}
                </a-tag>
              </template>

              <template v-if="column.key === 'courseStatus'">

                <a-tag :color="record.courseStatus === '未开始' ? 'processing' : 'default'">

                  {{ record.courseStatus }}

                </a-tag>

              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" @click="openDetail(record)">详情</a-button>
                  <a-button
                    v-if="record.canReserve"
                    type="link"
                    :disabled="actionLoading"
                    @click="reserve(record.id)"
                  >
                    预约
                  </a-button>
                  <a-button
                    v-if="record.canCancel"
                    type="link"
                    :disabled="actionLoading"
                    @click="cancelReserve(record.id)"
                  >
                    取消预约
                  </a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </a-card>
    </main>

    <a-modal
      v-model:open="detailOpen"
      title="预约详情"
      :confirm-loading="detailLoading"
      width="900px"
      @cancel="detailOpen = false"
      :footer="null"
    >
      <a-descriptions v-if="current" bordered size="small" :column="{ xs: 1, sm: 2, md: 2 }">
        <a-descriptions-item label="课程">{{ current.name }}</a-descriptions-item>
        <a-descriptions-item label="时间">{{ formatDateTime(current.startAt) }}</a-descriptions-item>
        <a-descriptions-item label="教师">{{ current.teacherName }}</a-descriptions-item>
        <a-descriptions-item label="人数">{{ current.enrolledCount }}/{{ current.capacity }}</a-descriptions-item>
        <a-descriptions-item label="学年/学期">{{ current.academicYear }} / {{ current.term }}</a-descriptions-item>
        <a-descriptions-item label="系部/班级">{{ current.department }} / {{ current.className }}</a-descriptions-item>
        <a-descriptions-item label="教研室">{{ current.office }}</a-descriptions-item>
        <a-descriptions-item label="地点">{{ current.location }}</a-descriptions-item>
        <a-descriptions-item label="课程内容">{{ current.course }}</a-descriptions-item>
        <a-descriptions-item label="课题">{{ current.topic }}</a-descriptions-item>
        <a-descriptions-item label="预约状态">
          <a-tag :color="current.reservationState === '已预约' ? 'success' : (current.reservationState === '未预约' ? 'default' : 'warning')">
            {{ current.reservationState }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="课程状态">
          <a-tag :color="current.courseStatus === '未开始' ? 'processing' : 'default'">
            {{ current.courseStatus }}
          </a-tag>
        </a-descriptions-item>
      </a-descriptions>

      <!-- 预约用户列表 -->
      <a-divider>预约用户列表</a-divider>
      <a-table
        :columns="reservedUserColumns"
        :data-source="reservedUsers"
        :loading="reservedUsersLoading"
        :pagination="false"
        size="small"
        row-key="userId"
      >
        <template #emptyText>
          <span style="color: #999">暂无预约用户</span>
        </template>
         <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'checkedIn'">
                <a-tag :color="record.checkedIn ? 'success' : 'warning' ">
                  {{ record.checkedIn ?'已签到':'未签到'}}
                </a-tag>
              </template>

        </template>
      </a-table>

      <div style="margin-top: 14px; display: flex; justify-content: flex-end; gap: 10px">
        <a-popconfirm
          v-if="isAdmin"
          title="确定要取消该预约吗？"
          ok-text="确定"
          cancel-text="取消"
          @confirm="current && cancelReserve(current.id)"
        >
          <a-button :loading="actionLoading" danger>取消预约</a-button>
        </a-popconfirm>
        <a-button
          v-else-if="current?.canCancel"
          :loading="actionLoading"
          @click="cancelReserve(current.id)"
        >
          取消预约
        </a-button>
        <a-button
          v-if="current?.canReserve"
          type="primary"
          :loading="actionLoading"
          @click="reserve(current.id)"
        >
          预约
        </a-button>
      </div>
    </a-modal>
  </div>
</template>
