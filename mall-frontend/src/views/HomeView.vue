<script setup lang="ts">
import { RefreshRight } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { onMounted, ref } from 'vue';

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

const products = ref<ProductCardData[]>([]);
const loading = ref(false);

const fallbackProducts: ProductCardData[] = [
  {
    id: 1001,
    name: '日本进口缝隙收纳架厨房置物架冰箱夹缝移动多层落地架',
    subtitle: '送运费险',
    price: 29,
    buyers: 23,
    imageUrl: 'https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?auto=format&fit=crop&w=600&q=80'
  },
  {
    id: 1002,
    name: '北美红雀冬青浆果欧式陶瓷餐具汤锅汤碗家用套装',
    subtitle: '精选好物',
    price: 118,
    buyers: 13,
    imageUrl: 'https://images.unsplash.com/photo-1518733057094-95b53143d2a7?auto=format&fit=crop&w=600&q=80'
  },
  {
    id: 1003,
    name: '结婚酒曲婚庆用品大红色双喜酒杯陶瓷小号杯',
    subtitle: '礼盒装',
    price: 29.85,
    buyers: 27,
    imageUrl: 'https://images.unsplash.com/photo-1598300042247-d088f8ab3a91?auto=format&fit=crop&w=600&q=80'
  },
  {
    id: 1004,
    name: '仙山集日式皆川明风刺绣布艺锅盖把手防烫垫',
    subtitle: '全店满减',
    price: 11.5,
    buyers: 67,
    imageUrl: 'https://images.unsplash.com/photo-1584346133934-a3afd2a33c4c?auto=format&fit=crop&w=600&q=80',
    badge: '春季焕新'
  },
  {
    id: 1005,
    name: '衣柜收纳神器分层置物架卧室柜子隔板储物架',
    subtitle: '家居收纳',
    price: 37.8,
    buyers: 56,
    imageUrl: 'https://images.unsplash.com/photo-1616486886892-ff366aa67ba4?auto=format&fit=crop&w=600&q=80'
  },
  {
    id: 1006,
    name: '木果盘原木创意家用木质干果盘客厅大号水果篮',
    subtitle: '送运费险',
    price: 33,
    buyers: 12,
    imageUrl: 'https://images.unsplash.com/photo-1565193566173-7a0ee3dbe261?auto=format&fit=crop&w=600&q=80'
  },
  {
    id: 1007,
    name: '304 不锈钢筷子勺子套装学生便携两件套',
    subtitle: '厨房好物',
    price: 19.9,
    buyers: 89,
    imageUrl: 'https://images.unsplash.com/photo-1610701596007-11502861dcfa?auto=format&fit=crop&w=600&q=80'
  },
  {
    id: 1008,
    name: '法式复古玫瑰 ins 少女心床裙款田园风四件套',
    subtitle: '新品上架',
    price: 168,
    buyers: 35,
    imageUrl: 'https://images.unsplash.com/photo-1616627451515-cbc80e6ece35?auto=format&fit=crop&w=600&q=80'
  },
  {
    id: 1009,
    name: '特大号粗藤脏衣收纳篮衣篓藤编洗衣桶玩具筐',
    subtitle: '春季焕新',
    price: 58,
    buyers: 41,
    imageUrl: 'https://images.unsplash.com/photo-1604014237800-1c9102c219da?auto=format&fit=crop&w=600&q=80',
    badge: '热门'
  },
  {
    id: 1010,
    name: '沙拉加厚不锈钢大盆家用洗菜盆圆形汤盆',
    subtitle: '厨房必备',
    price: 24.9,
    buyers: 73,
    imageUrl: 'https://images.unsplash.com/photo-1584464491033-06628f3a6b7b?auto=format&fit=crop&w=600&q=80'
  },
  {
    id: 1011,
    name: '特价超大号实木托盘长方形家用端菜餐盘',
    subtitle: '实木质感',
    price: 45,
    buyers: 18,
    imageUrl: 'https://images.unsplash.com/photo-1616627547584-bf28cee262db?auto=format&fit=crop&w=600&q=80'
  },
  {
    id: 1012,
    name: '不锈钢煎蛋神器早餐锅迷你煎锅蛋饺模具',
    subtitle: '早餐神器',
    price: 16.8,
    buyers: 52,
    imageUrl: 'https://images.unsplash.com/photo-1585653621032-a5fec164ee92?auto=format&fit=crop&w=600&q=80'
  }
];

onMounted(() => {
  loadRecommendProducts();
});

async function loadRecommendProducts() {
  loading.value = true;
  try {
    const response = await listRecommendProducts({ pageNo: 1, pageSize: 18 });
    products.value = response.data.data.records.map(toProductCard);
  } catch {
    products.value = fallbackProducts;
    ElMessage.warning('商品接口暂时不可用，已展示本地示例商品');
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
              <img alt="矿泉水" src="https://images.unsplash.com/photo-1564419320461-6870880221ad?auto=format&fit=crop&w=240&q=80" />
              <span>¥14.9</span>
            </div>
            <div>
              <img alt="服饰" src="https://images.unsplash.com/photo-1523381294911-8d3cead13475?auto=format&fit=crop&w=240&q=80" />
              <span>¥53.1</span>
            </div>
            <div>
              <img alt="纸巾" src="https://images.unsplash.com/photo-1583947215259-38e31be8751f?auto=format&fit=crop&w=240&q=80" />
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
        <h2 class="section-title">猜你喜欢</h2>
        <el-button :icon="RefreshRight" :loading="loading" circle @click="loadRecommendProducts" />
      </div>
      <div v-loading="loading" class="product-grid">
        <ProductCard v-for="product in products" :key="product.id" :product="product" />
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

.product-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 18px;
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
