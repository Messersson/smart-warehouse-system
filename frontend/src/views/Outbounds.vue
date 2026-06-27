<template>
  <div class="page-card">
    <div class="page-toolbar">
      <div>
        <h2 style="margin: 0">出库管理</h2>
        <div style="color: #64748b; margin-top: 6px">覆盖拣货、发运和出库超时监控。</div>
      </div>
      <div>
        <el-button type="primary" @click="openCreate">新建出库单</el-button>
      </div>
    </div>

    <div class="direct-transfer-panel">
      <el-select v-model="directTransferFormat" class="direct-transfer-format" size="small">
        <el-option label="自动识别" value="AUTO" />
        <el-option label="条形码" value="BAR_CODE" />
        <el-option label="二维码" value="QR_CODE" />
      </el-select>
      <el-input
        ref="directTransferInput"
        v-model="directTransferCode"
        size="small"
        placeholder="商品条形码 / SKU / 货物码"
        @keyup.enter.native="submitDirectTransfer"
      />
      <el-button
        type="success"
        size="small"
        :loading="directTransferLoading"
        @click="submitDirectTransfer"
      >
        直接出库
      </el-button>
    </div>

    <el-table :data="rows" stripe border>
      <el-table-column prop="orderNo" label="作业编码" min-width="170" />
      <el-table-column prop="warehouseName" label="仓库" min-width="120" />
      <el-table-column prop="customerName" label="客户" min-width="120" />
      <el-table-column prop="status" label="状态" width="140">
        <template slot-scope="scope">
          <el-tag :type="statusType(scope.row.status)">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="priorityLevel" label="优先级" width="100" />
      <el-table-column prop="totalPlannedQty" label="计划数量" width="120" />
      <el-table-column prop="totalShippedQty" label="已扫数量" width="120" />
      <el-table-column prop="plannedShipTime" label="计划发运" min-width="180" />
      <el-table-column label="操作" width="340" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" @click="openDetail(scope.row)">明细</el-button>
          <el-button type="text" @click="handlePicking(scope.row)" :disabled="scope.row.status !== 'CREATED'">拣货完成</el-button>
          <el-button type="text" @click="openScanShip(scope.row)" :disabled="scope.row.status === 'SHIPPED'">扫码出库</el-button>
          <el-button type="text" @click="submitApproval(scope.row)">提交审批</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="新建出库单" :visible.sync="dialogVisible" width="1080px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="作业编码">
              <el-input v-model="form.orderNo" placeholder="可留空自动生成统一编码" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所属仓库">
              <div class="quick-select">
                <el-select v-model="form.warehouseId" filterable style="width: 100%">
                  <el-option v-for="item in lookups.warehouses || []" :key="item.id" :label="item.warehouseName" :value="item.id" />
                </el-select>
                <el-button icon="el-icon-plus" @click="openQuickCreate('warehouses')" />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="客户">
              <div class="quick-select">
                <el-select v-model="form.customerId" filterable style="width: 100%">
                  <el-option v-for="item in lookups.customers || []" :key="item.id" :label="item.customerName" :value="item.id" />
                </el-select>
                <el-button icon="el-icon-plus" @click="openQuickCreate('customers')" />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="货主">
              <div class="quick-select">
                <el-select v-model="form.ownerId" filterable style="width: 100%">
                  <el-option v-for="item in lookups.owners || []" :key="item.id" :label="item.ownerName" :value="item.id" />
                </el-select>
                <el-button icon="el-icon-plus" @click="openQuickCreate('owners')" />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="优先级">
              <el-select v-model="form.priorityLevel" style="width: 100%">
                <el-option label="普通" value="NORMAL" />
                <el-option label="高" value="HIGH" />
                <el-option label="加急" value="URGENT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计划发运">
              <el-date-picker
                v-model="form.plannedShipTime"
                type="datetime"
                value-format="yyyy-MM-dd HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>

        <div class="page-toolbar" style="margin-top: 8px">
          <strong>出库明细</strong>
          <el-button type="primary" plain size="mini" @click="addItem">添加明细</el-button>
        </div>

        <el-table :data="form.items" border size="small">
          <el-table-column label="商品" min-width="220">
            <template slot-scope="scope">
              <div class="quick-select">
                <el-select v-model="scope.row.productId" filterable style="width: 100%">
                  <el-option v-for="item in lookups.products || []" :key="item.id" :label="`${item.skuCode} / ${item.productName}`" :value="item.id" />
                </el-select>
                <el-button icon="el-icon-plus" @click="openQuickCreate('products', scope.row)" />
              </div>
            </template>
          </el-table-column>
          <el-table-column label="批次号" min-width="160">
            <template slot-scope="scope">
              <el-input v-model="scope.row.batchNo" />
            </template>
          </el-table-column>
          <el-table-column label="计划数量" width="120">
            <template slot-scope="scope">
              <el-input-number v-model="scope.row.plannedQty" :min="0" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="已扫数量" width="120">
            <template slot-scope="scope">
              <span>{{ scope.row.shippedQty || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="拣货库位" min-width="180">
            <template slot-scope="scope">
              <div class="quick-select">
                <el-select v-model="scope.row.locationId" filterable style="width: 100%">
                  <el-option v-for="item in filteredLocations" :key="item.id" :label="item.locationCode" :value="item.id" />
                </el-select>
                <el-button icon="el-icon-plus" :disabled="!form.warehouseId" @click="openQuickCreate('locations', scope.row)" />
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template slot-scope="scope">
              <el-button type="text" class="text-danger" @click="removeItem(scope.$index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </span>
    </el-dialog>

    <quick-create-dialog
      v-model="quickCreateVisible"
      :resource="quickCreateResource"
      :context="quickCreateContext"
      @created="handleQuickCreated"
    />

    <el-dialog title="出库单明细" :visible.sync="detailVisible" width="920px">
      <el-descriptions :column="3" border v-if="currentRow.id">
        <el-descriptions-item label="作业编码">{{ currentRow.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ currentRow.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ currentRow.status }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ currentRow.customerName }}</el-descriptions-item>
        <el-descriptions-item label="计划发运">{{ currentRow.plannedShipTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划数量">{{ currentRow.totalPlannedQty }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="currentRow.items || []" border style="margin-top: 16px">
        <el-table-column prop="skuCode" label="SKU" min-width="140" />
        <el-table-column prop="productName" label="商品名称" min-width="160" />
        <el-table-column prop="batchNo" label="批次号" min-width="140" />
        <el-table-column prop="plannedQty" label="计划数量" width="120" />
        <el-table-column prop="shippedQty" label="已扫数量" width="120" />
        <el-table-column prop="locationId" label="库位ID" width="120" />
      </el-table>
    </el-dialog>

    <el-dialog title="扫码出库" :visible.sync="scanShipVisible" width="980px" @closed="resetScanShip">
      <div v-if="scanShipOrder.id" class="scan-ship-dialog">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="作业编码">{{ scanShipOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="仓库">{{ scanShipOrder.warehouseName }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ scanShipOrder.status }}</el-descriptions-item>
          <el-descriptions-item label="计划数量">{{ scanShipOrder.totalPlannedQty }}</el-descriptions-item>
          <el-descriptions-item label="已扫数量">{{ scanShipOrder.totalShippedQty }}</el-descriptions-item>
          <el-descriptions-item label="扫码进度">{{ scanShipCompleted ? '已完成' : '待扫码' }}</el-descriptions-item>
        </el-descriptions>

        <div class="scan-ship-bar">
          <el-select v-model="scanShipFormat" class="scan-ship-format">
            <el-option label="自动识别" value="AUTO" />
            <el-option label="二维码" value="QR_CODE" />
            <el-option label="条形码" value="BAR_CODE" />
            <el-option label="Data Matrix" value="DATA_MATRIX" />
            <el-option label="PDF417" value="PDF_417" />
            <el-option label="Aztec" value="AZTEC" />
            <el-option label="其他" value="OTHER" />
          </el-select>
          <el-input
            ref="scanShipInput"
            v-model="scanShipCode"
            placeholder="扫描商品条码/SKU/二维码内容后回车"
            @keyup.enter.native="submitScanShipCode"
          />
          <el-button type="primary" :loading="scanShipLoading" @click="submitScanShipCode">确认扫码</el-button>
        </div>

        <el-table :data="scanShipOrder.items || []" border size="small">
          <el-table-column prop="skuCode" label="SKU" min-width="140" />
          <el-table-column prop="productName" label="商品名称" min-width="160" />
          <el-table-column prop="batchNo" label="批次号" min-width="130" />
          <el-table-column prop="plannedQty" label="应扫数量" width="120" />
          <el-table-column prop="shippedQty" label="已扫数量" width="120" />
          <el-table-column label="状态" width="110">
            <template slot-scope="scope">
              <el-tag :type="scanItemCompleted(scope.row) ? 'success' : 'warning'" size="mini">
                {{ scanItemCompleted(scope.row) ? '完成' : '待扫码' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <span slot="footer">
        <el-button @click="scanShipVisible = false">关闭</el-button>
        <el-button type="primary" :loading="shipLoading" :disabled="!scanShipCompleted" @click="finishScanShip">完成出库</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { get, post } from '../api'
import QuickCreateDialog from '../components/QuickCreateDialog.vue'

const createItem = () => ({
  productId: null,
  batchNo: '',
  plannedQty: 0,
  shippedQty: 0,
  locationId: null,
  remark: ''
})

export default {
  name: 'OutboundsPage',
  components: {
    QuickCreateDialog
  },
  data() {
    return {
      rows: [],
      lookups: {},
      dialogVisible: false,
      detailVisible: false,
      scanShipVisible: false,
      scanShipOrder: {},
      scanShipCode: '',
      scanShipFormat: 'AUTO',
      scanShipLoading: false,
      shipLoading: false,
      directTransferCode: '',
      directTransferFormat: 'AUTO',
      directTransferLoading: false,
      quickCreateVisible: false,
      quickCreateResource: '',
      quickCreateTarget: null,
      currentRow: {},
      form: {
        orderNo: '',
        warehouseId: null,
        customerId: null,
        ownerId: null,
        priorityLevel: 'NORMAL',
        plannedShipTime: '',
        operatorName: '系统管理员',
        remark: '',
        items: [createItem()]
      }
    }
  },
  computed: {
    scanShipCompleted() {
      const items = this.scanShipOrder.items || []
      return items.length > 0 && items.every(item => this.toNumber(item.shippedQty) >= this.toNumber(item.plannedQty))
    },
    filteredLocations() {
      const locations = this.lookups.locations || []
      if (!this.form.warehouseId) {
        return locations
      }
      return locations.filter(item => item.warehouseId === this.form.warehouseId)
    },
    quickCreateContext() {
      return {
        warehouseId: this.form.warehouseId
      }
    }
  },
  async created() {
    await Promise.all([this.fetchRows(), this.fetchLookups()])
  },
  mounted() {
    this.focusDirectTransferInput()
  },
  methods: {
    async fetchRows() {
      const response = await get('/outbounds')
      this.rows = response.data || []
    },
    async fetchLookups() {
      const response = await get('/lookups', { _ts: Date.now() })
      this.lookups = response.data || {}
    },
    focusDirectTransferInput() {
      this.$nextTick(() => {
        if (this.$refs.directTransferInput) {
          this.$refs.directTransferInput.focus()
        }
      })
    },
    async submitDirectTransfer() {
      const rawContent = this.directTransferCode.trim()
      if (!rawContent) {
        this.$message.error('请先扫描商品条形码、SKU 或货物码')
        this.focusDirectTransferInput()
        return
      }
      try {
        this.directTransferLoading = true
        const response = await post('/outbounds/scan-transfer', {
          rawContent,
          scanFormat: this.directTransferFormat,
          sourceDevice: 'WEB_OUTBOUND_DIRECT',
          scannerInterface: 'WIRED_SCANNER',
          scannerDeviceId: 'WEB_OUTBOUND_DIRECT',
          operatorName: '系统管理员'
        })
        const data = response.data || {}
        this.directTransferCode = ''
        this.$message.success(data.message || response.message || '已直接转入出库单')
        await this.fetchRows()
      } catch (error) {
        this.$message.error(error.message || '直接出库失败')
      } finally {
        this.directTransferLoading = false
        this.focusDirectTransferInput()
      }
    },
    async openCreate() {
      await this.fetchLookups()
      this.form = {
        orderNo: '',
        warehouseId: null,
        customerId: null,
        ownerId: null,
        priorityLevel: 'NORMAL',
        plannedShipTime: '',
        operatorName: '系统管理员',
        remark: '',
        items: [createItem()]
      }
      this.dialogVisible = true
    },
    openDetail(row) {
      this.currentRow = row
      this.detailVisible = true
    },
    openQuickCreate(resource, target = null) {
      this.quickCreateResource = resource
      this.quickCreateTarget = target
      this.quickCreateVisible = true
    },
    async handleQuickCreated({ resource, item }) {
      await this.fetchLookups()
      const created = item || {}
      if (resource === 'warehouses') {
        this.form.warehouseId = created.id
      } else if (resource === 'customers') {
        this.form.customerId = created.id
      } else if (resource === 'owners') {
        this.form.ownerId = created.id
      } else if (resource === 'products' && this.quickCreateTarget) {
        this.quickCreateTarget.productId = created.id
      } else if (resource === 'locations' && this.quickCreateTarget) {
        this.quickCreateTarget.locationId = created.id
      }
      this.$message.success('新增成功，已自动选中')
    },
    addItem() {
      this.form.items.push(createItem())
    },
    removeItem(index) {
      this.form.items.splice(index, 1)
      if (!this.form.items.length) {
        this.form.items.push(createItem())
      }
    },
    async submit() {
      try {
        await post('/outbounds', this.form)
        this.$message.success('创建成功')
        this.dialogVisible = false
        await this.fetchRows()
      } catch (error) {
        this.$message.error(error.message || '创建失败')
      }
    },
    async handlePicking(row) {
      try {
        await post(`/outbounds/${row.id}/picking`)
        this.$message.success('拣货完成')
        await this.fetchRows()
      } catch (error) {
        this.$message.error(error.message || '操作失败')
      }
    },
    async handleShip(row) {
      try {
        await post(`/outbounds/${row.id}/ship`)
        this.$message.success('发运完成')
        await this.fetchRows()
        return true
      } catch (error) {
        this.$message.error(error.message || '发运失败')
        return false
      }
    },
    async openScanShip(row) {
      try {
        const response = await get(`/outbounds/${row.id}`)
        this.scanShipOrder = response.data || row
        this.scanShipCode = ''
        this.scanShipFormat = 'AUTO'
        this.scanShipVisible = true
        this.$nextTick(() => {
          if (this.$refs.scanShipInput) {
            this.$refs.scanShipInput.focus()
          }
        })
      } catch (error) {
        this.$message.error(error.message || '打开扫码出库失败')
      }
    },
    async submitScanShipCode() {
      if (!this.scanShipCode.trim()) {
        this.$message.error('请先扫描商品条码或二维码')
        return
      }
      try {
        this.scanShipLoading = true
        const response = await post(`/outbounds/${this.scanShipOrder.id}/scan-ship`, {
          rawContent: this.scanShipCode.trim(),
          scanFormat: this.scanShipFormat,
          sourceDevice: 'WEB_OUTBOUND',
          operatorName: '系统管理员'
        })
        this.scanShipOrder = response.data || this.scanShipOrder
        this.scanShipCode = ''
        this.$message.success('扫码确认成功')
        await this.fetchRows()
        this.$nextTick(() => {
          if (this.$refs.scanShipInput) {
            this.$refs.scanShipInput.focus()
          }
        })
      } catch (error) {
        this.$message.error(error.message || '扫码确认失败')
      } finally {
        this.scanShipLoading = false
      }
    },
    async finishScanShip() {
      try {
        this.shipLoading = true
        const success = await this.handleShip(this.scanShipOrder)
        if (success) {
          this.scanShipVisible = false
        }
      } finally {
        this.shipLoading = false
      }
    },
    resetScanShip() {
      this.scanShipCode = ''
      this.scanShipOrder = {}
      this.shipLoading = false
      this.scanShipLoading = false
    },
    scanItemCompleted(item) {
      return this.toNumber(item.shippedQty) >= this.toNumber(item.plannedQty)
    },
    toNumber(value) {
      const number = Number(value)
      return Number.isFinite(number) ? number : 0
    },
    async submitApproval(row) {
      try {
        await post('/approvals/submit', {
          approvalType: 'ORDER_APPROVAL',
          bizType: 'OUTBOUND_ORDER',
          bizId: row.id,
          bizNo: row.orderNo,
          applicantName: '系统管理员',
          approverName: '仓库主管',
          applyReason: `出库单 ${row.orderNo} 需要审批`
        })
        this.$message.success('审批已提交')
      } catch (error) {
        this.$message.error(error.message || '提交审批失败')
      }
    },
    statusType(status) {
      if (status === 'SHIPPED') return 'success'
      if (status === 'PICKING_COMPLETED') return 'warning'
      return 'info'
    }
  }
}
</script>

<style scoped>
.scan-ship-dialog {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.direct-transfer-panel {
  display: grid;
  grid-template-columns: 120px minmax(220px, 1fr) auto;
  gap: 8px;
  align-items: center;
  margin-bottom: 14px;
}

.direct-transfer-format {
  width: 120px;
}

.scan-ship-bar {
  display: grid;
  grid-template-columns: 140px 1fr auto;
  gap: 8px;
}

.scan-ship-format {
  width: 140px;
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

@media (max-width: 760px) {
  .direct-transfer-panel {
    grid-template-columns: 1fr;
  }

  .direct-transfer-format {
    width: 100%;
  }

  .scan-ship-bar {
    grid-template-columns: 1fr;
  }

  .scan-ship-format {
    width: 100%;
  }
}
</style>
