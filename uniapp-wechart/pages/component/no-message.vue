<template>
<view class="no-message">
	<view class="content">
		<image :src="src" class="img"></image>
		<view v-if="title" class="title">{{ title }}</view>
		<view v-if="title2" class="title2" :data-sty="title2" @tap="change">{{ title2 }}</view>
	</view>
</view>
</template>

<script setup lang="ts">
withDefaults(
	defineProps<{
		src?: string
		title?: string
		title2?: string
	}>(),
	{
		src: '',
		title: '',
		title2: ''
	}
)

const emit = defineEmits<{
	(e: 'setItem', payload: { detail: { sty: boolean } }): void
}>()

function change(e: any) {
	uni.showLoading({
		title: '加载中...'
	})
	if (e?.currentTarget?.dataset?.sty !== '刷新一下') return
	uni.getNetworkType({
		success: function(res) {
			setTimeout(() => {
				uni.hideLoading()
			}, 300)
			const ok = res.networkType !== 'none'
			if (!ok) {
				uni.showToast({ title: '无网络', icon: 'none' })
			}
			emit('setItem', { detail: { sty: ok } })
		}
	})
}
</script>
<style>
.no-message {
  width: 100%;
}

.content {
  width: 100%;
  margin-top: 120rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;

}

.img {
  width: 600rpx;
  height: 600rpx;
  display: inline-block;
  margin-left: calc(50% - 300rpx);
}

.title {
  color: #BDBEBE;
  font-size: 26rpx;
  text-align: center;
  padding: 10rpx 0;
}

.title2 {
  color: #2388FD;
  font-size: 30rpx;
  text-align: center;
  padding-top: 30rpx;
}
</style>
