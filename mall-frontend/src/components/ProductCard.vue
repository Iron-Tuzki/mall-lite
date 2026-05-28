<script setup lang="ts">
import { computed } from 'vue';

export interface ProductCardData {
  id: number;
  name: string;
  subtitle: string;
  price: number;
  buyers: number;
  imageUrl: string;
  badge?: string;
}

const props = defineProps<{
  product: ProductCardData;
}>();

const productLink = computed(() => `/product/${props.product.id}`);
</script>

<template>
  <RouterLink class="product-card" :to="productLink">
    <div class="image-wrap">
      <img :alt="product.name" :src="product.imageUrl" />
      <span v-if="product.badge" class="badge">{{ product.badge }}</span>
    </div>
    <h3>{{ product.name }}</h3>
    <p>{{ product.subtitle }}</p>
    <div class="meta-row">
      <span class="price">¥{{ product.price }}</span>
      <span>{{ product.buyers }} 人购买</span>
    </div>
  </RouterLink>
</template>

<style scoped>
.product-card {
  display: block;
  min-width: 0;
}

.image-wrap {
  position: relative;
  overflow: hidden;
  aspect-ratio: 1 / 1;
  background: #fff;
  border-radius: 8px;
}

.image-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s ease;
}

.product-card:hover img {
  transform: scale(1.03);
}

.badge {
  position: absolute;
  left: 10px;
  top: 10px;
  padding: 4px 8px;
  color: #fff;
  background: #ff4d00;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 800;
}

h3 {
  display: -webkit-box;
  min-height: 44px;
  margin: 10px 0 4px;
  overflow: hidden;
  color: #222;
  font-size: 15px;
  font-weight: 500;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

p {
  margin: 0 0 8px;
  overflow: hidden;
  color: #ff4d00;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  color: #8a8a8a;
  font-size: 13px;
}

.price {
  font-size: 21px;
}
</style>
