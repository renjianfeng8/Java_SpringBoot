<!--
  我的账户：余额 + 充值申请 + 充值单据 + 资金流水。

  充值流程刻意分成两步，与真实支付网关一致：
    1. 提交充值申请 → 生成「处理中」单据，余额不变
    2. 触发模拟支付回调（正式环境由支付平台异步回调）→ 成功才入账
-->
<template>
  <div class="account-page">
    <el-card shadow="never" class="balance-card">
      <div class="balance-row">
        <div>
          <div class="balance-label">账户余额（元）</div>
          <div class="balance-value">¥{{ money(balance) }}</div>
        </div>
        <el-button link type="primary" @click="loadAll">刷 新</el-button>
      </div>
      <div class="balance-tip">
        余额为模拟数据。充值需先提交充值单据，再由模拟支付回调入账；回调失败则余额不变。
      </div>
    </el-card>

    <el-card shadow="never" class="section">
      <template #header><span class="section-title">账户充值</span></template>

      <div class="tier-row">
        <el-button v-for="tier in TIERS"
                   :key="tier"
                   :type="Number(amount) === tier ? 'primary' : 'default'"
                   @click="amount = tier">
          {{ tier }} 元
        </el-button>
      </div>

      <div class="amount-row">
        <span class="amount-label">充值金额</span>
        <el-input-number v-model="amount"
                         :min="0.01"
                         :max="50000"
                         :precision="2"
                         :step="10"
                         style="width: 180px" />
        <el-button type="primary" :loading="submitting" @click="submitRecharge">
          提交充值申请
        </el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="section">
      <template #header><span class="section-title">我的充值单据</span></template>

      <el-table v-loading="loadingRecharges" stripe :data="recharges">
        <el-table-column label="充值单号" prop="rechargeNo" show-overflow-tooltip />
        <el-table-column label="金额（元）" width="110">
          <template #default="scope">¥{{ money(scope.row.amount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getRechargeStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" prop="createTime" show-overflow-tooltip />
        <el-table-column label="完成时间" prop="finishTime" show-overflow-tooltip />
        <el-table-column label="备注" prop="remark" show-overflow-tooltip />
        <el-table-column label="模拟支付回调" width="180">
          <template #default="scope">
            <!-- 只有「处理中」单据可被回调；已处理单据后端也会拒绝重复回调 -->
            <template v-if="scope.row.status === '处理中'">
              <el-button link type="success" @click="triggerCallback(scope.row, true)">模拟成功</el-button>
              <el-button link type="danger" @click="triggerCallback(scope.row, false)">模拟失败</el-button>
            </template>
            <span v-else class="muted">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="section">
      <template #header><span class="section-title">资金流水</span></template>

      <el-table v-loading="loadingFlows" stripe :data="flows">
        <el-table-column label="发生时间" prop="createTime" show-overflow-tooltip />
        <el-table-column label="业务来源" prop="source" width="100" />
        <el-table-column label="变动金额" width="120">
          <template #default="scope">
            <span :style="{ color: Number(scope.row.changeAmount) >= 0 ? '#67c23a' : '#f56c6c' }">
              {{ Number(scope.row.changeAmount) >= 0 ? '+' : '' }}{{ money(scope.row.changeAmount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="变动前余额" width="120">
          <template #default="scope">¥{{ money(scope.row.balanceBefore) }}</template>
        </el-table-column>
        <el-table-column label="变动后余额" width="120">
          <template #default="scope">¥{{ money(scope.row.balanceAfter) }}</template>
        </el-table-column>
        <el-table-column label="关联单据ID" prop="relatedId" width="110" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import request from '@/utils/request.js';
import { ACCOUNT_API, FUND_FLOW_API, RECHARGE_API, getRechargeStatusType } from '@/constants';

/** 快捷档位；与自由输入并存，二者都受后端金额校验约束 */
const TIERS = [20, 30, 50, 100];

const balance = ref(0);
const recharges = ref([]);
const flows = ref([]);
const amount = ref(50);
const submitting = ref(false);
const loadingRecharges = ref(false);
const loadingFlows = ref(false);

const money = (value) => Number(value ?? 0).toFixed(2);

const loadSummary = async () => {
  try {
    const res = await request.get(ACCOUNT_API.SUMMARY);
    if (res.code === '200') {
      balance.value = res.data?.balance ?? 0;
    }
  } catch (error) {
    // request.js 已统一提示
  }
};

const loadRecharges = async () => {
  loadingRecharges.value = true;
  try {
    const res = await request.get(RECHARGE_API.PAGE, { params: { pageNum: 1, pageSize: 50 } });
    if (res.code === '200') {
      recharges.value = res.data?.list || [];
    }
  } catch (error) {
    // request.js 已统一提示
  } finally {
    loadingRecharges.value = false;
  }
};

const loadFlows = async () => {
  loadingFlows.value = true;
  try {
    const res = await request.get(FUND_FLOW_API.PAGE, { params: { pageNum: 1, pageSize: 50 } });
    if (res.code === '200') {
      flows.value = res.data?.list || [];
    }
  } catch (error) {
    // request.js 已统一提示
  } finally {
    loadingFlows.value = false;
  }
};

const loadAll = async () => {
  await Promise.all([loadSummary(), loadRecharges(), loadFlows()]);
};

const submitRecharge = async () => {
  if (submitting.value) {
    return;
  }
  const value = Number(amount.value);
  if (!value || value <= 0) {
    ElMessage.warning('请输入大于 0 的充值金额');
    return;
  }
  submitting.value = true;
  try {
    const res = await request.post(RECHARGE_API.CREATE, { amount: value });
    if (res.code === '200') {
      ElMessage.success('充值申请已提交，等待支付回调入账');
      await loadAll();
    } else {
      ElMessage.error(res.msg || '提交失败');
    }
  } catch (error) {
    // request.js 已统一提示
  } finally {
    submitting.value = false;
  }
};

/** 演示环境手动触发回调；正式环境由支付网关调用同一端点 */
const triggerCallback = async (order, success) => {
  try {
    const res = await request.post(RECHARGE_API.CALLBACK(order.id), {
      success,
      remark: success ? '' : '模拟支付回调失败',
    });
    if (res.code === '200') {
      ElMessage.success(success ? '回调成功，充值已入账' : '回调失败，单据已置为已失败');
      await loadAll();
    } else {
      ElMessage.error(res.msg || '回调处理失败');
    }
  } catch (error) {
    // request.js 已统一提示
  }
};

onMounted(loadAll);
</script>

<style scoped>
.account-page {
  width: 85%;
  margin: 20px auto;
}

.balance-card {
  margin-bottom: 12px;
  border-radius: 8px;
}

.balance-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.balance-label {
  font-size: 14px;
  color: #666;
}

.balance-value {
  font-size: 32px;
  font-weight: bold;
  color: #ef4238;
  margin-top: 4px;
}

.balance-tip {
  margin-top: 10px;
  font-size: 13px;
  color: #999;
}

.section {
  margin-bottom: 12px;
  border-radius: 8px;
}

.section-title {
  font-weight: bold;
}

.tier-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.amount-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.amount-label {
  font-size: 14px;
  color: #666;
}

.muted {
  color: #999;
  font-size: 13px;
}
</style>
