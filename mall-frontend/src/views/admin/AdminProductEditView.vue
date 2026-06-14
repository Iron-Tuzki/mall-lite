<script setup lang="ts">
import { Delete, Plus } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  createAdminProduct,
  getAdminProduct,
  updateAdminProduct,
  type AdminProductRequest,
  type AdminProductSku
} from '@/api/admin/product';
import { listCategories, type CategoryItem } from '@/api/product';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const categories = ref<CategoryItem[]>([]);
const productId = computed(() => Number(route.params.id || 0));
const isEdit = computed(() => productId.value > 0);

const form = reactive<AdminProductRequest>({
  categoryId: null,
  productCode: '',
  name: '',
  subtitle: '',
  mainImageUrl: '',
  description: '',
  status: 1,
  sort: 0,
  skus: []
});

onMounted(async () => {
  await loadCategories();
  if (isEdit.value) {
    await loadProduct();
  } else {
    addSku();
  }
});

async function loadCategories() {
  const response = await listCategories();
  if (response.data.success) {
    categories.value = response.data.data;
  }
}

async function loadProduct() {
  loading.value = true;
  try {
    const response = await getAdminProduct(productId.value);
    if (!response.data.success) {
      throw new Error(response.data.message || '商品加载失败');
    }
    const product = response.data.data;
    Object.assign(form, {
      categoryId: product.categoryId,
      productCode: product.productCode,
      name: product.name,
      subtitle: product.subtitle || '',
      mainImageUrl: product.mainImageUrl || '',
      description: product.description || '',
      status: product.status,
      sort: product.sort,
      skus: (product.skus || []).map((sku) => ({ ...sku }))
    });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '商品加载失败');
  } finally {
    loading.value = false;
  }
}

function addSku() {
  form.skus.push({
    skuCode: '',
    skuName: '',
    specData: '{}',
    price: 0,
    originalPrice: 0,
    mainImageUrl: '',
    status: 1,
    availableStock: 0
  });
}

function removeSku(index: number) {
  if (form.skus.length <= 1) {
    ElMessage.warning('至少保留一个 SKU');
    return;
  }
  form.skus.splice(index, 1);
}

function validateForm() {
  if (!form.categoryId || !form.productCode || !form.name) {
    ElMessage.warning('请填写分类、商品编码和商品名称');
    return false;
  }
  if (form.skus.length === 0) {
    ElMessage.warning('至少需要一个 SKU');
    return false;
  }
  for (const sku of form.skus) {
    if (!sku.skuCode || !sku.skuName || sku.price < 0 || sku.availableStock < 0) {
      ElMessage.warning('请完整填写 SKU 编码、名称、价格和库存');
      return false;
    }
  }
  return true;
}

async function saveProduct() {
  if (!validateForm()) {
    return;
  }
  saving.value = true;
  try {
    const payload: AdminProductRequest = {
      ...form,
      skus: form.skus.map((sku: AdminProductSku) => ({ ...sku }))
    };
    const response = isEdit.value
      ? await updateAdminProduct(productId.value, payload)
      : await createAdminProduct(payload);
    if (!response.data.success) {
      throw new Error(response.data.message || '保存失败');
    }
    ElMessage.success('商品已保存');
    await router.push('/admin/products');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <section v-loading="loading" class="admin-page">
    <div class="page-title">
      <div>
        <h1>{{ isEdit ? '编辑商品' : '新增商品' }}</h1>
        <p>维护商品 SPU、SKU 与库存。</p>
      </div>
      <div class="actions">
        <el-button @click="router.push('/admin/products')">返回</el-button>
        <el-button type="primary" :loading="saving" @click="saveProduct">保存</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-form label-width="96px" class="product-form">
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类">
            <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品编码"><el-input v-model="form.productCode" /></el-form-item>
        <el-form-item label="商品名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="副标题"><el-input v-model="form.subtitle" /></el-form-item>
        <el-form-item label="主图"><el-input v-model="form.mainImageUrl" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button :label="1">上架</el-radio-button>
            <el-radio-button :label="0">下架</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
      </el-form>
    </el-card>

    <el-card class="sku-card" shadow="never">
      <template #header>
        <div class="card-head">
          <strong>SKU 明细</strong>
          <el-button :icon="Plus" type="primary" plain @click="addSku">添加 SKU</el-button>
        </div>
      </template>
      <el-table :data="form.skus">
        <el-table-column label="SKU 编码" min-width="150">
          <template #default="{ row }"><el-input v-model="row.skuCode" /></template>
        </el-table-column>
        <el-table-column label="SKU 名称" min-width="150">
          <template #default="{ row }"><el-input v-model="row.skuName" /></template>
        </el-table-column>
        <el-table-column label="规格 JSON" min-width="180">
          <template #default="{ row }"><el-input v-model="row.specData" /></template>
        </el-table-column>
        <el-table-column label="价格" width="150">
          <template #default="{ row }"><el-input-number v-model="row.price" :min="0" :precision="2" /></template>
        </el-table-column>
        <el-table-column label="原价" width="150">
          <template #default="{ row }"><el-input-number v-model="row.originalPrice" :min="0" :precision="2" /></template>
        </el-table-column>
        <el-table-column label="库存" width="140">
          <template #default="{ row }"><el-input-number v-model="row.availableStock" :min="0" /></template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ $index }">
            <el-button link type="danger" :icon="Delete" @click="removeSku($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </section>
</template>

<style scoped>
.admin-page {
  padding: 24px;
}

.page-title,
.card-head,
.actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-title {
  margin-bottom: 18px;
}

.page-title h1 {
  margin: 0 0 6px;
  font-size: 26px;
}

.page-title p {
  margin: 0;
  color: #6b7280;
}

.product-form {
  max-width: 780px;
}

.product-form .el-select {
  width: 100%;
}

.sku-card {
  margin-top: 18px;
}
</style>
