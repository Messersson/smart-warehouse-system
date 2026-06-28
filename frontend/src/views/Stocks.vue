<template>
  <div class="page-card">
    <div class="page-toolbar">
      <div>
        <h2 style="margin: 0">实时库存</h2>
        <div style="color: #64748b; margin-top: 6px">按仓库、商品、批次查看当前库存余额，出入库变化通过库存流水追溯</div>
      </div>
      <div>
        <el-select v-model="filters.warehouseId" clearable placeholder="按仓库筛选" style="width: 180px; margin-right: 12px">
          <el-option v-for="item in lookups.warehouses || []" :key="item.id" :label="item.warehouseName" :value="item.id" />
        </el-select>
        <el-input v-model="filters.keyword" clearable placeholder="搜索SKU/商品/批次" style="width: 220px; margin-right: 12px" />
        <el-button type="primary" @click="fetchRows">查询</el-button>
      </div>
    </div>

    <el-table :data="rows" stripe border show-summary :summary-method="getSummaries">
      <el-table-column prop="warehouseName" label="仓库" min-width="120" />
      <el-table-column prop="skuCode" label="SKU" min-width="140" />
      <el-table-column prop="productName" label="商品名称" min-width="180" />
      <el-table-column prop="locationCode" label="库位" min-width="120" />
      <el-table-column prop="batchNo" label="批次号" min-width="150" />
      <el-table-column prop="quantity" label="总库存" width="120" />
      <el-table-column prop="lockedQty" label="锁定库存" width="120" />
      <el-table-column prop="availableQty" label="可用库存" width="120" />
      <el-table-column prop="lastMovementAt" label="最近变动时间" min-width="180" />
      <el-table-column label="操作" width="100" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" @click="openMovements(scope.row)">流水</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="库存变动流水" :visible.sync="movementVisible" width="980px">
      <el-descriptions v-if="currentStock.id" :column="3" border size="small">
        <el-descriptions-item label="仓库">{{ currentStock.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="SKU">{{ currentStock.skuCode }}</el-descriptions-item>
        <el-descriptions-item label="商品">{{ currentStock.productName }}</el-descriptions-item>
        <el-descriptions-item label="库位">{{ currentStock.locationCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="批次">{{ currentStock.batchNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前总库存">{{ currentStock.quantity }}</el-descriptions-item>
      </el-descriptions>
      <el-table v-loading="movementLoading" :data="movements" border stripe style="margin-top: 16px">
        <el-table-column prop="createdAt" label="变动时间" min-width="170" />
        <el-table-column prop="movementType" label="类型" width="130">
          <template slot-scope="scope">
            <el-tag :type="movementTagType(scope.row.movementType)" size="mini">{{ movementLabel(scope.row.movementType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sourceNo" label="来源单号" min-width="160" />
        <el-table-column prop="beforeQty" label="变动前" width="100" />
        <el-table-column prop="changeQty" label="变动数量" width="110">
          <template slot-scope="scope">
            <span :class="Number(scope.row.changeQty) < 0 ? 'text-danger' : 'text-success'">
              {{ signedQty(scope.row.changeQty) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="afterQty" label="变动后" width="100" />
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="remark" label="备注" min-width="180" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { get } from '../api'

export default {
  name: 'StocksPage',
  data() {
    return {
      rows: [],
      movements: [],
      currentStock: {},
      movementVisible: false,
      movementLoading: false,
      lookups: {},
      filters: {
        warehouseId: null,
        keyword: ''
      }
    }
  },
  async created() {
    await Promise.all([this.fetchLookups(), this.fetchRows()])
  },
  methods: {
    async fetchLookups() {
      const response = await get('/lookups', { _ts: Date.now() })
      this.lookups = response.data || {}
    },
    async fetchRows() {
      try {
        const response = await get('/stocks', this.filters)
        this.rows = response.data || []
      } catch (error) {
        this.$message.error(error.message || '库存加载失败')
      }
    },
    async openMovements(row) {
      this.currentStock = row
      this.movementVisible = true
      this.movementLoading = true
      try {
        const response = await get(`/stocks/${row.id}/movements`)
        this.movements = response.data || []
      } catch (error) {
        this.$message.error(error.message || '库存流水加载失败')
      } finally {
        this.movementLoading = false
      }
    },
    getSummaries({ columns, data }) {
      const sumProps = new Set(['quantity', 'lockedQty', 'availableQty'])
      return columns.map((column, index) => {
        if (index === 0) {
          return '合计'
        }
        if (!sumProps.has(column.property)) {
          return ''
        }
        return data.reduce((sum, row) => sum + Number(row[column.property] || 0), 0)
      })
    },
    signedQty(value) {
      const number = Number(value || 0)
      return number > 0 ? `+${value}` : value
    },
    movementLabel(type) {
      const labels = {
        INBOUND: '入库入账',
        OUTBOUND: '出库扣减',
        CUSTOMER_PICKUP: '客户取件'
      }
      return labels[type] || type
    },
    movementTagType(type) {
      if (type === 'INBOUND') return 'success'
      if (type === 'OUTBOUND' || type === 'CUSTOMER_PICKUP') return 'warning'
      return 'info'
    }
  }
}
</script>
