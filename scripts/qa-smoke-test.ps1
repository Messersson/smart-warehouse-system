$base = 'http://127.0.0.1:18080/api'
$ts = Get-Date -Format 'yyyyMMddHHmmss'
$now = Get-Date
$nowStr = $now.ToString('s')
$futureStr = $now.AddHours(2).ToString('s')
$month = $now.ToString('yyyy-MM')
$results = @()

function Add-Result([string]$step, [string]$status, [string]$detail) {
  $script:results += [pscustomobject]@{ step = $step; status = $status; detail = $detail }
}

function Get-Api([string]$path) {
  Invoke-RestMethod -Method Get -Uri ($base + $path)
}

function Send-Api([string]$method, [string]$path, $body = $null) {
  if ($null -eq $body) {
    Invoke-RestMethod -Method $method -Uri ($base + $path)
  } else {
    $json = $body | ConvertTo-Json -Depth 20
    Invoke-RestMethod -Method $method -Uri ($base + $path) -ContentType 'application/json' -Body $json
  }
}

function Qty-Sum($stocks) {
  if ($null -eq $stocks) {
    return [decimal]0
  }
  return ($stocks | Measure-Object -Property quantity -Sum).Sum
}

$seed = $null
$warehouse1 = $null
$location1 = $null
$owner1 = $null
$supplier1 = $null
$customer1 = $null
$product1 = $null

try {
  $seed = Get-Api '/lookups'
  $warehouse1 = $seed.data.warehouses | Where-Object { $_.id -eq 1 } | Select-Object -First 1
  $location1 = $seed.data.locations | Where-Object { $_.warehouseId -eq $warehouse1.id } | Select-Object -First 1
  $owner1 = $seed.data.owners | Select-Object -First 1
  $supplier1 = $seed.data.suppliers | Select-Object -First 1
  $customer1 = $seed.data.customers | Select-Object -First 1
  $product1 = $seed.data.products | Select-Object -First 1
  Add-Result 'seed-lookups' 'PASS' "warehouse=$($warehouse1.id), location=$($location1.id), owner=$($owner1.id), supplier=$($supplier1.id), customer=$($customer1.id), product=$($product1.id)"
} catch {
  Add-Result 'seed-lookups' 'FAIL' $_.Exception.Message
}

$crudWarehouse = $null
$crudLocation = $null
$crudOwner = $null
$crudSupplier = $null
$crudCustomer = $null
$crudProduct = $null
$stationWarehouse = $null
$stationLocation = $null
$txProduct = $null
$inboundGeneral = $null
$outboundGeneral = $null
$outboundPda = $null
$inboundPickup = $null
$pickupAlert = $null
$pickupNotification = $null
$manualException = $null
$approvalException = $null
$approvalReject = $null
$qaRole = $null
$qaMenu = $null
$qaUser = $null
$qaLogin = $null
$billingContract = $null
$billingRule = $null
$billingStatement = $null
$stockTake = $null
$stockTakeItem = $null

if ($warehouse1 -and $location1 -and $owner1 -and $supplier1 -and $customer1 -and $product1) {
  try {
    $crudWarehouse = (Send-Api 'POST' '/warehouses' @{
      warehouseCode = "QA-CRUD-WH-$ts"
      warehouseName = "QA CRUD Warehouse $ts"
      warehouseType = 'GENERAL'
      sceneType = 'GENERAL_STORAGE'
      contactName = 'QA'
      contactPhone = '13900001001'
      province = 'Shanghai'
      city = 'Shanghai'
      district = 'Pudong'
      address = 'QA CRUD Address'
      dwellAlertMinutes = 0
      smsNotifyEnabled = $false
      smsReminderIntervalMinutes = 120
      autoAssignLocation = $true
      scanMode = 'QR_CODE'
      status = 'ACTIVE'
      remark = 'QA CRUD warehouse'
    }).data
    Add-Result 'warehouse-create' 'PASS' "id=$($crudWarehouse.id)"
  } catch {
    Add-Result 'warehouse-create' 'FAIL' $_.Exception.Message
  }

  if ($crudWarehouse) {
    try {
      $crudLocation = (Send-Api 'POST' '/locations' @{
        warehouseId = $crudWarehouse.id
        zoneName = 'QA-ZONE'
        locationCode = "QA-CRUD-LOC-$ts"
        locationName = "QA CRUD Location $ts"
        aisleNo = 'Q'
        shelfNo = '01'
        layerNo = '01'
        binNo = '01'
        capacityQty = 100
        usedQty = 0
        pickable = $true
        status = 'ACTIVE'
        remark = 'QA CRUD location'
      }).data
      Add-Result 'location-create' 'PASS' "id=$($crudLocation.id)"
    } catch {
      Add-Result 'location-create' 'FAIL' $_.Exception.Message
    }

    try {
      $crudWarehouse = (Send-Api 'PUT' "/warehouses/$($crudWarehouse.id)" @{
        id = $crudWarehouse.id
        warehouseCode = $crudWarehouse.warehouseCode
        warehouseName = "QA CRUD Warehouse Updated $ts"
        warehouseType = $crudWarehouse.warehouseType
        sceneType = $crudWarehouse.sceneType
        contactName = $crudWarehouse.contactName
        contactPhone = $crudWarehouse.contactPhone
        province = $crudWarehouse.province
        city = $crudWarehouse.city
        district = $crudWarehouse.district
        address = $crudWarehouse.address
        dwellAlertMinutes = $crudWarehouse.dwellAlertMinutes
        smsNotifyEnabled = $crudWarehouse.smsNotifyEnabled
        smsReminderIntervalMinutes = $crudWarehouse.smsReminderIntervalMinutes
        autoAssignLocation = $crudWarehouse.autoAssignLocation
        scanMode = $crudWarehouse.scanMode
        status = $crudWarehouse.status
        remark = 'QA CRUD warehouse updated'
      }).data
      Add-Result 'warehouse-update' 'PASS' $crudWarehouse.warehouseName
    } catch {
      Add-Result 'warehouse-update' 'FAIL' $_.Exception.Message
    }
  }

  if ($crudLocation) {
    try {
      $crudLocation = (Send-Api 'PUT' "/locations/$($crudLocation.id)" @{
        id = $crudLocation.id
        warehouseId = $crudLocation.warehouseId
        zoneName = $crudLocation.zoneName
        locationCode = $crudLocation.locationCode
        locationName = "QA CRUD Location Updated $ts"
        aisleNo = $crudLocation.aisleNo
        shelfNo = $crudLocation.shelfNo
        layerNo = $crudLocation.layerNo
        binNo = $crudLocation.binNo
        capacityQty = $crudLocation.capacityQty
        usedQty = $crudLocation.usedQty
        pickable = $crudLocation.pickable
        status = $crudLocation.status
        remark = 'QA CRUD location updated'
      }).data
      Add-Result 'location-update' 'PASS' $crudLocation.locationName
    } catch {
      Add-Result 'location-update' 'FAIL' $_.Exception.Message
    }
  }

  try {
    $crudOwner = (Send-Api 'POST' '/owners' @{
      ownerCode = "QA-OWN-$ts"
      ownerName = "QA Owner $ts"
      ownerType = 'SELF'
      contactName = 'QA Owner'
      contactPhone = '13900001002'
      email = 'qa-owner@example.com'
      address = 'QA Owner Address'
      status = 'ACTIVE'
      remark = 'QA owner'
    }).data
    Add-Result 'owner-create' 'PASS' "id=$($crudOwner.id)"
  } catch {
    Add-Result 'owner-create' 'FAIL' $_.Exception.Message
  }

  if ($crudOwner) {
    try {
      $crudOwner = (Send-Api 'PUT' "/owners/$($crudOwner.id)" @{
        id = $crudOwner.id
        ownerCode = $crudOwner.ownerCode
        ownerName = "QA Owner Updated $ts"
        ownerType = $crudOwner.ownerType
        contactName = $crudOwner.contactName
        contactPhone = $crudOwner.contactPhone
        email = $crudOwner.email
        address = $crudOwner.address
        status = $crudOwner.status
        remark = 'QA owner updated'
      }).data
      Add-Result 'owner-update' 'PASS' $crudOwner.ownerName
    } catch {
      Add-Result 'owner-update' 'FAIL' $_.Exception.Message
    }
    try {
      $null = Send-Api 'DELETE' "/owners/$($crudOwner.id)"
      Add-Result 'owner-delete' 'PASS' "id=$($crudOwner.id)"
    } catch {
      Add-Result 'owner-delete' 'FAIL' $_.Exception.Message
    }
  }

  try {
    $crudSupplier = (Send-Api 'POST' '/suppliers' @{
      supplierCode = "QA-SUP-$ts"
      supplierName = "QA Supplier $ts"
      contactName = 'QA Supplier'
      contactPhone = '13900001003'
      email = 'qa-supplier@example.com'
      address = 'QA Supplier Address'
      status = 'ACTIVE'
      remark = 'QA supplier'
    }).data
    Add-Result 'supplier-create' 'PASS' "id=$($crudSupplier.id)"
  } catch {
    Add-Result 'supplier-create' 'FAIL' $_.Exception.Message
  }

  if ($crudSupplier) {
    try {
      $crudSupplier = (Send-Api 'PUT' "/suppliers/$($crudSupplier.id)" @{
        id = $crudSupplier.id
        supplierCode = $crudSupplier.supplierCode
        supplierName = "QA Supplier Updated $ts"
        contactName = $crudSupplier.contactName
        contactPhone = $crudSupplier.contactPhone
        email = $crudSupplier.email
        address = $crudSupplier.address
        status = $crudSupplier.status
        remark = 'QA supplier updated'
      }).data
      Add-Result 'supplier-update' 'PASS' $crudSupplier.supplierName
    } catch {
      Add-Result 'supplier-update' 'FAIL' $_.Exception.Message
    }
    try {
      $null = Send-Api 'DELETE' "/suppliers/$($crudSupplier.id)"
      Add-Result 'supplier-delete' 'PASS' "id=$($crudSupplier.id)"
    } catch {
      Add-Result 'supplier-delete' 'FAIL' $_.Exception.Message
    }
  }

  try {
    $crudCustomer = (Send-Api 'POST' '/customers' @{
      customerCode = "QA-CUS-$ts"
      customerName = "QA Customer $ts"
      customerType = 'B2B'
      contactName = 'QA Customer'
      contactPhone = '13900001004'
      email = 'qa-customer@example.com'
      address = 'QA Customer Address'
      status = 'ACTIVE'
      remark = 'QA customer'
    }).data
    Add-Result 'customer-create' 'PASS' "id=$($crudCustomer.id)"
  } catch {
    Add-Result 'customer-create' 'FAIL' $_.Exception.Message
  }

  if ($crudCustomer) {
    try {
      $crudCustomer = (Send-Api 'PUT' "/customers/$($crudCustomer.id)" @{
        id = $crudCustomer.id
        customerCode = $crudCustomer.customerCode
        customerName = "QA Customer Updated $ts"
        customerType = $crudCustomer.customerType
        contactName = $crudCustomer.contactName
        contactPhone = $crudCustomer.contactPhone
        email = $crudCustomer.email
        address = $crudCustomer.address
        status = $crudCustomer.status
        remark = 'QA customer updated'
      }).data
      Add-Result 'customer-update' 'PASS' $crudCustomer.customerName
    } catch {
      Add-Result 'customer-update' 'FAIL' $_.Exception.Message
    }
    try {
      $null = Send-Api 'DELETE' "/customers/$($crudCustomer.id)"
      Add-Result 'customer-delete' 'PASS' "id=$($crudCustomer.id)"
    } catch {
      Add-Result 'customer-delete' 'FAIL' $_.Exception.Message
    }
  }

  try {
    $crudProduct = (Send-Api 'POST' '/products' @{
      skuCode = "QA-SKU-CRUD-$ts"
      productName = "QA CRUD Product $ts"
      productSpec = '1/box'
      categoryName = 'QA'
      brandName = 'QA'
      unitName = 'BOX'
      barcode = "7900$ts"
      safeStock = 0
      maxStock = 1000
      shelfLifeDays = 365
      enableBatch = $true
      enableSerial = $false
      weightKg = 1
      volumeM3 = 0.01
      salePrice = 10
      status = 'ACTIVE'
      remark = 'QA CRUD product'
    }).data
    Add-Result 'product-create' 'PASS' "id=$($crudProduct.id)"
  } catch {
    Add-Result 'product-create' 'FAIL' $_.Exception.Message
  }

  if ($crudProduct) {
    try {
      $crudProduct = (Send-Api 'PUT' "/products/$($crudProduct.id)" @{
        id = $crudProduct.id
        skuCode = $crudProduct.skuCode
        productName = "QA CRUD Product Updated $ts"
        productSpec = $crudProduct.productSpec
        categoryName = $crudProduct.categoryName
        brandName = $crudProduct.brandName
        unitName = $crudProduct.unitName
        barcode = $crudProduct.barcode
        safeStock = $crudProduct.safeStock
        maxStock = $crudProduct.maxStock
        shelfLifeDays = $crudProduct.shelfLifeDays
        enableBatch = $crudProduct.enableBatch
        enableSerial = $crudProduct.enableSerial
        weightKg = $crudProduct.weightKg
        volumeM3 = $crudProduct.volumeM3
        salePrice = $crudProduct.salePrice
        status = $crudProduct.status
        remark = 'QA CRUD product updated'
      }).data
      Add-Result 'product-update' 'PASS' $crudProduct.productName
    } catch {
      Add-Result 'product-update' 'FAIL' $_.Exception.Message
    }
    try {
      $null = Send-Api 'DELETE' "/products/$($crudProduct.id)"
      Add-Result 'product-delete' 'PASS' "id=$($crudProduct.id)"
    } catch {
      Add-Result 'product-delete' 'FAIL' $_.Exception.Message
    }
  }

  if ($crudLocation) {
    try {
      $null = Send-Api 'DELETE' "/locations/$($crudLocation.id)"
      Add-Result 'location-delete' 'PASS' "id=$($crudLocation.id)"
    } catch {
      Add-Result 'location-delete' 'FAIL' $_.Exception.Message
    }
  }

  if ($crudWarehouse) {
    try {
      $null = Send-Api 'DELETE' "/warehouses/$($crudWarehouse.id)"
      Add-Result 'warehouse-delete' 'PASS' "id=$($crudWarehouse.id)"
    } catch {
      Add-Result 'warehouse-delete' 'FAIL' $_.Exception.Message
    }
  }

  try {
    $stationWarehouse = (Send-Api 'POST' '/warehouses' @{
      warehouseCode = "QA-STN-WH-$ts"
      warehouseName = "QA Station Warehouse $ts"
      warehouseType = 'GENERAL'
      sceneType = 'PARCEL_STATION'
      contactName = 'QA Station'
      contactPhone = '13900001011'
      province = 'Shanghai'
      city = 'Shanghai'
      district = 'Pudong'
      address = 'QA Station Address'
      dwellAlertMinutes = 1
      smsNotifyEnabled = $true
      smsReminderIntervalMinutes = 15
      autoAssignLocation = $true
      scanMode = 'QR_CODE'
      status = 'ACTIVE'
      remark = 'QA station warehouse'
    }).data
    Add-Result 'station-warehouse-create' 'PASS' "id=$($stationWarehouse.id)"
  } catch {
    Add-Result 'station-warehouse-create' 'FAIL' $_.Exception.Message
  }

  if ($stationWarehouse) {
    try {
      $stationLocation = (Send-Api 'POST' '/locations' @{
        warehouseId = $stationWarehouse.id
        zoneName = 'PICKUP'
        locationCode = "QA-STN-LOC-$ts"
        locationName = "QA Station Location $ts"
        aisleNo = 'S'
        shelfNo = '01'
        layerNo = '01'
        binNo = '01'
        capacityQty = 100
        usedQty = 0
        pickable = $true
        status = 'ACTIVE'
        remark = 'QA station location'
      }).data
      Add-Result 'station-location-create' 'PASS' "id=$($stationLocation.id)"
    } catch {
      Add-Result 'station-location-create' 'FAIL' $_.Exception.Message
    }
  }

  try {
    $txProduct = (Send-Api 'POST' '/products' @{
      skuCode = "QA-SKU-TX-$ts"
      productName = "QA Transaction Product $ts"
      productSpec = '1/box'
      categoryName = 'QA'
      brandName = 'QA'
      unitName = 'BOX'
      barcode = "8800$ts"
      safeStock = 0
      maxStock = 1000
      shelfLifeDays = 365
      enableBatch = $true
      enableSerial = $false
      weightKg = 1
      volumeM3 = 0.01
      salePrice = 12
      status = 'ACTIVE'
      remark = 'QA transaction product'
    }).data
    Add-Result 'tx-product-create' 'PASS' "id=$($txProduct.id)"
  } catch {
    Add-Result 'tx-product-create' 'FAIL' $_.Exception.Message
  }

  if ($txProduct) {
    try {
      $txProduct = (Send-Api 'PUT' "/products/$($txProduct.id)" @{
        id = $txProduct.id
        skuCode = $txProduct.skuCode
        productName = "QA Transaction Product Updated $ts"
        productSpec = $txProduct.productSpec
        categoryName = $txProduct.categoryName
        brandName = $txProduct.brandName
        unitName = $txProduct.unitName
        barcode = $txProduct.barcode
        safeStock = $txProduct.safeStock
        maxStock = $txProduct.maxStock
        shelfLifeDays = $txProduct.shelfLifeDays
        enableBatch = $txProduct.enableBatch
        enableSerial = $txProduct.enableSerial
        weightKg = $txProduct.weightKg
        volumeM3 = $txProduct.volumeM3
        salePrice = 15
        status = $txProduct.status
        remark = 'QA transaction product updated'
      }).data
      Add-Result 'tx-product-update' 'PASS' $txProduct.productName
    } catch {
      Add-Result 'tx-product-update' 'FAIL' $_.Exception.Message
    }
  }

  if ($txProduct) {
    try {
      $inboundGeneral = (Send-Api 'POST' '/inbounds' @{
        orderNo = "QA-IN-$ts"
        warehouseId = $warehouse1.id
        supplierId = $supplier1.id
        ownerId = $owner1.id
        orderType = 'PURCHASE'
        sourceNo = "SRC-IN-$ts"
        expectedArrivalTime = $nowStr
        operatorName = 'QA Bot'
        remark = 'QA inbound general'
        items = @(@{
          productId = $txProduct.id
          batchNo = "BATCH-IN-$ts"
          expectedQty = 8
          actualQty = 8
          qualifiedQty = 8
          locationId = $location1.id
          productionDate = $now.ToString('yyyy-MM-dd')
          expiryDate = $now.AddDays(30).ToString('yyyy-MM-dd')
          remark = 'QA inbound item'
        })
      }).data
      Add-Result 'inbound-create' 'PASS' "id=$($inboundGeneral.id), orderNo=$($inboundGeneral.orderNo)"
    } catch {
      Add-Result 'inbound-create' 'FAIL' $_.Exception.Message
    }
  }

  if ($inboundGeneral) {
    try {
      $inboundGeneral = (Send-Api 'POST' "/inbounds/$($inboundGeneral.id)/receive").data
      Add-Result 'inbound-receive' 'PASS' $inboundGeneral.status
    } catch {
      Add-Result 'inbound-receive' 'FAIL' $_.Exception.Message
    }
    try {
      $inboundGeneral = (Send-Api 'POST' "/inbounds/$($inboundGeneral.id)/putaway").data
      Add-Result 'inbound-putaway' 'PASS' $inboundGeneral.status
    } catch {
      Add-Result 'inbound-putaway' 'FAIL' $_.Exception.Message
    }
    try {
      $stocksAfterInbound = (Get-Api ("/stocks?warehouseId={0}&keyword={1}" -f $warehouse1.id, [uri]::EscapeDataString($txProduct.skuCode))).data
      $qtyAfterInbound = Qty-Sum $stocksAfterInbound
      Add-Result 'stock-after-inbound' 'PASS' "qty=$qtyAfterInbound"
    } catch {
      Add-Result 'stock-after-inbound' 'FAIL' $_.Exception.Message
    }
  }

  if ($txProduct) {
    try {
      $outboundGeneral = (Send-Api 'POST' '/outbounds' @{
        orderNo = "QA-OUT-$ts"
        warehouseId = $warehouse1.id
        customerId = $customer1.id
        ownerId = $owner1.id
        orderType = 'SALES'
        sourceNo = "SRC-OUT-$ts"
        priorityLevel = 'NORMAL'
        plannedShipTime = $futureStr
        operatorName = 'QA Bot'
        logisticsNo = "QA-LOG-NORMAL-$ts"
        remark = 'QA outbound general'
        items = @(@{
          productId = $txProduct.id
          batchNo = "BATCH-IN-$ts"
          plannedQty = 3
          shippedQty = 0
          locationId = $location1.id
          remark = 'QA outbound item'
        })
      }).data
      Add-Result 'outbound-create' 'PASS' "id=$($outboundGeneral.id), orderNo=$($outboundGeneral.orderNo)"
    } catch {
      Add-Result 'outbound-create' 'FAIL' $_.Exception.Message
    }
  }

  if ($outboundGeneral) {
    try {
      $outboundGeneral = (Send-Api 'POST' "/outbounds/$($outboundGeneral.id)/picking").data
      Add-Result 'outbound-picking' 'PASS' $outboundGeneral.status
    } catch {
      Add-Result 'outbound-picking' 'FAIL' $_.Exception.Message
    }
    try {
      $outboundGeneral = (Send-Api 'POST' "/outbounds/$($outboundGeneral.id)/ship").data
      Add-Result 'outbound-ship' 'PASS' $outboundGeneral.status
    } catch {
      Add-Result 'outbound-ship' 'FAIL' $_.Exception.Message
    }
    try {
      $stocksAfterOutbound = (Get-Api ("/stocks?warehouseId={0}&keyword={1}" -f $warehouse1.id, [uri]::EscapeDataString($txProduct.skuCode))).data
      $qtyAfterOutbound = Qty-Sum $stocksAfterOutbound
      Add-Result 'stock-after-outbound' 'PASS' "qty=$qtyAfterOutbound"
    } catch {
      Add-Result 'stock-after-outbound' 'FAIL' $_.Exception.Message
    }
  }

  if ($txProduct) {
    try {
      $outboundPda = (Send-Api 'POST' '/outbounds' @{
        orderNo = "QA-OUT-PDA-$ts"
        warehouseId = $warehouse1.id
        customerId = $customer1.id
        ownerId = $owner1.id
        orderType = 'SALES'
        sourceNo = "SRC-OUT-PDA-$ts"
        priorityLevel = 'HIGH'
        plannedShipTime = $futureStr
        operatorName = 'QA Bot'
        logisticsNo = "QA-LOG-PDA-$ts"
        remark = 'QA outbound PDA'
        items = @(@{
          productId = $txProduct.id
          batchNo = "BATCH-IN-$ts"
          plannedQty = 2
          shippedQty = 0
          locationId = $location1.id
          remark = 'QA outbound PDA item'
        })
      }).data
      Add-Result 'pda-outbound-create' 'PASS' "id=$($outboundPda.id)"
    } catch {
      Add-Result 'pda-outbound-create' 'FAIL' $_.Exception.Message
    }
  }

  if ($outboundPda) {
    try {
      $scanOutbound = (Get-Api ("/scan/lookup?code={0}" -f [uri]::EscapeDataString($outboundPda.logisticsNo))).data
      Add-Result 'pda-outbound-scan' 'PASS' "$($scanOutbound.entityType):$($scanOutbound.orderNo)"
    } catch {
      Add-Result 'pda-outbound-scan' 'FAIL' $_.Exception.Message
    }
    try {
      $outboundPda = (Send-Api 'POST' "/outbounds/$($outboundPda.id)/ship").data
      Add-Result 'pda-outbound-ship' 'PASS' $outboundPda.status
    } catch {
      Add-Result 'pda-outbound-ship' 'FAIL' $_.Exception.Message
    }
    try {
      $stocksAfterPdaOutbound = (Get-Api ("/stocks?warehouseId={0}&keyword={1}" -f $warehouse1.id, [uri]::EscapeDataString($txProduct.skuCode))).data
      $qtyAfterPdaOutbound = Qty-Sum $stocksAfterPdaOutbound
      Add-Result 'stock-after-pda-outbound' 'PASS' "qty=$qtyAfterPdaOutbound"
    } catch {
      Add-Result 'stock-after-pda-outbound' 'FAIL' $_.Exception.Message
    }
  }

  if ($stationWarehouse -and $stationLocation) {
    try {
      $inboundPickup = (Send-Api 'POST' '/inbounds' @{
        orderNo = "QA-PICK-IN-$ts"
        warehouseId = $stationWarehouse.id
        supplierId = $supplier1.id
        ownerId = $owner1.id
        customerId = $customer1.id
        orderType = 'PURCHASE'
        sourceNo = "SRC-PICK-$ts"
        expectedArrivalTime = $nowStr
        operatorName = 'QA Bot'
        receiverName = 'QA Receiver'
        receiverPhone = '13900009999'
        pickupCode = "QA-PICK-$ts"
        scanCode = "QA-SCAN-$ts"
        remark = 'QA pickup inbound'
        items = @(@{
          productId = $product1.id
          batchNo = "PICK-BATCH-$ts"
          expectedQty = 2
          actualQty = 2
          qualifiedQty = 2
          locationId = $stationLocation.id
          productionDate = $now.ToString('yyyy-MM-dd')
          expiryDate = $now.AddDays(10).ToString('yyyy-MM-dd')
          remark = 'QA pickup item'
        })
      }).data
      Add-Result 'pickup-inbound-create' 'PASS' "id=$($inboundPickup.id)"
    } catch {
      Add-Result 'pickup-inbound-create' 'FAIL' $_.Exception.Message
    }
  }

  if ($inboundPickup) {
    try {
      $inboundPickup = (Send-Api 'POST' "/inbounds/$($inboundPickup.id)/receive").data
      Add-Result 'pickup-inbound-receive' 'PASS' $inboundPickup.status
    } catch {
      Add-Result 'pickup-inbound-receive' 'FAIL' $_.Exception.Message
    }
    try {
      $inboundPickup = (Send-Api 'POST' "/inbounds/$($inboundPickup.id)/putaway").data
      Add-Result 'pickup-inbound-putaway' 'PASS' "$($inboundPickup.status), pickup=$($inboundPickup.pickupStatus)"
    } catch {
      Add-Result 'pickup-inbound-putaway' 'FAIL' $_.Exception.Message
    }
    try {
      $scanInbound = (Get-Api ("/scan/lookup?code={0}" -f [uri]::EscapeDataString($inboundPickup.pickupCode))).data
      Add-Result 'pda-pickup-scan' 'PASS' "$($scanInbound.entityType):$($scanInbound.pickupStatus)"
    } catch {
      Add-Result 'pda-pickup-scan' 'FAIL' $_.Exception.Message
    }

    Start-Sleep -Seconds 75

    try {
      $null = Send-Api 'POST' '/alerts/scan'
      Add-Result 'alerts-scan' 'PASS' 'manual scan triggered'
    } catch {
      Add-Result 'alerts-scan' 'FAIL' $_.Exception.Message
    }

    try {
      $pickupAlert = (Get-Api '/alerts').data | Where-Object { $_.bizId -eq $inboundPickup.id -and $_.status -eq 'OPEN' } | Select-Object -First 1
      if ($pickupAlert) {
        Add-Result 'pickup-alert-open' 'PASS' "alertId=$($pickupAlert.id)"
      } else {
        Add-Result 'pickup-alert-open' 'FAIL' 'No OPEN alert found for pickup inbound order'
      }
    } catch {
      Add-Result 'pickup-alert-open' 'FAIL' $_.Exception.Message
    }

    try {
      $pickupNotification = (Get-Api '/notifications').data | Where-Object { $_.bizId -eq $inboundPickup.id } | Select-Object -First 1
      if ($pickupNotification) {
        Add-Result 'pickup-notification-queued' 'PASS' "messageId=$($pickupNotification.id), status=$($pickupNotification.sendStatus)"
      } else {
        Add-Result 'pickup-notification-queued' 'FAIL' 'No notification queued for pickup inbound order'
      }
    } catch {
      Add-Result 'pickup-notification-queued' 'FAIL' $_.Exception.Message
    }

    if ($pickupAlert) {
      try {
        $pickupAlert = (Send-Api 'POST' ("/alerts/{0}/ack?operatorName={1}" -f $pickupAlert.id, [uri]::EscapeDataString('QA Bot'))).data
        Add-Result 'alert-ack' 'PASS' $pickupAlert.status
      } catch {
        Add-Result 'alert-ack' 'FAIL' $_.Exception.Message
      }
    }

    try {
      $pickupAction = (Send-Api 'POST' "/inbounds/$($inboundPickup.id)/pickup-action" @{
        action = 'PICKED_UP'
        operatorName = 'QA PDA'
        remark = 'QA picked up'
      }).data
      Add-Result 'pickup-action-picked-up' 'PASS' $pickupAction.pickupStatus
    } catch {
      Add-Result 'pickup-action-picked-up' 'FAIL' $_.Exception.Message
    }
  }

  try {
    $manualException = (Send-Api 'POST' '/exceptions' @{
      warehouseId = $warehouse1.id
      bizType = 'MANUAL'
      bizNo = "QA-EX-$ts"
      ticketTitle = "QA Exception $ts"
      ticketContent = 'QA exception content'
      severity = 'HIGH'
      reporterName = 'QA Bot'
    }).data
    Add-Result 'exception-create' 'PASS' "id=$($manualException.id), ticketNo=$($manualException.ticketNo)"
  } catch {
    Add-Result 'exception-create' 'FAIL' $_.Exception.Message
  }

  if ($manualException) {
    try {
      $manualException = (Send-Api 'POST' ("/exceptions/{0}/assign?assigneeName={1}" -f $manualException.id, [uri]::EscapeDataString('QA Assignee'))).data
      Add-Result 'exception-assign' 'PASS' "$($manualException.status):$($manualException.assigneeName)"
    } catch {
      Add-Result 'exception-assign' 'FAIL' $_.Exception.Message
    }

    try {
      $approvalException = (Send-Api 'POST' '/approvals/submit' @{
        approvalType = 'EXCEPTION_APPROVAL'
        bizType = 'EXCEPTION_TICKET'
        bizId = $manualException.id
        bizNo = $manualException.ticketNo
        applicantName = 'QA Bot'
        approverName = 'QA Manager'
        applyReason = 'QA exception approval'
      }).data
      Add-Result 'approval-submit-exception' 'PASS' "approvalId=$($approvalException.id)"
    } catch {
      Add-Result 'approval-submit-exception' 'FAIL' $_.Exception.Message
    }

    if ($approvalException) {
      try {
        $approvalException = (Send-Api 'POST' "/approvals/$($approvalException.id)/approve" @{
          operatorName = 'QA Approver'
          comment = 'approved in QA'
        }).data
        Add-Result 'approval-approve' 'PASS' $approvalException.status
      } catch {
        Add-Result 'approval-approve' 'FAIL' $_.Exception.Message
      }
    }

    try {
      $exceptionApproved = (Get-Api '/exceptions').data | Where-Object { $_.id -eq $manualException.id } | Select-Object -First 1
      if ($exceptionApproved -and $exceptionApproved.approvalStatus -eq 'APPROVED') {
        Add-Result 'exception-approval-status-linked' 'PASS' $exceptionApproved.approvalStatus
      } else {
        Add-Result 'exception-approval-status-linked' 'FAIL' 'Exception approvalStatus did not become APPROVED'
      }
    } catch {
      Add-Result 'exception-approval-status-linked' 'FAIL' $_.Exception.Message
    }

    try {
      $manualException = (Send-Api 'POST' ("/exceptions/{0}/resolve?operatorName={1}" -f $manualException.id, [uri]::EscapeDataString('QA Resolver'))).data
      Add-Result 'exception-resolve' 'PASS' $manualException.status
    } catch {
      Add-Result 'exception-resolve' 'FAIL' $_.Exception.Message
    }
    try {
      $manualException = (Send-Api 'POST' "/exceptions/$($manualException.id)/close").data
      Add-Result 'exception-close' 'PASS' $manualException.status
    } catch {
      Add-Result 'exception-close' 'FAIL' $_.Exception.Message
    }
  }

  if ($outboundPda) {
    try {
      $approvalReject = (Send-Api 'POST' '/approvals/submit' @{
        approvalType = 'ORDER_APPROVAL'
        bizType = 'OUTBOUND_ORDER'
        bizId = $outboundPda.id
        bizNo = $outboundPda.orderNo
        applicantName = 'QA Bot'
        approverName = 'QA Manager'
        applyReason = 'QA reject approval flow'
      }).data
      Add-Result 'approval-submit-reject-case' 'PASS' "approvalId=$($approvalReject.id)"
    } catch {
      Add-Result 'approval-submit-reject-case' 'FAIL' $_.Exception.Message
    }

    if ($approvalReject) {
      try {
        $approvalReject = (Send-Api 'POST' "/approvals/$($approvalReject.id)/reject" @{
          operatorName = 'QA Approver'
          comment = 'rejected in QA'
        }).data
        Add-Result 'approval-reject' 'PASS' $approvalReject.status
      } catch {
        Add-Result 'approval-reject' 'FAIL' $_.Exception.Message
      }
    }
  }

  try {
    $qaRole = (Send-Api 'POST' '/permissions/roles' @{
      roleCode = "QA_ROLE_$ts"
      roleName = "QA Role $ts"
      roleDesc = 'QA role'
      status = 'ACTIVE'
    }).data
    Add-Result 'role-create' 'PASS' "id=$($qaRole.id)"
  } catch {
    Add-Result 'role-create' 'FAIL' $_.Exception.Message
  }

  if ($qaRole) {
    try {
      $qaRole = (Send-Api 'PUT' "/permissions/roles/$($qaRole.id)" @{
        id = $qaRole.id
        roleCode = $qaRole.roleCode
        roleName = "QA Role Updated $ts"
        roleDesc = 'QA role updated'
        status = 'ACTIVE'
      }).data
      Add-Result 'role-update' 'PASS' $qaRole.roleName
    } catch {
      Add-Result 'role-update' 'FAIL' $_.Exception.Message
    }
  }

  try {
    $qaMenu = (Send-Api 'POST' '/permissions/menus' @{
      menuCode = "QA_MENU_$ts"
      menuName = "QA Menu $ts"
      parentId = $null
      menuPath = "/qa/$ts"
      componentName = 'QaPage'
      iconName = 'Box'
      menuType = 'MENU'
      permissionCode = "qa:$ts"
      sortNo = 999
      visible = $true
      status = 'ACTIVE'
    }).data
    Add-Result 'menu-create' 'PASS' "id=$($qaMenu.id)"
  } catch {
    Add-Result 'menu-create' 'FAIL' $_.Exception.Message
  }

  if ($qaMenu) {
    try {
      $qaMenu = (Send-Api 'PUT' "/permissions/menus/$($qaMenu.id)" @{
        id = $qaMenu.id
        menuCode = $qaMenu.menuCode
        menuName = "QA Menu Updated $ts"
        parentId = $qaMenu.parentId
        menuPath = $qaMenu.menuPath
        componentName = $qaMenu.componentName
        iconName = $qaMenu.iconName
        menuType = $qaMenu.menuType
        permissionCode = $qaMenu.permissionCode
        sortNo = $qaMenu.sortNo
        visible = $qaMenu.visible
        status = $qaMenu.status
      }).data
      Add-Result 'menu-update' 'PASS' $qaMenu.menuName
    } catch {
      Add-Result 'menu-update' 'FAIL' $_.Exception.Message
    }
  }

  if ($qaRole -and $qaMenu) {
    try {
      $null = (Send-Api 'POST' "/permissions/roles/$($qaRole.id)/menus" @{ menuIds = @(1, $qaMenu.id) }).data
      Add-Result 'role-menu-assign' 'PASS' "roleId=$($qaRole.id), menuIds=1,$($qaMenu.id)"
    } catch {
      Add-Result 'role-menu-assign' 'FAIL' $_.Exception.Message
    }
  }

  if ($qaRole) {
    try {
      $qaUser = (Send-Api 'POST' '/permissions/users' @{
        username = "qa_user_$ts"
        password = 'qa123456'
        displayName = "QA User $ts"
        phone = '13900001012'
        email = 'qa-user@example.com'
        roleCode = $qaRole.roleCode
        status = 'ACTIVE'
        remark = 'QA user'
      }).data
      Add-Result 'user-create' 'PASS' "id=$($qaUser.id)"
    } catch {
      Add-Result 'user-create' 'FAIL' $_.Exception.Message
    }
  }

  if ($qaUser) {
    try {
      $qaUser = (Send-Api 'PUT' "/permissions/users/$($qaUser.id)" @{
        id = $qaUser.id
        username = $qaUser.username
        password = ''
        displayName = "QA User Updated $ts"
        phone = $qaUser.phone
        email = $qaUser.email
        roleCode = $qaUser.roleCode
        status = 'ACTIVE'
        remark = 'QA user updated'
      }).data
      Add-Result 'user-update' 'PASS' $qaUser.displayName
    } catch {
      Add-Result 'user-update' 'FAIL' $_.Exception.Message
    }
    try {
      $qaLogin = (Send-Api 'POST' '/auth/login' @{ username = $qaUser.username; password = 'qa123456' }).data
      if ($qaLogin.menuPaths -contains '/dashboard' -and $qaLogin.menuPaths -contains "/qa/$ts") {
        Add-Result 'user-login-with-role-menus' 'PASS' ((@($qaLogin.menuPaths)) -join ',')
      } else {
        Add-Result 'user-login-with-role-menus' 'FAIL' 'Expected menu paths were not returned after login'
      }
    } catch {
      Add-Result 'user-login-with-role-menus' 'FAIL' $_.Exception.Message
    }
  }

  try {
    $billingContract = (Send-Api 'POST' '/billing/contracts' @{
      customerId = $customer1.id
      ownerId = $owner1.id
      warehouseId = $warehouse1.id
      contractNo = "QA-BC-$ts"
      contractName = "QA Billing Contract $ts"
      effectiveDate = $now.ToString('yyyy-MM-01')
      expireDate = $now.AddMonths(1).ToString('yyyy-MM-dd')
      settlementCycle = 'MONTHLY'
      status = 'ACTIVE'
    }).data
    Add-Result 'billing-contract-create' 'PASS' "id=$($billingContract.id)"
  } catch {
    Add-Result 'billing-contract-create' 'FAIL' $_.Exception.Message
  }

  if ($billingContract) {
    try {
      $billingContract = (Send-Api 'PUT' "/billing/contracts/$($billingContract.id)" @{
        id = $billingContract.id
        customerId = $billingContract.customerId
        ownerId = $billingContract.ownerId
        warehouseId = $billingContract.warehouseId
        contractNo = $billingContract.contractNo
        contractName = "QA Billing Contract Updated $ts"
        effectiveDate = $billingContract.effectiveDate
        expireDate = $billingContract.expireDate
        settlementCycle = $billingContract.settlementCycle
        status = $billingContract.status
      }).data
      Add-Result 'billing-contract-update' 'PASS' $billingContract.contractName
    } catch {
      Add-Result 'billing-contract-update' 'FAIL' $_.Exception.Message
    }
    try {
      $billingRule = (Send-Api 'POST' '/billing/rules' @{
        contractId = $billingContract.id
        chargeType = 'INBOUND_QTY'
        ruleName = "QA Inbound Qty Rule $ts"
        unitName = 'BOX'
        unitPrice = 2.5
        status = 'ACTIVE'
      }).data
      Add-Result 'billing-rule-create' 'PASS' "id=$($billingRule.id)"
    } catch {
      Add-Result 'billing-rule-create' 'FAIL' $_.Exception.Message
    }
  }

  if ($billingRule) {
    try {
      $billingRule = (Send-Api 'PUT' "/billing/rules/$($billingRule.id)" @{
        id = $billingRule.id
        contractId = $billingRule.contractId
        chargeType = $billingRule.chargeType
        ruleName = $billingRule.ruleName
        unitName = $billingRule.unitName
        unitPrice = 3.0
        status = $billingRule.status
      }).data
      Add-Result 'billing-rule-update' 'PASS' "unitPrice=$($billingRule.unitPrice)"
    } catch {
      Add-Result 'billing-rule-update' 'FAIL' $_.Exception.Message
    }
  }

  if ($billingContract) {
    try {
      $billingStatement = (Send-Api 'POST' '/billing/statements/generate' @{
        contractId = $billingContract.id
        statementMonth = $month
      }).data
      if ([decimal]$billingStatement.totalAmount -gt 0) {
        Add-Result 'billing-generate-statement' 'PASS' "statementId=$($billingStatement.id), total=$($billingStatement.totalAmount)"
      } else {
        Add-Result 'billing-generate-statement' 'FAIL' 'Generated statement totalAmount is not greater than 0'
      }
    } catch {
      Add-Result 'billing-generate-statement' 'FAIL' $_.Exception.Message
    }
  }

  if ($billingStatement) {
    try {
      $billingStatement = (Send-Api 'POST' "/billing/statements/$($billingStatement.id)/pay").data
      Add-Result 'billing-pay-statement' 'PASS' $billingStatement.statementStatus
    } catch {
      Add-Result 'billing-pay-statement' 'FAIL' $_.Exception.Message
    }
  }

  try {
    $stockTake = (Send-Api 'POST' '/stock-takes' @{
      warehouseId = $warehouse1.id
      ownerId = $owner1.id
      takeType = 'CYCLE'
      plannedStartTime = $nowStr
      plannedEndTime = $futureStr
      operatorName = 'QA Bot'
      remark = 'QA stock take'
    }).data
    Add-Result 'stocktake-create' 'PASS' "id=$($stockTake.id), items=$(@($stockTake.items).Count)"
  } catch {
    Add-Result 'stocktake-create' 'FAIL' $_.Exception.Message
  }

  if ($stockTake) {
    try {
      $stockTakeItem = $stockTake.items | Where-Object { $_.productId -eq $txProduct.id } | Select-Object -First 1
      if (-not $stockTakeItem) {
        $stockTakeItem = $stockTake.items | Select-Object -First 1
      }
      if ($stockTakeItem) {
        $targetActual = [decimal]$stockTakeItem.systemQty - 1
        if ($targetActual -lt 0) {
          $targetActual = 0
        }
        $stockTake = (Send-Api 'POST' "/stock-takes/$($stockTake.id)/count" @{
          items = @(@{ id = $stockTakeItem.id; actualQty = $targetActual; remark = 'QA diff count' })
        }).data
        Add-Result 'stocktake-count' 'PASS' "itemId=$($stockTakeItem.id), actual=$targetActual"
      } else {
        Add-Result 'stocktake-count' 'FAIL' 'No stock take item available to count'
      }
    } catch {
      Add-Result 'stocktake-count' 'FAIL' $_.Exception.Message
    }

    try {
      $stockTake = (Send-Api 'POST' "/stock-takes/$($stockTake.id)/finish").data
      Add-Result 'stocktake-finish' 'PASS' $stockTake.status
    } catch {
      Add-Result 'stocktake-finish' 'FAIL' $_.Exception.Message
    }

    try {
      $stockTake = (Send-Api 'POST' "/stock-takes/$($stockTake.id)/adjust").data
      Add-Result 'stocktake-adjust' 'PASS' $stockTake.status
    } catch {
      Add-Result 'stocktake-adjust' 'FAIL' $_.Exception.Message
    }
  }

  try {
    $finalStocks = (Get-Api ("/stocks?warehouseId={0}&keyword={1}" -f $warehouse1.id, [uri]::EscapeDataString($txProduct.skuCode))).data
    $finalQty = Qty-Sum $finalStocks
    Add-Result 'stock-final-check' 'PASS' "qty=$finalQty"
  } catch {
    Add-Result 'stock-final-check' 'FAIL' $_.Exception.Message
  }
}

$summary = [pscustomobject]@{
  timestamp = $ts
  passed = @($results | Where-Object { $_.status -eq 'PASS' }).Count
  failed = @($results | Where-Object { $_.status -eq 'FAIL' }).Count
  skipped = @($results | Where-Object { $_.status -eq 'SKIP' }).Count
  results = $results
}

$summary | ConvertTo-Json -Depth 6 -Compress
