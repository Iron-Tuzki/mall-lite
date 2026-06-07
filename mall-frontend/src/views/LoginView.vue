<script setup lang="ts">
import { Lock, Message, Phone, User } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { register } from '@/api/user';
import SiteHeader from '@/components/SiteHeader.vue';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const loading = ref(false);
const mode = ref<'login' | 'register'>('login');

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
  email: ''
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
    await router.replace(resolveRedirect());
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败');
  } finally {
    loading.value = false;
  }
}

async function submitRegister() {
  if (!form.username.trim() || !form.password) {
    ElMessage.warning('请输入用户名和密码');
    return;
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码长度至少 6 位');
    return;
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致');
    return;
  }

  loading.value = true;
  try {
    const username = form.username.trim();
    const password = form.password;
    const response = await register({
      username,
      password,
      nickname: form.nickname.trim() || undefined,
      phone: form.phone.trim() || undefined,
      email: form.email.trim() || undefined
    });
    if (!response.data.success) {
      throw new Error(response.data.message || '注册失败');
    }
    await authStore.login(username, password);
    ElMessage.success('注册成功');
    await router.replace(resolveRedirect());
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '注册失败');
  } finally {
    loading.value = false;
  }
}

function resolveRedirect() {
  return typeof route.query.redirect === 'string' ? route.query.redirect : '/profile';
}

function toggleMode() {
  mode.value = mode.value === 'login' ? 'register' : 'login';
}
</script>

<template>
  <SiteHeader />

  <main class="login-page">
    <section class="login-panel">
      <div class="login-copy">
        <span class="eyebrow">Mall Lite</span>
        <h1>{{ mode === 'login' ? '欢迎回来' : '创建你的账号' }}</h1>
        <p>
          {{ mode === 'login'
            ? '登录后可以查看个人主页、订单、地址，并继续购物车和秒杀下单流程。'
            : '注册后会自动登录，可以继续访问刚才的页面。手机号和邮箱可稍后再补充。' }}
        </p>
      </div>

      <el-form class="login-form" @submit.prevent>
        <div class="form-head">
          <h2>{{ mode === 'login' ? '用户登录' : '用户注册' }}</h2>
          <el-segmented
            v-model="mode"
            :options="[
              { label: '登录', value: 'login' },
              { label: '注册', value: 'register' }
            ]"
          />
        </div>

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
            @keyup.enter="mode === 'login' ? submitLogin() : submitRegister()"
          />
        </el-form-item>

        <template v-if="mode === 'register'">
          <el-form-item>
            <el-input
              v-model="form.confirmPassword"
              :prefix-icon="Lock"
              placeholder="确认密码"
              show-password
              size="large"
              type="password"
              @keyup.enter="submitRegister"
            />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.nickname" :prefix-icon="User" placeholder="昵称，选填" size="large" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.phone" :prefix-icon="Phone" placeholder="手机号，选填" size="large" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.email" :prefix-icon="Message" placeholder="邮箱，选填" size="large" />
          </el-form-item>
        </template>

        <el-button
          class="login-button"
          :loading="loading"
          size="large"
          type="primary"
          @click="mode === 'login' ? submitLogin() : submitRegister()"
        >
          {{ mode === 'login' ? '立即登录' : '立即注册' }}
        </el-button>
        <el-button class="mode-link" text type="primary" @click="toggleMode">
          {{ mode === 'login' ? '还没有账号？立即注册' : '已有账号？返回登录' }}
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

.form-head {
  display: grid;
  gap: 14px;
  margin-bottom: 22px;
}

.form-head h2 {
  margin: 0;
}

.login-button {
  width: 100%;
  --el-button-bg-color: #ff4d00;
  --el-button-border-color: #ff4d00;
  --el-button-hover-bg-color: #ff6a22;
  --el-button-hover-border-color: #ff6a22;
  font-weight: 800;
}

.mode-link {
  justify-self: center;
  margin-top: 12px;
  font-weight: 700;
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
