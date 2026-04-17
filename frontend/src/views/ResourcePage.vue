<template>
  <div class="page-card">
    <div class="page-toolbar">
      <div>
        <h2 style="margin: 0">{{ config.title }}</h2>
        <div style="color: #64748b; margin-top: 6px">{{ labels.description }}</div>
      </div>
      <div>
        <el-input
          v-model="keyword"
          :placeholder="labels.search"
          style="width: 220px; margin-right: 12px"
          clearable
        />
        <el-button type="primary" @click="openCreate">{{ labels.create }}</el-button>
      </div>
    </div>

    <el-alert
      v-if="resourceKey === 'locations' && warehousesWithoutLocations.length"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
      :title="locationSetupMessage"
    />

    <div
      v-if="resourceKey === 'locations' && warehousesWithoutLocations.length"
      class="pending-warehouse-panel"
    >
      <div class="pending-warehouse-head">
        <div class="pending-warehouse-title">{{ labels.pendingWarehouseTitle }}</div>
        <div class="pending-warehouse-subtitle">{{ labels.pendingWarehouseSubtitle }}</div>
      </div>
      <div class="pending-warehouse-grid">
        <div
          v-for="warehouse in warehousesWithoutLocations"
          :key="warehouse.id"
          class="pending-warehouse-card"
        >
          <div class="pending-warehouse-name">{{ warehouse.warehouseName || warehouse.warehouseCode }}</div>
          <div class="pending-warehouse-meta">{{ warehouse.warehouseCode }}</div>
          <div class="pending-warehouse-meta">{{ warehouse.sceneType || '-' }}</div>
          <el-button
            type="primary"
            plain
            size="mini"
            @click="openCreateForWarehouse(warehouse)"
          >
            {{ labels.createFirstLocation }}
          </el-button>
        </div>
      </div>
    </div>

    <el-table :data="filteredRows" stripe border>
      <el-table-column
        v-for="column in config.columns"
        :key="column.prop"
        :prop="column.prop"
        :label="column.label"
        min-width="120"
      >
        <template slot-scope="scope">
          <span>{{ displayValue(scope.row, column) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="labels.actions" width="180" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" @click="openEdit(scope.row)">{{ labels.edit }}</el-button>
          <el-button type="text" class="text-danger" @click="handleDelete(scope.row)">{{ labels.delete }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="760px">
      <el-form :model="form" label-width="110px">
        <el-row :gutter="16">
          <el-col
            v-for="field in config.fields"
            :key="field.prop"
            :span="field.type === 'textarea' ? 24 : 12"
          >
            <el-form-item :label="field.label">
              <el-input
                v-if="field.type === 'text'"
                v-model="form[field.prop]"
                :placeholder="buildPlaceholder(field.label)"
              />
              <el-input
                v-else-if="field.type === 'textarea'"
                v-model="form[field.prop]"
                type="textarea"
                :rows="3"
                :placeholder="buildPlaceholder(field.label)"
              />
              <el-input-number
                v-else-if="field.type === 'number'"
                v-model="form[field.prop]"
                :min="0"
                :controls="false"
                style="width: 100%"
              />
              <el-select
                v-else-if="field.type === 'select'"
                v-model="form[field.prop]"
                :placeholder="labels.select"
                style="width: 100%"
              >
                <el-option
                  v-for="option in field.options"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
              <el-select
                v-else-if="field.type === 'lookup-select'"
                v-model="form[field.prop]"
                :placeholder="labels.select"
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="option in lookups[field.lookup] || []"
                  :key="option[field.valueProp || 'id']"
                  :label="option[field.labelProp || 'name']"
                  :value="option[field.valueProp || 'id']"
                />
              </el-select>
              <el-switch v-else-if="field.type === 'switch'" v-model="form[field.prop]" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">{{ labels.cancel }}</el-button>
        <el-button type="primary" @click="submit">{{ labels.save }}</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { get, post, put, remove } from '../api'
import resourceConfig from '../config/resource-config'

const labels = {
  description: '\u7ef4\u62a4\u4e1a\u52a1\u4e3b\u6570\u636e\uff0c\u4e3a\u4ed3\u50a8\u4f5c\u4e1a\u4e0e\u9884\u8b66\u63d0\u4f9b\u57fa\u7840\u6863\u6848\u3002',
  search: '\u641c\u7d22\u5173\u952e\u5b57',
  create: '\u65b0\u589e',
  edit: '\u7f16\u8f91',
  delete: '\u5220\u9664',
  actions: '\u64cd\u4f5c',
  cancel: '\u53d6\u6d88',
  save: '\u4fdd\u5b58',
  select: '\u8bf7\u9009\u62e9',
  updateSuccess: '\u66f4\u65b0\u6210\u529f',
  createSuccess: '\u521b\u5efa\u6210\u529f',
  saveFailed: '\u4fdd\u5b58\u5931\u8d25',
  deleteConfirmTitle: '\u63d0\u793a',
  deleteConfirmSuffix: '\u8bb0\u5f55\u5417\uff1f',
  deleteSuccess: '\u5220\u9664\u6210\u529f',
  deleteFailed: '\u5220\u9664\u5931\u8d25',
  yes: '\u662f',
  no: '\u5426',
  inputPrefix: '\u8bf7\u8f93\u5165',
  pendingWarehouseTitle: '\u5f85\u914d\u7f6e\u4ed3\u5e93',
  pendingWarehouseSubtitle: '\u8fd9\u4e9b\u4ed3\u5e93\u5df2\u7ecf\u521b\u5efa\u6210\u529f\uff0c\u4f46\u8fd8\u6ca1\u6709\u5efa\u7b2c\u4e00\u4e2a\u5e93\u4f4d\u3002',
  createFirstLocation: '\u65b0\u589e\u9996\u4e2a\u5e93\u4f4d'
}

export default {
  name: 'ResourcePage',
  data() {
    return {
      rows: [],
      lookups: {},
      keyword: '',
      dialogVisible: false,
      form: {},
      labels
    }
  },
  computed: {
    resourceKey() {
      return this.$route.meta.resource
    },
    config() {
      return resourceConfig[this.resourceKey]
    },
    dialogTitle() {
      return this.form.id ? `${labels.edit}${this.config.title}` : `${labels.create}${this.config.title}`
    },
    filteredRows() {
      if (!this.keyword) {
        return this.rows
      }
      const keyword = this.keyword.toLowerCase()
      return this.rows.filter(row => JSON.stringify(row).toLowerCase().includes(keyword))
    },
    warehousesWithoutLocations() {
      if (this.resourceKey !== 'locations') {
        return []
      }
      const usedWarehouseIds = new Set(this.rows.map(row => row.warehouseId))
      return (this.lookups.warehouses || []).filter(item => !usedWarehouseIds.has(item.id))
    },
    locationSetupMessage() {
      const names = this.warehousesWithoutLocations
        .map(item => item.warehouseName || item.warehouseCode || `ID:${item.id}`)
        .join(', ')
      return `${names} \u8fd8\u6ca1\u6709\u5e93\u4f4d\uff0c\u6240\u4ee5\u4e0d\u4f1a\u51fa\u73b0\u5728\u5e93\u4f4d\u5217\u8868\u3001\u5e93\u5b58\u5217\u8868\u548c\u4e0a\u4e0b\u67b6\u5e93\u4f4d\u9009\u9879\u91cc\u3002\u5148\u4e3a\u8fd9\u4e9b\u4ed3\u5e93\u65b0\u589e\u5e93\u4f4d\u5373\u53ef\u3002`
    }
  },
  watch: {
    '$route.meta.resource': {
      immediate: true,
      handler() {
        this.initPage()
      }
    }
  },
  methods: {
    async initPage() {
      this.resetForm()
      await Promise.all([this.fetchLookups(), this.fetchRows()])
    },
    async fetchLookups() {
      const response = await get('/lookups', { _ts: Date.now() })
      this.lookups = response.data || {}
    },
    async fetchRows() {
      const response = await get(this.config.endpoint)
      this.rows = response.data || []
    },
    resetForm() {
      this.form = { ...(this.config.defaults || {}) }
    },
    async openCreate() {
      await this.refreshLookupsIfNeeded()
      this.resetForm()
      if (this.resourceKey === 'locations' && this.warehousesWithoutLocations.length === 1) {
        this.form.warehouseId = this.warehousesWithoutLocations[0].id
      }
      this.dialogVisible = true
    },
    async openCreateForWarehouse(warehouse) {
      await this.refreshLookupsIfNeeded()
      this.resetForm()
      this.form.warehouseId = warehouse.id
      this.dialogVisible = true
    },
    async openEdit(row) {
      await this.refreshLookupsIfNeeded()
      this.form = JSON.parse(JSON.stringify(row))
      this.dialogVisible = true
    },
    async submit() {
      try {
        if (this.form.id) {
          await put(`${this.config.endpoint}/${this.form.id}`, this.form)
          this.$message.success(labels.updateSuccess)
        } else {
          await post(this.config.endpoint, this.form)
          this.$message.success(labels.createSuccess)
        }
        this.dialogVisible = false
        await this.fetchRows()
      } catch (error) {
        this.$message.error(error.message || labels.saveFailed)
      }
    },
    async handleDelete(row) {
      try {
        await this.$confirm(
          `${this.config.title} ${labels.deleteConfirmSuffix}`,
          labels.deleteConfirmTitle,
          { type: 'warning' }
        )
        await remove(`${this.config.endpoint}/${row.id}`)
        this.$message.success(labels.deleteSuccess)
        await this.fetchRows()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.message || labels.deleteFailed)
        }
      }
    },
    async refreshLookupsIfNeeded() {
      const hasLookupField = (this.config.fields || []).some(field => field.type === 'lookup-select')
      const hasLookupColumn = (this.config.columns || []).some(column => column.lookup)
      if (hasLookupField || hasLookupColumn) {
        await this.fetchLookups()
      }
    },
    buildPlaceholder(label) {
      return `${labels.inputPrefix}${label}`
    },
    displayValue(row, column) {
      if (column.lookup) {
        const source = this.lookups[column.lookup] || []
        const matched = source.find(item => item.id === row[column.prop])
        return matched ? matched[column.displayProp] : '-'
      }
      if (typeof row[column.prop] === 'boolean') {
        return row[column.prop] ? labels.yes : labels.no
      }
      return row[column.prop] === null || row[column.prop] === undefined || row[column.prop] === '' ? '-' : row[column.prop]
    }
  }
}
</script>

<style scoped>
.pending-warehouse-panel {
  margin-bottom: 16px;
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, #f8fafc 0%, #eef6ff 100%);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.pending-warehouse-head {
  margin-bottom: 14px;
}

.pending-warehouse-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.pending-warehouse-subtitle {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.pending-warehouse-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.pending-warehouse-card {
  padding: 14px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid rgba(148, 163, 184, 0.16);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pending-warehouse-name {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.pending-warehouse-meta {
  color: #64748b;
  font-size: 13px;
  word-break: break-all;
}
</style>
