<template>
	<view class="ui-page mine">
		<view class="ui-card user-card">
			<view class="row head">
				<image class="avatar" :src="userInfo.avatarUrl || '/static/logo.png'"></image>
				<view class="info">
					<view class="username">{{ userInfo.nickname || userInfo.username || '未登录' }}</view>
					<view class="role-tag">{{ userInfo.role || '' }}</view>
				</view>
			</view>
		</view>

		<!-- 功能菜单 -->
		<view class="menu-section">
			<view class="menu-card">
				<view class="menu-item" @click="goOrders">
					<view class="menu-left">
						<text class="iconfont icon-order">📦</text>
						<text class="menu-text">我的订单</text>
					</view>
					<text class="arrow">›</text>
				</view>
				<view class="menu-item" @click="goFavorites">
					<view class="menu-left">
						<text class="iconfont icon-fav">❤️</text>
						<text class="menu-text">我的收藏</text>
					</view>
					<text class="arrow">›</text>
				</view>
				<view class="menu-item" @click="goListen">
					<view class="menu-left">
						<text class="iconfont icon-listen">🎧</text>
						<text class="menu-text">我的听课</text>
					</view>
					<text class="arrow">›</text>
				</view>
				<view class="menu-item" v-if="isAdmin" @click="goAudit">
					<view class="menu-left">
						<text class="iconfont icon-audit">📋</text>
						<text class="menu-text">待审批</text>
					</view>
					<text class="arrow">›</text>
				</view>
			</view>
		</view>

		<!-- 设置入口 -->
		<view class="menu-section">
			<view class="menu-card">
				<view class="menu-item" @click="goSettings">
					<view class="menu-left">
						<text class="iconfont icon-settings">⚙️</text>
						<text class="menu-text">设置</text>
					</view>
					<text class="arrow">›</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
const userInfo = computed(() => auth.user || ({} as any))
const isAdmin = computed(() => (auth.user?.role || auth.user?.username || '').toLowerCase() === 'admin')

async function refreshMe() {
	if (!auth.token) {
		uni.stopPullDownRefresh()
		return
	}
	try {
		await auth.fetchMe()
	} finally {
		uni.stopPullDownRefresh()
	}
}

onLoad(() => {
	refreshMe()
})

onShow(() => {
	auth.ensureLoggedIn()
	refreshMe()
})

onPullDownRefresh(() => {
	refreshMe()
})

function goSettings() {
	uni.navigateTo({ url: '/pages/my/settings' })
}

function goAudit() {
	uni.navigateTo({ url: '/pages/my/audit-list' })
}

function goOrders() {
	uni.navigateTo({ url: '/pages/order/index' })
}

function goFavorites() {
	uni.navigateTo({ url: '/pages/favorite/index' })
}

function goListen() {
	uni.navigateTo({ url: '/pages/mylisten/index' })
}
</script>

<style scoped lang="scss">
.mine {
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

 
.menu-section {
	padding: 30rpx 30rpx 0;
}

.menu-card {
	background: #fff;
	border-radius: 20rpx;
	overflow: hidden;
	box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.menu-item {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 36rpx 32rpx;
	border-bottom: 1rpx solid #f0f0f0;

	&:last-child {
		border-bottom: none;
	}

	&:active {
		background: #f8f8f8;
	}
}

.menu-left {
	display: flex;
	align-items: center;
	gap: 24rpx;
}

.iconfont {
	font-size: 40rpx;
}

.menu-text {
	font-size: 30rpx;
	color: #333;
}

.arrow {
	font-size: 36rpx;
	color: #ccc;
	font-weight: 300;
}
</style>
