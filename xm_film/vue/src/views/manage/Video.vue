<template>
  <div class="crud-page">
    <div class="page-head">
      <h2 class="page-head__title">电影预告</h2>
      <div class="page-head__action">
        <el-button type="primary" :icon="VideoPlay" @click="$router.push('/manage/film')">去影片信息维护</el-button>
      </div>
    </div>

    <el-alert type="info" :closable="false" class="list-toolbar"
              title="预告片统一存在影片的 video 字段：TMDB 导入时自动带入，也可在「影片信息」里改或上传本地视频。前台详情页读的就是这里。" />

    <div class="card list-toolbar">
      <div class="list-toolbar__filters">
        <el-input v-model="searchForm.title" placeholder="请输入电影名称查询" aria-label="电影名称"
                  class="search-input" :prefix-icon="Search" @keyup.enter="onSearch" />
        <el-button type="primary" @click="onSearch">查 询</el-button>
        <el-button type="warning" @click="onReset">重 置</el-button>
      </div>
    </div>

    <div class="card table-card">
      <el-table v-loading="loading" stripe size="small" :data="dataList" row-key="id">
        <el-table-column label="电影名称" prop="title" />
        <el-table-column label="电影图片" prop="img">
          <template #default="scope">
            <el-image class="cell-thumb" v-if="scope.row.img" :src="scope.row.img" :preview-src-list="[scope.row.img]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column label="预告视频" prop="video" width="220">
          <!-- video 有两种形态：TMDB 导入的 YouTube 嵌入地址（须 iframe）与本地上传的 mp4（须 video） -->
          <template #default="scope">
            <iframe v-if="isEmbed(scope.row.video)" :src="scope.row.video" class="small-video"
                    title="预告视频" allowfullscreen></iframe>
            <video v-else-if="scope.row.video" :src="scope.row.video" class="small-video" controls></video>
          </template>
        </el-table-column>
        <el-table-column label="上映日期" prop="start" />
        <el-table-column label="操作" width="110">
          <template #default>
            <el-button class="row-action" link :icon="Edit" aria-label="编辑"
                       @click="$router.push('/manage/film')" type="primary" />
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-hint">{{ error ? '数据加载失败，请稍后重试' : '暂无已配置预告片的影片' }}</div>
        </template>
      </el-table>
      <div class="table-foot">
        <el-pagination
            @size-change="onSizeChange"
            @current-change="onPageChange"
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[5, 10, 15, 20, 50, 100]"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { API_PATHS } from '@/constants'
import { Edit, Search, VideoPlay } from '@element-plus/icons-vue'
import { useCrud } from '@/composables/useCrud'

// 读影片表：有预告片的排前面（SQL 排序），没有的也能看到并补上
const crud = useCrud(API_PATHS.FILMS)
const { dataList, total, pageNum, pageSize, searchForm, loading, error,
        onSearch, onReset, onPageChange, onSizeChange } = crud

crud.load()

// 与 TmdbClient.trailerUrl 落的地址同形（.../embed/<id>）
function isEmbed(url) {
  return !!url && url.includes('/embed/')
}
</script>

<style scoped>
.small-video {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
  aspect-ratio: 16 / 9;
}
</style>
