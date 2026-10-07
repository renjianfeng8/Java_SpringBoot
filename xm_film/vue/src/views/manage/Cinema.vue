<template>
  <div class="crud-page">
    <div class="page-head">
      <h2 class="page-head__title">影院信息</h2>
      <div class="page-head__action">
        <el-button type="primary" :icon="Plus" @click="openAdd">新 增</el-button>
      </div>
    </div>

    <div class="card list-toolbar">
      <div class="list-toolbar__filters">
        <el-input v-model="searchForm.name" placeholder="请输入影院名称查询" aria-label="影院名称"
                  class="search-input" :prefix-icon="Search" @keyup.enter="onSearch" />
        <el-select v-model="searchForm.status" placeholder="请选择审核状态" aria-label="审核状态" class="field-md">
          <el-option v-for="status in CINEMA_STATUS_OPTIONS" :key="status" :label="status" :value="status" />
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
        <el-table-column type="expand">
          <template #default="props">
            <el-descriptions title="影院信息" :column="4" border>
              <el-descriptions-item label="账号">{{ props.row.username }}</el-descriptions-item>
              <el-descriptions-item label="角色">{{ props.row.role }}</el-descriptions-item>
              <el-descriptions-item label="头像">
                <el-image class="cell-thumb" :src="props.row.avatar"/>
              </el-descriptions-item>
              <el-descriptions-item label="电影院名称">{{ props.row.name }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ props.row.phone }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ props.row.email }}</el-descriptions-item>
              <el-descriptions-item label="影院地址">{{ props.row.address }}</el-descriptions-item>
              <el-descriptions-item label="负责人姓名">{{ props.row.leader }}</el-descriptions-item>
              <el-descriptions-item label="身份证号">
                <el-popover placement="top-start" title="身份证号码" :width="200" trigger="hover" :content="props.row.code">
                  <template #reference>
                    <div class="line line--code">{{ props.row.code }}</div>
                  </template>
                </el-popover>
              </el-descriptions-item>
              <el-descriptions-item label="营业执照">
                <el-image class="cell-thumb" :src="props.row.certificate"/>
              </el-descriptions-item>
              <el-descriptions-item label="审核状态">
                <el-tag :type="getStatusType(props.row.status)">{{ props.row.status || '未知状态' }}</el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </template>
        </el-table-column>
        <el-table-column label="账号" prop="username"/>
        <el-table-column label="头像">
          <template #default="scope">
            <el-image class="cell-avatar"
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
            <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.status || '未知状态' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" prop="role">
          <template #default="scope">
            <el-tag>{{scope.row.role}}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="scope">
            <div class="row-actions">
              <el-button v-if="scope.row.status !== CINEMA_STATUS.APPROVED" link type="success"
                         @click="approve(scope.row)">审核通过</el-button>
              <el-button class="row-action" link :icon="Edit" aria-label="编辑" @click="openEdit(scope.row)" type="primary"></el-button>
              <el-button class="row-action" link :icon="Delete" aria-label="删除" @click="confirmDel(scope.row.id)" type="danger"></el-button>
            </div>
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

    <el-dialog v-model="dialogVisible" title="影院信息" width="500" destroy-on-close>
      <el-form ref="formRef" :rules="rules" :model="form" class="dialog-form" label-width="85px"
               status-icon @submit.prevent>
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" autocomplete="off" placeholder="请输入账号" @keyup.enter="submit"/>
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <el-upload :action="FILE_UPLOAD_URL" :on-success="handleFileUpload"
                     :auto-upload="true" list-type="picture">
            <el-button type="primary">点击上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="影院名称" prop="name">
          <el-input v-model="form.name" autocomplete="off" placeholder="请输入影院名称" @keyup.enter="submit"/>
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" autocomplete="off" placeholder="请输入电话" @keyup.enter="submit"/>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" autocomplete="off" placeholder="请输入邮箱" @keyup.enter="submit"/>
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input type="textarea" v-model="form.address" autocomplete="off" placeholder="请输入影院地址"/>
        </el-form-item>
        <el-form-item label="负责人" prop="leader">
          <el-input v-model="form.leader" autocomplete="off" placeholder="请输入负责人姓名" @keyup.enter="submit"/>
        </el-form-item>
        <el-form-item label="身份证号" prop="code">
          <el-input v-model="form.code" autocomplete="off" placeholder="请输入负责人身份证号" @keyup.enter="submit"/>
        </el-form-item>
        <el-form-item label="营业执照" prop="certificate">
          <el-upload :action="FILE_UPLOAD_URL" :on-success="handleCertificateUpload" list-type="picture">
            <el-button type="primary">上传影院的营业执照</el-button>
          </el-upload>
        </el-form-item>
        <!-- 新增时状态由后端固定为「未审核」，故仅在编辑时暴露审核状态 -->
        <el-form-item v-if="form.id" label="审核状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择审核状态" class="field-full">
            <el-option v-for="s in CINEMA_STATUS_OPTIONS" :key="s" :label="s" :value="s" />
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
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { useFormDialog } from '@/composables/useFormDialog'
import request from '@/utils/request'
import {
  API_PATHS, CINEMA_STATUS, CINEMA_STATUS_OPTIONS, FILE_UPLOAD_URL,
  getCinemaStatusType as getStatusType
} from '@/constants'

const crud = useCrud(API_PATHS.CINEMAS)
const { dataList, total, pageNum, pageSize, searchForm, selectedIds, loading, error,
        confirmDel, confirmDelBatch, onSearch, onReset, onPageChange, onSizeChange, onSelectionChange } = crud
const { dialogVisible, formRef, form, rules, openAdd, openEdit, submit, close } = useFormDialog(crud, {
  defaultForm: { username: '', name: '', phone: '', email: '', address: '', leader: '', code: '', certificate: '', avatar: '', status: CINEMA_STATUS.UNAUDITED },
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

const route = useRoute()

/* 待办卡带着 ?status=未审核 跳进来，这里把它预置成筛选项。
   必须 watch 参数本身而不能只写在顶层：站内跳转同页换参不会重挂组件（规则 83），
   从首页反复点待办卡时只有第一次会生效。immediate 让首屏也走同一条路径，
   于是 crud.load() 不再是独立的第二次取数。 */
watch(() => route.query.status, (status) => {
  if (status) searchForm.status = status
  onSearch()
}, { immediate: true })

function handleFileUpload(res) {
  if (res.code === '200') { form.avatar = res.data; ElMessage.success('头像上传成功') }
  else { ElMessage.error(res.msg || '头像上传失败') }
}

function handleCertificateUpload(res) {
  if (res.code === '200') { form.certificate = res.data; ElMessage.success('营业执照上传成功') }
  else { ElMessage.error(res.msg || '营业执照上传失败') }
}

// 审核通过：只提交状态，后端按 id 局部更新（影院信息其余字段不变）
function approve(row) {
  ElMessageBox.confirm(`确认通过「${row.name}」的影院审核吗？通过后该影院会在前台可见。`, '审核确认', { type: 'warning' })
    .then(async () => {
      try {
        const res = await request.put(API_PATHS.CINEMAS, { id: row.id, status: CINEMA_STATUS.APPROVED })
        if (res.code === '200') {
          ElMessage.success('审核通过')
          await crud.load()
        } else {
          ElMessage.error(res.msg || '审核失败')
        }
      } catch (error) {
        // request.js 已提示后端返回的错误信息
      }
    })
    .catch(() => {})
}
</script>
