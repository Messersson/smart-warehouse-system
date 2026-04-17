<template>
  <div class="page-card">
    <div class="page-toolbar">
      <div>
        <h2 style="margin: 0">审批中心</h2>
        <div style="color: #64748b; margin-top: 6px">统一处理订单审批与异常审批任务。</div>
      </div>
      <el-button @click="fetchRows">刷新</el-button>
    </div>

    <el-table :data="rows" stripe border>
      <el-table-column prop="approvalNo" label="审批单号" min-width="160" />
      <el-table-column prop="approvalType" label="审批类型" min-width="140">
        <template slot-scope="scope">{{ approvalTypeText(scope.row.approvalType) }}</template>
      </el-table-column>
      <el-table-column prop="bizType" label="业务类型" min-width="140">
        <template slot-scope="scope">{{ bizTypeText(scope.row.bizType) }}</template>
      </el-table-column>
      <el-table-column prop="bizNo" label="业务单号" min-width="160" />
      <el-table-column prop="applicantName" label="申请人" min-width="120" />
      <el-table-column prop="approverName" label="审批人" min-width="120" />
      <el-table-column prop="status" label="审批状态" width="120">
        <template slot-scope="scope">{{ statusText(scope.row.status) }}</template>
      </el-table-column>
      <el-table-column prop="appliedAt" label="申请时间" min-width="180" />
      <el-table-column label="操作" width="240">
        <template slot-scope="scope">
          <el-button type="text" @click="openDetail(scope.row)">详情</el-button>
          <el-button type="text" @click="approve(scope.row)" :disabled="scope.row.status !== 'PENDING'">通过</el-button>
          <el-button type="text" class="text-danger" @click="reject(scope.row)" :disabled="scope.row.status !== 'PENDING'">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="审批详情" :visible.sync="detailVisible" width="900px">
      <el-descriptions :column="2" border v-if="currentRow.id">
        <el-descriptions-item label="审批单号">{{ currentRow.approvalNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(currentRow.status) }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ bizTypeText(currentRow.bizType) }}</el-descriptions-item>
        <el-descriptions-item label="业务单号">{{ currentRow.bizNo }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ currentRow.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ currentRow.approverName }}</el-descriptions-item>
        <el-descriptions-item label="申请原因" :span="2">{{ currentRow.applyReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批意见" :span="2">{{ currentRow.approvalComment || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="currentRow.records || []" stripe border style="margin-top: 16px">
        <el-table-column prop="actionType" label="动作" width="140">
          <template slot-scope="scope">{{ actionText(scope.row.actionType) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="140" />
        <el-table-column prop="actionComment" label="说明" min-width="220" />
        <el-table-column prop="createdAt" label="操作时间" min-width="180" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { get, post } from '../api'

export default {
  name: 'ApprovalsPage',
  data() {
    return {
      rows: [],
      currentRow: {},
      detailVisible: false
    }
  },
  async created() {
    await this.fetchRows()
  },
  methods: {
    statusText(status) {
      const map = { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回' }
      return map[status] || status
    },
    approvalTypeText(type) {
      const map = { ORDER_APPROVAL: '订单审批', EXCEPTION_APPROVAL: '异常审批', BUSINESS_APPROVAL: '业务审批' }
      return map[type] || type
    },
    bizTypeText(type) {
      const map = {
        INBOUND_ORDER: '入库单',
        OUTBOUND_ORDER: '出库单',
        EXCEPTION_TICKET: '异常工单',
        STOCK_TAKE_ORDER: '盘点单'
      }
      return map[type] || type
    },
    actionText(actionType) {
      const map = { SUBMIT: '提交', APPROVE: '通过', REJECT: '驳回' }
      return map[actionType] || actionType
    },
    async fetchRows() {
      const response = await get('/approvals')
      this.rows = response.data || []
    },
    openDetail(row) {
      this.currentRow = row
      this.detailVisible = true
    },
    async approve(row) {
      await post(`/approvals/${row.id}/approve`, { operatorName: '仓库主管', comment: '审批通过' })
      this.$message.success('审批已通过')
      await this.fetchRows()
    },
    async reject(row) {
      await post(`/approvals/${row.id}/reject`, { operatorName: '仓库主管', comment: '审批驳回' })
      this.$message.success('审批已驳回')
      await this.fetchRows()
    }
  }
}
</script>
