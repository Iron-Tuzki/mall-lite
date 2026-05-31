<script setup lang="ts">
import { ArrowLeft, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { getOrderDetail, listOrders, type OrderDetail, type OrderMain } from '@/api/order';
import SiteHeader from '@/components/SiteHeader.vue';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const orders = ref<OrderMain[]>([]);
const orderDetails = ref<Record<number, OrderDetail>>({});
const filters = reactive<{
  status: number | null;
  dateRange: [string, string] | [];
}>({
  status: null,
  dateRange: []
});

const statusOptions = [
  { label: '全部状态', value: null },
  { label: '待支付', value: 10 },
  { label: '待发货', value: 20 },
  { label: '待收货', value: 25 },
  { label: '已取消', value: 30 },
  { label: '已完成', value: 40 }
];

const resultText = computed(() => `共找到 ${orders.value.length} 条订单`);

onMounted(async () => {
  const routeStatus = Number(route.query.status);
  filters.status = Number.isFinite(routeStatus) && route.query.status !== undefined ? routeStatus : null;
  await loadOrders();
});

async function loadOrders() {
  loading.value = true;
  try {
    const response = await listOrders({
      status: filters.status ?? undefined,
      startTime: filters.dateRange[0] ? `${filters.dateRange[0]}T00:00:00` : undefined,
      endTime: filters.dateRange[1] ? `${filters.dateRange[1]}T23:59:59` : undefined
    });
    if (!response.data.success) {
      throw new Error(response.data.message || '订单列表加载失败');
    }
    orders.value = response.data.data;
    await loadOrderDetails();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '订单列表加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadOrderDetails() {
  const details = await Promise.all(
    orders.value.map(async (order) => {
      const response = await getOrderDetail(order.orderId);
      return response.data.success ? response.data.data : null;
    })
  );
  orderDetails.value = details.reduce<Record<number, OrderDetail>>((result, detail) => {
    if (detail) {
      result[detail.orderId] = detail;
    }
    return result;
  }, {});
}

async function handleSearch() {
  await router.replace({
    path: '/orders',
    query: filters.status === null ? {} : { status: String(filters.status) }
  });
  await loadOrders();
}

async function handleReset() {
  filters.status = null;
  filters.dateRange = [];
  await router.replace('/orders');
  await loadOrders();
}

function getOrderTitle(order: OrderMain) {
  const detail = orderDetails.value[order.orderId];
  const firstItem = detail?.items?.[0];
  if (!firstItem) {
    return `订单 ${order.orderNo}`;
  }
  const itemCount = detail.items?.length || 1;
  return itemCount > 1 ? `${firstItem.productName} 等 ${itemCount} 件商品` : firstItem.productName;
}

function getOrderImage(order: OrderMain) {
  return orderDetails.value[order.orderId]?.items?.[0]?.mainImageUrl
    || 'https://images.unsplash.com/photo-1557821552-17105176677c?auto=format&fit=crop&w=240&q=80';
}

function formatCreateTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}

function getStatusText(status: number) {
  return statusOptions.find((item) => item.value === status)?.label || '未知状态';
}

function getStatusType(status: number) {
  if (status === 10) return 'warning';
  if (status === 20) return 'success';
  if (status === 30) return 'info';
  if (status === 40) return 'primary';
  return 'info';
}
</script>

<template>
  <SiteHeader />

  <main class="orders-page">
    <section class="page-shell page-heading">
      <el-button :icon="ArrowLeft" text @click="router.push('/profile')">返回个人主页</el-button>
      <div>
        <h1>我的订单</h1>
        <p>按订单状态和下单时间查找订单，处理待支付订单或查看历史记录。</p>
      </div>
    </section>

    <section class="page-shell filter-panel">
      <div class="filter-grid">
        <label>
          <span>订单状态</span>
          <el-select v-model="filters.status" placeholder="全部状态">
            <el-option v-for="option in statusOptions" :key="String(option.value)" :label="option.label" :value="option.value" />
          </el-select>
        </label>
        <label>
          <span>下单时间</span>
          <el-date-picker
            v-model="filters.dateRange"
            end-placeholder="结束日期"
            range-separator="至"
            start-placeholder="开始日期"
            type="daterange"
            value-format="YYYY-MM-DD"
          />
        </label>
        <div class="filter-actions">
          <el-button :icon="Search" type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>
    </section>

    <section v-loading="loading" class="page-shell order-panel">
      <div class="section-head">
        <h2>订单列表</h2>
        <span>{{ resultText }}</span>
      </div>

      <el-empty v-if="!loading && orders.length === 0" description="没有符合条件的订单" />

      <div v-else class="order-list">
        <article v-for="order in orders" :key="order.orderId" class="order-card">
          <div class="order-card-head">
            <div>
              <strong>订单号 {{ order.orderNo }}</strong>
              <span>{{ formatCreateTime(order.createTime) }}</span>
            </div>
            <el-tag :type="getStatusType(order.status)">{{ getStatusText(order.status) }}</el-tag>
          </div>
          <div class="order-card-body">
            <img :alt="getOrderTitle(order)" :src="getOrderImage(order)" />
            <div>
              <h3>{{ getOrderTitle(order) }}</h3>
              <p>订单总额 <strong>￥{{ order.payAmount }}</strong></p>
            </div>
            <el-button type="primary" plain @click="router.push(`/order/result/${order.orderId}`)">查看订单</el-button>
          </div>
        </article>
      </div>
    </section>
  </main>
</template>

<style scoped>
.orders-page {
  padding: 28px 0 56px;
}

.page-heading {
  display: grid;
  gap: 16px;
  margin-bottom: 18px;
}

.page-heading h1 {
  margin: 0;
  font-size: 32px;
}

.page-heading p {
  margin: 8px 0 0;
  color: #777;
}

.filter-panel,
.order-panel {
  padding: 22px;
  background: #fff;
  border-radius: 8px;
}

.filter-panel {
  margin-bottom: 18px;
}

.filter-grid {
  display: grid;
  grid-template-columns: 220px minmax(320px, 1fr) auto;
  gap: 16px;
  align-items: end;
}

.filter-grid label {
  display: grid;
  gap: 8px;
}

.filter-grid label > span {
  color: #555;
  font-size: 14px;
  font-weight: 800;
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.section-head,
.order-card-head,
.order-card-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.section-head {
  margin-bottom: 16px;
}

.section-head h2 {
  margin: 0;
}

.section-head span,
.order-card-head span {
  color: #888;
  font-size: 13px;
}

.order-list {
  display: grid;
  gap: 14px;
}

.order-card {
  overflow: hidden;
  border: 1px solid #eceef2;
  border-radius: 8px;
}

.order-card-head {
  padding: 12px 16px;
  background: #f7f8fb;
}

.order-card-head div {
  display: grid;
  gap: 6px;
}

.order-card-body {
  padding: 16px;
}

.order-card-body img {
  width: 88px;
  height: 88px;
  border-radius: 8px;
  object-fit: cover;
}

.order-card-body div {
  min-width: 0;
  flex: 1;
}

.order-card-body h3 {
  margin: 0 0 10px;
  overflow: hidden;
  font-size: 17px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-card-body p {
  margin: 0;
  color: #777;
}

.order-card-body strong {
  color: #ff4d00;
}

@media (max-width: 760px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .filter-grid :deep(.el-date-editor) {
    width: 100%;
  }

  .order-card-body {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .order-card-body .el-button {
    margin-left: 104px;
  }
}
</style>
