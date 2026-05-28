import { createRouter, createWebHistory } from 'vue-router';

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
    }
  ],
  scrollBehavior() {
    return { top: 0 };
  }
});

export default router;
