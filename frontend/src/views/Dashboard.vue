<template>
  <div>
    <div class="grid-cards">
      <div class="metric-card" style="background: linear-gradient(135deg, #0284c7, #38bdf8)">
        <div class="metric-title">在管仓库</div>
        <div class="metric-value">{{ overview.warehouseCount || 0 }}</div>
        <div class="metric-subtitle">仓网结构与库内作业总览</div>
      </div>
      <div class="metric-card" style="background: linear-gradient(135deg, #059669, #34d399)">
        <div class="metric-title">商品档案</div>
        <div class="metric-value">{{ overview.productCount || 0 }}</div>
        <div class="metric-subtitle">SKU 主数据规模</div>
      </div>
      <div class="metric-card" style="background: linear-gradient(135deg, #ea580c, #fb923c)">
        <div class="metric-title">待处理入库</div>
        <div class="metric-value">{{ overview.pendingInboundCount || 0 }}</div>
        <div class="metric-subtitle">包含未收货与待上架任务</div>
      </div>
      <div class="metric-card" style="background: linear-gradient(135deg, #7c3aed, #a78bfa)">
        <div class="metric-title">待处理出库</div>
        <div class="metric-value">{{ overview.pendingOutboundCount || 0 }}</div>
        <div class="metric-subtitle">包含未拣货与未发运任务</div>
      </div>
      <div class="metric-card" style="background: linear-gradient(135deg, #dc2626, #f87171)">
        <div class="metric-title">未关闭预警</div>
        <div class="metric-value">{{ overview.unresolvedAlertCount || 0 }}</div>
        <div class="metric-subtitle">重点关注超时与滞留风险</div>
      </div>
      <div class="metric-card" style="background: linear-gradient(135deg, #0f766e, #2dd4bf)">
        <div class="metric-title">库存总量</div>
        <div class="metric-value">{{ overview.totalStockQuantity || 0 }}</div>
        <div class="metric-subtitle">当前账面库存数量</div>
      </div>
    </div>

    <div class="two-column">
      <div class="page-card">
        <div class="page-toolbar">
          <h3 style="margin: 0">最近入库单</h3>
          <el-button type="text" @click="$router.push('/inbounds')">查看全部</el-button>
        </div>
        <el-table :data="overview.recentInbounds || []" stripe>
          <el-table-column prop="orderNo" label="入库单号" />
          <el-table-column prop="status" label="状态" />
          <el-table-column prop="totalExpectedQty" label="预计数量" />
          <el-table-column prop="receivedAt" label="收货时间" min-width="160" />
        </el-table>
      </div>

      <div class="page-card">
        <div class="page-toolbar">
          <h3 style="margin: 0">最近出库单</h3>
          <el-button type="text" @click="$router.push('/outbounds')">查看全部</el-button>
        </div>
        <el-table :data="overview.recentOutbounds || []" stripe>
          <el-table-column prop="orderNo" label="出库单号" />
          <el-table-column prop="status" label="状态" />
          <el-table-column prop="totalPlannedQty" label="计划数量" />
          <el-table-column prop="plannedShipTime" label="计划发运" min-width="160" />
        </el-table>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="page-toolbar">
        <h3 style="margin: 0">热点预警</h3>
        <el-button type="text" @click="$router.push('/alerts')">进入预警中心</el-button>
      </div>
      <el-table :data="overview.hotAlerts || []" stripe>
        <el-table-column prop="ruleName" label="规则" min-width="160" />
        <el-table-column prop="severity" label="等级" width="100" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="alertMessage" label="内容" min-width="280" />
        <el-table-column prop="lastTriggeredAt" label="最近触发时间" min-width="180" />
      </el-table>
    </div>
  </div>
</template>

<script>
import { get } from '../api'

export default {
  name: 'DashboardPage',
  data() {
    return {
      overview: {}
    }
  },
  async created() {
    await this.fetchOverview()
  },
  methods: {
    async fetchOverview() {
      try {
        const response = await get('/dashboard/overview')
        this.overview = response.data || {}
      } catch (error) {
        this.$message.error(error.message || '加载驾驶舱失败')
      }
    }
  }
}
</script>
