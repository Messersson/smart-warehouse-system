<template>
  <el-dialog
    :title="dialogTitle"
    :visible="value"
    width="620px"
    :close-on-click-modal="!submitting"
    @update:visible="$emit('input', $event)"
  >
    <el-form :model="form" label-width="110px">
      <el-row :gutter="16">
        <el-col v-for="field in fields" :key="field.prop" :span="field.span || 12">
          <el-form-item :label="field.label">
            <el-input v-if="field.type === 'text'" v-model="form[field.prop]" />
            <el-input-number
              v-else-if="field.type === 'number'"
              v-model="form[field.prop]"
              :min="0"
              :controls="false"
              style="width: 100%"
            />
            <el-select
              v-else-if="field.type === 'select'"
              v-model="form[field.prop]"
              filterable
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="option in field.options || []"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <el-switch v-else-if="field.type === 'switch'" v-model="form[field.prop]" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <span slot="footer">
      <el-button :disabled="submitting" @click="$emit('input', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">保存并选中</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { post } from '../api'

const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'INACTIVE' }
]

const platformOptions = [
  { label: '淘宝', value: 'TAOBAO' },
  { label: '天猫', value: 'TMALL' },
  { label: '拼多多', value: 'PINDUODUO' },
  { label: '美团', value: 'MEITUAN' },
  { label: '淘宝闪购', value: 'TAOBAO_FLASH' },
  { label: '其他', value: 'OTHER' }
]

function suffix() {
  return String(Date.now()).slice(-8)
}

const configs = {
  warehouses: {
    title: '快速新增仓库',
    endpoint: '/warehouses',
    labelProp: 'warehouseName',
    fields: [
      { prop: 'warehouseCode', label: '仓库编码', type: 'text' },
      { prop: 'warehouseName', label: '仓库名称', type: 'text' },
      { prop: 'sceneType', label: '业务场景', type: 'select', options: [
        { label: '普通仓储', value: 'GENERAL_STORAGE' },
        { label: '驿站', value: 'PARCEL_STATION' },
        { label: '外卖柜', value: 'TAKEOUT_LOCKER' },
        { label: '校园取件', value: 'CAMPUS_PICKUP' }
      ] },
      { prop: 'scanMode', label: '扫码模式', type: 'select', options: [
        { label: '二维码', value: 'QR_CODE' },
        { label: '条形码', value: 'BAR_CODE' },
        { label: '混合', value: 'HYBRID' }
      ] },
      { prop: 'dwellAlertMinutes', label: '滞留分钟', type: 'number' },
      { prop: 'autoAssignLocation', label: '自动库位', type: 'switch' }
    ],
    defaults: () => ({
      warehouseCode: `WH-${suffix()}`,
      warehouseName: '',
      warehouseType: 'GENERAL',
      sceneType: 'GENERAL_STORAGE',
      scanMode: 'QR_CODE',
      dwellAlertMinutes: 0,
      smsNotifyEnabled: false,
      smsReminderIntervalMinutes: 120,
      autoAssignLocation: true,
      status: 'ACTIVE'
    })
  },
  suppliers: {
    title: '快速新增供应商',
    endpoint: '/suppliers',
    labelProp: 'supplierName',
    fields: [
      { prop: 'supplierCode', label: '供应商编码', type: 'text' },
      { prop: 'supplierName', label: '供应商名称', type: 'text' },
      { prop: 'platformType', label: '平台类型', type: 'select', options: platformOptions },
      { prop: 'contactName', label: '联系人', type: 'text' },
      { prop: 'contactPhone', label: '联系电话', type: 'text' }
    ],
    defaults: () => ({ supplierCode: `SUP-${suffix()}`, supplierName: '', platformType: 'OTHER', status: 'ACTIVE' })
  },
  owners: {
    title: '快速新增货主',
    endpoint: '/owners',
    labelProp: 'ownerName',
    fields: [
      { prop: 'ownerCode', label: '货主编码', type: 'text' },
      { prop: 'ownerName', label: '货主名称', type: 'text' },
      { prop: 'contactName', label: '联系人', type: 'text' },
      { prop: 'contactPhone', label: '联系电话', type: 'text' }
    ],
    defaults: () => ({ ownerCode: `OWN-${suffix()}`, ownerName: '', ownerType: 'SELF', status: 'ACTIVE' })
  },
  customers: {
    title: '快速新增客户',
    endpoint: '/customers',
    labelProp: 'customerName',
    fields: [
      { prop: 'customerCode', label: '客户编码', type: 'text' },
      { prop: 'customerName', label: '客户名称', type: 'text' },
      { prop: 'contactName', label: '联系人', type: 'text' },
      { prop: 'contactPhone', label: '联系电话', type: 'text' }
    ],
    defaults: () => ({ customerCode: `CUS-${suffix()}`, customerName: '', customerType: 'B2B', status: 'ACTIVE' })
  },
  products: {
    title: '快速新增商品',
    endpoint: '/products',
    labelProp: 'productName',
    fields: [
      { prop: 'skuCode', label: 'SKU', type: 'text' },
      { prop: 'productName', label: '商品名称', type: 'text' },
      { prop: 'categoryName', label: '商品分类', type: 'text' },
      { prop: 'unitName', label: '单位', type: 'text' },
      { prop: 'barcode', label: '条形码', type: 'text' }
    ],
    defaults: () => ({ skuCode: `SKU-${suffix()}`, productName: '', unitName: '件', status: 'ACTIVE' })
  },
  locations: {
    title: '快速新增库位',
    endpoint: '/locations',
    labelProp: 'locationCode',
    fields: [
      { prop: 'locationCode', label: '库位编码', type: 'text' },
      { prop: 'locationName', label: '库位名称', type: 'text' },
      { prop: 'zoneName', label: '库区', type: 'text' },
      { prop: 'capacityQty', label: '容量', type: 'number' }
    ],
    defaults: context => ({
      warehouseId: context.warehouseId,
      locationCode: `LOC-${suffix()}`,
      locationName: '临时库位',
      zoneName: '默认库区',
      capacityQty: 999999,
      usedQty: 0,
      pickable: true,
      status: 'ACTIVE'
    })
  }
}

export default {
  name: 'QuickCreateDialog',
  props: {
    value: Boolean,
    resource: String,
    context: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      form: {},
      submitting: false
    }
  },
  computed: {
    config() {
      return configs[this.resource] || {}
    },
    fields() {
      return this.config.fields || []
    },
    dialogTitle() {
      return this.config.title || '快速新增'
    }
  },
  watch: {
    value(visible) {
      if (visible) {
        this.resetForm()
      }
    },
    resource() {
      this.resetForm()
    }
  },
  methods: {
    resetForm() {
      this.form = this.config.defaults ? this.config.defaults(this.context || {}) : {}
    },
    async submit() {
      if (this.resource === 'locations' && !this.form.warehouseId) {
        this.$message.error('请先选择仓库，再新增库位')
        return
      }
      try {
        this.submitting = true
        const response = await post(this.config.endpoint, this.form)
        this.$emit('created', {
          resource: this.resource,
          item: response.data,
          labelProp: this.config.labelProp || 'id'
        })
        this.$emit('input', false)
      } catch (error) {
        this.$message.error(error.message || '新增失败')
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>
