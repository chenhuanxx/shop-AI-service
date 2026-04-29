<template>
	<view class="ui-page settings">
		<view class="ui-card user-card">
			<view class="row head">
				<image class="avatar" :src="userInfo.avatarUrl || '/static/logo.png'"></image>
				<view class="info">
					<view class="username">{{ userInfo.nickname || userInfo.username || '未登录' }}</view>
					<view class="role-tag">{{ userInfo.role || '' }}</view>
				</view>
			</view>
		</view>
		
		<!-- 其他设置 -->
		<view class="section">
			<view class="section-title">其他</view>
			<view class="info-card">
				<view class="info-item no-border" @click="clearCache">
					<view class="info-left">
						<text class="info-icon">🗑️</text>
						<text class="info-label">清理缓存</text>
					</view>
					<view class="info-right">
						<text class="info-value">{{ cacheSize }}</text>
						<text class="arrow">›</text>
					</view>
				</view>
				<view class="info-item no-border" @click="checkVersion">
					<view class="info-left">
						<text class="info-icon">ℹ️</text>
						<text class="info-label">版本信息</text>
					</view>
					<view class="info-right">
						<text class="info-value">v1.0.0</text>
						<text class="arrow">›</text>
					</view>
				</view>
			</view>
		</view>
		
		<!-- 退出登录 -->
		<view class="logout-section">
			<u-button type="error" block @click="logout">退出登录</u-button>
		</view>
	</view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
const userInfo = computed(() => auth.user || ({} as any))
const isAdmin = computed(() => (auth.user?.role || auth.user?.username || '').toLowerCase() === 'admin')
const cacheSize = ref('0 KB')

function goBack() {
	uni.navigateBack({
		delta: 1,
		fail: () => {
			uni.switchTab({ url: '/pages/my/index' })
		}
	})
}

async function refresh() {
	try {
		await auth.fetchMe()
		uni.showToast({ title: '已同步', icon: 'success' })
	} catch {
		uni.showToast({ title: '同步失败', icon: 'error' })
	}
}

function logout() {
	uni.showModal({
		title: '提示',
		content: '确定退出登录吗？',
		success: (res) => {
			if (res.confirm) {
				auth.logout()
				uni.showToast({ title: '已退出', icon: 'success' })
				setTimeout(() => {
					uni.redirectTo({ url: '/pages/login/login' })
				}, 1000)
			}
		}
	})
}

function clearCache() {
	uni.showModal({
		title: '提示',
		content: '确定清理缓存吗？',
		success: (res) => {
			if (res.confirm) {
				cacheSize.value = '0 KB'
				uni.showToast({ title: '清理成功', icon: 'success' })
			}
		}
	})
}

function checkVersion() {
	uni.showToast({ title: '已是最新版本', icon: 'success' })
}

onLoad(() => {
	if (!auth.ensureLoggedIn()) return
	refresh()
})
</script>

<style scoped lang="scss">
.settings { 
	padding: 0 0; 
}
 .user-card {
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	padding: 60rpx 40rpx;
	display: flex;
	border-radius: 0 0 40rpx 40rpx;

	.head {
		display: flex;
		align-items: center;
		gap: 30rpx;
		color: #fff;
	}

	.avatar {
		width: 140rpx;
		height: 140rpx;
		border-radius: 70rpx;
		border: 6rpx solid rgba(255, 255, 255, 0.4);
		box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.15);
	}

	.info-text {
		flex: 1;
	}

	.username {
		font-size: 40rpx;
		font-weight: 600;
		color: #fff;
		margin-bottom: 12rpx;
	}

	.role-tag {
		display: inline-block;
		background: rgba(255, 255, 255, 0.25);
		color: #fff;
		font-size: 22rpx;
		padding: 6rpx 20rpx;
		border-radius: 30rpx;
	}
}

.refresh-btn {
	background: rgba(255, 255, 255, 0.2) !important;
	border-color: rgba(255, 255, 255, 0.4) !important;
	color: #fff !important;
}

.section {
	padding: 30rpx 30rpx 0;
}

.section-title {
	font-size: 28rpx;
	color: #999;
	margin-bottom: 16rpx;
	padding-left: 8rpx;
}

.info-card {
	background: #fff;
	border-radius: 20rpx;
	overflow: hidden;
	box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.info-item {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 30rpx 28rpx;
	border-bottom: 1rpx solid #f0f0f0;
	&.no-border {
		border-bottom: none;
	}
	&:active {
		background: #f8f8f8;
	}
}

.info-left {
	display: flex;
	align-items: center;
	gap: 20rpx;
}

.info-icon {
	font-size: 32rpx;
}

.info-label {
	font-size: 28rpx;
	color: #333;
}

.info-right {
	display: flex;
	align-items: center;
	gap: 12rpx;
}

.info-value {
	font-size: 26rpx;
	color: #999;
}

.arrow {
	font-size: 32rpx;
	color: #ccc;
	font-weight: 300;
}

.logout-section {
	padding: 60rpx 30rpx 30rpx;
}
</style>
