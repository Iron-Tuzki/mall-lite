<script setup lang="ts">
import { Clock, CreditCard, House } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  callbackPayment,
  getOrderDetail,
  payOrder,
  type MockPaymentResult,
  type OrderDetail,
  type PaymentPayResult
} from '@/api/order';
import SiteHeader from '@/components/SiteHeader.vue';

const ORDER_TIMEOUT_MINUTES = 30;

const route = useRoute();
const router = useRouter();
const orderId = computed(() => Number(route.params.orderId));
const order = ref<OrderDetail | null>(null);
const payment = ref<PaymentPayResult | null>(null);
const loading = ref(false);
const paying = ref(false);
const callbackSubmitting = ref(false);
const now = ref(Date.now());
let timer: number | undefined;

const expireAt = computed(() => {
  if (!order.value?.createTime) {
    return null;
  }
  return new Date(order.value.createTime).getTime() + ORDER_TIMEOUT_MINUTES * 60 * 1000;
});

const remainingSeconds = computed(() => {
  if (!expireAt.value) {
    return 0;
  }
  return Math.max(0, Math.floor((expireAt.value - now.value) / 1000));
});

const countdownText = computed(() => {
  const minutes = Math.floor(remainingSeconds.value / 60);
  const seconds = remainingSeconds.value % 60;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
});

const statusText = computed(() => {
  if (!order.value) {
    return '订单处理中';
  }
  if (order.value.status === 10) {
    return '待支付';
  }
  if (order.value.status === 20) {
    return '已支付';
  }
  if (order.value.status === 30) {
    return '已取消';
  }
  if (order.value.status === 40) {
    return '已完成';
  }
  return '未知状态';
});

const isPendingPayment = computed(() => order.value?.status === 10);

const resultView = computed(() => {
  if (!order.value) {
    return {
      badge: '订单处理中',
      title: '正在加载订单',
      description: '正在获取订单最新状态，请稍候。',
      countdownLabel: '剩余支付时间'
    };
  }
  if (order.value.status === 10 && remainingSeconds.value > 0) {
    return {
      badge: '下单成功',
      title: '订单已创建，等待支付',
      description: '请在倒计时结束前完成支付，超时后系统会自动取消订单并释放库存。',
      countdownLabel: '剩余支付时间'
    };
  }
  if (order.value.status === 10) {
    return {
      badge: '支付超时',
      title: '支付时间已结束',
      description: '订单仍在等待系统关闭，稍后会自动取消并释放库存。',
      countdownLabel: '支付时间已结束'
    };
  }
  if (order.value.status === 20) {
    return {
      badge: '支付成功',
      title: '订单已支付，等待发货',
      description: '支付已经完成，商家会尽快处理发货。',
      countdownLabel: '支付已完成'
    };
  }
  if (order.value.status === 30) {
    return {
      badge: '订单已取消',
      title: '订单已取消',
      description: '该订单已经关闭，如已锁定库存，系统会完成释放。',
      countdownLabel: '订单已关闭'
    };
  }
  if (order.value.status === 40) {
    return {
      badge: '订单完成',
      title: '订单已完成',
      description: '订单已经完成，感谢你的购买。',
      countdownLabel: '订单已完成'
    };
  }
  return {
    badge: '订单状态未知',
    title: '订单状态待确认',
    description: '暂时无法识别订单状态，请稍后刷新查看。',
    countdownLabel: '订单状态'
  };
});

onMounted(async () => {
  await loadOrder();
  timer = window.setInterval(() => {
    now.value = Date.now();
  }, 1000);
});

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer);
  }
});

async function loadOrder() {
  if (!orderId.value) {
    await router.replace('/');
    return;
  }
  loading.value = true;
  try {
    const response = await getOrderDetail(orderId.value);
    if (!response.data.success) {
      throw new Error(response.data.message || '订单加载失败');
    }
    order.value = response.data.data;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '订单加载失败');
  } finally {
    loading.value = false;
  }
}

async function handlePay() {
  if (!order.value) {
    return;
  }
  paying.value = true;
  try {
    const response = await payOrder(order.value.orderId);
    if (!response.data.success) {
      throw new Error(response.data.message || '发起支付失败');
    }
    payment.value = response.data.data;
    ElMessage.success('已发起模拟支付');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发起支付失败');
  } finally {
    paying.value = false;
  }
}

async function handleMockCallback(mockResult: MockPaymentResult) {
  if (!payment.value?.paymentNo) {
    ElMessage.warning('请先发起支付');
    return;
  }
  callbackSubmitting.value = true;
  try {
    const response = await callbackPayment(payment.value.paymentNo, mockResult);
    if (!response.data.success) {
      throw new Error(response.data.message || '模拟支付回调失败');
    }
    payment.value = response.data.data;
    await loadOrder();
    ElMessage.success(mockResult === 'SUCCESS' ? '模拟支付成功' : '模拟支付失败');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '模拟支付回调失败');
  } finally {
    callbackSubmitting.value = false;
  }
}
</script>

<template>
  <SiteHeader />

  <main v-loading="loading" class="result-page page-shell">
    <section class="result-card">
      <div class="status-badge">{{ resultView.badge }}</div>
      <h1>{{ resultView.title }}</h1>
      <p>{{ resultView.description }}</p>

      <div v-if="isPendingPayment" class="countdown-box" :class="{ expired: remainingSeconds === 0 }">
        <el-icon><Clock /></el-icon>
        <span>{{ resultView.countdownLabel }}</span>
        <strong>{{ countdownText }}</strong>
      </div>

      <div class="order-meta">
        <div>
          <span>订单号</span>
          <strong>{{ order?.orderNo }}</strong>
        </div>
        <div>
          <span>订单状态</span>
          <strong>{{ statusText }}</strong>
        </div>
        <div>
          <span>应付金额</span>
          <strong class="price">￥{{ order?.payAmount || 0 }}</strong>
        </div>
        <div>
          <span>收货人</span>
          <strong>{{ order?.receiverName }} {{ order?.receiverPhone }}</strong>
        </div>
      </div>

      <div class="action-row">
        <el-button :icon="House" size="large" @click="router.push('/')">返回首页</el-button>
        <el-button size="large" @click="router.push('/profile')">我的主页</el-button>
        <el-button
          v-if="isPendingPayment"
          class="pay-button"
          :disabled="remainingSeconds === 0"
          :icon="CreditCard"
          :loading="paying"
          size="large"
          type="primary"
          @click="handlePay"
        >
          去支付
        </el-button>
      </div>

      <el-alert
        v-if="payment"
        :closable="false"
        show-icon
        title="模拟支付已发起"
        type="success"
      >
        <template #default>
          支付流水号：{{ payment.paymentNo }}。可以点击下面按钮模拟第三方支付回调。
        </template>
      </el-alert>

      <div v-if="payment && order?.status === 10" class="callback-row">
        <el-button :loading="callbackSubmitting" type="success" @click="handleMockCallback('SUCCESS')">
          模拟支付成功回调
        </el-button>
        <el-button :loading="callbackSubmitting" type="danger" @click="handleMockCallback('FAILED')">
          模拟支付失败回调
        </el-button>
      </div>
    </section>
  </main>
</template>

<style scoped>
.result-page {
  display: grid;
  min-height: calc(100vh - 92px);
  place-items: center;
  padding: 42px 0;
}

.result-card {
  width: min(760px, 100%);
  padding: 36px;
  background: #fff;
  border-radius: 8px;
  text-align: center;
}

.status-badge {
  display: inline-flex;
  padding: 6px 14px;
  color: #ff4d00;
  background: #fff3ec;
  border-radius: 999px;
  font-weight: 900;
}

.result-card h1 {
  margin: 18px 0 8px;
  font-size: 34px;
}

.result-card p {
  margin: 0;
  color: #666;
}

.countdown-box {
  display: inline-grid;
  grid-template-columns: auto auto auto;
  gap: 10px;
  align-items: center;
  margin: 28px 0;
  padding: 14px 20px;
  color: #ff4d00;
  background: #fff7f2;
  border-radius: 8px;
}

.countdown-box strong {
  font-size: 26px;
}

.countdown-box.expired {
  color: #909399;
  background: #f4f4f5;
}

.order-meta {
  display: grid;
  gap: 12px;
  margin: 0 auto 28px;
  text-align: left;
}

.order-meta div {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 12px 0;
  border-bottom: 1px solid #f1f1f1;
}

.order-meta span {
  color: #777;
}

.order-meta strong {
  min-width: 0;
  text-align: right;
  word-break: break-all;
}

.price {
  color: #ff4d00;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin-bottom: 18px;
}

.callback-row {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}

.pay-button {
  --el-button-bg-color: #ff4d00;
  --el-button-border-color: #ff4d00;
  --el-button-hover-bg-color: #ff6a22;
  --el-button-hover-border-color: #ff6a22;
  font-weight: 800;
}

@media (max-width: 620px) {
  .result-card {
    padding: 24px;
  }

  .action-row {
    flex-direction: column;
  }

  .order-meta div {
    display: grid;
    gap: 8px;
  }

  .order-meta strong {
    text-align: left;
  }

  .callback-row {
    flex-direction: column;
  }
}
</style>
