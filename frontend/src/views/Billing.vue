<template>
  <div class="page-card">
    <div class="page-toolbar">
      <div>
        <h2 style="margin: 0">计费结算</h2>
        <div style="color: #64748b; margin-top: 6px">维护合同、计费规则和月度结算账单。</div>
      </div>
      <div>
        <el-button type="primary" plain @click="openContractDialog()">新增合同</el-button>
        <el-button type="primary" @click="openGenerateDialog">生成账单</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="合同管理" name="contracts">
        <el-table :data="overview.contracts || []" stripe border>
          <el-table-column prop="contractNo" label="合同编号" min-width="160" />
          <el-table-column prop="contractName" label="合同名称" min-width="180" />
          <el-table-column prop="customerName" label="客户" min-width="140" />
          <el-table-column prop="warehouseName" label="仓库" min-width="140" />
          <el-table-column prop="settlementCycle" label="结算周期" width="100">
            <template slot-scope="scope">{{ cycleText(scope.row.settlementCycle) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template slot-scope="scope">{{ statusText(scope.row.status) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220">
            <template slot-scope="scope">
              <el-button type="text" @click="openContractDialog(scope.row)">编辑</el-button>
              <el-button type="text" @click="openRuleDialog(scope.row)">计费规则</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="账单管理" name="statements">
        <el-table :data="overview.statements || []" stripe border>
          <el-table-column prop="statementNo" label="账单号" min-width="160" />
          <el-table-column prop="customerName" label="客户" min-width="140" />
          <el-table-column prop="statementMonth" label="账期" width="120" />
          <el-table-column prop="totalAmount" label="应收金额" width="140" />
          <el-table-column prop="paidAmount" label="实收金额" width="140" />
          <el-table-column prop="statementStatus" label="账单状态" width="120">
            <template slot-scope="scope">{{ statementStatusText(scope.row.statementStatus) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220">
            <template slot-scope="scope">
              <el-button type="text" @click="viewStatement(scope.row)">明细</el-button>
              <el-button type="text" @click="markPaid(scope.row)" :disabled="scope.row.statementStatus === 'PAID'">标记已支付</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog :title="contractForm.id ? '编辑合同' : '新增合同'" :visible.sync="contractDialogVisible" width="760px">
      <el-form :model="contractForm" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="合同编号"><el-input v-model="contractForm.contractNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="合同名称"><el-input v-model="contractForm.contractName" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="客户">
              <el-select v-model="contractForm.customerId" style="width: 100%">
                <el-option v-for="item in lookups.customers || []" :key="item.id" :label="item.customerName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="货主">
              <el-select v-model="contractForm.ownerId" clearable style="width: 100%">
                <el-option v-for="item in lookups.owners || []" :key="item.id" :label="item.ownerName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="仓库">
              <el-select v-model="contractForm.warehouseId" clearable style="width: 100%">
                <el-option v-for="item in lookups.warehouses || []" :key="item.id" :label="item.warehouseName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="结算周期"><el-select v-model="contractForm.settlementCycle" style="width: 100%"><el-option label="按月" value="MONTHLY" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="生效日期"><el-date-picker v-model="contractForm.effectiveDate" value-format="yyyy-MM-dd" type="date" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="失效日期"><el-date-picker v-model="contractForm.expireDate" value-format="yyyy-MM-dd" type="date" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态"><el-select v-model="contractForm.status" style="width: 100%"><el-option label="启用" value="ACTIVE" /><el-option label="停用" value="INACTIVE" /></el-select></el-form-item></el-col>
        </el-row>
      </el-form>
      <span slot="footer">
        <el-button @click="contractDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveContract">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog :title="`计费规则 - ${selectedContract.contractName || ''}`" :visible.sync="ruleDialogVisible" width="980px">
      <div class="page-toolbar">
        <div />
        <el-button type="primary" plain @click="addRule">新增规则</el-button>
      </div>
      <el-table :data="selectedRules" stripe border>
        <el-table-column prop="ruleName" label="规则名称" min-width="180" />
        <el-table-column prop="chargeType" label="计费类型" min-width="180">
          <template slot-scope="scope">{{ chargeTypeText(scope.row.chargeType) }}</template>
        </el-table-column>
        <el-table-column prop="unitName" label="单位" width="100" />
        <el-table-column prop="unitPrice" label="单价" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">{{ statusText(scope.row.status) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template slot-scope="scope">
            <el-button type="text" @click="editRule(scope.row)">编辑</el-button>
            <el-button type="text" class="text-danger" @click="deleteRule(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-divider />
      <el-form :model="ruleForm" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="规则名称"><el-input v-model="ruleForm.ruleName" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="计费类型">
              <el-select v-model="ruleForm.chargeType" style="width: 100%">
                <el-option label="出库单服务费" value="OUTBOUND_ORDER_COUNT" />
                <el-option label="出库数量服务费" value="OUTBOUND_QTY" />
                <el-option label="入库单服务费" value="INBOUND_ORDER_COUNT" />
                <el-option label="入库数量服务费" value="INBOUND_QTY" />
                <el-option label="库存占用费" value="STORAGE_SNAPSHOT_QTY" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="单位"><el-input v-model="ruleForm.unitName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="单价"><el-input-number v-model="ruleForm.unitPrice" :controls="false" :min="0" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态"><el-select v-model="ruleForm.status" style="width: 100%"><el-option label="启用" value="ACTIVE" /><el-option label="停用" value="INACTIVE" /></el-select></el-form-item></el-col>
        </el-row>
      </el-form>
      <span slot="footer">
        <el-button @click="ruleDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="saveRule">保存规则</el-button>
      </span>
    </el-dialog>

    <el-dialog title="生成账单" :visible.sync="generateDialogVisible" width="620px">
      <el-form :model="generateForm" label-width="120px">
        <el-form-item label="所属合同">
          <el-select v-model="generateForm.contractId" style="width: 100%">
            <el-option v-for="item in overview.contracts || []" :key="item.id" :label="item.contractName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="账期月份">
          <el-date-picker v-model="generateForm.statementMonth" type="month" value-format="yyyy-MM" style="width: 100%" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="generateStatement">生成</el-button>
      </span>
    </el-dialog>

    <el-dialog title="账单明细" :visible.sync="statementVisible" width="920px">
      <el-table :data="currentStatement.items || []" stripe border>
        <el-table-column prop="chargeName" label="费用名称" min-width="180" />
        <el-table-column prop="chargeType" label="计费类型" min-width="180">
          <template slot-scope="scope">{{ chargeTypeText(scope.row.chargeType) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="120" />
        <el-table-column prop="unitPrice" label="单价" width="120" />
        <el-table-column prop="amount" label="金额" width="120" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { get, post, put, remove } from '../api'

export default {
  name: 'BillingPage',
  data() {
    return {
      activeTab: 'contracts',
      overview: {},
      lookups: {},
      contractDialogVisible: false,
      ruleDialogVisible: false,
      generateDialogVisible: false,
      statementVisible: false,
      contractForm: { settlementCycle: 'MONTHLY', status: 'ACTIVE' },
      ruleForm: { status: 'ACTIVE', unitPrice: 0 },
      generateForm: { contractId: null, statementMonth: '' },
      selectedContract: {},
      selectedRules: [],
      currentStatement: {}
    }
  },
  async created() {
    await Promise.all([this.fetchOverview(), this.fetchLookups()])
  },
  methods: {
    statusText(status) {
      const map = { ACTIVE: '启用', INACTIVE: '停用' }
      return map[status] || status
    },
    statementStatusText(status) {
      const map = { DRAFT: '草稿', PAID: '已支付' }
      return map[status] || status
    },
    cycleText(cycle) {
      const map = { MONTHLY: '按月' }
      return map[cycle] || cycle
    },
    chargeTypeText(type) {
      const map = {
        OUTBOUND_ORDER_COUNT: '出库单服务费',
        OUTBOUND_QTY: '出库数量服务费',
        INBOUND_ORDER_COUNT: '入库单服务费',
        INBOUND_QTY: '入库数量服务费',
        STORAGE_SNAPSHOT_QTY: '库存占用费'
      }
      return map[type] || type
    },
    async fetchOverview() {
      const response = await get('/billing/overview')
      this.overview = response.data || {}
    },
    async fetchLookups() {
      const response = await get('/lookups', { _ts: Date.now() })
      this.lookups = response.data || {}
    },
    async openContractDialog(row) {
      await this.fetchLookups()
      this.contractForm = row ? { ...row } : { settlementCycle: 'MONTHLY', status: 'ACTIVE' }
      this.contractDialogVisible = true
    },
    validateContract() {
      if (!this.contractForm.customerId) {
        this.$message.error('请选择客户')
        return false
      }
      if (!this.contractForm.contractNo || !this.contractForm.contractNo.trim()) {
        this.$message.error('请输入合同编号')
        return false
      }
      if (!this.contractForm.contractName || !this.contractForm.contractName.trim()) {
        this.$message.error('请输入合同名称')
        return false
      }
      if (!this.contractForm.effectiveDate) {
        this.$message.error('请选择生效日期')
        return false
      }
      return true
    },
    async saveContract() {
      if (!this.validateContract()) {
        return
      }
      if (this.contractForm.id) {
        await put(`/billing/contracts/${this.contractForm.id}`, this.contractForm)
      } else {
        await post('/billing/contracts', this.contractForm)
      }
      this.$message.success('合同保存成功')
      this.contractDialogVisible = false
      await this.fetchOverview()
    },
    openRuleDialog(contract) {
      this.selectedContract = contract
      this.selectedRules = contract.rules || []
      this.ruleForm = { contractId: contract.id, status: 'ACTIVE', unitPrice: 0 }
      this.ruleDialogVisible = true
    },
    addRule() {
      this.ruleForm = { contractId: this.selectedContract.id, status: 'ACTIVE', unitPrice: 0 }
    },
    editRule(rule) {
      this.ruleForm = { ...rule }
    },
    validateRule() {
      if (!this.selectedContract.id) {
        this.$message.error('请先选择合同')
        return false
      }
      if (!this.ruleForm.ruleName || !this.ruleForm.ruleName.trim()) {
        this.$message.error('请输入规则名称')
        return false
      }
      if (!this.ruleForm.chargeType) {
        this.$message.error('请选择计费类型')
        return false
      }
      if (!this.ruleForm.unitName || !this.ruleForm.unitName.trim()) {
        this.$message.error('请输入单位')
        return false
      }
      return true
    },
    async saveRule() {
      if (!this.validateRule()) {
        return
      }
      if (this.ruleForm.id) {
        await put(`/billing/rules/${this.ruleForm.id}`, this.ruleForm)
      } else {
        await post('/billing/rules', { ...this.ruleForm, contractId: this.selectedContract.id })
      }
      this.$message.success('规则保存成功')
      await this.fetchOverview()
      const contract = (this.overview.contracts || []).find(item => item.id === this.selectedContract.id)
      this.selectedContract = contract || this.selectedContract
      this.selectedRules = (contract && contract.rules) || []
      this.ruleForm = { contractId: this.selectedContract.id, status: 'ACTIVE', unitPrice: 0 }
    },
    async deleteRule(rule) {
      await remove(`/billing/rules/${rule.id}`)
      this.$message.success('规则删除成功')
      await this.fetchOverview()
      const contract = (this.overview.contracts || []).find(item => item.id === this.selectedContract.id)
      this.selectedContract = contract || this.selectedContract
      this.selectedRules = (contract && contract.rules) || []
    },
    openGenerateDialog() {
      this.generateForm = { contractId: null, statementMonth: '' }
      this.generateDialogVisible = true
    },
    async generateStatement() {
      if (!this.generateForm.contractId) {
        this.$message.error('请选择所属合同')
        return
      }
      if (!this.generateForm.statementMonth) {
        this.$message.error('请选择账期月份')
        return
      }
      await post('/billing/statements/generate', this.generateForm)
      this.$message.success('账单生成成功')
      this.generateDialogVisible = false
      await this.fetchOverview()
      this.activeTab = 'statements'
    },
    viewStatement(row) {
      this.currentStatement = row
      this.statementVisible = true
    },
    async markPaid(row) {
      await post(`/billing/statements/${row.id}/pay`)
      this.$message.success('账单已标记为已支付')
      await this.fetchOverview()
    }
  }
}
</script>
