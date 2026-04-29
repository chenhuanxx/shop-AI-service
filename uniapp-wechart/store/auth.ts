import { defineStore } from 'pinia'
import { api } from '@/utils/api.js'

export type UserInfo = {
	id: number
	username: string
	role: string
	phone?: string
	address?: string
	gender?: string
	birthDate?: string
	nickname?: string
	avatarUrl?: string
}

type LoginResponse = {
	access_token: string
	token_type: string
	user: UserInfo
}

type WeixinUserInfo = {
	nickName?: string
	avatarUrl?: string
	gender?: number
	country?: string
	province?: string
	city?: string
	language?: string
}

function getStoredUser(): UserInfo | null {
	const raw = uni.getStorageSync('userInfo') as Partial<UserInfo> | null
	if (!raw) return null
	if (typeof raw.username !== 'string' || !raw.username) return null
	return raw as UserInfo
}

export const useAuthStore = defineStore('auth', {
	state: () => ({
		token: (uni.getStorageSync('auth_token') as string) || '',
		user: getStoredUser()
	}) as { token: string; user: UserInfo | null },
	getters: {
		isLoggedIn: (state) => !!state.token
	},
	actions: {
		setAuth(payload: LoginResponse) {
			this.token = payload?.access_token || ''
			this.user = payload?.user || null
			uni.setStorageSync('auth_token', this.token)
			uni.setStorageSync('userInfo', this.user || {})
		},
		clearAuth() {
			this.token = ''
			this.user = null
			uni.setStorageSync('auth_token', '')
			uni.setStorageSync('userInfo', {})
		},
		ensureLoggedIn() {
			if (this.token) return true
			uni.navigateTo({ url: '/pages/login/login' }).catch(() => {
				uni.navigateTo({ url: '/pages/login/login' })
			})
			return false
		},
		async loginWithPassword(username: string, password: string) {
			const res = (await api.login({
				username,
				password
			})) as LoginResponse
			this.setAuth(res)
			try {
				await this.fetchMe()
			} catch {}
			return res
		},
		async loginWithWeixinCode(code: string) {
			// 调用后端 /auth/weixin/login，用 code 换取 openid 与 JWT
			const res = (await api.weixinLogin({
				code
			})) as LoginResponse
			this.setAuth(res)
			try {
				await this.fetchMe()
			} catch {}
			return res
		},
		async loginWithWeixinProfile(code: string, userInfo: WeixinUserInfo) {
			const res = (await api.weixinLogin({
				code,
				userInfo
			})) as LoginResponse
			this.setAuth(res)
			try {
				await this.fetchMe()
			} catch {}
			return res
		},
		async loginWithWeixinPhone(loginCode: string, phoneCode: string, userInfo?: WeixinUserInfo) {
			// 调用后端 /auth/weixin/phone-login，用 loginCode+phoneCode 换取手机号并签发 JWT
			const res = (await api.weixinPhoneLogin({
				loginCode,
				phoneCode,
				userInfo
			})) as LoginResponse
			this.setAuth(res)
			try {
				await this.fetchMe()
			} catch {}
			return res
		},
		async fetchMe() {
			const me = (await api.me()) as UserInfo
			this.user = me
			uni.setStorageSync('userInfo', me || {})
			return me
		},
		logout() {
			this.clearAuth()
		}
	}
})
