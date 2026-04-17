<template>
  <div class="page-card">
    <div class="page-toolbar">
      <div>
        <h2 style="margin: 0">实时库存</h2>
        <div style="color: #64748b; margin-top: 6px">按仓库、商品、批次查看当前可用库存。</div>
      </div>
      <div>
        <el-select v-model="filters.warehouseId" clearable placeholder="按仓库筛选" style="width: 180px; margin-right: 12px">
          <el-option v-for="item in lookups.warehouses || []" :key="item.id" :label="item.warehouseName" :value="item.id" />
        </el-select>
        <el-input v-model="filters.keyword" clearable placeholder="搜索SKU/商品/批次" style="width: 220px; margin-right: 12px" />
        <el-button type="primary" @click="fetchRows">查询</el-button>
      </div>
    </div>

    <el-table :data="rows" stripe border>
      <el-table-column prop="warehouseName" label="仓库" min-width="120" />
      <el-table-column prop="skuCode" label="SKU" min-width="140" />
      <el-table-column prop="productName" label="商品名称" min-width="180" />
      <el-table-column prop="locationCode" label="库位" min-width="120" />
      <el-table-column prop="batchNo" label="批次号" min-width="160" />
      <el-table-column prop="quantity" label="总库存" width="120" />
      <el-table-column prop="lockedQty" label="锁定库存" width="120" />
      <el-table-column prop="availableQty" label="可用库存" width="120" />
      <el-table-column prop="lastMovementAt" label="最近变动时间" min-width="180" />
    </el-table>
  </div>
</template>

<script>
import { get } from '../api'

export default {
  name: 'StocksPage',
  data() {
    return {
      rows: [],
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
    }
  }
}
</script>
