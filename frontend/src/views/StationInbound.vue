<template>
  <div class="station-page">
    <div class="station-hero">
      <div>
        <h2>驿站/柜机专用入库</h2>
        <p>选择驿站或柜机仓库后，录入客户和包裹信息，系统会自动分配库位并直接生成取件码。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" plain @click="fetchLookups">刷新基础数据</el-button>
        <el-button type="primary" @click="submit" :loading="submitting">入库并上架</el-button>
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
                <el-select v-model="form.warehouseId" filterable placeholder="请选择驿站/柜机仓库" style="width: 100%">
                  <el-option
                    v-for="item in availableWarehouses"
                    :key="item.id"
                    :label="item.warehouseName"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="客户">
                <el-select v-model="form.customerId" clearable filterable placeholder="可直接选择客户" style="width: 100%">
                  <el-option
                    v-for="item in lookups.customers || []"
                    :key="item.id"
                    :label="item.customerName"
                    :value="item.id"
                  />
                </el-select>
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
                <el-select v-model="form.productId" filterable placeholder="请选择商品" style="width: 100%">
                  <el-option
                    v-for="item in lookups.products || []"
                    :key="item.id"
                    :label="`${item.skuCode} / ${item.productName}`"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="数量">
                <el-input-number v-model="form.qty" :min="1" :controls="false" style="width: 100%" />
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
          <div class="policy-item">
            <span>业务场景</span>
            <strong>{{ sceneText(selectedWarehouse.sceneType) }}</strong>
          </div>
          <div class="policy-item">
            <span>滞留阈值</span>
            <strong>{{ selectedWarehouse.dwellAlertMinutes || 0 }} 分钟</strong>
          </div>
          <div class="policy-item">
            <span>短信提醒</span>
            <strong>{{ selectedWarehouse.smsNotifyEnabled ? '开启' : '关闭' }}</strong>
          </div>
          <div class="policy-item">
            <span>自动分配库位</span>
            <strong>{{ selectedWarehouse.autoAssignLocation ? '是' : '否' }}</strong>
          </div>
          <div class="policy-item">
            <span>扫码模式</span>
            <strong>{{ scanModeText(selectedWarehouse.scanMode) }}</strong>
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
              <div class="result-label">取件码</div>
              <div class="code-line">{{ result.pickupCode || '-' }}</div>
            </div>
            <div>
              <div class="result-label">扫码码</div>
              <div class="code-line">{{ result.scanCode || '-' }}</div>
            </div>
          </div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="入库单号">{{ result.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="仓库">{{ result.warehouseName }}</el-descriptions-item>
            <el-descriptions-item label="收件人">{{ result.receiverName }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ result.receiverPhone }}</el-descriptions-item>
            <el-descriptions-item label="分配库位">{{ resolveLocationCode(result.items && result.items[0] && result.items[0].locationId) }}</el-descriptions-item>
            <el-descriptions-item label="取件截止">{{ result.pickupDueAt || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="result-actions">
            <el-button size="mini" @click="copyText(result.pickupCode)">复制取件码</el-button>
            <el-button size="mini" @click="copyText(result.scanCode)">复制扫码码</el-button>
            <el-button size="mini" type="primary" plain @click="openPda(result.pickupCode)">前往 PDA 扫码</el-button>
          </div>
        </div>
        <div v-else class="empty-hint">完成驿站入库后，这里会直接展示取件码、扫码码和自动分配的库位。</div>
      </div>

      <div class="page-card station-history-card">
        <div class="section-title">最近驿站入库</div>
        <el-table :data="recentRows" stripe size="small">
          <el-table-column prop="orderNo" label="入库单号" min-width="150" />
          <el-table-column prop="receiverName" label="收件人" min-width="110" />
          <el-table-column prop="pickupCode" label="取件码" min-width="150" />
          <el-table-column prop="pickupStatus" label="取件状态" min-width="110" />
          <el-table-column prop="pickupDueAt" label="截止时间" min-width="160" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
import { get, post } from '../api'

const sceneTypes = ['PARCEL_STATION', 'TAKEOUT_LOCKER', 'CAMPUS_PICKUP']

function pad(value) {
  return String(value).padStart(2, '0')
}

function formatNow() {
  const now = new Date()
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

export default {
  name: 'StationInboundPage',
  data() {
    return {
      lookups: {},
      recentRows: [],
      submitting: false,
      result: null,
      form: {
        warehouseId: null,
        customerId: null,
        receiverName: '',
        receiverPhone: '',
        sourceNo: '',
        expectedArrivalTime: formatNow(),
        productId: null,
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
      return sceneWarehouses.length ? sceneWarehouses : warehouses
    },
    selectedWarehouse() {
      return (this.lookups.warehouses || []).find(item => item.id === this.form.warehouseId) || null
    },
    selectedCustomer() {
      return (this.lookups.customers || []).find(item => item.id === this.form.customerId) || null
    }
  },
  watch: {
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
    resolveLocationCode(locationId) {
      const location = (this.lookups.locations || []).find(item => item.id === locationId)
      return location ? `${location.locationCode} / ${location.locationName}` : '-'
    },
    buildPayload() {
      const sourceNo = this.form.sourceNo || `PKG-${Date.now()}`
      const batchNo = this.form.batchNo || sourceNo
      return {
        warehouseId: this.form.warehouseId,
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
            locationId: null,
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
      if (!this.form.receiverName || !this.form.receiverPhone) {
        throw new Error('请补全收件人和联系电话')
      }
    },
    async submit() {
      try {
        this.validateForm()
        this.submitting = true
        const createResponse = await post('/inbounds', this.buildPayload())
        const orderId = createResponse.data.id
        const putawayResponse = await post(`/inbounds/${orderId}/putaway`)
        this.result = putawayResponse.data || createResponse.data
        this.$message.success('驿站入库完成，已生成取件码')
        await Promise.all([this.fetchLookups(), this.fetchRecent()])
      } catch (error) {
        this.$message.error(error.message || '驿站入库失败')
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
  gap: 20px;
}

.station-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
}

.station-hero h2 {
  margin: 0;
  font-size: 28px;
}

.station-hero p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.7;
  max-width: 720px;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.station-grid {
  display: grid;
  grid-template-columns: 1.4fr 0.8fr;
  gap: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 18px;
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
  gap: 14px;
}

.policy-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fafc;
}

.policy-item span {
  color: #64748b;
}

.result-hero {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  padding: 18px;
  border-radius: 16px;
  background: linear-gradient(135deg, #eff6ff 0%, #ecfeff 100%);
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

.empty-hint {
  color: #64748b;
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .station-grid {
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
