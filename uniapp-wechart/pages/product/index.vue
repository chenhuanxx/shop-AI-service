<template>
	<view class="page">
		<!-- 分类侧边栏 -->
		<view class="category-layout">
			<scroll-view class="category-left" scroll-y>
				<view 
					class="category-item" 
					:class="{ active: selectedCategoryId === null }"
					@click="selectCategory(null)"
				>
					全部商品
				</view>
				<view 
					v-for="cat in categories" 
					:key="cat.id" 
					class="category-item"
					:class="{ active: selectedCategoryId === cat.id }"
					@click="selectCategory(cat.id)"
				>
					{{ cat.name }}
				</view>
			</scroll-view>
			
			<!-- 商品列表 -->
			<scroll-view class="product-right" scroll-y @scrolltolower="loadMore">
				<!-- 搜索栏 -->
				<view class="search-bar">
					<u-search
						v-model="searchKey"
						placeholder="搜索商品"
						:show-action="false"
						@change="onSearch"
					></u-search>
				</view>
				
				<!-- 商品列表 -->
				<view v-if="loading && products.length === 0" class="loading">
					<u-loading-icon></u-loading-icon>
				</view>
				<view v-else-if="products.length === 0" class="empty">
					<u-empty text="暂无商品" icon="shop"></u-empty>
				</view>
				<view v-else class="product-grid">
					<view class="product-card" v-for="item in products" :key="item.id" @click="goProduct(item)">
						<image class="product-image" :src="getImageUrl(item.thumbnail) || '/static/image/no_cuoti.png'" mode="aspectFill"></image>
						<view class="product-info">
							<text class="product-name">{{ item.name }}</text>
							<text class="product-price">¥{{ item.actualPrice || item.officialPrice }}</text>
						</view>
					</view>
				</view>
				
				<!-- 加载更多 -->
				<view v-if="products.length > 0" class="load-more">
					<u-button v-if="hasMore" type="primary" plain size="small" @click="loadMore" :loading="loadingMore">
						加载更多
					</u-button>
					<text v-else class="no-more">— 没有更多了 —</text>
				</view>
			</scroll-view>
		</view>
	</view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { api } from '@/utils/api.js'
import { getImageUrl } from '@/utils/function.js'

type CategoryItem = {
	id: number
	name: string
	code: string
}

type ProductItem = {
	id: number
	name: string
	thumbnail: string
	officialPrice: number
	actualPrice: number
}

const categories = ref<CategoryItem[]>([])
const selectedCategoryId = ref<number | null>(null)
const searchKey = ref('')
const products = ref<ProductItem[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const page = ref(0)
const pageSize = ref(20)
const hasMore = ref(true)

onMounted(() => {
	loadCategories()
	loadProducts()
})

onPullDownRefresh(() => {
	page.value = 0
	hasMore.value = true
	loadProducts().finally(() => uni.stopPullDownRefresh())
})

async function loadCategories() {
	try {
		const data = await api.listCategories()
		categories.value = (data || []) as CategoryItem[]
	} catch (e) {
		console.error('加载分类失败', e)
	}
}

async function loadProducts() {
	loading.value = true
	try {
		const params: any = {
			page: page.value + 1,
			page_size: pageSize.value,
			status: true
		}
		if (searchKey.value) {
			params.keyword = searchKey.value
		}
		if (selectedCategoryId.value) {
			params.category_id = selectedCategoryId.value
		}
		
		const data = await api.listProducts(params)
		const res = (data || {}) as any
		const list = (res.content || res.list || []) as ProductItem[]
		
		if (page.value === 0) {
			products.value = list
		} else {
			products.value = [...products.value, ...list]
		}
		
		const total = res.totalElements || res.total || 0
		hasMore.value = products.value.length < total && list.length >= pageSize
	} catch (e) {
		console.error('加载商品失败', e)
	} finally {
		loading.value = false
	}
}

function selectCategory(id: number | null) {
	selectedCategoryId.value = id
	page.value = 0
	hasMore.value = true
	loadProducts()
}

function onSearch() {
	page.value = 0
	hasMore.value = true
	loadProducts()
}

function loadMore() {
	if (loadingMore.value || !hasMore.value) return
	loadingMore.value = true
	page.value++
	loadProducts().finally(() => {
		loadingMore.value = false
	})
}

function goProduct(item: ProductItem) {
	uni.navigateTo({ url: `/pages/product/detail?id=${item.id}` })
}
</script>

<style scoped lang="scss">
.page { 
	background: #e6e6e6;
}

.category-layout {
	display: flex;
	height: 100%;

}

.category-left {
	width: 180rpx;
	background: #fff;
	flex-shrink: 0;
	
	.category-item {
		padding: 32rpx 20rpx;
		font-size: 26rpx;
		color: #666;
		text-align: center;
		border-left: 6rpx solid transparent;
		transition: all 0.2s;
		
		&.active {
			color: #2388FD;
			background: #f0f7ff;
			border-left-color: #2388FD;
			font-weight: 600;
		}
	}
}

.product-right {
	flex: 1;
	padding: 20rpx;
}

.search-bar {
	margin-bottom: 20rpx;
}

.loading, .empty {
	padding: 100rpx 0;
	display: flex;
	justify-content: center;
}

.product-grid {
	display: grid;
	grid-template-columns: 1fr 1fr;
	gap: 20rpx;
}

.product-card {
	background: #fff;
	border-radius: 12rpx;
	overflow: hidden;
	
	.product-image {
		width: 100%;
		height: 300rpx;
		background: #f5f5f5;
	}
	
	.product-info {
		padding: 16rpx;
		
		.product-name {
			font-size: 26rpx;
			color: #333;
			line-height: 1.4;
			display: -webkit-box;
			-webkit-line-clamp: 2;
			-webkit-box-orient: vertical;
			overflow: hidden;
		}
		
		.product-price {
			display: block;
			margin-top: 12rpx;
			font-size: 30rpx;
			color: #ee0a24;
			font-weight: 600;
		}
	}
}

.load-more {
	padding: 30rpx;
	text-align: center;
}

.no-more {
	color: #999;
	font-size: 24rpx;
}
</style>
