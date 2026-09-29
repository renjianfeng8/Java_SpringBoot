<template>
  <div class="page-narrow">

    <div class="filter-panel">

      <div class="filter-row">
        <div class="filter-label">类型 :</div>
        <div class="filter-options">
          <el-row :gutter="10">
            <el-col :span="3">
              <div class="item_style" :class="{'item_active' : !data.typeFlag}" @click="changeTypeFlag(null)">全部</div>
            </el-col>
            <el-col :span="3" v-for="item in data.typeData">
              <div class="item_style" :class="{'item_active' : data.typeFlag === item.id}" @click="changeTypeFlag(item.id)">{{ item.title }}</div>
            </el-col>
          </el-row>
        </div>
      </div>

      <div class="filter-row">
        <div class="filter-label">年代 :</div>
        <div class="filter-options">
          <el-row :gutter="10">
            <el-col :span="3">
              <div class="item_style" :class="{'item_active' : !data.yearFlag}" @click="changeYearFlag(null)">全部</div>
            </el-col>
            <el-col :span="3" v-for="item in data.yearData">
              <div class="item_style" :class="{'item_active' : data.yearFlag === item}" @click="changeYearFlag(item)">{{ item }}</div>
            </el-col>
          </el-row>
        </div>
      </div>

      <div class="filter-row filter-row--last">
        <div class="filter-label">区域 :</div>
        <div class="filter-options">
          <el-row :gutter="10">
            <el-col :span="3">
              <div class="item_style" :class="{'item_active' : !data.areaFlag}" @click="changeAreaFlag(null)">全部</div>
            </el-col>
            <el-col :span="3" v-for="item in data.areaData">
              <div class="item_style" :class="{'item_active' : data.areaFlag === item.id}" @click="changeAreaFlag(item.id)">{{ item.title }}</div>
            </el-col>
          </el-row>
        </div>
      </div>

    </div>


    <div class="film-grid">
      <el-row :gutter="12">
        <el-col :span="6" class="film-grid__col" v-for="item in data.filmData">
          <!-- 核心修改：添加点击事件跳转详情页 -->
          <img
              :src="item.img"
              alt=""
              class="film-card__poster"
              @click="goToFilmDetail(item.id)"
          >
          <div class="film-card__title">{{item.title}}</div>

          <div class="film-card__meta">
            <div class="film-card__status">
              <el-tag :type="getStatusType(item.status)">
                {{ item.status }}
              </el-tag>
            </div>
            <div class="film-card__score">{{ formatScore(item.score) }}</div>
          </div>
        </el-col>
      </el-row>
      <div v-if="data.error" class="empty-hint">数据加载失败，请稍后重试</div>
      <div v-else-if="!data.filmData.length" class="empty-hint">暂无数据</div>
    </div>

    <div class="film-grid__pagination" v-if="data.total">
      <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :page-sizes="[12, 24, 36]"
          background
          layout="total, prev, pager, next"
          :total="data.total"
      />
    </div>

  </div>
</template>

<script setup>
import { reactive } from "vue"; // 补充导入reactive
import { useRouter } from "vue-router"; // 导入路由
import request from "@/utils/request.js";
import { formatScore } from '@/utils/format.js';
import { ElMessage } from "element-plus"; // 补充导入ElMessage
import { API_PATHS, apiPage, getFilmStatusType as getStatusType } from '@/constants';

// 初始化路由实例
const router = useRouter();

const data = reactive({
  typeFlag: null,
  yearFlag: null,
  areaFlag: null,
  typeData: [],
  areaData: [],
  yearData: [],
  pageNum: 1,
  pageSize: 12,
  total: 0,
  filmData: [],
  status: null,
  error: false
})

// 新增：跳转电影详情页方法
const goToFilmDetail = (filmId) => {
  // 校验ID是否存在，避免跳转错误页面
  if (!filmId) {
    ElMessage.warning("电影ID不存在，无法查看详情");
    return;
  }
  // 跳转到详情页，拼接电影ID
  router.push(`/front/filmDetail/${filmId}`);
}

const load = () => {
  request.get(apiPage(API_PATHS.FILMS),{
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      typeId: data.typeFlag,
      areaId: data.areaFlag,
      year: data.yearFlag
    }
  }).then(res => {
    if (res.code === '200') {
      data.filmData = res.data.list
      data.total = res.data.total
      data.error = false
    } else {
      data.error = true
      ElMessage.error(res.msg)
    }
  }).catch(err => {
    // 网络异常的统一提示由 request.js 的响应拦截器给出，这里只落错误态
    console.error('电影列表接口请求异常：', err)
    data.error = true
  })
}

const loadType = () => {
  request.get(API_PATHS.TYPES).then(res => {
    if (res.code === '200') {
      data.typeData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const loadArea = () => {
  request.get(API_PATHS.AREAS).then(res => {
    if (res.code === '200') {
      data.areaData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const loadYear = () => {
  request.get(API_PATHS.YEARS).then(res => {
    if (res.code === '200') {
      data.yearData = res.data
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const changeTypeFlag = (id) => {
  data.typeFlag = id
  load()
}

const changeYearFlag = (year) => {
  data.yearFlag = year
  load()
}

const changeAreaFlag = (id) => {
  data.areaFlag = id
  load()
}


const handleSizeChange = (newSize) => {
  data.pageSize = newSize;
  load();
}

// 处理页码变化
const handleCurrentChange = (newPage) => {
  data.pageNum = newPage;
  load();
};

loadType()
loadArea()
loadYear()
load()
</script>

<style scoped>
.item_style {
  padding: var(--space-4);
  margin: var(--space-4);
  border: 1px solid var(--el-border-color-darker);
  border-radius: var(--el-border-radius-base);
  text-align: center;
  cursor: pointer;
}

/* 选中态承载文字，用主色（白字 5.58:1，达 AA） */
.item_active {
  border: none;
  background-color: var(--el-color-primary);
  color: var(--color-on-accent);
}

.film-grid {
  margin-top: var(--space-20);
}

.film-grid__col {
  margin-bottom: var(--space-24);
}

.film-grid__pagination {
  margin: var(--space-4);
}

.film-card__poster {
  width: 100%;
  height: 240px;
  border-radius: var(--el-border-radius-base);
  object-fit: cover;
  cursor: pointer;
}

.film-card__title {
  margin-top: var(--space-4);
  font-size: var(--fs-md);
  font-weight: var(--fw-bold);
  font-style: italic;
}

.film-card__meta {
  display: flex;
  align-items: center;
  margin-top: var(--space-4);
}

.film-card__status {
  flex: 1;
}

/* 评分承载文字，用白底评分文字色（4.68:1） */
.film-card__score {
  width: 80px;
  font-size: var(--fs-xl);
  color: var(--color-rating-text);
  text-align: right;
}

/* 无数据 / 加载失败的占位（规范 §608：禁止用假数据填充，无数据渲染「暂无数据」） */
.empty-hint {
  padding: var(--space-40) 0;
  font-size: var(--fs-base);
  color: var(--el-text-color-regular);
  text-align: center;
}
</style>
