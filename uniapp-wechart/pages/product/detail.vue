<template>
	<view class="product-detail">
		<!-- 产品图片轮播 -->
		<swiper class="product-swiper" indicator-dots autoplay circular v-if="productImages.length > 0">
			<swiper-item v-for="(img, idx) in productImages" :key="idx">
				<image class="product-image" :src="img" mode="aspectFill" @click="previewImage(idx)"></image>
			</swiper-item>
		</swiper>
		<image v-else class="product-image-placeholder" src="/static/image/no_cuoti.png" mode="aspectFill"></image>

		<!-- 产品信息 -->
		<view class="product-info">
			<view class="price-row">
				<text class="current-price">¥{{ product.actualPrice || product.officialPrice }}</text>
				<text class="original-price" v-if="product.officialPrice && product.actualPrice < product.officialPrice">
					¥{{ product.officialPrice }}
				</text>
			</view>
			<view class="product-name">{{ product.name }}</view>
			<view class="product-category" v-if="product.category">
				分类：{{ product.category.name }}
			</view>
		</view>

		<!-- 产品详情图 -->
		<view class="detail-section" v-if="product.poster">
			<view class="section-title">商品详情</view>
			<image class="detail-image" :src="product.poster" mode="widthFix" @click="previewDetailImage"></image>
		</view>

		<!-- 产品描述 -->
		<view class="detail-section" v-if="product.description">
			<view class="section-title">产品描述</view>
			<view class="product-desc">{{ product.description }}</view>
		</view>

		<!-- 底部操作栏 -->
		<view class="bottom-bar">
			<view class="action-icons">
				<view class="action-item" @click="goCart">
					<u-icon name="shopping-cart" size="44rpx"></u-icon>
					<text class="action-text">购物车</text>
					<view class="cart-badge" v-if="cartCount > 0">{{ cartCount > 99 ? '99+' : cartCount }}</view>
				</view>
				<view class="action-item" @click="toggleFavorite">
					<u-icon :name="isFavorited ? 'heart-fill' : 'heart'" size="44rpx" :color="isFavorited ? '#ff4d4f' : '#666'"></u-icon>
					<text class="action-text">{{ isFavorited ? '已收藏' : '收藏' }}</text>
				</view>
				<view class="action-item" @click="contactService">
					<u-icon name="kefu-ermai" size="44rpx"></u-icon>
					<text class="action-text">客服</text>
				</view>
			</view>
			<view class="action-buttons">
				<view class="btn-add-cart" @click="addToCart">加入购物车</view>
				<view class="btn-buy" @click="goPay">立即购买</view>
			</view>
		</view>
	</view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store/auth'
import { api } from '@/utils/api.js'
import { getImageUrl } from '@/utils/function.js'

type ProductDetail = {
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

const auth = useAuthStore()

const productId = ref<number>(0)
const product = ref<ProductDetail>({} as ProductDetail)
const isFavorited = ref(false)
const cartCount = ref(0)

const productImages = computed(() => {
	const images: string[] = []
	if (product.value.thumbnail) {
		images.push(getImageUrl(product.value.thumbnail))
	}
	if (product.value.poster && product.value.poster !== product.value.thumbnail) {
		images.push(getImageUrl(product.value.poster))
	}
	return images
})

onLoad((options: any) => {
	if (options?.id) {
		productId.value = parseInt(options.id)
		loadProductDetail()
		checkFavoriteStatus()
	}
	loadCartCount()
})

async function loadProductDetail() {
	try {
		const data = await api.getProduct(productId.value)
		product.value = data as ProductDetail
	} catch (e) {
		console.error('加载产品详情失败', e)
		uni.showToast({ title: '加载失败', icon: 'none' })
	}
}

async function checkFavoriteStatus() {
	try {
		const data = await api.isFavorited(productId.value)
		isFavorited.value = data === true || data === 'true'
	} catch (e) {
		isFavorited.value = false
	}
}

async function loadCartCount() {
	try {
		const data = await api.getCart()
		const cart = (data || {}) as any
		const items = cart.items || []
		cartCount.value = items.reduce((sum: number, item: any) => sum + (item.quantity || 0), 0)
	} catch (e) {
		cartCount.value = 0
	}
}

async function toggleFavorite() {
	if (!auth.ensureLoggedIn()) return
	try {
		if (isFavorited.value) {
			await api.removeFavorite(productId.value)
			isFavorited.value = false
			uni.showToast({ title: '已取消收藏', icon: 'success' })
		} else {
			await api.addFavorite({ productId: productId.value })
			isFavorited.value = true
			uni.showToast({ title: '收藏成功', icon: 'success' })
		}
	} catch (e) {
		uni.showToast({ title: '操作失败', icon: 'none' })
	}
}

async function addToCart() {
	if (!auth.ensureLoggedIn()) return
	try {
		await api.addToCart({
			productId: productId.value,
			quantity: 1
		})
		uni.showToast({ title: '已加入购物车', icon: 'success' })
		loadCartCount()
	} catch (e) {
		uni.showToast({ title: '加入购物车失败', icon: 'none' })
	}
}

function goCart() {
	uni.navigateTo({ url: '/pages/cart/index' })
}

function goPay() {
	if (!auth.ensureLoggedIn()) return
	const item = {
		productId: productId.value,
		name: product.value.name,
		thumbnail: getImageUrl(product.value.thumbnail),
		price: product.value.actualPrice || product.value.officialPrice,
		quantity: 1
	}
	uni.navigateTo({
		url: '/pages/pay/index?items=' + encodeURIComponent(JSON.stringify([item]))
	})
}

function contactService() {
	uni.showToast({ title: '客服功能开发中', icon: 'none' })
}

function previewImage(index: number) {
	uni.previewImage({
		urls: productImages.value,
		current: index
	})
}

function previewDetailImage() {
	if (product.value.poster) {
		uni.previewImage({
			urls: [product.value.poster]
		})
	}
}
</script>

<style lang="scss" scoped>
.product-detail {
	padding-bottom: 120rpx;
}

.product-swiper {
	width: 100%;
	height: 600rpx;
	
	.product-image {
		width: 100%;
		height: 100%;
	}
}

.product-image-placeholder {
	width: 100%;
	height: 400rpx;
	background: #f5f5f5;
}

.product-info {
	padding: 30rpx;
	background: #fff;
	
	.price-row {
		display: flex;
		align-items: baseline;
		gap: 16rpx;
		
		.current-price {
			font-size: 48rpx;
			font-weight: 600;
			color: #ff4d4f;
		}
		
		.original-price {
			font-size: 28rpx;
			color: #999;
			text-decoration: line-through;
		}
	}
	
	.product-name {
		margin-top: 20rpx;
		font-size: 32rpx;
		font-weight: 600;
		color: #1F2328;
		line-height: 1.5;
	}
	
	.product-category {
		margin-top: 12rpx;
		font-size: 24rpx;
		color: #999;
	}
}

.detail-section {
	margin-top: 20rpx;
	padding: 30rpx;
	background: #fff;
	
	.section-title {
		font-size: 30rpx;
		font-weight: 600;
		color: #1F2328;
		margin-bottom: 20rpx;
	}
	
	.detail-image {
		width: 100%;
		display: block;
	}
	
	.product-desc {
		font-size: 28rpx;
		color: #666;
		line-height: 1.8;
		white-space: pre-wrap;
	}
}

.bottom-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	display: flex;
	align-items: center;
	padding: 16rpx 24rpx;
	padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
	background: #fff;
	box-shadow: 0 -2rpx 20rpx rgba(0, 0, 0, 0.06);
	z-index: 100;
	
	.action-icons {
		display: flex;
		gap: 24rpx;
		
		.action-item {
			position: relative;
			display: flex;
			flex-direction: column;
			align-items: center;
			gap: 6rpx;
			
			.action-text {
				font-size: 20rpx;
				color: #666;
			}
			
			.cart-badge {
				position: absolute;
				top: -10rpx;
				right: -16rpx;
				min-width: 32rpx;
				height: 32rpx;
				line-height: 32rpx;
				text-align: center;
				background: #ff4d4f;
				color: #fff;
				font-size: 20rpx;
				border-radius: 16rpx;
				padding: 0 8rpx;
			}
		}
	}
	
	.action-buttons {
		flex: 1;
		display: flex;
		margin-left: 30rpx;
		gap: 16rpx;
		
		.btn-add-cart {
			flex: 1;
			height: 72rpx;
			line-height: 72rpx;
			text-align: center;
			background: linear-gradient(135deg, #FF9A43 0%, #FF6B26 100%);
			color: #fff;
			font-size: 28rpx;
			border-radius: 36rpx;
		}
		
		.btn-buy {
			flex: 1;
			height: 72rpx;
			line-height: 72rpx;
			text-align: center;
			background: linear-gradient(135deg, #2388FD 0%, #3B82F6 100%);
			color: #fff;
			font-size: 28rpx;
			border-radius: 36rpx;
		}
	}
}
</style>
