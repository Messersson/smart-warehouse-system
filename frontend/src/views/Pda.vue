<template>
  <div class="pda-page">
    <div class="pda-shell">
      <div class="pda-header">
        <div>
          <h2>PDA 扫码台</h2>
          <p>支持扫码查询、贴码入库确认、扫码取件、扫码出库，适合手持设备或手机浏览器快速操作。</p>
        </div>
      </div>

      <div class="page-card pda-card">
        <div class="scan-bar">
          <el-select v-model="scanFormat" size="medium" class="format-select">
            <el-option label="自动识别" value="AUTO" />
            <el-option label="二维码" value="QR_CODE" />
            <el-option label="条形码" value="BAR_CODE" />
            <el-option label="Data Matrix" value="DATA_MATRIX" />
            <el-option label="PDF417" value="PDF_417" />
            <el-option label="Aztec" value="AZTEC" />
            <el-option label="其他" value="OTHER" />
          </el-select>
          <el-select v-model="scannerInterface" size="medium" class="interface-select">
            <el-option label="手工/键盘" value="WEB_MANUAL" />
            <el-option label="有线扫码枪" value="WIRED_SCANNER" />
            <el-option label="无线扫码枪" value="WIRELESS_SCANNER" />
            <el-option label="PDA摄像头" value="PDA_CAMERA" />
          </el-select>
          <el-input
            v-model="scannerDeviceId"
            size="medium"
            class="device-input"
            placeholder="设备ID"
          />
          <el-input
            v-model="code"
            size="medium"
            placeholder="扫描二维码/条码后可直接回车"
            @keyup.enter.native="lookup"
          />
          <el-button type="primary" :loading="lookupLoading" @click="lookup">扫码查询</el-button>
          <el-button @click="clearResult">清空</el-button>
        </div>

        <div v-if="savedRecord && !result" class="result-card">
          <div class="result-top">
            <div>
              <div class="entity-type">已保存扫码记录</div>
              <div class="entity-code">{{ savedRecord.parsedCode || savedRecord.rawContent }}</div>
            </div>
            <el-tag type="info">未匹配业务对象</el-tag>
          </div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="扫码格式">{{ savedRecord.scanFormat || '-' }}</el-descriptions-item>
            <el-descriptions-item label="内容格式">{{ savedRecord.contentFormat || '-' }}</el-descriptions-item>
            <el-descriptions-item label="编码类型">{{ savedRecord.codeType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="商家平台">{{ savedRecord.merchantPlatform || '-' }}</el-descriptions-item>
            <el-descriptions-item label="扫码接口">{{ savedRecord.scannerInterface || '-' }}</el-descriptions-item>
            <el-descriptions-item label="设备ID">{{ savedRecord.scannerDeviceId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="保存时间">{{ savedRecord.createdAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="原始内容">{{ savedRecord.rawContent || '-' }}</el-descriptions-item>
          </el-descriptions>
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
            <el-descriptions-item v-if="primaryCode !== '-'" label="作业编码">{{ primaryCode }}</el-descriptions-item>
            <el-descriptions-item v-if="result.cargoCode" label="货物码">{{ result.cargoCode }}</el-descriptions-item>
            <el-descriptions-item v-if="result.cargoCodeType" label="货物码类型">{{ codeTypeText(result.cargoCodeType) }}</el-descriptions-item>
            <el-descriptions-item v-if="result.externalPlatform" label="商家平台">{{ merchantPlatformText(result.externalPlatform) }}</el-descriptions-item>
            <el-descriptions-item v-if="result.externalCode" label="外部商家码">{{ result.externalCode }}</el-descriptions-item>
            <el-descriptions-item v-if="result.skuCode" label="SKU">{{ result.skuCode }}</el-descriptions-item>
            <el-descriptions-item v-if="result.productName" label="商品">{{ result.productName }}</el-descriptions-item>
            <el-descriptions-item v-if="result.batchNo" label="批次号">{{ result.batchNo }}</el-descriptions-item>
            <el-descriptions-item v-if="result.pickupDueAt" label="取件截止">{{ result.pickupDueAt }}</el-descriptions-item>
            <el-descriptions-item v-if="result.locationCode" label="库位编码">{{ result.locationCode }}</el-descriptions-item>
            <el-descriptions-item v-if="result.locationName" label="库位名称">{{ result.locationName }}</el-descriptions-item>
            <el-descriptions-item v-if="result.locationFullName" label="具体库位">{{ result.locationFullName }}</el-descriptions-item>
            <el-descriptions-item v-if="result.zoneName" label="库区">{{ result.zoneName }}</el-descriptions-item>
            <el-descriptions-item v-if="result.aisleNo || result.shelfNo || result.layerNo || result.binNo" label="货架层位">
              {{ [result.aisleNo, result.shelfNo, result.layerNo, result.binNo].filter(Boolean).join(' / ') || '-' }}
            </el-descriptions-item>
            <el-descriptions-item v-if="result.entityType === 'INBOUND_ORDER_ITEM'" label="入库扫码确认">
              {{ result.putawayScanConfirmed ? '已确认' : '待确认' }}
            </el-descriptions-item>
            <el-descriptions-item v-if="result.putawayScanConfirmedAt" label="确认时间">{{ result.putawayScanConfirmedAt }}</el-descriptions-item>
            <el-descriptions-item v-if="result.putawayScanOperator" label="确认人">{{ result.putawayScanOperator }}</el-descriptions-item>
          </el-descriptions>

          <div class="action-panel" v-if="result.entityType === 'INBOUND_ORDER_ITEM'">
            <div class="action-title">入库贴码确认</div>
            <div class="action-hint">
              标签需先打印并贴到货物上，现场扫描该标签确认后，入库单才能正式上架入库。
            </div>
            <div class="action-row">
              <el-button
                type="primary"
                :loading="actionLoading === 'scanPutaway'"
                :disabled="result.putawayScanConfirmed || result.status === 'PUTAWAY_COMPLETED'"
                @click="confirmInboundPutaway"
              >
                扫码确认入库
              </el-button>
              <el-button
                type="success"
                plain
                :loading="actionLoading === 'transferOutbound'"
                @click="transferInboundCargoToOutbound"
              >
                扫码转出库
              </el-button>
              <el-button
                :disabled="!result.orderId"
                @click="$router.push({ path: '/inbounds' })"
              >
                查看入库单
              </el-button>
            </div>
          </div>

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
            <div class="outbound-scan-bar">
              <el-input
                v-model="outboundScanCode"
                placeholder="扫描商品条码/SKU/二维码内容后回车"
                @keyup.enter.native="scanOutboundItem"
              />
              <el-button
                type="primary"
                :loading="actionLoading === 'scanShip'"
                :disabled="result.status === 'SHIPPED'"
                @click="scanOutboundItem"
              >
                确认扫码
              </el-button>
            </div>
            <el-table v-if="result.items && result.items.length" :data="result.items" size="small" border>
              <el-table-column prop="skuCode" label="SKU" min-width="130" />
              <el-table-column prop="productName" label="商品" min-width="150" />
              <el-table-column prop="plannedQty" label="应扫" width="90" />
              <el-table-column prop="shippedQty" label="已扫" width="90" />
            </el-table>
            <div class="action-row">
              <el-button
                :loading="actionLoading === 'ship'"
                :disabled="result.status === 'SHIPPED' || !outboundScanCompleted"
                @click="shipOutbound"
              >
                完成出库
              </el-button>
            </div>
          </div>
        </div>

        <div v-if="!result && !savedRecord" class="pda-empty">
          扫描后会在这里显示作业编码、库位或库存信息，并提供直接操作按钮。
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

      <div class="page-card scan-record-card">
        <div class="history-head">
          <div class="section-title">系统扫码记录</div>
          <el-button type="text" :loading="recordsLoading" @click="fetchScanRecords">刷新</el-button>
        </div>
        <el-table :data="scanRecords" size="small" stripe>
          <el-table-column prop="createdAt" label="时间" min-width="155" />
          <el-table-column prop="scanFormat" label="扫码格式" width="90" />
          <el-table-column prop="contentFormat" label="内容格式" width="100" />
          <el-table-column prop="merchantPlatform" label="平台" width="100">
            <template slot-scope="scope">
              {{ merchantPlatformText(scope.row.merchantPlatform) }}
            </template>
          </el-table-column>
          <el-table-column prop="scannerInterface" label="接口" width="110" />
          <el-table-column prop="parsedCode" label="解析编码" min-width="160" />
          <el-table-column label="匹配对象" min-width="130">
            <template slot-scope="scope">
              <el-tag size="mini" :type="scope.row.matched ? 'success' : 'info'">
                {{ entityTypeText(scope.row.entityType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="rawContent" label="原始内容" min-width="220" show-overflow-tooltip />
        </el-table>
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
      scanFormat: 'AUTO',
      result: null,
      savedRecord: null,
      scanRecords: [],
      history: [],
      scannerInterface: 'WEB_MANUAL',
      scannerDeviceId: '',
      outboundScanCode: '',
      delayMinutes: 30,
      lookupLoading: false,
      actionLoading: '',
      recordsLoading: false
    }
  },
  computed: {
    primaryCode() {
      if (!this.result) {
        return '-'
      }
      return this.result.code || this.result.orderNo || this.result.cargoCode || this.result.pickupCode || this.result.scanCode || this.result.logisticsNo || this.result.locationCode || '-'
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
    },
    outboundScanCompleted() {
      if (!this.result || this.result.entityType !== 'OUTBOUND_ORDER') {
        return false
      }
      const items = this.result.items || []
      return items.length > 0 && items.every(item => this.toNumber(item.shippedQty) >= this.toNumber(item.plannedQty))
    }
  },
  async created() {
    await this.fetchScanRecords()
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
        INBOUND_ORDER_ITEM: '入库货物',
        LOCATION: '库位',
        PRODUCT_STOCK: '商品库存',
        UNMATCHED: '未匹配'
      }
      return map[type] || type || '-'
    },
    merchantPlatformText(platform) {
      const map = {
        TAOBAO: '淘宝',
        TMALL: '天猫',
        PINDUODUO: '拼多多',
        MEITUAN: '美团',
        TAOBAO_FLASH: '淘宝闪购',
        WMS: 'WMS'
      }
      return map[platform] || platform || '-'
    },
    codeTypeText(type) {
      return { QR_CODE: '二维码', BAR_CODE: '条形码' }[type] || type || '-'
    },
    async lookup() {
      if (!this.code.trim()) {
        this.$message.error('请先输入或扫描编码')
        return
      }
      try {
        this.lookupLoading = true
        const response = await post('/scan/records', {
          rawContent: this.code.trim(),
          scanFormat: this.scanFormat,
          sourceDevice: 'WEB_PDA',
          scannerInterface: this.scannerInterface,
          scannerDeviceId: this.scannerDeviceId,
          operatorName: '系统管理员'
        })
        const data = response.data || {}
        this.savedRecord = data.record || null
        this.result = data.lookupData || null
        if (this.result && this.result.entityType === 'OUTBOUND_ORDER') {
          await this.fetchOutboundDetail(this.result.id)
        }
        this.pushHistory((this.savedRecord && this.savedRecord.parsedCode) || this.code.trim(), this.result || this.savedRecord)
        await this.fetchScanRecords()
        if (!data.matched) {
          this.$message.warning(data.message || '扫码记录已保存，但未匹配业务对象')
        }
      } catch (error) {
        this.$message.error(error.message || '扫码查询失败')
      } finally {
        this.lookupLoading = false
      }
    },
    async fetchOutboundDetail(id) {
      const response = await get(`/outbounds/${id}`)
      this.result = { ...(response.data || {}), entityType: 'OUTBOUND_ORDER' }
    },
    async scanOutboundItem() {
      if (!this.result || this.result.entityType !== 'OUTBOUND_ORDER') {
        return
      }
      if (!this.outboundScanCode.trim()) {
        this.$message.error('请先扫描商品条码或二维码')
        return
      }
      try {
        this.actionLoading = 'scanShip'
        const response = await post(`/outbounds/${this.result.id}/scan-ship`, {
          rawContent: this.outboundScanCode.trim(),
          scanFormat: this.scanFormat,
          sourceDevice: 'WEB_PDA',
          scannerInterface: this.scannerInterface,
          scannerDeviceId: this.scannerDeviceId,
          operatorName: '系统管理员'
        })
        this.result = { ...(response.data || {}), entityType: 'OUTBOUND_ORDER' }
        this.outboundScanCode = ''
        this.$message.success('扫码确认成功')
        await this.fetchScanRecords()
      } catch (error) {
        this.$message.error(error.message || '扫码确认失败')
      } finally {
        this.actionLoading = ''
      }
    },
    async confirmInboundPutaway() {
      if (!this.result || this.result.entityType !== 'INBOUND_ORDER_ITEM') {
        return
      }
      if (!this.code.trim()) {
        this.$message.error('请先扫描货物标签')
        return
      }
      try {
        this.actionLoading = 'scanPutaway'
        await post('/inbounds/scan-putaway', {
          rawContent: this.code.trim(),
          scanFormat: this.scanFormat,
          sourceDevice: 'WEB_PDA',
          scannerInterface: this.scannerInterface,
          scannerDeviceId: this.scannerDeviceId,
          operatorName: '系统管理员',
          remark: 'PDA 扫码确认入库贴码'
        })
        this.$message.success('货物标签已确认，可以正式上架入库')
        const lookupResponse = await get('/scan/lookup', { code: this.code.trim(), _ts: Date.now() })
        this.result = lookupResponse.data || this.result
        await this.fetchScanRecords()
      } catch (error) {
        this.$message.error(error.message || '入库扫码确认失败')
      } finally {
        this.actionLoading = ''
      }
    },
    async transferInboundCargoToOutbound() {
      if (!this.result || this.result.entityType !== 'INBOUND_ORDER_ITEM') {
        return
      }
      const rawContent = this.code.trim() || this.result.cargoCode
      if (!rawContent) {
        this.$message.error('请先扫描入库货物标签')
        return
      }
      try {
        this.actionLoading = 'transferOutbound'
        const response = await post('/outbounds/scan-transfer', {
          rawContent,
          scanFormat: this.scanFormat,
          sourceDevice: 'WEB_PDA',
          scannerInterface: this.scannerInterface,
          scannerDeviceId: this.scannerDeviceId,
          operatorName: '系统管理员'
        })
        const data = response.data || {}
        this.result = { ...data, entityType: 'OUTBOUND_ORDER' }
        this.outboundScanCode = ''
        this.$message.success(data.message || response.message || '已转入出库单')
        await this.fetchScanRecords()
      } catch (error) {
        this.$message.error(error.message || '扫码转出库失败')
      } finally {
        this.actionLoading = ''
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
      this.savedRecord = null
      this.outboundScanCode = ''
    },
    reuseCode(code) {
      this.code = code
      this.lookup()
    },
    pushHistory(code, result) {
      const next = [{ code, entityType: result.entityType }].concat(this.history.filter(item => item.code !== code))
      this.history = next.slice(0, 8)
    },
    async fetchScanRecords() {
      try {
        this.recordsLoading = true
        const response = await get('/scan/records', { limit: 20, _ts: Date.now() })
        this.scanRecords = response.data || []
      } catch (error) {
        this.$message.error(error.message || '加载扫码记录失败')
      } finally {
        this.recordsLoading = false
      }
    },
    toNumber(value) {
      const number = Number(value)
      return Number.isFinite(number) ? number : 0
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
  grid-template-columns: 130px 140px 130px minmax(180px, 1fr) auto auto;
  gap: 8px;
}

.format-select {
  width: 130px;
}

.interface-select {
  width: 140px;
}

.device-input {
  width: 130px;
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

.outbound-scan-bar {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
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

  .format-select {
    width: 100%;
  }

  .interface-select,
  .device-input {
    width: 100%;
  }

  .result-top {
    flex-direction: column;
    align-items: flex-start;
  }

  .action-row {
    flex-direction: column;
    align-items: stretch;
  }

  .outbound-scan-bar {
    grid-template-columns: 1fr;
  }

  .action-row .el-button,
  .action-row .el-input-number {
    width: 100%;
  }
}
</style>
