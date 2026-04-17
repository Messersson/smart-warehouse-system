<template>
  <div class="page-card">
    <div class="page-toolbar">
      <div>
        <h2 style="margin: 0">盘点管理</h2>
        <div style="color: #64748b; margin-top: 6px">创建盘点任务、录入实盘数量并执行库存调整。</div>
      </div>
      <el-button type="primary" @click="openCreate">新建盘点单</el-button>
    </div>

    <el-table :data="rows" stripe border>
      <el-table-column prop="takeNo" label="盘点单号" min-width="160" />
      <el-table-column prop="warehouseName" label="仓库" min-width="140" />
      <el-table-column prop="ownerName" label="货主" min-width="140" />
      <el-table-column prop="takeType" label="盘点类型" width="120">
        <template slot-scope="scope">{{ takeTypeText(scope.row.takeType) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="盘点状态" width="140">
        <template slot-scope="scope">{{ statusText(scope.row.status) }}</template>
      </el-table-column>
      <el-table-column prop="operatorName" label="操作人" min-width="120" />
      <el-table-column prop="finishedAt" label="完成时间" min-width="180" />
      <el-table-column label="操作" width="260">
        <template slot-scope="scope">
          <el-button type="text" @click="openDetail(scope.row)">详情</el-button>
          <el-button type="text" @click="finish(scope.row)">完成盘点</el-button>
          <el-button type="text" @click="adjust(scope.row)">调整库存</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="新建盘点单" :visible.sync="createVisible" width="720px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="仓库">
          <el-select v-model="form.warehouseId" style="width: 100%">
            <el-option v-for="item in lookups.warehouses || []" :key="item.id" :label="item.warehouseName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="货主">
          <el-select v-model="form.ownerId" clearable style="width: 100%">
            <el-option v-for="item in lookups.owners || []" :key="item.id" :label="item.ownerName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点类型">
          <el-select v-model="form.takeType" style="width: 100%">
            <el-option label="循环盘点" value="CYCLE" />
            <el-option label="全盘" value="FULL" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人"><el-input v-model="form.operatorName" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="create">创建</el-button>
      </span>
    </el-dialog>

    <el-dialog title="盘点详情" :visible.sync="detailVisible" width="1100px">
      <el-table :data="currentRow.items || []" stripe border>
        <el-table-column prop="skuCode" label="SKU" min-width="140" />
        <el-table-column prop="productName" label="商品名称" min-width="160" />
        <el-table-column prop="systemQty" label="系统数量" width="120" />
        <el-table-column label="实盘数量" width="140">
          <template slot-scope="scope">
            <el-input-number v-model="scope.row.actualQty" :controls="false" :min="0" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column prop="diffQty" label="差异数量" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template slot-scope="scope">{{ itemStatusText(scope.row.status) }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="180">
          <template slot-scope="scope">
            <el-input v-model="scope.row.remark" />
          </template>
        </el-table-column>
      </el-table>
      <span slot="footer">
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="saveCount">保存盘点结果</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { get, post } from '../api'

export default {
  name: 'StockTakesPage',
  data() {
    return {
      rows: [],
      lookups: {},
      createVisible: false,
      detailVisible: false,
      currentRow: {},
      form: {
        warehouseId: null,
        ownerId: null,
        takeType: 'CYCLE',
        operatorName: '系统管理员',
        remark: ''
      }
    }
  },
  async created() {
    await Promise.all([this.fetchLookups(), this.fetchRows()])
  },
  methods: {
    takeTypeText(type) {
      const map = { CYCLE: '循环盘点', FULL: '全盘' }
      return map[type] || type
    },
    statusText(status) {
      const map = { CREATED: '已创建', COUNTING: '盘点中', REVIEW_REQUIRED: '待复核', FINISHED: '已完成', ADJUSTED: '已调整' }
      return map[status] || status
    },
    itemStatusText(status) {
      const map = { PENDING: '待盘点', MATCHED: '一致', DIFF: '有差异', ADJUSTED: '已调整' }
      return map[status] || status
    },
    async fetchLookups() {
      const response = await get('/lookups', { _ts: Date.now() })
      this.lookups = response.data || {}
    },
    async fetchRows() {
      const response = await get('/stock-takes')
      this.rows = response.data || []
    },
    async openCreate() {
      await this.fetchLookups()
      this.form = { warehouseId: null, ownerId: null, takeType: 'CYCLE', operatorName: '系统管理员', remark: '' }
      this.createVisible = true
    },
    async create() {
      await post('/stock-takes', this.form)
      this.$message.success('盘点单创建成功')
      this.createVisible = false
      await this.fetchRows()
    },
    async openDetail(row) {
      const response = await get(`/stock-takes/${row.id}`)
      this.currentRow = response.data || {}
      this.detailVisible = true
    },
    async saveCount() {
      await post(`/stock-takes/${this.currentRow.id}/count`, {
        items: (this.currentRow.items || []).map(item => ({
          id: item.id,
          actualQty: item.actualQty,
          remark: item.remark
        }))
      })
      this.$message.success('盘点结果已保存')
      await this.openDetail(this.currentRow)
      await this.fetchRows()
    },
    async finish(row) {
      await post(`/stock-takes/${row.id}/finish`)
      this.$message.success('盘点已完成')
      await this.fetchRows()
    },
    async adjust(row) {
      await post(`/stock-takes/${row.id}/adjust`)
      this.$message.success('库存调整完成')
      await this.fetchRows()
    }
  }
}
</script>
