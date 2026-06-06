<template>
  <div class="layout-shell">
    <aside class="layout-sidebar">
      <div class="brand-block">
        <div class="brand-mark">W</div>
        <div>
          <div class="brand-title">仓储管理平台</div>
          <div class="brand-subtitle">WMS Enterprise</div>
        </div>
      </div>

      <el-menu
        :default-active="$route.path"
        router
        class="layout-menu"
        background-color="#0b1220"
        text-color="#b7c0cf"
        active-text-color="#ffffff"
      >
        <el-menu-item v-if="canAccess('/dashboard')" index="/dashboard">
          <i class="el-icon-s-home"></i>
          <span slot="title">驾驶舱</span>
        </el-menu-item>
        <el-submenu v-if="visibleMasterMenus.length" index="master">
          <template slot="title">
            <i class="el-icon-menu"></i>
            <span>基础资料</span>
          </template>
          <el-menu-item v-for="item in visibleMasterMenus" :key="item.path" :index="item.path">
            {{ item.label }}
          </el-menu-item>
        </el-submenu>
        <el-menu-item v-if="canAccess('/inbounds')" index="/inbounds">
          <i class="el-icon-download"></i>
          <span slot="title">入库管理</span>
        </el-menu-item>
        <el-menu-item v-if="canAccess('/cargo-code-records', ['/inbounds'])" index="/cargo-code-records">
          <i class="el-icon-printer"></i>
          <span slot="title">码记录打印</span>
        </el-menu-item>
        <el-menu-item v-if="canAccess('/station-inbound', ['/inbounds'])" index="/station-inbound">
          <i class="el-icon-office-building"></i>
          <span slot="title">驿站入库</span>
        </el-menu-item>
        <el-menu-item v-if="canAccess('/pda', ['/inbounds', '/outbounds', '/stocks'])" index="/pda">
          <i class="el-icon-full-screen"></i>
          <span slot="title">PDA 扫码</span>
        </el-menu-item>
        <el-menu-item v-if="canAccess('/outbounds')" index="/outbounds">
          <i class="el-icon-upload2"></i>
          <span slot="title">出库管理</span>
        </el-menu-item>
        <el-menu-item v-if="canAccess('/stocks')" index="/stocks">
          <i class="el-icon-box"></i>
          <span slot="title">库存管理</span>
        </el-menu-item>
        <el-menu-item v-if="canAccess('/alerts')" index="/alerts">
          <i class="el-icon-warning-outline"></i>
          <span slot="title">预警中心</span>
        </el-menu-item>
        <el-menu-item v-if="canAccess('/permissions')" index="/permissions">
          <i class="el-icon-user"></i>
          <span slot="title">权限管理</span>
        </el-menu-item>
        <el-menu-item v-if="canAccess('/stock-takes')" index="/stock-takes">
          <i class="el-icon-document-checked"></i>
          <span slot="title">盘点管理</span>
        </el-menu-item>
        <el-menu-item v-if="canAccess('/billing')" index="/billing">
          <i class="el-icon-tickets"></i>
          <span slot="title">计费结算</span>
        </el-menu-item>
        <el-menu-item v-if="canAccess('/approvals')" index="/approvals">
          <i class="el-icon-finished"></i>
          <span slot="title">审批中心</span>
        </el-menu-item>
        <el-menu-item v-if="canAccess('/exceptions')" index="/exceptions">
          <i class="el-icon-document-delete"></i>
          <span slot="title">异常工单</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <div class="layout-main">
      <header class="layout-header">
        <div class="header-heading">
          <div class="header-title">{{ $route.meta.title || '仓储管理平台' }}</div>
          <div class="header-path">
            <span>运营中心</span>
            <i>/</i>
            <span>{{ $route.meta.title || '工作台' }}</span>
          </div>
        </div>
        <div class="header-user">
          <div class="header-chip">企业版</div>
          <div class="user-avatar">{{ userInitial }}</div>
          <div class="user-meta">
            <div class="user-name">{{ currentUser.displayName || currentUser.username || '管理员' }}</div>
            <div class="user-role">{{ currentUser.roleCode || 'ADMIN' }}</div>
          </div>
          <el-button size="mini" @click="logout">退出</el-button>
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
    userInitial() {
      const name = this.currentUser.displayName || this.currentUser.username || 'A'
      return name.slice(0, 1).toUpperCase()
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
  background: #f3f5f9;
}

.layout-sidebar {
  width: 232px;
  flex: 0 0 232px;
  background: #0b1220;
  color: #ffffff;
  border-right: 1px solid #101827;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 10px rgba(15, 23, 42, 0.12);
}

.brand-block {
  height: 64px;
  padding: 12px 18px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: #2563eb;
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.brand-title {
  font-size: 16px;
  line-height: 20px;
  font-weight: 700;
  color: #ffffff;
}

.brand-subtitle {
  margin-top: 2px;
  font-size: 12px;
  line-height: 16px;
  color: #8390a4;
}

.layout-menu {
  flex: 1;
  border-right: none;
  padding: 10px 8px;
  overflow-y: auto;
}

.layout-menu i {
  color: #8390a4;
}

::v-deep .layout-menu .el-menu-item,
::v-deep .layout-menu .el-submenu__title {
  height: 40px;
  line-height: 40px;
  border-radius: 8px;
  margin: 3px 0;
}

::v-deep .layout-menu .el-menu-item.is-active {
  background: #2563eb;
  color: #ffffff !important;
  font-weight: 650;
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.28);
}

::v-deep .layout-menu .el-menu-item.is-active i {
  color: #ffffff;
}

::v-deep .layout-menu .el-menu-item:hover,
::v-deep .layout-menu .el-submenu__title:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}

::v-deep .layout-menu .el-submenu.is-active > .el-submenu__title {
  color: #ffffff !important;
}

::v-deep .layout-menu .el-submenu .el-menu-item {
  height: 36px;
  line-height: 36px;
  min-width: 0;
  padding-left: 42px !important;
}

.layout-main {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.layout-header {
  height: 64px;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

.header-heading {
  min-width: 0;
}

.header-title {
  color: #172033;
  font-size: 19px;
  line-height: 24px;
  font-weight: 700;
}

.header-path {
  margin-top: 2px;
  color: #6b7280;
  font-size: 12px;
  line-height: 16px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.header-path i {
  color: #c0c8d4;
  font-style: normal;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-chip {
  height: 26px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  border: 1px solid #bbf7d0;
  border-radius: 999px;
  background: #f0fdf4;
  color: #047857;
  font-size: 12px;
  font-weight: 650;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e6f0ff;
  color: #1d4ed8;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.user-meta {
  min-width: 96px;
}

.user-name {
  color: #172033;
  font-size: 13px;
  line-height: 18px;
  font-weight: 650;
}

.user-role {
  color: #6b7280;
  font-size: 12px;
  line-height: 16px;
}

.layout-content {
  min-height: 0;
  flex: 1;
  padding: 18px 20px;
  overflow: auto;
}

@media (max-width: 960px) {
  .layout-shell {
    flex-direction: column;
  }

  .layout-sidebar {
    width: 100%;
    flex: 0 0 auto;
  }

  .brand-block {
    border-bottom-color: rgba(255, 255, 255, 0.08);
  }

  .layout-menu {
    max-height: 320px;
  }

  .layout-header {
    height: auto;
    min-height: 64px;
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
    padding: 12px 16px;
  }

  .header-user {
    width: 100%;
    justify-content: space-between;
  }

  .layout-content {
    padding: 12px;
  }
}
</style>
