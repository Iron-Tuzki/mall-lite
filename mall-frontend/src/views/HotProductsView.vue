<script setup lang="ts">
import { ArrowLeft, TrendCharts } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  batchProductFavoriteStatus,
  cancelFavoriteProduct,
  favoriteProduct,
  listHotProducts,
  type ProductSummary
} from '@/api/product';
import ProductCard, { type ProductCardData } from '@/components/ProductCard.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const fallbackImages = [
  'https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1518733057094-95b53143d2a7?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1598300042247-d088f8ab3a91?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1584346133934-a3afd2a33c4c?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1616486886892-ff366aa67ba4?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1565193566173-7a0ee3dbe261?auto=format&fit=crop&w=600&q=80'
];

const products = ref<ProductCardData[]>([]);
const loading = ref(false);

onMounted(loadHotProducts);

async function loadHotProducts() {
  loading.value = true;
  try {
    const response = await listHotProducts({ limit: 50 });
    products.value = response.data.data.map(toProductCard);
    await refreshFavoriteStatus();
  } catch {
    products.value = [];
    ElMessage.error('热门商品加载失败');
  } finally {
    loading.value = false;
  }
}

async function refreshFavoriteStatus() {
  if (!authStore.isLoggedIn || products.value.length === 0) {
    return;
  }
  const response = await batchProductFavoriteStatus(products.value.map((product) => product.id));
  const statusMap = response.data.data;
  products.value = products.value.map((product) => ({
    ...product,
    favorited: statusMap[String(product.id)] ?? false
  }));
}

async function handleToggleFavorite(product: ProductCardData) {
  if (!authStore.isLoggedIn) {
    await router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  try {
    if (product.favorited) {
      await cancelFavoriteProduct(product.id);
      product.favorited = false;
      ElMessage.success('已取消收藏');
    } else {
      await favoriteProduct(product.id);
      product.favorited = true;
      ElMessage.success('收藏成功');
    }
  } catch {
    ElMessage.error('收藏操作失败');
  }
}

function toProductCard(product: ProductSummary, index: number): ProductCardData {
  return {
    id: product.id,
    name: product.name,
    subtitle: product.subtitle || '正在热卖',
    price: product.minPrice ?? 0,
    buyers: (product.id % 90) + 10,
    imageUrl: product.mainImageUrl || fallbackImages[index % fallbackImages.length],
    badge: index < 3 ? `TOP ${index + 1}` : '热门',
    detailPath: `/product/${product.id}?source=hot`
  };
}
</script>

<template>
  <SiteHeader />

  <main class="hot-products-page">
    <section class="page-shell hot-hero">
      <el-button :icon="ArrowLeft" text @click="router.push('/')">返回首页</el-button>
      <div class="hero-copy">
        <el-icon><TrendCharts /></el-icon>
        <div>
          <h1>热门商品</h1>
          <p>根据最近浏览、收藏、加购和支付行为动态排序，越新的行为权重越高。</p>
        </div>
      </div>
    </section>

    <section v-loading="loading" class="page-shell hot-panel">
      <div class="section-head">
        <div>
          <h2>全部热门商品</h2>
          <p>当前展示最近 24 小时综合热度最高的商品。</p>
        </div>
        <strong>共 {{ products.length }} 件</strong>
      </div>

      <div class="product-grid">
        <ProductCard
          v-for="product in products"
          :key="product.id"
          :product="product"
          @toggle-favorite="handleToggleFavorite"
        />
        <el-empty v-if="!loading && products.length === 0" description="暂无热门商品" />
      </div>
    </section>
  </main>
</template>

<style scoped>
.hot-products-page {
  min-height: 100vh;
  padding-bottom: 48px;
  background: #f6f7fb;
}

.hot-hero {
  padding: 22px 0 12px;
}

.hero-copy {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 10px;
  padding: 24px;
  color: #fff;
  background: linear-gradient(135deg, #ff4d00, #ff8a34);
  border-radius: 8px;
}

.hero-copy .el-icon {
  font-size: 36px;
}

.hero-copy h1 {
  margin: 0 0 6px;
  font-size: 30px;
}

.hero-copy p {
  margin: 0;
  opacity: 0.92;
}

.hot-panel {
  padding: 24px;
  background: #fff;
  border-radius: 8px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.section-head h2 {
  margin: 0;
  font-size: 22px;
}

.section-head p {
  margin: 6px 0 0;
  color: #888;
  font-size: 13px;
}

.section-head strong {
  color: #ff4d00;
  white-space: nowrap;
}

.product-grid {
  display: grid;
  min-height: 280px;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 18px;
}

.product-grid :deep(.el-empty) {
  grid-column: 1 / -1;
}

@media (max-width: 1180px) {
  .product-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .hero-copy,
  .section-head {
    align-items: stretch;
    flex-direction: column;
  }

  .product-grid {
    grid-template-columns: 1fr;
  }
}
</style>
