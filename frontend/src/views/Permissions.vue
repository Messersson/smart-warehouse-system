<template>
  <div class="page-card">
    <div class="page-toolbar">
      <div>
        <h2 style="margin: 0">权限管理</h2>
        <div style="color: #64748b; margin-top: 6px">维护用户、角色、菜单以及角色菜单授权关系。</div>
      </div>
      <div>
        <el-button type="primary" plain @click="openUserDialog()">新增用户</el-button>
        <el-button type="primary" plain @click="openRoleDialog()">新增角色</el-button>
        <el-button type="primary" @click="openMenuDialog()">新增菜单</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="用户列表" name="users">
        <el-table :data="overview.users || []" stripe border>
          <el-table-column prop="username" label="用户名" min-width="140" />
          <el-table-column prop="displayName" label="显示名称" min-width="140" />
          <el-table-column prop="roleCode" label="角色编码" width="120" />
          <el-table-column prop="phone" label="联系电话" min-width="120" />
          <el-table-column prop="status" label="状态" width="100">
            <template slot-scope="scope">{{ statusText(scope.row.status) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template slot-scope="scope">
              <el-button type="text" @click="openUserDialog(scope.row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="角色列表" name="roles">
        <el-table :data="overview.roles || []" stripe border>
          <el-table-column prop="roleCode" label="角色编码" min-width="140" />
          <el-table-column prop="roleName" label="角色名称" min-width="140" />
          <el-table-column prop="roleDesc" label="角色说明" min-width="200" />
          <el-table-column prop="status" label="状态" width="100">
            <template slot-scope="scope">{{ statusText(scope.row.status) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template slot-scope="scope">
              <el-button type="text" @click="openRoleDialog(scope.row)">编辑</el-button>
              <el-button type="text" @click="openRoleMenuDialog(scope.row)">菜单授权</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="菜单列表" name="menus">
        <el-table :data="overview.menus || []" stripe border>
          <el-table-column prop="menuCode" label="菜单编码" min-width="160" />
          <el-table-column prop="menuName" label="菜单名称" min-width="160" />
          <el-table-column prop="menuPath" label="路由路径" min-width="180" />
          <el-table-column prop="sortNo" label="排序" width="80" />
          <el-table-column prop="status" label="状态" width="100">
            <template slot-scope="scope">{{ statusText(scope.row.status) }}</template>
          </el-table-column>
          <el-table-column label="是否显示" width="100">
            <template slot-scope="scope">{{ scope.row.visible ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template slot-scope="scope">
              <el-button type="text" @click="openMenuDialog(scope.row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog :title="userForm.id ? '编辑用户' : '新增用户'" :visible.sync="userDialogVisible" width="720px">
      <el-form :model="userForm" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="用户名"><el-input v-model="userForm.username" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="密码"><el-input v-model="userForm.password" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="显示名称"><el-input v-model="userForm.displayName" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="角色">
              <el-select v-model="userForm.roleCode" style="width: 100%">
                <el-option v-for="role in overview.roles || []" :key="role.id" :label="role.roleName" :value="role.roleCode" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="userForm.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="邮箱"><el-input v-model="userForm.email" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="userForm.status" style="width: 100%">
                <el-option label="启用" value="ACTIVE" />
                <el-option label="待启用" value="PENDING" />
                <el-option label="停用" value="INACTIVE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <span slot="footer">
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog :title="roleForm.id ? '编辑角色' : '新增角色'" :visible.sync="roleDialogVisible" width="640px">
      <el-form :model="roleForm" label-width="110px">
        <el-form-item label="角色编码"><el-input v-model="roleForm.roleCode" /></el-form-item>
        <el-form-item label="角色名称"><el-input v-model="roleForm.roleName" /></el-form-item>
        <el-form-item label="角色说明"><el-input v-model="roleForm.roleDesc" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="roleForm.status" style="width: 100%">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRole">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog :title="menuForm.id ? '编辑菜单' : '新增菜单'" :visible.sync="menuDialogVisible" width="720px">
      <el-form :model="menuForm" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="菜单编码"><el-input v-model="menuForm.menuCode" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="菜单名称"><el-input v-model="menuForm.menuName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="路由路径"><el-input v-model="menuForm.menuPath" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="组件名"><el-input v-model="menuForm.componentName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="图标"><el-input v-model="menuForm.iconName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="权限编码"><el-input v-model="menuForm.permissionCode" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="排序"><el-input-number v-model="menuForm.sortNo" :controls="false" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="menuForm.status" style="width: 100%">
                <el-option label="启用" value="ACTIVE" />
                <el-option label="停用" value="INACTIVE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="是否显示"><el-switch v-model="menuForm.visible" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <span slot="footer">
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMenu">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog :title="`角色菜单授权 - ${selectedRole.roleName || ''}`" :visible.sync="roleMenuDialogVisible" width="720px">
      <el-tree
        ref="menuTree"
        show-checkbox
        node-key="id"
        :data="menuTree"
        :props="{ label: 'menuName', children: 'children' }"
        default-expand-all
      />
      <span slot="footer">
        <el-button @click="roleMenuDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRoleMenus">保存授权</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { get, post, put } from '../api'

export default {
  name: 'PermissionsPage',
  data() {
    return {
      activeTab: 'users',
      overview: {},
      userDialogVisible: false,
      roleDialogVisible: false,
      menuDialogVisible: false,
      roleMenuDialogVisible: false,
      userForm: { status: 'ACTIVE' },
      roleForm: { status: 'ACTIVE' },
      menuForm: { status: 'ACTIVE', visible: true, sortNo: 0 },
      selectedRole: {}
    }
  },
  computed: {
    menuTree() {
      const menus = (this.overview.menus || []).map(item => ({ ...item, children: [] }))
      const map = {}
      menus.forEach(item => { map[item.id] = item })
      const roots = []
      menus.forEach(item => {
        if (item.parentId && map[item.parentId]) {
          map[item.parentId].children.push(item)
        } else {
          roots.push(item)
        }
      })
      return roots
    }
  },
  async created() {
    await this.fetchOverview()
  },
  methods: {
    statusText(status) {
      const map = { ACTIVE: '启用', PENDING: '待启用', INACTIVE: '停用' }
      return map[status] || status
    },
    async fetchOverview() {
      const response = await get('/permissions/overview')
      this.overview = response.data || {}
    },
    openUserDialog(row) {
      this.userForm = row ? { ...row } : { status: 'ACTIVE' }
      this.userDialogVisible = true
    },
    openRoleDialog(row) {
      this.roleForm = row ? { ...row } : { status: 'ACTIVE' }
      this.roleDialogVisible = true
    },
    openMenuDialog(row) {
      this.menuForm = row ? { ...row } : { status: 'ACTIVE', visible: true, sortNo: 0 }
      this.menuDialogVisible = true
    },
    openRoleMenuDialog(role) {
      this.selectedRole = role
      this.roleMenuDialogVisible = true
      this.$nextTick(() => {
        const checked = (this.overview.roleMenus && this.overview.roleMenus[role.id]) || []
        this.$refs.menuTree.setCheckedKeys(checked)
      })
    },
    async saveUser() {
      if (this.userForm.id) {
        await put(`/permissions/users/${this.userForm.id}`, this.userForm)
      } else {
        await post('/permissions/users', this.userForm)
      }
      this.$message.success('用户保存成功')
      this.userDialogVisible = false
      await this.fetchOverview()
    },
    async saveRole() {
      if (this.roleForm.id) {
        await put(`/permissions/roles/${this.roleForm.id}`, this.roleForm)
      } else {
        await post('/permissions/roles', this.roleForm)
      }
      this.$message.success('角色保存成功')
      this.roleDialogVisible = false
      await this.fetchOverview()
    },
    async saveMenu() {
      if (this.menuForm.id) {
        await put(`/permissions/menus/${this.menuForm.id}`, this.menuForm)
      } else {
        await post('/permissions/menus', this.menuForm)
      }
      this.$message.success('菜单保存成功')
      this.menuDialogVisible = false
      await this.fetchOverview()
    },
    async saveRoleMenus() {
      const checkedKeys = this.$refs.menuTree.getCheckedKeys()
      await post(`/permissions/roles/${this.selectedRole.id}/menus`, { menuIds: checkedKeys })
      this.$message.success('角色菜单授权已保存')
      this.roleMenuDialogVisible = false
      await this.fetchOverview()
    }
  }
}
</script>
