import Vue from 'vue'
import Router from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import MainLayout from '../layouts/MainLayout.vue'
import Dashboard from '../views/Dashboard.vue'
import ResourcePage from '../views/ResourcePage.vue'
import Inbounds from '../views/Inbounds.vue'
import Outbounds from '../views/Outbounds.vue'
import StationInbound from '../views/StationInbound.vue'
import Pda from '../views/Pda.vue'
import Stocks from '../views/Stocks.vue'
import Alerts from '../views/Alerts.vue'
import Permissions from '../views/Permissions.vue'
import StockTakes from '../views/StockTakes.vue'
import Billing from '../views/Billing.vue'
import Approvals from '../views/Approvals.vue'
import Exceptions from '../views/Exceptions.vue'

Vue.use(Router)

const routes = [
  {
    path: '/login',
    component: Login,
    meta: { public: true }
  },
  {
    path: '/register',
    component: Register,
    meta: { public: true }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: Dashboard, meta: { title: '驾驶舱' } },
      { path: 'master/warehouses', component: ResourcePage, meta: { title: '仓库管理', resource: 'warehouses' } },
      { path: 'master/locations', component: ResourcePage, meta: { title: '库位管理', resource: 'locations' } },
      { path: 'master/owners', component: ResourcePage, meta: { title: '货主管理', resource: 'owners' } },
      { path: 'master/suppliers', component: ResourcePage, meta: { title: '供应商管理', resource: 'suppliers' } },
      { path: 'master/customers', component: ResourcePage, meta: { title: '客户管理', resource: 'customers' } },
      { path: 'master/products', component: ResourcePage, meta: { title: '商品管理', resource: 'products' } },
      { path: 'inbounds', component: Inbounds, meta: { title: '入库管理' } },
      {
        path: 'station-inbound',
        component: StationInbound,
        meta: { title: '驿站/柜机入库', accessFallback: ['/inbounds'] }
      },
      {
        path: 'pda',
        component: Pda,
        meta: { title: 'PDA扫码台', accessFallback: ['/inbounds', '/outbounds', '/stocks'] }
      },
      { path: 'outbounds', component: Outbounds, meta: { title: '出库管理' } },
      { path: 'stocks', component: Stocks, meta: { title: '库存管理' } },
      { path: 'alerts', component: Alerts, meta: { title: '预警中心' } },
      { path: 'permissions', component: Permissions, meta: { title: '权限管理' } },
      { path: 'stock-takes', component: StockTakes, meta: { title: '盘点管理' } },
      { path: 'billing', component: Billing, meta: { title: '计费结算' } },
      { path: 'approvals', component: Approvals, meta: { title: '审批中心' } },
      { path: 'exceptions', component: Exceptions, meta: { title: '异常工单' } }
    ]
  }
]

const router = new Router({
  mode: 'hash',
  routes
})

function getCurrentUser() {
  try {
    return JSON.parse(localStorage.getItem('wms-user') || '{}')
  } catch (error) {
    return {}
  }
}

function normalizeFallback(meta) {
  if (!meta || !meta.accessFallback) {
    return []
  }
  return Array.isArray(meta.accessFallback) ? meta.accessFallback : [meta.accessFallback]
}

function hasRouteAccess(to, menuPaths) {
  if (!menuPaths.length) {
    return true
  }
  if (menuPaths.includes(to.path)) {
    return true
  }
  return normalizeFallback(to.meta).some(path => menuPaths.includes(path))
}

router.beforeEach((to, from, next) => {
  if (to.meta.public) {
    next()
    return
  }

  const user = getCurrentUser()
  if (!user || !user.token) {
    next('/login')
    return
  }

  const menuPaths = user.menuPaths || []
  if (!hasRouteAccess(to, menuPaths)) {
    next(menuPaths[0] || '/dashboard')
    return
  }

  next()
})

export default router
