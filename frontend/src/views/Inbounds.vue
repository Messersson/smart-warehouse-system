<template>
  <div class="page-card">
    <div class="page-toolbar">
      <div>
        <h2 style="margin: 0">入库管理</h2>
        <div style="color: #64748b; margin-top: 6px">管理收货、上架和入库滞留情况。</div>
      </div>
      <div>
        <el-button type="primary" @click="openCreate">新建入库单</el-button>
      </div>
    </div>

    <el-table :data="rows" stripe border>
      <el-table-column prop="orderNo" label="入库单号" min-width="160" />
      <el-table-column prop="warehouseName" label="仓库" min-width="120" />
      <el-table-column prop="supplierName" label="供应商" min-width="120" />
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
          <el-button type="text" @click="handlePutaway(scope.row)" :disabled="scope.row.status === 'PUTAWAY_COMPLETED'">上架</el-button>
          <el-button type="text" @click="submitApproval(scope.row)">提交审批</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="新建入库单" :visible.sync="dialogVisible" width="1080px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="入库单号">
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
            <el-form-item label="供应商">
              <el-select v-model="form.supplierId" filterable style="width: 100%">
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
              <el-select v-model="scope.row.locationId" filterable style="width: 100%">
                <el-option v-for="item in lookups.locations || []" :key="item.id" :label="item.locationCode" :value="item.id" />
              </el-select>
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

    <el-dialog title="入库单明细" :visible.sync="detailVisible" width="920px">
      <el-descriptions :column="3" border v-if="currentRow.id">
        <el-descriptions-item label="入库单号">{{ currentRow.orderNo }}</el-descriptions-item>
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
        <el-table-column prop="expectedQty" label="预计数量" width="120" />
        <el-table-column prop="actualQty" label="实收数量" width="120" />
        <el-table-column prop="qualifiedQty" label="合格数量" width="120" />
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
    }
  }
}
</script>
