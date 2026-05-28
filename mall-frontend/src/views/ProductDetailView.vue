<script setup lang="ts">
import { ArrowLeft, ShoppingCart } from '@element-plus/icons-vue';
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();

const productId = computed(() => Number(route.params.id));
</script>

<template>
  <main class="detail-page">
    <div class="page-shell">
      <el-button class="back-button" :icon="ArrowLeft" text @click="router.back()">返回首页</el-button>

      <section class="detail-panel">
        <div class="gallery">
          <img
            alt="商品详情主图"
            src="https://images.unsplash.com/photo-1616486886892-ff366aa67ba4?auto=format&fit=crop&w=900&q=80"
          />
        </div>

        <div class="product-info">
          <el-tag type="danger">商品 ID：{{ productId }}</el-tag>
          <h1>商品详情页占位</h1>
          <p class="subtitle">下一步这里会接入 `/api/products/{{ productId }}`，并由后端 Redis 缓存商品详情。</p>

          <div class="price-box">
            <span>到手价</span>
            <strong>¥37.8</strong>
          </div>

          <div class="sku-section">
            <h2>规格</h2>
            <div class="sku-list">
              <button class="sku active">默认规格</button>
              <button class="sku">组合装</button>
              <button class="sku">家庭装</button>
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
