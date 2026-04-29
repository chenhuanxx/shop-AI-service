<template>
	<view class="pay-page">
		<!-- 商品列表 -->
		<view class="goods-section">
			<view class="section-title">商品信息</view>
			<view class="goods-list">
				<view class="goods-item" v-for="item in orderItems" :key="item.productId">
					<image class="goods-image" :src="getImageUrl(item.thumbnail) || '/static/image/no_cuoti.png'" mode="aspectFill"></image>
					<view class="goods-info">
						<view class="goods-name">{{ item.name }}</view>
						<view class="goods-meta">
							<text class="goods-price">¥{{ item.price }}</text>
							<text class="goods-qty">x{{ item.quantity }}</text>
						</view>
					</view>
				</view>
			</view>
		</view>

		<!-- 收货信息 -->
		<view class="address-section">
			<view class="section-title">收货信息</view>
			<view class="address-card">
				<view class="address-row">
					<text class="address-label">收货人</text>
					<input class="address-input" v-model="address.name" placeholder="请输入收货人姓名" />
				</view>
				<view class="address-row">
					<text class="address-label">联系电话</text>
					<input class="address-input" v-model="address.phone" type="number" placeholder="请输入联系电话" />
				</view>
				<view class="address-row">
					<text class="address-label">收货地址</text>
					<input class="address-input" v-model="address.detail" placeholder="请输入详细地址" />
				</view>
			</view>
		</view>

		<!-- 订单备注 -->
		<view class="remark-section">
			<view class="section-title">订单备注</view>
			<textarea class="remark-input" v-model="remark" placeholder="选填，可备注特殊需求" />
		</view>

		<!-- 订单金额 -->
		<view class="amount-section">
			<view class="amount-row">
				<text class="amount-label">商品金额</text>
				<text class="amount-value">¥{{ goodsAmount }}</text>
			</view>
			<view class="amount-row">
				<text class="amount-label">运费</text>
				<text class="amount-value">¥{{ freight }}</text>
			</view>
			<view class="amount-row total">
				<text class="amount-label">合计</text>
				<text class="amount-value total-price">¥{{ totalAmount }}</text>
			</view>
		</view>

		<!-- 底部支付栏 -->
		<view class="bottom-bar">
			<view class="total-info">
				<text class="total-label">实付款</text>
				<text class="total-price">¥{{ totalAmount }}</text>
			</view>
			<view class="btn-pay" @click="submitOrder">提交订单</view>
		</view>

		<!-- 支付成功弹窗 -->
		<u-popup :show="showPaySuccess" mode="center" :round="16" @close="closePaySuccess">
			<view class="pay-success-popup">
				<u-icon name="checkmark-circle" size="120rpx" color="#52c41a"></u-icon>
				<view class="success-text">支付成功</view>
				<view class="success-desc">您的订单已提交</view>
				<view class="success-btns">
					<view class="btn-view-order" @click="viewOrder">查看订单</view>
					<view class="btn-continue" @click="continueShopping">继续购物</view>
				</view>
			</view>
		</u-popup>
	</view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useAuthStore } from '@/store/auth'
import { api } from '@/utils/api.js'
import { getImageUrl } from '@/utils/function.js'

type OrderItem = {
	productId: number
	name: string
	thumbnail: string
	price: number
	quantity: number
	cartId?: number
}

const auth = useAuthStore()

const orderItems = ref<OrderItem[]>([])
const showPaySuccess = ref(false)
const currentOrderId = ref<number>(0)

const address = ref({
	name: '',
	phone: '',
	detail: ''
})

const remark = ref('')
const freight = ref(0)

const goodsAmount = computed(() => {
	return orderItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2)
})

const totalAmount = computed(() => {
	return (parseFloat(goodsAmount.value) + freight.value).toFixed(2)
})

onMounted(() => {
	if (auth.user) {
		address.value.name = auth.user.username || ''
		address.value.phone = auth.user.phone || ''
	}
})

onLoad((options: any) => {
	if (options?.items) {
		try {
			const items = JSON.parse(decodeURIComponent(options.items))
			orderItems.value = items
		} catch (e) {
			console.error('解析订单商品失败', e)
		}
	}
})

async function submitOrder() {
	// 验证收货信息
	if (!address.value.name) {
		uni.showToast({ title: '请输入收货人姓名', icon: 'none' })
		return
	}
	if (!address.value.phone) {
		uni.showToast({ title: '请输入联系电话', icon: 'none' })
		return
	}
	if (!address.value.detail) {
		uni.showToast({ title: '请输入收货地址', icon: 'none' })
		return
	}

	uni.showLoading({ title: '提交中...' })
	try {
		const orderData = {
			items: orderItems.value.map(item => ({
				productId: item.productId,
				quantity: item.quantity,
				price: item.price
			})),
			receiverName: address.value.name,
			receiverPhone: address.value.phone,
			receiverAddress: address.value.detail,
			remark: remark.value
		}

		const data = await api.createOrder(orderData)
		const result = (data || {}) as any
		currentOrderId.value = result.id || 0

		// 如果是从购物车来的，清理已购买的购物车商品
		const cartIds = orderItems.value
			.filter(item => item.cartId)
			.map(item => item.cartId)
		if (cartIds.length > 0) {
			for (const cartId of cartIds) {
				try {
					await api.removeCartItem(cartId)
				} catch (e) {
					console.error('清理购物车失败', e)
				}
			}
		}

		uni.hideLoading()
		showPaySuccess.value = true
	} catch (e) {
		uni.hideLoading()
		uni.showToast({ title: '订单提交失败', icon: 'none' })
	}
}

function closePaySuccess() {
	showPaySuccess.value = false
}

function viewOrder() {
	showPaySuccess.value = false
	uni.navigateTo({
		url: `/pages/order/detail?id=${currentOrderId.value}`
	})
}

function continueShopping() {
	showPaySuccess.value = false
	uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style lang="scss" scoped>
.pay-page {
	min-height: 100vh;
	padding-bottom: 120rpx;
	background: #f5f5f5;
}

.section-title {
	font-size: 30rpx;
	font-weight: 600;
	color: #1F2328;
	padding: 24rpx;
}

.goods-section {
	background: #fff;
	margin-bottom: 20rpx;
	
	.goods-list {
		.goods-item {
			display: flex;
			padding: 24rpx;
			border-top: 1rpx solid #f0f0f0;
			
			.goods-image {
				width: 160rpx;
				height: 160rpx;
				border-radius: 12rpx;
				background: #f5f5f5;
				margin-right: 20rpx;
				flex-shrink: 0;
			}
			
			.goods-info {
				flex: 1;
				display: flex;
				flex-direction: column;
				justify-content: space-between;
				
				.goods-name {
					font-size: 28rpx;
					color: #1F2328;
					line-height: 1.4;
					overflow: hidden;
					text-overflow: ellipsis;
					display: -webkit-box;
					-webkit-line-clamp: 2;
					-webkit-box-orient: vertical;
				}
				
				.goods-meta {
					display: flex;
					justify-content: space-between;
					align-items: center;
					
					.goods-price {
						font-size: 30rpx;
						color: #ff4d4f;
						font-weight: 600;
					}
					
					.goods-qty {
						font-size: 26rpx;
						color: #999;
					}
				}
			}
		}
	}
}

.address-section {
	background: #fff;
	margin-bottom: 20rpx;
	
	.address-card {
		padding: 0 24rpx 24rpx;
		
		.address-row {
			display: flex;
			align-items: center;
			padding: 24rpx 0;
			border-bottom: 1rpx solid #f0f0f0;
			
			&:last-child {
				border-bottom: none;
			}
			
			.address-label {
				width: 140rpx;
				font-size: 28rpx;
				color: #666;
				flex-shrink: 0;
			}
			
			.address-input {
				flex: 1;
				font-size: 28rpx;
				color: #1F2328;
			}
		}
	}
}

.remark-section {
	background: #fff;
	margin-bottom: 20rpx;
	
	.remark-input {
		width: 100%;
		height: 160rpx;
		padding: 0 24rpx 24rpx;
		font-size: 28rpx;
		color: #1F2328;
		box-sizing: border-box;
	}
}

.amount-section {
	background: #fff;
	padding: 24rpx;
	margin-bottom: 20rpx;
	
	.amount-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 16rpx 0;
		
		.amount-label {
			font-size: 28rpx;
			color: #666;
		}
		
		.amount-value {
			font-size: 28rpx;
			color: #1F2328;
		}
		
		&.total {
			padding-top: 24rpx;
			border-top: 1rpx solid #f0f0f0;
			margin-top: 10rpx;
			
			.total-price {
				font-size: 36rpx;
				color: #ff4d4f;
				font-weight: 600;
			}
		}
	}
}

.bottom-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 20rpx 30rpx;
	padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
	background: #fff;
	box-shadow: 0 -2rpx 20rpx rgba(0, 0, 0, 0.06);
	z-index: 100;
	
	.total-info {
		display: flex;
		flex-direction: column;
		
		.total-label {
			font-size: 24rpx;
			color: #999;
		}
		
		.total-price {
			font-size: 36rpx;
			color: #ff4d4f;
			font-weight: 600;
		}
	}
	
	.btn-pay {
		padding: 0 60rpx;
		height: 72rpx;
		line-height: 72rpx;
		background: linear-gradient(135deg, #2388FD 0%, #3B82F6 100%);
		color: #fff;
		font-size: 28rpx;
		border-radius: 36rpx;
	}
}

.pay-success-popup {
	width: 560rpx;
	padding: 60rpx 40rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	
	.success-text {
		margin-top: 30rpx;
		font-size: 36rpx;
		font-weight: 600;
		color: #1F2328;
	}
	
	.success-desc {
		margin-top: 16rpx;
		font-size: 28rpx;
		color: #666;
	}
	
	.success-btns {
		display: flex;
		gap: 24rpx;
		margin-top: 40rpx;
		
		.btn-view-order {
			padding: 20rpx 40rpx;
			background: #f5f5f5;
			color: #1F2328;
			font-size: 28rpx;
			border-radius: 36rpx;
		}
		
		.btn-continue {
			padding: 20rpx 40rpx;
			background: linear-gradient(135deg, #2388FD 0%, #3B82F6 100%);
			color: #fff;
			font-size: 28rpx;
			border-radius: 36rpx;
		}
	}
}
</style>
