<template>
	<view class="page">
		<!-- 搜索和筛选 -->
		<view class="search-bar">
			<u-search
				v-model="searchKey"
				placeholder="搜索订单号/收货人"
				:show-action="false"
				@change="onSearch"
			></u-search>
		</view>
		
		<!-- 状态筛选 -->
		<view class="status-tabs">
			<u-tag
				v-for="tab in statusTabs"
				:key="tab.value"
				:text="tab.text"
				:type="currentStatus === tab.value ? 'primary' : 'info'"
				:plain="currentStatus !== tab.value"
				@click="changeStatus(tab.value)"
				style="margin-right: 16rpx;"
			/>
		</view>
		
		<!-- 订单列表 -->
		<view class="order-list">
			<view v-if="loading" class="loading">
				<u-loading-icon></u-loading-icon>
			</view>
			<view v-else-if="orders.length === 0" class="empty">
				<u-empty text="暂无订单" icon="order"></u-empty>
			</view>
			<view v-else>
				<view v-for="order in orders" :key="order.id" class="order-card" @click="goDetail(order)">
					<view class="order-header">
						<text class="order-no">订单号: {{ order.orderNo }}</text>
						<text class="order-status" :class="getStatusClass(order.status)">
							{{ getStatusText(order.status) }}
						</text>
					</view>
					<view class="order-items">
						<view v-for="item in order.items" :key="item.id" class="order-item">
							<image class="item-image" :src="getImageUrl(item.productImage) || '/static/logo.png'" mode="aspectFill"></image>
							<view class="item-info">
								<text class="item-name">{{ item.productName }}</text>
								<text class="item-price">¥{{ item.price }}</text>
								<text class="item-quantity">x{{ item.quantity }}</text>
							</view>
						</view>
					</view>
					<view class="order-footer">
						<text class="order-time">{{ formatTime(order.createdAt) }}</text>
						<text class="order-total">合计: ¥{{ order.totalAmount }}</text>
					</view>
					<view class="order-actions">
						<u-button v-if="order.status === 0" size="small" type="primary" @click.stop="confirmPay(order)">确认支付</u-button>
						<u-button v-if="order.status === 1" size="small" type="warning" @click.stop="confirmDeliver(order)">确认发货</u-button>
						<u-button v-if="order.status === 2" size="small" type="success" @click.stop="confirmReceive(order)">确认收货</u-button>
						<u-button v-if="order.status === 3 || order.status === 4" size="small" plain @click.stop="deleteOrder(order)">删除</u-button>
					</view>
				</view>
			</view>
		</view>
		
		<!-- 分页 -->
		<view v-if="orders.length > 0" class="pagination">
			<u-button v-if="hasMore" type="primary" plain size="small" @click="loadMore" :loading="loadingMore">
				加载更多
			</u-button>
			<text v-else class="no-more">— 没有更多了 —</text>
		</view>
	</view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store/auth'
import { api } from '@/utils/api'
import { getImageUrl } from '@/utils/function.js'

const auth = useAuthStore()

const searchKey = ref('')
const currentStatus = ref<number | null>(null)
const orders = ref<any[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const page = ref(0)
const pageSize = ref(10)
const hasMore = ref(true)

const statusTabs = [
	{ text: '全部', value: null },
	{ text: '待支付', value: 0 },
	{ text: '已支付', value: 1 },
	{ text: '已发货', value: 2 },
	{ text: '已完成', value: 3 },
	{ text: '已取消', value: 4 },
]

onMounted(() => {
	loadOrders()
})

onPullDownRefresh(() => {
	page.value = 0
	hasMore.value = true
	loadOrders().finally(() => uni.stopPullDownRefresh())
})

function onSearch() {
	page.value = 0
	hasMore.value = true
	loadOrders()
}

function changeStatus(status: number | null) {
	currentStatus.value = status
	page.value = 0
	hasMore.value = true
	loadOrders()
}

async function loadOrders() {
		loading.value = true
		try {
			const res = await api.getOrders({
			keyword: searchKey.value || undefined,
			status: currentStatus.value ?? undefined,
			page: page.value,
			size: pageSize.value,
		})
		if (page.value === 0) {
			orders.value = res.content || []
		} else {
			orders.value = [...orders.value, ...(res.content || [])]
		}
		hasMore.value = !res.last
	} catch (e) {
		console.error('加载订单失败', e)
	} finally {
		loading.value = false
	}
}

function loadMore() {
	if (loadingMore.value || !hasMore.value) return
	loadingMore.value = true
	page.value++
	loadOrders().finally(() => {
		loadingMore.value = false
	})
}

function goDetail(order: any) {
	uni.navigateTo({ url: `/pages/order/detail?id=${order.id}` })
}

function getStatusText(status: number): string {
	const map: Record<number, string> = {
		0: '待支付',
		1: '已支付',
		2: '已发货',
		3: '已完成',
		4: '已取消',
	}
	return map[status] || '未知'
}

function getStatusClass(status: number): string {
	const map: Record<number, string> = {
		0: 'status-pending',
		1: 'status-paid',
		2: 'status-shipped',
		3: 'status-done',
		4: 'status-cancelled',
	}
	return map[status] || ''
}

function formatTime(time: string): string {
	if (!time) return ''
	return time.replace('T', ' ').substring(0, 19)
}

async function confirmPay(order: any) {
	try {
		await api.updateOrderStatus(order.id, 1)
		uni.showToast({ title: '已确认支付', icon: 'success' })
		loadOrders()
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'error' })
	}
}

async function confirmDeliver(order: any) {
	try {
		await api.updateOrderStatus(order.id, 2)
		uni.showToast({ title: '已确认发货', icon: 'success' })
		loadOrders()
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'error' })
	}
}

async function confirmReceive(order: any) {
	try {
		await api.updateOrderStatus(order.id, 3)
		uni.showToast({ title: '已确认收货', icon: 'success' })
		loadOrders()
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'error' })
	}
}

async function deleteOrder(order: any) {
	uni.showModal({
		title: '提示',
		content: '确定删除该订单吗？',
		success: async (res) => {
			if (res.confirm) {
				try {
					await api.deleteOrder(order.id)
					uni.showToast({ title: '已删除', icon: 'success' })
					loadOrders()
				} catch (e) {
					uni.showToast({ title: '删除失败', icon: 'error' })
				}
			}
		}
	})
}
</script>

<style scoped lang="scss">
.page {
	min-height: 100vh;
	background: #f5f5f5;
	padding-bottom: 40rpx;
}

.search-bar {
	padding: 24rpx 32rpx;
	background: #fff;
}

.status-tabs {
	padding: 16rpx 32rpx;
	background: #fff;
	border-bottom: 1rpx solid #eee;
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
}

.order-list {
	padding: 24rpx 32rpx;
}

.loading, .empty {
	padding: 100rpx 0;
	display: flex;
	justify-content: center;
}

.order-card {
	background: #fff;
	border-radius: 16rpx;
	padding: 24rpx;
	margin-bottom: 24rpx;
	box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.order-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding-bottom: 20rpx;
	border-bottom: 1rpx solid #f0f0f0;
}

.order-no {
	font-size: 24rpx;
	color: #666;
}

.order-status {
	font-size: 26rpx;
	font-weight: 500;
	&.status-pending { color: #ff9900; }
	&.status-paid { color: #2388FD; }
	&.status-shipped { color: #19be6b; }
	&.status-done { color: #666; }
	&.status-cancelled { color: #999; }
}

.order-items {
	padding: 20rpx 0;
}

.order-item {
	display: flex;
	gap: 20rpx;
	padding: 12rpx 0;
}

.item-image {
	width: 160rpx;
	height: 160rpx;
	border-radius: 12rpx;
	background: #f5f5f5;
}

.item-info {
	flex: 1;
	display: flex;
	flex-direction: column;
	justify-content: space-between;
}

.item-name {
	font-size: 28rpx;
	color: #333;
	line-height: 1.4;
	overflow: hidden;
	text-overflow: ellipsis;
	display: -webkit-box;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
}

.item-price {
	color: #ee0a24;
	font-size: 28rpx;
}

.item-quantity {
	color: #999;
	font-size: 24rpx;
}

.order-footer {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding-top: 16rpx;
	border-top: 1rpx solid #f0f0f0;
}

.order-time {
	font-size: 24rpx;
	color: #999;
}

.order-total {
	font-size: 30rpx;
	font-weight: 600;
	color: #ee0a24;
}

.order-actions {
	display: flex;
	justify-content: flex-end;
	gap: 16rpx;
	margin-top: 20rpx;
}

.pagination {
	padding: 24rpx;
	text-align: center;
}

.no-more {
	color: #999;
	font-size: 24rpx;
}
</style>
