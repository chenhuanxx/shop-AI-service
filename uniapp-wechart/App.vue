<script>
import { useAuthStore } from '@/store/auth'

export default {
	globalData: {
		version: '1.0.0',
		userInfo: null,
		authReady: false
	},
	onLaunch() {
		// #ifdef MP-WEIXIN
		const auth = useAuthStore()
		this.globalData.authReady = false

		setTimeout(async () => {
			try {
				if (!auth.token) {
					uni.reLaunch({ url: '/pages/login/login' })
					return
				}

				try {
					await auth.fetchMe()
				} catch (ignored) {}

				this.globalData.userInfo = auth.user || (uni.getStorageSync('userInfo') || null)
				this.globalData.authReady = true

				try {
					const pages = getCurrentPages()
					const currentRoute = pages && pages.length ? pages[pages.length - 1].route : ''
					if (currentRoute === 'pages/login/login') {
						uni.switchTab({ url: '/pages/index/index' })
					}
				} catch (ignored) {}
			} catch (e) {
				try {
					auth.clearAuth()
				} catch (ignored) {}
				uni.reLaunch({ url: '/pages/login/login' })
			}
		}, 0)
		// #endif
	}
}
</script>

<style lang="scss">
	/* #ifndef APP-PLUS-NVUE */
	@import "uview-plus/index.scss";  

	page {
		background-color: #F7F6FE;
		color: #1F2328;
		--ui-bg: #F7F6FE;
		--ui-card-bg: #FFFFFF;
		--ui-text: #1F2328;
		--ui-muted: #64748B;
		--ui-border: rgba(15, 23, 42, 0.06);
		--ui-shadow: 0 12rpx 26rpx rgba(15, 23, 42, 0.06);
		--ui-radius: 16rpx;
		--ui-gap: 24rpx;
		--ui-padding: 24rpx;
		--ui-primary: #2388FD;
	}

	.ui-page {
		min-height: 100vh;
		padding: var(--ui-padding);
		box-sizing: border-box;
		display: flex;
		flex-direction: column;
		gap: var(--ui-gap);
	}

	.ui-card {
		background: var(--ui-card-bg);
		border-radius: var(--ui-radius);
		padding: 26rpx;
		box-shadow: var(--ui-shadow);
		border: 1rpx solid var(--ui-border);
	}

	.ui-section-title {
		font-size: 28rpx;
		font-weight: 600;
		color: var(--ui-text);
	}

	.ui-muted {
		color: var(--ui-muted);
	}

	.ui-kv {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
		gap: 20rpx;
		padding: 16rpx 0;
		border-bottom: 1rpx solid #F2F4F7;
	}

	.ui-kv:last-child {
		border-bottom: none;
	}

	.ui-kv-label {
		width: 150rpx;
		flex-shrink: 0;
		font-size: 26rpx;
		color: var(--ui-text);
	}

	.ui-kv-value {
		flex: 1;
		font-size: 26rpx;
		color: var(--ui-muted);
		text-align: right;
		word-break: break-all;
	}

	/* H5 兼容 pc 所需 */
	/* #ifdef H5 */
	@media screen and (min-width: 768px) {
		body {
			overflow-y: scroll;
		}
	}

	.uni-top-window uni-tabbar .uni-tabbar {
		background-color: #fff !important;
	}

	.uni-app--showleftwindow .hideOnPc {
		display: none !important;
	}

	/* #endif */


	.no-more {
		display: flex;
		justify-content: center;
		color: #c1bebe;
	}

	.fix-pc-padding {
		padding: 0 50px;
	}

	.uni-header-logo {
		padding: 30rpx;
		flex-direction: column;
		justify-content: center;
		align-items: center;
		margin-top: 10rpx;
	}

	.uni-header-image {
		width: 100px;
		height: 100px;
	}

	.uni-hello-text {
		color: #7A7E83;
	}

	.uni-hello-addfile {
		text-align: center;
		line-height: 300rpx;
		background: #FFF;
		padding: 50rpx;
		margin-top: 10px;
		font-size: 38rpx;
		color: #808080;
	}

	/* #endif*/


	.container {
		height: 100%;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: space-between;
		padding: 200rpx 0;
		box-sizing: border-box;
	}

	.page {
		width: 100vw;
		height: 100vh;
	}

	.pages {
		position: relative;
		z-index: 9999998;
		background: #fff;
	}

	.center-x {
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.center-y {
		display: flex;
		align-items: center;
		justify-content: center;
		flex-direction: column;
	}

	.lineing {
		background-color: #F2F2F2;
		height: 20rpx;
		width: 100vw;
	}

	.overflow {
		overflow: hidden;
		text-overflow: ellipsis;
		display: -webkit-box;
		-webkit-box-orient: vertical;
	}

	::-webkit-scrollbar {
		width: 0;
		height: 0;
		color: transparent;
	}

	button::after {
		border-width: 0;
	}

	.richImgs {
		margin: 20rpx 0;
	}

	rich-text .richImg {
		max-width: 100%;
		max-height: 100%;
		margin: 20rpx 0;
		vertical-align: middle;
		height: auto !important;
		width: auto !important;
	}

	.no-data {
		width: 100%;
		height: calc(100vh - 100px);
		display: flex;
		justify-content: center;
		align-items: center;
	}

	.no-data image {
		height: 130px;
		width: 130px;
	}


	.page-loading {
		height: 80rpx;
		width: 100vw;
		text-align: center;
		line-height: 80rpx;
		color: #fff;
		background-color: rgba(0, 0, 0, 0.4);
		font-size: 28rpx;
		position: fixed;
		bottom: 0;
		left: 0;
	}


	.top-main {
		position: sticky;
		top: 0;
		width: 100%;
		background: rgba(255, 255, 255, 0.92);
		backdrop-filter: blur(12px);
		padding: 20rpx 20rpx;
		display: flex;
		justify-content: space-between;
		align-items: center;
		z-index: 99;
		box-shadow: 0 10rpx 24rpx rgba(15, 23, 42, 0.06);

		.top-bg {
			width: 100%;
			display: flex;
			justify-content: space-between;
			align-items: center;
			overflow: hidden;

			.search {
				margin-left: 20rpx;
				height: 80rpx;
				flex: 1;
				padding: 10rpx 20rpx;
				border-radius: 999rpx;
				background-color: #F6F7FB;
				text-align: left;
				color: #1F2328;
				border: 1rpx solid rgba(15, 23, 42, 0.06);
			}

			.icon {
				display: flex;
				align-items: center;
				width: 100rpx;
				margin-left: 20rpx;
				line-height: 64rpx;
				font-size: 32rpx;
				color: #A3B2C0;
				border-radius: 100%;
				justify-content: center;
				height: 80rpx;
				background: #F6F7FB;
				border: 1rpx solid rgba(15, 23, 42, 0.06);

			}
			
			.scan{
				display: flex;
				align-items: center;
				width: 100rpx;
				margin: 0 20rpx;
				line-height: 64rpx;
				font-size: 32rpx;
				color: #00aaff;
				border-radius: 100%;
				justify-content: center;
				height: 80rpx; 
			}

		}
	}


	.about-tit {
		flex: 1;
		font-size: 32rpx;
		font-weight: 600;
		color: #000000;

		padding: 30rpx 30rpx 30rpx 30rpx;
	}

	.bor-top {
		border-top: 16rpx solid #F7F6FE;
	}
</style>
