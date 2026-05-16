import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 创建 axios 实例并配置基础选项
 * - baseURL: API基础路径
 * - timeout: 请求超时时间（毫秒）
 */
const request = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:9090',
    timeout: 30000,
})

/**
 * 请求拦截器
 * - 设置请求头为 JSON 格式
 * - 可在此添加认证信息（如 token）
 * - 可添加加载动画开始逻辑
 */
request.interceptors.request.use(
    config => {
        config.headers['Content-Type'] = 'application/json;charset=utf-8';
        // 添加JWT令牌认证
        const userStr = localStorage.getItem('xm-pro-user');
        if (userStr) {
            try {
                const user = JSON.parse(userStr);
                if (user.token) {
                    config.headers['Authorization'] = 'Bearer ' + user.token;
                }
            } catch (e) {
                console.error('解析用户信息失败', e);
            }
        }
        return config;
    },
    error => {
        console.error('请求拦截器错误:', error);
        return Promise.reject(error);
    }
);

/**
 * 响应拦截器
 * - 处理返回数据格式转换
 * - 统一错误处理
 */
request.interceptors.response.use(
    response => {
        // 获取响应数据
        let res = response.data;
        // 处理返回的字符串数据（可能是 JSON 字符串）
        if (typeof res === 'string') {
            res = res ? JSON.parse(res) : res;
        }

        return res;
    },
    error => {
        // 获取错误状态码
        const status = error.response?.status;
        // 根据不同状态码进行错误提示
        switch (status) {
            case 404:
                ElMessage.error('未找到请求接口');
                break;
            case 500:
                ElMessage.error('系统异常,请检查后端控制台报错');
                break;
            case 401:
                ElMessage.warning('登录状态已过期，请重新登录');
                // 可在此添加跳转登录页逻辑
                break;
            case 403:
                ElMessage.error('权限不足，无法访问');
                break;
            default:
                console.error('请求错误:', error.message);
                ElMessage.error(`请求失败: ${error.message}`);
        }

        return Promise.reject(error);
    }
);

export default request;