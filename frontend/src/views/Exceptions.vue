<template>
  <div class="page-card">
    <div class="page-toolbar">
      <div>
        <h2 style="margin: 0">异常工单</h2>
        <div style="color: #64748b; margin-top: 6px">跟踪异常订单、盘点差异和现场运营问题。</div>
      </div>
      <el-button type="primary" @click="openCreate">新建工单</el-button>
    </div>

    <el-table :data="rows" stripe border>
      <el-table-column prop="ticketNo" label="工单号" min-width="160" />
      <el-table-column prop="warehouseName" label="仓库" min-width="140" />
      <el-table-column prop="bizType" label="业务类型" min-width="140">
        <template slot-scope="scope">{{ bizTypeText(scope.row.bizType) }}</template>
      </el-table-column>
      <el-table-column prop="ticketTitle" label="标题" min-width="220" />
      <el-table-column prop="severity" label="严重级别" width="100">
        <template slot-scope="scope">{{ severityText(scope.row.severity) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="工单状态" width="120">
        <template slot-scope="scope">{{ statusText(scope.row.status) }}</template>
      </el-table-column>
      <el-table-column prop="approvalStatus" label="审批状态" width="120">
        <template slot-scope="scope">{{ approvalStatusText(scope.row.approvalStatus) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="320">
        <template slot-scope="scope">
          <el-button type="text" @click="assign(scope.row)">指派</el-button>
          <el-button type="text" @click="resolve(scope.row)" :disabled="scope.row.status === 'RESOLVED' || scope.row.status === 'CLOSED'">解决</el-button>
          <el-button type="text" @click="close(scope.row)" :disabled="scope.row.status === 'CLOSED'">关闭</el-button>
          <el-button type="text" @click="submitApproval(scope.row)" :disabled="scope.row.approvalStatus === 'PENDING'">提交审批</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="新建异常工单" :visible.sync="dialogVisible" width="760px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="所属仓库">
          <el-select v-model="form.warehouseId" clearable style="width: 100%">
            <el-option v-for="item in lookups.warehouses || []" :key="item.id" :label="item.warehouseName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务类型"><el-input v-model="form.bizType" /></el-form-item>
        <el-form-item label="业务单号"><el-input v-model="form.bizNo" /></el-form-item>
        <el-form-item label="工单标题"><el-input v-model="form.ticketTitle" /></el-form-item>
        <el-form-item label="工单内容"><el-input v-model="form.ticketContent" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="严重级别">
          <el-select v-model="form.severity" style="width: 100%">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理人"><el-input v-model="form.assigneeName" /></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="create">创建</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { get, post } from '../api'

export default {
  name: 'ExceptionsPage',
  data() {
    return {
      rows: [],
      lookups: {},
      dialogVisible: false,
      form: {
        warehouseId: null,
        bizType: 'MANUAL',
        bizNo: '',
        ticketTitle: '',
        ticketContent: '',
        severity: 'MEDIUM',
        assigneeName: '',
        reporterName: '系统管理员'
      }
    }
  },
  async created() {
    await Promise.all([this.fetchRows(), this.fetchLookups()])
  },
  methods: {
    severityText(severity) {
      const map = { LOW: '低', MEDIUM: '中', HIGH: '高' }
      return map[severity] || severity
    },
    bizTypeText(type) {
      const map = {
        MANUAL: '手工工单',
        INBOUND_ORDER: '入库单',
        OUTBOUND_ORDER: '出库单',
        STOCK_TAKE_ORDER: '盘点单',
        EXCEPTION_TICKET: '异常工单'
      }
      return map[type] || type
    },
    statusText(status) {
      const map = { OPEN: '待处理', IN_PROGRESS: '处理中', RESOLVED: '已解决', CLOSED: '已关闭' }
      return map[status] || status
    },
    approvalStatusText(status) {
      const map = { NOT_SUBMITTED: '未提交', PENDING: '审批中', APPROVED: '已通过', REJECTED: '已驳回' }
      return map[status] || status
    },
    async fetchRows() {
      const response = await get('/exceptions')
      this.rows = response.data || []
    },
    async fetchLookups() {
      const response = await get('/lookups', { _ts: Date.now() })
      this.lookups = response.data || {}
    },
    async openCreate() {
      await this.fetchLookups()
      this.form = { warehouseId: null, bizType: 'MANUAL', bizNo: '', ticketTitle: '', ticketContent: '', severity: 'MEDIUM', assigneeName: '', reporterName: '系统管理员' }
      this.dialogVisible = true
    },
    async create() {
      await post('/exceptions', this.form)
      this.$message.success('异常工单创建成功')
      this.dialogVisible = false
      await this.fetchRows()
    },
    async assign(row) {
      await post(`/exceptions/${row.id}/assign`, null, { params: { assigneeName: '仓库主管' } })
      this.$message.success('工单已指派')
      await this.fetchRows()
    },
    async resolve(row) {
      await post(`/exceptions/${row.id}/resolve`, null, { params: { operatorName: '仓库主管' } })
      this.$message.success('工单已解决')
      await this.fetchRows()
    },
    async close(row) {
      await post(`/exceptions/${row.id}/close`)
      this.$message.success('工单已关闭')
      await this.fetchRows()
    },
    async submitApproval(row) {
      await post('/approvals/submit', {
        approvalType: 'EXCEPTION_APPROVAL',
        bizType: 'EXCEPTION_TICKET',
        bizId: row.id,
        bizNo: row.ticketNo,
        applicantName: '系统管理员',
        approverName: '仓库主管',
        applyReason: row.ticketTitle
      })
      this.$message.success('审批已提交')
      await this.fetchRows()
    }
  }
}
</script>
