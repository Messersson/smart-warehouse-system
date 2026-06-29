<template>
  <div class="virtual-platform-page">
    <div class="page-card">
      <div class="page-toolbar">
        <div>
          <h2>平台供货入库</h2>
          <div>模拟淘宝、拼多多、京东等平台推送供货单，先生成入库单，再进行扫码入库和后续出库。</div>
        </div>
        <el-button icon="el-icon-refresh" :loading="loadingLookups" @click="fetchLookups">刷新基础数据</el-button>
      </div>

      <div class="platform-grid">
        <button
          v-for="platform in platforms"
          :key="platform.value"
          class="platform-card"
          :class="{ active: form.platformType === platform.value }"
          type="button"
          @click="selectPlatform(platform)"
        >
          <span class="platform-logo" :style="{ background: platform.color }">{{ platform.shortName }}</span>
          <span class="platform-info">
            <strong>{{ platform.label }}</strong>
            <small>{{ platform.description }}</small>
          </span>
          <i class="el-icon-check" />
        </button>
      </div>
    </div>

    <div class="order-layout">
      <div class="page-card">
        <div class="section-heading">
          <h3>供货单信息</h3>
          <el-button type="primary" plain size="mini" icon="el-icon-document-add" @click="resetForm">生成新供货单号</el-button>
        </div>

        <el-form :model="form" label-width="110px">
          <el-row :gutter="14">
            <el-col :span="12">
              <el-form-item label="供货平台">
                <el-input :value="selectedPlatform.label" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="平台单号">
                <el-input v-model="form.platformOrderNo" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="平台店铺">
                <el-input v-model="form.shopName" />
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
            <el-col :span="8">
              <el-form-item label="入库仓库">
                <el-select v-model="form.warehouseId" filterable style="width: 100%" @change="handleWarehouseChanged">
                  <el-option v-for="item in lookups.warehouses || []" :key="item.id" :label="item.warehouseName" :value="item.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="供应商">
                <el-select v-model="form.supplierId" clearable filterable style="width: 100%" @change="handleSupplierChanged">
                  <el-option v-for="item in lookups.suppliers || []" :key="item.id" :label="item.supplierName" :value="item.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="货主">
                <el-select v-model="form.ownerId" filterable style="width: 100%">
                  <el-option v-for="item in lookups.owners || []" :key="item.id" :label="item.ownerName" :value="item.id" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <div class="section-heading compact">
            <h3>收货信息</h3>
          </div>
          <el-row :gutter="14">
            <el-col :span="8">
              <el-form-item label="收货人">
                <el-input v-model="form.receiverName" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="联系方式">
                <el-input v-model="form.receiverPhone" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="物流单号">
                <el-input v-model="form.logisticsNo" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="平台发货地址">
                <el-input v-model="form.receiverAddress" />
              </el-form-item>
            </el-col>
          </el-row>

          <div class="section-heading compact">
            <h3>供货明细</h3>
            <el-button type="primary" plain size="mini" icon="el-icon-plus" @click="addItem">添加商品</el-button>
          </div>
          <el-table :data="form.items" border size="small">
            <el-table-column label="商品" min-width="240">
              <template slot-scope="scope">
                <el-select v-model="scope.row.productId" filterable style="width: 100%" @change="syncProduct(scope.row)">
                  <el-option
                    v-for="item in lookups.products || []"
                    :key="item.id"
                    :label="`${item.skuCode} / ${item.productName}`"
                    :value="item.id"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="平台 SKU" min-width="145">
              <template slot-scope="scope">
                <el-input v-model="scope.row.platformSku" />
              </template>
            </el-table-column>
            <el-table-column label="条码" min-width="145">
              <template slot-scope="scope">
                <el-input v-model="scope.row.barcode" />
              </template>
            </el-table-column>
            <el-table-column label="预计入库数" width="120">
              <template slot-scope="scope">
                <el-input-number v-model="scope.row.quantity" :min="1" :controls="false" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="入库库位" min-width="170">
              <template slot-scope="scope">
                <el-select v-model="scope.row.locationId" filterable style="width: 100%">
                  <el-option
                    v-for="item in filteredLocations"
                    :key="item.id"
                    :label="item.locationCode"
                    :value="item.id"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="批次" min-width="130">
              <template slot-scope="scope">
                <el-input v-model="scope.row.batchNo" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="86">
              <template slot-scope="scope">
                <el-button type="text" class="text-danger" @click="removeItem(scope.$index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-form-item label="备注" class="remark-field">
            <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入需求" />
          </el-form-item>

          <div class="submit-bar">
            <el-button @click="$router.push('/inbounds')">查看入库管理</el-button>
            <el-button type="primary" :loading="submitting" icon="el-icon-s-promotion" @click="submitOrder">
              推送供货入库单
            </el-button>
          </div>
        </el-form>
      </div>

      <div class="page-card side-panel">
        <div class="section-heading">
          <h3>入库预览</h3>
        </div>
        <div class="preview-platform">
          <span class="platform-logo" :style="{ background: selectedPlatform.color }">{{ selectedPlatform.shortName }}</span>
          <div>
            <strong>{{ selectedPlatform.label }}</strong>
            <small>{{ form.platformOrderNo }}</small>
          </div>
        </div>
        <div class="preview-lines">
          <div><span>平台店铺</span><strong>{{ form.shopName || '-' }}</strong></div>
          <div><span>入库仓库</span><strong>{{ selectedName('warehouses', form.warehouseId, 'warehouseName') }}</strong></div>
          <div><span>供应商</span><strong>{{ selectedName('suppliers', form.supplierId, 'supplierName') }}</strong></div>
          <div><span>货主</span><strong>{{ selectedName('owners', form.ownerId, 'ownerName') }}</strong></div>
          <div><span>收货人</span><strong>{{ form.receiverName || '-' }}</strong></div>
          <div><span>预计入库</span><strong>{{ totalQuantity }}</strong></div>
        </div>
        <el-alert
          v-if="createdOrder.orderNo"
          type="success"
          :closable="false"
          show-icon
          class="created-alert"
        >
          <template slot="title">
            已生成入库单：{{ createdOrder.orderNo }}
          </template>
        </el-alert>
      </div>
    </div>
  </div>
</template>

<script>
import { get, post } from '../api'

const platforms = [
  { label: '淘宝', value: 'TAOBAO', shortName: '淘', color: '#f97316', description: '模拟淘宝平台供货入库' },
  { label: '拼多多', value: 'PINDUODUO', shortName: '拼', color: '#dc2626', description: '模拟拼多多平台供货入库' },
  { label: '京东', value: 'JD', shortName: '京', color: '#b91c1c', description: '模拟京东平台供货入库' },
  { label: '美团', value: 'MEITUAN', shortName: '美', color: '#f59e0b', description: '模拟美团平台供货入库' },
  { label: '抖音小店', value: 'DOUYIN', shortName: '抖', color: '#111827', description: '模拟抖音小店供货入库' },
  { label: '自建商城', value: 'SELF_MALL', shortName: '商', color: '#2563eb', description: '模拟自建商城供货入库' }
]

function createItem() {
  return {
    productId: null,
    skuCode: '',
    platformSku: '',
    barcode: '',
    quantity: 1,
    locationId: null,
    cargoCodeType: 'QR_CODE',
    externalCode: '',
    batchNo: '',
    remark: ''
  }
}

export default {
  name: 'VirtualPlatforms',
  data() {
    return {
      platforms,
      loadingLookups: false,
      submitting: false,
      lookups: {},
      createdOrder: {},
      form: this.createForm(platforms[0])
    }
  },
  computed: {
    selectedPlatform() {
      return this.platforms.find(item => item.value === this.form.platformType) || this.platforms[0]
    },
    filteredLocations() {
      const locations = this.lookups.locations || []
      if (!this.form.warehouseId) {
        return locations
      }
      return locations.filter(item => item.warehouseId === this.form.warehouseId)
    },
    totalQuantity() {
      return this.form.items.reduce((sum, item) => sum + this.toNumber(item.quantity), 0)
    }
  },
  async created() {
    await this.fetchLookups()
  },
  methods: {
    createForm(platform) {
      const now = new Date()
      const stamp = this.formatStamp(now)
      return {
        platformType: platform.value,
        platformOrderNo: `${platform.value}-IN-${stamp}`,
        shopName: `${platform.label}供货店铺`,
        warehouseId: null,
        supplierId: null,
        customerId: null,
        ownerId: null,
        receiverName: '仓库收货员',
        receiverPhone: '13800000000',
        receiverAddress: `${platform.label}平台供货地址`,
        logisticsNo: `WL-IN-${stamp}`,
        expectedArrivalTime: this.formatDateTime(new Date(now.getTime() + 2 * 60 * 60 * 1000)),
        operatorName: 'Virtual Platform',
        remark: '',
        items: [createItem()]
      }
    },
    async fetchLookups() {
      try {
        this.loadingLookups = true
        const response = await get('/lookups', { _ts: Date.now() })
        this.lookups = response.data || {}
        this.applyDefaultLookups()
      } catch (error) {
        this.$message.error(error.message || '基础数据加载失败')
      } finally {
        this.loadingLookups = false
      }
    },
    applyDefaultLookups() {
      if (!this.form.warehouseId && this.lookups.warehouses?.length) {
        this.form.warehouseId = this.lookups.warehouses[0].id
      }
      if (!this.form.supplierId && this.lookups.suppliers?.length) {
        this.form.supplierId = this.lookups.suppliers[0].id
        this.handleSupplierChanged(this.form.supplierId)
      }
      if (!this.form.ownerId && this.lookups.owners?.length) {
        this.form.ownerId = this.lookups.owners[0].id
      }
      this.form.items.forEach(item => {
        if (!item.productId && this.lookups.products?.length) {
          item.productId = this.lookups.products[0].id
          this.syncProduct(item)
        }
        if (!item.locationId) {
          item.locationId = this.defaultLocationId()
        }
        item.externalCode = item.externalCode || this.form.platformOrderNo
      })
    },
    selectPlatform(platform) {
      const previous = this.form
      this.form = {
        ...this.createForm(platform),
        warehouseId: previous.warehouseId,
        supplierId: previous.supplierId,
        ownerId: previous.ownerId,
        receiverName: previous.receiverName,
        receiverPhone: previous.receiverPhone,
        receiverAddress: previous.receiverAddress,
        items: previous.items
      }
      this.form.items.forEach(item => {
        item.externalCode = this.form.platformOrderNo
      })
      this.createdOrder = {}
      this.applyDefaultLookups()
    },
    resetForm() {
      this.form = this.createForm(this.selectedPlatform)
      this.createdOrder = {}
      this.applyDefaultLookups()
    },
    addItem() {
      const item = createItem()
      if (this.lookups.products?.length) {
        item.productId = this.lookups.products[0].id
        this.syncProduct(item)
      }
      item.locationId = this.defaultLocationId()
      item.externalCode = this.form.platformOrderNo
      this.form.items.push(item)
    },
    removeItem(index) {
      this.form.items.splice(index, 1)
      if (!this.form.items.length) {
        this.form.items.push(createItem())
        this.applyDefaultLookups()
      }
    },
    syncProduct(row) {
      const product = (this.lookups.products || []).find(item => item.id === row.productId)
      if (!product) {
        return
      }
      row.skuCode = product.skuCode
      row.platformSku = row.platformSku || product.skuCode
      row.barcode = row.barcode || product.barcode || ''
    },
    handleSupplierChanged(supplierId) {
      const supplier = (this.lookups.suppliers || []).find(item => item.id === supplierId)
      if (supplier?.warehouseId) {
        this.form.warehouseId = supplier.warehouseId
        this.handleWarehouseChanged()
      }
    },
    handleWarehouseChanged() {
      this.form.items.forEach(item => {
        if (!this.locationBelongsToWarehouse(item.locationId)) {
          item.locationId = this.defaultLocationId()
        }
      })
    },
    defaultLocationId() {
      const location = this.filteredLocations[0]
      return location ? location.id : null
    },
    locationBelongsToWarehouse(locationId) {
      if (!locationId) {
        return false
      }
      return this.filteredLocations.some(item => item.id === locationId)
    },
    validateForm() {
      if (!this.form.platformOrderNo) return '请填写平台供货单号'
      if (!this.form.warehouseId) return '请选择入库仓库'
      if (!this.form.ownerId) return '请选择货主'
      if (!this.form.receiverName || !this.form.receiverPhone) return '请填写收货人和联系方式'
      if (!this.form.items.length) return '请至少添加一个商品'
      const invalidItem = this.form.items.find(item => !item.productId && !item.skuCode && !item.barcode)
      if (invalidItem) return '供货明细必须选择商品或填写 SKU/条码'
      const invalidQty = this.form.items.find(item => this.toNumber(item.quantity) <= 0)
      if (invalidQty) return '预计入库数必须大于 0'
      const invalidLocation = this.form.items.find(item => !item.locationId)
      if (invalidLocation) return '请为每个商品选择入库库位'
      return ''
    },
    async submitOrder() {
      const message = this.validateForm()
      if (message) {
        this.$message.error(message)
        return
      }
      try {
        this.submitting = true
        const response = await post('/virtual-platform/orders', this.form)
        this.createdOrder = response.data || {}
        this.$message.success(`平台供货单已进入入库管理，入库单 ${this.createdOrder.orderNo || ''} 已生成`)
      } catch (error) {
        this.$message.error(error.message || '平台供货入库单推送失败')
      } finally {
        this.submitting = false
      }
    },
    selectedName(resource, id, field) {
      const item = (this.lookups[resource] || []).find(row => row.id === id)
      return item ? item[field] : '-'
    },
    formatStamp(date) {
      return [
        date.getFullYear(),
        String(date.getMonth() + 1).padStart(2, '0'),
        String(date.getDate()).padStart(2, '0'),
        String(date.getHours()).padStart(2, '0'),
        String(date.getMinutes()).padStart(2, '0'),
        String(date.getSeconds()).padStart(2, '0'),
        String(date.getMilliseconds()).padStart(3, '0')
      ].join('')
    },
    formatDateTime(date) {
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`
    },
    toNumber(value) {
      const number = Number(value)
      return Number.isFinite(number) ? number : 0
    }
  }
}
</script>

<style scoped>
.virtual-platform-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.platform-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 12px;
}

.platform-card {
  min-height: 82px;
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) 18px;
  gap: 12px;
  align-items: center;
  text-align: left;
  cursor: pointer;
  color: #172033;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.platform-card:hover,
.platform-card.active {
  border-color: #2563eb;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.12);
}

.platform-card.active {
  transform: translateY(-1px);
}

.platform-card .el-icon-check {
  color: #2563eb;
  opacity: 0;
}

.platform-card.active .el-icon-check {
  opacity: 1;
}

.platform-logo {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
}

.platform-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.platform-info strong {
  font-size: 15px;
  line-height: 20px;
}

.platform-info small,
.preview-platform small {
  color: #667085;
  font-size: 12px;
  line-height: 17px;
}

.order-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 14px;
  align-items: start;
}

.section-heading {
  min-height: 32px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-heading.compact {
  margin-top: 8px;
}

.section-heading h3 {
  margin: 0;
  color: #172033;
  font-size: 16px;
  line-height: 22px;
}

.remark-field {
  margin-top: 16px;
}

.submit-bar {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.side-panel {
  position: sticky;
  top: 0;
}

.preview-platform {
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.preview-platform > div {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.preview-lines {
  margin-top: 12px;
  border-top: 1px solid #edf1f6;
}

.preview-lines div {
  min-height: 38px;
  padding: 8px 0;
  border-bottom: 1px solid #edf1f6;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.preview-lines span {
  color: #667085;
}

.preview-lines strong {
  min-width: 0;
  color: #172033;
  text-align: right;
  font-weight: 650;
  overflow-wrap: anywhere;
}

.created-alert {
  margin-top: 14px;
}

@media (max-width: 1180px) {
  .order-layout {
    grid-template-columns: 1fr;
  }

  .side-panel {
    position: static;
  }
}

@media (max-width: 760px) {
  .platform-card {
    grid-template-columns: 38px minmax(0, 1fr) 16px;
  }

  .platform-logo {
    width: 38px;
    height: 38px;
  }
}
</style>
