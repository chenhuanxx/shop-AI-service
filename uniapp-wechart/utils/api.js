import {
	request
} from './request.js';

export const API = () => {
	return {
		login: payload => request('/auth/login', 'POST', payload),
		weixinLogin: payload => request('/auth/weixin/login', 'POST', payload),
		weixinPhoneLogin: payload => request('/auth/weixin/phone-login', 'POST', payload),
		me: () => request('/auth/me', 'GET', {}),

		myApplicationsPage: params => request('/public-course/my-applications/page', 'GET', params),
		myApplicationDetail: params => request('/public-course/my-applications/detail', 'GET', params),
		applicationsPage: params => request('/public-course/applications/page', 'GET', params),
		auditApplication: payload => request('/public-course/applications/audit', 'POST', payload),

		listCoursesPage: params => request('/booking/courses/page', 'GET', params),
		getCourseDetail: params => request('/booking/course-detail', 'GET', params),
		
		// 预约中心新API
		getAvailableCourses: params => request('/booking/available-courses', 'GET', params),
		getMyReservedCourses: params => request('/booking/my-reserved-courses', 'GET', params),
		getMyCheckedInCourses: params => request('/booking/my-checked-in-courses', 'GET', params),

		reserve: payload => request('/booking/reserve', 'POST', payload),
		cancelReserve: payload => request('/booking/cancel', 'POST', payload),

		// 签到相关
		checkin: payload => request('/booking/checkin', 'POST', payload),
		checkinStatus: params => request('/booking/checkin-status', 'GET', params),
		myCheckins: () => request('/booking/my-checkins', 'GET', {}),

		// 产品相关
		listProducts: params => request('/products', 'GET', params),
		getProduct: (id) => request(`/products/${id}`, 'GET', {}),
		listCategories: () => request('/products/categories', 'GET', {}),

		// 购物车相关
		getCart: () => request('/cart', 'GET', {}),
		addToCart: payload => request('/cart/add', 'POST', payload),
		updateCartItem: payload => request('/cart/update', 'PUT', payload),
		removeCartItem: (id) => request(`/cart/${id}`, 'DELETE', {}),
		clearCart: () => request('/cart/clear', 'DELETE', {}),

		// 收藏相关
		getFavorites: () => request('/favorites', 'GET', {}),
		addFavorite: payload => request('/favorites/add', 'POST', payload),
		removeFavorite: (productId) => request(`/favorites/${productId}`, 'DELETE', {}),
		isFavorited: (productId) => request(`/favorites/check/${productId}`, 'GET', {}),

		// Banner相关
		getBanners: () => request('/banners/enabled', 'GET', {}),

		// 订单相关
		createOrder: payload => request('/orders/create', 'POST', payload),
		getOrders: params => request('/orders', 'GET', params),
		getOrderDetail: (id) => request(`/orders/${id}`, 'GET', {}),
		updateOrderStatus: (id, status) => request(`/orders/${id}/status`, 'PUT', { status }),
		deleteOrder: (id) => request(`/orders/${id}`, 'DELETE', {}),
	};
}

export const api = API()
