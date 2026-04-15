<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="data.filmName" placeholder="请输入电影名称查询" style="width: 300px; margin-right:10px" :prefix-icon="Search"/>
      <el-button type="primary" @click="load">查 询</el-button>
      <el-button type="warning" @click="reset">重 置</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-button type="danger" @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55"/>
        <el-table-column label="用户名称" prop="userName"/>
        <el-table-column label="电影名称" prop="filmName" show-overflow-tooltip/>
        <el-table-column label="电影图片" prop="img">
          <template #default="scope">
            <el-image style="margin-top: 7px; width:36px;height:36px;border-radius:10%;object-fit:cover;
                      align-items:center;"
                      v-if="scope.row.img"
                      :src="scope.row.img"
                      :preview-src-list="[scope.row.img]"
                      preview-teleported/>
          </template>
        </el-table-column>
        <el-table-column label="用户评分" prop="mark"/>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button style="font-size: 18px" link :icon="Delete" @click="() => del(scope.row.id)" type="danger"></el-button>
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

interface MarkData {
  id?: number;
  userId?: number;
  filmId?: number;
  img?: string;
  mark?: string;
  filmName?: string; // 添加 filmName 到数据接口
}

interface UserData {
  id: number;
  name: string;
}

interface FilmData {
  id: number;
  title: string;
}

const data = reactive({
  tableData: [] as MarkData[],
  pageNumber: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  form: {} as MarkData,
  ids: [] as number[],
  UserData: [] as UserData[],
  FilmData: [] as FilmData[],
  mark: null,
  filmName: '', // 定义 filmName 属性
});

const loadUser = () => {
  request.get('/user/selectAll').then(res => {
    if(res.code === '200') {
      data.UserData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const loadFilm = () => {
  request.get('/film/selectAll').then(res => {
    if(res.code === '200') {
      data.FilmData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const load = () => {
  request.get('/mark/selectPage', {
    params: {
      pageNum: data.pageNumber,
      pageSize: data.pageSize,
      filmName: data.filmName, // 使用正确的属性名
    }
  }).then(res => {
    if (res && res.data) {
      data.tableData = res.data.list || [];
      data.total = res.data.total || 0;
    }
  }).catch(error => {
    console.error('加载数据失败:', error);
    ElMessage.error('加载数据失败，请重试');
  });
}

const del = (id: number) => {
  ElMessageBox.confirm('删除数据后无法恢复,您确认删除吗?', '删除确认', { type: 'warning' }).then(() => {
    request.delete(`/mark/delete/${id}`).then(res => {
      if (res.code === '200') {
        ElMessage.success('操作成功')
        load()
        data.formVisible = false
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch()
}

const delBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('请选择数据')
    return
  }
  ElMessageBox.confirm('删除数据后无法恢复,您确认删除吗?', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/mark/deleteBatch', { data: data.ids }).then(res => {
      if (res.code === '200') {
        ElMessage.success('操作成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch()
}

const handleSelectionChange = (rows: MarkData[]) => {
  data.ids = rows.map(row => row.id).filter((id): id is number => id !== undefined);
  console.log(data.ids);
}

const reset = () => {
  data.filmName = ''; // 重置正确的属性
  load();
}

// 初始加载
load()
loadUser()
loadFilm()
</script>

<style scoped>
</style>