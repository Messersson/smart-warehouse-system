<template>
  <div class="pda-page">
    <div class="pda-shell">
      <div class="pda-header">
        <div>
          <h2>PDA 扫码台</h2>
          <p>支持扫码查询、扫码取件、扫码出库，适合手持设备或手机浏览器快速操作。</p>
        </div>
      </div>

      <div class="page-card pda-card">
        <div class="scan-bar">
          <el-input
            v-model="code"
            size="medium"
            placeholder="扫描二维码/条码后可直接回车"
            @keyup.enter.native="lookup"
          />
          <el-button type="primary" :loading="lookupLoading" @click="lookup">扫码查询</el-button>
          <el-button @click="clearResult">清空</el-button>
        </div>

        <div v-if="result" class="result-card">
          <div class="result-top">
            <div>
              <div class="entity-type">{{ entityTypeText(result.entityType) }}</div>
              <div class="entity-code">{{ primaryCode }}</div>
            </div>
            <el-tag :type="statusTagType">{{ statusText }}</el-tag>
          </div>

          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="仓库">{{ result.warehouseName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="主状态">{{ result.status || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="result.receiverName" label="收件人">{{ result.receiverName }}</el-descriptions-item>
            <el-descriptions-item v-if="result.receiverPhone" label="联系电话">{{ result.receiverPhone }}</el-descriptions-item>
            <el-descriptions-item v-if="result.pickupCode" label="取件码">{{ result.pickupCode }}</el-descriptions-item>
            <el-descriptions-item v-if="result.scanCode" label="扫码码">{{ result.scanCode }}</el-descriptions-item>
            <el-descriptions-item v-if="result.pickupDueAt" label="取件截止">{{ result.pickupDueAt }}</el-descriptions-item>
            <el-descriptions-item v-if="result.logisticsNo" label="物流单号">{{ result.logisticsNo }}</el-descriptions-item>
            <el-descriptions-item v-if="result.locationCode" label="库位编码">{{ result.locationCode }}</el-descriptions-item>
            <el-descriptions-item v-if="result.locationName" label="库位名称">{{ result.locationName }}</el-descriptions-item>
          </el-descriptions>

          <div class="action-panel" v-if="result.entityType === 'INBOUND_ORDER'">
            <div class="action-title">取件动作</div>
            <div class="action-row">
              <el-input-number v-model="delayMinutes" :min="15" :step="15" :controls="false" />
              <el-button
                type="primary"
                :loading="actionLoading === 'pickup'"
                :disabled="result.pickupStatus === 'PICKED_UP' || result.status !== 'PUTAWAY_COMPLETED'"
                @click="submitPickupAction('PICKED_UP')"
              >
                取件完成
              </el-button>
              <el-button
                :loading="actionLoading === 'delay'"
                :disabled="result.status !== 'PUTAWAY_COMPLETED'"
                @click="submitPickupAction('DELAY')"
              >
                延迟取货
              </el-button>
              <el-button
                type="danger"
                plain
                :loading="actionLoading === 'refuse'"
                :disabled="result.pickupStatus === 'PICKED_UP'"
                @click="submitPickupAction('REFUSE')"
              >
                拒收登记
              </el-button>
            </div>
          </div>

          <div class="action-panel" v-if="result.entityType === 'OUTBOUND_ORDER'">
            <div class="action-title">出库动作</div>
            <div class="action-row">
              <el-button
                type="primary"
                :loading="actionLoading === 'ship'"
                :disabled="result.status === 'SHIPPED'"
                @click="shipOutbound"
              >
                扫码出库
              </el-button>
            </div>
          </div>
        </div>

        <div v-else class="pda-empty">
          扫描后会在这里显示订单、取件码、库位或出库信息，并提供直接操作按钮。
        </div>
      </div>

      <div class="page-card pda-history-card">
        <div class="history-head">
          <div class="section-title">最近扫码</div>
          <el-button type="text" @click="history = []">清空历史</el-button>
        </div>
        <div class="history-list" v-if="history.length">
          <button
            v-for="item in history"
            :key="item.code"
            class="history-chip"
            @click="reuseCode(item.code)"
          >
            <span>{{ item.code }}</span>
            <small>{{ entityTypeText(item.entityType) }}</small>
          </button>
        </div>
        <div v-else class="pda-empty">还没有扫码历史。</div>
      </div>
    </div>
  </div>
</template>

<script>
import { get, post } from '../api'

export default {
  name: 'PdaPage',
  data() {
    return {
      code: '',
      result: null,
      history: [],
      delayMinutes: 30,
      lookupLoading: false,
      actionLoading: ''
    }
  },
  computed: {
    primaryCode() {
      if (!this.result) {
        return '-'
      }
      return this.result.pickupCode || this.result.scanCode || this.result.orderNo || this.result.locationCode || '-'
    },
    statusText() {
      if (!this.result) {
        return '-'
      }
      if (this.result.entityType === 'INBOUND_ORDER') {
        return this.result.pickupStatus || this.result.status || '-'
      }
      return this.result.status || '-'
    },
    statusTagType() {
      if (!this.result) {
        return 'info'
      }
      if (this.statusText === 'PICKED_UP' || this.statusText === 'SHIPPED') {
        return 'success'
      }
      if (this.statusText === 'REFUSED') {
        return 'danger'
      }
      if (this.statusText === 'NOTICE_SENT' || this.statusText === 'PENDING_PICKUP' || this.statusText === 'PICKING_COMPLETED') {
        return 'warning'
      }
      return 'info'
    }
  },
  async created() {
    const code = this.$route.query.code
    if (code) {
      this.code = code
      await this.lookup()
    }
  },
  methods: {
    entityTypeText(type) {
      const map = {
        INBOUND_ORDER: '入库单',
        OUTBOUND_ORDER: '出库单',
        LOCATION: '库位'
      }
      return map[type] || type || '-'
    },
    async lookup() {
      if (!this.code.trim()) {
        this.$message.error('请先输入或扫描编码')
        return
      }
      try {
        this.lookupLoading = true
        const response = await get('/scan/lookup', { code: this.code.trim() })
        this.result = response.data || null
        this.pushHistory(this.code.trim(), this.result)
      } catch (error) {
        this.$message.error(error.message || '扫码查询失败')
      } finally {
        this.lookupLoading = false
      }
    },
    async submitPickupAction(action) {
      if (!this.result) {
        return
      }
      const actionKey = action === 'PICKED_UP' ? 'pickup' : action === 'DELAY' ? 'delay' : 'refuse'
      try {
        this.actionLoading = actionKey
        await post(`/inbounds/${this.result.id}/pickup-action`, {
          action,
          operatorName: 'PDA Device',
          extendMinutes: action === 'DELAY' ? this.delayMinutes : null,
          remark: action === 'REFUSE' ? 'PDA 扫码拒收登记' : 'PDA 扫码操作'
        })
        this.$message.success(action === 'PICKED_UP' ? '取件完成' : action === 'DELAY' ? '已登记延迟取货' : '已登记拒收')
        await this.lookup()
      } catch (error) {
        this.$message.error(error.message || '取件动作失败')
      } finally {
        this.actionLoading = ''
      }
    },
    async shipOutbound() {
      if (!this.result) {
        return
      }
      try {
        this.actionLoading = 'ship'
        await post(`/outbounds/${this.result.id}/ship`)
        this.$message.success('扫码出库完成')
        await this.lookup()
      } catch (error) {
        this.$message.error(error.message || '扫码出库失败')
      } finally {
        this.actionLoading = ''
      }
    },
    clearResult() {
      this.code = ''
      this.result = null
    },
    reuseCode(code) {
      this.code = code
      this.lookup()
    },
    pushHistory(code, result) {
      const next = [{ code, entityType: result.entityType }].concat(this.history.filter(item => item.code !== code))
      this.history = next.slice(0, 8)
    }
  }
}
</script>

<style scoped>
.pda-page {
  display: flex;
  justify-content: center;
}

.pda-shell {
  width: min(920px, 100%);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pda-header h2 {
  margin: 0;
  font-size: 20px;
  line-height: 28px;
}

.pda-header p {
  display: none;
}

.scan-bar {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 8px;
}

.result-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 14px;
  border: 1px solid #d8dee6;
  border-radius: 8px;
  background: #f8fafc;
}

.entity-type {
  color: #64748b;
  font-size: 13px;
}

.entity-code {
  font-size: 26px;
  font-weight: 700;
  margin-top: 6px;
  word-break: break-all;
}

.action-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  border: 1px solid #d8dee6;
  border-radius: 8px;
  background: #f8fafc;
}

.action-title,
.section-title {
  font-size: 15px;
  font-weight: 700;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.history-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.history-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.history-chip {
  border: 1px solid #d8dee6;
  border-radius: 6px;
  background: #ffffff;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  cursor: pointer;
}

.history-chip span {
  font-weight: 600;
}

.history-chip small {
  color: #64748b;
  margin-top: 4px;
}

.pda-empty {
  color: #64748b;
  line-height: 1.7;
}

@media (max-width: 760px) {
  .scan-bar {
    grid-template-columns: 1fr;
  }

  .result-top {
    flex-direction: column;
    align-items: flex-start;
  }

  .action-row {
    flex-direction: column;
    align-items: stretch;
  }

  .action-row .el-button,
  .action-row .el-input-number {
    width: 100%;
  }
}
</style>
