<script setup lang="ts">
import { Delete, Edit, Plus, Refresh, VideoPlay } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import {
  deleteAdminSeckillActivity,
  listAdminSeckillActivities,
  preheatAdminSeckillActivity,
  updateAdminSeckillActivityStatus,
  type AdminSeckillActivity
} from '@/api/admin/seckill';

const router = useRouter();
const loading = ref(false);
const activities = ref<AdminSeckillActivity[]>([]);
const total = ref(0);
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  status: undefined as number | undefined
});

onMounted(loadActivities);

async function loadActivities() {
  loading.value = true;
  try {
    const response = await listAdminSeckillActivities(query);
    if (!response.data.success) {
      throw new Error(response.data.message || '活动加载失败');
    }
    activities.value = response.data.data.records;
    total.value = response.data.data.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '活动加载失败');
  } finally {
    loading.value = false;
  }
}

async function changeStatus(row: AdminSeckillActivity) {
  const nextStatus = row.status === 1 ? 0 : 1;
  const response = await updateAdminSeckillActivityStatus(row.id, nextStatus);
  if (!response.data.success) {
    ElMessage.error(response.data.message || '状态更新失败');
    return;
  }
  ElMessage.success(nextStatus === 1 ? '已启用' : '已禁用');
  await loadActivities();
}

async function preheat(row: AdminSeckillActivity) {
  const response = await preheatAdminSeckillActivity(row.id);
  if (!response.data.success) {
    ElMessage.error(response.data.message || '预热失败');
    return;
  }
  ElMessage.success('活动库存已预热');
}

async function removeActivity(row: AdminSeckillActivity) {
  await ElMessageBox.confirm(`确认删除活动「${row.name}」？`, '删除秒杀活动', { type: 'warning' });
  const response = await deleteAdminSeckillActivity(row.id);
  if (!response.data.success) {
    ElMessage.error(response.data.message || '删除失败');
    return;
  }
  ElMessage.success('活动已删除');
  await loadActivities();
}
</script>

<template>
  <section class="admin-page">
    <div class="page-title">
      <div>
        <h1>秒杀管理</h1>
        <p>维护秒杀活动和活动商品。</p>
      </div>
      <el-button :icon="Plus" type="primary" @click="router.push('/admin/seckill/new')">新增活动</el-button>
    </div>

    <el-card shadow="never">
      <div class="filters">
        <el-input v-model="query.keyword" clearable placeholder="活动名称" @keyup.enter="loadActivities" />
        <el-select v-model="query.status" clearable placeholder="状态">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="loadActivities">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="activities" row-key="id">
        <el-table-column prop="id" label="ID" min-width="170" />
        <el-table-column prop="name" label="活动名称" min-width="180" />
        <el-table-column prop="startTime" label="开始时间" min-width="170" />
        <el-table-column prop="endTime" label="结束时间" min-width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="router.push(`/admin/seckill/${row.id}`)">编辑</el-button>
            <el-button link type="warning" @click="changeStatus(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
            <el-button link type="success" :icon="VideoPlay" @click="preheat(row)">预热</el-button>
            <el-button link type="danger" :icon="Delete" @click="removeActivity(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        class="pagination"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="loadActivities"
        @size-change="loadActivities"
      />
    </el-card>
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
</style>
