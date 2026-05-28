<script setup lang="ts">
import { Delete, Edit, Location, Plus } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import {
  createAddress,
  deleteAddress,
  listAddresses,
  updateAddress,
  type AddressItem,
  type AddressRequest
} from '@/api/address';
import SiteHeader from '@/components/SiteHeader.vue';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const editingAddressId = ref<number | null>(null);
const addresses = ref<AddressItem[]>([]);
const formRef = ref<FormInstance>();

const form = reactive<AddressRequest>({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  postalCode: '',
  defaultFlag: 0
});

const rules: FormRules<AddressRequest> = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [{ required: true, message: '请输入收货人手机号', trigger: 'blur' }],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区县', trigger: 'blur' }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
};

const dialogTitle = computed(() => (editingAddressId.value ? '编辑收货地址' : '新增收货地址'));
const userId = computed(() => authStore.user?.id);

onMounted(async () => {
  if (!authStore.user) {
    await authStore.fetchCurrentUser().catch(async () => {
      await router.replace({ path: '/login', query: { redirect: '/addresses' } });
    });
  }
  await loadAddresses();
});

async function loadAddresses() {
  if (!userId.value) {
    return;
  }
  loading.value = true;
  try {
    const response = await listAddresses(userId.value);
    if (!response.data.success) {
      throw new Error(response.data.message || '获取收货地址失败');
    }
    addresses.value = response.data.data;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取收货地址失败');
  } finally {
    loading.value = false;
  }
}

function openCreateDialog() {
  editingAddressId.value = null;
  resetForm();
  dialogVisible.value = true;
}

function openEditDialog(address: AddressItem) {
  editingAddressId.value = address.id;
  Object.assign(form, toRequest(address));
  dialogVisible.value = true;
}

async function submitAddress() {
  if (!userId.value) {
    return;
  }
  await formRef.value?.validate();
  submitting.value = true;
  try {
    const request = normalizeRequest(form);
    const response = editingAddressId.value
      ? await updateAddress(userId.value, editingAddressId.value, request)
      : await createAddress(userId.value, request);
    if (!response.data.success) {
      throw new Error(response.data.message || '保存收货地址失败');
    }
    ElMessage.success('收货地址已保存');
    dialogVisible.value = false;
    await loadAddresses();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存收货地址失败');
  } finally {
    submitting.value = false;
  }
}

async function setDefaultAddress(address: AddressItem) {
  if (!userId.value || address.defaultFlag === 1) {
    return;
  }
  try {
    const response = await updateAddress(userId.value, address.id, {
      ...toRequest(address),
      defaultFlag: 1
    });
    if (!response.data.success) {
      throw new Error(response.data.message || '设置默认地址失败');
    }
    ElMessage.success('已设为默认地址');
    await loadAddresses();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '设置默认地址失败');
  }
}

async function removeAddress(address: AddressItem) {
  if (!userId.value) {
    return;
  }
  await ElMessageBox.confirm(`确认删除 ${address.receiverName} 的收货地址吗？`, '删除收货地址', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  });
  const response = await deleteAddress(userId.value, address.id);
  if (!response.data.success) {
    ElMessage.error(response.data.message || '删除收货地址失败');
    return;
  }
  ElMessage.success('收货地址已删除');
  await loadAddresses();
}

function resetForm() {
  Object.assign(form, {
    receiverName: '',
    receiverPhone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    postalCode: '',
    defaultFlag: addresses.value.length === 0 ? 1 : 0
  });
  formRef.value?.clearValidate();
}

function toRequest(address: AddressItem): AddressRequest {
  return {
    receiverName: address.receiverName,
    receiverPhone: address.receiverPhone,
    province: address.province,
    city: address.city,
    district: address.district,
    detailAddress: address.detailAddress,
    postalCode: address.postalCode,
    defaultFlag: address.defaultFlag
  };
}

function normalizeRequest(request: AddressRequest): AddressRequest {
  return {
    receiverName: request.receiverName.trim(),
    receiverPhone: request.receiverPhone.trim(),
    province: request.province.trim(),
    city: request.city.trim(),
    district: request.district.trim(),
    detailAddress: request.detailAddress.trim(),
    postalCode: request.postalCode?.trim() || null,
    defaultFlag: request.defaultFlag
  };
}
</script>

<template>
  <SiteHeader />

  <main class="address-page page-shell">
    <section class="address-head">
      <div>
        <span>个人中心</span>
        <h1>收货地址</h1>
        <p>管理下单时使用的收货人、手机号和详细地址。</p>
      </div>
      <el-button :icon="Plus" type="primary" @click="openCreateDialog">新增地址</el-button>
    </section>

    <section v-loading="loading" class="address-list">
      <el-empty v-if="!loading && addresses.length === 0" description="暂无收货地址">
        <el-button type="primary" @click="openCreateDialog">新增第一个地址</el-button>
      </el-empty>

      <article v-for="address in addresses" :key="address.id" class="address-card">
        <div class="address-main">
          <div class="receiver-row">
            <strong>{{ address.receiverName }}</strong>
            <span>{{ address.receiverPhone }}</span>
            <el-tag v-if="address.defaultFlag === 1" type="warning">默认地址</el-tag>
          </div>
          <p>
            <el-icon><Location /></el-icon>
            {{ address.province }} {{ address.city }} {{ address.district }} {{ address.detailAddress }}
          </p>
          <small v-if="address.postalCode">邮编：{{ address.postalCode }}</small>
        </div>

        <div class="address-actions">
          <el-button v-if="address.defaultFlag !== 1" text type="primary" @click="setDefaultAddress(address)">
            设为默认
          </el-button>
          <el-button :icon="Edit" text @click="openEditDialog(address)">编辑</el-button>
          <el-button :icon="Delete" text type="danger" @click="removeAddress(address)">删除</el-button>
        </div>
      </article>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" maxlength="64" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" maxlength="20" placeholder="请输入收货人手机号" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="form.province" maxlength="64" placeholder="例如：浙江省" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="form.city" maxlength="64" placeholder="例如：杭州市" />
        </el-form-item>
        <el-form-item label="区县" prop="district">
          <el-input v-model="form.district" maxlength="64" placeholder="例如：西湖区" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input v-model="form.detailAddress" maxlength="255" placeholder="街道、小区、门牌号" type="textarea" />
        </el-form-item>
        <el-form-item label="邮编">
          <el-input v-model="form.postalCode" maxlength="20" placeholder="可选" />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="form.defaultFlag" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="submitting" type="primary" @click="submitAddress">保存</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<style scoped>
.address-page {
  padding: 28px 0 56px;
}

.address-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 28px;
  color: #fff;
  background: linear-gradient(135deg, #ff4d00, #ff8a34);
  border-radius: 8px;
}

.address-head span {
  font-weight: 800;
  opacity: 0.9;
}

.address-head h1 {
  margin: 8px 0;
  font-size: 34px;
}

.address-head p {
  margin: 0;
  opacity: 0.92;
}

.address-head :deep(.el-button) {
  --el-button-bg-color: #fff;
  --el-button-border-color: #fff;
  --el-button-text-color: #ff4d00;
  --el-button-hover-bg-color: #fff7f2;
  --el-button-hover-border-color: #fff7f2;
  font-weight: 800;
}

.address-list {
  display: grid;
  min-height: 260px;
  gap: 14px;
  margin-top: 18px;
}

.address-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 20px;
  align-items: center;
  padding: 22px;
  background: #fff;
  border-radius: 8px;
}

.receiver-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.receiver-row strong {
  font-size: 20px;
}

.receiver-row span {
  color: #555;
}

.address-main p {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 12px 0 6px;
  color: #333;
  line-height: 1.7;
}

.address-main .el-icon {
  margin-top: 4px;
  color: #ff4d00;
}

.address-main small {
  color: #888;
}

.address-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 760px) {
  .address-head,
  .address-card {
    grid-template-columns: 1fr;
  }

  .address-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .address-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
