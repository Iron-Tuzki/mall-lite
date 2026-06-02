<script setup lang="ts">
import { Delete, ShoppingCart } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import {
  deleteCartItem,
  listCartItems,
  updateCartItemQuantity,
  type CartItem
} from '@/api/cart';
import SiteHeader from '@/components/SiteHeader.vue';
import { setCartCheckoutItems } from '@/utils/checkoutStorage';

const router = useRouter();
const items = ref<CartItem[]>([]);
const selectedSkuIds = ref<number[]>([]);
const loading = ref(false);
const updatingSkuId = ref<number | null>(null);

const selectedItems = computed(() => items.value.filter((item) => selectedSkuIds.value.includes(item.skuId)));
const selectedAmount = computed(() => selectedItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2));
const allAvailableSelected = computed({
  get: () => {
    const availableItems = items.value.filter((item) => item.available);
    return availableItems.length > 0 && availableItems.every((item) => selectedSkuIds.value.includes(item.skuId));
  },
  set: (checked: boolean) => {
    selectedSkuIds.value = checked ? items.value.filter((item) => item.available).map((item) => item.skuId) : [];
  }
});

onMounted(loadCartItems);

async function loadCartItems() {
  loading.value = true;
  try {
    const response = await listCartItems();
    items.value = response.data.data;
    const availableSkuIds = new Set(items.value.filter((item) => item.available).map((item) => item.skuId));
    selectedSkuIds.value = selectedSkuIds.value.filter((skuId) => availableSkuIds.has(skuId));
  } catch {
    ElMessage.error('购物车加载失败');
  } finally {
    loading.value = false;
  }
}

async function updateQuantity(item: CartItem, quantity: number | undefined) {
  if (!quantity || quantity === item.quantity) {
    return;
  }
  updatingSkuId.value = item.skuId;
  try {
    await updateCartItemQuantity(item.skuId, quantity);
    item.quantity = quantity;
  } catch {
    ElMessage.error('商品数量更新失败');
    await loadCartItems();
  } finally {
    updatingSkuId.value = null;
  }
}

async function removeItem(item: CartItem) {
  const confirmed = await ElMessageBox.confirm(`确认从购物车移除“${item.productName}”吗？`, '移除商品', {
    confirmButtonText: '移除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => true).catch(() => false);
  if (!confirmed) {
    return;
  }
  try {
    await deleteCartItem(item.skuId);
    selectedSkuIds.value = selectedSkuIds.value.filter((skuId) => skuId !== item.skuId);
    items.value = items.value.filter((cartItem) => cartItem.skuId !== item.skuId);
    ElMessage.success('已移除商品');
  } catch {
    ElMessage.error('移除商品失败');
  }
}

async function checkout() {
  if (selectedItems.value.length === 0) {
    ElMessage.warning('请选择需要结算的商品');
    return;
  }
  setCartCheckoutItems(selectedItems.value.map((item) => ({
    productId: item.productId,
    skuId: item.skuId,
    quantity: item.quantity
  })));
  await router.push({ path: '/order/confirm', query: { source: 'cart' } });
}
</script>

<template>
  <SiteHeader />

  <main class="cart-page">
    <div class="page-shell">
      <header class="cart-head">
        <div>
          <span>Shopping Cart</span>
          <h1>购物车</h1>
        </div>
        <strong>{{ items.length }} 件商品</strong>
      </header>

      <section v-loading="loading" class="cart-layout">
        <div class="cart-list">
          <el-empty v-if="items.length === 0" description="购物车还是空的">
            <el-button type="primary" @click="router.push('/')">去逛逛</el-button>
          </el-empty>

          <article v-for="item in items" :key="item.skuId" class="cart-row" :class="{ unavailable: !item.available }">
            <el-checkbox
              v-model="selectedSkuIds"
              :disabled="!item.available"
              :label="item.skuId"
              :value="item.skuId"
            />
            <img :alt="item.productName" :src="item.mainImageUrl" />
            <div class="goods-info">
              <h2>{{ item.productName }}</h2>
              <p>{{ item.skuName }}</p>
              <el-tag v-if="!item.available" size="small" type="info">暂不可购买</el-tag>
            </div>
            <strong>￥{{ item.price }}</strong>
            <el-input-number
              :disabled="!item.available || updatingSkuId === item.skuId"
              :max="99"
              :min="1"
              :model-value="item.quantity"
              size="small"
              @change="(value: number | undefined) => updateQuantity(item, value)"
            />
            <el-button :icon="Delete" circle text title="移除商品" @click="removeItem(item)" />
          </article>
        </div>

        <aside class="cart-summary">
          <div class="summary-title">
            <el-icon><ShoppingCart /></el-icon>
            <h2>结算清单</h2>
          </div>
          <el-checkbox v-model="allAvailableSelected">全选可购买商品</el-checkbox>
          <div>
            <span>已选商品</span>
            <strong>{{ selectedItems.length }} 件</strong>
          </div>
          <div class="total-row">
            <span>合计</span>
            <strong>￥{{ selectedAmount }}</strong>
          </div>
          <el-button class="checkout-button" size="large" type="primary" @click="checkout">去结算</el-button>
        </aside>
      </section>
    </div>
  </main>
</template>

<style scoped>
.cart-page {
  min-height: 100vh;
  padding: 30px 0 56px;
  background: #f6f7fb;
}

.cart-head,
.summary-title,
.cart-summary div {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cart-head {
  margin-bottom: 18px;
}

.cart-head span {
  color: #ff4d00;
  font-weight: 900;
}

.cart-head h1 {
  margin: 4px 0 0;
  font-size: 32px;
}

.cart-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 18px;
}

.cart-list,
.cart-summary {
  background: #fff;
  border-radius: 8px;
}

.cart-list {
  min-height: 220px;
}

.cart-row {
  display: grid;
  grid-template-columns: 26px 96px minmax(0, 1fr) 96px 130px 36px;
  gap: 14px;
  align-items: center;
  padding: 18px;
  border-bottom: 1px solid #f0f0f0;
}

.cart-row:last-child {
  border-bottom: 0;
}

.cart-row.unavailable {
  color: #999;
  background: #fafafa;
}

.cart-row img {
  width: 96px;
  height: 96px;
  object-fit: cover;
  border-radius: 8px;
}

.goods-info h2 {
  margin: 0 0 10px;
  font-size: 17px;
}

.goods-info p {
  margin: 0 0 8px;
  color: #777;
}

.cart-row > strong,
.total-row strong {
  color: #ff4d00;
}

.cart-summary {
  align-self: start;
  position: sticky;
  top: 116px;
  display: grid;
  gap: 16px;
  padding: 20px;
}

.summary-title {
  justify-content: flex-start;
  gap: 8px;
}

.summary-title h2 {
  margin: 0;
  font-size: 20px;
}

.total-row {
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  font-size: 18px;
}

.checkout-button {
  --el-button-bg-color: #ff4d00;
  --el-button-border-color: #ff4d00;
  --el-button-hover-bg-color: #ff6a22;
  --el-button-hover-border-color: #ff6a22;
  font-weight: 800;
}

@media (max-width: 900px) {
  .cart-layout {
    grid-template-columns: 1fr;
  }

  .cart-summary {
    position: static;
  }
}

@media (max-width: 720px) {
  .cart-row {
    grid-template-columns: 24px 76px minmax(0, 1fr) 36px;
    gap: 10px;
    padding: 14px;
  }

  .cart-row img {
    width: 76px;
    height: 76px;
  }

  .cart-row > strong,
  .cart-row :deep(.el-input-number) {
    grid-column: 3;
  }
}
</style>
