import {
	goLogin,
	showToast
} from './function';

function resolveBaseUrl() { 
	return 'http://localhost:8813' 
}

function normalizeRequestData(method, data) {
	if (!data || typeof data !== 'object') return data
	if (Array.isArray(data)) return data
	const cleaned = {}
	Object.keys(data).forEach((k) => {
		const v = data[k]
		if (v === undefined || v === null) return
		cleaned[k] = v
	})
	return cleaned
}

export const request = (url, method, data, showLoad) => {
	let showLoading = true;
	let alreadyShow = false;
	setTimeout(() => {
		if (showLoading) {
			alreadyShow = true;
			uni.showLoading({
				title: '加载中'
			});
		}
	}, 500);
	var baseUrl = resolveBaseUrl();
	var auth_token = uni.getStorageSync('auth_token');
	if (baseUrl == null || baseUrl == "") {
		showToast("未配置 baseUrl");
		return Promise.reject(new Error("baseUrl is empty"));
	}
	const {
		model
	} = uni.getSystemInfoSync();
	return new Promise(function(resolve, reject) {
		uni.request({
			url: baseUrl + url,
			data: normalizeRequestData(method, data),
			method: method,
			header: Object.assign({
				'Content-Type': 'application/json',
				'Authorization': auth_token ? `Bearer ${auth_token}` : "",
				'devicename': model || ''
			}),
			success: function(res) {
				uni.stopPullDownRefresh();
				setTimeout(() => {
					if (alreadyShow) uni.hideLoading();
				}, 50);
				const payload = res.data || {};
				if (res.statusCode === 401) {
					uni.setStorageSync('auth_token', '');
					goLogin();
					return reject(new Error('未登录'));
				}
				if (typeof payload.code === 'number' && typeof payload.message === 'string') {
					if (payload.code === 0) {
						return resolve(payload.data);
					}
					showToast(payload.message || '请求失败');
					return reject(new Error(payload.message || '请求失败'));
				}
				return resolve(payload);
			},
			fail: function(err) {
				setTimeout(() => {
					if (alreadyShow) uni.hideLoading();
				}, 50);
				showToast(err?.errMsg || '请求失败');
				uni.getNetworkType({
					success: function(res) {
						if (res.networkType == 'none') {
							showToast('无网络');
						}
					}
				});
				return reject(err);
			},
			complete: function(res) {
				showLoading = false; // return resolve(res.data);
			}
		});
	});
};
