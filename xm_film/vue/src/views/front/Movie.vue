<template>
  <div style="width: 55%; margin:20px auto ">

    <div style="border: 1px solid #ccc; padding: 20px 30px; border-radius: 5px">

      <div style="display: flex; align-items: center; border-bottom: 1px solid #eee; padding: 15px 0">
        <div style="width: 60px; font-size: 16px">类型 :</div>
        <div style="flex: 1">
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

      <div style="display: flex; align-items: center; border-bottom: 1px solid #eee; padding: 15px 0">
        <div style="width: 60px; font-size: 16px">年代 :</div>
        <div style="flex: 1">
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

      <div style="display: flex; align-items: center; padding: 15px 0">
        <div style="width: 60px; font-size: 16px">区域 :</div>
        <div style="flex: 1">
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


    <div style="margin-top: 20px">
      <el-row :gutter="12">
        <el-col :span="6" v-for="item in data.filmData" style="margin-bottom: 25px;">
          <!-- 核心修改：添加点击事件跳转详情页 -->
          <img
              :src="item.img"
              alt=""
              style="width: 100%; height: 240px; border-radius: 5px; cursor: pointer"
              @click="goToFilmDetail(item.id)"
          >
          <div style="margin-top: 5px; font-size: 16px;font-weight: bold;font-style: italic">{{item.title}}</div>

          <div style="margin-top: 5px;display: flex; align-items: center">
            <div style="flex: 1">
              <el-tag :type="getStatusType(item.status)">
                {{ item.status }}
              </el-tag>
            </div>
            <div style="width: 80px; color: orange; font-size: 20px; text-align: right;">{{ item.score }} 分</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <div style="margin: 5px" v-if="data.total">
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
import { ElMessage } from "element-plus"; // 补充导入ElMessage
import { API_PATHS, apiPage } from '@/constants';

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
  status: null
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
    } else {
      ElMessage.error(res.msg)
    }
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

const getStatusType = (status) => {
  switch (status) {
    case '待上映': return 'warning';
    case '已上映': return 'success';
    case '停止上映': return 'danger';
    default: return 'info';
  }
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
  border: 1px solid #ccc;
  padding: 5px;
  margin: 5px;
  border-radius: 5px;
  text-align: center;
  cursor: pointer;
}

.item_active {
  background-color: #ef4238;
  color: white;
  border: none;
}
</style>


