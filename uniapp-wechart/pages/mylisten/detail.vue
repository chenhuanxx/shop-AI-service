<template>
	<view class="detail-page">
		<!-- 顶部渐变背景 - 蓝色系 -->
		<view class="header-bg">
			<view class="header-content">
				<view class="course-icon reserved">
					<text class="icon-text">预</text>
				</view>
				<view class="course-title">{{ courseInfo.name || '预约详情' }}</view>
				<view class="course-meta">
					<view class="meta-item">
						<u-icon name="account" size="28" color="rgba(255,255,255,0.8)"></u-icon>
						<text class="meta-text">{{ courseInfo.teacherName || '未知教师' }}</text>
					</view>
					<view class="meta-item">
						<u-icon name="map" size="28" color="rgba(255,255,255,0.8)"></u-icon>
						<text class="meta-text">{{ courseInfo.location || '待定' }}</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 预约状态卡片 -->
		<view class="status-card">
			<view class="status-info">
				<view class="status-left">
					<view class="status-icon" :class="getStatusClass(courseInfo.reservationState)">
						<u-icon :name="getStatusIcon(courseInfo.reservationState)" size="36" :color="getStatusColor(courseInfo.reservationState)"></u-icon>
					</view>
					<view class="status-text-wrap">
						<text class="status-label">预约状态</text>
						<text class="status-value" :style="{ color: getStatusColor(courseInfo.reservationState) }">
							{{ courseInfo.reservationState || '已预约' }}
						</text>
					</view>
				</view>
				<view class="course-status-tag" :class="courseInfo.courseStatus === '未开始' ? 'upcoming' : 'ended'">
					{{ courseInfo.courseStatus === '未开始' ? '未开始' : '已结束' }}
				</view>
			</view>
			<view class="capacity-bar">
				<text class="bar-label">预约人数</text>
				<view class="bar-track">
					<view class="bar-fill" :style="{ width: getCapacityPercent() + '%' }"></view>
				</view>
				<text class="bar-count">{{ courseInfo.enrolledCount || 0 }}/{{ courseInfo.capacity || '--' }}</text>
			</view>
		</view>

		<!-- 课程信息卡片 -->
		<view class="info-card">
			<view class="card-header">
				<u-icon name="info-circle" size="32" color="#3B82F6"></u-icon>
				<text class="card-title">课程信息</text>
			</view>
			<view class="info-list">
				<view class="info-row">
					<text class="info-label">上课时间</text>
					<text class="info-value highlight">{{ courseInfo.startAt || '--' }}</text>
				</view>
				<view class="info-row">
					<text class="info-label">上课地点</text>
					<text class="info-value">{{ courseInfo.location || courseInfo.office || '--' }}</text>
				</view>
				<view class="info-row" v-if="courseInfo.academicYear">
					<text class="info-label">学年/学期</text>
					<text class="info-value">{{ courseInfo.academicYear }} / {{ courseInfo.term }}</text>
				</view>
				<view class="info-row" v-if="courseInfo.department">
					<text class="info-label">教学系部</text>
					<text class="info-value">{{ courseInfo.department }}</text>
				</view>
				<view class="info-row" v-if="courseInfo.className">
					<text class="info-label">开课班级</text>
					<text class="info-value">{{ courseInfo.className }}</text>
				</view>
			</view>
		</view>

		<!-- 课程详情卡片 -->
		<view class="detail-card" v-if="courseInfo.topic || courseInfo.course">
			<view class="card-header">
				<u-icon name="book" size="32" color="#3B82F6"></u-icon>
				<text class="card-title">课程详情</text>
			</view>
			<view class="detail-content" v-if="courseInfo.topic">
				<text class="detail-label">课题</text>
				<text class="detail-text">{{ courseInfo.topic }}</text>
			</view>
			<view class="detail-content" v-if="courseInfo.course">
				<text class="detail-label">课程内容</text>
				<text class="detail-text">{{ courseInfo.course }}</text>
			</view>
		</view>

		<!-- 签到区域卡片 -->
		<view class="checkin-card">
			<view class="checkin-header">
				<u-icon name="checkmark-circle" size="32" color="#3B82F6"></u-icon>
				<text class="checkin-title">听课签到</text>
			</view>

			<!-- 已签到状态 -->
			<view class="checkin-success" v-if="isCheckedIn">
				<view class="success-icon">
					<u-icon name="checkmark-circle-fill" size="96" color="#10B981"></u-icon>
				</view>
				<view class="success-text">
					<text class="success-label">签到成功</text>
					<text class="success-time" v-if="checkinTime">签到时间：{{ checkinTime }}</text>
				</view>
			</view>

			<!-- 可签到状态 -->
			<view class="checkin-available" v-else-if="courseInfo.courseStatus === '未开始'">
				<view class="checkin-tip warning">
					<u-icon name="clock" size="28" color="#F59E0B"></u-icon>
					<text class="tip-text">课程未开始，请等待开课</text>
				</view>
				<view class="btn-waiting">
					<u-icon name="clock" size="32" color="#9CA3AF"></u-icon>
					<text>等待开课</text>
				</view>
			</view>

			<!-- 课程已结束 -->
			<view class="checkin-ended" v-else>
				<view class="checkin-tip">
					<u-icon name="info-circle" size="28" color="#9CA3AF"></u-icon>
					<text class="tip-text">课程已结束</text>
				</view>
				<view class="btn-ended">
					<text>签到已截止</text>
				</view>
			</view>
		</view>

		<!-- 底部操作按钮 -->
		<view class="bottom-actions" v-if="canCancel">
			<button class="btn-cancel" :disabled="loading" @click="onCancelReservation">
				<u-icon name="close-circle" size="32" color="#EF4444"></u-icon>
				<text>取消预约</text>
			</button>
		</view>
	</view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store/auth'
import { api } from '@/utils/api.js'

type CourseDetail = {
	id?: string
	name?: string
	startAt?: string
	teacherName?: string
	enrolledCount?: number
	capacity?: number
	academicYear?: string
	term?: string
	department?: string
	className?: string
	office?: string
	location?: string
	course?: string
	topic?: string
	reservationState?: string
	courseStatus?: string
	canReserve?: boolean
	canCancel?: boolean
}

const auth = useAuthStore()
const courseId = ref('')
const courseInfo = ref<CourseDetail>({})
const isCheckedIn = ref(false)
const checkinTime = ref('')
const loading = ref(false)
const cancelLoading = ref(false)

// 可以取消预约：已预约状态 且 课程未开始 且 未签到
const canCancel = computed(() => {
	return courseInfo.value.reservationState === '已预约' &&
		courseInfo.value.courseStatus === '未开始' &&
		!isCheckedIn.value
})

function getStatusClass(status?: string): string {
	const map: Record<string, string> = {
		'已预约': 'info',
		'已签到': 'success',
		'可预约': 'success',
		'已满': 'warning',
		'已结束': 'default'
	}
	return map[status || ''] || 'info'
}

function getStatusIcon(status?: string): string {
	const map: Record<string, string> = {
		'已预约': 'checkmark-circle-fill',
		'已签到': 'checkmark-circle-fill',
		'可预约': 'plus-circle-fill',
		'已满': 'minus-circle-fill',
		'已结束': 'info-circle-fill'
	}
	return map[status || ''] || 'info-circle'
}

function getStatusColor(status?: string): string {
	const map: Record<string, string> = {
		'已预约': '#3B82F6',
		'已签到': '#10B981',
		'可预约': '#10B981',
		'已满': '#F59E0B',
		'已结束': '#9CA3AF'
	}
	return map[status || ''] || '#6B7280'
}

function getCapacityPercent(): number {
	const capacity = courseInfo.value.capacity || 0
	if (capacity === 0) return 0
	const enrolled = courseInfo.value.enrolledCount || 0
	return Math.min(100, (enrolled / capacity) * 100)
}

async function loadCourseDetail() {
	try {
		const res: any = await api.getCourseDetail({ id: courseId.value })
		if (res && res.id) {
			courseInfo.value = res
		}
	} catch (e) {
		console.error('加载课程详情失败', e)
	}
}

async function checkCheckinStatus() {
	try {
		const res: any = await api.checkinStatus({ courseId: courseId.value })
		isCheckedIn.value = res.checkedIn || false
	} catch (e) {
		console.error('检查签到状态失败', e)
	}
}

async function onCheckin() {
	if (loading.value) return
	loading.value = true
	try {
		const res: any = await api.checkin({ id: courseId.value })
		if (res && res.ok) {
			isCheckedIn.value = true
			checkinTime.value = new Date().toLocaleString('zh-CN')
			uni.showToast({ title: '签到成功', icon: 'success' })
		} else {
			uni.showToast({ title: res?.message || '签到失败', icon: 'none' })
		}
	} catch (e: any) {
		uni.showToast({ title: e?.message || '签到失败', icon: 'none' })
	} finally {
		loading.value = false
	}
}

// 取消预约
async function onCancelReservation() {
	if (cancelLoading.value) return

	uni.showModal({
		title: '确认取消',
		content: '确定要取消这个课程的预约吗？',
		confirmColor: '#EF4444',
		success: async (res) => {
			if (!res.confirm) return

			cancelLoading.value = true
			try {
				const result: any = await api.cancelReserve({ id: courseId.value })
				if (result && result.ok) {
					uni.showToast({ title: '已取消预约', icon: 'success' })
					// 重新获取课程详情以更新状态
					await loadCourseDetail()
					setTimeout(() => {
						uni.navigateBack()
					}, 1500)
				} else {
					uni.showToast({ title: result?.message || '取消失败', icon: 'none' })
				}
			} catch (e: any) {
				uni.showToast({ title: e?.message || '取消失败', icon: 'none' })
			} finally {
				cancelLoading.value = false
			}
		}
	})
}

onLoad((options: any) => {
	if (options?.id) {
		courseId.value = options.id
	}
})

onMounted(() => {
	if (!auth.ensureLoggedIn()) return
	if (courseId.value) {
		loadCourseDetail()
		checkCheckinStatus()
	}
})
</script>

<style lang="scss" scoped>
.detail-page {
	min-height: 100vh;
	background: #F7F8FA;
	padding-bottom: env(safe-area-inset-bottom);
}

.header-bg {
	background: linear-gradient(135deg, #3B82F6 0%, #6366F1 100%);
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

	&.reserved {
		background: rgba(16, 185, 129, 0.3);
	}

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
	box-shadow: 0 12rpx 40rpx rgba(59, 130, 246, 0.15);
}

.status-info {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 24rpx;
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

	&.success { background: rgba(16, 185, 129, 0.12); }
	&.warning { background: rgba(245, 158, 11, 0.12); }
	&.info { background: rgba(59, 130, 246, 0.12); }
	&.default { background: rgba(156, 163, 175, 0.12); }
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

.course-status-tag {
	padding: 8rpx 20rpx;
	border-radius: 999rpx;
	font-size: 24rpx;
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

.capacity-bar {
	display: flex;
	align-items: center;
	gap: 16rpx;

	.bar-label {
		font-size: 24rpx;
		color: #9CA3AF;
	}

	.bar-track {
		flex: 1;
		height: 8rpx;
		background: #E5E7EB;
		border-radius: 4rpx;
		overflow: hidden;

		.bar-fill {
			height: 100%;
			background: linear-gradient(90deg, #3B82F6, #6366F1);
			border-radius: 4rpx;
			transition: width 0.3s ease;
		}
	}

	.bar-count {
		font-size: 24rpx;
		color: #64748B;
		font-weight: 500;
	}
}

.info-card, .detail-card, .checkin-card {
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
			color: #3B82F6;
			font-weight: 600;
		}
	}
}

.detail-content {
	margin-bottom: 24rpx;

	&:last-child { margin-bottom: 0; }

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

.checkin-header {
	display: flex;
	align-items: center;
	gap: 12rpx;
	margin-bottom: 32rpx;

	.checkin-title {
		font-size: 32rpx;
		font-weight: 600;
		color: #1F2328;
	}
}

.checkin-success {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 32rpx 0;
	gap: 20rpx;

	.success-icon {
		width: 160rpx;
		height: 160rpx;
		border-radius: 50%;
		background: rgba(16, 185, 129, 0.1);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.success-text {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 8rpx;

		.success-label {
			font-size: 36rpx;
			font-weight: 600;
			color: #10B981;
		}
		.success-time {
			font-size: 26rpx;
			color: #9CA3AF;
		}
	}
}

.checkin-available, .checkin-ended {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 24rpx;
}

.checkin-tip {
	display: flex;
	align-items: center;
	gap: 12rpx;
	padding: 16rpx 24rpx;
	background: rgba(245, 158, 11, 0.1);
	border-radius: 12rpx;

	&.warning {
		background: rgba(245, 158, 11, 0.1);
		.tip-text {
			color: #F59E0B;
		}
	}

	&:not(.warning) {
		background: rgba(156, 163, 175, 0.1);
		.tip-text {
			color: #9CA3AF;
		}
	}

	.tip-text {
		font-size: 26rpx;
	}
}

.btn-waiting, .btn-ended {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 8rpx;
	width: 100%;
	height: 96rpx;
	border-radius: 48rpx;
	font-size: 28rpx;
	color: #9CA3AF;
	background: rgba(156, 163, 175, 0.12);
}

.bottom-actions {
	padding: 32rpx;
	padding-bottom: calc(32rpx + env(safe-area-inset-bottom));
}

.btn-cancel {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 12rpx;
	width: 100%;
	height: 96rpx;
	border-radius: 48rpx;
	font-size: 32rpx;
	font-weight: 600;
	color: #EF4444;
	background: rgba(239, 68, 68, 0.1);
	border: none;

	&[disabled] {
		opacity: 0.6;
	}

	&::after {
		border: none;
	}
}
</style>
