<template>
	<view class="page">
		<!-- 收藏列表 -->
		<view class="favorite-list">
			<view v-if="loading" class="loading">
				<u-loading-icon></u-loading-icon>
			</view>
			<view v-else-if="favorites.length === 0" class="empty">
				<u-empty text="暂无收藏" icon="heart" iconSize="120"></u-empty>
				<u-button type="primary" size="small" plain @click="goShopping" style="margin-top: 40rpx;">
					去逛逛
				</u-button>
			</view>
			<view v-else>
				<view v-for="item in favorites" :key="item.id" class="favorite-card">
					<view class="card-content" @click="goProduct(item.productId)">
						<image class="product-image" :src="getImageUrl(item.productThumbnail) || '/static/logo.png'" mode="aspectFill"></image>
						<view class="product-info">
							<text class="product-name">{{ item.productName }}</text>
							<text class="product-price">¥{{ item.price }}</text>
							<text class="favorite-time">收藏于 {{ formatTime(item.createdAt) }}</text>
						</view>
					</view>
					<view class="card-actions">
						<u-button size="small" type="primary" @click="addToCart(item)">加入购物车</u-button>
						<u-button size="small" type="error" plain @click="removeFavorite(item)">取消收藏</u-button>
					</view>
				</view>
			</view>
		</view>
		
		<!-- 分页 -->
		<view v-if="favorites.length > 0" class="pagination">
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
import { api } from '@/utils/api'
import { getImageUrl } from '@/utils/function.js'

interface FavoriteItem {
	id: number
	productId: number
	productName: string
	productThumbnail: string
	price: number
	createdAt: string
}

const favorites = ref<FavoriteItem[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const page = ref(0)
const pageSize = ref(10)
const hasMore = ref(true)

onMounted(() => {
	loadFavorites()
})

onPullDownRefresh(() => {
	page.value = 0
	hasMore.value = true
	loadFavorites().finally(() => uni.stopPullDownRefresh())
})

async function loadFavorites() {
	loading.value = true
	try {
		const res = await api.getFavorites({ page: page.value + 1, page_size: pageSize.value })
		const list = res.content || []
		if (page.value === 0) {
			favorites.value = list
		} else {
			favorites.value = [...favorites.value, ...list]
		}
		hasMore.value = !res.last
	} catch (e: any) {
		console.error('加载收藏失败', e)
		if (e?.message?.includes('未登录') || e?.status === 401) {
			uni.showToast({ title: '请先登录', icon: 'none' })
			setTimeout(() => uni.navigateTo({ url: '/pages/login/login' }), 1500)
		}
	} finally {
		loading.value = false
	}
}

function loadMore() {
	if (loadingMore.value || !hasMore.value) return
	loadingMore.value = true
	page.value++
	loadFavorites().finally(() => {
		loadingMore.value = false
	})
}

function formatTime(time: string): string {
	if (!time) return ''
	const date = new Date(time)
	const month = (date.getMonth() + 1).toString().padStart(2, '0')
	const day = date.getDate().toString().padStart(2, '0')
	return `${month}-${day}`
}

function goProduct(productId: number) {
	uni.navigateTo({ url: `/pages/product/detail?id=${productId}` })
}

async function addToCart(item: FavoriteItem) {
	try {
		await api.addToCart({ productId: item.productId, quantity: 1 })
		uni.showToast({ title: '已加入购物车', icon: 'success' })
	} catch (e) {
		uni.showToast({ title: '加入失败', icon: 'error' })
	}
}

function removeFavorite(item: FavoriteItem) {
	uni.showModal({
		title: '提示',
		content: '确定取消收藏该商品吗？',
		success: async (res) => {
			if (res.confirm) {
				try {
					await api.removeFavorite(item.productId)
					uni.showToast({ title: '已取消收藏', icon: 'success' })
					// 从列表中移除
					const index = favorites.value.findIndex(f => f.id === item.id)
					if (index > -1) {
						favorites.value.splice(index, 1)
					}
				} catch (e) {
					uni.showToast({ title: '取消失败', icon: 'error' })
				}
			}
		}
	})
}

function goShopping() {
	uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style scoped lang="scss">
.page {
	min-height: 100vh;
	background: #f5f5f5;
	padding-bottom: 40rpx;
}

.favorite-list {
	padding: 24rpx 30rpx;
}

.loading, .empty {
	padding: 100rpx 0;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.favorite-card {
	background: #fff;
	border-radius: 16rpx;
	margin-bottom: 24rpx;
	overflow: hidden;
	box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.card-content {
	display: flex;
	gap: 24rpx;
	padding: 24rpx;
}

.product-image {
	width: 200rpx;
	height: 200rpx;
	border-radius: 12rpx;
	background: #f5f5f5;
	flex-shrink: 0;
}

.product-info {
	flex: 1;
	display: flex;
	flex-direction: column;
	justify-content: space-between;
	min-height: 200rpx;
}

.product-name {
	font-size: 28rpx;
	color: #333;
	line-height: 1.4;
	overflow: hidden;
	text-overflow: ellipsis;
	display: -webkit-box;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
}

.product-price {
	font-size: 32rpx;
	color: #ee0a24;
	font-weight: 600;
	margin: 12rpx 0;
}

.favorite-time {
	font-size: 22rpx;
	color: #999;
}

.card-actions {
	display: flex;
	justify-content: flex-end;
	gap: 16rpx;
	padding: 20rpx 24rpx;
	border-top: 1rpx solid #f0f0f0;
	background: #fafafa;
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
