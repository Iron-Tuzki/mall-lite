<script setup lang="ts">
import { ArrowLeft, Grid, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { listCategories, listProducts, type CategoryItem, type ProductSummary } from '@/api/product';
import CategorySidebar from '@/components/CategorySidebar.vue';
import ProductCard, { type ProductCardData } from '@/components/ProductCard.vue';
import SiteHeader from '@/components/SiteHeader.vue';

const route = useRoute();
const router = useRouter();

const fallbackImages = [
  'https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1518733057094-95b53143d2a7?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1589003077984-894e133dabab?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1616627451515-cbc80e6ece35?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1583947215259-38e31be8751f?auto=format&fit=crop&w=600&q=80'
];

const fallbackCategories: CategoryItem[] = [
  { id: 910001, name: '家居收纳' },
  { id: 910002, name: '厨房餐具' },
  { id: 910003, name: '数码家电' },
  { id: 910004, name: '床品布艺' },
  { id: 910005, name: '个护清洁' }
];

const categories = ref<CategoryItem[]>(fallbackCategories);
const products = ref<ProductCardData[]>([]);
const loading = ref(false);

const categoryId = computed(() => Number(route.params.categoryId));
const keyword = computed(() => String(route.query.keyword || '').trim());
const activeCategory = computed(() => categories.value.find((category) => category.id === categoryId.value));
const filteredProducts = computed(() => {
  if (!keyword.value) {
    return products.value;
  }
  const loweredKeyword = keyword.value.toLowerCase();
  return products.value.filter((product) => {
    return [product.name, product.subtitle].some((text) => text.toLowerCase().includes(loweredKeyword));
  });
});

onMounted(async () => {
  await Promise.all([loadCategories(), loadProducts()]);
});

watch(
  () => route.params.categoryId,
  () => {
    loadProducts();
  }
);

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

async function loadProducts() {
  loading.value = true;
  try {
    const response = await listProducts({ categoryId: categoryId.value });
    products.value = response.data.data.map(toProductCard);
  } catch {
    products.value = [];
    ElMessage.error('分类商品加载失败');
  } finally {
    loading.value = false;
  }
}

function toProductCard(product: ProductSummary, index: number): ProductCardData {
  return {
    id: product.id,
    name: product.name,
    subtitle: product.subtitle || '精选好物',
    price: product.minPrice ?? 0,
    buyers: (product.id % 90) + 10,
    imageUrl: product.mainImageUrl || fallbackImages[index % fallbackImages.length],
    badge: index < 2 ? '分类精选' : undefined
  };
}
</script>

<template>
  <SiteHeader />

  <main class="category-page">
    <section class="page-shell category-hero">
      <el-button :icon="ArrowLeft" text @click="router.push('/')">返回首页</el-button>
      <div class="hero-copy">
        <el-icon><Grid /></el-icon>
        <div>
          <h1>{{ activeCategory?.name || '分类商品' }}</h1>
          <p>先选分类，再浏览该分类下的商品。顶部搜索会在当前分类内筛选商品。</p>
        </div>
      </div>
    </section>

    <section class="page-shell category-layout">
      <CategorySidebar :active-category-id="categoryId" />

      <div class="category-content">
        <div class="category-tabs">
          <RouterLink
            v-for="category in categories"
            :key="category.id"
            class="category-tab"
            :class="{ active: category.id === categoryId }"
            :to="`/category/${category.id}`"
          >
            {{ category.name }}
          </RouterLink>
        </div>

        <div class="list-heading">
          <div>
            <h2>{{ activeCategory?.name || '商品列表' }}</h2>
            <p v-if="keyword">
              <el-icon><Search /></el-icon>
              当前搜索：{{ keyword }}
            </p>
          </div>
          <span>共 {{ filteredProducts.length }} 件商品</span>
        </div>

        <div v-loading="loading" class="product-grid">
          <ProductCard v-for="product in filteredProducts" :key="product.id" :product="product" />
          <el-empty v-if="!loading && filteredProducts.length === 0" description="暂无符合条件的商品" />
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.category-page {
  min-height: 100vh;
  padding-bottom: 48px;
  background: #f6f7fb;
}

.category-hero {
  padding: 22px 0 12px;
}

.hero-copy {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 10px;
  padding: 20px;
  color: #fff;
  background: #ff4d00;
  border-radius: 8px;
}

.hero-copy .el-icon {
  font-size: 34px;
}

.hero-copy h1 {
  margin: 0 0 6px;
  font-size: 30px;
}

.hero-copy p {
  margin: 0;
  opacity: 0.9;
}

.category-layout {
  display: grid;
  grid-template-columns: 230px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.category-content {
  min-width: 0;
}

.category-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 14px;
  background: #fff;
  border-radius: 8px;
}

.category-tab {
  padding: 8px 14px;
  color: #333;
  background: #f6f7fb;
  border-radius: 8px;
  font-weight: 700;
}

.category-tab:hover,
.category-tab.active {
  color: #ff4d00;
  background: #fff3ec;
}

.list-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin: 18px 0 14px;
}

.list-heading h2 {
  margin: 0 0 6px;
  font-size: 24px;
}

.list-heading p,
.list-heading span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 0;
  color: #888;
  font-size: 14px;
}

.product-grid {
  display: grid;
  min-height: 280px;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 18px;
}

.product-grid :deep(.el-empty) {
  grid-column: 1 / -1;
  background: #fff;
  border-radius: 8px;
}

@media (max-width: 1180px) {
  .product-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .category-layout {
    grid-template-columns: 1fr;
  }

  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .hero-copy {
    align-items: flex-start;
  }

  .product-grid {
    grid-template-columns: 1fr;
  }
}
</style>
