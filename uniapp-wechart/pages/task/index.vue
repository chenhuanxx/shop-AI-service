<template>
	<view class="course-page">
		<!-- 固定顶部搜索栏 -->
		<view class="search-header">
			<view class="search-bar">
				<u-icon name="search" size="32" color="#999"></u-icon>
				<input class="search-input" placeholder="搜索课程" v-model="form.title" @confirm="goSearch" />
			</view>
		</view>

		<!-- 标签页 -->
		<view class="tab-bar">
			<view
				v-for="(tab, index) in tabs"
				:key="index"
				class="tab-item"
				:class="{ active: activeTab === index }"
				@click="switchTab(index)"
			>
				<text class="tab-text">{{ tab }}</text>
				<view class="tab-indicator" v-if="activeTab === index"></view>
			</view>
		</view>

		<!-- 预约中心 - 可预约课程 -->
		<view v-if="activeTab === 0" class="content">
			<view class="course-list" v-if="availableList.length > 0">
				<view
					v-for="item in availableList"
					:key="item.id"
					class="course-card"
					@click="goCourseDetail(item)"
				>
					<view class="card-header">
						<view class="card-icon">
							<text class="icon-text">课</text>
						</view>
						<view class="card-info">
							<view class="course-name">{{ item.name }}</view>
							<view class="course-meta">
								<u-icon name="account" size="24" color="#999"></u-icon>
								<text class="meta-text">{{ item.teacherName || '未知教师' }}</text>
							</view>
						</view>
						<view class="card-badge success">
							可预约
						</view>
					</view>
					<view class="card-footer">
						<view class="footer-item">
							<u-icon name="clock" size="26" color="#999"></u-icon>
							<text class="footer-text">{{ item.startAt || '--' }}</text>
						</view>
						<view class="footer-item">
							<u-icon name="account" size="26" color="#999"></u-icon>
							<text class="footer-text">{{ item.enrolledCount || 0 }}/{{ item.capacity || '--' }}</text>
						</view>
						<view class="footer-item capacity-indicator">
							<text class="capacity-text" :class="{ 'low': getCapacityPercent(item) < 20 }">
								剩余 {{ getRemaining(item) }} 名
							</text>
						</view>
					</view>
				</view>
				<view v-if="availableShowMore" class="no-more">没有更多了</view>
			</view>
			<view class="empty-state" v-else>
				<image src="/static/image/no_cuoti.png" mode="aspectFit" class="empty-image"></image>
				<text class="empty-text">暂无可预约课程</text>
			</view>
		</view>

		<!-- 预约中心 - 已预约课程 -->
		<view v-else-if="activeTab === 1" class="content">
			<view class="course-list" v-if="reservedList.length > 0">
				<view
					v-for="item in reservedList"
					:key="item.id"
					class="course-card"
					@click="goCourseDetail(item)"
				>
					<view class="card-header">
						<view class="card-icon reserved">
							<text class="icon-text">已</text>
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
							<u-icon name="account" size="26" color="#999"></u-icon>
							<text class="footer-text">{{ item.enrolledCount || 0 }}/{{ item.capacity || '--' }}</text>
						</view>
						<view class="footer-item">
							<view class="status-tag" :class="item.courseStatus === '未开始' ? 'upcoming' : 'ended'">
								{{ item.courseStatus || '未知' }}
							</view>
						</view>
					</view>
				</view>
				<view v-if="reservedShowMore" class="no-more">没有更多了</view>
			</view>
			<view class="empty-state" v-else>
				<image src="/static/image/no_cuoti.png" mode="aspectFit" class="empty-image"></image>
				<text class="empty-text">暂无已预约课程</text>
			</view>
		</view>

		<!-- 课程申请 -->
		<view v-else-if="activeTab === 2" class="content">
			<view class="apply-list" v-if="applyList.length > 0">
				<view
					v-for="(item, idx) in applyList"
					:key="item.id || idx"
					class="apply-card"
					@click="goApplyDetail(item)"
				>
					<view class="apply-header">
						<view class="apply-icon">
							<text class="icon-text">申</text>
						</view>
						<view class="apply-info">
							<view class="apply-title">{{ item.courseName || '课程申请' }}</view>
							<view class="apply-topic">{{ item.topic || '--' }}</view>
						</view>
						<view class="apply-status" :class="getApplyStatusClass(item.state)">
							{{ item.state || '' }}
						</view>
					</view>
					<view class="apply-details">
						<view class="detail-row">
							<u-icon name="calendar" size="28" color="#64748B"></u-icon>
							<text class="detail-text">时间：{{ item.startDate || '--' }}</text>
						</view>
						<view class="detail-row">
							<u-icon name="map" size="28" color="#64748B"></u-icon>
							<text class="detail-text">地点：{{ item.location || '--' }}</text>
						</view>
					</view>
					<view class="apply-arrow">
						<u-icon name="arrow-right" size="32" color="#B6BDC4"></u-icon>
					</view>
				</view>
				<view v-if="applyShowMore" class="no-more">没有更多了</view>
			</view>
			<view class="empty-state" v-else>
				<image src="/static/image/no_cuoti.png" mode="aspectFit" class="empty-image"></image>
				<text class="empty-text">暂无申请记录</text>
			</view>
		</view>

	</view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
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

type ApplicationItem = {
	id?: string
	category?: string
	academicYear?: string
	term?: string
	department?: string
	className?: string
	office?: string
	courseName?: string
	topic?: string
	startDate?: string
	location?: string
	state?: string
	weekNo?: number
	weekday?: string
	section?: string
	applyReason?: string
	auditOpinion?: string
	auditedAt?: string
}

const auth = useAuthStore()

const tabs = ['可预约', '已预约', '课程申请']

const form = reactive({
	pageNo: 1,
	pageSize: 20,
	title: ''
})

const activeTab = ref<0 | 1 | 2>(0)

// 可预约课程列表
const availableList = ref<CourseItem[]>([])
const availableShowMore = ref(false)
const availableTotal = ref(0)

// 已预约课程列表
const reservedList = ref<CourseItem[]>([])
const reservedShowMore = ref(false)
const reservedTotal = ref(0)

// 申请列表
const applyList = ref<ApplicationItem[]>([])
const applyShowMore = ref(false)
const applyTotal = ref(0)

function switchTab(next: 0 | 1 | 2) {
	if (activeTab.value === next) return
	activeTab.value = next
	form.pageNo = 1
	if (next === 0) getAvailableList(1)
	else if (next === 1) getReservedList(1)
	else if (next === 2) getApplyList(1)
}

function resetAndFetch() {
	form.pageNo = 1
	if (activeTab.value === 0) getAvailableList(1)
	else if (activeTab.value === 1) getReservedList(1)
	else if (activeTab.value === 2) getApplyList(1)
}

function getApplyStatusClass(state?: string): string {
	const map: Record<string, string> = {
		'待审核': 'warning',
		'已通过': 'success',
		'已拒绝': 'danger',
		'已撤回': 'default'
	}
	return map[state || ''] || 'default'
}

// 计算剩余名额
function getRemaining(item: CourseItem): number {
	const capacity = item.capacity || 0
	const enrolled = item.enrolledCount || 0
	return Math.max(0, capacity - enrolled)
}

// 计算名额百分比
function getCapacityPercent(item: CourseItem): number {
	const capacity = item.capacity || 0
	if (capacity === 0) return 100
	const enrolled = item.enrolledCount || 0
	return ((capacity - enrolled) / capacity) * 100
}

function goCourseDetail(item: CourseItem) {
	const id = item?.id
	if (!id) return
	// 已预约课程跳转到预约详情页
	if (activeTab.value === 1) {
		uni.navigateTo({
			url: `/pages/mylisten/detail?id=${id}`
		})
	} else {
		uni.navigateTo({
			url: `/pages/task/detail?id=${id}`
		})
	}
}

function goSearch() {
	resetAndFetch()
}

onShow(() => {
	if (!auth.ensureLoggedIn()) return
	if (activeTab.value === 0 && availableList.value.length === 0) {
		getAvailableList(1)
	}
})

onPullDownRefresh(() => {
	resetAndFetch()
})

onReachBottom(() => {
	if (activeTab.value === 0) {
		if (availableShowMore.value) return
	} else if (activeTab.value === 1) {
		if (reservedShowMore.value) return
	} else if (activeTab.value === 2) {
		if (applyShowMore.value) return
	}
	form.pageNo += 1
	if (activeTab.value === 0) {
		if (availableList.value.length === 0) form.pageNo = 1
		getAvailableList(2)
	} else if (activeTab.value === 1) {
		if (reservedList.value.length === 0) form.pageNo = 1
		getReservedList(2)
	} else if (activeTab.value === 2) {
		if (applyList.value.length === 0) form.pageNo = 1
		getApplyList(2)
	}
})

// 获取可预约课程列表
async function getAvailableList(sty: 1 | 2) {
	try {
		const data = await api.getAvailableCourses({
			keyword: form.title || '',
			page: form.pageNo,
			page_size: form.pageSize
		})
		availableShowMore.value = false
		const res = (data || {}) as any
		const list = (res.list || []) as CourseItem[]
		availableTotal.value = Number(res.total || 0)

		if (sty === 2) availableList.value = availableList.value.concat(list)
		else availableList.value = list

		const noMoreBySize = list.length < form.pageSize
		const noMoreByTotal = availableTotal.value > 0 && availableList.value.length >= availableTotal.value
		availableShowMore.value = noMoreBySize || noMoreByTotal
	} finally {
		uni.stopPullDownRefresh()
	}
}

// 获取已预约课程列表
async function getReservedList(sty: 1 | 2) {
	try {
		const data = await api.getMyReservedCourses({
			page: form.pageNo,
			page_size: form.pageSize
		})
		reservedShowMore.value = false
		const res = (data || {}) as any
		const list = (res.list || []) as CourseItem[]
		reservedTotal.value = Number(res.total || 0)

		if (sty === 2) reservedList.value = reservedList.value.concat(list)
		else reservedList.value = list

		const noMoreBySize = list.length < form.pageSize
		const noMoreByTotal = reservedTotal.value > 0 && reservedList.value.length >= reservedTotal.value
		reservedShowMore.value = noMoreBySize || noMoreByTotal
	} finally {
		uni.stopPullDownRefresh()
	}
}

function goApplyDetail(item: ApplicationItem) {
	const id = (item?.id || '').trim()
	if (!id) return
	uni.navigateTo({
		url: '/pages/task/detail?mode=apply&id=' + encodeURIComponent(String(id))
	})
}

async function getApplyList(sty: 1 | 2) {
	try {
		const data = await api.myApplicationsPage({
			keyword: form.title || '',
			page: form.pageNo,
			page_size: form.pageSize
		})
		applyShowMore.value = false
		const res = (data || {}) as any
		const list = (res.list || []) as ApplicationItem[]
		applyTotal.value = Number(res.total || 0)

		if (sty === 2) applyList.value = applyList.value.concat(list)
		else applyList.value = list

		const noMoreBySize = list.length < form.pageSize
		const noMoreByTotal = applyTotal.value > 0 && applyList.value.length >= applyTotal.value
		applyShowMore.value = noMoreBySize || noMoreByTotal
	} finally {
		uni.stopPullDownRefresh()
	}
}
</script>

<style lang="scss" scoped>
.course-page {
	min-height: 100vh;
	background: #F7F8FA;
	padding-bottom: env(safe-area-inset-bottom);
}

// 固定顶部搜索栏
.search-header {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 999;
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.08);

		.search-input {
			flex: 1;
			height: 48rpx;
			font-size: 28rpx;
			color: #1F2328;
		}
	}
}

// 标签栏
.tab-bar {
	position: fixed;
	top: calc(108rpx + env(safe-area-inset-top));
	left: 0;
	right: 0;
	z-index: 998;
	display: flex;
	background: #fff;
	padding: 0 24rpx;
	box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);

	.tab-item {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 24rpx 0;
		position: relative;

		.tab-text {
			font-size: 30rpx;
			color: #64748B;
			font-weight: 500;
			transition: all 0.3s ease;
		}

		.tab-indicator {
			position: absolute;
			bottom: 0;
			left: 50%;
			transform: translateX(-50%);
			width: 48rpx;
			height: 6rpx;
			background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
			border-radius: 3rpx;
		}

		&.active .tab-text {
			color: #667eea;
			font-weight: 600;
		}
	}
}

// 内容区域
.content {
	padding: 180rpx 24rpx 24rpx;
}

// 课程卡片
.course-list {
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

	&.warning {
		background: rgba(245, 158, 11, 0.12);
		color: #F59E0B;
	}

	&.info {
		background: rgba(59, 130, 246, 0.12);
		color: #3B82F6;
	}

	&.danger {
		background: rgba(239, 68, 68, 0.12);
		color: #EF4444;
	}

	&.default {
		background: rgba(107, 114, 128, 0.12);
		color: #6B7280;
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

	.capacity-indicator {
		margin-left: auto;
	}

	.capacity-text {
		font-size: 24rpx;
		color: #10B981;
		font-weight: 500;

		&.low {
			color: #EF4444;
		}
	}

	.status-tag {
		padding: 4rpx 12rpx;
		border-radius: 8rpx;
		font-size: 22rpx;
		font-weight: 500;

		&.upcoming {
			background: rgba(16, 185, 129, 0.12);
			color: #10B981;
		}

		&.ended {
			background: rgba(107, 114, 128, 0.12);
			color: #6B7280;
		}
	}
}

// 申请卡片
.apply-list {
	.apply-card {
		background: #fff;
		border-radius: 20rpx;
		padding: 28rpx;
		margin-bottom: 24rpx;
		box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
		position: relative;
		transition: transform 0.2s ease, box-shadow 0.2s ease;

		&:active {
			transform: scale(0.98);
			box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
		}
	}
}

.apply-header {
	display: flex;
	align-items: flex-start;
	gap: 20rpx;
}

.apply-icon {
	width: 88rpx;
	height: 88rpx;
	border-radius: 20rpx;
	background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;

	.icon-text {
		font-size: 32rpx;
		font-weight: 600;
		color: #fff;
	}
}

.apply-info {
	flex: 1;
	min-width: 0;
}

.apply-title {
	font-size: 32rpx;
	font-weight: 600;
	color: #1F2328;
	margin-bottom: 8rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.apply-topic {
	font-size: 26rpx;
	color: #64748B;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.apply-status {
	padding: 8rpx 20rpx;
	border-radius: 999rpx;
	font-size: 22rpx;
	font-weight: 500;
	flex-shrink: 0;

	&.success {
		background: rgba(16, 185, 129, 0.12);
		color: #10B981;
	}

	&.warning {
		background: rgba(245, 158, 11, 0.12);
		color: #F59E0B;
	}

	&.danger {
		background: rgba(239, 68, 68, 0.12);
		color: #EF4444;
	}

	&.default {
		background: rgba(107, 114, 128, 0.12);
		color: #6B7280;
	}
}

.apply-details {
	margin-top: 20rpx;
	padding-top: 20rpx;
	border-top: 1rpx solid #F1F3F5;

	.detail-row {
		display: flex;
		align-items: center;
		gap: 12rpx;
		margin-bottom: 12rpx;

		&:last-child {
			margin-bottom: 0;
		}

		.detail-text {
			font-size: 26rpx;
			color: #64748B;
		}
	}
}

.apply-arrow {
	position: absolute;
	right: 28rpx;
	top: 50%;
	transform: translateY(-50%);
}

// 空状态
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
}

// 没有更多
.no-more {
	text-align: center;
	padding: 32rpx;
	font-size: 26rpx;
	color: #999;
}
</style>
