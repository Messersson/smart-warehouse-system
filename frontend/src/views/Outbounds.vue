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

    <el-table :data="rows" stripe border>
      <el-table-column prop="orderNo" label="出库单号" min-width="160" />
      <el-table-column prop="warehouseName" label="仓库" min-width="120" />
      <el-table-column prop="customerName" label="客户" min-width="120" />
      <el-table-column prop="status" label="状态" width="140">
        <template slot-scope="scope">
          <el-tag :type="statusType(scope.row.status)">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="priorityLevel" label="优先级" width="100" />
      <el-table-column prop="totalPlannedQty" label="计划数量" width="120" />
      <el-table-column prop="plannedShipTime" label="计划发运" min-width="180" />
      <el-table-column label="操作" width="340" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" @click="openDetail(scope.row)">明细</el-button>
          <el-button type="text" @click="handlePicking(scope.row)" :disabled="scope.row.status !== 'CREATED'">拣货完成</el-button>
          <el-button type="text" @click="handleShip(scope.row)" :disabled="scope.row.status === 'SHIPPED'">发运</el-button>
          <el-button type="text" @click="submitApproval(scope.row)">提交审批</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="新建出库单" :visible.sync="dialogVisible" width="1080px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="出库单号">
              <el-input v-model="form.orderNo" placeholder="可留空自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所属仓库">
              <el-select v-model="form.warehouseId" filterable style="width: 100%">
                <el-option v-for="item in lookups.warehouses || []" :key="item.id" :label="item.warehouseName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="客户">
              <el-select v-model="form.customerId" filterable style="width: 100%">
                <el-option v-for="item in lookups.customers || []" :key="item.id" :label="item.customerName" :value="item.id" />
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
              <el-select v-model="scope.row.productId" filterable style="width: 100%">
                <el-option v-for="item in lookups.products || []" :key="item.id" :label="`${item.skuCode} / ${item.productName}`" :value="item.id" />
              </el-select>
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
          <el-table-column label="已发数量" width="120">
            <template slot-scope="scope">
              <el-input-number v-model="scope.row.shippedQty" :min="0" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="拣货库位" min-width="180">
            <template slot-scope="scope">
              <el-select v-model="scope.row.locationId" filterable style="width: 100%">
                <el-option v-for="item in lookups.locations || []" :key="item.id" :label="item.locationCode" :value="item.id" />
              </el-select>
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

    <el-dialog title="出库单明细" :visible.sync="detailVisible" width="920px">
      <el-descriptions :column="3" border v-if="currentRow.id">
        <el-descriptions-item label="出库单号">{{ currentRow.orderNo }}</el-descriptions-item>
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
        <el-table-column prop="shippedQty" label="已发数量" width="120" />
        <el-table-column prop="locationId" label="库位ID" width="120" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { get, post } from '../api'

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
  data() {
    return {
      rows: [],
      lookups: {},
      dialogVisible: false,
      detailVisible: false,
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
  async created() {
    await Promise.all([this.fetchRows(), this.fetchLookups()])
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
      } catch (error) {
        this.$message.error(error.message || '发运失败')
      }
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
