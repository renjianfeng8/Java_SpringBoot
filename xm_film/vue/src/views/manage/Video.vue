<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="searchForm.title" placeholder="请输入电影名称查询" style="width: 300px; margin-right:10px" :prefix-icon="Search" />
      <el-button type="primary" @click="onSearch">查 询</el-button>
      <el-button type="warning" @click="onReset">重 置</el-button>
    </div>
    <div class="card" style="margin-bottom: 5px">
      <el-button type="info" @click="openAdd">新 增</el-button>
      <el-button type="danger" @click="handleDelBatch">批量删除</el-button>
    </div>
    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="dataList" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="70" />
        <el-table-column label="电影名称" prop="title" />
        <el-table-column label="电影图片" prop="img">
          <template #default="scope">
            <el-image style="margin-top:7px;width:36px;height:36px;border-radius:10%;object-fit:cover" v-if="scope.row.img" :src="scope.row.img" :preview-src-list="[scope.row.img]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column label="视频名称" prop="name" />
        <el-table-column label="预告视频" prop="preview" width="220">
          <template #default="scope">
            <div v-if="scope.row.preview" class="video-wrapper">
              <video :src="scope.row.preview" class="small-video" controls />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" prop="start" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button style="font-size: 18px" link :icon="Edit" @click="openEdit(scope.row)" type="primary" />
            <el-button style="font-size: 18px" link :icon="Delete" @click="handleDel(scope.row.id)" type="danger" />
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" style="margin-bottom: 5px">
      <el-pagination @size-change="onSizeChange" @current-change="onPageChange" v-model:current-page="pageNum" v-model:page-size="pageSize" :page-sizes="[5, 10, 15, 20]" background layout="total, sizes, prev, pager, next, jumper" :total="total" />
    </div>
    <el-dialog v-model="dialogVisible" title="电影预告视频" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="rules" :model="form" style="padding-right:50px;padding-top:20px" label-width="85px">
        <el-form-item label="电影名称" prop="title">
          <el-input v-model="form.title" autocomplete="off" placeholder="请输入电影名称" />
        </el-form-item>
        <el-form-item label="电影图片" prop="img">
          <el-upload :action="uploadUrl" :on-success="handleFileUpload" :auto-upload="true" list-type="picture">
            <el-button type="primary">点击上传电影图片</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="视频名称" prop="name">
          <el-input v-model="form.name" autocomplete="off" placeholder="请输入视频名称" />
        </el-form-item>
        <el-form-item label="发布时间" prop="start">
          <el-date-picker v-model="form.start" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="预告视频" prop="preview">
          <el-upload :action="uploadUrl" :on-success="handleVideoUpload" :auto-upload="true" list-type="text">
            <el-button type="primary">点击上传视频</el-button>
          </el-upload>
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
import { API_PATHS, FILE_UPLOAD_URL } from '@/constants'

const uploadUrl = FILE_UPLOAD_URL

const crud = useCrud(API_PATHS.VIDEOS)
const { dataList, total, pageNum, pageSize, searchForm, selectedIds, del, delBatch, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = crud
const { dialogVisible, formRef, form, rules, openAdd, openEdit, submit, close } = useFormDialog(crud, {
  defaultForm: { title: '', name: '', img: '', preview: '', start: '' },
  rules: {
    title: [{ required: true, message: '请输入电影名称', trigger: 'blur' }],
    start: [{ required: true, message: '请选择上映日期', trigger: 'change' }]
  }
})

crud.load()

function handleFileUpload(res) {
  if (res.code === '200') { form.img = res.data; ElMessage.success('图片上传成功') }
  else { ElMessage.error(res.msg || '图片上传失败') }
}

function handleVideoUpload(res) {
  if (res.code === '200') { form.preview = res.data; ElMessage.success('视频上传成功') }
  else { ElMessage.error(res.msg || '视频上传失败') }
}

function handleDel(id) {
  ElMessageBox.confirm('删除数据后无法恢复，您确认删除吗?', '删除确认', { type: 'warning' }).then(() => del(id)).catch()
}

function handleDelBatch() {
  if (!selectedIds.length) return
  ElMessageBox.confirm(`确定删除选中的 ${selectedIds.length} 条数据吗？`, '删除确认', { type: 'warning' }).then(() => delBatch(selectedIds)).catch()
}
</script>

<style scoped>
.small-video { width: 100%; height: 100%; object-fit: cover; cursor: pointer; }
.card { box-shadow: 0 1px 4px rgba(0,21,41,.08); border-radius: 4px; padding: 10px; background-color: #fff; }
</style>
