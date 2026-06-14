<script setup lang="ts">
import { Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import {
  deleteAdminProduct,
  getAdminProduct,
  listAdminProducts,
  updateAdminProductStatus,
  type AdminProduct
} from '@/api/admin/product';

const router = useRouter();
const loading = ref(false);
const detailLoading = ref(false);
const detailVisible = ref(false);
const productDetail = ref<AdminProduct | null>(null);
const products = ref<AdminProduct[]>([]);
const total = ref(0);
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  status: undefined as number | undefined
});

onMounted(loadProducts);

async function loadProducts() {
  loading.value = true;
  try {
    const response = await listAdminProducts(query);
    if (!response.data.success) {
      throw new Error(response.data.message || '商品加载失败');
    }
    products.value = response.data.data.records;
    total.value = response.data.data.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '商品加载失败');
  } finally {
    loading.value = false;
  }
}

async function changeStatus(row: AdminProduct) {
  const nextStatus = row.status === 1 ? 0 : 1;
  const response = await updateAdminProductStatus(row.id, nextStatus);
  if (!response.data.success) {
    ElMessage.error(response.data.message || '状态更新失败');
    return;
  }
  ElMessage.success(nextStatus === 1 ? '已上架' : '已下架');
  await loadProducts();
}

async function removeProduct(row: AdminProduct) {
  await ElMessageBox.confirm(`确认删除商品「${row.name}」？`, '删除商品', { type: 'warning' });
  const response = await deleteAdminProduct(row.id);
  if (!response.data.success) {
    ElMessage.error(response.data.message || '删除失败');
    return;
  }
  ElMessage.success('商品已删除');
  await loadProducts();
}

async function showProductDetail(row: AdminProduct) {
  detailVisible.value = true;
  detailLoading.value = true;
  productDetail.value = null;
  try {
    const response = await getAdminProduct(row.id);
    if (!response.data.success) {
      throw new Error(response.data.message || '商品详情加载失败');
    }
    productDetail.value = response.data.data;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '商品详情加载失败');
    detailVisible.value = false;
  } finally {
    detailLoading.value = false;
  }
}
</script>

<template>
  <section class="admin-page">
    <div class="page-title">
      <div>
        <h1>商品管理</h1>
        <p>维护平台所有商品、SKU 和库存。</p>
      </div>
      <el-button :icon="Plus" type="primary" @click="router.push('/admin/products/new')">新增商品</el-button>
    </div>

    <el-card shadow="never">
      <div class="filters">
        <el-input v-model="query.keyword" clearable placeholder="商品名称 / 商品编码" @keyup.enter="loadProducts" />
        <el-select v-model="query.status" clearable placeholder="状态">
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="loadProducts">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="products" row-key="id" @row-dblclick="showProductDetail">
        <el-table-column prop="id" label="ID" width="100" />
        <el-table-column prop="productCode" label="商品编码" min-width="150" />
        <el-table-column prop="name" label="商品名称" min-width="180" />
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="router.push(`/admin/products/${row.id}`)">编辑</el-button>
            <el-button link type="warning" @click="changeStatus(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button>
            <el-button link type="danger" :icon="Delete" @click="removeProduct(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        class="pagination"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="loadProducts"
        @size-change="loadProducts"
      />
    </el-card>

    <el-drawer v-model="detailVisible" title="商品详情" size="720px">
      <div v-loading="detailLoading" class="detail-panel">
        <template v-if="productDetail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="ID">{{ productDetail.id }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="productDetail.status === 1 ? 'success' : 'info'">
                {{ productDetail.status === 1 ? '上架' : '下架' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="商品编码">{{ productDetail.productCode }}</el-descriptions-item>
            <el-descriptions-item label="分类 ID">{{ productDetail.categoryId }}</el-descriptions-item>
            <el-descriptions-item label="商品名称">{{ productDetail.name }}</el-descriptions-item>
            <el-descriptions-item label="排序">{{ productDetail.sort }}</el-descriptions-item>
            <el-descriptions-item label="副标题" :span="2">{{ productDetail.subtitle || '-' }}</el-descriptions-item>
            <el-descriptions-item label="主图" :span="2">{{ productDetail.mainImageUrl || '-' }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ productDetail.description || '-' }}</el-descriptions-item>
          </el-descriptions>

          <h2 class="detail-title">SKU 与库存</h2>
          <el-table :data="productDetail.skus || []" border>
            <el-table-column prop="skuCode" label="SKU 编码" min-width="130" />
            <el-table-column prop="skuName" label="SKU 名称" min-width="140" />
            <el-table-column prop="specData" label="规格" min-width="150" show-overflow-tooltip />
            <el-table-column prop="price" label="价格" width="100" />
            <el-table-column prop="originalPrice" label="原价" width="100" />
            <el-table-column prop="availableStock" label="可用库存" width="110" />
            <el-table-column prop="lockedStock" label="锁定库存" width="110" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-drawer>
  </section>
</template>

<style scoped>
.admin-page {
  padding: 24px;
}

.page-title,
.filters {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
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

.filters {
  justify-content: flex-start;
}

.filters .el-input {
  max-width: 280px;
}

.filters .el-select {
  width: 140px;
}

.pagination {
  justify-content: flex-end;
  margin-top: 18px;
}

.detail-panel {
  min-height: 320px;
}

.detail-title {
  margin: 22px 0 12px;
  font-size: 18px;
}
</style>
