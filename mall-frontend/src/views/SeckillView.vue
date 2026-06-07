<script setup lang="ts">
import { Lightning, RefreshRight, ShoppingCart, Timer } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { listAddresses } from '@/api/address';
import {
  createSeckillOrder,
  listActiveSeckillActivities,
  type SeckillActivity,
  type SeckillSku
} from '@/api/seckill';
import SiteHeader from '@/components/SiteHeader.vue';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const activities = ref<SeckillActivity[]>([]);
const loading = ref(false);
const submittingSkuId = ref<number | null>(null);
const quantities = reactive<Record<number, number>>({});

const hasActivities = computed(() => activities.value.some((activity) => activity.skus.length > 0));

onMounted(() => {
  loadActivities();
});

async function loadActivities() {
  loading.value = true;
  try {
    const response = await listActiveSeckillActivities();
    if (!response.data.success) {
      throw new Error(response.data.message || '秒杀活动加载失败');
    }
    activities.value = response.data.data;
    for (const activity of activities.value) {
      for (const sku of activity.skus) {
        quantities[sku.id] = quantities[sku.id] || 1;
      }
    }
  } catch (error) {
    activities.value = [];
    ElMessage.error(error instanceof Error ? error.message : '秒杀活动加载失败');
  } finally {
    loading.value = false;
  }
}

async function submitSeckillOrder(sku: SeckillSku) {
  if (!authStore.isLoggedIn) {
    await router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  submittingSkuId.value = sku.id;
  try {
    const user = authStore.user || await authStore.fetchCurrentUser();
    if (!user) {
      await router.push({ path: '/login', query: { redirect: route.fullPath } });
      return;
    }
    const addressResponse = await listAddresses(user.id);
    const addresses = addressResponse.data.data;
    const address = addresses.find((item) => item.defaultFlag === 1) || addresses[0];
    if (!address) {
      ElMessage.warning('请先新增收货地址');
      await router.push('/addresses');
      return;
    }
    const response = await createSeckillOrder({
      seckillSkuId: sku.id,
      requestId: buildRequestId(sku.id),
      addressId: address.id,
      quantity: quantities[sku.id] || 1,
      remark: '前台秒杀下单'
    });
    if (!response.data.success) {
      throw new Error(response.data.message || '秒杀下单失败');
    }
    ElMessage.success('秒杀订单创建成功');
    await router.replace(`/order/result/${response.data.data.orderId}`);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '秒杀下单失败');
  } finally {
    submittingSkuId.value = null;
  }
}

function buildRequestId(seckillSkuId: number) {
  return `front-${seckillSkuId}-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
}

function formatTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '';
}

function activityTimeText(activity: SeckillActivity) {
  return `${formatTime(activity.startTime)} - ${formatTime(activity.endTime)}`;
}
</script>

<template>
  <SiteHeader />

  <main class="seckill-page">
    <section class="page-shell page-head">
      <div>
        <span class="eyebrow">
          <el-icon><Lightning /></el-icon>
          限时秒杀
        </span>
        <h1>正在开抢的好价商品</h1>
        <p>库存已提前写入 Redis，下单时会校验活动时间、限购、幂等请求和真实库存。</p>
      </div>
      <el-button :icon="RefreshRight" :loading="loading" round @click="loadActivities">刷新</el-button>
    </section>

    <section v-loading="loading" class="page-shell activity-list">
      <el-empty v-if="!loading && !hasActivities" description="暂无可参与的秒杀活动" />

      <article v-for="activity in activities" :key="activity.id" class="activity-section">
        <header class="activity-head">
          <div>
            <h2>{{ activity.name }}</h2>
            <span>
              <el-icon><Timer /></el-icon>
              {{ activityTimeText(activity) }}
            </span>
          </div>
          <el-tag type="danger" effect="dark">进行中</el-tag>
        </header>

        <div class="sku-grid">
          <article v-for="sku in activity.skus" :key="sku.id" class="sku-card">
            <img :alt="sku.productName" :src="sku.mainImageUrl" />
            <div class="sku-info">
              <h3>{{ sku.productName }}</h3>
              <p>{{ sku.skuName }}</p>
              <div class="price-row">
                <span>秒杀价</span>
                <strong>￥{{ sku.seckillPrice }}</strong>
              </div>
              <div class="meta-row">
                <span>活动库存 {{ sku.stockCount }}</span>
                <span>限购 {{ sku.limitQuantity }}</span>
              </div>
              <div class="action-row">
                <el-input-number
                  v-model="quantities[sku.id]"
                  :min="1"
                  :max="sku.limitQuantity"
                  size="small"
                />
                <el-button
                  class="seckill-button"
                  :icon="ShoppingCart"
                  :loading="submittingSkuId === sku.id"
                  type="primary"
                  @click="submitSeckillOrder(sku)"
                >
                  立即秒杀
                </el-button>
              </div>
            </div>
          </article>
        </div>
      </article>
    </section>
  </main>
</template>

<style scoped>
.seckill-page {
  min-height: 100vh;
  padding-bottom: 56px;
  background: #f6f7fb;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 30px 0 20px;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #d6001c;
  font-size: 14px;
  font-weight: 900;
}

.page-head h1 {
  margin: 8px 0;
  font-size: 34px;
  line-height: 1.2;
}

.page-head p {
  max-width: 620px;
  margin: 0;
  color: #5f6368;
  line-height: 1.7;
}

.activity-list {
  display: grid;
  min-height: 360px;
  gap: 18px;
}

.activity-section {
  padding: 22px;
  background: #fff;
  border-radius: 8px;
}

.activity-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.activity-head h2 {
  margin: 0 0 8px;
  font-size: 22px;
}

.activity-head span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #5f6368;
  font-size: 14px;
}

.sku-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.sku-card {
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #eef0f4;
  border-radius: 8px;
}

.sku-card img {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 0.72;
  object-fit: cover;
  background: #eef0f4;
}

.sku-info {
  display: grid;
  gap: 10px;
  padding: 14px;
}

.sku-info h3 {
  margin: 0;
  overflow: hidden;
  color: #202124;
  font-size: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sku-info p {
  min-height: 20px;
  margin: 0;
  overflow: hidden;
  color: #5f6368;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price-row,
.meta-row,
.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.price-row span,
.meta-row {
  color: #6b7280;
  font-size: 13px;
}

.price-row strong {
  color: #d6001c;
  font-size: 24px;
  line-height: 1;
}

.meta-row span {
  min-width: 0;
  padding: 4px 8px;
  color: #24545f;
  background: #e8f7f5;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
}

.action-row {
  align-items: stretch;
}

.action-row :deep(.el-input-number) {
  width: 108px;
}

.seckill-button {
  --el-button-bg-color: #d6001c;
  --el-button-border-color: #d6001c;
  --el-button-hover-bg-color: #f04438;
  --el-button-hover-border-color: #f04438;
  flex: 1;
  min-width: 112px;
  font-weight: 800;
}

@media (max-width: 1180px) {
  .sku-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .page-head {
    align-items: stretch;
    flex-direction: column;
  }

  .sku-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .sku-grid {
    grid-template-columns: 1fr;
  }

  .action-row {
    flex-direction: column;
  }

  .action-row :deep(.el-input-number) {
    width: 100%;
  }
}
</style>
