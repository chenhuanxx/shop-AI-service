<template>
	<view class="login-page">
		<view class="bg">
			<view class="circle c1"></view>
			<view class="circle c2"></view>
			<view class="circle c3"></view>
		</view>
		<view class="content">
			<u-status-bar></u-status-bar>
			<view class="brand">
				<image class="logo" src="/static/logo.png" mode="aspectFit"></image> 
				<view class="slogan">欢迎回来，请登录继续</view>
			</view>

			<view class="card">
				<view class="card-title">账号登录</view>
				<view class="form">
					<view class="field">
						<u-input v-model="form.username" placeholder="请输入账号或手机号" clearable border="none"></u-input>
					</view>
					<view class="field">
						<u-input v-model="form.password" placeholder="请输入密码" clearable border="none" type="password"></u-input>
					</view>

					<view class="options">
						<view class="opt-left">
							<u-switch v-model="remember" size="20"></u-switch>	
							<view class="opt-text">记住账号</view>
						</view>
					</view>

					<u-button type="primary" :loading="loading" @click="handlePasswordLogin">登录</u-button>

					<u-divider text="其他登录方式"></u-divider>

					<!-- #ifdef MP-WEIXIN -->
					<button class="wx-profile-btn" :disabled="wxLoading" @click="handleWeixinProfileLogin">
						微信授权登录
					</button>
					<!-- 官方推荐：手机号一键登录（open-type=getPhoneNumber 返回动态 code，服务端换手机号 + 签发 JWT） -->
					<button class="wx-phone-btn" open-type="getPhoneNumber" @getphonenumber="handleWeixinPhoneLogin" :disabled="wxLoading">
						微信手机号一键登录
					</button>
					<!-- #endif -->
				</view>
			</view>

			<view class="footer">登录即代表你同意用户协议与隐私政策</view>
		</view>
	</view>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useAuthStore } from '@/store/auth'
import { showToast } from '@/utils/function'

const auth = useAuthStore()

const form = reactive({
	username: '',
	password: ''
})

const remember = ref(true)
const loading = ref(false)
const wxLoading = ref(false)

onMounted(() => {
	const lastAccount = uni.getStorageSync('last_account') as string
	if (lastAccount) form.username = lastAccount
})

async function handlePasswordLogin() {
	if (!form.username || !form.password) {
		showToast('请输入账号和密码')
		return
	}
	loading.value = true
	try {
		await auth.loginWithPassword(form.username, form.password)
		if (remember.value) uni.setStorageSync('last_account', form.username)
		else uni.setStorageSync('last_account', '')
		showToast('登录成功', 'success')
		uni.switchTab({ url: '/pages/index/index' })
	} catch (e: any) {
		showToast(e?.message || '登录失败')
	} finally {
		loading.value = false
	}
}

// 需要认证的微信小程序

async function handleWeixinPhoneLogin(e: any) {
	wxLoading.value = true
	try {
		const errMsg = e?.detail?.errMsg ? String(e.detail.errMsg) : ''
		const phoneCode = e?.detail?.code ? String(e.detail.code) : ''
		if (!phoneCode) {
			showToast(errMsg || '未获取到手机号授权 code')
			return
		}
		let userInfo: any = {}
		try {
			userInfo = await new Promise<any>((resolve, reject) => {
				// #ifdef MP-WEIXIN
				wx.getUserProfile({
					desc: '用于完善用户资料',
					success: (res) => resolve(res?.userInfo || {}),
					fail: (err) => reject(err)
				})
				// #endif
				// #ifndef MP-WEIXIN
				resolve({})
				// #endif
			})
		} catch {
			userInfo = {}
		}
		const loginCode = await new Promise<string>((resolve, reject) => {
			// 小程序官方：使用 wx.login 获取临时登录凭证 code
			// #ifdef MP-WEIXIN
			wx.login({
				success: (res) => resolve(res.code),
				fail: (err) => reject(err)
			})
			// #endif
			// #ifndef MP-WEIXIN
			reject(new Error('wx.login 仅支持微信小程序环境'))
			// #endif
		})
		if (!loginCode) {
			showToast('获取微信登录 code 失败')
			return
		}
		await auth.loginWithWeixinPhone(loginCode, phoneCode, userInfo || {})
		showToast('登录成功', 'success')
		uni.switchTab({ url: '/pages/index/index' })
	} catch (err: any) {
		showToast(err?.message || '手机号登录失败')
	} finally {
		wxLoading.value = false
	}
}

async function handleWeixinProfileLogin() {
	wxLoading.value = true
	try {
		const userInfo = await new Promise<any>((resolve, reject) => {
			// #ifdef MP-WEIXIN
			wx.getUserProfile({
				desc: '用于完善用户资料',
				success: (res) => resolve(res?.userInfo || {}),
				fail: (err) => reject(err)
			})
			// #endif
			// #ifndef MP-WEIXIN
			reject(new Error('微信授权登录仅支持微信小程序环境'))
			// #endif
		})

		const loginCode = await new Promise<string>((resolve, reject) => {
			// #ifdef MP-WEIXIN
			wx.login({
				success: (res) => resolve(res.code),
				fail: (err) => reject(err)
			})
			// #endif
			// #ifndef MP-WEIXIN
			reject(new Error('wx.login 仅支持微信小程序环境'))
			// #endif
		})
		if (!loginCode) {
			showToast('获取微信登录 code 失败')
			return
		}
		await auth.loginWithWeixinProfile(loginCode, userInfo || {})
		showToast('登录成功', 'success')
		uni.switchTab({ url: '/pages/index/index' })
	} catch (err: any) {
		const msg = String(err?.errMsg || err?.message || '')
		if (msg.includes('deny') || msg.includes('cancel') || msg.includes('取消')) {
			showToast('你已取消授权')
		} else {
			showToast(msg || '微信授权登录失败')
		}
	} finally {
		wxLoading.value = false
	}
}


</script>

<style scoped lang="scss">
	.login-page {
		min-height: 100vh;
		position: relative;
		background: #f7f6fe;
		overflow: hidden;
	}

	.bg {
		position: absolute;
		inset: 0;
		background: linear-gradient(180deg, #2a7cf7 0%, #6e7cf8 50%, #f7f6fe 100%);
		pointer-events: none;
	}

	.circle {
		position: absolute;
		border-radius: 9999px;
		opacity: 0.18;
		background: rgba(255, 255, 255, 1);
	}

	.c1 {
		width: 520rpx;
		height: 520rpx;
		left: -220rpx;
		top: -220rpx;
	}

	.c2 {
		width: 360rpx;
		height: 360rpx;
		right: -180rpx;
		top: 120rpx;
	}

	.c3 {
		width: 420rpx;
		height: 420rpx;
		left: 60rpx;
		top: 520rpx;
		opacity: 0.12;
	}

	.content {
		position: relative;
		padding: 0 40rpx 60rpx;
	}

	.brand {
		padding: 90rpx 0 36rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.logo {
		width: 140rpx;
		height: 140rpx;
		border-radius: 28rpx;
		background: rgba(255, 255, 255, 0.85);
	}

	.name {
		margin-top: 18rpx;
		font-size: 44rpx;
		font-weight: 700;
		color: #ffffff;
		letter-spacing: 2rpx;
	}

	.slogan {
		margin-top: 12rpx;
		font-size: 26rpx;
		color: rgba(255, 255, 255, 0.9);
	}

	.card { 
		background: rgba(255, 255, 255, 0.98);
		border-radius: 24rpx;
		padding: 40rpx 34rpx;
		box-shadow: 0 18rpx 50rpx rgba(17, 38, 146, 0.15);
	}

	.card-title {
		font-size: 32rpx;
		font-weight: 700;
		color: #1f2d3d;
		margin-bottom: 26rpx;
	}

	.form {
		display: flex;
		flex-direction: column;
		gap: 22rpx;
	}

	.field {
		border: 2rpx solid rgba(31, 45, 61, 0.12);
		border-radius: 16rpx;
		padding: 8rpx 14rpx;
		background: #fff;
	}

 

	.options {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 6rpx 6rpx 0;
	}

	.opt-left {
		display: flex;
		align-items: center;
		gap: 14rpx;
	}

	.opt-text {
		font-size: 26rpx;
		color: #4a5568;
	}

	.footer {
		margin-top: 26rpx;
		text-align: center;
		font-size: 22rpx;
		color: rgba(255, 255, 255, 0.85);
	}

	.wx-phone-btn {
		width: 100%;
		border: none;
		border-radius: 16rpx;
		padding: 22rpx 0;
		background: #07c160;
		color: #fff;
		font-size: 30rpx;
		line-height: 1;
	}

	.wx-profile-btn {
		width: 100%;
		border: none;
		border-radius: 16rpx;
		padding: 22rpx 0;
		background: #2388fd;
		color: #fff;
		font-size: 30rpx;
		line-height: 1;
	}
</style>
