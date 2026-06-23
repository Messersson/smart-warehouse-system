const base = process.env.WMS_BASE_URL || 'http://127.0.0.1:18080/api'
const username = process.env.WMS_USERNAME || 'admin'
const password = process.env.WMS_PASSWORD || 'admin123'
const ts = new Date().toISOString().replace(/\D/g, '').slice(0, 14)
const now = new Date()
const nowStr = now.toISOString().slice(0, 19)
const futureStr = new Date(now.getTime() + 2 * 60 * 60 * 1000).toISOString().slice(0, 19)
const month = now.toISOString().slice(0, 7)
const results = []
let token = ''

function add(step, status, detail = '') {
  results.push({ step, status, detail: String(detail || '') })
}

async function request(method, path, body) {
  const headers = { Accept: 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  const options = { method, headers }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    options.body = JSON.stringify(body)
  }
  const res = await fetch(`${base}${path}`, options)
  const text = await res.text()
  let payload
  try {
    payload = text ? JSON.parse(text) : null
  } catch {
    payload = text
  }
  if (!res.ok) {
    const message = payload && payload.message ? payload.message : text || res.statusText
    throw new Error(`${method} ${path} -> ${res.status}: ${message}`)
  }
  if (payload && payload.success === false) {
    throw new Error(`${method} ${path}: ${payload.message || 'API returned success=false'}`)
  }
  return payload
}

const get = path => request('GET', path)
const post = (path, body) => request('POST', path, body)
const put = (path, body) => request('PUT', path, body)
const del = path => request('DELETE', path)

async function step(name, fn) {
  try {
    const detail = await fn()
    add(name, 'PASS', detail)
    return detail
  } catch (error) {
    add(name, 'FAIL', error.message)
    return null
  }
}

function sumQty(stocks) {
  return (stocks || []).reduce((sum, item) => sum + Number(item.quantity || 0), 0)
}

const ids = {}

await step('auth-login', async () => {
  const payload = await post('/auth/login', { username, password })
  token = payload.data.token
  if (!token) throw new Error('login did not return token')
  return payload.data.username
})

const seed = await step('seed-lookups', async () => {
  const payload = await get('/lookups')
  const data = payload.data
  const first = key => (data[key] || [])[0]
  const found = {
    warehouse: first('warehouses'),
    location: first('locations'),
    owner: first('owners'),
    supplier: first('suppliers'),
    customer: first('customers'),
    product: first('products')
  }
  for (const [key, value] of Object.entries(found)) {
    if (!value || !value.id) throw new Error(`missing seed ${key}`)
  }
  return found
})

if (seed) {
  const warehouse = await step('warehouse-create-update-delete', async () => {
    const created = (await post('/warehouses', {
      warehouseCode: `AUTO-WH-${ts}`,
      warehouseName: `Auto Warehouse ${ts}`,
      warehouseType: 'GENERAL',
      sceneType: 'GENERAL_STORAGE',
      contactName: 'Auto',
      contactPhone: '13900001001',
      province: 'Shanghai',
      city: 'Shanghai',
      district: 'Pudong',
      address: 'Auto test address',
      status: 'ACTIVE'
    })).data
    const updated = (await put(`/warehouses/${created.id}`, { ...created, warehouseName: `Auto Warehouse Updated ${ts}` })).data
    await del(`/warehouses/${created.id}`)
    return `${updated.id}:${updated.warehouseName}`
  })

  await step('master-data-crud', async () => {
    const owner = (await post('/owners', {
      ownerCode: `AUTO-OWN-${ts}`,
      ownerName: `Auto Owner ${ts}`,
      ownerType: 'SELF',
      contactName: 'Auto',
      contactPhone: '13900001002',
      status: 'ACTIVE'
    })).data
    await put(`/owners/${owner.id}`, { ...owner, ownerName: `Auto Owner Updated ${ts}` })
    await del(`/owners/${owner.id}`)

    const supplier = (await post('/suppliers', {
      supplierCode: `AUTO-SUP-${ts}`,
      supplierName: `Auto Supplier ${ts}`,
      contactName: 'Auto',
      contactPhone: '13900001003',
      status: 'ACTIVE'
    })).data
    await put(`/suppliers/${supplier.id}`, { ...supplier, supplierName: `Auto Supplier Updated ${ts}` })
    await del(`/suppliers/${supplier.id}`)

    const customer = (await post('/customers', {
      customerCode: `AUTO-CUS-${ts}`,
      customerName: `Auto Customer ${ts}`,
      customerType: 'B2B',
      contactName: 'Auto',
      contactPhone: '13900001004',
      status: 'ACTIVE'
    })).data
    await put(`/customers/${customer.id}`, { ...customer, customerName: `Auto Customer Updated ${ts}` })
    await del(`/customers/${customer.id}`)

    const product = (await post('/products', {
      skuCode: `AUTO-SKU-CRUD-${ts}`,
      productName: `Auto Product ${ts}`,
      productSpec: '1/box',
      categoryName: 'Auto',
      brandName: 'Auto',
      unitName: 'BOX',
      barcode: `7900${ts}`,
      safeStock: 0,
      maxStock: 1000,
      shelfLifeDays: 365,
      status: 'ACTIVE'
    })).data
    await put(`/products/${product.id}`, { ...product, productName: `Auto Product Updated ${ts}` })
    await del(`/products/${product.id}`)
    return warehouse ? 'warehouse and master CRUD ok' : 'master CRUD ok'
  })

  const txProduct = await step('transaction-product-create', async () => {
    const product = (await post('/products', {
      skuCode: `AUTO-SKU-TX-${ts}`,
      productName: `Auto Transaction Product ${ts}`,
      productSpec: '1/box',
      categoryName: 'Auto',
      brandName: 'Auto',
      unitName: 'BOX',
      barcode: `8800${ts}`,
      safeStock: 0,
      maxStock: 1000,
      shelfLifeDays: 365,
      status: 'ACTIVE'
    })).data
    ids.productId = product.id
    return product
  })

  let inbound
  if (txProduct) {
    inbound = await step('inbound-create-receive-putaway', async () => {
      const created = (await post('/inbounds', {
        orderNo: `AUTO-IN-${ts}`,
        warehouseId: seed.warehouse.id,
        supplierId: seed.supplier.id,
        ownerId: seed.owner.id,
        orderType: 'PURCHASE',
        sourceNo: `SRC-IN-${ts}`,
        expectedArrivalTime: nowStr,
        operatorName: 'Auto Test',
        items: [{
          productId: txProduct.id,
          batchNo: `BATCH-${ts}`,
          expectedQty: 8,
          actualQty: 8,
          qualifiedQty: 8,
          locationId: seed.location.id,
          productionDate: nowStr.slice(0, 10),
          expiryDate: new Date(now.getTime() + 30 * 86400000).toISOString().slice(0, 10)
        }]
      })).data
      await post(`/inbounds/${created.id}/receive`)
      const receivedDetail = (await get(`/inbounds/${created.id}`)).data
      const items = receivedDetail.items || []
      for (const item of items) {
        await post('/inbounds/scan-putaway', {
          rawContent: item.cargoCodeContent || item.cargoCode,
          scanFormat: item.cargoCodeType || 'QR_CODE',
          sourceDevice: 'AUTO',
          scannerInterface: 'AUTO_TEST',
          operatorName: 'Auto Test',
          remark: 'Auto putaway scan confirmation'
        })
      }
      const putaway = (await post(`/inbounds/${created.id}/putaway`)).data
      ids.inboundId = created.id
      return putaway
    })
  }

  await step('stock-after-inbound', async () => {
    const payload = await get(`/stocks?warehouseId=${seed.warehouse.id}&keyword=${encodeURIComponent(txProduct.skuCode)}`)
    const qty = sumQty(payload.data)
    if (qty < 8) throw new Error(`expected stock >= 8, got ${qty}`)
    return `qty=${qty}`
  })

  let outbound
  if (txProduct && inbound) {
    outbound = await step('outbound-create-pick-ship', async () => {
      const created = (await post('/outbounds', {
        orderNo: `AUTO-OUT-${ts}`,
        warehouseId: seed.warehouse.id,
        customerId: seed.customer.id,
        ownerId: seed.owner.id,
        orderType: 'SALES',
        sourceNo: `SRC-OUT-${ts}`,
        priorityLevel: 'NORMAL',
        plannedShipTime: futureStr,
        operatorName: 'Auto Test',
        logisticsNo: `AUTO-LOG-${ts}`,
        items: [{
          productId: txProduct.id,
          batchNo: `BATCH-${ts}`,
          plannedQty: 3,
          shippedQty: 0,
          locationId: seed.location.id
        }]
      })).data
      await post(`/outbounds/${created.id}/picking`)
      for (let i = 0; i < 3; i += 1) {
        await post(`/outbounds/${created.id}/scan-ship`, {
          rawContent: txProduct.skuCode,
          scanFormat: 'TEXT',
          operatorName: 'Auto Test',
          sourceDevice: 'AUTO',
          scannerInterface: 'AUTO_TEST'
        })
      }
      const shipped = (await post(`/outbounds/${created.id}/ship`)).data
      ids.outboundId = created.id
      return shipped
    })
  }

  await step('scan-lookup-and-record', async () => {
    if (!outbound) throw new Error('missing outbound')
    const lookup = (await get(`/scan/lookup?code=${encodeURIComponent(outbound.logisticsNo)}`)).data
    const record = (await post('/scan/records', {
      rawContent: outbound.logisticsNo,
      scanFormat: 'TEXT',
      sourceDevice: 'AUTO',
      operatorName: 'Auto Test'
    })).data
    if (!record.record || !record.record.id) throw new Error('scan record response missing record.id')
    return `${lookup.entityType}:${record.record.id}`
  })

  await step('alert-notification-read', async () => {
    await post('/alerts/scan')
    const alerts = (await get('/alerts')).data
    const notifications = (await get('/notifications')).data
    return `alerts=${alerts.length}, notifications=${notifications.length}`
  })

  let exceptionTicket
  await step('exception-and-approval-flow', async () => {
    exceptionTicket = (await post('/exceptions', {
      warehouseId: seed.warehouse.id,
      bizType: 'MANUAL',
      bizNo: `AUTO-EX-${ts}`,
      ticketTitle: `Auto Exception ${ts}`,
      ticketContent: 'Auto exception content',
      severity: 'HIGH',
      reporterName: 'Auto Test'
    })).data
    await post(`/exceptions/${exceptionTicket.id}/assign?assigneeName=${encodeURIComponent('Auto Assignee')}`)
    const approval = (await post('/approvals/submit', {
      approvalType: 'EXCEPTION_APPROVAL',
      bizType: 'EXCEPTION_TICKET',
      bizId: exceptionTicket.id,
      bizNo: exceptionTicket.ticketNo,
      applicantName: 'Auto Test',
      approverName: 'Auto Manager',
      applyReason: 'Auto approval'
    })).data
    if (approval.status !== 'APPROVED') {
      await post(`/approvals/${approval.id}/approve`, { operatorName: 'Auto Approver', comment: 'approved' })
    }
    await post(`/exceptions/${exceptionTicket.id}/resolve?operatorName=${encodeURIComponent('Auto Resolver')}`)
    const closed = (await post(`/exceptions/${exceptionTicket.id}/close`)).data
    return closed.status
  })

  await step('permission-user-role-menu-flow', async () => {
    const role = (await post('/permissions/roles', {
      roleCode: `AUTO_ROLE_${ts}`,
      roleName: `Auto Role ${ts}`,
      roleDesc: 'Auto role',
      status: 'ACTIVE'
    })).data
    const menu = (await post('/permissions/menus', {
      menuCode: `AUTO_MENU_${ts}`,
      menuName: `Auto Menu ${ts}`,
      menuPath: `/auto/${ts}`,
      componentName: 'AutoPage',
      iconName: 'Box',
      menuType: 'MENU',
      permissionCode: `auto:${ts}`,
      sortNo: 999,
      visible: true,
      status: 'ACTIVE'
    })).data
    await post(`/permissions/roles/${role.id}/menus`, { menuIds: [1, menu.id] })
    const user = (await post('/permissions/users', {
      username: `auto_user_${ts}`,
      password: 'qa123456',
      displayName: `Auto User ${ts}`,
      phone: '13900001012',
      email: 'auto-user@example.com',
      roleCode: role.roleCode,
      status: 'ACTIVE'
    })).data
    const login = (await post('/auth/login', { username: user.username, password: 'qa123456' })).data
    if (!login.menuPaths.includes(`/auto/${ts}`)) throw new Error('created role menu missing in login response')
    return `user=${user.id}, role=${role.id}, menu=${menu.id}`
  })

  await step('billing-flow', async () => {
    const contract = (await post('/billing/contracts', {
      customerId: seed.customer.id,
      ownerId: seed.owner.id,
      warehouseId: seed.warehouse.id,
      contractNo: `AUTO-BC-${ts}`,
      contractName: `Auto Billing Contract ${ts}`,
      effectiveDate: `${month}-01`,
      expireDate: new Date(now.getTime() + 31 * 86400000).toISOString().slice(0, 10),
      settlementCycle: 'MONTHLY',
      status: 'ACTIVE'
    })).data
    const rule = (await post('/billing/rules', {
      contractId: contract.id,
      chargeType: 'INBOUND_QTY',
      ruleName: `Auto Inbound Qty Rule ${ts}`,
      unitName: 'BOX',
      unitPrice: 2.5,
      status: 'ACTIVE'
    })).data
    await put(`/billing/rules/${rule.id}`, { ...rule, unitPrice: 3 })
    const statement = (await post('/billing/statements/generate', {
      contractId: contract.id,
      statementMonth: month
    })).data
    await post(`/billing/statements/${statement.id}/pay`)
    return `statement=${statement.id}, total=${statement.totalAmount}`
  })

  await step('stocktake-flow', async () => {
    const stockTake = (await post('/stock-takes', {
      warehouseId: seed.warehouse.id,
      ownerId: seed.owner.id,
      takeType: 'CYCLE',
      plannedStartTime: nowStr,
      plannedEndTime: futureStr,
      operatorName: 'Auto Test'
    })).data
    if (!stockTake.items || !stockTake.items.length) throw new Error('no stock take items')
    const item = stockTake.items.find(i => i.productId === txProduct.id) || stockTake.items[0]
    const actualQty = Math.max(0, Number(item.systemQty || 0) - 1)
    await post(`/stock-takes/${stockTake.id}/count`, { items: [{ id: item.id, actualQty, remark: 'Auto count' }] })
    await post(`/stock-takes/${stockTake.id}/finish`)
    const adjusted = (await post(`/stock-takes/${stockTake.id}/adjust`)).data
    return adjusted.status
  })

  await step('list-endpoints-read', async () => {
    const paths = [
      '/dashboard/overview',
      '/warehouses',
      '/locations',
      '/owners',
      '/suppliers',
      '/customers',
      '/products',
      '/inbounds',
      '/outbounds',
      '/stocks',
      '/alerts',
      '/permissions/overview',
      '/billing/overview',
      '/approvals',
      '/exceptions',
      '/stock-takes',
      '/scan/records',
      '/scan/cargo-code-records'
    ]
    for (const path of paths) await get(path)
    return `${paths.length} GET endpoints ok`
  })
}

const summary = {
  timestamp: ts,
  passed: results.filter(r => r.status === 'PASS').length,
  failed: results.filter(r => r.status === 'FAIL').length,
  results
}

console.log(JSON.stringify(summary, null, 2))
if (summary.failed > 0) process.exit(1)
