<template>
  <div class="layout-shell">
    <aside class="layout-sidebar">
      <div class="brand-block">
        <div class="brand-title">仓储管理平台</div>
        <div class="brand-subtitle">运营、预警、驿站、柜机与扫码一体化</div>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        class="layout-menu"
        background-color="transparent"
        text-color="#dbeafe"
        active-text-color="#ffffff"
      >
        <el-menu-item v-if="canAccess('/dashboard')" index="/dashboard">驾驶舱</el-menu-item>
        <el-submenu v-if="visibleMasterMenus.length" index="master">
          <template slot="title">基础资料</template>
          <el-menu-item v-for="item in visibleMasterMenus" :key="item.path" :index="item.path">
            {{ item.label }}
          </el-menu-item>
        </el-submenu>
        <el-menu-item v-if="canAccess('/inbounds')" index="/inbounds">入库管理</el-menu-item>
        <el-menu-item v-if="canAccess('/station-inbound', ['/inbounds'])" index="/station-inbound">驿站/柜机入库</el-menu-item>
        <el-menu-item v-if="canAccess('/pda', ['/inbounds', '/outbounds', '/stocks'])" index="/pda">PDA扫码台</el-menu-item>
        <el-menu-item v-if="canAccess('/outbounds')" index="/outbounds">出库管理</el-menu-item>
        <el-menu-item v-if="canAccess('/stocks')" index="/stocks">库存管理</el-menu-item>
        <el-menu-item v-if="canAccess('/alerts')" index="/alerts">预警中心</el-menu-item>
        <el-menu-item v-if="canAccess('/permissions')" index="/permissions">权限管理</el-menu-item>
        <el-menu-item v-if="canAccess('/stock-takes')" index="/stock-takes">盘点管理</el-menu-item>
        <el-menu-item v-if="canAccess('/billing')" index="/billing">计费结算</el-menu-item>
        <el-menu-item v-if="canAccess('/approvals')" index="/approvals">审批中心</el-menu-item>
        <el-menu-item v-if="canAccess('/exceptions')" index="/exceptions">异常工单</el-menu-item>
      </el-menu>
    </aside>

    <div class="layout-main">
      <header class="layout-header">
        <div>
          <div class="header-title">{{ $route.meta.title || '仓储管理平台' }}</div>
          <div class="header-subtitle">覆盖仓库作业、驿站履约、柜机滞留预警、短信反馈与手持扫码。</div>
        </div>
        <div class="header-user">
          <span>{{ currentUser.displayName || currentUser.username || '管理员' }}</span>
          <el-button type="text" @click="logout">退出登录</el-button>
        </div>
      </header>

      <main class="layout-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script>
const masterMenus = [
  { path: '/master/warehouses', label: '仓库管理' },
  { path: '/master/locations', label: '库位管理' },
  { path: '/master/owners', label: '货主管理' },
  { path: '/master/suppliers', label: '供应商管理' },
  { path: '/master/customers', label: '客户管理' },
  { path: '/master/products', label: '商品管理' }
]

export default {
  name: 'MainLayout',
  computed: {
    currentUser() {
      try {
        return JSON.parse(localStorage.getItem('wms-user') || '{}')
      } catch (error) {
        return {}
      }
    },
    menuPaths() {
      return this.currentUser.menuPaths || []
    },
    visibleMasterMenus() {
      return masterMenus.filter(item => this.canAccess(item.path))
    }
  },
  methods: {
    canAccess(path, fallbacks = []) {
      if (!this.menuPaths.length) {
        return true
      }
      if (this.menuPaths.includes(path)) {
        return true
      }
      return fallbacks.some(item => this.menuPaths.includes(item))
    },
    logout() {
      localStorage.removeItem('wms-user')
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.layout-shell {
  display: flex;
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.18), transparent 30%),
    radial-gradient(circle at bottom right, rgba(34, 197, 94, 0.12), transparent 25%),
    #f3f7fb;
}

.layout-sidebar {
  width: 260px;
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%);
  color: #fff;
  padding: 20px 16px;
  box-shadow: 12px 0 28px rgba(15, 23, 42, 0.12);
}

.brand-block {
  padding: 10px 12px 20px;
}

.brand-title {
  font-size: 24px;
  font-weight: 700;
}

.brand-subtitle {
  font-size: 12px;
  color: #93c5fd;
  margin-top: 6px;
}

.layout-menu {
  border-right: none;
}

.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.layout-header {
  height: 80px;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
}

.header-title {
  font-size: 22px;
  font-weight: 700;
}

.header-subtitle {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.layout-content {
  padding: 24px;
}

@media (max-width: 960px) {
  .layout-shell {
    flex-direction: column;
  }

  .layout-sidebar {
    width: 100%;
  }
}
</style>
