<script setup lang="ts">
import {
  Basketball,
  Briefcase,
  Cellphone,
  CoffeeCup,
  Food,
  Goods,
  House,
  Monitor,
  Present,
  Van
} from '@element-plus/icons-vue';

const categories = [
  { icon: Monitor, text: '电脑 / 配件 / 办公 / 文具' },
  { icon: Briefcase, text: '工业品 / 商业 / 农业 / 定制' },
  { icon: Cellphone, text: '家电 / 手机 / 通信 / 数码' },
  { icon: House, text: '家具 / 家装 / 家居 / 厨具' },
  { icon: Goods, text: '女装 / 男装 / 内衣 / 配饰' },
  { icon: Basketball, text: '女鞋 / 男鞋 / 运动 / 户外' },
  { icon: Van, text: '汽车 / 珠宝 / 文玩 / 箱包' },
  { icon: Food, text: '食品 / 生鲜 / 酒类 / 健康' },
  { icon: Present, text: '母婴 / 童装 / 玩具 / 宠物' },
  { icon: CoffeeCup, text: '美妆 / 个护 / 家清 / 香氛' }
];
</script>

<template>
  <aside class="category-sidebar" aria-label="商品分类">
    <a v-for="item in categories" :key="item.text" class="category-item" href="#">
      <el-icon><component :is="item.icon" /></el-icon>
      <span>{{ item.text }}</span>
    </a>
  </aside>
</template>

<style scoped>
.category-sidebar {
  display: grid;
  gap: 4px;
  padding: 14px;
  background: #fff;
  border-radius: 8px;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 34px;
  padding: 0 4px;
  color: #333;
  border-radius: 6px;
  font-size: 15px;
  white-space: nowrap;
}

.category-item:hover {
  color: #ff4d00;
  background: #fff3ec;
}
</style>
