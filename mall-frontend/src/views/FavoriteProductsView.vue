<script setup lang="ts">
import { ArrowLeft, Delete, StarFilled, View } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { cancelFavoriteProduct, listFavoriteProducts, type FavoriteProduct } from '@/api/product';
import SiteHeader from '@/components/SiteHeader.vue';

const router = useRouter();
const loading = ref(false);
const favorites = ref<FavoriteProduct[]>([]);

onMounted(loadFavorites);

async function loadFavorites() {
  loading.value = true;
  try {
    const response = await listFavoriteProducts({ limit: 20 });
    if (!response.data.success) {
      throw new Error(response.data.message || '收藏商品加载失败');
    }
    favorites.value = response.data.data;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '收藏商品加载失败');
  } finally {
    loading.value = false;
  }
}

async function handleCancelFavorite(product: FavoriteProduct) {
  try {
    await ElMessageBox.confirm(`确定取消收藏「${product.name}」吗？`, '取消收藏', {
      type: 'warning',
      confirmButtonText: '取消收藏',
      cancelButtonText: '再想想'
    });
    const response = await cancelFavoriteProduct(product.productId);
    if (!response.data.success) {
      throw new Error(response.data.message || '取消收藏失败');
    }
    favorites.value = favorites.value.filter((item) => item.productId !== product.productId);
    ElMessage.success('已取消收藏');
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return;
    }
    ElMessage.error(error instanceof Error ? error.message : '取消收藏失败');
  }
}

function openProduct(productId: number) {
  router.push(`/product/${productId}`);
}

function formatFavoriteTime(value: string) {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 16);
}

function getProductImage(product: FavoriteProduct) {
  return product.mainImageUrl || 'https://images.unsplash.com/photo-1557821552-17105176677c?auto=format&fit=crop&w=360&q=80';
}
</script>

<template>
  <SiteHeader />

  <main class="favorite-page">
    <section class="page-shell favorite-hero">
      <el-button :icon="ArrowLeft" text @click="router.push('/profile')">返回个人主页</el-button>
      <div>
        <span><el-icon><StarFilled /></el-icon> 我的收藏</span>
      </div>
    </section>

    <section v-loading="loading" class="page-shell favorite-panel">
      <div class="section-head">
        <div>
          <h2>收藏商品</h2>
        </div>
        <strong>共 {{ favorites.length }} 件</strong>
      </div>

      <el-empty v-if="!loading && favorites.length === 0" description="暂无收藏商品" />

      <div v-else class="favorite-grid">
        <article v-for="product in favorites" :key="product.productId" class="favorite-card">
          <button class="product-image-button" type="button" @click="openProduct(product.productId)">
            <img :alt="product.name" :src="getProductImage(product)" />
          </button>
          <div class="favorite-info">
            <button class="product-title" type="button" @click="openProduct(product.productId)">
              {{ product.name }}
            </button>
            <p>{{ product.subtitle || '这个商品还没有副标题' }}</p>
            <span>收藏时间 {{ formatFavoriteTime(product.favoriteTime) }}</span>
          </div>
          <div class="favorite-footer">
            <strong>￥{{ product.minPrice || 0 }}</strong>
            <div>
              <el-button :icon="View" round @click="openProduct(product.productId)">查看</el-button>
              <el-button :icon="Delete" round type="danger" plain @click="handleCancelFavorite(product)">取消收藏</el-button>
            </div>
          </div>
        </article>
      </div>
    </section>
  </main>
</template>

<style scoped>
.favorite-page {
  padding: 28px 0 56px;
}

.favorite-hero {
  display: grid;
  gap: 18px;
  padding: 28px 0 22px;
}

.favorite-hero > div {
  padding: 28px;
  color: #fff;
  background: linear-gradient(135deg, #ff4d00, #ff8a34);
  border-radius: 8px;
}

.favorite-hero span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 800;
}

.favorite-hero h1 {
  margin: 12px 0 8px;
  font-size: 32px;
}

.favorite-hero p {
  max-width: 680px;
  margin: 0;
  opacity: 0.9;
}

.favorite-panel {
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

.favorite-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.favorite-card {
  display: grid;
  grid-template-rows: auto 1fr auto;
  overflow: hidden;
  background: #f7f8fb;
  border-radius: 8px;
}

.product-image-button {
  display: block;
  padding: 0;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.product-image-button img {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
}

.favorite-info {
  display: grid;
  gap: 8px;
  padding: 14px 14px 8px;
}

.product-title {
  padding: 0;
  overflow: hidden;
  color: #202124;
  background: transparent;
  border: 0;
  cursor: pointer;
  font-size: 16px;
  font-weight: 900;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-title:hover {
  color: #ff4d00;
}

.favorite-info p {
  display: -webkit-box;
  min-height: 42px;
  margin: 0;
  overflow: hidden;
  color: #666;
  font-size: 14px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.favorite-info span {
  color: #999;
  font-size: 13px;
}

.favorite-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 14px 14px;
}

.favorite-footer strong {
  color: #ff4d00;
  font-size: 20px;
}

.favorite-footer > div {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 640px) {
  .favorite-panel {
    padding: 18px;
  }

  .section-head,
  .favorite-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .favorite-footer > div {
    justify-content: flex-start;
  }
}
</style>
