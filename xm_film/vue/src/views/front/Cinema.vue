<template>
  <div style="width: 55%; margin: 20px auto;">
    <!-- 影院列表 -->
    <div style="padding: 15px;">
      <!-- 循环渲染每个影院 -->
      <div v-for="(cinema, index) in data.filmData" :key="index"
           style="border: 1px solid #e0e0e0; border-radius: 5px; padding: 15px; margin-bottom: 15px;display: flex;">
        <!-- 左侧图片 -->
        <div style="width: 200px; flex-shrink: 0;">
          <img :src="cinema.avatar " alt="影院图片" style="width: 100%; height: 140px; border-radius: 5px">
        </div>
        <!-- 右侧信息区域 -->
        <div style="flex: 1; margin-left: 15px">
          <!-- 影院名称 -->
          <div style="font-size: 18px; font-weight: bold; color: #333;">{{ cinema.name }}</div>

          <!-- 影院服务标签（4个标签，不同颜色） -->
          <div style="margin-top: 10px; display: flex; gap: 8px; flex-wrap: wrap;">
            <!-- 退票无忧：红色系 -->
            <div style="padding: 3px 10px; border-radius: 12px; font-size: 12px; color: #fff; background-color: #ef4238;">
              退票无忧
            </div>
            <!-- 儿童优惠：橙色系 -->
            <div style="padding: 3px 10px; border-radius: 12px; font-size: 12px; color: #fff; background-color: #fa8c16;">
              儿童优惠
            </div>
            <!-- WiFi覆盖：蓝色系 -->
            <div style="padding: 3px 10px; border-radius: 12px; font-size: 12px; color: #fff; background-color: #1890ff;">
              WiFi覆盖
            </div>
            <!-- 免费停车：绿色系 -->
            <div style="padding: 3px 10px; border-radius: 12px; font-size: 12px; color: #fff; background-color: #52c41a;">
              免费停车
            </div>
          </div>

          <!-- 详细信息 -->
          <div style="margin-bottom: 15px; color: #666;">

            <div style="margin: 8px 0; display: flex; align-items: center;">
              <div style="margin-right: 8px; color: #ef4238;">电话:</div>
              <div>{{ cinema.phone }}</div>
            </div>

            <div style="margin: 8px 0; display: flex; align-items: center;">
              <div style="margin-right: 8px; color: #ef4238;">邮箱:</div>
              <div>{{ cinema.email }}</div>
            </div>

            <div style="margin: 8px 0; display: flex; align-items: center;">
              <div style="margin-right: 8px; color: #ef4238;">地址:</div>
              <div>{{ cinema.address }}</div>
            </div>
          </div>

        </div>

      </div>
    </div>

    <!-- 分页组件 -->
    <div style="margin: 5px;padding: 5px" v-if="data.total">
      <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :page-sizes="[5, 10, 15]"
          background
          layout="total, prev, pager, next"
          :total="data.total"
      />
    </div>
  </div>
</template>

<script setup>
import {reactive} from 'vue';
import request from "@/utils/request.js";
import {ElMessage} from 'element-plus';

const data = reactive({
  cinemaData: null,
  pageNum: 1,
  pageSize: 12,
  total: 0,
  filmData: [], // 存储影院数据
  status: null
})

// 加载影院数据
const load = () => {
  request.get('cinema/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      cinemaId: data.cinemaData,
    }
  }).then(res => {
    if (res.code === '200') {
      data.filmData = res.data.list
      data.total = res.data.total
    } else {
      ElMessage.error(res.msg)
    }
  }).catch(err => {
    ElMessage.error('数据加载失败，请稍后重试')
    console.error(err)
  })
}

// 处理每页条数变化
const handleSizeChange = (newSize) => {
  data.pageSize = newSize;
  data.pageNum = 1; // 重置为第一页
  load();
}

// 处理页码变化
const handleCurrentChange = (newPage) => {
  data.pageNum = newPage;
  load();
};

// 初始加载数据
load()
</script>