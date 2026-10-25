<template>
  <div class="crud-page">
    <div class="page-head">
      <h2 class="page-head__title">充值 / 资金流水</h2>
    </div>

    <div class="card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="充值单据" name="recharge">
          <div class="list-toolbar list-toolbar--embedded">
            <div class="list-toolbar__filters">
              <el-input v-model="rechargeQuery.rechargeNo" placeholder="充值单号" aria-label="充值单号"
                        class="search-input" clearable @keyup.enter="searchRecharges"/>
              <el-input v-model="rechargeQuery.userId" placeholder="用户 ID" aria-label="用户 ID"
                        class="field-sm" clearable @keyup.enter="searchRecharges"/>
              <el-select v-model="rechargeQuery.status" placeholder="单据状态" aria-label="单据状态"
                         class="field-md" clearable>
                <el-option v-for="status in RECHARGE_STATUS_OPTIONS" :key="status" :label="status" :value="status"/>
              </el-select>
              <el-button type="primary" @click="searchRecharges">查 询</el-button>
              <el-button type="warning" @click="resetRecharges">重 置</el-button>
            </div>
          </div>

          <el-table v-loading="rechargeState.loading" stripe size="small" :data="rechargeItems">
            <el-table-column label="充值单号" prop="rechargeNo" min-width="170" show-overflow-tooltip/>
            <el-table-column label="用户" min-width="150">
              <template #default="scope">
                {{ scope.row.userName || '—' }}
                <span class="muted">#{{ scope.row.userId }}</span>
              </template>
            </el-table-column>
            <el-table-column label="金额（元）" width="110" align="right">
              <template #default="scope">¥{{ money(scope.row.amount) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="scope">
                <el-tag :type="getRechargeStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="申请时间" prop="createTime" min-width="150" show-overflow-tooltip/>
            <el-table-column label="完成时间" prop="finishTime" min-width="150" show-overflow-tooltip/>
            <el-table-column label="备注" prop="remark" min-width="140" show-overflow-tooltip/>
            <el-table-column label="审核" width="140" align="center">
              <template #default="scope">
                <template v-if="scope.row.status === '处理中'">
                  <el-button link type="success" :loading="callbacking === scope.row.id"
                             @click="review(scope.row, true)">通过</el-button>
                  <el-button link type="danger" :loading="callbacking === scope.row.id"
                             @click="review(scope.row, false)">拒绝</el-button>
                </template>
                <span v-else class="muted">已处理</span>
              </template>
            </el-table-column>
            <template #empty>
              <div class="empty-hint">{{ rechargeState.error || '暂无充值单据' }}</div>
            </template>
          </el-table>
          <div class="table-foot">
            <el-pagination
                v-model:current-page="rechargeState.pageNum"
                v-model:page-size="rechargeState.pageSize"
                :page-sizes="[5, 10, 15, 20, 50, 100]"
                background layout="total, sizes, prev, pager, next, jumper"
                :total="rechargeState.total"
                @current-change="recharges.setPage"
                @size-change="recharges.setPageSize"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="资金流水" name="flow">
          <div class="list-toolbar list-toolbar--embedded">
            <div class="list-toolbar__filters">
              <el-input v-model="flowQuery.userId" placeholder="用户 ID" aria-label="用户 ID"
                        class="field-sm" clearable @keyup.enter="searchFlows"/>
              <el-select v-model="flowQuery.source" placeholder="业务来源" aria-label="业务来源"
                         class="field-md" clearable>
                <el-option v-for="source in FUND_SOURCE_OPTIONS" :key="source" :label="source" :value="source"/>
              </el-select>
              <el-button type="primary" @click="searchFlows">查 询</el-button>
              <el-button type="warning" @click="resetFlows">重 置</el-button>
            </div>
          </div>

          <el-table v-loading="flowState.loading" stripe size="small" :data="flowItems">
            <el-table-column label="发生时间" prop="createTime" min-width="160" show-overflow-tooltip/>
            <el-table-column label="用户" min-width="150">
              <template #default="scope">
                {{ scope.row.userName || '—' }}
                <span class="muted">#{{ scope.row.userId }}</span>
              </template>
            </el-table-column>
            <el-table-column label="业务来源" prop="source" width="100" align="center"/>
            <el-table-column label="关联单据 ID" prop="relatedId" width="120" align="center"/>
            <el-table-column label="变动金额" width="130" align="right">
              <template #default="scope">
                <span :class="Number(scope.row.changeAmount) >= 0 ? 'flow-amount--in' : 'flow-amount--out'">
                  {{ Number(scope.row.changeAmount) >= 0 ? '+' : '' }}{{ money(scope.row.changeAmount) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="变动后余额" width="130" align="right">
              <template #default="scope">¥{{ money(scope.row.balanceAfter) }}</template>
            </el-table-column>
            <template #empty>
              <div class="empty-hint">{{ flowState.error || '暂无资金流水' }}</div>
            </template>
          </el-table>
          <div class="table-foot">
            <el-pagination
                v-model:current-page="flowState.pageNum"
                v-model:page-size="flowState.pageSize"
                :page-sizes="[5, 10, 15, 20, 50, 100]"
                background layout="total, sizes, prev, pager, next, jumper"
                :total="flowState.total"
                @current-change="flows.setPage"
                @size-change="flows.setPageSize"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { usePagedList } from '@/composables/usePagedList'
import {
  FUND_FLOW_API, FUND_SOURCE_OPTIONS, RECHARGE_API, RECHARGE_STATUS_OPTIONS,
  getRechargeStatusType, isSuccess,
} from '@/constants'

const activeTab = ref('recharge')

const rechargeQuery = reactive({ rechargeNo: '', userId: '', status: '' })
const flowQuery = reactive({ userId: '', source: '' })

const recharges = usePagedList({
  pageSize: 10,
  fetch: (params) => request.get(RECHARGE_API.PAGE, { params }),
})
const flows = usePagedList({
  pageSize: 10,
  fetch: (params) => request.get(FUND_FLOW_API.PAGE, { params }),
})
// 模板直接绑 items/state，按 usePagedList 的既有用法提到顶层才会自动解包 ref
const rechargeItems = recharges.items
const rechargeState = recharges.state
const flowItems = flows.items
const flowState = flows.state

const money = (value) => Number(value ?? 0).toFixed(2)

/** 同一时刻只处理一张单据；后端对已处理单据也会返回 409，这里先挡住双击 */
const callbacking = ref(null)

const review = async (row, success) => {
  if (callbacking.value) return
  callbacking.value = row.id
  try {
    const res = await request.post(RECHARGE_API.CALLBACK(row.id), {
      success,
      remark: success ? '管理员审核通过' : '管理员审核拒绝',
    })
    if (isSuccess(res)) {
      ElMessage.success(success ? '已通过并入账' : '已拒绝')
      recharges.reload(rechargeParams())
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (error) {
    // request.js 已统一提示
  } finally {
    callbacking.value = null
  }
}

function rechargeParams() {
  return {
    rechargeNo: rechargeQuery.rechargeNo || undefined,
    userId: rechargeQuery.userId || undefined,
    status: rechargeQuery.status || undefined,
  }
}
function flowParams() {
  return {
    userId: flowQuery.userId || undefined,
    source: flowQuery.source || undefined,
  }
}

const searchRecharges = () => recharges.reload(rechargeParams())
const resetRecharges = () => {
  Object.assign(rechargeQuery, { rechargeNo: '', userId: '', status: '' })
  return recharges.reload({})
}
const searchFlows = () => flows.reload(flowParams())
const resetFlows = () => {
  Object.assign(flowQuery, { userId: '', source: '' })
  return flows.reload({})
}

recharges.load()
flows.load()
</script>

<style scoped>
.list-toolbar--embedded {
  padding: 0 0 var(--space-16);
  border: 0;
  box-shadow: none;
}

.field-sm {
  width: 120px;
}

.flow-amount--in {
  color: var(--el-color-success);
  font-weight: var(--fw-bold);
}

.flow-amount--out {
  color: var(--el-color-danger);
  font-weight: var(--fw-bold);
}

.muted {
  color: var(--el-text-color-secondary);
  font-size: var(--fs-xs);
}
</style>

