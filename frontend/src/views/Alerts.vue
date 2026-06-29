<template>
  <div class="page-card">
    <div class="page-toolbar">
      <div>
        <h2 style="margin: 0">{{ labels.title }}</h2>
        <div style="color: #64748b; margin-top: 6px">{{ labels.subtitle }}</div>
      </div>
      <div>
        <el-button :loading="scanLoading" @click="scanNow">{{ labels.scanNow }}</el-button>
        <el-dropdown trigger="click" @command="deleteHistory">
          <el-button type="danger" plain :loading="deleteLoading">
            {{ labels.deleteHistory }}<i class="el-icon-arrow-down el-icon--right"></i>
          </el-button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="ONE_WEEK">{{ labels.deleteOneWeek }}</el-dropdown-item>
            <el-dropdown-item command="ONE_MONTH">{{ labels.deleteOneMonth }}</el-dropdown-item>
            <el-dropdown-item command="THREE_MONTHS">{{ labels.deleteThreeMonths }}</el-dropdown-item>
            <el-dropdown-item command="ONE_YEAR">{{ labels.deleteOneYear }}</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
        <el-button @click="fetchRows">{{ labels.refresh }}</el-button>
      </div>
    </div>

    <el-table :data="rows" stripe border>
      <el-table-column :label="labels.ruleName" min-width="160">
        <template slot-scope="scope">
          {{ ruleNameText(scope.row.ruleName, scope.row.ruleCode) }}
        </template>
      </el-table-column>
      <el-table-column :label="labels.severity" width="100">
        <template slot-scope="scope">
          <el-tag :type="severityType(scope.row.severity)">{{ severityText(scope.row.severity) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="labels.status" width="120">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 'OPEN' ? 'danger' : 'success'">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="labels.bizType" width="140">
        <template slot-scope="scope">
          {{ bizTypeText(scope.row.bizType) }}
        </template>
      </el-table-column>
      <el-table-column :label="labels.alertMessage" min-width="320">
        <template slot-scope="scope">
          {{ alertMessageText(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column :label="labels.firstTriggeredAt" min-width="180">
        <template slot-scope="scope">
          {{ formatDateTime(scope.row.firstTriggeredAt) }}
        </template>
      </el-table-column>
      <el-table-column :label="labels.lastTriggeredAt" min-width="180">
        <template slot-scope="scope">
          {{ formatDateTime(scope.row.lastTriggeredAt) }}
        </template>
      </el-table-column>
      <el-table-column :label="labels.actions" width="140" fixed="right">
        <template slot-scope="scope">
          <el-button
            type="text"
            :disabled="scope.row.status !== 'OPEN'"
            @click="acknowledge(scope.row)"
          >
            {{ labels.acknowledge }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import { get, post, remove } from '../api'

const labels = {
  title: '预警中心',
  subtitle: '聚焦入库滞留、出库超时、库存呆滞和客户取件超时等风险。',
  refresh: '刷新',
  scanNow: '立即扫描',
  deleteHistory: '删除记录',
  deleteOneWeek: '删除超过一周',
  deleteOneMonth: '删除超过一个月',
  deleteThreeMonths: '删除超过三个月',
  deleteOneYear: '删除超过一年',
  ruleName: '预警规则',
  severity: '等级',
  status: '状态',
  bizType: '业务类型',
  alertMessage: '预警内容',
  firstTriggeredAt: '首次触发',
  lastTriggeredAt: '最近触发',
  actions: '操作',
  acknowledge: '确认处理',
  loadFailed: '预警加载失败',
  ackSuccess: '已确认处理',
  ackFailed: '处理失败',
  scanSuccess: '预警扫描完成',
  scanFailed: '预警扫描失败',
  deleteSuccess: '历史记录已删除',
  deleteFailed: '删除历史记录失败'
}

export default {
  name: 'AlertsPage',
  data() {
    return {
      labels,
      rows: [],
      scanLoading: false,
      deleteLoading: false
    }
  },
  async created() {
    await this.fetchRows()
  },
  methods: {
    async fetchRows() {
      try {
        const response = await get('/alerts')
        this.rows = response.data || []
      } catch (error) {
        this.$message.error(error.message || labels.loadFailed)
      }
    },
    async acknowledge(row) {
      try {
        await post(`/alerts/${row.id}/ack`, null, {
          params: { operatorName: '系统管理员' }
        })
        this.$message.success(labels.ackSuccess)
        await this.fetchRows()
      } catch (error) {
        this.$message.error(error.message || labels.ackFailed)
      }
    },
    async scanNow() {
      try {
        this.scanLoading = true
        const response = await post('/alerts/scan')
        const data = response.data || {}
        this.$message.success(`${labels.scanSuccess}，当前待处理 ${data.openCount || 0} 条`)
        await this.fetchRows()
      } catch (error) {
        this.$message.error(error.message || labels.scanFailed)
      } finally {
        this.scanLoading = false
      }
    },
    async deleteHistory(period) {
      const text = this.deletePeriodText(period)
      try {
        await this.$confirm(`确定删除${text}的预警记录吗？删除后不可恢复。`, '删除确认', {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch (error) {
        return
      }

      try {
        this.deleteLoading = true
        const response = await remove('/alerts/history', { period })
        const data = response.data || {}
        this.$message.success(`${labels.deleteSuccess}，共删除 ${data.deletedCount || 0} 条`)
        await this.fetchRows()
      } catch (error) {
        this.$message.error(error.message || labels.deleteFailed)
      } finally {
        this.deleteLoading = false
      }
    },
    deletePeriodText(period) {
      const map = {
        ONE_WEEK: '超过一周',
        ONE_MONTH: '超过一个月',
        THREE_MONTHS: '超过三个月',
        ONE_YEAR: '超过一年'
      }
      return map[period] || '指定周期'
    },
    formatDateTime(value) {
      if (!value) {
        return '-'
      }
      const text = String(value)
      if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(text)) {
        return text
      }
      const date = new Date(text)
      if (Number.isNaN(date.getTime())) {
        return text
      }
      const pad = item => String(item).padStart(2, '0')
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    },
    bizTypeText(bizType) {
      const map = {
        INBOUND_ORDER: '入库单',
        OUTBOUND_ORDER: '出库单',
        INVENTORY_STOCK: '库存',
        STOCK_TAKE_ORDER: '盘点单'
      }
      return map[bizType] || bizType || '-'
    },
    ruleNameText(ruleName, ruleCode) {
      const codeMap = {
        INBOUND_RECEIVE_TIMEOUT: '收货超时预警',
        INBOUND_PUTAWAY_TIMEOUT: '上架滞留预警',
        OUTBOUND_SHIP_TIMEOUT: '出库超时预警',
        STOCK_STAGNANT: '呆滞库存预警',
        WAREHOUSE_DWELL_TIMEOUT: '客户取件滞留预警'
      }
      return codeMap[ruleCode] || ruleName || '-'
    },
    alertMessageText(row) {
      const message = row.alertMessage || ''
      if (!message) {
        return '-'
      }

      let matched = message.match(/^Inbound order (.+) has exceeded receive timeout by (\d+) minutes$/)
      if (matched) {
        return `入库单 ${matched[1]} 已超过 ${matched[2]} 分钟仍未收货`
      }

      matched = message.match(/^Inbound order (.+) has not been put away within (\d+) minutes$/)
      if (matched) {
        return `入库单 ${matched[1]} 收货后超过 ${matched[2]} 分钟仍未上架`
      }

      matched = message.match(/^Outbound order (.+) has exceeded the planned ship time by (\d+) minutes$/)
      if (matched) {
        return `出库单 ${matched[1]} 已超过计划发运时限 ${matched[2]} 分钟`
      }

      matched = message.match(/^Stock (.+) has not moved for more than (\d+) days$/)
      if (matched) {
        return `库存 ${matched[1]} 已超过 ${matched[2]} 天无库存变动`
      }

      matched = message.match(/^Warehouse (.+) order (.+) has exceeded dwell time\. Receiver=(.+), phone=(.+), pickupCode=(.+)$/)
      if (matched) {
        return `仓库 ${matched[1]} 的入库单 ${matched[2]} 已超过滞留时长，收件人：${matched[3]}，联系电话：${matched[4]}，作业编码：${matched[5]}`
      }

      return message
    },
    severityText(severity) {
      const map = {
        HIGH: '高',
        MEDIUM: '中',
        LOW: '低'
      }
      return map[severity] || severity || '-'
    },
    statusText(status) {
      const map = {
        OPEN: '待处理',
        ACKNOWLEDGED: '已确认',
        RESOLVED: '已恢复'
      }
      return map[status] || status || '-'
    },
    severityType(severity) {
      if (severity === 'HIGH') return 'danger'
      if (severity === 'MEDIUM') return 'warning'
      return 'info'
    }
  }
}
</script>
