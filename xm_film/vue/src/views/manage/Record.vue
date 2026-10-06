<template>
  <div class="crud-page">
    <div class="page-head">
      <h2 class="page-head__title">放映记录</h2>
    </div>

    <div class="card list-toolbar">
      <div class="list-toolbar__filters">
        <el-input v-model="searchForm.title" placeholder="请输入电影名称查询" aria-label="电影名称"
                  class="search-input" :prefix-icon="Search" @keyup.enter="onSearch"/>
        <el-input v-model="searchForm.start" placeholder="按放映日期查询 (YYYY-MM-DD)" aria-label="放映日期"
                  class="search-input" :prefix-icon="Search" @keyup.enter="onSearch"/>
        <el-select v-model="searchForm.status" placeholder="请选择放映状态" aria-label="放映状态" class="field-md">
          <el-option label="正常" value="正常" />
          <el-option label="停售" value="停售" />
        </el-select>
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
        <el-table-column type="selection" width="55"/>
        <el-table-column label="影院名称" prop="cinemaName"/>
        <el-table-column label="影厅名称" prop="roomName"/>
        <el-table-column label="电影名称" prop="title"/>
        <el-table-column label="放映时间" prop="start" />
        <el-table-column label="电影票价 (元)" prop="price" />
        <el-table-column label="放映状态" prop="status">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="scope">
            <el-button class="row-action" link :icon="Delete" aria-label="删除" @click="confirmDel(scope.row.id)" type="danger"></el-button>
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
import { API_PATHS, getRecordStatusType as getStatusType } from '@/constants'

// 后端 selectAll 已 JOIN 出 cinemaName / roomName，前端不再自行拼接。
// 本页没有弹窗表单，用不到 crud 对象本身，直接解构（与 Room / Mark 同形）。
const { dataList, total, pageNum, pageSize, searchForm, selectedIds, loading, error,
        confirmDel, confirmDelBatch, load, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = useCrud(API_PATHS.RECORDS)

load()
</script>
