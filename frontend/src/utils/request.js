import axios from 'axios'
import { ElMessage } from 'element-plus'

// 需要登录态失效的错误码（-1 通用失败；A02/A03 系列为 token 相关）
const AUTH_ERROR_CODES = ['A0230', 'A0300', 'A0301']

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // 请求的前缀
  timeout: 15000, // 请求的超时时间
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 在发送请求之前做些什么
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['token'] = token
    }
    return config
  },
  (error) => {
    // 对请求错误做些什么
    return Promise.reject(error)
  }
)

// 清空登录态并跳转登录页
const redirectToLogin = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  if (!window.location.pathname.startsWith('/auth/login')) {
    window.location.href = '/auth/login'
  }
}

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    const { data, config } = response
    // 业务成功：直接返回 data 部分
    if (data.code === '200') {
      return data.data
    }
    // 登录态失效
    if (AUTH_ERROR_CODES.includes(data.code)) {
      ElMessage.error(data.msg || '登录已过期，请重新登录')
      if (!config.url?.includes('/login')) {
        redirectToLogin()
      }
      return Promise.reject(data.msg || '登录已过期')
    }
    // 其他业务错误：交给调用方处理（保持返回完整 response 以兼容旧代码）
    return response
  },
  (error) => {
    // HTTP 层错误：401 统一按登录失效处理
    if (error.response && error.response.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      redirectToLogin()
    }
    return Promise.reject(error)
  }
)

export default service
