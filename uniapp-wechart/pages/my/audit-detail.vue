<template>
	<view class="ui-page page">
		<view class="ui-card card">
			<view class="title">{{ detail.courseName || '公开课申请' }}</view>
			<view class="sub">{{ detail.topic || '--' }}</view>
		</view>

		<view class="ui-card card">
			<view class="ui-kv">
				<view class="ui-kv-label">申请人</view>
				<view class="ui-kv-value">{{ detail.userName || detail.userId || '—' }}</view>
			</view>
			<view class="ui-kv">
				<view class="ui-kv-label">系部</view>
				<view class="ui-kv-value">{{ detail.department || '—' }}</view>
			</view>
			<view class="ui-kv">
				<view class="ui-kv-label">班级</view>
				<view class="ui-kv-value">{{ detail.className || '—' }}</view>
			</view>
			<view class="ui-kv">
				<view class="ui-kv-label">教研室</view>
				<view class="ui-kv-value">{{ detail.office || '—' }}</view>
			</view>
			<view class="ui-kv">
				<view class="ui-kv-label">地点</view>
				<view class="ui-kv-value">{{ detail.location || '—' }}</view>
			</view>
			<view class="ui-kv">
				<view class="ui-kv-label">时间</view>
				<view class="ui-kv-value">
					{{ (detail.startDate || '—') + ' · 第' + (detail.weekNo || '-') + '周 · ' + (detail.weekday || '-') + ' · ' + (detail.section || '-') }}
				</view>
			</view>
			<view class="ui-kv">
				<view class="ui-kv-label">申报理由</view>
				<view class="ui-kv-value">{{ detail.applyReason || '—' }}</view>
			</view>
			<view class="ui-kv">
				<view class="ui-kv-label">申请时间</view>
				<view class="ui-kv-value">{{ detail.createdAt || '—' }}</view>
			</view>
		</view>

		<view class="ui-card card">
			<view class="label2 ui-section-title">审核意见（可选）</view>
			<u-input v-model="opinion" placeholder="请输入" clearable border="surround"></u-input>
		</view>

		<view class="bottom-actions" v-if="isAdmin">
			<u-button type="error" plain :loading="submitting" @click="submit('reject')">驳回</u-button>
			<u-button type="success" :loading="submitting" @click="submit('approve')">同意</u-button>
		</view>
	</view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store/auth'
import { api } from '@/utils/api.js'
import { showToast } from '@/utils/function'

type ApplicationDetail = {
	id?: string
	courseName?: string
	topic?: string
	department?: string
	className?: string
	office?: string
	location?: string
	startDate?: string
	weekNo?: number
	weekday?: string
	section?: string
	applyReason?: string
	createdAt?: string
	userId?: number
	userName?: string
	state?: string
	auditOpinion?: string
	auditedAt?: string
}

const auth = useAuthStore()
const isAdmin = computed(() => (auth.user?.role || auth.user?.username || '').toLowerCase() === 'admin')

const id = ref('')
const detail = ref<ApplicationDetail>({})
const opinion = ref('')
const submitting = ref(false)

async function fetchDetail() {
	if (!id.value) return
	try {
		const data = await api.myApplicationDetail({ id: id.value })
		detail.value = (data || {}) as ApplicationDetail
		opinion.value = ''
	} catch (e: any) {
		showToast(e?.message || '加载失败')
		detail.value = {}
	}
}

async function submit(action: 'approve' | 'reject') {
	if (!isAdmin.value) {
		showToast('无权限')
		return
	}
	if (!id.value) return
	if (submitting.value) return
	submitting.value = true
	try {
		await api.auditApplication({
			id: id.value,
			action,
			opinion: opinion.value.trim() || ''
		})
		showToast(action === 'approve' ? '已同意' : '已驳回', 'success')
		uni.navigateBack({ delta: 1 })
	} catch (e: any) {
		showToast(e?.message || '操作失败')
	} finally {
		submitting.value = false
	}
}

onLoad((options) => {
	if (!auth.ensureLoggedIn()) return
	if (!isAdmin.value) {
		showToast('无权限')
		uni.navigateBack({ delta: 1 })
		return
	}
	id.value = String((options as any)?.id || '').trim()
	fetchDetail()
})
</script>

<style scoped lang="scss">
	.page {
		padding-bottom: 160rpx;
	}
	.title {
		font-size: 32rpx;
		font-weight: 700;
		color: #1F2328;
	}
	.sub {
		margin-top: 10rpx;
		font-size: 24rpx;
		color: #64748B;
	}
	.label2 {
		margin-bottom: 14rpx;
	}
	.bottom-actions {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		padding: 20rpx 24rpx;
		background: rgba(255, 255, 255, 0.96);
		backdrop-filter: blur(12px);
		border-top: 1rpx solid rgba(15, 23, 42, 0.06);
		display: flex;
		gap: 20rpx;
	}
</style>
