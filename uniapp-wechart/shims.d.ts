declare module '*.vue' {
	import type { DefineComponent } from 'vue'
	const component: DefineComponent<{}, {}, any>
	export default component
}

declare module '@dcloudio/uni-app' {
	export const onLoad: (...args: any[]) => any
	export const onShow: (...args: any[]) => any
	export const onPullDownRefresh: (...args: any[]) => any
	export const onReachBottom: (...args: any[]) => any
}

declare const uni: any
