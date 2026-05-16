<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input
          v-model="data.name"
      placeholder="请输入影厅名称"
          style="width: 300px; margin-right:10px"
          :prefix-icon="Search"
      />
      <el-button type="primary" @click="load">查 询</el-button>
      <el-button type="warning" @click="reset">重 置</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-button type="danger" @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table
          stripe
          :data="data.tableData"
          @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="影院名称" prop="title" />
        <el-table-column label="影厅名称" prop="name" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button
                style="font-size: 18px"
                link
                :icon="Delete"
                @click="() => del(scope.row.id)"
                type="danger"
            />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-pagination
          @size-change="load"
          @current-change="load"
          v-model:current-page="data.pageNumber"
          v-model:page-size="data.pageSize"
          :page-sizes="[5, 10, 15, 20]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="data.total"
      />
    </div>

  </div>
</template>

<script setup lang="ts">
import { reactive } from "vue";
import { Delete, Search } from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";

interface RoomForm {
  id?: number;
  title?: string;
  name?: string;
}


const data = reactive({
  tableData: [] as RoomForm[],
  pageNumber: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  name: '',
  form: {} as RoomForm, // 表单数据
  ids: [] as number[],
});


// 加载电影分类列表
const load = () => {
  request.get('/room/selectPage', {
    params: {
      pageNum: data.pageNumber,
      pageSize: data.pageSize,
      name: data.name
    }
  }).then(res => {
    if (res && res.data) {
      data.tableData = res.data.list || [];
      data.total = res.data.total || 0;
    }
  }).catch(error => {
    console.error('加载电影分类数据失败:', error);
    ElMessage.error('加载数据失败，请重试');
  });
}

// 重置查询条件
const reset = () => {
  data.name = '';
  load();
}

// 初始加载数据
load();



// 删除单个电影分类
const del = (id: number) => {
  ElMessageBox.confirm('删除数据后无法恢复，您确认删除吗?', '删除确认', { type: 'warning' })
      .then(() => {
        request.delete(`/room/delete/${id}`).then(res => {
          if (res.code === '200') {
            ElMessage.success('删除成功');
            load();
          } else {
            ElMessage.error(res.msg || '删除失败，请重试');
          }
        }).catch(() => {
          ElMessage.error('删除失败，请检查网络连接');
        });
      })
      .catch();
}

// 处理选中行变更
const handleSelectionChange = (rows: RoomForm[]) => {
  data.ids = rows.map(row => row.id).filter((id): id is number => id !== undefined);
}

// 批量删除电影分类
const delBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('请先选择要删除的分类');
    return;
  }
  ElMessageBox.confirm(`确定删除选中的 ${data.ids.length} 条数据吗？删除后无法恢复`, '删除确认', { type: 'warning' })
      .then(() => {
        request.delete('/room/deleteBatch', { data: data.ids }).then(res => {
          if (res.code === '200') {
            ElMessage.success('批量删除成功');
            load();
          } else {
            ElMessage.error(res.msg || '删除失败，请重试');
          }
        }).catch(() => {
          ElMessage.error('删除失败，请检查网络连接');
        });
      })
      .catch();
}

</script>

<style scoped>


</style>