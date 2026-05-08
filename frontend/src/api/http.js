import axios from 'axios'
import router from '../router'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
})

http.interceptors.request.use(config => {
  try {
    const isAuthRequest = config.url && config.url.startsWith('/auth/')
    const user = JSON.parse(localStorage.getItem('wms-user') || '{}')
    if (!isAuthRequest && user && user.token) {
      config.headers.Authorization = `Bearer ${user.token}`
    }
  } catch (error) {
    localStorage.removeItem('wms-user')
  }
  return config
})

http.interceptors.response.use(
  response => {
    const payload = response.data
    if (payload && payload.success === false) {
      return Promise.reject(new Error(payload.message || '\u8bf7\u6c42\u5931\u8d25'))
    }
    return payload
  },
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('wms-user')
      router.push('/login')
    }

    const message =
      error.response?.data?.message ||
      error.response?.data?.error ||
      (error.response?.status === 403 ? '\u65e0\u6743\u8bbf\u95ee\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55\u6216\u8054\u7cfb\u7ba1\u7406\u5458\u5f00\u901a\u6743\u9650' : '') ||
      error.message ||
      '\u8bf7\u6c42\u5931\u8d25'

    return Promise.reject(new Error(message))
  }
)

export default http
