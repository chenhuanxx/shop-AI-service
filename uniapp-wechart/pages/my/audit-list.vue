<template>
	<view class="ui-page page">
		<view class="ui-card search">
			<u-icon name="search" size="34" @click="onSearch"></u-icon>
			<input class="search-input" v-model="keyword" placeholder="搜索课程/课题/班级" />
			<view class="search-btn" @tap="onSearch">搜索</view>
		</view>

		<view class="list" v-if="items.length > 0">
			<view v-for="(item, idx) in items" :key="item.id || idx" class="ui-card card" @tap="goDetail(item)">
				<view class="row">
					<view class="title">{{ item.courseName || '公开课申请' }}</view>
					<view class="tag">待审批</view>
				</view>
				<view class="sub">{{ item.topic || '--' }}</view>
				<view class="meta">
					<view class="meta-item">申请人：{{ item.userName || item.userId || '—' }}</view>
					<view class="meta-item">时间：{{ item.startDate || '—' }}</view>
					<view class="meta-item">地点：{{ item.location || '—' }}</view>
				</view>
			</view>
			<view v-if="showMore" class="no-more">没有更多了</view>
		</view>
		<view class="no-data" v-else>
			<image src="/static/image/no_cuoti.png" alt=""></image>
		</view>
	</view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store/auth'
import { api } from '@/utils/api.js'
import { showToast } from '@/utils/function'

type ApplicationItem = {
	id?: string
	courseName?: string
	topic?: string
	startDate?: string
	location?: string
	state?: string
	createdAt?: string
	userId?: number
	userName?: string
}

const auth = useAuthStore()
const keyword = ref('')

const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)
const items = ref<ApplicationItem[]>([])
const showMore = ref(false)
const loading = ref(false)

function isAdmin() {
	return (auth.user?.role || auth.user?.username || '').toLowerCase() === 'admin'
}

function ensureAdmin() {
	if (!auth.ensureLoggedIn()) return false
	if (isAdmin()) return true
	showToast('无权限')
	uni.navigateBack({ delta: 1 })
	return false
}

function resetAndFetch() {
	pageNo.value = 1
	fetchList(1)
}

function onSearch() {
	resetAndFetch()
}

function goDetail(item: ApplicationItem) {
	const id = (item?.id || '').toString().trim()
	if (!id) return
	uni.navigateTo({ url: '/pages/my/audit-detail?id=' + encodeURIComponent(id) })
}

async function fetchList(sty: 1 | 2) {
	if (loading.value) return
	loading.value = true
	try {
		const data = await api.applicationsPage({
			page: pageNo.value,
			page_size: pageSize.value,
			keyword: keyword.value.trim() || undefined,
			state: '待审批'
		})
		const res = (data || {}) as any
		const list = (res.list || []) as ApplicationItem[]
		total.value = Number(res.total || 0)
		if (sty === 2) items.value = items.value.concat(list)
		else items.value = list

		const noMoreBySize = list.length < pageSize.value
		const noMoreByTotal = total.value > 0 && items.value.length >= total.value
		showMore.value = noMoreBySize || noMoreByTotal
	} catch (e: any) {
		showToast(e?.message || '请求失败')
		items.value = []
		total.value = 0
		showMore.value = false
	} finally {
		loading.value = false
		uni.stopPullDownRefresh()
	}
}

onLoad(() => {
	if (!ensureAdmin()) return
	resetAndFetch()
})

onShow(() => {
	if (!ensureAdmin()) return
})

onPullDownRefresh(() => {
	if (!ensureAdmin()) return
	resetAndFetch()
})

onReachBottom(() => {
	if (!ensureAdmin()) return
	if (showMore.value) return
	pageNo.value += 1
	if (items.value.length === 0) pageNo.value = 1
	fetchList(2)
})
</script>

<style scoped lang="scss">
	.search {
		display: flex;
		align-items: center;
		gap: 14rpx;
		padding: 18rpx 18rpx;
	}
	.search-input {
		flex: 1;
		font-size: 28rpx;
		color: #1F2328;
	}
	.search-btn {
		font-size: 26rpx;
		color: #2388FD;
		padding: 8rpx 12rpx;
	}
	.list {
		margin-top: 0;
	}
	.card {
		margin-bottom: 20rpx;
	}
	.row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 16rpx;
	}
	.title {
		flex: 1;
		font-size: 30rpx;
		font-weight: 600;
		color: #1F2328;
	}
	.tag {
		flex-shrink: 0;
		font-size: 22rpx;
		line-height: 44rpx;
		padding: 0 16rpx;
		border-radius: 999rpx;
		color: #D97706;
		background: rgba(217, 119, 6, 0.10);
	}
	.sub {
		margin-top: 10rpx;
		font-size: 24rpx;
		color: #64748B;
	}
	.meta {
		margin-top: 14rpx;
		display: flex;
		flex-direction: column;
		gap: 8rpx;
	}
	.meta-item {
		font-size: 24rpx;
		color: #7E858B;
	}
	.no-more {
		text-align: center;
		color: #94A3B8;
		font-size: 24rpx;
		padding: 18rpx 0 10rpx;
	}
</style>
