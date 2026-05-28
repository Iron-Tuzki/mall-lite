<script setup lang="ts">
import { Lock, User } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import SiteHeader from '@/components/SiteHeader.vue';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const loading = ref(false);

const form = reactive({
  username: '',
  password: ''
});

async function submitLogin() {
  if (!form.username.trim() || !form.password) {
    ElMessage.warning('请输入用户名和密码');
    return;
  }

  loading.value = true;
  try {
    await authStore.login(form.username.trim(), form.password);
    ElMessage.success('登录成功');
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/profile';
    await router.replace(redirect);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <SiteHeader />

  <main class="login-page">
    <section class="login-panel">
      <div class="login-copy">
        <span class="eyebrow">Mall Lite</span>
        <h1>欢迎回来</h1>
        <p>登录后可以查看个人主页、订单、地址，后续购物车和下单也会复用这份登录态。</p>
      </div>

      <el-form class="login-form" @submit.prevent>
        <h2>用户登录</h2>
        <el-form-item>
          <el-input v-model="form.username" :prefix-icon="User" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            placeholder="密码"
            show-password
            size="large"
            type="password"
            @keyup.enter="submitLogin"
          />
        </el-form-item>
        <el-button class="login-button" :loading="loading" size="large" type="primary" @click="submitLogin">
          立即登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  min-height: calc(100vh - 88px);
  place-items: center;
  padding: 40px 24px;
  background:
    linear-gradient(120deg, rgba(255, 77, 0, 0.1), rgba(255, 255, 255, 0.88)),
    url("https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?auto=format&fit=crop&w=1600&q=80") center / cover;
}

.login-panel {
  display: grid;
  width: min(940px, 100%);
  grid-template-columns: 1fr 380px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 8px;
  box-shadow: 0 20px 60px rgba(31, 31, 31, 0.16);
}

.login-copy {
  display: grid;
  align-content: center;
  gap: 12px;
  padding: 48px;
}

.eyebrow {
  color: #ff4d00;
  font-weight: 900;
}

.login-copy h1 {
  margin: 0;
  font-size: 42px;
}

.login-copy p {
  max-width: 420px;
  margin: 0;
  color: #555;
  font-size: 16px;
  line-height: 1.8;
}

.login-form {
  display: grid;
  align-content: center;
  padding: 40px;
  background: #fff;
}

.login-form h2 {
  margin: 0 0 24px;
}

.login-button {
  width: 100%;
  --el-button-bg-color: #ff4d00;
  --el-button-border-color: #ff4d00;
  --el-button-hover-bg-color: #ff6a22;
  --el-button-hover-border-color: #ff6a22;
  font-weight: 800;
}

@media (max-width: 760px) {
  .login-panel {
    grid-template-columns: 1fr;
  }

  .login-copy {
    padding: 32px;
  }

  .login-copy h1 {
    font-size: 32px;
  }
}
</style>
