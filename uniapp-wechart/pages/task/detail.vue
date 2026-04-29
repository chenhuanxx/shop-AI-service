<template>
	<view class="detail-page">
		<!-- 顶部渐变背景 -->
		<view class="header-bg">
			<view class="header-content">
				<view class="course-icon">
					<text class="icon-text">课</text>
				</view>
				<view class="course-title">{{ goodDetail.name || '课程详情' }}</view>
				<view class="course-meta">
					<view class="meta-item">
						<u-icon name="account" size="28" color="rgba(255,255,255,0.8)"></u-icon>
						<text class="meta-text">{{ goodDetail.teacherName || '未知教师' }}</text>
					</view>
					<view class="meta-item">
						<u-icon name="map" size="28" color="rgba(255,255,255,0.8)"></u-icon>
						<text class="meta-text">{{ goodDetail.location || '待定' }}</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 预约状态卡片 -->
		<view class="status-card">
			<view class="status-info">
				<view class="status-left">
					<view class="status-icon" :class="getStatusClass(goodDetail.reservationState)">
						<u-icon :name="getStatusIcon(goodDetail.reservationState)" size="36" :color="getStatusColor(goodDetail.reservationState)"></u-icon>
					</view>
					<view class="status-text-wrap">
						<text class="status-label">预约状态</text>
						<text class="status-value" :style="{ color: getStatusColor(goodDetail.reservationState) }">
							{{ goodDetail.reservationState || '未知' }}
						</text>
					</view>
				</view>
				<view class="status-right">
					<text class="people-count">{{ goodDetail.enrolledCount || 0 }}</text>
					<text class="people-divider">/</text>
					<text class="people-total">{{ goodDetail.capacity || '--' }}</text>
					<text class="people-label">已预约人数</text>
				</view>
			</view>
			<!-- 课程状态标签 -->
			<view class="course-status-tag" v-if="goodDetail.courseStatus">
				<view class="status-dot" :class="goodDetail.courseStatus === '未开始' ? 'upcoming' : 'ended'"></view>
				<text>{{ goodDetail.courseStatus }}</text>
			</view>
		</view>

		<!-- 课程信息卡片 -->
		<view class="info-card">
			<view class="card-header">
				<u-icon name="info-circle" size="32" color="#667eea"></u-icon>
				<text class="card-title">课程信息</text>
			</view>
			<view class="info-list">
				<view class="info-row">
					<text class="info-label">上课时间</text>
					<text class="info-value highlight">{{ goodDetail.startAt || goodDetail.time || '--' }}</text>
				</view>
				<view class="info-row">
					<text class="info-label">上课地点</text>
					<text class="info-value">{{ goodDetail.location || '--' }}</text>
				</view>
				<view class="info-row" v-if="goodDetail.academicYear">
					<text class="info-label">学年/学期</text>
					<text class="info-value">{{ goodDetail.academicYear }} / {{ goodDetail.term }}</text>
				</view>
				<view class="info-row" v-if="goodDetail.department">
					<text class="info-label">教学系部</text>
					<text class="info-value">{{ goodDetail.department }}</text>
				</view>
				<view class="info-row" v-if="goodDetail.className">
					<text class="info-label">开课班级</text>
					<text class="info-value">{{ goodDetail.className }}</text>
				</view>
				<view class="info-row" v-if="goodDetail.office">
					<text class="info-label">教研室</text>
					<text class="info-value">{{ goodDetail.office }}</text>
				</view>
			</view>
		</view>

		<!-- 课程详情卡片 -->
		<view class="detail-card" v-if="goodDetail.topic || goodDetail.course">
			<view class="card-header">
				<u-icon name="book" size="32" color="#667eea"></u-icon>
				<text class="card-title">课程详情</text>
			</view>
			<view class="detail-content" v-if="goodDetail.topic">
				<text class="detail-label">课题</text>
				<text class="detail-text">{{ goodDetail.topic }}</text>
			</view>
			<view class="detail-content" v-if="goodDetail.course">
				<text class="detail-label">内容</text>
				<text class="detail-text">{{ goodDetail.course }}</text>
			</view>
		</view>

		<!-- 底部操作按钮 -->
		<view class="bottom-bar">
			<view class="bar-content">
				<view class="btn-cancel" v-if="goodDetail.canCancel" @click="onCancel">
					取消预约
				</view>
				<view class="btn-reserve" v-if="goodDetail.canReserve" @click="onReserve">
					<u-icon name="plus" size="32" color="#fff"></u-icon>
					<text>立即预约</text>
				</view>
				<view class="btn-disabled" v-if="!goodDetail.canReserve && !goodDetail.canCancel && (goodDetail.reservationState === '已预约' || goodDetail.reservationState === '已签到')">
					{{ goodDetail.reservationState === '已签到' ? '已完成签到' : '已预约，待开课' }}
				</view>
			</view>
		</view>
	</view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store/auth'
import { api } from '@/utils/api.js'
import { showToast } from '@/utils/function'

type CourseDetail = {
	id?: string
	name?: string
	topic?: string
	course?: string
	teacherName?: string
	startAt?: string
	time?: string
	enrolledCount?: number
	capacity?: number
	academicYear?: string
	term?: string
	department?: string
	className?: string
	office?: string
	location?: string
	courseStatus?: string
	canCancel?: boolean
	canReserve?: boolean
	reservationState?: string
	state?: string
	applyReason?: string
	auditOpinion?: string
	auditedAt?: string
}

const auth = useAuthStore()

const id = ref('')
const mode = ref<'course' | 'apply'>('course')
const goodDetail = ref<CourseDetail>({})

function getStatusClass(status?: string): string {
	const map: Record<string, string> = {
		'可预约': 'success',
		'已满': 'warning',
		'已预约': 'info',
		'已签到': 'success',
		'未开始': 'info',
		'已结束': 'default'
	}
	return map[status || ''] || 'default'
}

function getStatusIcon(status?: string): string {
	const map: Record<string, string> = {
		'可预约': 'checkmark-circle-fill',
		'已满': 'minus-circle-fill',
		'已预约': 'checkmark-circle-fill',
		'已签到': 'checkmark-circle-fill',
		'未开始': 'clock-fill',
		'已结束': 'info-circle-fill'
	}
	return map[status || ''] || 'info-circle'
}

function getStatusColor(status?: string): string {
	const map: Record<string, string> = {
		'可预约': '#10B981',
		'已满': '#F59E0B',
		'已预约': '#3B82F6',
		'已签到': '#10B981',
		'未开始': '#3B82F6',
		'已结束': '#9CA3AF'
	}
	return map[status || ''] || '#6B7280'
}

async function fetchDetail() {
	if (!id.value) return
	if (mode.value === 'apply') {
		const data = await api.myApplicationDetail({ id: id.value })
		const d = (data || {}) as any
		goodDetail.value = {
			id: String(d.id || ''),
			name: d.courseName || d.name || '',
			course: d.category || d.course || '',
			topic: d.topic || '',
			teacherName: '我',
			startAt: buildApplyStartAt(d),
			academicYear: d.academicYear || '',
			term: d.term || '',
			department: d.department || '',
			className: d.className || '',
			office: d.office || '',
			location: d.location || '',
			state: d.state || '',
			applyReason: d.applyReason || '',
			auditOpinion: d.auditOpinion || '',
			auditedAt: d.auditedAt || '',
		}
		return
	}
	const data = await api.getCourseDetail({ id: id.value })
	goodDetail.value = (data || {}) as CourseDetail
}

async function onReserve() {
	if (!id.value) return
	try {
		await api.reserve({ id: id.value })
		showToast('预约成功', 'success')
		await fetchDetail()
		// 刷新列表
		const pages = getCurrentPages()
		const prevPage = pages[pages.length - 2]
		if (prevPage) {
			prevPage.onLoad()
		}
	} catch (e: any) {
		showToast(e?.message || '预约失败', 'error')
	}
}

async function onCancel() {
	if (!id.value) return
	try {
		await api.cancelReserve({ id: id.value })
		showToast('已取消预约', 'success')
		await fetchDetail()
		// 刷新列表
		const pages = getCurrentPages()
		const prevPage = pages[pages.length - 2]
		if (prevPage) {
			prevPage.onLoad()
		}
		uni.navigateBack()
	} catch (e: any) {
		showToast(e?.message || '取消失败', 'error')
	}
}

function buildApplyStartAt(d: any) {
	const date = (d?.startDate || '').trim()
	const weekday = (d?.weekday || '').trim()
	const weekNo = d?.weekNo ? String(d.weekNo) : ''
	const section = (d?.section || '').trim()
	const parts: string[] = []
	if (date) parts.push(date)
	if (weekday) parts.push(weekday)
	if (weekNo) parts.push('第' + weekNo + '周')
	if (section) parts.push(section)
	return parts.join(' ')
}

onLoad((options) => {
	if (!auth.ensureLoggedIn()) return
	id.value = (options as any)?.id ? String((options as any).id) : ''
	mode.value = ((options as any)?.mode ? String((options as any).mode) : 'course') === 'apply' ? 'apply' : 'course'
	fetchDetail().catch(() => { })
})
</script>

<style lang="scss" scoped>
.detail-page {
	min-height: 100vh;
	background: #F7F8FA;
	padding-bottom: 140rpx;
}

.header-bg {
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	padding: 100rpx 40rpx 100rpx;
	border-radius: 0 0 48rpx 48rpx;
}

.header-content {
	display: flex;
	flex-direction: column;
	align-items: center;
}

.course-icon {
	width: 140rpx;
	height: 140rpx;
	border-radius: 36rpx;
	background: rgba(255, 255, 255, 0.2);
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 28rpx;

	.icon-text {
		font-size: 56rpx;
		font-weight: 700;
		color: #fff;
	}
}

.course-title {
	font-size: 40rpx;
	font-weight: 700;
	color: #fff;
	text-align: center;
	margin-bottom: 20rpx;
}

.course-meta {
	display: flex;
	flex-wrap: wrap;
	justify-content: center;
	gap: 32rpx;

	.meta-item {
		display: flex;
		align-items: center;
		gap: 8rpx;

		.meta-text {
			font-size: 28rpx;
			color: rgba(255, 255, 255, 0.9);
		}
	}
}

.status-card {
	margin: -60rpx 32rpx 24rpx;
	background: #fff;
	border-radius: 24rpx;
	padding: 32rpx;
	box-shadow: 0 12rpx 40rpx rgba(102, 126, 234, 0.15);
}

.status-info {
	display: flex;
	justify-content: space-between;
	align-items: center;
}

.status-left {
	display: flex;
	align-items: center;
	gap: 20rpx;
}

.status-icon {
	width: 80rpx;
	height: 80rpx;
	border-radius: 20rpx;
	display: flex;
	align-items: center;
	justify-content: center;

	&.success {
		background: rgba(16, 185, 129, 0.12);
	}
	&.warning {
		background: rgba(245, 158, 11, 0.12);
	}
	&.info {
		background: rgba(59, 130, 246, 0.12);
	}
	&.default {
		background: rgba(156, 163, 175, 0.12);
	}
}

.status-text-wrap {
	display: flex;
	flex-direction: column;
	gap: 4rpx;

	.status-label {
		font-size: 24rpx;
		color: #9CA3AF;
	}
	.status-value {
		font-size: 32rpx;
		font-weight: 600;
	}
}

.status-right {
	display: flex;
	flex-direction: column;
	align-items: flex-end;
	gap: 2rpx;

	.people-count {
		font-size: 40rpx;
		font-weight: 700;
		color: #667eea;
	}
	.people-divider {
		font-size: 28rpx;
		color: #9CA3AF;
	}
	.people-total {
		font-size: 28rpx;
		color: #9CA3AF;
	}
	.people-label {
		font-size: 22rpx;
		color: #9CA3AF;
		margin-top: 4rpx;
	}
}

.course-status-tag {
	display: flex;
	align-items: center;
	gap: 8rpx;
	margin-top: 20rpx;
	padding-top: 20rpx;
	border-top: 1rpx solid #F1F3F5;
	font-size: 24rpx;
	color: #64748B;

	.status-dot {
		width: 12rpx;
		height: 12rpx;
		border-radius: 50%;

		&.upcoming {
			background: #3B82F6;
		}
		&.ended {
			background: #9CA3AF;
		}
	}
}

.info-card, .detail-card {
	margin: 0 32rpx 24rpx;
	background: #fff;
	border-radius: 24rpx;
	padding: 32rpx;
	box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
}

.card-header {
	display: flex;
	align-items: center;
	gap: 12rpx;
	margin-bottom: 24rpx;
	padding-bottom: 20rpx;
	border-bottom: 1rpx solid #F1F3F5;

	.card-title {
		font-size: 32rpx;
		font-weight: 600;
		color: #1F2328;
	}
}

.info-list {
	display: flex;
	flex-direction: column;
	gap: 20rpx;
}

.info-row {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 12rpx 0;

	.info-label {
		font-size: 28rpx;
		color: #64748B;
	}
	.info-value {
		font-size: 28rpx;
		color: #1F2328;
		font-weight: 500;
		&.highlight {
			color: #667eea;
			font-weight: 600;
		}
	}
}

.detail-content {
	margin-bottom: 24rpx;

	&:last-child {
		margin-bottom: 0;
	}

	.detail-label {
		display: block;
		font-size: 24rpx;
		color: #9CA3AF;
		margin-bottom: 8rpx;
	}
	.detail-text {
		display: block;
		font-size: 28rpx;
		color: #64748B;
		line-height: 1.6;
	}
}

.bottom-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(255, 255, 255, 0.98);
	backdrop-filter: blur(12px);
	border-top: 1rpx solid rgba(0, 0, 0, 0.06);
	padding: 20rpx 32rpx;
	padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
	z-index: 100;
}

.bar-content {
	display: flex;
	gap: 24rpx;
}

.btn-cancel, .btn-reserve, .btn-disabled {
	flex: 1;
	height: 96rpx;
	border-radius: 48rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 8rpx;
	font-size: 32rpx;
	font-weight: 600;
}

.btn-cancel {
	background: #fff;
	color: #64748B;
	border: 2rpx solid #E5E7EB;
}

.btn-reserve {
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	color: #fff;
	box-shadow: 0 12rpx 32rpx rgba(102, 126, 234, 0.35);
}

.btn-disabled {
	background: rgba(156, 163, 175, 0.12);
	color: #9CA3AF;
}
</style>
