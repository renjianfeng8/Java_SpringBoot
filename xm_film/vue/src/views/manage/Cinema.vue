<template>
  <div>
    <div class="card" style="margin-bottom: 5px">
      <el-input v-model="searchForm.name" placeholder="请输入影院名称查询" style="width: 300px; margin-right:10px" :prefix-icon="Search"/>
      <el-button type="primary" @click="onSearch">查 询</el-button>
      <el-button type="warning" @click="onReset">重 置</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-button type="info" @click="openAdd">新 增</el-button>
      <el-button type="danger" @click="handleDelBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="dataList" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="50"/>
        <el-table-column type="expand">
          <template #default="props">
            <el-descriptions title="影院信息" :column="4" border>
              <el-descriptions-item label="账号">{{ props.row.username }}</el-descriptions-item>
              <el-descriptions-item label="角色">{{ props.row.role }}</el-descriptions-item>
              <el-descriptions-item label="头像">
                <el-image style="width:36px;height:36px;object-fit:cover;" :src="props.row.avatar"/>
              </el-descriptions-item>
              <el-descriptions-item label="电影院名称">{{ props.row.name }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ props.row.phone }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ props.row.email }}</el-descriptions-item>
              <el-descriptions-item label="影院地址">{{ props.row.address }}</el-descriptions-item>
              <el-descriptions-item label="负责人姓名">{{ props.row.leader }}</el-descriptions-item>
              <el-descriptions-item label="身份证号">
                <el-popover placement="top-start" title="身份证号码" :width="200" trigger="hover" :content="props.row.code">
                  <template #reference>
                    <div class="line" style="width: 50px">{{ props.row.code }}</div>
                  </template>
                </el-popover>
              </el-descriptions-item>
              <el-descriptions-item label="营业执照">
                <el-image style="width:36px;height:36px;object-fit:cover;" :src="props.row.certificate"/>
              </el-descriptions-item>
              <el-descriptions-item label="审核状态">
                <el-tag :type="getStatusType(getTransformedStatus(props.row.status))">
                  {{ getTransformedStatus(props.row.status) }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </template>
        </el-table-column>
        <el-table-column label="账号" prop="username"/>
        <el-table-column label="头像">
          <template #default="scope">
            <el-image style="margin-top: 5px;width:40px;height:40px;border-radius:50%;object-fit:cover;"
                      v-if="scope.row.avatar" :src="scope.row.avatar"
                      :preview-src-list="[scope.row.avatar]" preview-teleported/>
          </template>
        </el-table-column>
        <el-table-column label="影院名称" prop="name" />
        <el-table-column label="电话" prop="phone" show-overflow-tooltip/>
        <el-table-column label="邮箱" prop="email" show-overflow-tooltip/>
        <el-table-column label="地址" prop="address" show-overflow-tooltip/>
        <el-table-column label="审核状态" prop="status">
          <template #default="scope">
            <el-tag :type="getStatusType(getTransformedStatus(scope.row.status))">
              {{ getTransformedStatus(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" prop="role">
          <template #default="scope">
            <el-tag>{{scope.row.role}}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button style="font-size: 18px" link :icon="Edit" @click="openEdit(scope.row)" type="primary"></el-button>
            <el-button style="font-size: 18px" link :icon="Delete" @click="() => handleDel(scope.row.id)" type="danger"></el-button>
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

    <el-dialog v-model="dialogVisible" title="影院信息" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="rules" :model="form" style="padding-right: 50px;padding-top: 20px" label-width="85px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" autocomplete="off" placeholder="请输入账号"/>
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <el-upload :action="FILE_UPLOAD_URL" :on-success="handleFileUpload"
                     :auto-upload="true" list-type="picture">
            <el-button type="primary">点击上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="影院名称" prop="name">
          <el-input v-model="form.name" autocomplete="off" placeholder="请输入影院名称"/>
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" autocomplete="off" placeholder="请输入电话"/>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" autocomplete="off" placeholder="请输入邮箱"/>
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input type="textarea" v-model="form.address" autocomplete="off" placeholder="请输入影院地址"/>
        </el-form-item>
        <el-form-item label="负责人" prop="leader">
          <el-input v-model="form.leader" autocomplete="off" placeholder="请输入负责人姓名"/>
        </el-form-item>
        <el-form-item label="身份证号" prop="code">
          <el-input v-model="form.code" autocomplete="off" placeholder="请输入负责人身份证号"/>
        </el-form-item>
        <el-form-item label="营业执照" prop="certificate">
          <el-upload :action="FILE_UPLOAD_URL" :on-success="handleCertificateUpload" list-type="picture">
            <el-button type="primary">上传影院的营业执照</el-button>
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

const crud = useCrud(API_PATHS.CINEMAS)
const { dataList, total, pageNum, pageSize, searchForm, selectedIds,
        del, delBatch, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = crud
const { dialogVisible, formRef, form, rules, openAdd, openEdit, submit, close } = useFormDialog(crud, {
  defaultForm: { username: '', name: '', phone: '', email: '', address: '', leader: '', code: '', certificate: '', avatar: '' },
  rules: {
    username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
    name: [{ required: true, message: '请输入影院名称', trigger: 'blur' }],
    email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }],
    address: [{ required: true, message: '请输入影院地址', trigger: 'blur' }],
    leader: [{ required: true, message: '请输入负责人姓名', trigger: 'blur' }],
    code: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
    certificate: [{ required: true, message: '请上传营业执照', trigger: 'change' }]
  }
})

crud.load()

function handleDel(id) {
  ElMessageBox.confirm('删除数据后无法恢复,您确认删除吗?', '删除确认', { type: 'warning' })
    .then(() => del(id)).catch()
}

function handleDelBatch() {
  if (!selectedIds.value.length) { ElMessage.warning('请选择数据'); return }
  ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条数据吗？`, '删除确认', { type: 'warning' })
    .then(() => delBatch(selectedIds.value)).catch()
}

function handleFileUpload(res) {
  if (res.code === '200') { form.avatar = res.data; ElMessage.success('头像上传成功') }
  else { ElMessage.error(res.msg || '头像上传失败') }
}

function handleCertificateUpload(res) {
  if (res.code === '200') { form.certificate = res.data; ElMessage.success('营业执照上传成功') }
  else { ElMessage.error(res.msg || '营业执照上传失败') }
}

function getTransformedStatus(status) {
  if (!status) return '未知状态'
  if (status === '未审核') return '待审核'
  if (status === '审核通过') return '已审核'
  return status
}

function getStatusType(status) {
  if (!status) return 'info'
  switch (status) {
    case '未审核': return 'warning'
    case '待审核': return 'info'
    case '已审核': return 'success'
    case '审核拒绝': return 'danger'
    case '审核通过': return 'success'
    case '已审批': return 'warning'
    default: return 'info'
  }
}
</script>

<style scoped>
.line {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
