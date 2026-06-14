<script setup lang="ts">
import { Delete, Plus, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { listAdminSelectableSkus, type AdminProductSku } from '@/api/admin/product';
import {
  addAdminSeckillSku,
  createAdminSeckillActivity,
  deleteAdminSeckillSku,
  getAdminSeckillActivity,
  updateAdminSeckillActivity,
  updateAdminSeckillSku,
  type AdminSeckillActivityRequest,
  type AdminSeckillSku
} from '@/api/admin/seckill';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const skuDialogVisible = ref(false);
const skuLoading = ref(false);
const selectableSkus = ref<AdminProductSku[]>([]);
const selectedSku = ref<AdminProductSku | null>(null);
const activitySkus = ref<AdminSeckillSku[]>([]);
const skuKeyword = ref('');
const activityId = computed(() => Number(route.params.id || 0));
const isEdit = computed(() => activityId.value > 0);

const form = reactive<AdminSeckillActivityRequest>({
  name: '',
  startTime: '',
  endTime: '',
  status: 1,
  remark: ''
});

const skuForm = reactive({
  seckillPrice: 0,
  stockCount: 0,
  limitQuantity: 1,
  sort: 0,
  status: 1
});

onMounted(async () => {
  if (isEdit.value) {
    await loadActivity();
  }
});

async function loadActivity() {
  loading.value = true;
  try {
    const response = await getAdminSeckillActivity(activityId.value);
    if (!response.data.success) {
      throw new Error(response.data.message || '活动加载失败');
    }
    const activity = response.data.data;
    Object.assign(form, {
      name: activity.name,
      startTime: activity.startTime,
      endTime: activity.endTime,
      status: activity.status,
      remark: activity.remark || ''
    });
    activitySkus.value = activity.skus || [];
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '活动加载失败');
  } finally {
    loading.value = false;
  }
}

function normalizeDateTime(value: string) {
  return value ? value.replace(' ', 'T').slice(0, 19) : '';
}

async function saveActivity() {
  if (!form.name || !form.startTime || !form.endTime) {
    ElMessage.warning('请填写活动名称和时间');
    return;
  }
  saving.value = true;
  try {
    const payload = {
      ...form,
      startTime: normalizeDateTime(form.startTime),
      endTime: normalizeDateTime(form.endTime)
    };
    const response = isEdit.value
      ? await updateAdminSeckillActivity(activityId.value, payload)
      : await createAdminSeckillActivity(payload);
    if (!response.data.success) {
      throw new Error(response.data.message || '保存失败');
    }
    ElMessage.success('活动已保存');
    if (!isEdit.value) {
      await router.replace(`/admin/seckill/${response.data.data.id}`);
    } else {
      await loadActivity();
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

async function openSkuDialog() {
  if (!isEdit.value) {
    ElMessage.warning('请先保存活动');
    return;
  }
  skuDialogVisible.value = true;
  await loadSelectableSkus();
}

async function loadSelectableSkus() {
  skuLoading.value = true;
  try {
    const response = await listAdminSelectableSkus({ pageNo: 1, pageSize: 20, keyword: skuKeyword.value });
    if (!response.data.success) {
      throw new Error(response.data.message || 'SKU 加载失败');
    }
    selectableSkus.value = response.data.data.records;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'SKU 加载失败');
  } finally {
    skuLoading.value = false;
  }
}

function chooseSku(row: AdminProductSku) {
  selectedSku.value = row;
  skuForm.seckillPrice = row.price;
  skuForm.stockCount = row.availableStock;
  skuForm.limitQuantity = 1;
  skuForm.sort = 0;
  skuForm.status = 1;
}

async function addSelectedSku() {
  if (!selectedSku.value) {
    ElMessage.warning('请选择 SKU');
    return;
  }
  const response = await addAdminSeckillSku(activityId.value, {
    skuId: selectedSku.value.id || 0,
    ...skuForm
  });
  if (!response.data.success) {
    ElMessage.error(response.data.message || '添加失败');
    return;
  }
  ElMessage.success('秒杀商品已添加');
  skuDialogVisible.value = false;
  selectedSku.value = null;
  await loadActivity();
}

async function saveSku(row: AdminSeckillSku) {
  const response = await updateAdminSeckillSku(activityId.value, row.id, {
    skuId: row.skuId,
    seckillPrice: row.seckillPrice,
    stockCount: row.stockCount,
    limitQuantity: row.limitQuantity,
    sort: row.sort,
    status: row.status
  });
  if (!response.data.success) {
    ElMessage.error(response.data.message || '保存失败');
    return;
  }
  ElMessage.success('秒杀商品已保存');
  await loadActivity();
}

async function removeSku(row: AdminSeckillSku) {
  const response = await deleteAdminSeckillSku(activityId.value, row.id);
  if (!response.data.success) {
    ElMessage.error(response.data.message || '删除失败');
    return;
  }
  ElMessage.success('秒杀商品已删除');
  await loadActivity();
}
</script>

<template>
  <section v-loading="loading" class="admin-page">
    <div class="page-title">
      <div>
        <h1>{{ isEdit ? '编辑秒杀活动' : '新增秒杀活动' }}</h1>
        <p>维护活动时间、状态和活动商品。</p>
      </div>
      <div class="actions">
        <el-button @click="router.push('/admin/seckill')">返回</el-button>
        <el-button type="primary" :loading="saving" @click="saveActivity">保存活动</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-form label-width="96px" class="activity-form">
        <el-form-item label="活动名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button :label="1">启用</el-radio-button>
            <el-radio-button :label="0">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
    </el-card>

    <el-card class="sku-card" shadow="never">
      <template #header>
        <div class="card-head">
          <strong>活动商品</strong>
          <el-button :icon="Plus" type="primary" plain @click="openSkuDialog">选择商品</el-button>
        </div>
      </template>
      <el-table :data="activitySkus" empty-text="暂无活动商品">
        <el-table-column prop="productName" label="商品" min-width="150" />
        <el-table-column prop="skuName" label="SKU" min-width="150" />
        <el-table-column label="秒杀价" width="150">
          <template #default="{ row }"><el-input-number v-model="row.seckillPrice" :min="0.01" :precision="2" /></template>
        </el-table-column>
        <el-table-column label="库存" width="140">
          <template #default="{ row }"><el-input-number v-model="row.stockCount" :min="0" /></template>
        </el-table-column>
        <el-table-column label="限购" width="140">
          <template #default="{ row }"><el-input-number v-model="row.limitQuantity" :min="1" /></template>
        </el-table-column>
        <el-table-column label="排序" width="120">
          <template #default="{ row }"><el-input-number v-model="row.sort" :min="0" /></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-switch v-model="row.status" :active-value="1" :inactive-value="0" /></template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="saveSku(row)">保存</el-button>
            <el-button link type="danger" :icon="Delete" @click="removeSku(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="skuDialogVisible" title="选择秒杀商品" width="820px">
      <div class="dialog-search">
        <el-input v-model="skuKeyword" clearable placeholder="商品 / SKU 关键字" @keyup.enter="loadSelectableSkus" />
        <el-button :icon="Search" :loading="skuLoading" @click="loadSelectableSkus">搜索</el-button>
      </div>
      <el-table v-loading="skuLoading" :data="selectableSkus" highlight-current-row @current-change="chooseSku">
        <el-table-column prop="productName" label="商品" min-width="160" />
        <el-table-column prop="skuName" label="SKU" min-width="160" />
        <el-table-column prop="skuCode" label="SKU 编码" min-width="150" />
        <el-table-column prop="price" label="原价" width="100" />
        <el-table-column prop="availableStock" label="库存" width="100" />
      </el-table>
      <div class="sku-form">
        <el-input-number v-model="skuForm.seckillPrice" :min="0.01" :precision="2" />
        <el-input-number v-model="skuForm.stockCount" :min="0" />
        <el-input-number v-model="skuForm.limitQuantity" :min="1" />
        <el-input-number v-model="skuForm.sort" :min="0" />
      </div>
      <template #footer>
        <el-button @click="skuDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addSelectedSku">添加</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.admin-page {
  padding: 24px;
}

.page-title,
.card-head,
.actions,
.dialog-search,
.sku-form {
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

.activity-form {
  max-width: 760px;
}

.sku-card {
  margin-top: 18px;
}

.dialog-search {
  justify-content: flex-start;
  margin-bottom: 12px;
}

.dialog-search .el-input {
  max-width: 280px;
}

.sku-form {
  justify-content: flex-start;
  margin-top: 14px;
}
</style>
