<template>
  <div class="crud-page">
    <div class="page-head">
      <h2 class="page-head__title">影厅管理</h2>
    </div>

    <div class="card list-toolbar">
      <div class="list-toolbar__filters">
        <el-input v-model="searchForm.name" placeholder="请输入影厅名称" aria-label="影厅名称"
                  class="search-input" :prefix-icon="Search" @keyup.enter="onSearch" />
        <el-button type="primary" @click="onSearch">查 询</el-button>
        <el-button type="warning" @click="onReset">重 置</el-button>
      </div>
      <div class="list-toolbar__actions">
        <span v-if="selectedIds.length" class="selection-count" aria-live="polite">已选 {{ selectedIds.length }} 项</span>
        <el-button type="danger" :disabled="!selectedIds.length" @click="confirmDelBatch">批量删除</el-button>
      </div>
    </div>

    <div class="card table-card">
      <el-table v-loading="loading" stripe size="small" :data="dataList" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column label="影院名称" prop="title" />
        <el-table-column label="影厅名称" prop="name" />
        <el-table-column label="座位规模" width="120">
          <template #default="scope">{{ scope.row.seatRows }} 排 × {{ scope.row.seatCols }} 座</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="scope">
            <el-button class="row-action" link :icon="Delete" aria-label="删除" @click="confirmDel(scope.row.id)" type="danger" />
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-hint">{{ error ? '数据加载失败，请稍后重试' : '暂无数据' }}</div>
        </template>
      </el-table>
      <div class="table-foot">
        <el-pagination
            @size-change="onSizeChange"
            @current-change="onPageChange"
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[5, 10, 15, 20]"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { Delete, Search } from '@element-plus/icons-vue'
import { useCrud } from '@/composables/useCrud'
import { API_PATHS } from '@/constants'

const { dataList, total, pageNum, pageSize, searchForm, selectedIds, loading, error,
        confirmDel, confirmDelBatch, load, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = useCrud(API_PATHS.ROOMS)

load()
</script>
