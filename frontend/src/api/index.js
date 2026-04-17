import http from './http'

export function get(url, params) {
  return http.get(url, { params })
}

export function post(url, data, config) {
  return http.post(url, data, config)
}

export function put(url, data) {
  return http.put(url, data)
}

export function remove(url) {
  return http.delete(url)
}
