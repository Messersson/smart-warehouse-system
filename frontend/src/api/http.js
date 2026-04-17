import axios from 'axios'
import router from '../router'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
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
      error.message ||
      '\u8bf7\u6c42\u5931\u8d25'

    return Promise.reject(new Error(message))
  }
)

export default http
