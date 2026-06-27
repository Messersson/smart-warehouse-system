<template>
  <div class="page-card">
    <div class="page-toolbar">
      <div>
        <h2 style="margin: 0">入库管理</h2>
      <div style="color: #64748b; margin-top: 6px">管理收货、标签打印、扫码确认、上架和入库滞留情况。</div>
      </div>
      <div>
        <el-button @click="openCargoCodeRecords">已生成码记录</el-button>
        <el-button type="primary" @click="openCreate">新建入库单</el-button>
      </div>
    </div>

    <el-table :data="rows" stripe border>
      <el-table-column prop="orderNo" label="作业编码" min-width="170" />
      <el-table-column prop="warehouseName" label="仓库" min-width="120" />
      <el-table-column prop="supplierName" label="供应商" min-width="120" />
      <el-table-column label="标签扫码" width="110">
        <template slot-scope="scope">
          <el-tag :type="allItemsScanConfirmed(scope.row) ? 'success' : 'warning'">
            {{ allItemsScanConfirmed(scope.row) ? '已确认' : '待扫码' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template slot-scope="scope">
          <el-tag :type="statusType(scope.row.status)">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="totalExpectedQty" label="预计数量" width="120" />
      <el-table-column prop="receivedAt" label="收货时间" min-width="180" />
      <el-table-column prop="putawayCompletedAt" label="上架完成时间" min-width="180" />
      <el-table-column label="操作" width="320" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" @click="openDetail(scope.row)">明细</el-button>
          <el-button type="text" @click="handleReceive(scope.row)" :disabled="scope.row.status !== 'CREATED'">收货</el-button>
          <el-button type="text" @click="handlePutaway(scope.row)" :disabled="scope.row.status === 'PUTAWAY_COMPLETED'">扫码后上架</el-button>
          <el-button type="text" @click="submitApproval(scope.row)">提交审批</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="新建入库单" :visible.sync="dialogVisible" width="1080px">
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
                <el-select v-model="form.warehouseId" filterable style="width: 100%" :disabled="!!selectedSupplierWarehouse">
                  <el-option v-for="item in lookups.warehouses || []" :key="item.id" :label="item.warehouseName" :value="item.id" />
                </el-select>
                <el-button icon="el-icon-plus" :disabled="!!selectedSupplierWarehouse" @click="openQuickCreate('warehouses')" />
              </div>
              <div v-if="selectedSupplierWarehouse" class="form-tip">
                已按供应商专属仓库自动分配：{{ selectedSupplierWarehouse.warehouseName }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商">
              <div class="quick-select">
                <el-select v-model="form.supplierId" filterable style="width: 100%">
                  <el-option v-for="item in lookups.suppliers || []" :key="item.id" :label="item.supplierName" :value="item.id" />
                </el-select>
                <el-button icon="el-icon-plus" @click="openQuickCreate('suppliers')" />
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
            <el-form-item label="来源单号">
              <el-input v-model="form.sourceNo" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="预计到仓">
              <el-date-picker
                v-model="form.expectedArrivalTime"
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
          <strong>入库明细</strong>
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
          <el-table-column label="货物码类型" width="130">
            <template slot-scope="scope">
              <el-select v-model="scope.row.cargoCodeType" style="width: 100%">
                <el-option label="二维码" value="QR_CODE" />
                <el-option label="条形码" value="BAR_CODE" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="指定货物码" min-width="170">
            <template slot-scope="scope">
              <el-input v-model="scope.row.cargoCode" placeholder="留空自动生成" />
            </template>
          </el-table-column>
          <el-table-column label="商家平台" width="130">
            <template slot-scope="scope">
              <el-select v-model="scope.row.externalPlatform" clearable style="width: 100%">
                <el-option label="淘宝" value="TAOBAO" />
                <el-option label="天猫" value="TMALL" />
                <el-option label="拼多多" value="PINDUODUO" />
                <el-option label="美团" value="MEITUAN" />
                <el-option label="淘宝闪购" value="TAOBAO_FLASH" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="外部商家码" min-width="170">
            <template slot-scope="scope">
              <el-input v-model="scope.row.externalCode" placeholder="可粘贴平台订单/包裹码" />
            </template>
          </el-table-column>
          <el-table-column label="预计数量" width="120">
            <template slot-scope="scope">
              <el-input-number v-model="scope.row.expectedQty" :min="0" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="实收数量" width="120">
            <template slot-scope="scope">
              <el-input-number v-model="scope.row.actualQty" :min="0" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="合格数量" width="120">
            <template slot-scope="scope">
              <el-input-number v-model="scope.row.qualifiedQty" :min="0" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="上架库位" min-width="180">
            <template slot-scope="scope">
              <div class="quick-select">
                <el-select v-model="scope.row.locationId" filterable style="width: 100%">
                  <el-option v-for="item in filteredLocations" :key="item.id" :label="item.locationCode" :value="item.id" />
                </el-select>
                <el-button icon="el-icon-plus" :disabled="!form.warehouseId" @click="openQuickCreate('locations', scope.row)" />
              </div>
            </template>
          </el-table-column>
          <el-table-column label="生产日期" width="150">
            <template slot-scope="scope">
              <el-date-picker v-model="scope.row.productionDate" type="date" value-format="yyyy-MM-dd" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="失效日期" width="150">
            <template slot-scope="scope">
              <el-date-picker v-model="scope.row.expiryDate" type="date" value-format="yyyy-MM-dd" style="width: 100%" />
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

    <el-dialog title="入库单明细" :visible.sync="detailVisible" width="920px">
      <el-descriptions :column="3" border v-if="currentRow.id">
        <el-descriptions-item label="作业编码">{{ currentRow.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ currentRow.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ currentRow.status }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ currentRow.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="预计数量">{{ currentRow.totalExpectedQty }}</el-descriptions-item>
        <el-descriptions-item label="收货时间">{{ currentRow.receivedAt || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="currentRow.items || []" border style="margin-top: 16px">
        <el-table-column prop="skuCode" label="SKU" min-width="140" />
        <el-table-column prop="productName" label="商品名称" min-width="160" />
        <el-table-column prop="batchNo" label="批次号" min-width="140" />
        <el-table-column prop="cargoCodeType" label="码类型" width="100" />
        <el-table-column prop="cargoCode" label="货物码" min-width="170" />
        <el-table-column prop="externalPlatform" label="商家平台" width="110" />
        <el-table-column prop="externalCode" label="外部商家码" min-width="160" />
        <el-table-column prop="cargoCodeContent" label="码内容" min-width="220" show-overflow-tooltip />
        <el-table-column label="库位" min-width="170">
          <template slot-scope="scope">
            {{ scope.row.locationFullName || resolveLocationText(scope.row.locationId) }}
          </template>
        </el-table-column>
        <el-table-column label="扫码确认" width="110">
          <template slot-scope="scope">
            <el-tag :type="scope.row.putawayScanConfirmed ? 'success' : 'warning'">
              {{ scope.row.putawayScanConfirmed ? '已确认' : '待扫码' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="码标签" width="100">
          <template slot-scope="scope">
            <el-button type="text" @click="openCargoCode(scope.row)">预览</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="expectedQty" label="预计数量" width="120" />
        <el-table-column prop="actualQty" label="实收数量" width="120" />
        <el-table-column prop="qualifiedQty" label="合格数量" width="120" />
        <el-table-column prop="locationId" label="库位ID" width="120" />
      </el-table>
    </el-dialog>

    <el-dialog title="货物码标签" :visible.sync="cargoCodeVisible" width="920px">
      <div v-if="cargoCodeItem.id" class="cargo-code-preview">
        <div class="cargo-code-label" ref="cargoCodeLabel">
          <div class="cargo-code-title">{{ cargoCodeItem.productName }}</div>
          <div class="cargo-code-meta">
            作业 {{ cargoCodeItem.operationCode || cargoCodeItem.orderNo || '-' }}
          </div>
          <div class="cargo-code-meta">
            库位 {{ cargoCodeItem.locationFullName || cargoCodeItem.locationCode || resolveLocationText(cargoCodeItem.locationId) }}
          </div>
          <div class="cargo-code-meta">
            {{ cargoCodeItem.skuCode }} / {{ cargoCodeItem.batchNo || '-' }}
          </div>
          <div v-if="cargoCodeSvg" class="code-svg-box" v-html="cargoCodeSvg"></div>
          <div v-else class="code-render-placeholder">
            {{ cargoCodeRenderLoading ? '码图形生成中...' : cargoCodeRenderError || '暂无码图形' }}
          </div>
          <div class="cargo-code-value">{{ cargoCodeItem.cargoCode }}</div>
          <div class="cargo-code-meta">
            {{ platformText(cargoCodeItem.externalPlatform) }} / {{ cargoCodeItem.externalCode || '无外部码' }}
          </div>
        </div>
      </div>
      <span slot="footer">
        <el-button @click="copyText(cargoCodeRawContent(cargoCodeItem))">复制码内容</el-button>
        <el-button
          type="success"
          plain
          :loading="cargoScanConfirming"
          :disabled="cargoCodeItem.putawayScanConfirmed"
          @click="confirmCargoCodeScan(cargoCodeRawContent(cargoCodeItem))"
        >扫码确认入库</el-button>
        <el-button
          type="success"
          :loading="cargoTransferLoading"
          :disabled="!cargoCodeItem.putawayScanConfirmed || !!cargoCodeItem.outboundOrderId"
          @click="transferCargoCodeToOutbound(cargoCodeRawContent(cargoCodeItem))"
        >直接出库</el-button>
        <el-button type="primary" :loading="cargoCodeRenderLoading" :disabled="!cargoCodeSvg" @click="printCargoCodeLabel">打印标签</el-button>
      </span>
    </el-dialog>

    <el-dialog title="已生成码记录" :visible.sync="cargoCodeRecordsVisible" width="1040px">
      <el-table :data="cargoCodeRecords" border v-loading="cargoCodeRecordsLoading">
        <el-table-column prop="id" label="记录ID" width="90" />
        <el-table-column prop="cargoCodeType" label="码类型" width="100">
          <template slot-scope="scope">{{ codeTypeText(scope.row.cargoCodeType) }}</template>
        </el-table-column>
        <el-table-column prop="cargoCode" label="货物码" min-width="190" />
        <el-table-column prop="operationCode" label="作业编码" min-width="170" />
        <el-table-column label="库位" min-width="190">
          <template slot-scope="scope">
            {{ scope.row.locationCode ? `${scope.row.locationCode} / ${scope.row.locationName || '-'}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="inboundOrderId" label="入库单ID" width="110" />
        <el-table-column prop="inboundOrderItemId" label="明细ID" width="100" />
        <el-table-column prop="renderFormat" label="渲染格式" width="110" />
        <el-table-column prop="createdAt" label="生成时间" min-width="170" />
        <el-table-column prop="rawContent" label="码内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" @click="openSavedCargoCode(scope.row)">预览/打印</el-button>
          </template>
        </el-table-column>
      </el-table>
      <span slot="footer">
        <el-button @click="fetchCargoCodeRecords">刷新记录</el-button>
        <el-button @click="cargoCodeRecordsVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { get, post } from '../api'
import QuickCreateDialog from '../components/QuickCreateDialog.vue'

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

function buildCode39Svg(value) {
  const code = String(value || '').trim().toUpperCase()
  if (!code) {
    return ''
  }
  const fullCode = `*${code}*`
  const invalid = fullCode.split('').find(char => !code39Patterns[char])
  if (invalid) {
    return ''
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
    code39Patterns[char].split('').forEach((part, index) => {
      const width = part === 'w' ? wide : narrow
      if (index % 2 === 0) {
        rects.push(`<rect x="${x}" y="${barTop}" width="${width}" height="${barHeight}" fill="#111827" />`)
      }
      x += width
    })
    x += gap
  })
  const width = x + margin - gap
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="112" viewBox="0 0 ${width} 112" role="img" aria-label="${escapeHtml(code)}">
    <rect width="100%" height="100%" fill="#ffffff" />
    ${rects.join('')}
    <text x="${width / 2}" y="102" text-anchor="middle" font-family="Consolas, monospace" font-size="14" fill="#111827">${escapeHtml(code)}</text>
  </svg>`
}

const createItem = () => ({
  productId: null,
  batchNo: '',
  cargoCodeType: 'QR_CODE',
  cargoCode: '',
  externalPlatform: '',
  externalCode: '',
  expectedQty: 0,
  actualQty: 0,
  qualifiedQty: 0,
  locationId: null,
  productionDate: '',
  expiryDate: '',
  remark: ''
})

export default {
  name: 'InboundsPage',
  components: {
    QuickCreateDialog
  },
  data() {
    return {
      rows: [],
      lookups: {},
      dialogVisible: false,
      detailVisible: false,
      cargoCodeVisible: false,
      cargoCodeSvg: '',
      cargoCodeRenderLoading: false,
      cargoCodeRenderError: '',
      cargoScanConfirming: false,
      cargoTransferLoading: false,
      cargoCodeRecordsVisible: false,
      cargoCodeRecordsLoading: false,
      cargoCodeRecords: [],
      scannerBuffer: '',
      scannerBufferTimer: null,
      quickCreateVisible: false,
      quickCreateResource: '',
      quickCreateTarget: null,
      currentRow: {},
      cargoCodeItem: {},
      form: {
        orderNo: '',
        warehouseId: null,
        supplierId: null,
        ownerId: null,
        sourceNo: '',
        expectedArrivalTime: '',
        operatorName: '系统管理员',
        remark: '',
        items: [createItem()]
      }
    }
  },
  async created() {
    await Promise.all([this.fetchRows(), this.fetchLookups()])
    if (this.$route.path === '/cargo-code-records') {
      await this.openCargoCodeRecords()
    }
  },
  mounted() {
    window.addEventListener('keydown', this.handleCargoScannerKeydown, true)
  },
  beforeDestroy() {
    window.removeEventListener('keydown', this.handleCargoScannerKeydown, true)
    if (this.scannerBufferTimer) {
      clearTimeout(this.scannerBufferTimer)
      this.scannerBufferTimer = null
    }
  },
  computed: {
    selectedSupplier() {
      return (this.lookups.suppliers || []).find(item => item.id === this.form.supplierId) || null
    },
    selectedSupplierWarehouse() {
      if (!this.selectedSupplier || !this.selectedSupplier.warehouseId) {
        return null
      }
      return (this.lookups.warehouses || []).find(item => item.id === this.selectedSupplier.warehouseId) || null
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
  watch: {
    selectedSupplier(supplier) {
      if (supplier && supplier.warehouseId) {
        this.form.warehouseId = supplier.warehouseId
      }
      if (supplier && supplier.platformType) {
        this.form.items.forEach(item => {
          if (!item.externalPlatform) {
            item.externalPlatform = supplier.platformType
          }
        })
      }
    }
  },
  methods: {
    async fetchRows() {
      const response = await get('/inbounds')
      this.rows = response.data || []
    },
    async fetchLookups() {
      const response = await get('/lookups', { _ts: Date.now() })
      this.lookups = response.data || {}
    },
    async openCreate() {
      await this.fetchLookups()
      this.form = {
        orderNo: '',
        warehouseId: null,
        supplierId: null,
        ownerId: null,
        sourceNo: '',
        expectedArrivalTime: '',
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
      } else if (resource === 'suppliers') {
        this.form.supplierId = created.id
      } else if (resource === 'owners') {
        this.form.ownerId = created.id
      } else if (resource === 'products' && this.quickCreateTarget) {
        this.quickCreateTarget.productId = created.id
      } else if (resource === 'locations' && this.quickCreateTarget) {
        this.quickCreateTarget.locationId = created.id
      }
      this.$message.success('新增成功，已自动选中')
    },
    async openCargoCode(row) {
      const records = row.cargoCodeRecords || []
      const latestRecord = records.length ? records[records.length - 1] : null
      this.cargoCodeItem = {
        ...row,
        operationCode: row.operationCode || (latestRecord && latestRecord.operationCode),
        operationType: row.operationType || (latestRecord && latestRecord.operationType),
        locationCode: row.locationCode || (latestRecord && latestRecord.locationCode),
        locationName: row.locationName || (latestRecord && latestRecord.locationName),
        zoneName: row.zoneName || (latestRecord && latestRecord.zoneName)
      }
      this.cargoCodeVisible = true
      this.cargoCodeSvg = this.savedCargoCodeSvg(this.cargoCodeItem)
      if (this.cargoCodeItem.cargoCodeType === 'BAR_CODE' || !this.cargoCodeSvg) {
        await this.renderCargoCode(this.cargoCodeItem)
      }
    },
    async openSavedCargoCode(record) {
      this.cargoCodeItem = {
        id: record.inboundOrderItemId || record.id,
        productName: `码记录 #${record.id}`,
        skuCode: `入库单 ${record.inboundOrderId || '-'}`,
        batchNo: `明细 ${record.inboundOrderItemId || '-'}`,
        operationCode: record.operationCode,
        operationType: record.operationType,
        locationId: record.locationId,
        locationCode: record.locationCode,
        locationName: record.locationName,
        zoneName: record.zoneName,
        locationFullName: record.locationCode ? `${record.zoneName ? record.zoneName + ' / ' : ''}${record.locationCode} / ${record.locationName || '-'}` : '',
        cargoCode: record.cargoCode,
        cargoCodeType: record.cargoCodeType,
        cargoCodeContent: record.rawContent,
        externalPlatform: record.externalPlatform,
        externalCode: record.externalCode,
        cargoCodeRecords: [record]
      }
      this.cargoCodeSvg = record.svgContent || ''
      this.cargoCodeRenderError = this.cargoCodeSvg ? '' : '该记录未保存码图形'
      this.cargoCodeVisible = true
      if (this.cargoCodeItem.cargoCodeType === 'BAR_CODE') {
        await this.renderCargoCode(this.cargoCodeItem)
      }
    },
    async openCargoCodeRecords() {
      this.cargoCodeRecordsVisible = true
      await this.fetchCargoCodeRecords()
    },
    async fetchCargoCodeRecords() {
      try {
        this.cargoCodeRecordsLoading = true
        const response = await get('/scan/cargo-code-records', { limit: 300, _ts: Date.now() })
        this.cargoCodeRecords = response.data || []
      } catch (error) {
        this.$message.error(error.message || '加载码记录失败')
      } finally {
        this.cargoCodeRecordsLoading = false
      }
    },
    async renderCargoCode(row) {
      this.cargoCodeSvg = ''
      this.cargoCodeRenderError = ''
      const rawContent = this.cargoCodeRawContent(row)
      if (!rawContent) {
        this.cargoCodeRenderError = '码内容为空'
        return
      }
      try {
        this.cargoCodeRenderLoading = true
        const response = await post('/scan/render-code', {
          rawContent,
          scanFormat: row.cargoCodeType || 'QR_CODE',
          width: row.cargoCodeType === 'BAR_CODE' ? 720 : 240,
          height: row.cargoCodeType === 'BAR_CODE' ? 180 : 240
        })
        this.cargoCodeSvg = response.data && response.data.svg ? response.data.svg : ''
        if (!this.cargoCodeSvg) {
          this.cargoCodeRenderError = '码图形生成失败'
        }
      } catch (error) {
        if (row.cargoCodeType === 'BAR_CODE') {
          this.cargoCodeSvg = buildCode39Svg(row.cargoCode)
        }
        this.cargoCodeRenderError = this.cargoCodeSvg ? '' : error.message || '码图形生成失败'
      } finally {
        this.cargoCodeRenderLoading = false
      }
    },
    handleCargoScannerKeydown(event) {
      if (!this.cargoCodeVisible || this.cargoScanConfirming || this.cargoTransferLoading) {
        return
      }
      if (event.ctrlKey || event.altKey || event.metaKey) {
        return
      }
      if (event.key === 'Enter' || event.key === 'Tab') {
        if (this.scannerBuffer) {
          event.preventDefault()
          event.stopPropagation()
          this.flushCargoScannerBuffer()
        }
        return
      }
      if (!event.key || event.key.length !== 1) {
        return
      }
      this.scannerBuffer += event.key
      if (this.scannerBufferTimer) {
        clearTimeout(this.scannerBufferTimer)
      }
      this.scannerBufferTimer = setTimeout(() => {
        this.flushCargoScannerBuffer()
      }, 180)
    },
    flushCargoScannerBuffer() {
      if (this.scannerBufferTimer) {
        clearTimeout(this.scannerBufferTimer)
        this.scannerBufferTimer = null
      }
      const scannedCode = this.scannerBuffer.trim()
      this.scannerBuffer = ''
      if (scannedCode.length < 4) {
        return
      }
      if (this.cargoCodeItem && this.cargoCodeItem.putawayScanConfirmed) {
        this.transferCargoCodeToOutbound(scannedCode)
      } else {
        this.confirmCargoCodeScan(scannedCode)
      }
    },
    async confirmCargoCodeScan(rawContent) {
      const scanContent = (rawContent || '').trim()
      if (!scanContent) {
        this.$message.error('请先扫描货物标签')
        return
      }
      try {
        this.cargoScanConfirming = true
        const response = await post('/inbounds/scan-putaway', {
          rawContent: scanContent,
          scanFormat: this.cargoCodeItem.cargoCodeType || 'AUTO',
          sourceDevice: 'WEB_INBOUND_LABEL',
          scannerInterface: 'WIRED_SCANNER',
          scannerDeviceId: 'WEB_LABEL_PREVIEW',
          operatorName: '系统管理员',
          remark: '入库管理标签弹窗扫码确认'
        })
        const data = response.data || {}
        const confirmedItemId = data.confirmedItemId || this.cargoCodeItem.id
        const confirmedItem = (data.items || []).find(item => item.id === confirmedItemId)
        if (confirmedItem) {
          this.cargoCodeItem = {
            ...this.cargoCodeItem,
            ...confirmedItem
          }
        } else {
          this.cargoCodeItem = {
            ...this.cargoCodeItem,
            putawayScanConfirmed: true
          }
        }
        if (this.currentRow && this.currentRow.id === data.id) {
          this.currentRow = data
        }
        this.$message.success(data.message || response.message || '扫码确认入库成功')
        await Promise.all([
          this.fetchRows(),
          this.cargoCodeRecordsVisible ? this.fetchCargoCodeRecords() : Promise.resolve()
        ])
      } catch (error) {
        this.$message.error(error.message || '扫码确认入库失败')
      } finally {
        this.cargoScanConfirming = false
      }
    },
    async transferCargoCodeToOutbound(rawContent) {
      const scanContent = (rawContent || '').trim()
      if (!scanContent) {
        this.$message.error('请先扫描商品条形码、SKU 或货物码')
        return
      }
      if (!this.cargoCodeItem.putawayScanConfirmed) {
        this.$message.error('请先扫码确认入库')
        return
      }
      if (this.cargoCodeItem.outboundOrderId) {
        this.$message.warning(`该货物已转入出库单 ${this.cargoCodeItem.outboundOrderNo || ''}`)
        return
      }
      try {
        this.cargoTransferLoading = true
        const response = await post('/outbounds/scan-transfer', {
          rawContent: scanContent,
          scanFormat: this.cargoCodeItem.cargoCodeType || 'AUTO',
          sourceDevice: 'WEB_INBOUND_LABEL',
          scannerInterface: 'WIRED_SCANNER',
          scannerDeviceId: 'WEB_LABEL_PREVIEW',
          operatorName: '系统管理员'
        })
        const data = response.data || {}
        const transferredItemId = this.cargoCodeItem.id
        this.cargoCodeItem = {
          ...this.cargoCodeItem,
          outboundOrderId: data.id,
          outboundOrderNo: data.orderNo || data.code,
          outboundTransferredAt: data.shippedAt || null
        }
        if (this.currentRow && Array.isArray(this.currentRow.items)) {
          this.currentRow = {
            ...this.currentRow,
            items: this.currentRow.items.filter(item => item.id !== transferredItemId)
          }
        }
        this.$message.success(data.message || response.message || '已直接转入出库单')
        await Promise.all([
          this.fetchRows(),
          this.cargoCodeRecordsVisible ? this.fetchCargoCodeRecords() : Promise.resolve()
        ])
        this.cargoCodeVisible = false
      } catch (error) {
        this.$message.error(error.message || '直接出库失败')
      } finally {
        this.cargoTransferLoading = false
      }
    },
    cargoCodeRawContent(row) {
      if (!row) {
        return ''
      }
      const savedRecord = row.cargoCodeRecords && row.cargoCodeRecords.length ? row.cargoCodeRecords[row.cargoCodeRecords.length - 1] : null
      if (savedRecord && savedRecord.rawContent) {
        return savedRecord.rawContent
      }
      return row.cargoCodeType === 'BAR_CODE'
        ? row.cargoCode
        : row.cargoCodeContent || row.cargoCode
    },
    savedCargoCodeSvg(row) {
      if (!row) {
        return ''
      }
      const records = row.cargoCodeRecords || []
      if (!records.length) {
        return row.cargoCodeSvg || ''
      }
      return records[records.length - 1].svgContent || row.cargoCodeSvg || ''
    },
    addItem() {
      const item = createItem()
      if (this.selectedSupplier && this.selectedSupplier.platformType) {
        item.externalPlatform = this.selectedSupplier.platformType
      }
      this.form.items.push(item)
    },
    removeItem(index) {
      this.form.items.splice(index, 1)
      if (!this.form.items.length) {
        this.form.items.push(createItem())
      }
    },
    async submit() {
      try {
        if (this.selectedSupplier && this.selectedSupplier.warehouseId) {
          this.form.warehouseId = this.selectedSupplier.warehouseId
        }
        await post('/inbounds', this.form)
        this.$message.success('创建成功')
        this.dialogVisible = false
        await this.fetchRows()
      } catch (error) {
        this.$message.error(error.message || '创建失败')
      }
    },
    async handleReceive(row) {
      try {
        await post(`/inbounds/${row.id}/receive`)
        this.$message.success('收货完成')
        await this.fetchRows()
      } catch (error) {
        this.$message.error(error.message || '收货失败')
      }
    },
    async handlePutaway(row) {
      try {
        await post(`/inbounds/${row.id}/putaway`)
        this.$message.success('上架完成')
        await this.fetchRows()
      } catch (error) {
        this.$message.error(error.message || '上架失败')
      }
    },
    async submitApproval(row) {
      try {
        await post('/approvals/submit', {
          approvalType: 'ORDER_APPROVAL',
          bizType: 'INBOUND_ORDER',
          bizId: row.id,
          bizNo: row.orderNo,
          applicantName: '系统管理员',
          approverName: '仓库主管',
          applyReason: `入库单 ${row.orderNo} 需要审批`
        })
        this.$message.success('审批已提交')
      } catch (error) {
        this.$message.error(error.message || '提交审批失败')
      }
    },
    statusType(status) {
      if (status === 'PUTAWAY_COMPLETED') return 'success'
      if (status === 'RECEIVED') return 'warning'
      return 'info'
    },
    allItemsScanConfirmed(row) {
      const items = row && row.items ? row.items : []
      return items.length > 0 && items.every(item => item.putawayScanConfirmed)
    },
    platformText(platform) {
      const map = {
        TAOBAO: '淘宝',
        TMALL: '天猫',
        PINDUODUO: '拼多多',
        MEITUAN: '美团',
        TAOBAO_FLASH: '淘宝闪购',
        OTHER: '其他'
      }
      return map[platform] || platform || '-'
    },
    codeTypeText(type) {
      return { QR_CODE: '二维码', BAR_CODE: '条形码' }[type] || type || '-'
    },
    resolveLocationText(locationId) {
      const location = (this.lookups.locations || []).find(item => item.id === locationId)
      if (!location) {
        return locationId ? `库位ID ${locationId}` : '-'
      }
      return `${location.locationCode} / ${location.locationName}`
    },
    async copyText(value) {
      if (!value) {
        return
      }
      try {
        await navigator.clipboard.writeText(value)
        this.$message.success('已复制')
      } catch (error) {
        this.$message.error('复制失败，请手动复制')
      }
    },
    printCargoCodeLabel() {
      if (!this.cargoCodeItem.id) {
        return
      }
      const html = this.$refs.cargoCodeLabel ? this.$refs.cargoCodeLabel.outerHTML : ''
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
            <title>${escapeHtml(this.cargoCodeItem.cargoCode)}</title>
            <style>
              @page { size: 78mm 52mm; margin: 4mm; }
              * { box-sizing: border-box; }
              body { margin: 0; font-family: Arial, "Microsoft YaHei", sans-serif; color: #111827; }
              .cargo-code-label { width: 70mm; min-height: 44mm; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2mm; }
              .cargo-code-title { max-width: 100%; font-size: 12px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
              .cargo-code-meta { max-width: 100%; font-size: 10px; color: #4b5563; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
              .cargo-code-value { font-size: 10px; font-family: Consolas, monospace; letter-spacing: 0.06em; }
              .code-svg-box { max-width: 66mm; max-height: 28mm; overflow: hidden; display: flex; justify-content: center; }
              .code-svg-box svg { max-width: 66mm; max-height: 28mm; width: auto; height: auto; }
              .code-render-placeholder { width: 62mm; min-height: 22mm; display: flex; align-items: center; justify-content: center; border: 1px solid #d8dee6; font-size: 9px; color: #64748b; }
            </style>
          </head>
          <body>${html}</body>
        </html>
      `)
      doc.close()
      setTimeout(() => {
        frame.contentWindow.focus()
        frame.contentWindow.print()
        setTimeout(() => document.body.removeChild(frame), 500)
      }, 100)
    }
  }
}
</script>

<style scoped>
.form-tip {
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
  margin-top: 6px;
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

.cargo-code-preview {
  display: flex;
  justify-content: center;
}

.cargo-code-label {
  width: 100%;
  min-height: 280px;
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

.cargo-code-title {
  max-width: 100%;
  color: #111827;
  font-size: 16px;
  line-height: 22px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cargo-code-meta,
.cargo-code-value {
  max-width: 100%;
  color: #64748b;
  font-size: 13px;
  line-height: 18px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cargo-code-value {
  color: #111827;
  font-family: Consolas, monospace;
  letter-spacing: 0.06em;
}

.code-svg-box {
  max-width: 100%;
  display: flex;
  justify-content: center;
  overflow: auto;
}

.code-svg-box ::v-deep svg {
  max-width: none;
  height: auto;
}

.code-render-placeholder {
  width: 100%;
  min-height: 160px;
  border: 1px solid #d8dee6;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
