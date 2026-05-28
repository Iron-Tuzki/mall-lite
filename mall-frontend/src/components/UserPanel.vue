<script setup lang="ts">
import { Clock, CollectionTag, Goods, Star } from '@element-plus/icons-vue';
</script>

<template>
  <aside class="user-panel">
    <div class="profile-row">
      <img
        alt="用户头像"
        class="avatar"
        src="https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=160&q=80"
      />
      <div>
        <div class="hello">上午好</div>
        <div class="quick-links">注册 / 开店</div>
      </div>
    </div>

    <div class="login-copy">登录后查看更多推荐、订单和购物车信息</div>
    <el-button class="login-button" type="primary">立即登录</el-button>

    <div class="shortcut-grid">
      <a href="#">
        <el-icon><Star /></el-icon>
        <span>收藏</span>
      </a>
      <a href="#">
        <el-icon><Goods /></el-icon>
        <span>买过</span>
      </a>
      <a href="#">
        <el-icon><CollectionTag /></el-icon>
        <span>卡券</span>
      </a>
      <a href="#">
        <el-icon><Clock /></el-icon>
        <span>足迹</span>
      </a>
    </div>

    <div class="coin-card">
      <span>新人专享红包</span>
      <strong>满 20 减 5</strong>
    </div>
  </aside>
</template>

<style scoped>
.user-panel {
  padding: 18px;
  background: #fff;
  border-radius: 8px;
}

.profile-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  object-fit: cover;
}

.hello {
  font-size: 20px;
  font-weight: 900;
}

.quick-links,
.login-copy {
  color: #777;
  font-size: 13px;
}

.login-copy {
  margin: 16px 0 10px;
}

.login-button {
  width: 100%;
  --el-button-bg-color: #ff4d00;
  --el-button-border-color: #ff4d00;
  --el-button-hover-bg-color: #ff6a22;
  --el-button-hover-border-color: #ff6a22;
  font-weight: 800;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin: 18px 0;
}

.shortcut-grid a {
  display: grid;
  justify-items: center;
  gap: 4px;
  color: #333;
  font-size: 12px;
}

.shortcut-grid .el-icon {
  font-size: 22px;
}

.coin-card {
  display: grid;
  gap: 4px;
  padding: 14px;
  color: #ff4d00;
  background: linear-gradient(135deg, #fff3ec, #ffe0bf);
  border-radius: 8px;
}

.coin-card strong {
  font-size: 22px;
}
</style>
