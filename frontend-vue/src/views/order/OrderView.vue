<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { TablePaginationConfig, TableProps } from 'ant-design-vue'
import HomeHeader from '../../components/HomeHeader.vue'
import { http } from '../../services/http'
import { useAuthStore } from '../../stores/auth'
import { formatDateTime } from '../../utils/date'

type OrderItemType = {
  id: number
  productId: number
  productName: string
  productThumbnail: string
  price: number
  quantity: number
}

type Order = {
  id: number
  userId: number
  orderNo: string
  totalAmount: number
  status: string
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  remark: string
  items: OrderItemType[]
  createdAt: string
}

type OrderPage = {
  content: Order[]
  totalElements: number
  size: number
  number: number
}

const authStore = useAuthStore()
const router = useRouter()

const loading = ref(false)
const items = ref<Order[]>([])
const keyword = ref('')
const statusFilter = ref<string | undefined>(undefined)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const detailVisible = ref(false)
const detailData = ref<Order | null>(null)
const detailLoading = ref(false)

const statusOptions = [
  { label: '全部状态', value: undefined },
  { label: '待支付', value: 'pending' },
  { label: '已支付', value: 'paid' },
  { label: '已发货', value: 'shipped' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' },
]

const statusMap: Record<string, { color: string; text: string }> = {
  pending: { color: 'orange', text: '待支付' },
  paid: { color: 'blue', text: '已支付' },
  shipped: { color: 'purple', text: '已发货' },
  completed: { color: 'green', text: '已完成' },
  cancelled: { color: 'red', text: '已取消' },
}

const rangeText = computed(() => {
  if (total.value <= 0) return '0 - 0'
  const start = (page.value - 1) * pageSize.value + 1
  const end = Math.min(total.value, page.value * pageSize.value)
  return `${start} - ${end}`
})

const columns: NonNullable<TableProps['columns']> = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 200, ellipsis: true },
  { title: '收货人', dataIndex: 'receiverName', key: 'receiverName', width: 100 },
  { title: '联系电话', dataIndex: 'receiverPhone', key: 'receiverPhone', width: 130 },
  { title: '收货地址', dataIndex: 'receiverAddress', key: 'receiverAddress', width: 200, ellipsis: true },
  { title: '订单金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120 },
  { title: '订单状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '下单时间', dataIndex: 'createdAt', key: 'createdAt', width: 180, customRender: ({ text }) => formatDateTime(text) },
  { title: '操作', key: 'action', width: 180, fixed: 'right' },
]

async function onLogout() {
  authStore.logout()
  await router.replace('/login')
}

async function fetchList() {
  loading.value = true
  try {
    const res = await http.get<OrderPage>('/admin/orders', {
      params: {
        page: page.value,
        page_size: pageSize.value,
        keyword: keyword.value.trim() || undefined,
        status: statusFilter.value,
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
  statusFilter.value = undefined
  page.value = 1
  pageSize.value = 10
  void fetchList()
}

function onTableChange(p: TablePaginationConfig) {
  page.value = p.current || 1
  pageSize.value = p.pageSize || 10
  void fetchList()
}

async function showDetail(record: Order) {
  detailLoading.value = true
  detailVisible.value = true
  try {
    const res = await http.get<any>(`/admin/orders/${record.id}`)
    detailData.value = res.data
  } catch (e: any) {
    message.error('获取订单详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function updateStatus(id: number, status: string) {
  try {
    await http.put(`/admin/orders/${id}/status`, { status })
    message.success('状态更新成功')
    void fetchList()
  } catch (e: any) {
    const err = e as { response?: { data?: { detail?: string } }; message?: string }
    message.error(err.response?.data?.detail || '更新失败')
  }
}

function getNextStatus(current: string): { status: string; label: string } | null {
  const flow: Record<string, { status: string; label: string }> = {
    pending: { status: 'paid', label: '确认支付' },
    paid: { status: 'shipped', label: '确认发货' },
    shipped: { status: 'completed', label: '确认收货' },
  }
  return flow[current] || null
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
      title="订单管理"
      subtitle="管理所有用户订单 · /admin/orders"
      :loading="loading"
      @refresh="fetchList"
      @logout="onLogout"
    />

    <main class="pa-list-wrap">
      <a-card class="pa-list-card" :bordered="false">
        <template #title>
          <div class="pa-list-card-title">
            <span>订单列表</span>
            <span class="pa-list-card-sub">共 {{ total }} 条 · 显示 {{ rangeText }}</span>
          </div>
        </template>

        <template #extra>
          <a-space wrap>
            <a-input-search
              v-model:value="keyword"
              placeholder="搜索订单号/收货人"
              style="width: 200px"
              allow-clear
              @search="onSearch"
            />
            <a-select
              v-model:value="statusFilter"
              placeholder="订单状态"
              style="width: 120px"
              allow-clear
              @change="onSearch"
            >
              <a-select-option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </a-select-option>
            </a-select>
            <a-button @click="onSearch" :loading="loading">查询</a-button>
            <a-button @click="onReset" :disabled="loading">重置</a-button>
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
            <template v-if="column.key === 'totalAmount'">
              <span style="color: #ff4d4f; font-weight: bold">¥{{ record.totalAmount }}</span>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="statusMap[record.status]?.color || 'default'">
                {{ statusMap[record.status]?.text || record.status }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="showDetail(record)">详情</a-button>
                <a-dropdown v-if="getNextStatus(record.status)">
                  <a-button type="link" size="small">
                    {{ getNextStatus(record.status)?.label }}
                  </a-button>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item
                        v-if="record.status === 'pending'"
                        key="paid"
                        @click="updateStatus(record.id, 'paid')"
                      >
                        标记已支付
                      </a-menu-item>
                      <a-menu-item
                        v-if="record.status === 'paid'"
                        key="shipped"
                        @click="updateStatus(record.id, 'shipped')"
                      >
                        标记已发货
                      </a-menu-item>
                      <a-menu-item
                        v-if="record.status === 'shipped'"
                        key="completed"
                        @click="updateStatus(record.id, 'completed')"
                      >
                        标记已完成
                      </a-menu-item>
                      <a-menu-divider v-if="record.status === 'pending'" />
                      <a-menu-item
                        v-if="record.status === 'pending'"
                        key="cancelled"
                        @click="updateStatus(record.id, 'cancelled')"
                      >
                        <span style="color: red">取消订单</span>
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </main>

    <!-- 订单详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="订单详情"
      :footer="null"
      width="700px"
    >
      <a-spin :spinning="detailLoading">
        <a-descriptions bordered :column="2" v-if="detailData">
          <a-descriptions-item label="订单号">{{ detailData.orderNo }}</a-descriptions-item>
          <a-descriptions-item label="订单状态">
            <a-tag :color="statusMap[detailData.status]?.color || 'default'">
              {{ statusMap[detailData.status]?.text || detailData.status }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="用户ID">{{ detailData.userId }}</a-descriptions-item>
          <a-descriptions-item label="下单时间">{{ formatDateTime(detailData.createdAt) }}</a-descriptions-item>
          <a-descriptions-item label="收货人">{{ detailData.receiverName }}</a-descriptions-item>
          <a-descriptions-item label="联系电话">{{ detailData.receiverPhone }}</a-descriptions-item>
          <a-descriptions-item label="收货地址" :span="2">{{ detailData.receiverAddress }}</a-descriptions-item>
          <a-descriptions-item label="订单备注" :span="2">{{ detailData.remark || '-' }}</a-descriptions-item>
          <a-descriptions-item label="订单金额" :span="2">
            <span style="color: #ff4d4f; font-size: 18px; font-weight: bold">
              ¥{{ detailData.totalAmount }}
            </span>
          </a-descriptions-item>

          <a-descriptions-item label="商品列表" :span="2">
            <a-table
              :data-source="detailData.items"
              :pagination="false"
              size="small"
              :scroll="{ x: 500 }"
            >
              <a-table-column title="商品图片" width="80">
                <template #default="{ record }">
                  <a-image :src="record.productThumbnail" :width="50" fallback="/placeholder.png" />
                </template>
              </a-table-column>
              <a-table-column title="商品名称" dataIndex="productName" />
              <a-table-column title="单价" dataIndex="price" />
              <a-table-column title="数量" dataIndex="quantity" />
              <a-table-column title="小计">
                <template #default="{ record }">
                  ¥{{ (record.price * record.quantity).toFixed(2) }}
                </template>
              </a-table-column>
            </a-table>
          </a-descriptions-item>
        </a-descriptions>
      </a-spin>
    </a-modal>
  </div>
</template>
