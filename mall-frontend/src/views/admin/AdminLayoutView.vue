<script setup lang="ts">
import { Goods, Menu as MenuIcon, Stopwatch } from '@element-plus/icons-vue';
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="brand">
        <strong>mall-lite</strong>
        <span>后台管理</span>
      </div>
      <el-menu router :default-active="$route.path" class="admin-menu">
        <el-menu-item index="/admin/products">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/seckill">
          <el-icon><Stopwatch /></el-icon>
          <span>秒杀管理</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <main class="admin-main">
      <header class="admin-topbar">
        <div>
          <el-icon><MenuIcon /></el-icon>
          <span>平台运营后台</span>
        </div>
        <el-tag type="info">无权限校验 MVP</el-tag>
      </header>
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.admin-shell {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  min-height: 100vh;
  background: #f5f7fb;
}

.admin-sidebar {
  background: #111827;
  color: #fff;
}

.brand {
  display: grid;
  gap: 4px;
  padding: 22px 20px;
}

.brand strong {
  font-size: 20px;
}

.brand span {
  color: #9ca3af;
  font-size: 13px;
}

.admin-menu {
  border-right: 0;
}

.admin-main {
  min-width: 0;
}

.admin-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.admin-topbar div {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
}

@media (max-width: 760px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-sidebar {
    position: sticky;
    top: 0;
    z-index: 2;
  }
}
</style>
