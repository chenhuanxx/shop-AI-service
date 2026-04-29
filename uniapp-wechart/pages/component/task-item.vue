<template>
	<view class="box">
		<view v-for="(item, index) in taskList" :key="index">
			<view class="task-card" @tap="goDetial(item)">
				<view class="card-content">
					<view class="row">
						<view class="title">{{ item.name || '' }}</view>
						<view class="right">
							<view class="state" :class="stateClass(item.reservationState)">{{ item.reservationState || '' }}</view> 
						</view>
					</view>
					<view class="meta">
						<view class="meta-item">教师：{{ item.teacherName || '—' }}</view>
						<view class="meta-item">时间：{{ item.startAt || '—' }}</view>
						<view class="meta-item">人数：{{ item.enrolledCount || 0 }}/{{ item.capacity || 0 }}</view>
					</view> 
				</view>
			</view>
		</view>
	</view>
</template>

<script setup lang="ts">
import { api } from '@/utils/api.js'
import { showToast } from '@/utils/function'

type CourseItem = {
	id?: string | number
	name?: string
	reservationState?: string
	teacherName?: string
	startAt?: string
	enrolledCount?: number
	capacity?: number
	canCancel?: boolean
	canReserve?: boolean
}

withDefaults(
	defineProps<{
		taskList: CourseItem[]
	}>(),
	{
		taskList: () => []
	}
)

const emit = defineEmits<{
	(e: 'refresh'): void
}>()

function stateClass(s?: string) {
	const v = (s || '').trim()
	if (v === '已预约') return 'is-success'
	if (v === '未预约') return 'is-default'
	if (v) return 'is-warn'
	return 'is-default'
}

function goDetial(val: CourseItem) {
	const id = val?.id ?? ''
	uni.navigateTo({
		url: '/pages/task/detail?id=' + encodeURIComponent(String(id))
	})
}

async function reserve(item: CourseItem) {
	const id = item?.id ?? ''
	if (!id) return
	try {
		await api.reserve({ id })
		showToast('已预约', 'success')
		emit('refresh')
	} catch {}
}

async function cancel(item: CourseItem) {
	const id = item?.id ?? ''
	if (!id) return
	try {
		await api.cancel({ id })
		showToast('已取消预约', 'success')
		emit('refresh')
	} catch {}
}
</script>

<style lang="scss" scoped>
	.box {

		.task-card {
			position: relative;
			margin-bottom: 23rpx;
			padding: 26rpx 26rpx;
			background: #FFFFFF;
			box-shadow: 0 12rpx 26rpx rgba(15, 23, 42, 0.06);
			border-radius: 16rpx;
			border: 1rpx solid rgba(15, 23, 42, 0.06);


			.card-content {
				display: flex;
				flex-direction: column;
				gap: 14rpx;

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
					color: #333333;
				}

				.right {
					display: flex;
					align-items: center;
					gap: 10rpx;
					flex-shrink: 0;
				}

				.state {
					font-size: 22rpx;
					line-height: 44rpx;
					padding: 0 16rpx;
					border-radius: 999rpx;
					flex-shrink: 0;
					border: 1rpx solid rgba(15, 23, 42, 0.06);
				}

				.is-success {
					color: #16A34A;
					background: rgba(22, 163, 74, 0.08);
				}

				.is-default {
					color: #64748B;
					background: rgba(100, 116, 139, 0.08);
				}

				.is-warn {
					color: #D97706;
					background: rgba(217, 119, 6, 0.10);
				}
 

				.meta {
					display: flex;
					flex-direction: column;
					gap: 6rpx;
				}

				.meta-item {
					font-size: 24rpx;
					color: #7E858B;
				}

				.actions {
					display: flex;
					justify-content: flex-end;
				}
			}


		}
	}
</style>
