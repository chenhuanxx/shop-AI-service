import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '../views/home/HomeView.vue'
import DashboardView from '../views/home/DashboardView.vue'
import LoginView from '../views/auth/LoginView.vue'
import OpenCoursesView from '../views/course/OpenCoursesView.vue'
import PublicCourseApplyView from '../views/course/PublicCourseApplyView.vue'
import BookingCenterView from '../views/course/BookingCenterView.vue'
import OpenCoursesPublishView from '../views/course/OpenCoursesPublishView.vue'
import RolesView from '../views/system/RolesView.vue'
import UsersView from '../views/system/UsersView.vue'
import ProductView from '../views/product/ProductView.vue'
import ProductCategoryView from '../views/product/ProductCategoryView.vue'
import OrderView from '../views/order/OrderView.vue'
import BannerView from '../views/banner/BannerView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView, meta: { requiresAuth: true } },
    { path: '/dashboard', name: 'dashboard', component: DashboardView, meta: { requiresAuth: true } },
    {
      path: '/products',
      name: 'products',
      component: ProductView,
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/product-categories',
      name: 'productCategories',
      component: ProductCategoryView,
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/orders',
      name: 'orders',
      component: OrderView,
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/banners',
      name: 'banners',
      component: BannerView,
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/open-courses',
      name: 'openCourses',
      component: OpenCoursesView,
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/open-publish',
      name: 'openCoursesPublish',
      component: OpenCoursesPublishView,
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/public-course-apply',
      name: 'publicCourseApply',
      component: PublicCourseApplyView,
      meta: { requiresAuth: true },
    },
    {
      path: '/booking-center',
      name: 'bookingCenter',
      component: BookingCenterView,
      meta: { requiresAuth: true },
    },
    {
      path: '/users',
      name: 'users',
      component: UsersView,
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/roles',
      name: 'roles',
      component: RolesView,
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    { path: '/login', name: 'login', component: LoginView },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('access_token') || ''
  if (to.path === '/login' && token) {
    return { path: '/' }
  }
  if (to.meta.requiresAuth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin) {
    const raw = localStorage.getItem('pa_user') || ''
    const role = (() => {
      if (!raw) return ''
      try {
        const u = JSON.parse(raw) as { role?: string; username?: string }
        return (u.role || u.username || '').toString()
      } catch {
        return ''
      }
    })()
    const isAdmin = role.trim().toLowerCase() === 'admin'
    if (!isAdmin) {
      return { path: '/' }
    }
  }
  return true
})

export default router
