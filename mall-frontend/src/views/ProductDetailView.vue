<script setup lang="ts">
import { ArrowLeft, ShoppingCart } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { getProductDetail, type ProductDetail } from '@/api/product';

const route = useRoute();
const router = useRouter();

const productId = computed(() => Number(route.params.id));
const product = ref<ProductDetail | null>(null);
const loading = ref(false);

const mainImage = computed(() => {
  return product.value?.mainImageUrl
    || product.value?.skus?.[0]?.mainImageUrl
    || 'https://images.unsplash.com/photo-1616486886892-ff366aa67ba4?auto=format&fit=crop&w=900&q=80';
});

const displayPrice = computed(() => {
  return product.value?.minPrice ?? product.value?.skus?.[0]?.price ?? 0;
});

onMounted(() => {
  loadProductDetail();
});

watch(productId, () => {
  loadProductDetail();
});

async function loadProductDetail() {
  if (!productId.value) {
    return;
  }
  loading.value = true;
  try {
    const response = await getProductDetail(productId.value);
    product.value = response.data.data;
  } catch {
    product.value = null;
    ElMessage.error('商品详情加载失败');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="detail-page">
    <div class="page-shell">
      <el-button class="back-button" :icon="ArrowLeft" text @click="router.back()">返回首页</el-button>

      <section v-loading="loading" class="detail-panel">
        <div class="gallery">
          <img
            alt="商品详情主图"
            :src="mainImage"
          />
        </div>

        <div class="product-info">
          <el-tag type="danger">商品 ID：{{ productId }}</el-tag>
          <h1>{{ product?.name || '商品详情' }}</h1>
          <p class="subtitle">{{ product?.description || product?.subtitle || '商品详情正在路上。' }}</p>

          <div class="price-box">
            <span>到手价</span>
            <strong>¥{{ displayPrice }}</strong>
          </div>

          <div class="sku-section">
            <h2>规格</h2>
            <div class="sku-list">
              <button
                v-for="(sku, index) in product?.skus || []"
                :key="sku.id"
                class="sku"
                :class="{ active: index === 0 }"
              >
                {{ sku.skuName }}
              </button>
              <button v-if="!product?.skus?.length" class="sku active">默认规格</button>
            </div>
          </div>

          <div class="action-row">
            <el-button size="large" :icon="ShoppingCart">加入购物车</el-button>
            <el-button class="buy-button" size="large" type="primary">立即购买</el-button>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding: 24px 0 48px;
  background: #f6f7fb;
}

.back-button {
  margin-bottom: 16px;
}

.detail-panel {
  display: grid;
  grid-template-columns: minmax(320px, 520px) 1fr;
  gap: 36px;
  padding: 28px;
  background: #fff;
  border-radius: 8px;
}

.gallery img {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border-radius: 8px;
}

.product-info h1 {
  margin: 18px 0 10px;
  font-size: 30px;
}

.subtitle {
  color: #666;
  line-height: 1.7;
}

.price-box {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin: 24px 0;
  padding: 18px;
  color: #ff4d00;
  background: #fff3ec;
  border-radius: 8px;
}

.price-box strong {
  font-size: 36px;
}

.sku-section h2 {
  margin: 0 0 12px;
  font-size: 18px;
}

.sku-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.sku {
  min-height: 38px;
  padding: 0 16px;
  color: #333;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
}

.sku.active {
  color: #ff4d00;
  border-color: #ff4d00;
  background: #fff7f2;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 36px;
}

.buy-button {
  --el-button-bg-color: #ff4d00;
  --el-button-border-color: #ff4d00;
  --el-button-hover-bg-color: #ff6a22;
  --el-button-hover-border-color: #ff6a22;
}

@media (max-width: 860px) {
  .detail-panel {
    grid-template-columns: 1fr;
    padding: 18px;
  }
}
</style>
