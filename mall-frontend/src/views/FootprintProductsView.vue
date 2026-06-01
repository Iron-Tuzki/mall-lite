<script setup lang="ts">
import { ArrowLeft, Delete, DeleteFilled, View } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import {
  clearProductFootprints,
  deleteProductFootprint,
  listProductFootprints,
  type FootprintProduct
} from '@/api/product';
import SiteHeader from '@/components/SiteHeader.vue';

const router = useRouter();
const loading = ref(false);
const footprints = ref<FootprintProduct[]>([]);

onMounted(loadFootprints);

async function loadFootprints() {
  loading.value = true;
  try {
    const response = await listProductFootprints();
    if (!response.data.success) {
      throw new Error(response.data.message || '浏览足迹加载失败');
    }
    footprints.value = response.data.data;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '浏览足迹加载失败');
  } finally {
    loading.value = false;
  }
}

async function handleDelete(product: FootprintProduct) {
  try {
    await ElMessageBox.confirm(`确定删除「${product.name}」的浏览记录吗？`, '删除足迹', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    });
    const response = await deleteProductFootprint(product.productId);
    if (!response.data.success) {
      throw new Error(response.data.message || '删除足迹失败');
    }
    footprints.value = footprints.value.filter((item) => item.productId !== product.productId);
    ElMessage.success('足迹已删除');
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return;
    }
    ElMessage.error(error instanceof Error ? error.message : '删除足迹失败');
  }
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('确定清空全部浏览足迹吗？', '清空足迹', {
      type: 'warning',
      confirmButtonText: '清空',
      cancelButtonText: '取消'
    });
    const response = await clearProductFootprints();
    if (!response.data.success) {
      throw new Error(response.data.message || '清空足迹失败');
    }
    footprints.value = [];
    ElMessage.success('浏览足迹已清空');
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return;
    }
    ElMessage.error(error instanceof Error ? error.message : '清空足迹失败');
  }
}

function openProduct(productId: number) {
  router.push(`/product/${productId}`);
}

function formatBrowseTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}

function getProductImage(product: FootprintProduct) {
  return product.mainImageUrl || 'https://images.unsplash.com/photo-1557821552-17105176677c?auto=format&fit=crop&w=360&q=80';
}
</script>

<template>
  <SiteHeader />

  <main class="footprint-page">
    <section class="page-shell footprint-hero">
      <el-button :icon="ArrowLeft" text @click="router.push('/profile')">返回个人主页</el-button>
      <div>
        <span><el-icon><View /></el-icon> 我的足迹</span>
      </div>
    </section>

    <section v-loading="loading" class="page-shell footprint-panel">
      <div class="section-head">
        <div>
          <h2>浏览记录</h2>
          <p>展示最近浏览的 100 件商品，再次查看商品会更新浏览时间。</p>
        </div>
        <div class="head-actions">
          <strong>共 {{ footprints.length }} 件</strong>
          <el-button v-if="footprints.length" :icon="DeleteFilled" plain type="danger" @click="handleClear">清空全部</el-button>
        </div>
      </div>

      <el-empty v-if="!loading && footprints.length === 0" description="暂无浏览足迹" />

      <div v-else class="footprint-list">
        <article v-for="product in footprints" :key="product.productId" class="footprint-card">
          <button class="product-image-button" type="button" @click="openProduct(product.productId)">
            <img :alt="product.name" :src="getProductImage(product)" />
          </button>
          <div class="product-info">
            <button class="product-title" type="button" @click="openProduct(product.productId)">
              {{ product.name }}
            </button>
            <p>{{ product.subtitle || '这个商品还没有副标题' }}</p>
            <span>最近浏览 {{ formatBrowseTime(product.browseTime) }}</span>
          </div>
          <strong class="price">￥{{ product.minPrice || 0 }}</strong>
          <div class="card-actions">
            <el-button :icon="View" round @click="openProduct(product.productId)">查看</el-button>
            <el-button :icon="Delete" round plain type="danger" @click="handleDelete(product)">删除</el-button>
          </div>
        </article>
      </div>
    </section>
  </main>
</template>

<style scoped>
.footprint-page {
  padding: 28px 0 56px;
}

.footprint-hero {
  display: grid;
  gap: 18px;
  padding: 28px 0 22px;
}

.footprint-hero > div {
  padding: 28px;
  color: #fff;
  background: linear-gradient(135deg, #ff4d00, #ff8a34);
  border-radius: 8px;
}

.footprint-hero span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 800;
}

.footprint-panel {
  padding: 24px;
  background: #fff;
  border-radius: 8px;
}

.section-head,
.head-actions,
.card-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-head {
  justify-content: space-between;
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

.head-actions strong,
.price {
  color: #ff4d00;
  white-space: nowrap;
}

.footprint-list {
  display: grid;
  gap: 12px;
}

.footprint-card {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr) auto auto;
  gap: 16px;
  align-items: center;
  padding: 14px;
  background: #f7f8fb;
  border-radius: 8px;
}

.product-image-button,
.product-title {
  padding: 0;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.product-image-button img {
  display: block;
  width: 112px;
  height: 86px;
  border-radius: 8px;
  object-fit: cover;
}

.product-info {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.product-title {
  overflow: hidden;
  color: #202124;
  font-size: 16px;
  font-weight: 900;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-title:hover {
  color: #ff4d00;
}

.product-info p {
  margin: 0;
  overflow: hidden;
  color: #666;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-info span {
  color: #999;
  font-size: 13px;
}

.price {
  font-size: 18px;
}

@media (max-width: 720px) {
  .section-head,
  .head-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .footprint-card {
    grid-template-columns: 88px minmax(0, 1fr);
  }

  .product-image-button img {
    width: 88px;
    height: 76px;
  }

  .price,
  .card-actions {
    grid-column: 2;
  }
}
</style>
