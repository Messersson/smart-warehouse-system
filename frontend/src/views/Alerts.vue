<template>
  <div class="page-card">
    <div class="page-toolbar">
      <div>
        <h2 style="margin: 0">{{ labels.title }}</h2>
        <div style="color: #64748b; margin-top: 6px">{{ labels.subtitle }}</div>
      </div>
      <div>
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
      <el-table-column prop="firstTriggeredAt" :label="labels.firstTriggeredAt" min-width="180" />
      <el-table-column prop="lastTriggeredAt" :label="labels.lastTriggeredAt" min-width="180" />
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
import { get, post } from '../api'

const labels = {
  title: '预警中心',
  subtitle: '聚焦入库滞留、出库超时、库存呆滞和客户取件超时等风险。',
  refresh: '刷新',
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
  ackFailed: '处理失败'
}

export default {
  name: 'AlertsPage',
  data() {
    return {
      labels,
      rows: []
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
        return `仓库 ${matched[1]} 的入库单 ${matched[2]} 已超过滞留时长，收件人：${matched[3]}，联系电话：${matched[4]}，取件码：${matched[5]}`
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
