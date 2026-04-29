<template>
	<view class="listen-page">
		<!-- 顶部搜索栏 -->
		<view class="search-header">
			<view class="search-bar">
				<u-icon name="search" size="32" color="#999"></u-icon>
				<input class="search-input" placeholder="搜索课程" v-model="keyword" @confirm="onSearch" />
			</view>
		</view>

		<!-- 课程列表 -->
		<view class="content" v-if="list.length > 0">
			<view
				v-for="item in list"
				:key="item.id"
				class="course-card"
				@click="goCourseDetail(item)"
			>
				<view class="card-header">
					<view class="card-icon reserved">
						<text class="icon-text">预</text>
					</view>
					<view class="card-info">
						<view class="course-name">{{ item.name }}</view>
						<view class="course-meta">
							<u-icon name="account" size="24" color="#999"></u-icon>
							<text class="meta-text">{{ item.teacherName || '未知教师' }}</text>
						</view>
					</view>
					<view class="card-badge" :class="item.reservationState === '已签到' ? 'success' : 'info'">
						{{ item.reservationState || '已预约' }}
					</view>
				</view>
				<view class="card-footer">
					<view class="footer-item">
						<u-icon name="clock" size="26" color="#999"></u-icon>
						<text class="footer-text">{{ item.startAt || '--' }}</text>
					</view>
					<view class="footer-item">
						<view class="status-tag" :class="item.courseStatus === '未开始' ? 'upcoming' : 'ended'">
							{{ item.courseStatus || '未知' }}
						</view>
					</view>
				</view>
			</view>
			<view v-if="noMore" class="no-more">没有更多了</view>
		</view>

		<!-- 空状态 -->
		<view class="empty-state" v-else>
			<image src="/static/image/no_cuoti.png" mode="aspectFit" class="empty-image"></image>
			<text class="empty-text">暂无签到记录</text>
			<view class="empty-btn" @click="goToCourse">去预约课程</view>
		</view>
	</view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store/auth'
import { api } from '@/utils/api.js'

type CourseItem = {
	id?: string | number
	name?: string
	reservationState?: string
	courseStatus?: string
	teacherName?: string
	startAt?: string
	enrolledCount?: number
	capacity?: number
	canCancel?: boolean
	canReserve?: boolean
}

const auth = useAuthStore()

const keyword = ref('')
const list = ref<CourseItem[]>([])
const page = ref(1)
const pageSize = 20
const noMore = ref(false)
const total = ref(0)

async function fetchList(sty: 1 | 2 = 1) {
	try {
		if (sty === 1) {
			list.value = []
			page.value = 1
		}

		const data = await api.getMyCheckedInCourses({
			page: page.value,
			page_size: pageSize
		})
		const res = (data || {}) as any
		const listData = (res.list || []) as CourseItem[]
		total.value = Number(res.total || 0)

		if (sty === 2) {
			list.value = list.value.concat(listData)
		} else {
			list.value = listData
		}

		noMore.value = listData.length < pageSize || list.value.length >= total.value
	} finally {
		uni.stopPullDownRefresh()
	}
}

function onSearch() {
	page.value = 1
	noMore.value = false
	fetchList(1)
}

function goCourseDetail(item: CourseItem) {
	const id = item?.id
	if (!id) return
	uni.navigateTo({
		url: `/pages/mylisten/detail?id=${id}`
	})
}

function goToCourse() {
	uni.switchTab({ url: '/pages/task/index' })
}

onShow(() => {
	if (!auth.ensureLoggedIn()) return
	// 每次显示时刷新列表，确保取消预约等操作后状态正确
	page.value = 1
	noMore.value = false
	fetchList(1)
})

onPullDownRefresh(() => {
	page.value = 1
	noMore.value = false
	fetchList(1)
})

onReachBottom(() => {
	if (noMore.value) return
	page.value += 1
	fetchList(2)
})
</script>

<style lang="scss" scoped>
.listen-page {
	min-height: 100vh;
	background: #F7F8FA;
	padding-bottom: env(safe-area-inset-bottom);
}

.search-header {
	background: linear-gradient(135deg, #3B82F6 0%, #6366F1 100%);
	padding: 16rpx 24rpx;
	padding-top: calc(16rpx + env(safe-area-inset-top));
	box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.1);

	.search-bar {
		display: flex;
		align-items: center;
		background: rgba(255, 255, 255, 0.95);
		border-radius: 40rpx;
		padding: 16rpx 24rpx;
		gap: 16rpx;

		.search-input {
			flex: 1;
			height: 48rpx;
			font-size: 28rpx;
			color: #1F2328;
		}
	}
}

.content {
	padding: 24rpx;

	.course-card {
		background: #fff;
		border-radius: 20rpx;
		padding: 28rpx;
		margin-bottom: 24rpx;
		box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
		transition: transform 0.2s ease, box-shadow 0.2s ease;

		&:active {
			transform: scale(0.98);
			box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
		}
	}
}

.card-header {
	display: flex;
	align-items: flex-start;
	gap: 20rpx;
}

.card-icon {
	width: 88rpx;
	height: 88rpx;
	border-radius: 20rpx;
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;

	&.reserved {
		background: linear-gradient(135deg, #3B82F6 0%, #6366F1 100%);
	}

	.icon-text {
		font-size: 32rpx;
		font-weight: 600;
		color: #fff;
	}
}

.card-info {
	flex: 1;
	min-width: 0;
}

.course-name {
	font-size: 32rpx;
	font-weight: 600;
	color: #1F2328;
	line-height: 1.4;
	margin-bottom: 12rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.course-meta {
	display: flex;
	align-items: center;
	gap: 8rpx;

	.meta-text {
		font-size: 26rpx;
		color: #64748B;
	}
}

.card-badge {
	padding: 8rpx 20rpx;
	border-radius: 999rpx;
	font-size: 22rpx;
	font-weight: 500;
	flex-shrink: 0;

	&.success {
		background: rgba(16, 185, 129, 0.12);
		color: #10B981;
	}

	&.info {
		background: rgba(59, 130, 246, 0.12);
		color: #3B82F6;
	}

	&.warning {
		background: rgba(245, 158, 11, 0.12);
		color: #F59E0B;
	}
}

.card-footer {
	display: flex;
	align-items: center;
	gap: 24rpx;
	margin-top: 24rpx;
	padding-top: 24rpx;
	border-top: 1rpx solid #F1F3F5;

	.footer-item {
		display: flex;
		align-items: center;
		gap: 10rpx;

		.footer-text {
			font-size: 26rpx;
			color: #64748B;
		}
	}

	.status-tag {
		padding: 4rpx 12rpx;
		border-radius: 8rpx;
		font-size: 22rpx;
		font-weight: 500;

		&.upcoming {
			background: rgba(59, 130, 246, 0.12);
			color: #3B82F6;
		}

		&.ended {
			background: rgba(156, 163, 175, 0.12);
			color: #9CA3AF;
		}
	}
}

.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 120rpx 0;

	.empty-image {
		width: 240rpx;
		height: 240rpx;
		opacity: 0.6;
	}

	.empty-text {
		margin-top: 24rpx;
		font-size: 28rpx;
		color: #999;
	}

	.empty-btn {
		margin-top: 32rpx;
		padding: 20rpx 48rpx;
		background: linear-gradient(135deg, #3B82F6 0%, #6366F1 100%);
		color: #fff;
		font-size: 28rpx;
		border-radius: 40rpx;
	}
}

.no-more {
	text-align: center;
	padding: 32rpx;
	font-size: 26rpx;
	color: #999;
}
</style>
