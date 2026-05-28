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
import { ElMessage } from 'element-plus';
import { computed, onMounted, ref } from 'vue';

import { listCategories, type CategoryItem } from '@/api/product';

const props = defineProps<{
  activeCategoryId?: number;
}>();

const icons = [House, Food, Cellphone, Goods, CoffeeCup, Monitor, Briefcase, Basketball, Van, Present];

const fallbackCategories: CategoryItem[] = [
  { id: 910001, name: '家居收纳' },
  { id: 910002, name: '厨房餐具' },
  { id: 910003, name: '数码家电' },
  { id: 910004, name: '床品布艺' },
  { id: 910005, name: '个护清洁' }
];

const categories = ref<CategoryItem[]>(fallbackCategories);

const displayCategories = computed(() => {
  return categories.value.map((category, index) => ({
    ...category,
    icon: icons[index % icons.length]
  }));
});

onMounted(() => {
  loadCategories();
});

async function loadCategories() {
  try {
    const response = await listCategories();
    if (response.data.data.length) {
      categories.value = response.data.data;
    }
  } catch {
    ElMessage.warning('分类接口暂时不可用，已展示本地分类');
  }
}
</script>

<template>
  <aside class="category-sidebar" aria-label="商品分类">
    <RouterLink
      v-for="item in displayCategories"
      :key="item.id"
      class="category-item"
      :class="{ active: item.id === props.activeCategoryId }"
      :to="`/category/${item.id}`"
    >
      <el-icon><component :is="item.icon" /></el-icon>
      <span>{{ item.name }}</span>
    </RouterLink>
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

.category-item:hover,
.category-item.active {
  color: #ff4d00;
  background: #fff3ec;
}

.category-item.active {
  font-weight: 800;
}
</style>
