<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { getSignInYearlyProfile, type SignInMonthProfile, type SignInYearlyProfile } from '@/api/user';
import SiteHeader from '@/components/SiteHeader.vue';

const router = useRouter();
const loading = ref(false);
const profile = ref<SignInYearlyProfile | null>(null);
const selectedYear = ref(new Date().getFullYear());

const totalDaysInYear = computed(() => profile.value?.months.reduce((total, month) => total + month.daysInMonth, 0) || 365);
const signedRate = computed(() => {
  if (!profile.value) {
    return 0;
  }
  return Math.round((profile.value.yearSignedCount / totalDaysInYear.value) * 100);
});

onMounted(() => {
  loadYearlyProfile();
});

async function loadYearlyProfile() {
  loading.value = true;
  try {
    const response = await getSignInYearlyProfile(selectedYear.value);
    if (!response.data.success) {
      throw new Error(response.data.message || '年度签到记录加载失败');
    }
    profile.value = response.data.data;
    selectedYear.value = response.data.data.year;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '年度签到记录加载失败');
  } finally {
    loading.value = false;
  }
}

function buildMonthCells(month: SignInMonthProfile) {
  return Array.from({ length: month.daysInMonth }, (_, index) => {
    const day = index + 1;
    const signed = month.signedDays.includes(day);
    return {
      day,
      signed,
      level: signed ? ((day % 4) + 1) : 0
    };
  });
}

function monthTitle(month: SignInMonthProfile) {
  return `${profile.value?.year || selectedYear.value}年${month.month}月${month.signedCount}天`;
}
</script>

<template>
  <SiteHeader />

  <main v-loading="loading" class="yearly-page page-shell">
    <section class="yearly-head">
      <button class="back-button" type="button" @click="router.push('/profile')">
        <el-icon><ArrowLeft /></el-icon>
        返回个人主页
      </button>
      <div>
        <span>年度签到详情</span>
        <h1>{{ profile?.year || selectedYear }} 年签到热力图</h1>
        <p>按 12 个月 Redis Bitmap 汇总展示，不新增 MySQL 签到明细表。</p>
      </div>
    </section>

    <section class="yearly-summary">
      <div>
        <span>年度签到</span>
        <strong>{{ profile?.yearSignedCount || 0 }}</strong>
        <em>天</em>
      </div>
      <div>
        <span>全年天数</span>
        <strong>{{ totalDaysInYear }}</strong>
        <em>天</em>
      </div>
      <div>
        <span>完成率</span>
        <strong>{{ signedRate }}</strong>
        <em>%</em>
      </div>
    </section>

    <section class="yearly-month-grid" aria-label="年度签到热力图">
      <article v-for="month in profile?.months || []" :key="month.month" class="yearly-month-card">
        <div class="month-card-head">
          <h2>{{ month.month }}月</h2>
          <span>{{ month.signedCount }} / {{ month.daysInMonth }} 天</span>
        </div>
        <div class="yearly-heatmap">
          <span
            v-for="cell in buildMonthCells(month)"
            :key="cell.day"
            class="yearly-sign-cell"
            :class="`level-${cell.level}`"
            :title="`${monthTitle(month)} ${cell.day}日：${cell.signed ? '已签到' : '未签到'}`"
          />
        </div>
      </article>
    </section>
  </main>
</template>

<style scoped>
.yearly-page {
  padding: 28px 0 56px;
}

.yearly-head {
  display: grid;
  gap: 18px;
  margin-bottom: 22px;
  padding: 28px;
  color: #fff;
  background: linear-gradient(135deg, #ff4d00, #ff8a34);
  border-radius: 8px;
}

.yearly-head span {
  display: inline-block;
  padding: 4px 10px;
  color: #ff4d00;
  background: #fff;
  border-radius: 999px;
  font-weight: 800;
}

.yearly-head h1 {
  margin: 12px 0 6px;
  font-size: 34px;
}

.yearly-head p {
  margin: 0;
  opacity: 0.92;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: fit-content;
  padding: 8px 12px;
  color: #fff;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 999px;
  cursor: pointer;
}

.yearly-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 22px;
}

.yearly-summary div {
  padding: 22px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(20, 32, 54, 0.06);
}

.yearly-summary span {
  display: block;
  color: #777;
  font-size: 14px;
}

.yearly-summary strong {
  color: #ff4d00;
  font-size: 36px;
}

.yearly-summary em {
  margin-left: 4px;
  color: #777;
  font-style: normal;
}

.yearly-month-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.yearly-month-card {
  min-width: 0;
  padding: 18px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(20, 32, 54, 0.06);
}

.month-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.month-card-head h2 {
  margin: 0;
  font-size: 18px;
}

.month-card-head span {
  color: #777;
  font-size: 13px;
  white-space: nowrap;
}

.yearly-heatmap {
  display: grid;
  grid-template-columns: repeat(7, 16px);
  grid-auto-rows: 16px;
  gap: 7px;
  padding: 14px;
  background: #f7f8fb;
  border-radius: 8px;
}

.yearly-sign-cell {
  display: inline-block;
  width: 16px;
  height: 16px;
  background: #ebedf0;
  border-radius: 4px;
}

.level-1 {
  background: #ffd8c8;
}

.level-2 {
  background: #ffad85;
}

.level-3 {
  background: #ff7a3d;
}

.level-4 {
  background: #ff4d00;
}

@media (max-width: 1000px) {
  .yearly-month-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .yearly-summary,
  .yearly-month-grid {
    grid-template-columns: 1fr;
  }

  .yearly-head h1 {
    font-size: 28px;
  }
}
</style>
