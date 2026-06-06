<template>
  <div class="page-card">
    <div class="page-toolbar">
      <div>
        <h2 class="resource-title">{{ config.title }}</h2>
        <div class="resource-description">{{ labels.description }}</div>
      </div>
      <div class="toolbar-actions">
        <el-input
          v-model="keyword"
          :placeholder="labels.search"
          class="search-input"
          clearable
        />
        <el-button :loading="loading" @click="initPage">{{ labels.refresh }}</el-button>
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

    <el-table
      v-loading="loading"
      :data="filteredRows"
      :empty-text="emptyText"
      stripe
      border
    >
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
      <el-table-column :label="labels.actions" :width="resourceKey === 'products' ? 240 : 180" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" @click="openEdit(scope.row)">{{ labels.edit }}</el-button>
          <el-button
            v-if="resourceKey === 'products'"
            type="text"
            :loading="barcodeGeneratingId === scope.row.id"
            @click="openBarcode(scope.row)"
          >
            条码
          </el-button>
          <el-button
            type="text"
            class="text-danger"
            :loading="deletingId === scope.row.id"
            @click="handleDelete(scope.row)"
          >
            {{ labels.delete }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      :close-on-click-modal="!submitting"
      width="760px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col
            v-for="field in config.fields"
            :key="field.prop"
            :span="field.type === 'textarea' ? 24 : 12"
          >
            <el-form-item :label="field.label" :prop="field.prop">
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
        <el-button :disabled="submitting" @click="dialogVisible = false">{{ labels.cancel }}</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">{{ labels.save }}</el-button>
      </span>
    </el-dialog>

    <el-dialog
      title="货物条码标签"
      :visible.sync="barcodeVisible"
      width="520px"
      class="barcode-dialog"
    >
      <div v-if="barcodeProduct.id" class="barcode-preview">
        <div class="barcode-label" ref="barcodeLabel">
          <div class="barcode-product-name">{{ barcodeProduct.productName }}</div>
          <div class="barcode-product-meta">{{ barcodeProduct.skuCode }} / {{ barcodeProduct.productSpec || '-' }}</div>
          <div class="barcode-svg-box" v-html="barcodeSvg"></div>
          <div class="barcode-value">{{ barcodeProduct.barcode }}</div>
        </div>
        <el-alert
          v-if="barcodeError"
          type="warning"
          :closable="false"
          show-icon
          :title="barcodeError"
        />
      </div>
      <span slot="footer">
        <el-button @click="downloadBarcodeSvg">下载 SVG</el-button>
        <el-button type="primary" :disabled="!!barcodeError" @click="printBarcodeLabel">打印标签</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { get, post, put, remove } from '../api'
import resourceConfig from '../config/resource-config'

const code39Patterns = {
  '0': 'nnnwwnwnn',
  '1': 'wnnwnnnnw',
  '2': 'nnwwnnnnw',
  '3': 'wnwwnnnnn',
  '4': 'nnnwwnnnw',
  '5': 'wnnwwnnnn',
  '6': 'nnwwwnnnn',
  '7': 'nnnwnnwnw',
  '8': 'wnnwnnwnn',
  '9': 'nnwwnnwnn',
  A: 'wnnnnwnnw',
  B: 'nnwnnwnnw',
  C: 'wnwnnwnnn',
  D: 'nnnnwwnnw',
  E: 'wnnnwwnnn',
  F: 'nnwnwwnnn',
  G: 'nnnnnwwnw',
  H: 'wnnnnwwnn',
  I: 'nnwnnwwnn',
  J: 'nnnnwwwnn',
  K: 'wnnnnnnww',
  L: 'nnwnnnnww',
  M: 'wnwnnnnwn',
  N: 'nnnnwnnww',
  O: 'wnnnwnnwn',
  P: 'nnwnwnnwn',
  Q: 'nnnnnnwww',
  R: 'wnnnnnwwn',
  S: 'nnwnnnwwn',
  T: 'nnnnwnwwn',
  U: 'wwnnnnnnw',
  V: 'nwwnnnnnw',
  W: 'wwwnnnnnn',
  X: 'nwnnwnnnw',
  Y: 'wwnnwnnnn',
  Z: 'nwwnwnnnn',
  '-': 'nwnnnnwnw',
  '.': 'wwnnnnwnn',
  ' ': 'nwwnnnwnn',
  '$': 'nwnwnwnnn',
  '/': 'nwnwnnnwn',
  '+': 'nwnnnwnwn',
  '%': 'nnnwnwnwn',
  '*': 'nwnnwnwnn'
}

function escapeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function normalizeBarcode(value) {
  return String(value || '').trim().toUpperCase()
}

function buildCode39Svg(value) {
  const code = normalizeBarcode(value)
  if (!code) {
    throw new Error('当前商品还没有条码')
  }
  const fullCode = `*${code}*`
  const invalid = fullCode.split('').find(char => !code39Patterns[char])
  if (invalid) {
    throw new Error(`条码包含 Code 39 不支持的字符：${invalid}`)
  }

  const narrow = 2
  const wide = 5
  const gap = narrow
  const margin = 14
  const barTop = 12
  const barHeight = 70
  let x = margin
  const rects = []

  fullCode.split('').forEach(char => {
    const pattern = code39Patterns[char]
    pattern.split('').forEach((part, index) => {
      const width = part === 'w' ? wide : narrow
      if (index % 2 === 0) {
        rects.push(`<rect x="${x}" y="${barTop}" width="${width}" height="${barHeight}" fill="#111827" />`)
      }
      x += width
    })
    x += gap
  })

  const width = x + margin - gap
  const height = 112
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${escapeHtml(code)}">
    <rect width="100%" height="100%" fill="#ffffff" />
    ${rects.join('')}
    <text x="${width / 2}" y="102" text-anchor="middle" font-family="Consolas, monospace" font-size="14" fill="#111827">${escapeHtml(code)}</text>
  </svg>`
}

const labels = {
  description: '\u7ef4\u62a4\u4e1a\u52a1\u4e3b\u6570\u636e\uff0c\u4e3a\u4ed3\u50a8\u4f5c\u4e1a\u4e0e\u9884\u8b66\u63d0\u4f9b\u57fa\u7840\u6863\u6848\u3002',
  search: '\u641c\u7d22\u5173\u952e\u5b57',
  refresh: '\u5237\u65b0',
  create: '\u65b0\u589e',
  edit: '\u7f16\u8f91',
  delete: '\u5220\u9664',
  actions: '\u64cd\u4f5c',
  cancel: '\u53d6\u6d88',
  save: '\u4fdd\u5b58',
  select: '\u8bf7\u9009\u62e9',
  updateSuccess: '\u66f4\u65b0\u6210\u529f',
  createSuccess: '\u521b\u5efa\u6210\u529f',
  loadFailed: '\u52a0\u8f7d\u6570\u636e\u5931\u8d25',
  saveFailed: '\u4fdd\u5b58\u5931\u8d25',
  deleteConfirmTitle: '\u63d0\u793a',
  deleteConfirmSuffix: '\u8bb0\u5f55\u5417\uff1f',
  deleteSuccess: '\u5220\u9664\u6210\u529f',
  deleteFailed: '\u5220\u9664\u5931\u8d25',
  yes: '\u662f',
  no: '\u5426',
  noData: '\u6682\u65e0\u6570\u636e',
  noSearchResults: '\u6ca1\u6709\u5339\u914d\u7684\u6570\u636e',
  inputPrefix: '\u8bf7\u8f93\u5165',
  selectPrefix: '\u8bf7\u9009\u62e9',
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
      loading: false,
      submitting: false,
      deletingId: null,
      dialogVisible: false,
      barcodeVisible: false,
      barcodeGeneratingId: null,
      barcodeProduct: {},
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
    },
    rules() {
      const result = {}
      const fields = this.config.fields || []
      fields.forEach(field => {
        if (!field.required) {
          return
        }
        const isSelect = field.type === 'select' || field.type === 'lookup-select'
        result[field.prop] = [
          {
            required: true,
            message: `${isSelect ? labels.selectPrefix : labels.inputPrefix}${field.label}`,
            trigger: isSelect ? 'change' : 'blur'
          }
        ]
      })
      return result
    },
    emptyText() {
      return this.keyword ? labels.noSearchResults : labels.noData
    },
    barcodeSvg() {
      if (!this.barcodeProduct.barcode) {
        return ''
      }
      try {
        return buildCode39Svg(this.barcodeProduct.barcode)
      } catch (error) {
        return ''
      }
    },
    barcodeError() {
      if (!this.barcodeProduct.barcode) {
        return ''
      }
      try {
        buildCode39Svg(this.barcodeProduct.barcode)
        return ''
      } catch (error) {
        return error.message || '条码生成失败'
      }
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
      this.loading = true
      try {
        await Promise.all([this.fetchLookups(), this.fetchRows()])
      } catch (error) {
        this.$message.error(error.message || labels.loadFailed)
      } finally {
        this.loading = false
      }
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
      if (!(await this.refreshLookupsSafely())) {
        return
      }
      this.resetForm()
      if (this.resourceKey === 'locations' && this.warehousesWithoutLocations.length === 1) {
        this.form.warehouseId = this.warehousesWithoutLocations[0].id
      }
      this.dialogVisible = true
      this.clearFormValidation()
    },
    async openCreateForWarehouse(warehouse) {
      if (!(await this.refreshLookupsSafely())) {
        return
      }
      this.resetForm()
      this.form.warehouseId = warehouse.id
      this.dialogVisible = true
      this.clearFormValidation()
    },
    async openEdit(row) {
      if (!(await this.refreshLookupsSafely())) {
        return
      }
      this.form = JSON.parse(JSON.stringify(row))
      this.dialogVisible = true
      this.clearFormValidation()
    },
    async submit() {
      const valid = await this.validateForm()
      if (!valid) {
        return
      }
      this.submitting = true
      try {
        let saved = null
        if (this.form.id) {
          const response = await put(`${this.config.endpoint}/${this.form.id}`, this.form)
          saved = response.data
          this.$message.success(labels.updateSuccess)
        } else {
          const response = await post(this.config.endpoint, this.form)
          saved = response.data
          this.$message.success(labels.createSuccess)
        }
        this.dialogVisible = false
        await this.fetchRows()
        if (this.resourceKey === 'products' && saved) {
          this.openBarcode(saved)
        }
      } catch (error) {
        this.$message.error(error.message || labels.saveFailed)
      } finally {
        this.submitting = false
      }
    },
    async handleDelete(row) {
      try {
        await this.$confirm(
          `${this.config.title} ${labels.deleteConfirmSuffix}`,
          labels.deleteConfirmTitle,
          { type: 'warning' }
        )
        this.deletingId = row.id
        await remove(`${this.config.endpoint}/${row.id}`)
        this.$message.success(labels.deleteSuccess)
        await this.fetchRows()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.message || labels.deleteFailed)
        }
      } finally {
        this.deletingId = null
      }
    },
    async refreshLookupsIfNeeded() {
      const hasLookupField = (this.config.fields || []).some(field => field.type === 'lookup-select')
      const hasLookupColumn = (this.config.columns || []).some(column => column.lookup)
      if (hasLookupField || hasLookupColumn) {
        await this.fetchLookups()
      }
    },
    async refreshLookupsSafely() {
      try {
        await this.refreshLookupsIfNeeded()
        return true
      } catch (error) {
        this.$message.error(error.message || labels.loadFailed)
        return false
      }
    },
    validateForm() {
      return new Promise(resolve => {
        if (!this.$refs.formRef) {
          resolve(true)
          return
        }
        this.$refs.formRef.validate(valid => resolve(valid))
      })
    },
    clearFormValidation() {
      this.$nextTick(() => {
        if (this.$refs.formRef) {
          this.$refs.formRef.clearValidate()
        }
      })
    },
    async openBarcode(row) {
      let product = row
      if (!product.barcode) {
        try {
          this.barcodeGeneratingId = product.id
          const response = await put(`${this.config.endpoint}/${product.id}`, { ...product, barcode: '' })
          product = response.data || product
          await this.fetchRows()
        } catch (error) {
          this.$message.error(error.message || '条码生成失败')
          return
        } finally {
          this.barcodeGeneratingId = null
        }
      }
      this.barcodeProduct = JSON.parse(JSON.stringify(product))
      this.barcodeVisible = true
    },
    downloadBarcodeSvg() {
      if (this.barcodeError || !this.barcodeSvg) {
        this.$message.error(this.barcodeError || '条码生成失败')
        return
      }
      const blob = new Blob([this.barcodeSvg], { type: 'image/svg+xml;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `${this.barcodeProduct.barcode || 'barcode'}.svg`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    },
    printBarcodeLabel() {
      if (this.barcodeError || !this.barcodeSvg) {
        this.$message.error(this.barcodeError || '条码生成失败')
        return
      }
      const frame = document.createElement('iframe')
      frame.style.position = 'fixed'
      frame.style.right = '0'
      frame.style.bottom = '0'
      frame.style.width = '0'
      frame.style.height = '0'
      frame.style.border = '0'
      document.body.appendChild(frame)

      const doc = frame.contentWindow.document
      doc.open()
      doc.write(`
        <!doctype html>
        <html>
          <head>
            <meta charset="utf-8">
            <title>${escapeHtml(this.barcodeProduct.barcode)}</title>
            <style>
              @page { size: 70mm 42mm; margin: 4mm; }
              * { box-sizing: border-box; }
              body { margin: 0; font-family: Arial, "Microsoft YaHei", sans-serif; color: #111827; }
              .label { width: 62mm; min-height: 34mm; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2mm; }
              .name { width: 100%; text-align: center; font-size: 12px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
              .meta { width: 100%; text-align: center; font-size: 10px; color: #4b5563; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
              svg { max-width: 58mm; height: 22mm; }
              .code { font-size: 10px; letter-spacing: 0.08em; }
            </style>
          </head>
          <body>
            <div class="label">
              <div class="name">${escapeHtml(this.barcodeProduct.productName)}</div>
              <div class="meta">${escapeHtml(this.barcodeProduct.skuCode)} / ${escapeHtml(this.barcodeProduct.productSpec || '-')}</div>
              ${this.barcodeSvg}
              <div class="code">${escapeHtml(this.barcodeProduct.barcode)}</div>
            </div>
          </body>
        </html>
      `)
      doc.close()
      setTimeout(() => {
        frame.contentWindow.focus()
        frame.contentWindow.print()
        setTimeout(() => document.body.removeChild(frame), 500)
      }, 100)
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
  padding: 16px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #d8dee6;
}

.resource-title {
  margin: 0;
  font-size: 18px;
  line-height: 24px;
  font-weight: 700;
}

.resource-description {
  color: #6b7280;
  margin-top: 4px;
  font-size: 13px;
  line-height: 18px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  width: 220px;
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
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid #d8dee6;
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

.barcode-preview {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.barcode-label {
  width: 100%;
  min-height: 230px;
  padding: 18px;
  border: 1px solid #d8dee6;
  border-radius: 8px;
  background: #ffffff;
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.barcode-product-name {
  max-width: 100%;
  color: #111827;
  font-size: 16px;
  line-height: 22px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.barcode-product-meta,
.barcode-value {
  max-width: 100%;
  color: #64748b;
  font-size: 13px;
  line-height: 18px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.barcode-value {
  color: #111827;
  font-family: Consolas, monospace;
  letter-spacing: 0.06em;
}

.barcode-svg-box {
  max-width: 100%;
  overflow-x: auto;
}

@media (max-width: 760px) {
  .toolbar-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }
}
</style>
