<script setup lang="ts">
import { Location, Plus } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { listAddresses, type AddressItem } from '@/api/address';
import { deleteCartItems } from '@/api/cart';
import { createOrder } from '@/api/order';
import { getProductDetail, type ProductDetail, type SkuItem } from '@/api/product';
import SiteHeader from '@/components/SiteHeader.vue';
import { useAuthStore } from '@/stores/auth';
import {
  clearCartCheckoutItems,
  getCartCheckoutItems,
  type CheckoutItem
} from '@/utils/checkoutStorage';

interface SelectedGoods {
  checkoutItem: CheckoutItem;
  product: ProductDetail;
  sku: SkuItem;
}

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const checkoutItems = ref<CheckoutItem[]>([]);
const products = ref<Record<number, ProductDetail>>({});
const addresses = ref<AddressItem[]>([]);
const selectedAddressId = ref<number | null>(null);
const remark = ref('');
const loading = ref(false);
const submitting = ref(false);

const isCartCheckout = computed(() => route.query.source === 'cart');
const selectedAddress = computed(() => addresses.value.find((address) => address.id === selectedAddressId.value) || null);
const selectedGoods = computed<SelectedGoods[]>(() => checkoutItems.value.flatMap((checkoutItem) => {
  const product = products.value[checkoutItem.productId];
  const sku = product?.skus.find((item) => item.id === checkoutItem.skuId);
  return product && sku ? [{ checkoutItem, product, sku }] : [];
}));
const goodsAmount = computed(() => selectedGoods.value.reduce((sum, goods) => (
  sum + goods.sku.price * goods.checkoutItem.quantity
), 0).toFixed(2));

onMounted(async () => {
  if (!authStore.user) {
    await authStore.fetchCurrentUser().catch(async () => {
      await router.replace({ path: '/login', query: { redirect: route.fullPath } });
    });
  }
  if (!authStore.user) {
    return;
  }
  await loadConfirmData();
});

function prepareCheckoutItems() {
  if (isCartCheckout.value) {
    checkoutItems.value = getCartCheckoutItems();
    return checkoutItems.value.length > 0;
  }
  const productId = Number(route.query.productId);
  const skuId = Number(route.query.skuId);
  const quantity = Number(route.query.quantity || 1);
  if (!productId || !skuId || !Number.isInteger(quantity) || quantity < 1 || quantity > 99) {
    return false;
  }
  checkoutItems.value = [{ productId, skuId, quantity }];
  return true;
}

async function loadConfirmData() {
  if (!prepareCheckoutItems()) {
    ElMessage.warning('缺少商品信息，请重新选择商品');
    await router.replace(isCartCheckout.value ? '/cart' : '/');
    return;
  }
  loading.value = true;
  try {
    const productIds = [...new Set(checkoutItems.value.map((item) => item.productId))];
    const [productResponses, addressResponse] = await Promise.all([
      Promise.all(productIds.map((productId) => getProductDetail(productId))),
      listAddresses(authStore.user!.id)
    ]);
    products.value = Object.fromEntries(productResponses.map((response) => {
      const product = response.data.data;
      return [product.id, product];
    }));
    if (selectedGoods.value.length !== checkoutItems.value.length) {
      ElMessage.warning('部分商品规格已失效，请重新选择');
      await router.replace(isCartCheckout.value ? '/cart' : `/product/${checkoutItems.value[0].productId}`);
      return;
    }
    addresses.value = addressResponse.data.data;
    selectedAddressId.value = addresses.value.find((address) => address.defaultFlag === 1)?.id || addresses.value[0]?.id || null;
  } catch {
    ElMessage.error('确认订单信息加载失败');
  } finally {
    loading.value = false;
  }
}

async function submitOrder() {
  if (!selectedAddress.value) {
    ElMessage.warning('请选择收货地址');
    return;
  }
  if (selectedGoods.value.length === 0) {
    ElMessage.warning('请选择商品规格');
    return;
  }
  submitting.value = true;
  try {
    const response = await createOrder({
      requestId: `front-${isCartCheckout.value ? 'cart' : 'buy-now'}-${Date.now()}-${Math.random().toString(16).slice(2)}`,
      addressId: selectedAddress.value.id,
      items: selectedGoods.value.map((goods) => ({
        skuId: goods.sku.id,
        quantity: goods.checkoutItem.quantity
      })),
      remark: remark.value.trim() || undefined
    });
    if (!response.data.success) {
      throw new Error(response.data.message || '提交订单失败');
    }
    if (isCartCheckout.value) {
      await deleteCartItems(checkoutItems.value.map((item) => item.skuId)).catch(() => {
        ElMessage.warning('订单已创建，购物车稍后可手动清理');
      });
      clearCartCheckoutItems();
    }
    await router.replace(`/order/result/${response.data.data.orderId}`);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交订单失败');
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <SiteHeader />

  <main v-loading="loading" class="confirm-page page-shell">
    <section class="confirm-head">
      <div>
        <span>{{ isCartCheckout ? 'Cart Checkout' : 'Buy Now' }}</span>
        <h1>确认订单</h1>
        <p>确认收货地址、商品信息和支付金额后提交订单。</p>
      </div>
    </section>

    <section class="confirm-grid">
      <div class="main-column">
        <div class="confirm-section">
          <div class="section-head">
            <h2>收货地址</h2>
            <el-button :icon="Plus" text type="primary" @click="router.push('/addresses')">管理地址</el-button>
          </div>

          <el-empty v-if="addresses.length === 0" description="暂无收货地址">
            <el-button type="primary" @click="router.push('/addresses')">新增收货地址</el-button>
          </el-empty>

          <el-radio-group v-else v-model="selectedAddressId" class="address-options">
            <el-radio v-for="address in addresses" :key="address.id" :label="address.id" border>
              <div class="address-option">
                <strong>{{ address.receiverName }} {{ address.receiverPhone }}</strong>
                <span>
                  <el-icon><Location /></el-icon>
                  {{ address.province }} {{ address.city }} {{ address.district }} {{ address.detailAddress }}
                </span>
              </div>
            </el-radio>
          </el-radio-group>
        </div>

        <div class="confirm-section">
          <div class="section-head">
            <h2>商品信息</h2>
          </div>
          <div v-for="goods in selectedGoods" :key="goods.sku.id" class="goods-row">
            <img :alt="goods.product.name" :src="goods.sku.mainImageUrl || goods.product.mainImageUrl" />
            <div>
              <h3>{{ goods.product.name }}</h3>
              <p>{{ goods.sku.skuName }}</p>
              <span>数量：{{ goods.checkoutItem.quantity }}</span>
            </div>
            <strong>￥{{ goods.sku.price }}</strong>
          </div>
        </div>

        <div class="confirm-section">
          <div class="section-head">
            <h2>订单备注</h2>
          </div>
          <el-input v-model="remark" maxlength="120" placeholder="可填写配送备注，选填" show-word-limit />
        </div>
      </div>

      <aside class="summary-card">
        <h2>订单汇总</h2>
        <div>
          <span>商品金额</span>
          <strong>￥{{ goodsAmount }}</strong>
        </div>
        <div>
          <span>运费</span>
          <strong>￥0</strong>
        </div>
        <div class="total-row">
          <span>应付金额</span>
          <strong>￥{{ goodsAmount }}</strong>
        </div>
        <el-button class="submit-button" :loading="submitting" size="large" type="primary" @click="submitOrder">
          提交订单
        </el-button>
      </aside>
    </section>
  </main>
</template>

<style scoped>
.confirm-page {
  padding: 28px 0 56px;
}

.confirm-head {
  padding: 28px;
  color: #fff;
  background: linear-gradient(135deg, #ff4d00, #ff8a34);
  border-radius: 8px;
}

.confirm-head span {
  font-weight: 900;
}

.confirm-head h1 {
  margin: 8px 0;
  font-size: 34px;
}

.confirm-head p {
  margin: 0;
}

.confirm-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
  margin-top: 18px;
}

.main-column,
.address-options {
  display: grid;
  gap: 14px;
}

.confirm-section,
.summary-card {
  padding: 22px;
  background: #fff;
  border-radius: 8px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-head h2,
.summary-card h2 {
  margin: 0;
  font-size: 20px;
}

.address-options :deep(.el-radio) {
  width: 100%;
  height: auto;
  margin: 0;
  padding: 14px;
}

.address-option {
  display: grid;
  gap: 8px;
  line-height: 1.6;
}

.address-option span {
  color: #666;
}

.goods-row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #f0f0f0;
}

.goods-row:first-of-type {
  padding-top: 0;
}

.goods-row:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.goods-row img {
  width: 96px;
  height: 96px;
  object-fit: cover;
  border-radius: 8px;
}

.goods-row h3 {
  margin: 0 0 8px;
}

.goods-row p,
.goods-row span {
  color: #666;
}

.goods-row strong,
.summary-card strong {
  color: #ff4d00;
}

.summary-card {
  align-self: start;
  position: sticky;
  top: 116px;
  display: grid;
  gap: 16px;
}

.summary-card div {
  display: flex;
  justify-content: space-between;
}

.total-row {
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  font-size: 18px;
}

.submit-button {
  --el-button-bg-color: #ff4d00;
  --el-button-border-color: #ff4d00;
  --el-button-hover-bg-color: #ff6a22;
  --el-button-hover-border-color: #ff6a22;
  font-weight: 800;
}

@media (max-width: 900px) {
  .confirm-grid {
    grid-template-columns: 1fr;
  }

  .summary-card {
    position: static;
  }
}
</style>
