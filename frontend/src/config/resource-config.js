const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'INACTIVE' }
]

const warehouseTypeOptions = [
  { label: '普通仓', value: 'GENERAL' },
  { label: '冷链仓', value: 'COLD_CHAIN' },
  { label: '保税仓', value: 'BONDED' }
]

const warehouseSceneOptions = [
  { label: '通用仓储', value: 'GENERAL_STORAGE' },
  { label: '菜鸟驿站', value: 'PARCEL_STATION' },
  { label: '外卖柜', value: 'TAKEOUT_LOCKER' },
  { label: '校园驿站', value: 'CAMPUS_PICKUP' }
]

const warehouseScanOptions = [
  { label: '二维码', value: 'QR_CODE' },
  { label: '条码', value: 'BAR_CODE' },
  { label: '二维码+条码', value: 'HYBRID' }
]

export default {
  warehouses: {
    title: '仓库管理',
    endpoint: '/warehouses',
    columns: [
      { prop: 'warehouseCode', label: '仓库编码' },
      { prop: 'warehouseName', label: '仓库名称' },
      { prop: 'sceneType', label: '业务场景' },
      { prop: 'dwellAlertMinutes', label: '滞留时长(分钟)' },
      { prop: 'smsNotifyEnabled', label: '短信提醒' },
      { prop: 'autoAssignLocation', label: '自动分配库位' },
      { prop: 'status', label: '状态' }
    ],
    fields: [
      { prop: 'warehouseCode', label: '仓库编码', type: 'text', required: true },
      { prop: 'warehouseName', label: '仓库名称', type: 'text', required: true },
      { prop: 'warehouseType', label: '仓库类型', type: 'select', options: warehouseTypeOptions, required: true },
      { prop: 'sceneType', label: '业务场景', type: 'select', options: warehouseSceneOptions, required: true },
      { prop: 'contactName', label: '联系人', type: 'text' },
      { prop: 'contactPhone', label: '联系电话', type: 'text' },
      { prop: 'province', label: '省份', type: 'text' },
      { prop: 'city', label: '城市', type: 'text' },
      { prop: 'district', label: '区县', type: 'text' },
      { prop: 'address', label: '详细地址', type: 'textarea' },
      { prop: 'dwellAlertMinutes', label: '滞留时长(分钟)', type: 'number' },
      { prop: 'smsNotifyEnabled', label: '启用短信提醒', type: 'switch' },
      { prop: 'smsReminderIntervalMinutes', label: '短信重发间隔(分钟)', type: 'number' },
      { prop: 'autoAssignLocation', label: '自动分配库位', type: 'switch' },
      { prop: 'scanMode', label: '扫码模式', type: 'select', options: warehouseScanOptions },
      { prop: 'status', label: '状态', type: 'select', options: statusOptions, required: true },
      { prop: 'remark', label: '备注', type: 'textarea' }
    ],
    defaults: {
      warehouseType: 'GENERAL',
      sceneType: 'GENERAL_STORAGE',
      dwellAlertMinutes: 0,
      smsNotifyEnabled: false,
      smsReminderIntervalMinutes: 120,
      autoAssignLocation: true,
      scanMode: 'QR_CODE',
      status: 'ACTIVE'
    }
  },
  locations: {
    title: '库位管理',
    endpoint: '/locations',
    columns: [
      { prop: 'warehouseId', label: '所属仓库', lookup: 'warehouses', displayProp: 'warehouseName' },
      { prop: 'zoneName', label: '库区' },
      { prop: 'locationCode', label: '库位编码' },
      { prop: 'locationName', label: '库位名称' },
      { prop: 'capacityQty', label: '容量' },
      { prop: 'usedQty', label: '已用' },
      { prop: 'pickable', label: '可拣选' },
      { prop: 'status', label: '状态' }
    ],
    fields: [
      { prop: 'warehouseId', label: '所属仓库', type: 'lookup-select', lookup: 'warehouses', valueProp: 'id', labelProp: 'warehouseName', required: true },
      { prop: 'zoneName', label: '库区', type: 'text' },
      { prop: 'locationCode', label: '库位编码', type: 'text', required: true },
      { prop: 'locationName', label: '库位名称', type: 'text', required: true },
      { prop: 'aisleNo', label: '通道', type: 'text' },
      { prop: 'shelfNo', label: '货架', type: 'text' },
      { prop: 'layerNo', label: '层', type: 'text' },
      { prop: 'binNo', label: '位', type: 'text' },
      { prop: 'capacityQty', label: '容量', type: 'number' },
      { prop: 'usedQty', label: '已用', type: 'number' },
      { prop: 'pickable', label: '可拣选', type: 'switch' },
      { prop: 'status', label: '状态', type: 'select', options: statusOptions, required: true },
      { prop: 'remark', label: '备注', type: 'textarea' }
    ],
    defaults: {
      capacityQty: 0,
      usedQty: 0,
      pickable: true,
      status: 'ACTIVE'
    }
  },
  owners: {
    title: '货主管理',
    endpoint: '/owners',
    columns: [
      { prop: 'ownerCode', label: '货主编码' },
      { prop: 'ownerName', label: '货主名称' },
      { prop: 'ownerType', label: '类型' },
      { prop: 'contactName', label: '联系人' },
      { prop: 'contactPhone', label: '联系电话' },
      { prop: 'status', label: '状态' }
    ],
    fields: [
      { prop: 'ownerCode', label: '货主编码', type: 'text', required: true },
      { prop: 'ownerName', label: '货主名称', type: 'text', required: true },
      { prop: 'ownerType', label: '货主类型', type: 'select', options: [{ label: '自营', value: 'SELF' }, { label: '第三方', value: 'THIRD_PARTY' }], required: true },
      { prop: 'contactName', label: '联系人', type: 'text' },
      { prop: 'contactPhone', label: '联系电话', type: 'text' },
      { prop: 'email', label: '邮箱', type: 'text' },
      { prop: 'address', label: '地址', type: 'textarea' },
      { prop: 'status', label: '状态', type: 'select', options: statusOptions, required: true },
      { prop: 'remark', label: '备注', type: 'textarea' }
    ],
    defaults: {
      ownerType: 'SELF',
      status: 'ACTIVE'
    }
  },
  suppliers: {
    title: '供应商管理',
    endpoint: '/suppliers',
    columns: [
      { prop: 'supplierCode', label: '供应商编码' },
      { prop: 'supplierName', label: '供应商名称' },
      { prop: 'contactName', label: '联系人' },
      { prop: 'contactPhone', label: '联系电话' },
      { prop: 'status', label: '状态' }
    ],
    fields: [
      { prop: 'supplierCode', label: '供应商编码', type: 'text', required: true },
      { prop: 'supplierName', label: '供应商名称', type: 'text', required: true },
      { prop: 'contactName', label: '联系人', type: 'text' },
      { prop: 'contactPhone', label: '联系电话', type: 'text' },
      { prop: 'email', label: '邮箱', type: 'text' },
      { prop: 'address', label: '地址', type: 'textarea' },
      { prop: 'status', label: '状态', type: 'select', options: statusOptions, required: true },
      { prop: 'remark', label: '备注', type: 'textarea' }
    ],
    defaults: {
      status: 'ACTIVE'
    }
  },
  customers: {
    title: '客户管理',
    endpoint: '/customers',
    columns: [
      { prop: 'customerCode', label: '客户编码' },
      { prop: 'customerName', label: '客户名称' },
      { prop: 'customerType', label: '客户类型' },
      { prop: 'contactName', label: '联系人' },
      { prop: 'contactPhone', label: '联系电话' },
      { prop: 'status', label: '状态' }
    ],
    fields: [
      { prop: 'customerCode', label: '客户编码', type: 'text', required: true },
      { prop: 'customerName', label: '客户名称', type: 'text', required: true },
      { prop: 'customerType', label: '客户类型', type: 'select', options: [{ label: 'B2B', value: 'B2B' }, { label: 'B2C', value: 'B2C' }], required: true },
      { prop: 'contactName', label: '联系人', type: 'text' },
      { prop: 'contactPhone', label: '联系电话', type: 'text' },
      { prop: 'email', label: '邮箱', type: 'text' },
      { prop: 'address', label: '地址', type: 'textarea' },
      { prop: 'status', label: '状态', type: 'select', options: statusOptions, required: true },
      { prop: 'remark', label: '备注', type: 'textarea' }
    ],
    defaults: {
      customerType: 'B2B',
      status: 'ACTIVE'
    }
  },
  products: {
    title: '商品管理',
    endpoint: '/products',
    columns: [
      { prop: 'skuCode', label: 'SKU编码' },
      { prop: 'productName', label: '商品名称' },
      { prop: 'productSpec', label: '规格' },
      { prop: 'categoryName', label: '分类' },
      { prop: 'brandName', label: '品牌' },
      { prop: 'safeStock', label: '安全库存' },
      { prop: 'status', label: '状态' }
    ],
    fields: [
      { prop: 'skuCode', label: 'SKU编码', type: 'text', required: true },
      { prop: 'productName', label: '商品名称', type: 'text', required: true },
      { prop: 'productSpec', label: '规格', type: 'text' },
      { prop: 'categoryName', label: '分类', type: 'text' },
      { prop: 'brandName', label: '品牌', type: 'text' },
      { prop: 'unitName', label: '单位', type: 'text', required: true },
      { prop: 'barcode', label: '条码', type: 'text' },
      { prop: 'safeStock', label: '安全库存', type: 'number' },
      { prop: 'maxStock', label: '最大库存', type: 'number' },
      { prop: 'shelfLifeDays', label: '保质期天数', type: 'number' },
      { prop: 'enableBatch', label: '批次管理', type: 'switch' },
      { prop: 'enableSerial', label: '序列号管理', type: 'switch' },
      { prop: 'weightKg', label: '重量(kg)', type: 'number' },
      { prop: 'volumeM3', label: '体积(m3)', type: 'number' },
      { prop: 'salePrice', label: '销售价', type: 'number' },
      { prop: 'status', label: '状态', type: 'select', options: statusOptions, required: true },
      { prop: 'remark', label: '备注', type: 'textarea' }
    ],
    defaults: {
      unitName: '件',
      safeStock: 0,
      maxStock: 0,
      shelfLifeDays: 0,
      enableBatch: true,
      enableSerial: false,
      weightKg: 0,
      volumeM3: 0,
      salePrice: 0,
      status: 'ACTIVE'
    }
  }
}
