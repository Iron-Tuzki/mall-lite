<script setup lang="ts">
import { Search, ShoppingCart, User } from '@element-plus/icons-vue';
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';

import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const authStore = useAuthStore();
const keyword = ref('');
const accountLinkText = computed(() => (authStore.isLoggedIn ? authStore.displayName : '登录'));
const accountLinkTarget = computed(() => (authStore.isLoggedIn ? '/profile' : '/login'));

function searchProducts() {
  router.push({ path: router.currentRoute.value.path, query: keyword.value ? { keyword: keyword.value } : {} });
}
</script>

<template>
  <header class="site-header">
    <div class="page-shell header-inner">
      <RouterLink class="brand" to="/">
        <span class="brand-main">Mall</span>
        <span class="brand-sub">Lite.com</span>
      </RouterLink>

      <div class="search-box">
        <el-select class="search-type" model-value="product" size="large">
          <el-option label="商品" value="product" />
        </el-select>
        <el-input
          v-model="keyword"
          class="search-input"
          placeholder="搜索商品、分类、SKU"
          size="large"
          @keyup.enter="searchProducts"
        />
        <el-button class="search-button" size="large" type="primary" :icon="Search" @click="searchProducts">
          搜索
        </el-button>
      </div>

      <nav class="header-actions" aria-label="用户入口">
        <RouterLink class="action-link" :to="accountLinkTarget">
          <el-icon><User /></el-icon>
          {{ accountLinkText }}
        </RouterLink>
        <RouterLink class="action-link cart-link" to="/cart">
          <el-icon><ShoppingCart /></el-icon>
          购物车
        </RouterLink>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.94);
  border-bottom: 1px solid #f0f0f0;
  backdrop-filter: blur(10px);
}

.header-inner {
  display: grid;
  grid-template-columns: 180px minmax(320px, 1fr) 220px;
  gap: 24px;
  align-items: center;
  min-height: 92px;
}

.brand {
  display: inline-flex;
  flex-direction: column;
  width: fit-content;
  color: #ff4d00;
  line-height: 1;
}

.brand-main {
  font-size: 42px;
  font-weight: 900;
  letter-spacing: 0;
}

.brand-sub {
  margin-top: 4px;
  font-size: 16px;
  font-weight: 800;
}

.search-box {
  display: grid;
  grid-template-columns: 110px minmax(180px, 1fr) 112px;
  align-items: stretch;
  overflow: hidden;
  border: 2px solid #ff4d00;
  border-radius: 12px;
  background: #fff;
}

.search-type,
.search-input,
.search-button {
  --el-border-color: transparent;
  --el-border-radius-base: 0;
}

.search-button {
  --el-button-bg-color: #ff4d00;
  --el-button-border-color: #ff4d00;
  --el-button-hover-bg-color: #ff6a22;
  --el-button-hover-border-color: #ff6a22;
  border-radius: 8px;
  margin: 4px;
  font-weight: 800;
}

.header-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  min-width: 0;
}

.action-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
  min-width: 88px;
  min-height: 40px;
  padding: 0 12px;
  color: #373737;
  background: #fff3ec;
  border-radius: 8px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

.cart-link {
  color: #ff4d00;
}

@media (max-width: 980px) {
  .header-inner {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 14px 0;
  }

  .header-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 560px) {
  .search-box {
    grid-template-columns: 1fr;
    border-width: 1px;
  }

  .search-button {
    margin: 0;
    border-radius: 0;
  }
}
</style>
