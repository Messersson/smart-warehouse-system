<template>
  <div class="station-page">
    <div class="station-hero">
      <div>
        <h2>驿站/柜机专用入库</h2>
        <p>选择驿站或柜机仓库后，录入客户和包裹信息，系统会自动分配库位并生成待打印标签。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" plain @click="fetchLookups">刷新基础数据</el-button>
        <el-button type="primary" @click="submit" :loading="submitting">生成标签</el-button>
      </div>
    </div>

    <el-alert
      v-if="!availableWarehouses.length"
      title="还没有可用于驿站/柜机场景的仓库，请先到仓库管理里把业务场景改成菜鸟驿站、外卖柜或校园驿站。"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 20px"
    />

    <div class="station-grid">
      <div class="page-card station-form-card">
        <div class="section-title">入库表单</div>
        <el-form :model="form" label-width="110px">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="目标仓库">
                <div class="quick-select">
                  <el-select v-model="form.warehouseId" filterable placeholder="请选择驿站/柜机仓库" style="width: 100%">
                    <el-option
                      v-for="item in availableWarehouses"
                      :key="item.id"
                      :label="item.warehouseName"
                      :value="item.id"
                    />
                  </el-select>
                  <el-button icon="el-icon-plus" @click="openQuickCreate('warehouses')" />
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="客户">
                <div class="quick-select">
                  <el-select v-model="form.customerId" clearable filterable placeholder="可直接选择客户" style="width: 100%">
                    <el-option
                      v-for="item in lookups.customers || []"
                      :key="item.id"
                      :label="item.customerName"
                      :value="item.id"
                    />
                  </el-select>
                  <el-button icon="el-icon-plus" @click="openQuickCreate('customers')" />
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="收件人">
                <el-input v-model="form.receiverName" placeholder="请输入收件人姓名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系电话">
                <el-input v-model="form.receiverPhone" placeholder="请输入手机号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="包裹单号">
                <el-input v-model="form.sourceNo" placeholder="留空可自动生成业务单号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="预计到仓">
                <el-date-picker
                  v-model="form.expectedArrivalTime"
                  type="datetime"
                  value-format="yyyy-MM-dd HH:mm:ss"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="商品">
                <div class="quick-select">
                  <el-select v-model="form.productId" filterable placeholder="请选择商品" style="width: 100%">
                    <el-option
                      v-for="item in lookups.products || []"
                      :key="item.id"
                      :label="`${item.skuCode} / ${item.productName}`"
                      :value="item.id"
                    />
                  </el-select>
                  <el-button icon="el-icon-plus" @click="openQuickCreate('products')" />
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="数量">
                <el-input-number v-model="form.qty" :min="1" :controls="false" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col v-if="!policyForm.autoAssignLocation" :span="12">
              <el-form-item label="手动库位">
                <div class="quick-select">
                  <el-select v-model="form.locationId" filterable placeholder="请选择上架库位" style="width: 100%">
                    <el-option
                      v-for="item in warehouseLocations"
                      :key="item.id"
                      :label="`${item.locationCode} / ${item.locationName}`"
                      :value="item.id"
                    />
                  </el-select>
                  <el-button icon="el-icon-plus" :disabled="!form.warehouseId" @click="openQuickCreate('locations')" />
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="批次号">
                <el-input v-model="form.batchNo" placeholder="留空可按包裹单号识别" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="操作人">
                <el-input v-model="form.operatorName" placeholder="默认系统管理员" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="可填写包裹特征、柜口要求等信息" />
          </el-form-item>
        </el-form>
      </div>

      <div class="page-card station-side-card">
        <div class="section-title">仓库策略</div>
        <div v-if="selectedWarehouse" class="policy-stack">
          <div class="policy-field">
            <div class="policy-label">业务场景</div>
            <el-select v-model="policyForm.sceneType" style="width: 100%">
              <el-option
                v-for="option in warehouseSceneOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </div>
          <div class="policy-field">
            <div class="policy-label">滞留阈值</div>
            <el-input-number
              v-model="policyForm.dwellAlertMinutes"
              :min="0"
              :controls="false"
              style="width: 100%"
            />
          </div>
          <div class="policy-field">
            <div class="policy-label">短信提醒</div>
            <el-switch
              v-model="policyForm.smsNotifyEnabled"
              active-text="开启"
              inactive-text="关闭"
            />
          </div>
          <div v-if="policyForm.smsNotifyEnabled" class="policy-field">
            <div class="policy-label">短信重发间隔</div>
            <el-input-number
              v-model="policyForm.smsReminderIntervalMinutes"
              :min="1"
              :controls="false"
              style="width: 100%"
            />
          </div>
          <div class="policy-field">
            <div class="policy-label">自动分配库位</div>
            <el-switch
              v-model="policyForm.autoAssignLocation"
              active-text="是"
              inactive-text="否"
            />
          </div>
          <div class="policy-field">
            <div class="policy-label">扫码模式</div>
            <el-select v-model="policyForm.scanMode" style="width: 100%">
              <el-option
                v-for="option in warehouseScanOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </div>
          <div class="policy-actions">
            <el-button :disabled="savingPolicy || !isPolicyDirty" @click="resetPolicyForm">还原</el-button>
            <el-button
              type="primary"
              :loading="savingPolicy"
              :disabled="!isPolicyDirty"
              @click="savePolicy()"
            >
              保存策略
            </el-button>
          </div>
        </div>
        <div v-else class="empty-hint">选择仓库后，可查看当前驿站/柜机的入库与通知策略。</div>
      </div>
    </div>

    <div class="station-grid result-grid">
      <div class="page-card station-result-card">
        <div class="section-title">本次入库结果</div>
        <div v-if="result" class="result-stack">
          <div class="result-hero">
            <div>
              <div class="result-label">作业编码</div>
              <div class="code-line">{{ unifiedCode(result) }}</div>
            </div>
            <el-alert
              title="请先打印标签并贴到货物上，现场扫描货物标签确认后，才能正式上架入库。"
              type="warning"
              :closable="false"
              show-icon
            />
          </div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="作业编码">{{ unifiedCode(result) }}</el-descriptions-item>
            <el-descriptions-item label="仓库">{{ result.warehouseName }}</el-descriptions-item>
            <el-descriptions-item label="入库状态">{{ result.status || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收件人">{{ result.receiverName }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ result.receiverPhone }}</el-descriptions-item>
            <el-descriptions-item label="分配库位">{{ resolveLocationCode(result.items && result.items[0] && result.items[0].locationId) }}</el-descriptions-item>
            <el-descriptions-item label="扫码确认">{{ primaryCargoItem && primaryCargoItem.putawayScanConfirmed ? '已确认' : '待扫描货物标签' }}</el-descriptions-item>
            <el-descriptions-item label="取件截止">{{ result.pickupDueAt || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div v-if="primaryCargoItem" class="cargo-code-panel">
            <div class="cargo-code-preview">
              <div v-if="primaryCargoSvg" class="code-svg-box" v-html="primaryCargoSvg"></div>
              <div v-else class="code-render-placeholder">暂无码图形</div>
            </div>
            <el-descriptions :column="2" border size="small" class="cargo-code-meta-table">
              <el-descriptions-item label="货物码">{{ primaryCargoItem.cargoCode || '-' }}</el-descriptions-item>
              <el-descriptions-item label="码类型">{{ scanModeText(primaryCargoItem.cargoCodeType) }}</el-descriptions-item>
              <el-descriptions-item label="独立记录ID">{{ primaryCargoRecord ? primaryCargoRecord.id : '-' }}</el-descriptions-item>
              <el-descriptions-item label="渲染格式">{{ primaryCargoRecord ? primaryCargoRecord.renderFormat : '-' }}</el-descriptions-item>
              <el-descriptions-item label="作业类型">{{ primaryCargoRecord ? operationTypeText(primaryCargoRecord.operationType) : '-' }}</el-descriptions-item>
              <el-descriptions-item label="库位">{{ primaryCargoItem.locationFullName || resolveLocationCode(primaryCargoItem.locationId) }}</el-descriptions-item>
              <el-descriptions-item label="码内容">{{ primaryCargoRawContent }}</el-descriptions-item>
            </el-descriptions>
          </div>
          <div class="result-actions">
            <el-button size="mini" @click="copyText(unifiedCode(result))">复制作业编码</el-button>
            <el-button size="mini" @click="copyText(primaryCargoRawContent)" :disabled="!primaryCargoRawContent">复制码内容</el-button>
            <el-button size="mini" type="primary" plain :disabled="!primaryCargoRawContent" @click="openPda(primaryCargoRawContent)">前往 PDA 扫货物码</el-button>
          </div>
        </div>
        <div v-else class="empty-hint">生成标签后，这里会展示统一作业编码、自动分配库位和可打印码信息。</div>
      </div>

      <div class="page-card station-history-card">
        <div class="section-title">最近驿站入库</div>
        <el-table :data="recentRows" stripe size="small">
          <el-table-column prop="orderNo" label="作业编码" min-width="170" />
          <el-table-column prop="receiverName" label="收件人" min-width="110" />
          <el-table-column prop="pickupStatus" label="取件状态" min-width="110" />
          <el-table-column prop="pickupDueAt" label="截止时间" min-width="160" />
        </el-table>
      </div>
    </div>

    <quick-create-dialog
      v-model="quickCreateVisible"
      :resource="quickCreateResource"
      :context="quickCreateContext"
      @created="handleQuickCreated"
    />
  </div>
</template>

<script>
import { get, post, put } from '../api'
import QuickCreateDialog from '../components/QuickCreateDialog.vue'

const sceneTypes = ['PARCEL_STATION', 'TAKEOUT_LOCKER', 'CAMPUS_PICKUP']

const warehouseSceneOptions = [
  { label: '通用仓储', value: 'GENERAL_STORAGE' },
  { label: '菜鸟驿站', value: 'PARCEL_STATION' },
  { label: '外卖柜', value: 'TAKEOUT_LOCKER' },
  { label: '校园驿站', value: 'CAMPUS_PICKUP' }
]

const warehouseScanOptions = [
  { label: '二维码', value: 'QR_CODE' },
  { label: '条码', value: 'BAR_CODE' },
  { label: '二维码 + 条码', value: 'HYBRID' }
]

function toNumber(value, fallback, min = 0) {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) {
    return fallback
  }
  return Math.max(min, Math.round(numberValue))
}

function normalizePolicy(source = {}) {
  return {
    sceneType: source.sceneType || 'PARCEL_STATION',
    dwellAlertMinutes: toNumber(source.dwellAlertMinutes, 0),
    smsNotifyEnabled: Boolean(source.smsNotifyEnabled),
    smsReminderIntervalMinutes: toNumber(source.smsReminderIntervalMinutes, 120, 1),
    autoAssignLocation: source.autoAssignLocation !== false,
    scanMode: source.scanMode || 'QR_CODE'
  }
}

function pad(value) {
  return String(value).padStart(2, '0')
}

function formatNow() {
  const now = new Date()
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

export default {
  name: 'StationInboundPage',
  components: {
    QuickCreateDialog
  },
  data() {
    return {
      lookups: {},
      recentRows: [],
      submitting: false,
      savingPolicy: false,
      quickCreateVisible: false,
      quickCreateResource: '',
      result: null,
      warehouseSceneOptions,
      warehouseScanOptions,
      policyForm: normalizePolicy(),
      form: {
        warehouseId: null,
        customerId: null,
        receiverName: '',
        receiverPhone: '',
        sourceNo: '',
        expectedArrivalTime: formatNow(),
        productId: null,
        locationId: null,
        ownerId: null,
        qty: 1,
        batchNo: '',
        operatorName: '系统管理员',
        remark: ''
      }
    }
  },
  computed: {
    availableWarehouses() {
      const warehouses = this.lookups.warehouses || []
      const sceneWarehouses = warehouses.filter(item => sceneTypes.includes(item.sceneType))
      if (!sceneWarehouses.length) {
        return warehouses
      }
      const currentWarehouse = warehouses.find(item => item.id === this.form.warehouseId)
      if (currentWarehouse && !sceneWarehouses.some(item => item.id === currentWarehouse.id)) {
        return [currentWarehouse, ...sceneWarehouses]
      }
      return sceneWarehouses
    },
    selectedWarehouse() {
      return (this.lookups.warehouses || []).find(item => item.id === this.form.warehouseId) || null
    },
    selectedCustomer() {
      return (this.lookups.customers || []).find(item => item.id === this.form.customerId) || null
    },
    warehouseLocations() {
      return (this.lookups.locations || [])
        .filter(item => item.warehouseId === this.form.warehouseId && item.status === 'ACTIVE')
    },
    isPolicyDirty() {
      if (!this.selectedWarehouse) {
        return false
      }
      const savedPolicy = normalizePolicy(this.selectedWarehouse)
      const editingPolicy = normalizePolicy(this.policyForm)
      return Object.keys(savedPolicy).some(key => savedPolicy[key] !== editingPolicy[key])
    },
    primaryCargoItem() {
      return this.result && this.result.items && this.result.items.length ? this.result.items[0] : null
    },
    primaryCargoRecord() {
      const records = this.primaryCargoItem && this.primaryCargoItem.cargoCodeRecords
      return records && records.length ? records[records.length - 1] : null
    },
    primaryCargoSvg() {
      return this.primaryCargoItem ? this.primaryCargoItem.cargoCodeSvg || (this.primaryCargoRecord && this.primaryCargoRecord.svgContent) : ''
    },
    primaryCargoRawContent() {
      if (!this.primaryCargoItem) {
        return ''
      }
      return this.primaryCargoItem.cargoCodeType === 'BAR_CODE'
        ? this.primaryCargoItem.cargoCode
        : this.primaryCargoItem.cargoCodeContent || this.primaryCargoItem.cargoCode || ''
    },
    quickCreateContext() {
      return {
        warehouseId: this.form.warehouseId
      }
    }
  },
  watch: {
    selectedWarehouse: {
      immediate: true,
      handler(warehouse) {
        this.syncPolicyForm(warehouse)
      }
    },
    'form.warehouseId'() {
      this.form.locationId = null
    },
    'policyForm.autoAssignLocation'(enabled) {
      if (enabled) {
        this.form.locationId = null
      }
    },
    selectedCustomer: {
      immediate: true,
      handler(customer) {
        if (!customer) {
          return
        }
        this.form.receiverName = customer.customerName || customer.contactName || this.form.receiverName
        this.form.receiverPhone = customer.contactPhone || this.form.receiverPhone
      }
    },
    availableWarehouses: {
      immediate: true,
      handler(list) {
        if (!this.form.warehouseId && list.length) {
          this.form.warehouseId = list[0].id
        }
      }
    },
    'lookups.owners': {
      immediate: true,
      handler(list) {
        if (!this.form.ownerId && list && list.length) {
          this.form.ownerId = list[0].id
        }
      }
    }
  },
  async created() {
    await Promise.all([this.fetchLookups(), this.fetchRecent()])
  },
  methods: {
    async fetchLookups() {
      const response = await get('/lookups', { _ts: Date.now() })
      this.lookups = response.data || {}
    },
    openQuickCreate(resource) {
      this.quickCreateResource = resource
      this.quickCreateVisible = true
    },
    async handleQuickCreated({ resource, item }) {
      await this.fetchLookups()
      const created = item || {}
      if (resource === 'warehouses') {
        this.form.warehouseId = created.id
      } else if (resource === 'customers') {
        this.form.customerId = created.id
      } else if (resource === 'products') {
        this.form.productId = created.id
      } else if (resource === 'locations') {
        this.form.locationId = created.id
      }
      this.$message.success('新增成功，已自动选中')
    },
    syncPolicyForm(warehouse = this.selectedWarehouse) {
      this.policyForm = normalizePolicy(warehouse || {})
    },
    resetPolicyForm() {
      this.syncPolicyForm()
    },
    async savePolicy(silent = false) {
      if (!this.selectedWarehouse || !this.isPolicyDirty) {
        return this.selectedWarehouse
      }
      const policy = normalizePolicy(this.policyForm)
      const payload = {
        ...this.selectedWarehouse,
        ...policy
      }
      this.savingPolicy = true
      try {
        const response = await put(`/warehouses/${this.selectedWarehouse.id}`, payload)
        const savedWarehouse = response.data || payload
        const warehouses = [...(this.lookups.warehouses || [])]
        const index = warehouses.findIndex(item => item.id === savedWarehouse.id)
        if (index >= 0) {
          this.$set(warehouses, index, savedWarehouse)
        } else {
          warehouses.push(savedWarehouse)
        }
        this.$set(this.lookups, 'warehouses', warehouses)
        this.syncPolicyForm(savedWarehouse)
        if (!silent) {
          this.$message.success('仓库策略已保存')
        }
        return savedWarehouse
      } catch (error) {
        error.policySaveFailed = true
        this.$message.error(error.message || '仓库策略保存失败')
        throw error
      } finally {
        this.savingPolicy = false
      }
    },
    async fetchRecent() {
      const response = await get('/inbounds')
      const rows = response.data || []
      this.recentRows = rows
        .filter(item => item.pickupCode || sceneTypes.includes(this.resolveWarehouseScene(item.warehouseId)))
        .slice(0, 8)
    },
    resolveWarehouseScene(warehouseId) {
      const warehouse = (this.lookups.warehouses || []).find(item => item.id === warehouseId)
      return warehouse ? warehouse.sceneType : null
    },
    sceneText(sceneType) {
      const map = {
        GENERAL_STORAGE: '通用仓储',
        PARCEL_STATION: '菜鸟驿站',
        TAKEOUT_LOCKER: '外卖柜',
        CAMPUS_PICKUP: '校园驿站'
      }
      return map[sceneType] || sceneType || '-'
    },
    scanModeText(scanMode) {
      const map = {
        QR_CODE: '二维码',
        BAR_CODE: '条码',
        HYBRID: '二维码 + 条码'
      }
      return map[scanMode] || scanMode || '-'
    },
    operationTypeText(type) {
      const map = {
        INBOUND_PUTAWAY: '入库上架'
      }
      return map[type] || type || '-'
    },
    resolveLocationCode(locationId) {
      const location = (this.lookups.locations || []).find(item => item.id === locationId)
      return location ? `${location.locationCode} / ${location.locationName}` : '-'
    },
    unifiedCode(row) {
      if (!row) {
        return '-'
      }
      return row.code || row.orderNo || row.pickupCode || row.scanCode || '-'
    },
    buildPayload() {
      const sourceNo = this.form.sourceNo || `PKG-${Date.now()}`
      const batchNo = this.form.batchNo || sourceNo
      return {
        warehouseId: this.form.warehouseId,
        ownerId: this.form.ownerId,
        customerId: this.form.customerId,
        sourceNo,
        expectedArrivalTime: this.form.expectedArrivalTime || formatNow(),
        operatorName: this.form.operatorName || '系统管理员',
        receiverName: this.form.receiverName,
        receiverPhone: this.form.receiverPhone,
        remark: this.form.remark,
        items: [
          {
            productId: this.form.productId,
            batchNo,
            expectedQty: this.form.qty,
            actualQty: this.form.qty,
            qualifiedQty: this.form.qty,
            locationId: this.policyForm.autoAssignLocation ? null : this.form.locationId,
            remark: this.form.remark
          }
        ]
      }
    },
    validateForm() {
      if (!this.form.warehouseId) {
        throw new Error('请选择目标仓库')
      }
      if (!this.form.productId) {
        throw new Error('请选择商品')
      }
      if (!this.form.ownerId) {
        throw new Error('请选择货主')
      }
      if (!this.form.receiverName || !this.form.receiverPhone) {
        throw new Error('请补全收件人和联系电话')
      }
      if (!this.policyForm.autoAssignLocation && !this.form.locationId) {
        throw new Error('自动分配库位关闭时，请选择手动库位')
      }
    },
    async submit() {
      try {
        this.validateForm()
        await this.savePolicy(true)
        this.submitting = true
        const createResponse = await post('/inbounds', this.buildPayload())
        this.result = createResponse.data
        this.$message.success('标签已生成，请打印贴货并扫码确认后再上架')
        await Promise.all([this.fetchLookups(), this.fetchRecent()])
      } catch (error) {
        if (!error.policySaveFailed) {
          this.$message.error(error.message || '驿站入库失败')
        }
      } finally {
        this.submitting = false
      }
    },
    async copyText(value) {
      if (!value) {
        return
      }
      try {
        await navigator.clipboard.writeText(value)
        this.$message.success('已复制到剪贴板')
      } catch (error) {
        this.$message.error('复制失败，请手动复制')
      }
    },
    openPda(code) {
      this.$router.push({ path: '/pda', query: { code } })
    }
  }
}
</script>

<style scoped>
.station-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.station-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
}

.station-hero h2 {
  margin: 0;
  font-size: 20px;
  line-height: 28px;
}

.station-hero p {
  display: none;
}

.hero-actions {
  display: flex;
  gap: 8px;
}

.station-grid {
  display: grid;
  grid-template-columns: 1.4fr 0.8fr;
  gap: 12px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 14px;
}

.station-form-card,
.station-side-card,
.station-result-card,
.station-history-card {
  min-height: 100%;
}

.policy-stack,
.result-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.policy-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.policy-label {
  color: #64748b;
  font-size: 13px;
  line-height: 18px;
}

.quick-select {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 34px;
  gap: 6px;
  align-items: center;
}

.quick-select .el-button {
  width: 34px;
  min-width: 34px;
  padding-left: 0;
  padding-right: 0;
}

.policy-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 4px;
}

.result-hero {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
  padding: 14px;
  border: 1px solid #d8dee6;
  border-radius: 8px;
  background: #f8fafc;
}

.result-label {
  color: #64748b;
  margin-bottom: 8px;
}

.code-line {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.result-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.cargo-code-panel {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 12px;
  align-items: stretch;
}

.cargo-code-preview {
  min-height: 220px;
  border: 1px solid #d8dee6;
  border-radius: 8px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
}

.code-svg-box {
  width: 100%;
  display: flex;
  justify-content: center;
}

.code-svg-box svg {
  max-width: 190px;
  max-height: 190px;
  width: auto;
  height: auto;
}

.code-render-placeholder {
  width: 180px;
  height: 180px;
  border: 1px dashed #cbd5e1;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cargo-code-meta-table {
  min-width: 0;
}

.empty-hint {
  color: #64748b;
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .station-grid {
    grid-template-columns: 1fr;
  }

  .cargo-code-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .station-hero {
    flex-direction: column;
  }

  .hero-actions {
    width: 100%;
  }

  .hero-actions .el-button {
    flex: 1;
  }
}
</style>
