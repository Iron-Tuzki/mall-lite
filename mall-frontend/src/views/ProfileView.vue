<script setup lang="ts">
import { ArrowRight, Location, Lock, SwitchButton, UserFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import SiteHeader from '@/components/SiteHeader.vue';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);

const orderEntries = [
  { label: '待支付', count: 0 },
  { label: '待发货', count: 0 },
  { label: '待收货', count: 0 },
  { label: '已完成', count: 0 },
  { label: '已取消', count: 0 }
];

onMounted(async () => {
  if (!authStore.user) {
    loading.value = true;
    try {
      await authStore.fetchCurrentUser();
    } catch (error) {
      ElMessage.warning(error instanceof Error ? error.message : '请重新登录');
      await router.replace({ path: '/login', query: { redirect: '/profile' } });
    } finally {
      loading.value = false;
    }
  }
});

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
    </section>

    <section class="content-grid">
      <div class="profile-section order-section">
        <div class="section-head">
          <h2>我的订单</h2>
          <a href="#">全部订单 <el-icon><ArrowRight /></el-icon></a>
        </div>
        <div class="order-status-grid">
          <button v-for="entry in orderEntries" :key="entry.label" type="button">
            <strong>{{ entry.count }}</strong>
            <span>{{ entry.label }}</span>
          </button>
        </div>
      </div>

      <div class="profile-section">
        <div class="section-head">
          <h2>最近订单</h2>
        </div>
        <div class="empty-orders">
          <p>这里后续接入我的订单列表，展示最近 3-5 条订单。</p>
        </div>
      </div>

      <div class="profile-section service-section">
        <div class="section-head">
          <h2>我的服务</h2>
        </div>
        <button type="button" @click="router.push('/addresses')">
          <el-icon><Location /></el-icon>
          <span>收货地址</span>
          <el-icon><ArrowRight /></el-icon>
        </button>
        <button type="button">
          <el-icon><Lock /></el-icon>
          <span>账号安全</span>
          <el-icon><ArrowRight /></el-icon>
        </button>
        <button type="button" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          <span>退出登录</span>
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>

      <div class="profile-section account-section">
        <div class="section-head">
          <h2>账号信息</h2>
        </div>
        <dl>
          <div>
            <dt><el-icon><UserFilled /></el-icon> 用户 ID</dt>
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
      </div>
    </section>
  </main>
</template>

<style scoped>
.profile-page {
  padding: 28px 0 56px;
}

.profile-hero {
  display: flex;
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

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
  margin-top: 18px;
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
  margin-bottom: 16px;
}

.section-head h2 {
  margin: 0;
  font-size: 20px;
}

.section-head a {
  display: inline-flex;
  align-items: center;
  color: #777;
  font-size: 14px;
}

.order-status-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.order-status-grid button,
.service-section button {
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

.empty-orders {
  display: grid;
  min-height: 140px;
  place-items: center;
  color: #888;
  background: #f7f8fb;
  border-radius: 8px;
}

.service-section,
.account-section {
  grid-column: 2;
}

.service-section button {
  display: grid;
  width: 100%;
  grid-template-columns: 28px 1fr 18px;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  padding: 14px;
  text-align: left;
}

.service-section .el-icon {
  color: #ff4d00;
  font-size: 20px;
}

.account-section dl {
  display: grid;
  gap: 14px;
  margin: 0;
}

.account-section div {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.account-section dt {
  color: #777;
}

.account-section dd {
  margin: 0;
  font-weight: 800;
}

@media (max-width: 900px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .service-section,
  .account-section {
    grid-column: auto;
  }

  .order-status-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .profile-hero {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
