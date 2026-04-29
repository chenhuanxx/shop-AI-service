<template>
	<view class="cart-page">
		<!-- 购物车列表 -->
		<view class="cart-list" v-if="cartItems.length > 0">
			<view class="cart-item" v-for="item in cartItems" :key="item.id">
				<view class="item-checkbox" @click="toggleSelect(item)">
					<u-icon :name="item.selected ? 'checkmark-circle-fill' : 'circle'" :color="item.selected ? '#2388FD' : '#999'" size="44rpx"></u-icon>
				</view>
				<image class="item-image" :src="getImageUrl(item.thumbnail) || '/static/image/no_cuoti.png'" mode="aspectFill" @click="goProduct(item)"></image>
				<view class="item-info">
					<view class="item-name" @click="goProduct(item)">{{ item.name }}</view>
					<view class="item-price">¥{{ item.price }}</view>
					<view class="item-controls">
						<view class="qty-btn" @click="decreaseQty(item)">-</view>
						<input class="qty-input" type="number" :value="item.quantity" @change="updateQty(item, $event)" />
						<view class="qty-btn" @click="increaseQty(item)">+</view>
					</view>
				</view>
				<view class="item-delete" @click="deleteItem(item)">
					<u-icon name="trash" size="36rpx" color="#999"></u-icon>
				</view>
			</view>
		</view>

		<!-- 空购物车 -->
		<view class="empty-cart" v-else>
			<image src="/static/image/no_cuoti.png" mode="aspectFit"></image>
			<view class="empty-text">购物车是空的</view>
			<view class="btn-shopping" @click="goShopping">去购物</view>
		</view>

		<!-- 底部结算栏 -->
		<view class="bottom-bar" v-if="cartItems.length > 0">
			<view class="select-all" @click="toggleSelectAll">
				<u-icon :name="isAllSelected ? 'checkmark-circle-fill' : 'circle'" :color="isAllSelected ? '#2388FD' : '#999'" size="44rpx"></u-icon>
				<text class="select-text">全选</text>
			</view>
			<view class="total-info">
				<text class="total-label">合计：</text>
				<text class="total-price">¥{{ totalPrice }}</text>
			</view>
			<view class="btn-settle" @click="goSettle">结算{{ selectedCount > 0 ? `(${selectedCount})` : '' }}</view>
		</view>
	</view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store/auth'
import { api } from '@/utils/api.js'
import { getImageUrl } from '@/utils/function.js'

type CartItem = {
	id: number
	productId: number
	name: string
	thumbnail: string
	price: number
	quantity: number
	selected: boolean
}

const auth = useAuthStore()

const cartItems = ref<CartItem[]>([])

const isAllSelected = computed(() => {
	return cartItems.value.length > 0 && cartItems.value.every(item => item.selected)
})

const selectedCount = computed(() => {
	return cartItems.value.filter(item => item.selected).length
})

const totalPrice = computed(() => {
	return cartItems.value
		.filter(item => item.selected)
		.reduce((sum, item) => sum + item.price * item.quantity, 0)
		.toFixed(2)
})

onMounted(() => {
	auth.ensureLoggedIn()
	loadCart()
})

onShow(() => {
	loadCart()
})

async function loadCart() {
	try {
		const data = await api.getCart()
		const cart = (data || {}) as any
		cartItems.value = (cart.items || []).map((item: any) => ({
			...item,
			selected: true
		}))
	} catch (e) {
		console.error('加载购物车失败', e)
		cartItems.value = []
	}
}

function toggleSelect(item: CartItem) {
	item.selected = !item.selected
}

function toggleSelectAll() {
	const newVal = !isAllSelected.value
	cartItems.value.forEach(item => {
		item.selected = newVal
	})
}

async function updateQty(item: CartItem, event: any) {
	const qty = parseInt(event.detail.value) || 1
	if (qty < 1) return
	await updateCartItem(item, qty)
}

async function increaseQty(item: CartItem) {
	await updateCartItem(item, item.quantity + 1)
}

async function decreaseQty(item: CartItem) {
	if (item.quantity <= 1) return
	await updateCartItem(item, item.quantity - 1)
}

async function updateCartItem(item: CartItem, quantity: number) {
	try {
		await api.updateCartItem({
			id: item.id,
			quantity: quantity
		})
		item.quantity = quantity
	} catch (e) {
		uni.showToast({ title: '更新失败', icon: 'none' })
	}
}

async function deleteItem(item: CartItem) {
	uni.showModal({
		title: '提示',
		content: '确定要删除该商品吗？',
		success: async (res) => {
			if (res.confirm) {
				try {
					await api.removeCartItem(item.id)
					const index = cartItems.value.findIndex(i => i.id === item.id)
					if (index > -1) {
						cartItems.value.splice(index, 1)
					}
					uni.showToast({ title: '已删除', icon: 'success' })
				} catch (e) {
					uni.showToast({ title: '删除失败', icon: 'none' })
				}
			}
		}
	})
}

function goProduct(item: CartItem) {
	uni.navigateTo({
		url: `/pages/product/detail?id=${item.productId}`
	})
}

function goShopping() {
	uni.switchTab({ url: '/pages/index/index' })
}

function goSettle() {
	const selectedItems = cartItems.value.filter(item => item.selected)
	if (selectedItems.length === 0) {
		uni.showToast({ title: '请选择商品', icon: 'none' })
		return
	}
	const settleItems = selectedItems.map(item => ({
		productId: item.productId,
		name: item.name,
		thumbnail: item.thumbnail,
		price: item.price,
		quantity: item.quantity,
		cartId: item.id
	}))
	uni.navigateTo({
		url: '/pages/pay/index?items=' + encodeURIComponent(JSON.stringify(settleItems))
	})
}
</script>

<style lang="scss" scoped>
.cart-page {
	min-height: 100vh;
	padding-bottom: 140rpx;
	background: #f5f5f5;
}

.cart-list {
	padding: 20rpx;
	
	.cart-item {
		display: flex;
		align-items: center;
		padding: 24rpx;
		margin-bottom: 20rpx;
		background: #fff;
		border-radius: 16rpx;
		
		.item-checkbox {
			margin-right: 20rpx;
		}
		
		.item-image {
			width: 160rpx;
			height: 160rpx;
			border-radius: 12rpx;
			background: #f5f5f5;
			margin-right: 20rpx;
			flex-shrink: 0;
		}
		
		.item-info {
			flex: 1;
			min-width: 0;
			
			.item-name {
				font-size: 28rpx;
				color: #1F2328;
				line-height: 1.4;
				overflow: hidden;
				text-overflow: ellipsis;
				display: -webkit-box;
				-webkit-line-clamp: 2;
				-webkit-box-orient: vertical;
			}
			
			.item-price {
				margin-top: 10rpx;
				font-size: 30rpx;
				color: #ff4d4f;
				font-weight: 600;
			}
			
			.item-controls {
				display: flex;
				align-items: center;
				margin-top: 12rpx;
				
				.qty-btn {
					width: 48rpx;
					height: 48rpx;
					line-height: 48rpx;
					text-align: center;
					background: #f5f5f5;
					border-radius: 8rpx;
					font-size: 28rpx;
					color: #666;
				}
				
				.qty-input {
					width: 60rpx;
					height: 48rpx;
					margin: 0 12rpx;
					text-align: center;
					font-size: 28rpx;
					background: #f5f5f5;
					border-radius: 8rpx;
				}
			}
		}
		
		.item-delete {
			margin-left: 20rpx;
			padding: 10rpx;
		}
	}
}

.empty-cart {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding-top: 200rpx;
	
	image {
		width: 240rpx;
		height: 240rpx;
	}
	
	.empty-text {
		margin-top: 30rpx;
		font-size: 28rpx;
		color: #999;
	}
	
	.btn-shopping {
		margin-top: 40rpx;
		padding: 20rpx 60rpx;
		background: linear-gradient(135deg, #2388FD 0%, #3B82F6 100%);
		color: #fff;
		font-size: 28rpx;
		border-radius: 36rpx;
	}
}

.bottom-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	display: flex;
	align-items: center;
	padding: 20rpx 30rpx;
	padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
	background: #fff;
	box-shadow: 0 -2rpx 20rpx rgba(0, 0, 0, 0.06);
	z-index: 100;
	
	.select-all {
		display: flex;
		align-items: center;
		gap: 10rpx;
		
		.select-text {
			font-size: 28rpx;
			color: #666;
		}
	}
	
	.total-info {
		flex: 1;
		text-align: right;
		margin-right: 20rpx;
		
		.total-label {
			font-size: 26rpx;
			color: #666;
		}
		
		.total-price {
			font-size: 36rpx;
			color: #ff4d4f;
			font-weight: 600;
		}
	}
	
	.btn-settle {
		padding: 0 50rpx;
		height: 72rpx;
		line-height: 72rpx;
		background: linear-gradient(135deg, #2388FD 0%, #3B82F6 100%);
		color: #fff;
		font-size: 28rpx;
		border-radius: 36rpx;
	}
}
</style>
