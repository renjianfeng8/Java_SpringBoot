<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="searchForm.actor" placeholder="请输入演员名称查询" style="width: 300px; margin-right:10px" :prefix-icon="Search" />
      <el-button type="primary" @click="onSearch">查 询</el-button>
      <el-button type="warning" @click="onReset">重 置</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-button type="info" @click="openAdd">新 增</el-button>
      <el-button type="danger" @click="handleDelBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="dataList" @selection-change="onSelectionChange" empty-text="暂无数据">
        <el-table-column type="selection" width="55" />
        <el-table-column label="电影名称" prop="title" />
        <el-table-column label="电影图片" prop="img" >
          <template #default="scope">
            <el-image style="width:40px;height:40px;border-radius:5%;object-fit:cover;margin-top: 5px"
                      v-if="scope.row.img" :src="scope.row.img"
                      :preview-src-list="[scope.row.img]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column label="演员名称" prop="actor" />
        <el-table-column label="饰演角色" prop="figure" />
        <el-table-column label="演员照片" prop="picture" >
          <template #default="scope">
            <el-image style="width:40px;height:40px;border-radius:5%;object-fit:cover;margin-top: 5px"
                      v-if="scope.row.picture" :src="scope.row.picture"
                      :preview-src-list="[scope.row.picture]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column label="角色评级" prop="grade">
          <template #default="scope">
            <el-tag :type="getGradeType(scope.row.grade)">{{ scope.row.grade }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button style="font-size: 18px" link :icon="Edit" @click="openEdit(scope.row)" type="primary" />
            <el-button style="font-size: 18px" link :icon="Delete" @click="() => handleDel(scope.row.id)" type="danger" />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card" style="margin-bottom: 5px">
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

    <el-dialog v-model="dialogVisible" title="演职人员信息" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="rules" :model="form" style="padding-right: 50px;padding-top: 20px" label-width="80px">
        <el-form-item label="电影名称" prop="title">
          <el-input v-model="form.title" autocomplete="off" placeholder="请输入电影名称" />
        </el-form-item>
        <el-form-item label="电影图片" prop="img">
          <el-upload :action="baseUrl + '/api/v1/files/upload'" :on-success="handleMovieImgUpload"
                     :auto-upload="true" list-type="picture">
            <el-button type="primary">点击上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="演员名称" prop="actor">
          <el-input v-model="form.actor" autocomplete="off" placeholder="请输入演员名称" />
        </el-form-item>
        <el-form-item label="饰演角色" prop="figure">
          <el-input v-model="form.figure" autocomplete="off" placeholder="请输入饰演角色名称" />
        </el-form-item>
        <el-form-item label="演员照片" prop="picture">
          <el-upload :action="baseUrl + '/api/v1/files/upload'" :on-success="handleActorImgUpload"
                     :auto-upload="true" list-type="picture">
            <el-button type="primary">点击上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="所属角色" prop="grade">
          <el-select v-model="form.grade" placeholder="请选择所属角色">
            <el-option label="导演" value="导演" />
            <el-option label="主演" value="主演" />
            <el-option label="编剧" value="编剧" />
            <el-option label="二级演员" value="二级演员" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="close">取 消</el-button>
          <el-button type="primary" @click="submit">保 存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { Delete, Edit, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { useFormDialog } from '@/composables/useFormDialog'
import { API_PATHS } from '@/constants'

const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:9090'

const crud = useCrud(API_PATHS.ACTORS)
const { dataList, total, pageNum, pageSize, searchForm, selectedIds,
        del, delBatch, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = crud
const { dialogVisible, formRef, form, rules, openAdd, openEdit, submit, close } = useFormDialog(crud, {
  defaultForm: { title: '', actor: '', figure: '', picture: '', img: '', grade: '' },
  rules: {
    title: [{ required: true, message: '请输入电影名称', trigger: 'blur' }],
    actor: [{ required: true, message: '请输入主演名称', trigger: 'blur' }],
    figure: [{ required: true, message: '请输入饰演角色名称', trigger: 'blur' }],
    grade: [{ required: true, message: '请选择角色评级', trigger: 'change' }]
  }
})

crud.load()

function handleDel(id) {
  ElMessageBox.confirm('删除数据后无法恢复，您确认删除吗?', '删除确认', { type: 'warning' })
    .then(() => del(id)).catch()
}

function handleDelBatch() {
  if (!selectedIds.value.length) { ElMessage.warning('请先选择要删除的演职人员'); return }
  ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条数据吗？删除后无法恢复`, '删除确认', { type: 'warning' })
    .then(() => delBatch(selectedIds.value)).catch()
}

function handleMovieImgUpload(res) {
  if (res.code === '200') { form.img = res.data; ElMessage.success('电影图片上传成功') }
  else { ElMessage.error(res.msg || '电影图片上传失败') }
}

function handleActorImgUpload(res) {
  if (res.code === '200') { form.picture = res.data; ElMessage.success('主演照片上传成功') }
  else { ElMessage.error(res.msg || '主演照片上传失败') }
}

function getGradeType(grade) {
  switch (grade) {
    case '导演': return 'warning'
    case '主演': return 'success'
    case '编剧': return 'info'
    case '二级演员': return 'primary'
    default: return 'info'
  }
}
</script>

<style scoped>
</style>
