<template>
  <div v-loading="loading" class="dashboard-page">
    <div class="overview-bar">
      <div>
        <div class="overview-title">运营总览</div>
        <div class="overview-meta">实时汇总仓库、订单、库存与预警状态</div>
      </div>
      <div class="overview-actions">
        <span class="health-dot"></span>
        <span class="health-text">运行中</span>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="fetchOverview">刷新</el-button>
      </div>
    </div>

    <div class="grid-cards">
      <div class="metric-card">
        <div class="metric-title">在管仓库</div>
        <div class="metric-value">{{ overview.warehouseCount || 0 }}</div>
        <div class="metric-subtitle">仓网结构与库内作业总览</div>
      </div>
      <div class="metric-card tone-teal">
        <div class="metric-title">商品档案</div>
        <div class="metric-value">{{ overview.productCount || 0 }}</div>
        <div class="metric-subtitle">SKU 主数据规模</div>
      </div>
      <div class="metric-card tone-amber">
        <div class="metric-title">待处理入库</div>
        <div class="metric-value">{{ overview.pendingInboundCount || 0 }}</div>
        <div class="metric-subtitle">未收货与待上架任务</div>
      </div>
      <div class="metric-card tone-indigo">
        <div class="metric-title">待处理出库</div>
        <div class="metric-value">{{ overview.pendingOutboundCount || 0 }}</div>
        <div class="metric-subtitle">未拣货与未发运任务</div>
      </div>
      <div class="metric-card tone-red">
        <div class="metric-title">未关闭预警</div>
        <div class="metric-value">{{ overview.unresolvedAlertCount || 0 }}</div>
        <div class="metric-subtitle">超时与滞留风险</div>
      </div>
      <div class="metric-card tone-cyan">
        <div class="metric-title">库存总量</div>
        <div class="metric-value">{{ overview.totalStockQuantity || 0 }}</div>
        <div class="metric-subtitle">当前账面库存数量</div>
      </div>
    </div>

    <div class="two-column">
      <div class="page-card">
        <div class="page-toolbar compact-toolbar">
          <h3>最近入库单</h3>
          <el-button type="text" icon="el-icon-right" @click="$router.push('/inbounds')">查看全部</el-button>
        </div>
        <el-table :data="overview.recentInbounds || []" :empty-text="labels.noData" stripe>
          <el-table-column prop="orderNo" label="入库单号" min-width="150" />
          <el-table-column prop="status" label="状态" width="120" />
          <el-table-column prop="totalExpectedQty" label="预计数量" width="110" />
          <el-table-column prop="receivedAt" label="收货时间" min-width="160" />
        </el-table>
      </div>

      <div class="page-card">
        <div class="page-toolbar compact-toolbar">
          <h3>最近出库单</h3>
          <el-button type="text" icon="el-icon-right" @click="$router.push('/outbounds')">查看全部</el-button>
        </div>
        <el-table :data="overview.recentOutbounds || []" :empty-text="labels.noData" stripe>
          <el-table-column prop="orderNo" label="出库单号" min-width="150" />
          <el-table-column prop="status" label="状态" width="120" />
          <el-table-column prop="totalPlannedQty" label="计划数量" width="110" />
          <el-table-column prop="plannedShipTime" label="计划发运" min-width="160" />
        </el-table>
      </div>
    </div>

    <div class="page-card alert-section">
      <div class="page-toolbar compact-toolbar">
        <h3>热点预警</h3>
        <el-button type="text" icon="el-icon-right" @click="$router.push('/alerts')">进入预警中心</el-button>
      </div>
      <el-table :data="overview.hotAlerts || []" :empty-text="labels.noData" stripe>
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
      loading: false,
      overview: {},
      labels: {
        noData: '\u6682\u65e0\u6570\u636e',
        loadFailed: '\u52a0\u8f7d\u9a7e\u9a76\u8231\u5931\u8d25'
      }
    }
  },
  async created() {
    await this.fetchOverview()
  },
  methods: {
    async fetchOverview() {
      this.loading = true
      try {
        const response = await get('/dashboard/overview')
        this.overview = response.data || {}
      } catch (error) {
        this.$message.error(error.message || this.labels.loadFailed)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.dashboard-page {
  min-height: 100%;
}

.overview-bar {
  margin-bottom: 14px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.045);
}

.overview-title {
  color: #101828;
  font-size: 18px;
  line-height: 24px;
  font-weight: 700;
}

.overview-meta {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
  line-height: 18px;
}

.overview-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.health-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #12b76a;
}

.health-text {
  margin-right: 8px;
  color: #047857;
  font-size: 13px;
  font-weight: 650;
}

.compact-toolbar {
  margin-bottom: 10px;
}

.compact-toolbar h3 {
  margin: 0;
  font-size: 15px;
  line-height: 22px;
}

.alert-section {
  margin-top: 12px;
}

@media (max-width: 760px) {
  .overview-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .overview-actions {
    justify-content: space-between;
  }
}
</style>
