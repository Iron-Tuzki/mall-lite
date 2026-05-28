<script setup lang="ts">
import { ArrowRight, Location, Lock, Star, SwitchButton } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { getOrderDetail, listOrders, type OrderDetail, type OrderMain } from '@/api/order';
import { getSignInProfile, signInToday, type SignInProfile } from '@/api/user';
import SiteHeader from '@/components/SiteHeader.vue';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const signSubmitting = ref(false);
const orders = ref<OrderMain[]>([]);
const orderDetails = ref<Record<number, OrderDetail>>({});
const signInProfile = ref<SignInProfile | null>(null);

const orderEntries = computed(() => [
  { label: '待支付', count: countByStatus(10) },
  { label: '待发货', count: countByStatus(20) },
  { label: '待收货', count: 0 },
  { label: '已完成', count: countByStatus(40) },
  { label: '已取消', count: countByStatus(30) }
]);

const recentOrders = computed(() => orders.value.slice(0, 5));
const today = new Date();
const currentMonthLabel = computed(() => {
  const year = signInProfile.value?.year || today.getFullYear();
  const month = signInProfile.value?.month || today.getMonth() + 1;
  return `${year}年${month}月`;
});
const signCells = computed(() => {
  const days = signInProfile.value?.daysInMonth || new Date(today.getFullYear(), today.getMonth() + 1, 0).getDate();
  const signedDays = signInProfile.value?.signedDays || [];
  return Array.from({ length: days }, (_, index) => {
    const day = index + 1;
    const signed = signedDays.includes(day);
    return {
      day,
      signed,
      level: signed ? ((day % 4) + 1) : 0
    };
  });
});
const signedCount = computed(() => signInProfile.value?.monthSignedCount || 0);
const continuousSignDays = computed(() => signInProfile.value?.continuousSignedDays || 0);
const todaySigned = computed(() => Boolean(signInProfile.value?.todaySigned));
const browsingFootprints = [
  {
    id: 910037,
    title: '北欧陶瓷马克杯',
    price: 49.8,
    imageUrl: 'https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?auto=format&fit=crop&w=300&q=80'
  },
  {
    id: 910024,
    title: '桌面机械键盘',
    price: 159,
    imageUrl: 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=300&q=80'
  },
  {
    id: 910018,
    title: '智能床头小夜灯',
    price: 98,
    imageUrl: 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=300&q=80'
  }
];
const favoriteProducts = [
  {
    id: 910021,
    title: '原木托盘茶点盘',
    price: 33,
    imageUrl: 'https://images.unsplash.com/photo-1604014237800-1c9102c219da?auto=format&fit=crop&w=300&q=80'
  },
  {
    id: 910032,
    title: '家用不锈钢汤锅',
    price: 118,
    imageUrl: 'https://images.unsplash.com/photo-1584990347449-a034611cc2b6?auto=format&fit=crop&w=300&q=80'
  },
  {
    id: 910035,
    title: '日式藤编收纳篮',
    price: 68,
    imageUrl: 'https://images.unsplash.com/photo-1596079890701-dd42edf0b7d4?auto=format&fit=crop&w=300&q=80'
  }
];

onMounted(async () => {
  loading.value = true;
  try {
    if (!authStore.user) {
      await authStore.fetchCurrentUser();
    }
    await Promise.all([loadOrders(), loadSignInProfile()]);
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '请重新登录');
    await router.replace({ path: '/login', query: { redirect: '/profile' } });
  } finally {
    loading.value = false;
  }
});

async function loadOrders() {
  if (!authStore.user?.id) {
    return;
  }
  const response = await listOrders(authStore.user.id);
  if (!response.data.success) {
    throw new Error(response.data.message || '订单列表加载失败');
  }
  orders.value = response.data.data;
  await loadRecentOrderDetails();
}

async function loadSignInProfile() {
  const response = await getSignInProfile();
  if (!response.data.success) {
    throw new Error(response.data.message || '签到记录加载失败');
  }
  signInProfile.value = response.data.data;
}

async function loadRecentOrderDetails() {
  const recent = orders.value.slice(0, 5);
  const details = await Promise.all(
    recent.map(async (order) => {
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

function countByStatus(status: number) {
  return orders.value.filter((order) => order.status === status).length;
}

function getOrderStatusText(status: number) {
  if (status === 10) {
    return '待支付';
  }
  if (status === 20) {
    return '待发货';
  }
  if (status === 30) {
    return '已取消';
  }
  if (status === 40) {
    return '已完成';
  }
  return '未知状态';
}

function getOrderStatusType(status: number) {
  if (status === 10) {
    return 'warning';
  }
  if (status === 20) {
    return 'success';
  }
  if (status === 30) {
    return 'info';
  }
  return 'primary';
}

function formatCreateTime(value: string) {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 16);
}

function getRecentOrderTitle(order: OrderMain) {
  const detail = orderDetails.value[order.orderId];
  const firstItem = detail?.items?.[0];
  if (!firstItem) {
    return `订单 ${order.orderNo}`;
  }
  const extraCount = Math.max((detail.items?.length || 1) - 1, 0);
  return extraCount > 0 ? `${firstItem.productName} 等 ${detail.items.length} 件商品` : firstItem.productName;
}

function getRecentOrderImage(order: OrderMain) {
  return orderDetails.value[order.orderId]?.items?.[0]?.mainImageUrl || 'https://images.unsplash.com/photo-1557821552-17105176677c?auto=format&fit=crop&w=240&q=80';
}

async function handleSignIn() {
  signSubmitting.value = true;
  try {
    const response = await signInToday();
    if (!response.data.success) {
      throw new Error(response.data.message || '签到失败');
    }
    signInProfile.value = response.data.data;
    ElMessage.success(response.data.data.todaySigned ? '签到成功，今日已点亮' : '签到成功');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '签到失败');
  } finally {
    signSubmitting.value = false;
  }
}

async function handleLogout() {
  await authStore.logout();
  ElMessage.success('已退出登录');
  await router.replace('/');
}
</script>

<template>
  <SiteHeader />

  <main v-loading="loading" class="profile-page page-shell">
    <section class="profile-hero">
      <img
        :alt="authStore.displayName"
        class="avatar"
        :src="authStore.user?.avatarUrl || 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=180&q=80'"
      />
      <div class="profile-info">
        <span>普通会员</span>
        <h1>{{ authStore.displayName }}</h1>
        <p>{{ authStore.user?.username }} · {{ authStore.maskedPhone }}</p>
      </div>
      <dl class="hero-account">
        <div>
          <dt>用户 ID</dt>
          <dd>{{ authStore.user?.id }}</dd>
        </div>
        <div>
          <dt>邮箱</dt>
          <dd>{{ authStore.user?.email || '未绑定邮箱' }}</dd>
        </div>
        <div>
          <dt>手机号</dt>
          <dd>{{ authStore.maskedPhone }}</dd>
        </div>
      </dl>
      <div class="hero-actions">
        <button type="button" @click="router.push('/addresses')">
          <el-icon><Location /></el-icon>
          <span>收货地址</span>
        </button>
        <button type="button">
          <el-icon><Lock /></el-icon>
          <span>账号安全</span>
        </button>
        <button type="button" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          <span>退出登录</span>
        </button>
      </div>
    </section>

    <section class="content-grid">
      <div class="main-column">
        <div class="profile-section order-section">
          <div class="section-head">
            <div>
              <h2>我的订单</h2>
              <p>按状态快速查看订单进度</p>
            </div>
            <button class="plain-link" type="button">全部订单 <el-icon><ArrowRight /></el-icon></button>
          </div>
          <div class="order-status-grid">
            <button v-for="entry in orderEntries" :key="entry.label" type="button">
              <strong>{{ entry.count }}</strong>
              <span>{{ entry.label }}</span>
            </button>
          </div>
        </div>

        <div class="profile-section product-activity-section">
          <div class="activity-panel">
            <div class="section-head">
              <div>
                <h2>浏览足迹</h2>
                <p>后续可用 Redis ZSet 按访问时间排序</p>
              </div>
              <button class="plain-link" type="button">全部足迹 <el-icon><ArrowRight /></el-icon></button>
            </div>
            <div class="activity-list">
              <article v-for="product in browsingFootprints" :key="product.id" class="activity-card">
                <img :alt="product.title" :src="product.imageUrl" />
                <div>
                  <strong>{{ product.title }}</strong>
                  <span>刚刚看过</span>
                </div>
                <em>￥{{ product.price }}</em>
              </article>
            </div>
          </div>

          <div class="activity-panel">
            <div class="section-head">
              <div>
                <h2>商品收藏</h2>
                <p>后续可用收藏表或 Redis Set 做快速判断</p>
              </div>
              <button class="plain-link" type="button">全部收藏 <el-icon><ArrowRight /></el-icon></button>
            </div>
            <div class="activity-list">
              <article v-for="product in favoriteProducts" :key="product.id" class="activity-card">
                <img :alt="product.title" :src="product.imageUrl" />
                <div>
                  <strong>{{ product.title }}</strong>
                  <span><el-icon><Star /></el-icon> 已收藏</span>
                </div>
                <em>￥{{ product.price }}</em>
              </article>
            </div>
          </div>
        </div>
      </div>

      <aside class="side-column">
        <div class="profile-section sign-section">
          <div class="section-head">
            <div>
              <h2>本月签到记录</h2>
            </div>
            <el-button round :disabled="todaySigned" :loading="signSubmitting" type="primary" @click="handleSignIn">
              {{ todaySigned ? '今日已签到' : '今日签到' }}
            </el-button>
          </div>
          <div class="sign-summary">
            <div>
              <strong>{{ signedCount }}</strong>
              <span>{{ currentMonthLabel }}已签到</span>
            </div>
            <div>
              <strong>{{ continuousSignDays }}</strong>
              <span>连续签到</span>
            </div>
            <div>
              <strong>{{ signCells.length - signedCount }}</strong>
              <span>本月待点亮</span>
            </div>
          </div>
          <div class="sign-heatmap" aria-label="本月签到热力图">
            <span
              v-for="cell in signCells"
              :key="cell.day"
              class="sign-cell"
              :class="`level-${cell.level}`"
              :title="`${currentMonthLabel}${cell.day}日：${cell.signed ? '已签到' : '未签到'}`"
            />
          </div>
        </div>

        <div class="profile-section recent-order-section">
          <div class="section-head">
            <h2>最近订单</h2>
            <span class="order-total">共 {{ orders.length }} 单</span>
          </div>

          <el-empty v-if="recentOrders.length === 0" description="暂无订单" />

          <div v-else class="recent-order-list">
            <article v-for="order in recentOrders" :key="order.orderId" class="recent-order-card">
              <img :alt="getRecentOrderTitle(order)" :src="getRecentOrderImage(order)" />
              <div class="recent-order-main">
                <strong class="recent-order-title">{{ getRecentOrderTitle(order) }}</strong>
                <span class="recent-order-time">{{ formatCreateTime(order.createTime) }}</span>
                <div class="recent-order-meta">
                  <el-tag :type="getOrderStatusType(order.status)">
                    {{ getOrderStatusText(order.status) }}
                  </el-tag>
                  <strong class="recent-order-price">￥{{ order.payAmount }}</strong>
                </div>
              </div>
              <el-button text type="primary" @click="router.push(`/order/result/${order.orderId}`)">
                查看
              </el-button>
            </article>
          </div>
        </div>

      </aside>
    </section>
  </main>
</template>

<style scoped>
.profile-page {
  padding: 28px 0 56px;
}

.profile-hero {
  display: grid;
  grid-template-columns: 88px minmax(180px, 1fr) minmax(240px, auto) auto;
  align-items: center;
  gap: 22px;
  padding: 30px;
  color: #fff;
  background: linear-gradient(135deg, #ff4d00, #ff8a34);
  border-radius: 8px;
}

.avatar {
  width: 88px;
  height: 88px;
  border: 3px solid rgba(255, 255, 255, 0.72);
  border-radius: 50%;
  object-fit: cover;
}

.profile-info span {
  display: inline-block;
  padding: 4px 10px;
  color: #ff4d00;
  background: #fff;
  border-radius: 999px;
  font-weight: 800;
}

.profile-info h1 {
  margin: 12px 0 4px;
  font-size: 34px;
}

.profile-info p {
  margin: 0;
  opacity: 0.92;
}

.hero-account {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, auto));
  gap: 16px;
  margin: 0;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 8px;
}

.hero-account div {
  display: grid;
  gap: 6px;
}

.hero-account dt {
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
}

.hero-account dd {
  overflow: hidden;
  margin: 0;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-actions {
  display: grid;
  grid-template-columns: repeat(3, auto);
  gap: 10px;
}

.hero-actions button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  color: #fff;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  cursor: pointer;
  font-weight: 800;
}

.hero-actions button:hover {
  background: rgba(255, 255, 255, 0.24);
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
  margin-top: 18px;
}

.main-column,
.side-column {
  display: grid;
  align-content: start;
  gap: 18px;
}

.profile-section {
  padding: 22px;
  background: #fff;
  border-radius: 8px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.section-head h2 {
  margin: 0;
  font-size: 20px;
}

.section-head p {
  margin: 6px 0 0;
  color: #888;
  font-size: 13px;
}

.plain-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 0;
  color: #777;
  background: transparent;
  border: 0;
  cursor: pointer;
  font-size: 14px;
}

.order-status-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.order-status-grid button {
  border: 0;
  background: #f7f8fb;
  border-radius: 8px;
  cursor: pointer;
}

.order-status-grid button {
  display: grid;
  gap: 6px;
  padding: 18px 10px;
}

.order-status-grid strong {
  color: #ff4d00;
  font-size: 24px;
}

.recent-order-list {
  display: grid;
  gap: 10px;
}

.recent-order-card {
  display: grid;
  grid-template-columns: 62px minmax(0, 1fr) 44px;
  gap: 12px;
  align-items: center;
  padding: 14px;
  background: #f7f8fb;
  border-radius: 8px;
}

.recent-order-card > img {
  width: 62px;
  height: 62px;
  border-radius: 8px;
  object-fit: cover;
}

.recent-order-main {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.recent-order-title {
  overflow: hidden;
  color: #202124;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-order-no,
.recent-order-time,
.order-total {
  color: #888;
  font-size: 13px;
}

.recent-order-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.recent-order-price {
  color: #ff4d00;
  white-space: nowrap;
}

.sign-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.sign-summary div {
  display: grid;
  gap: 4px;
  padding: 16px;
  background: #f7f8fb;
  border-radius: 8px;
}

.sign-summary strong {
  color: #ff4d00;
  font-size: 26px;
}

.sign-summary span {
  color: #777;
  font-size: 13px;
}

.sign-heatmap {
  display: grid;
  grid-template-columns: repeat(16, 14px);
  grid-auto-rows: 14px;
  gap: 7px;
  padding: 18px;
  background: #f7f8fb;
  border-radius: 8px;
}

.side-column .sign-heatmap {
  grid-template-columns: repeat(12, 14px);
}

.sign-cell {
  display: inline-block;
  width: 14px;
  height: 14px;
  background: #ebedf0;
  border-radius: 4px;
}

.level-1 {
  background: #ffd8c8;
}

.level-2 {
  background: #ffad85;
}

.level-3 {
  background: #ff7a3d;
}

.level-4 {
  background: #ff4d00;
}

.product-activity-section {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.activity-panel {
  min-width: 0;
}

.activity-list {
  display: grid;
  gap: 12px;
}

.activity-card {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
  background: #f7f8fb;
  border-radius: 8px;
}

.activity-card img {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  object-fit: cover;
}

.activity-card div {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.activity-card strong {
  overflow: hidden;
  color: #202124;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-card span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #888;
  font-size: 13px;
}

.activity-card em {
  color: #ff4d00;
  font-style: normal;
  font-weight: 900;
}

.activity-card .el-icon {
  color: #ff4d00;
}

@media (max-width: 900px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .order-status-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .product-activity-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .profile-hero {
    align-items: flex-start;
    grid-template-columns: 1fr;
  }

  .section-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .sign-summary {
    grid-template-columns: 1fr;
  }

  .sign-heatmap {
    grid-template-columns: repeat(10, 14px);
  }

  .hero-account,
  .hero-actions {
    grid-template-columns: 1fr;
    width: 100%;
  }

  .recent-order-card {
    grid-template-columns: 56px minmax(0, 1fr);
  }

  .recent-order-card .el-button {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
