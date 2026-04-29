//消息提醒
export const showToast = (msg, icon) => {
	let ico = "none"
	if (icon) {
		ico = icon
	}
	return uni.showToast({
		title: msg,
		icon: ico
	});
}

export const goStyPage = (obj) => {
	showToast(obj?.title2 || obj?.title || '网络异常');
}

export const goLogin = () => {
	uni.navigateTo({
		url: '/pages/login/login'
	}).catch(() => {
		uni.navigateTo({
			url: '/pages/login/login'
		});
	});
}

export const getUserInfoCache = async () => {
	return uni.getStorageSync('userInfo') || null
}

// 获取完整图片URL
export const getImageUrl = (path) => {
	if (!path) return ''
	// 如果已经是完整URL，直接返回
	if (path.startsWith('http://') || path.startsWith('https://')) {
		return path
	}
	// 开发环境使用 localhost，生产环境使用服务器IP
	const baseUrl = 'http://223.109.140.116:8813'
	return baseUrl + path
}
 
