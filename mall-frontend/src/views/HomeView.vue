<script setup lang="ts">
import { RefreshRight } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue';

import { listRecommendProducts, type ProductSummary } from '@/api/product';
import CategorySidebar from '@/components/CategorySidebar.vue';
import ProductCard, { type ProductCardData } from '@/components/ProductCard.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import UserPanel from '@/components/UserPanel.vue';

const channels = ['天猫', '直播', '企业购', '闪购', '超市', '闲鱼', '国际'];

const fallbackImages = [
  'https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1518733057094-95b53143d2a7?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1598300042247-d088f8ab3a91?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1584346133934-a3afd2a33c4c?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1616486886892-ff366aa67ba4?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1565193566173-7a0ee3dbe261?auto=format&fit=crop&w=600&q=80'
];

const fallbackProducts: ProductCardData[] = [
  {
    id: 1001,
    name: '夹缝移动收纳架厨房置物架',
    subtitle: '送运费险',
    price: 29,
    buyers: 23,
    imageUrl: fallbackImages[0],
    badge: '示例'
  },
  {
    id: 1002,
    name: '复古陶瓷汤锅餐具套装',
    subtitle: '精选好物',
    price: 118,
    buyers: 13,
    imageUrl: fallbackImages[1],
    badge: '示例'
  }
];

const products = ref<ProductCardData[]>([]);
const loading = ref(false);
const loadingMore = ref(false);
const finished = ref(false);
const pageNo = ref(1);
const pageSize = 6;
const total = ref(0);
const loadMoreTrigger = ref<HTMLElement | null>(null);

let observer: IntersectionObserver | null = null;

onMounted(async () => {
  await loadRecommendProducts(true);
  await nextTick();
  observeLoadMoreTrigger();
  window.addEventListener('scroll', handleWindowScroll, { passive: true });
  await ensureEnoughScrollableContent();
});

onBeforeUnmount(() => {
  observer?.disconnect();
  window.removeEventListener('scroll', handleWindowScroll);
});

async function refreshProducts() {
  await loadRecommendProducts(true);
}

async function loadNextPage() {
  if (loading.value || loadingMore.value || finished.value) {
    return;
  }
  pageNo.value += 1;
  await loadRecommendProducts(false);
}

async function loadRecommendProducts(reset: boolean) {
  if (reset) {
    pageNo.value = 1;
    finished.value = false;
    loading.value = true;
  } else {
    loadingMore.value = true;
  }

  try {
    const response = await listRecommendProducts({ pageNo: pageNo.value, pageSize });
    const pageData = response.data.data;
    const nextProducts = pageData.records.map((product, index) => {
      const globalIndex = (pageData.pageNo - 1) * pageData.pageSize + index;
      return toProductCard(product, globalIndex);
    });

    total.value = pageData.total;
    products.value = reset ? nextProducts : [...products.value, ...nextProducts];
    finished.value = products.value.length >= pageData.total || nextProducts.length === 0;
  } catch {
    if (reset) {
      products.value = fallbackProducts;
      finished.value = true;
    } else {
      pageNo.value -= 1;
    }
    ElMessage.warning('商品接口暂时不可用，已展示本地示例商品');
  } finally {
    loading.value = false;
    loadingMore.value = false;
    await nextTick();
    await ensureEnoughScrollableContent();
  }
}

function observeLoadMoreTrigger() {
  if (!loadMoreTrigger.value) {
    return;
  }
  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0]?.isIntersecting) {
        loadNextPage();
      }
    },
    {
      root: null,
      rootMargin: '240px 0px',
      threshold: 0
    }
  );
  observer.observe(loadMoreTrigger.value);
}

async function ensureEnoughScrollableContent() {
  if (loading.value || loadingMore.value || finished.value) {
    return;
  }
  const pageHeight = document.documentElement.scrollHeight;
  const viewportHeight = window.innerHeight;
  if (pageHeight <= viewportHeight + 120) {
    await loadNextPage();
  }
}

function handleWindowScroll() {
  if (loading.value || loadingMore.value || finished.value) {
    return;
  }
  const scrollTop = window.scrollY || document.documentElement.scrollTop;
  const viewportHeight = window.innerHeight;
  const pageHeight = document.documentElement.scrollHeight;
  if (scrollTop + viewportHeight >= pageHeight - 260) {
    loadNextPage();
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
    badge: index < 2 ? '推荐' : undefined
  };
}
</script>

<template>
  <SiteHeader />

  <main class="home-page">
    <section class="page-shell channel-row" aria-label="频道导航">
      <a v-for="channel in channels" :key="channel" href="#">{{ channel }}</a>
    </section>

    <section class="page-shell hero-grid">
      <CategorySidebar />

      <div class="main-promotions">
        <div class="banner-card">
          <div>
            <span class="eyebrow">聚划算 × Mall Lite</span>
            <h1>百亿补贴节<br />限时秒杀</h1>
            <p>好物 5 折起，后续接入秒杀活动</p>
            <div class="dots">
              <span />
              <span />
              <span />
              <span />
            </div>
          </div>
          <img
            alt="扫地机器人促销图"
            src="https://images.unsplash.com/photo-1589003077984-894e133dabab?auto=format&fit=crop&w=520&q=80"
          />
        </div>

        <div class="deal-card">
          <strong>百亿补贴 · 买贵必赔</strong>
          <div class="deal-products">
            <div>
              <img alt="家庭饮用水" src="https://images.unsplash.com/photo-1564419320461-6870880221ad?auto=format&fit=crop&w=240&q=80" />
              <span>¥14.9</span>
            </div>
            <div>
              <img alt="服饰背心" src="https://images.unsplash.com/photo-1523381294911-8d3cead13475?auto=format&fit=crop&w=240&q=80" />
              <span>¥53.1</span>
            </div>
            <div>
              <img alt="家庭纸巾" src="https://images.unsplash.com/photo-1583947215259-38e31be8751f?auto=format&fit=crop&w=240&q=80" />
              <span>¥9.9</span>
            </div>
          </div>
        </div>

        <div class="small-card">
          <strong>淘江湖</strong>
          <p>#来波回忆杀# 一起找寻商品的珍贵记忆</p>
        </div>
        <div class="small-card image-card">
          <strong>直播精选</strong>
          <p>好物推荐</p>
        </div>
        <div class="small-card">
          <strong>淘工厂</strong>
          <p>源头好货，超低价</p>
        </div>
        <div class="small-card">
          <strong>低价专区</strong>
          <p>每天都有新惊喜</p>
        </div>
      </div>

      <UserPanel />
    </section>

    <section class="page-shell product-section">
      <div class="product-heading">
        <div>
          <h2 class="section-title">猜你喜欢</h2>
          <span v-if="total" class="total-text">共 {{ total }} 件商品</span>
        </div>
        <el-button :icon="RefreshRight" :loading="loading" circle @click="refreshProducts" />
      </div>

      <div v-loading="loading" class="product-grid">
        <ProductCard v-for="product in products" :key="product.id" :product="product" />
      </div>

      <div ref="loadMoreTrigger" class="load-more">
        <span v-if="loadingMore">正在加载更多商品...</span>
        <span v-else-if="finished">已经到底啦</span>
        <span v-else>继续下滑加载更多</span>
      </div>
    </section>
  </main>
</template>

<style scoped>
.home-page {
  padding-bottom: 48px;
}

.channel-row {
  display: flex;
  justify-content: center;
  gap: 42px;
  padding: 26px 0 18px;
  color: #1f1f1f;
  font-size: 16px;
  font-weight: 800;
}

.channel-row a:nth-child(1),
.channel-row a:nth-child(2),
.channel-row a:nth-child(3) {
  color: #ff0050;
}

.channel-row a:nth-child(5) {
  color: #14b400;
}

.hero-grid {
  display: grid;
  grid-template-columns: 230px minmax(0, 1fr) 230px;
  gap: 16px;
  align-items: stretch;
}

.main-promotions {
  display: grid;
  grid-template-columns: 1.35fr 1fr;
  gap: 14px;
}

.banner-card,
.deal-card,
.small-card {
  overflow: hidden;
  background: #fff;
  border-radius: 8px;
}

.banner-card {
  display: grid;
  grid-template-columns: 1fr 210px;
  align-items: center;
  min-height: 180px;
  padding: 22px;
  color: #fff;
  background: #e70d19;
}

.banner-card h1 {
  margin: 8px 0;
  font-size: 34px;
  line-height: 1.18;
}

.banner-card p {
  margin: 0;
  font-size: 16px;
}

.banner-card img {
  width: 100%;
  height: 140px;
  object-fit: cover;
  border-radius: 8px;
}

.eyebrow {
  font-weight: 900;
}

.dots {
  display: flex;
  gap: 8px;
  margin-top: 28px;
}

.dots span {
  width: 10px;
  height: 10px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 50%;
}

.deal-card {
  padding: 18px;
}

.deal-products {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: 16px;
}

.deal-products div {
  position: relative;
  min-width: 0;
}

.deal-products img {
  width: 100%;
  aspect-ratio: 1 / 0.9;
  object-fit: cover;
  border-radius: 8px;
}

.deal-products span {
  position: absolute;
  left: 8px;
  bottom: 8px;
  padding: 3px 7px;
  color: #fff;
  background: #ff4d00;
  border-radius: 5px;
  font-weight: 900;
}

.small-card {
  min-height: 122px;
  padding: 18px;
}

.small-card strong {
  font-size: 18px;
}

.small-card p {
  color: #555;
  line-height: 1.7;
}

.image-card {
  color: #fff;
  background:
    linear-gradient(rgba(0, 0, 0, 0.16), rgba(0, 0, 0, 0.28)),
    url("https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=520&q=80") center / cover;
}

.image-card p {
  color: #fff;
}

.product-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.total-text {
  margin-left: 10px;
  color: #888;
  font-size: 14px;
}

.product-grid {
  display: grid;
  min-height: 240px;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 18px;
}

.load-more {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80px;
  color: #888;
  font-size: 14px;
}

@media (max-width: 1200px) {
  .hero-grid {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .hero-grid :deep(.user-panel) {
    display: none;
  }

  .product-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .channel-row {
    justify-content: flex-start;
    gap: 18px;
    overflow-x: auto;
  }

  .hero-grid,
  .main-promotions {
    grid-template-columns: 1fr;
  }

  .banner-card {
    grid-template-columns: 1fr;
  }

  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .product-grid {
    grid-template-columns: 1fr;
  }
}
</style>
