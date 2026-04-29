<template>
	<view class="ui-page home">
		<!-- 搜索栏 - 固定顶部 -->
		<view class="search-bar-fixed">
			<view class="search-bar">
				<input class="search-input" placeholder="搜索商品" v-model="searchKeyword" @confirm="handleSearch" />
				<u-icon name="search" size="36" @click="handleSearch"></u-icon>
			</view>
		</view>

		<!-- Banner 轮播 -->
		<view class="banner-section" v-if="banners.length > 0">
			<swiper class="banner-swiper" indicator-dots autoplay circular @click="onBannerClick">
				<swiper-item v-for="(banner, index) in banners" :key="index">
					<image class="banner-image" :src="getImageUrl(banner.imageUrl)" mode="aspectFill"></image>
				</swiper-item>
			</swiper>
		</view>

		<!-- 产品列表 -->
		<view class="product-section">
			<view class="section-title">热门产品</view>
			<view class="product-grid" v-if="productList.length > 0">
				<view class="product-card" v-for="item in productList" :key="item.id" @click="goProductDetail(item)">
					<image class="product-image" :src="getImageUrl(item.thumbnail) || '/static/image/no_cuoti.png'" mode="aspectFill"></image>
					<view class="product-info">
						<view class="product-name">{{ item.name }}</view>
						<view class="product-price">
							<text class="price-symbol">¥</text>
							<text class="price-value">{{ item.actualPrice || item.officialPrice }}</text>
						</view>
					</view>
				</view>
			</view>
			<view class="no-data" v-else>
				<image src="/static/image/no_cuoti.png" alt="暂无商品"></image>
				<view class="no-data-text">暂无商品</view>
			</view>
			<view v-if="productList.length > 0 && noMore" class="no-more">没有更多了</view>
		</view>

	</view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'  
import { useAuthStore } from '@/store/auth'
import { api } from '@/utils/api.js'
import { getImageUrl } from '@/utils/function.js'

type ProductItem = {
	id: number
	name: string
	thumbnail: string
	poster: string
	description: string
	officialPrice: number
	discount: number
	actualPrice: number
	category: { id: number; name: string } | null
	status: boolean
}

type BannerItem = {
	id: number
	title: string
	imageUrl: string
	linkUrl: string
	enabled: boolean
}

const auth = useAuthStore()

const searchKeyword = ref('')
const productList = ref<ProductItem[]>([])
const banners = ref<BannerItem[]>([])
const page = ref(1)
const pageSize = 10
const noMore = ref(false)
const loading = ref(false)

function handleSearch() {
	page.value = 1
	productList.value = []
	noMore.value = false
	loadProducts()
}

async function loadBanners() {
	try {
		const data = await api.getBanners()
		banners.value = ((data || {}).content || data || []) as BannerItem[]
	} catch (e) {
		console.error('加载Banner失败', e)
	}
}

function onBannerClick(e: any) {
	const index = e.detail.current
	const banner = banners.value[index]
	if (banner?.linkUrl) {
		// 如果有链接，可以跳转到对应页面
		if (banner.linkUrl.includes('/pages/product/detail?id=')) {
			const id = banner.linkUrl.split('id=')[1]
			uni.navigateTo({ url: `/pages/product/detail?id=${id}` })
		}
	}
}

async function loadProducts() {
	if (loading.value) return
	loading.value = true
	try {
		const params: any = {
			page: page.value,
			page_size: pageSize,
			status: true  // 只获取上架产品
		}
		if (searchKeyword.value) {
			params.keyword = searchKeyword.value
		}
		const data = await api.listProducts(params)
		const res = (data || {}) as any
		const list = (res.content || res.list || []) as ProductItem[]
		
		if (page.value === 1) {
			productList.value = list
		} else {
			productList.value = [...productList.value, ...list]
		}
		
		const total = res.totalElements || res.total || 0
		noMore.value = productList.value.length >= total || list.length < pageSize
	} catch (e) {
		console.error('加载产品失败', e)
	} finally {
		loading.value = false
		uni.stopPullDownRefresh()
	}
}

function goProductDetail(item: ProductItem) {
	uni.navigateTo({
		url: `/pages/product/detail?id=${item.id}`
	})
}

onMounted(() => {
	auth.ensureLoggedIn()
	loadBanners()
	loadProducts()
})

onShow(() => {
	if (productList.value.length === 0) {
		loadProducts()
	}
})

onPullDownRefresh(() => {
	page.value = 1
	noMore.value = false
	loadBanners()
	loadProducts()
})

onReachBottom(() => {
	if (noMore.value) return
	page.value += 1
	loadProducts()
})
</script>

<style lang="scss" scoped>
.home {
	padding: 0 0;
	padding-top: 54rpx; // 为固定搜索栏留出空间
}
<!-- #ifdef MP-WEIXIN -->
	.home { 
		padding-top: 108rpx; // 为固定搜索栏留出空间
	}
<!-- #endif -->

.search-bar-fixed {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 999;
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	padding: 16rpx 24rpx;
	padding-top: calc(16rpx + env(safe-area-inset-top));
	box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.1);
}

.search-bar {
	display: flex;
	align-items: center;
	background: #fff;
	padding: 16rpx 24rpx;
	gap: 16rpx;
	border-radius: 40rpx;
	box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.08);

	.search-input {
		flex: 1;
		height: 64rpx;
		background: #f8f8f8;
		border-radius: 32rpx;
		padding: 0 24rpx;
		font-size: 28rpx;
	}
}

.banner-section {
	width: 100%;
	margin-top: 0;
}

.banner-swiper {
	width: 100%;
	height: 320rpx;

	.banner-image {
		width: 100%;
		height: 100%;
	}
}

.product-section {
	padding: 24rpx 0 ;
	
	.section-title {
		font-size: 32rpx;
		font-weight: 600;
		color: #1F2328;
		margin-bottom: 24rpx;
		padding: 0 30rpx;
	}
}

.product-grid {
	padding: 0 24rpx;
	display: grid;
	grid-template-columns: 1fr 1fr;
	gap: 24rpx;
	
	.product-card {
		background: #fff;
		border-radius: 16rpx;
		overflow: hidden;
		box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
		
		.product-image {
			width: 100%;
			height: 320rpx;
			background: #f5f5f5;
		}
		
		.product-info {
			padding: 20rpx;
			
			.product-name {
				font-size: 28rpx;
				color: #1F2328;
				line-height: 1.4;
				overflow: hidden;
				text-overflow: ellipsis;
				display: -webkit-box;
				-webkit-line-clamp: 2;
				-webkit-box-orient: vertical;
			}
			
			.product-price {
				margin-top: 12rpx;
				color: #ff4d4f;
				
				.price-symbol {
					font-size: 24rpx;
				}
				
				.price-value {
					font-size: 32rpx;
					font-weight: 600;
				}
			}
		}
	}
}

.no-data {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 80rpx 0;
	
	.no-data-text {
		margin-top: 20rpx;
		color: #999;
		font-size: 28rpx;
	}
	
	image {
		width: 200rpx;
		height: 200rpx;
	}
}

.no-more {
	text-align: center;
	padding: 30rpx;
	color: #999;
	font-size: 24rpx;
}
</style>
