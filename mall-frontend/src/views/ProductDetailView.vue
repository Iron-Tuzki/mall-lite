<script setup lang="ts">
import { ArrowLeft, ShoppingCart, Star, StarFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { addCartItem } from '@/api/cart';
import {
  cancelFavoriteProduct,
  checkProductFavorite,
  favoriteProduct,
  getHotProductDetail,
  getProductDetail,
  type ProductDetail,
  type SkuItem
} from '@/api/product';
import SiteHeader from '@/components/SiteHeader.vue';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const productId = computed(() => Number(route.params.id));
const hotSource = computed(() => route.query.source === 'hot');
const product = ref<ProductDetail | null>(null);
const selectedSkuId = ref<number | null>(null);
const quantity = ref(1);
const loading = ref(false);
const favorited = ref(false);
const favoriteSubmitting = ref(false);
const cartSubmitting = ref(false);

const selectedSku = computed(() => product.value?.skus.find((sku) => sku.id === selectedSkuId.value) || null);
const mainImage = computed(() => {
  return selectedSku.value?.mainImageUrl
    || product.value?.mainImageUrl
    || 'https://images.unsplash.com/photo-1616486886892-ff366aa67ba4?auto=format&fit=crop&w=900&q=80';
});
const displayPrice = computed(() => selectedSku.value?.price ?? product.value?.minPrice ?? 0);

onMounted(() => {
  loadProductDetail();
});

watch([productId, hotSource], () => {
  loadProductDetail();
});

async function loadProductDetail() {
  if (!productId.value) {
    return;
  }
  loading.value = true;
  try {
    const response = hotSource.value
      ? await getHotProductDetail(productId.value)
      : await getProductDetail(productId.value);
    product.value = response.data.data;
    selectedSkuId.value = product.value.skus[0]?.id || null;
    await loadFavoriteStatus();
  } catch {
    product.value = null;
    selectedSkuId.value = null;
    ElMessage.error('商品详情加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadFavoriteStatus() {
  if (!authStore.isLoggedIn || !productId.value) {
    favorited.value = false;
    return;
  }
  const response = await checkProductFavorite(productId.value);
  favorited.value = Boolean(response.data.data);
}

function selectSku(sku: SkuItem) {
  selectedSkuId.value = sku.id;
}

async function toggleFavorite() {
  if (!authStore.isLoggedIn) {
    await router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  favoriteSubmitting.value = true;
  try {
    if (favorited.value) {
      await cancelFavoriteProduct(productId.value);
      favorited.value = false;
      ElMessage.success('已取消收藏');
    } else {
      await favoriteProduct(productId.value);
      favorited.value = true;
      ElMessage.success('收藏成功');
    }
  } catch {
    ElMessage.error('收藏操作失败');
  } finally {
    favoriteSubmitting.value = false;
  }
}

async function addToCart() {
  if (!selectedSku.value) {
    ElMessage.warning('请选择商品规格');
    return;
  }
  if (!authStore.isLoggedIn) {
    await router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  cartSubmitting.value = true;
  try {
    await addCartItem(selectedSku.value.id, quantity.value);
    ElMessage.success('已加入购物车');
  } catch {
    ElMessage.error('加入购物车失败');
  } finally {
    cartSubmitting.value = false;
  }
}

function buyNow() {
  if (!selectedSku.value) {
    ElMessage.warning('请选择商品规格');
    return;
  }
  if (!authStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  router.push({
    path: '/order/confirm',
    query: {
      productId: String(productId.value),
      skuId: String(selectedSku.value.id),
      quantity: String(quantity.value)
    }
  });
}
</script>

<template>
  <SiteHeader />

  <main class="detail-page">
    <div class="page-shell">
      <el-button class="back-button" :icon="ArrowLeft" text @click="router.back()">返回</el-button>

      <section v-loading="loading" class="detail-panel">
        <div class="gallery">
          <img alt="商品详情主图" :src="mainImage" />
        </div>

        <div class="product-info">
          <el-tag type="danger">商品 ID：{{ productId }}</el-tag>
          <el-button
            class="favorite-action"
            :icon="favorited ? StarFilled : Star"
            :loading="favoriteSubmitting"
            round
            @click="toggleFavorite"
          >
            {{ favorited ? '已收藏' : '收藏商品' }}
          </el-button>
          <h1>{{ product?.name || '商品详情' }}</h1>
          <p class="subtitle">{{ product?.description || product?.subtitle || '商品详情正在加载中' }}</p>

          <div class="price-box">
            <span>到手价</span>
            <strong>￥{{ displayPrice }}</strong>
          </div>

          <div class="sku-section">
            <h2>规格</h2>
            <div class="sku-list">
              <button
                v-for="sku in product?.skus || []"
                :key="sku.id"
                class="sku"
                :class="{ active: sku.id === selectedSkuId }"
                type="button"
                @click="selectSku(sku)"
              >
                {{ sku.skuName }}
              </button>
              <button v-if="!product?.skus?.length" class="sku active" type="button">暂无规格</button>
            </div>
          </div>

          <div class="quantity-row">
            <span>数量</span>
            <el-input-number v-model="quantity" :min="1" :max="99" />
          </div>

          <div class="action-row">
            <el-button size="large" :icon="ShoppingCart" :loading="cartSubmitting" @click="addToCart">
              加入购物车
            </el-button>
            <el-button class="buy-button" size="large" type="primary" @click="buyNow">立即购买</el-button>
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

.favorite-action {
  margin-left: 10px;
  color: #ff4d00;
  border-color: #ffd7c4;
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

.sku-section h2,
.quantity-row span {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 800;
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

.quantity-row {
  display: grid;
  gap: 10px;
  margin-top: 24px;
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
