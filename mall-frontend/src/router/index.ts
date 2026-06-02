import { createRouter, createWebHistory } from 'vue-router';

import { useAuthStore } from '@/stores/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue')
    },
    {
      path: '/product/:id',
      name: 'product-detail',
      component: () => import('@/views/ProductDetailView.vue')
    },
    {
      path: '/category/:categoryId',
      name: 'category-products',
      component: () => import('@/views/CategoryProductsView.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue')
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: {
        requiresAuth: true
      }
    },
    {
      path: '/favorites',
      name: 'favorites',
      component: () => import('@/views/FavoriteProductsView.vue'),
      meta: {
        requiresAuth: true
      }
    },
    {
      path: '/footprints',
      name: 'footprints',
      component: () => import('@/views/FootprintProductsView.vue'),
      meta: {
        requiresAuth: true
      }
    },
    {
      path: '/addresses',
      name: 'addresses',
      component: () => import('@/views/AddressManageView.vue'),
      meta: {
        requiresAuth: true
      }
    },
    {
      path: '/cart',
      name: 'cart',
      component: () => import('@/views/CartView.vue'),
      meta: {
        requiresAuth: true
      }
    },
    {
      path: '/order/confirm',
      name: 'order-confirm',
      component: () => import('@/views/OrderConfirmView.vue'),
      meta: {
        requiresAuth: true
      }
    },
    {
      path: '/orders',
      name: 'orders',
      component: () => import('@/views/OrdersView.vue'),
      meta: {
        requiresAuth: true
      }
    },
    {
      path: '/order/result/:orderId',
      name: 'order-result',
      component: () => import('@/views/OrderResultView.vue'),
      meta: {
        requiresAuth: true
      }
    }
  ],
  scrollBehavior() {
    return { top: 0 };
  }
});

router.beforeEach((to) => {
  const authStore = useAuthStore();
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    };
  }
  return true;
});

export default router;
