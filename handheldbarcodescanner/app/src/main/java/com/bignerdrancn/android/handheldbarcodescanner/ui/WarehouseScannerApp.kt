@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.bignerdrancn.android.handheldbarcodescanner.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bignerdrancn.android.handheldbarcodescanner.data.ApiException
import com.bignerdrancn.android.handheldbarcodescanner.data.JsonMap
import com.bignerdrancn.android.handheldbarcodescanner.data.RecentScan
import com.bignerdrancn.android.handheldbarcodescanner.data.SessionStore
import com.bignerdrancn.android.handheldbarcodescanner.data.UserSession
import com.bignerdrancn.android.handheldbarcodescanner.data.WmsApiClient
import com.bignerdrancn.android.handheldbarcodescanner.data.asMap
import com.bignerdrancn.android.handheldbarcodescanner.data.decimal
import com.bignerdrancn.android.handheldbarcodescanner.data.long
import com.bignerdrancn.android.handheldbarcodescanner.data.mapList
import com.bignerdrancn.android.handheldbarcodescanner.data.string
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigDecimal

@Composable
fun WarehouseScannerApp(
    scannedCode: String?,
    onScannedCodeConsumed: () -> Unit,
    onScannerCaptureChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val sessionStore = remember { SessionStore(context.applicationContext) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var baseUrl by remember { mutableStateOf(sessionStore.baseUrl) }
    var session by remember { mutableStateOf(sessionStore.loadSession()) }
    var selectedTab by remember { mutableStateOf(WorkTab.Scan) }
    var loginLoading by remember { mutableStateOf(false) }
    var connectionChecking by remember { mutableStateOf(false) }

    val api = remember(baseUrl, session?.token) {
        WmsApiClient(baseUrl) { session?.token }
    }

    var scanCode by remember { mutableStateOf("") }
    var scanLoading by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<JsonMap?>(null) }
    var scanStockMatches by remember { mutableStateOf<List<JsonMap>>(emptyList()) }
    var scanHistory by remember { mutableStateOf(sessionStore.loadScanHistory()) }

    var inboundLoading by remember { mutableStateOf(false) }
    var inboundOrders by remember { mutableStateOf<List<JsonMap>>(emptyList()) }
    var selectedInbound by remember { mutableStateOf<JsonMap?>(null) }

    var outboundLoading by remember { mutableStateOf(false) }
    var outboundOrders by remember { mutableStateOf<List<JsonMap>>(emptyList()) }
    var selectedOutbound by remember { mutableStateOf<JsonMap?>(null) }
    var outboundScanCode by remember { mutableStateOf("") }

    var exceptionLoading by remember { mutableStateOf(false) }
    var exceptionTickets by remember { mutableStateOf<List<JsonMap>>(emptyList()) }
    var selectedException by remember { mutableStateOf<JsonMap?>(null) }

    var approvalLoading by remember { mutableStateOf(false) }
    var approvalOrders by remember { mutableStateOf<List<JsonMap>>(emptyList()) }
    var selectedApproval by remember { mutableStateOf<JsonMap?>(null) }

    var stockKeyword by remember { mutableStateOf("") }
    var stockLoading by remember { mutableStateOf(false) }
    var stockItems by remember { mutableStateOf<List<JsonMap>>(emptyList()) }

    var takeLoading by remember { mutableStateOf(false) }
    var takeOrders by remember { mutableStateOf<List<JsonMap>>(emptyList()) }
    var selectedTake by remember { mutableStateOf<JsonMap?>(null) }
    var selectedTakeItemId by remember { mutableStateOf<Long?>(null) }
    var takeScanCode by remember { mutableStateOf("") }
    var actualQtyInput by remember { mutableStateOf("") }
    var takeRemark by remember { mutableStateOf("") }

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    fun runConnectionCheck(url: String = baseUrl) {
        scope.launch {
            connectionChecking = true
            try {
                sessionStore.baseUrl = url
                baseUrl = sessionStore.baseUrl
                val check = WmsApiClient(url) { session?.token }.testConnection()
                val status = check.statusCode?.let { " (HTTP $it)" }.orEmpty()
                showMessage(check.message + status)
            } catch (exception: Exception) {
                showMessage(exception.message ?: "连接检测失败")
            } finally {
                connectionChecking = false
            }
        }
    }

    suspend fun performStockSearch(keyword: String) {
        stockLoading = true
        try {
            stockKeyword = keyword
            stockItems = api.stocks(keyword)
            if (stockItems.isEmpty()) {
                showMessage("没有匹配库存")
            }
        } finally {
            stockLoading = false
        }
    }

    suspend fun loadInbounds(selectFirst: Boolean = false) {
        inboundLoading = true
        try {
            inboundOrders = api.inbounds()
            if (selectFirst && selectedInbound == null) {
                inboundOrders.firstOrNull()?.let { selectedInbound = api.inboundDetail(it.long("id")) }
            }
        } finally {
            inboundLoading = false
        }
    }

    suspend fun loadInboundDetail(id: Long) {
        inboundLoading = true
        try {
            selectedInbound = api.inboundDetail(id)
        } finally {
            inboundLoading = false
        }
    }

    suspend fun loadOutbounds(selectFirst: Boolean = false) {
        outboundLoading = true
        try {
            outboundOrders = api.outbounds()
            if (selectFirst && selectedOutbound == null) {
                outboundOrders.firstOrNull()?.let { selectedOutbound = api.outboundDetail(it.long("id")) }
            }
        } finally {
            outboundLoading = false
        }
    }

    suspend fun loadOutboundDetail(id: Long) {
        outboundLoading = true
        try {
            selectedOutbound = api.outboundDetail(id)
        } finally {
            outboundLoading = false
        }
    }

    suspend fun loadExceptions(selectFirst: Boolean = false) {
        exceptionLoading = true
        try {
            exceptionTickets = api.exceptions()
            if (selectFirst && selectedException == null) {
                selectedException = exceptionTickets.firstOrNull()
            }
        } finally {
            exceptionLoading = false
        }
    }

    suspend fun loadApprovals(selectFirst: Boolean = false) {
        approvalLoading = true
        try {
            approvalOrders = api.approvals()
            if (selectFirst && selectedApproval == null) {
                selectedApproval = approvalOrders.firstOrNull()
            }
        } finally {
            approvalLoading = false
        }
    }

    fun selectTakeItem(item: JsonMap) {
        selectedTakeItemId = item.long("id")
        actualQtyInput = item.decimal("actualQty").cleanText()
        takeRemark = item.string("remark")
        takeScanCode = item.string("barcode").ifBlank { item.string("skuCode") }
    }

    suspend fun loadTakeDetail(id: Long) {
        selectedTake = api.stockTakeDetail(id)
        val firstPending = selectedTake?.mapList("items")
            ?.firstOrNull { it.string("status") == "PENDING" || it.string("status") == "DIFF" }
            ?: selectedTake?.mapList("items")?.firstOrNull()
        if (firstPending != null) {
            selectTakeItem(firstPending)
        }
    }

    suspend fun loadStockTakes(selectFirst: Boolean = true) {
        takeLoading = true
        try {
            takeOrders = api.stockTakes()
            if (selectFirst && selectedTake == null) {
                takeOrders.firstOrNull()?.let { loadTakeDetail(it.long("id")) }
            }
        } finally {
            takeLoading = false
        }
    }

    suspend fun performLookup(code: String) {
        val trimmed = code.trim()
        if (trimmed.isBlank()) {
            showMessage("请输入或扫描编码")
            return
        }

        scanLoading = true
        scanCode = trimmed
        scanStockMatches = emptyList()
        try {
            val scanSave = api.recordScan(
                rawContent = trimmed,
                operatorName = session.operatorName()
            )
            val record = scanSave["record"].asMap()
            val lookup = scanSave["lookupData"].asMap()
            if (lookup.isEmpty()) {
                showMessage(scanSave.string("message").ifBlank { "扫码记录已保存，未匹配业务对象" })
                scanResult = null
                scanStockMatches = api.stocks(record.string("parsedCode").ifBlank { trimmed })
                return
            }
            val entityType = lookup.string("entityType")
            val id = lookup.long("id")
            val detailed = when (entityType) {
                "INBOUND_ORDER" -> api.inboundDetail(id) + mapOf("entityType" to entityType)
                "OUTBOUND_ORDER" -> api.outboundDetail(id) + mapOf("entityType" to entityType)
                else -> lookup
            }
            scanResult = detailed
            if (entityType == "PRODUCT_STOCK") {
                scanStockMatches = detailed.mapList("stocks")
            }
            val updatedHistory = (listOf(
                RecentScan(
                    code = record.string("parsedCode").ifBlank { trimmed },
                    entityType = entityType,
                    title = detailed.string("orderNo")
                        .ifBlank { detailed.string("locationCode") }
                        .ifBlank { detailed.string("skuCode") }
                        .ifBlank { trimmed }
                )
            ) + scanHistory.filterNot { it.code == record.string("parsedCode").ifBlank { trimmed } }).take(SessionStore.MAX_SCAN_HISTORY)
            scanHistory = updatedHistory
            sessionStore.saveScanHistory(updatedHistory)
        } catch (exception: ApiException) {
            scanResult = null
            try {
                scanStockMatches = api.stocks(trimmed)
                if (scanStockMatches.isEmpty()) {
                    showMessage(exception.message)
                } else {
                    showMessage("已显示库存匹配")
                }
            } catch (stockException: ApiException) {
                showMessage(stockException.message)
            }
        } finally {
            scanLoading = false
        }
    }

    fun runLookup(code: String = scanCode) {
        scope.launch {
            performLookup(code)
        }
    }

    fun runStockSearch(keyword: String = stockKeyword) {
        scope.launch {
            try {
                performStockSearch(keyword)
            } catch (exception: ApiException) {
                showMessage(exception.message)
            }
        }
    }

    suspend fun performInboundAction(target: JsonMap, action: InboundAction): JsonMap {
        val updated = when (action) {
            InboundAction.Receive -> api.receiveInbound(target.long("id"))
            InboundAction.Putaway -> api.putawayInbound(target.long("id"))
            InboundAction.Pickup -> api.pickupAction(
                id = target.long("id"),
                action = "PICKED_UP",
                operatorName = session.operatorName(),
                extendMinutes = null,
                remark = "手持扫码枪取件完成"
            )
            InboundAction.Delay -> api.pickupAction(
                id = target.long("id"),
                action = "DELAY",
                operatorName = session.operatorName(),
                extendMinutes = 30,
                remark = "手持扫码枪延迟取件"
            )
            InboundAction.Refuse -> api.pickupAction(
                id = target.long("id"),
                action = "REFUSE",
                operatorName = session.operatorName(),
                extendMinutes = null,
                remark = "手持扫码枪拒收登记"
            )
        }
        showMessage(action.doneMessage)
        return updated + mapOf("entityType" to "INBOUND_ORDER")
    }

    fun runInboundAction(action: InboundAction, target: JsonMap? = scanResult) {
        val result = target ?: return
        scope.launch {
            scanLoading = true
            try {
                val updated = performInboundAction(result, action)
                scanResult = updated
                if (selectedInbound?.long("id") == result.long("id")) {
                    selectedInbound = updated
                    loadInbounds(selectFirst = false)
                }
            } catch (exception: ApiException) {
                showMessage(exception.message)
            } finally {
                scanLoading = false
            }
        }
    }

    suspend fun performOutboundAction(target: JsonMap, action: OutboundAction): JsonMap {
        val updated = when (action) {
            OutboundAction.Picking -> api.pickingOutbound(target.long("id"))
            OutboundAction.Ship -> api.shipOutbound(target.long("id"))
        }
        showMessage(action.doneMessage)
        return updated + mapOf("entityType" to "OUTBOUND_ORDER")
    }

    fun runOutboundAction(action: OutboundAction, target: JsonMap? = scanResult) {
        val result = target ?: return
        scope.launch {
            scanLoading = true
            try {
                val updated = performOutboundAction(result, action)
                scanResult = updated
                if (selectedOutbound?.long("id") == result.long("id")) {
                    selectedOutbound = updated
                    loadOutbounds(selectFirst = false)
                }
            } catch (exception: ApiException) {
                showMessage(exception.message)
            } finally {
                scanLoading = false
            }
        }
    }

    fun runScanResultOutboundItem(code: String = scanCode) {
        val order = scanResult ?: return
        if (order.string("entityType") != "OUTBOUND_ORDER") {
            return
        }
        scope.launch {
            scanLoading = true
            try {
                val updated = api.scanShipOutbound(
                    id = order.long("id"),
                    rawContent = code.trim(),
                    operatorName = session.operatorName()
                ) + mapOf("entityType" to "OUTBOUND_ORDER")
                scanResult = updated
                if (selectedOutbound?.long("id") == updated.long("id")) {
                    selectedOutbound = updated
                }
                scanCode = ""
                showMessage("扫码确认成功")
                loadOutbounds(selectFirst = false)
            } catch (exception: ApiException) {
                showMessage(exception.message)
            } finally {
                scanLoading = false
            }
        }
    }

    fun submitStockTakeCount() {
        val take = selectedTake ?: return
        val itemId = selectedTakeItemId ?: return
        val actualQty = actualQtyInput.trim().toBigDecimalOrNull()
        if (actualQty == null) {
            showMessage("盘点数量格式不正确")
            return
        }

        scope.launch {
            takeLoading = true
            try {
                selectedTake = api.countStockTakeItem(
                    stockTakeId = take.long("id"),
                    itemId = itemId,
                    actualQty = actualQty,
                    remark = takeRemark
                )
                selectedTake?.mapList("items")?.firstOrNull { it.long("id") == itemId }?.let(::selectTakeItem)
                showMessage("盘点数量已提交")
            } catch (exception: ApiException) {
                showMessage(exception.message)
            } finally {
                takeLoading = false
            }
        }
    }

    fun runStockTakeOrderAction(action: StockTakeOrderAction) {
        val take = selectedTake ?: return
        scope.launch {
            takeLoading = true
            try {
                selectedTake = when (action) {
                    StockTakeOrderAction.Finish -> api.finishStockTake(take.long("id"))
                    StockTakeOrderAction.Adjust -> api.adjustStockTake(take.long("id"))
                }
                showMessage(action.doneMessage)
            } catch (exception: ApiException) {
                showMessage(exception.message)
            } finally {
                takeLoading = false
            }
        }
    }

    fun runExceptionAction(action: ExceptionAction) {
        val ticket = selectedException ?: return
        scope.launch {
            exceptionLoading = true
            try {
                val operator = session.operatorName()
                val updated = when (action) {
                    ExceptionAction.Assign -> api.assignException(ticket.long("id"), operator)
                    ExceptionAction.Resolve -> api.resolveException(ticket.long("id"), operator)
                    ExceptionAction.Close -> api.closeException(ticket.long("id"))
                    ExceptionAction.SubmitApproval -> {
                        api.submitExceptionApproval(
                            ticketId = ticket.long("id"),
                            ticketNo = ticket.string("ticketNo"),
                            applicantName = operator,
                            approverName = "仓库主管",
                            reason = ticket.string("ticketTitle").ifBlank { "异常工单审批" }
                        )
                        api.exceptions().firstOrNull { it.long("id") == ticket.long("id") } ?: ticket
                    }
                }
                selectedException = updated
                exceptionTickets = api.exceptions()
                showMessage(action.doneMessage)
            } catch (exception: ApiException) {
                showMessage(exception.message)
            } finally {
                exceptionLoading = false
            }
        }
    }

    fun runApprovalAction(action: ApprovalAction) {
        val approval = selectedApproval ?: return
        scope.launch {
            approvalLoading = true
            try {
                selectedApproval = when (action) {
                    ApprovalAction.Approve -> api.approve(
                        id = approval.long("id"),
                        operatorName = session.operatorName(),
                        comment = "移动端审批通过"
                    )
                    ApprovalAction.Reject -> api.reject(
                        id = approval.long("id"),
                        operatorName = session.operatorName(),
                        comment = "移动端审批驳回"
                    )
                }
                approvalOrders = api.approvals()
                showMessage(action.doneMessage)
            } catch (exception: ApiException) {
                showMessage(exception.message)
            } finally {
                approvalLoading = false
            }
        }
    }

    fun handleTakeScan(code: String) {
        takeScanCode = code
        val match = selectedTake?.mapList("items")?.firstOrNull { item ->
            item.string("barcode").equals(code, ignoreCase = true) ||
                item.string("skuCode").equals(code, ignoreCase = true) ||
                item.long("id").toString() == code ||
                item.long("stockId").toString() == code
        }

        if (match != null) {
            selectTakeItem(match)
            showMessage("已定位盘点明细")
        } else {
            showMessage("当前盘点单没有匹配明细")
        }
    }

    suspend fun openInboundFromScan(code: String) {
        inboundLoading = true
        try {
            val scanSave = api.recordScan(
                rawContent = code.trim(),
                operatorName = session.operatorName()
            )
            val lookup = scanSave["lookupData"].asMap()
            if (lookup.isEmpty() || lookup.string("entityType") != "INBOUND_ORDER") {
                showMessage("扫码结果不是入库单")
                return
            }
            selectedInbound = api.inboundDetail(lookup.long("id")) + mapOf("entityType" to "INBOUND_ORDER")
            if (inboundOrders.isEmpty()) {
                inboundOrders = api.inbounds()
            }
            showMessage("已打开入库单")
        } catch (exception: ApiException) {
            showMessage(exception.message)
        } finally {
            inboundLoading = false
        }
    }

    suspend fun scanOutboundItem(order: JsonMap, code: String) {
        val trimmed = code.trim()
        if (trimmed.isBlank()) {
            showMessage("请先扫描商品条码")
            return
        }

        outboundLoading = true
        try {
            val updated = api.scanShipOutbound(
                id = order.long("id"),
                rawContent = trimmed,
                operatorName = session.operatorName()
            ) + mapOf("entityType" to "OUTBOUND_ORDER")
            selectedOutbound = updated
            if (scanResult?.long("id") == updated.long("id")) {
                scanResult = updated
            }
            outboundScanCode = ""
            showMessage("扫码确认成功")
            loadOutbounds(selectFirst = false)
        } catch (exception: ApiException) {
            showMessage(exception.message)
        } finally {
            outboundLoading = false
        }
    }

    suspend fun openOutboundFromScan(code: String) {
        val currentOrder = selectedOutbound
        if (currentOrder != null && currentOrder.string("status") != "SHIPPED") {
            scanOutboundItem(currentOrder, code)
            return
        }

        outboundLoading = true
        try {
            val scanSave = api.recordScan(
                rawContent = code.trim(),
                operatorName = session.operatorName()
            )
            val lookup = scanSave["lookupData"].asMap()
            if (lookup.isEmpty() || lookup.string("entityType") != "OUTBOUND_ORDER") {
                showMessage("扫码结果不是出库单")
                return
            }
            selectedOutbound = api.outboundDetail(lookup.long("id")) + mapOf("entityType" to "OUTBOUND_ORDER")
            if (outboundOrders.isEmpty()) {
                outboundOrders = api.outbounds()
            }
            showMessage("已打开出库单")
        } catch (exception: ApiException) {
            showMessage(exception.message)
        } finally {
            outboundLoading = false
        }
    }

    DisposableEffect(session != null) {
        onScannerCaptureChanged(session != null)
        onDispose { onScannerCaptureChanged(false) }
    }

    LaunchedEffect(scannedCode) {
        val code = scannedCode ?: return@LaunchedEffect
        onScannedCodeConsumed()
        if (session == null) {
            showMessage("请先登录")
            return@LaunchedEffect
        }

        when (selectedTab) {
            WorkTab.Scan -> performLookup(code)
            WorkTab.Inbound -> openInboundFromScan(code)
            WorkTab.Outbound -> openOutboundFromScan(code)
            WorkTab.Stock -> performStockSearch(code)
            WorkTab.StockTake -> {
                if (selectedTake == null && takeOrders.isEmpty()) {
                    loadStockTakes()
                }
                handleTakeScan(code)
            }
            WorkTab.Exception -> {
                selectedTab = WorkTab.Scan
                performLookup(code)
            }
            WorkTab.Approval -> {
                selectedTab = WorkTab.Scan
                performLookup(code)
            }
            WorkTab.Settings -> {
                selectedTab = WorkTab.Scan
                performLookup(code)
            }
        }
    }

    if (session == null) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            LoginScreen(
                modifier = Modifier.padding(padding),
                loading = loginLoading,
                baseUrl = baseUrl,
                checking = connectionChecking,
                onBaseUrlChange = { baseUrl = it },
                onCheck = { runConnectionCheck() },
                onLogin = { username, password ->
                    scope.launch {
                        loginLoading = true
                        try {
                            val loginBaseUrl = SessionStore.normalizeBaseUrl(baseUrl)
                            sessionStore.baseUrl = loginBaseUrl
                            baseUrl = loginBaseUrl
                            val nextSession = WmsApiClient(loginBaseUrl) { null }.login(username, password)
                            sessionStore.saveSession(nextSession)
                            session = nextSession
                            selectedTab = WorkTab.Settings
                            showMessage("登录成功")
                        } catch (exception: ApiException) {
                            showMessage(exception.message)
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (exception: Exception) {
                            showMessage(exception.message ?: "登录失败")
                        } finally {
                            loginLoading = false
                        }
                    }
                }
            )
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
            ) {
                ModuleNavigationBar(
                    tabs = WorkTab.entries,
                    selectedTab = selectedTab,
                    onSelect = { tab ->
                        selectedTab = tab
                        if (tab == WorkTab.Inbound && inboundOrders.isEmpty()) {
                            scope.launch {
                                try {
                                    loadInbounds()
                                } catch (exception: ApiException) {
                                    showMessage(exception.message)
                                }
                            }
                        }
                        if (tab == WorkTab.Outbound && outboundOrders.isEmpty()) {
                            scope.launch {
                                try {
                                    loadOutbounds()
                                } catch (exception: ApiException) {
                                    showMessage(exception.message)
                                }
                            }
                        }
                        if (tab == WorkTab.StockTake && takeOrders.isEmpty()) {
                            scope.launch {
                                try {
                                    loadStockTakes()
                                } catch (exception: ApiException) {
                                    showMessage(exception.message)
                                }
                            }
                        }
                        if (tab == WorkTab.Exception && exceptionTickets.isEmpty()) {
                            scope.launch {
                                try {
                                    loadExceptions()
                                } catch (exception: ApiException) {
                                    showMessage(exception.message)
                                }
                            }
                        }
                        if (tab == WorkTab.Approval && approvalOrders.isEmpty()) {
                            scope.launch {
                                try {
                                    loadApprovals()
                                } catch (exception: ApiException) {
                                    showMessage(exception.message)
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            AppHeader(session = session, selectedTab = selectedTab)
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    WorkTab.Scan -> ScanWorkspace(
                        code = scanCode,
                        loading = scanLoading,
                        result = scanResult,
                        stockMatches = scanStockMatches,
                        history = scanHistory,
                        onCodeChange = { scanCode = it },
                        onLookup = { runLookup() },
                        onCameraScan = { scannedCode ->
                            runLookup(scannedCode)
                        },
                        onReuseHistory = { runLookup(it) },
                        onClear = {
                            scanCode = ""
                            scanResult = null
                            scanStockMatches = emptyList()
                        },
                        onClearHistory = {
                            scanHistory = emptyList()
                            sessionStore.clearScanHistory()
                            showMessage("最近扫码已清空")
                        },
                        onInboundAction = ::runInboundAction,
                        onOutboundAction = ::runOutboundAction,
                        onOutboundScanItem = ::runScanResultOutboundItem
                    )
                    WorkTab.Inbound -> InboundWorkspace(
                        loading = inboundLoading,
                        orders = inboundOrders,
                        selectedOrder = selectedInbound,
                        onRefresh = {
                            scope.launch {
                                try {
                                    loadInbounds(selectFirst = false)
                                } catch (exception: ApiException) {
                                    showMessage(exception.message)
                                }
                            }
                        },
                        onSelectOrder = { order ->
                            scope.launch {
                                try {
                                    loadInboundDetail(order.long("id"))
                                } catch (exception: ApiException) {
                                    showMessage(exception.message)
                                }
                            }
                        },
                        onAction = { action ->
                            val order = selectedInbound ?: return@InboundWorkspace
                            scope.launch {
                                inboundLoading = true
                                try {
                                    selectedInbound = performInboundAction(order, action)
                                    loadInbounds(selectFirst = false)
                                } catch (exception: ApiException) {
                                    showMessage(exception.message)
                                } finally {
                                    inboundLoading = false
                                }
                            }
                        }
                    )
                    WorkTab.Outbound -> OutboundWorkspace(
                        loading = outboundLoading,
                        orders = outboundOrders,
                        selectedOrder = selectedOutbound,
                        onRefresh = {
                            scope.launch {
                                try {
                                    loadOutbounds(selectFirst = false)
                                } catch (exception: ApiException) {
                                    showMessage(exception.message)
                                }
                            }
                        },
                        onSelectOrder = { order ->
                            scope.launch {
                                try {
                                    loadOutboundDetail(order.long("id"))
                                    outboundScanCode = ""
                                } catch (exception: ApiException) {
                                    showMessage(exception.message)
                                }
                            }
                        },
                        onAction = { action ->
                            val order = selectedOutbound ?: return@OutboundWorkspace
                            scope.launch {
                                outboundLoading = true
                                try {
                                    selectedOutbound = performOutboundAction(order, action)
                                    loadOutbounds(selectFirst = false)
                                } catch (exception: ApiException) {
                                    showMessage(exception.message)
                                } finally {
                                    outboundLoading = false
                                }
                            }
                        },
                        scanCode = outboundScanCode,
                        onScanCodeChange = { outboundScanCode = it },
                        onScanItem = {
                            val order = selectedOutbound ?: return@OutboundWorkspace
                            scope.launch {
                                scanOutboundItem(order, outboundScanCode)
                            }
                        }
                    )
                    WorkTab.Stock -> StockWorkspace(
                        keyword = stockKeyword,
                        loading = stockLoading,
                        stocks = stockItems,
                        onKeywordChange = { stockKeyword = it },
                        onSearch = { runStockSearch() },
                        onClear = {
                            stockKeyword = ""
                            stockItems = emptyList()
                        }
                    )
                    WorkTab.StockTake -> StockTakeWorkspace(
                        loading = takeLoading,
                        orders = takeOrders,
                        selectedTake = selectedTake,
                        selectedItemId = selectedTakeItemId,
                        scanCode = takeScanCode,
                        actualQty = actualQtyInput,
                        remark = takeRemark,
                        onRefreshOrders = {
                            scope.launch {
                                try {
                                    loadStockTakes(selectFirst = false)
                                } catch (exception: ApiException) {
                                    showMessage(exception.message)
                                }
                            }
                        },
                        onSelectOrder = { order ->
                            scope.launch {
                                takeLoading = true
                                try {
                                    loadTakeDetail(order.long("id"))
                                } catch (exception: ApiException) {
                                    showMessage(exception.message)
                                } finally {
                                    takeLoading = false
                                }
                            }
                        },
                        onScanCodeChange = { takeScanCode = it },
                        onLocateItem = { handleTakeScan(takeScanCode.trim()) },
                        onSelectItem = ::selectTakeItem,
                        onActualQtyChange = { actualQtyInput = it },
                        onRemarkChange = { takeRemark = it },
                        onSubmitCount = ::submitStockTakeCount,
                        onOrderAction = ::runStockTakeOrderAction
                    )
                    WorkTab.Exception -> ExceptionWorkspace(
                        loading = exceptionLoading,
                        tickets = exceptionTickets,
                        selectedTicket = selectedException,
                        onRefresh = {
                            scope.launch {
                                try {
                                    loadExceptions(selectFirst = false)
                                } catch (exception: ApiException) {
                                    showMessage(exception.message)
                                }
                            }
                        },
                        onSelectTicket = { ticket -> selectedException = ticket },
                        onAction = ::runExceptionAction
                    )
                    WorkTab.Approval -> ApprovalWorkspace(
                        loading = approvalLoading,
                        approvals = approvalOrders,
                        selectedApproval = selectedApproval,
                        onRefresh = {
                            scope.launch {
                                try {
                                    loadApprovals(selectFirst = false)
                                } catch (exception: ApiException) {
                                    showMessage(exception.message)
                                }
                            }
                        },
                        onSelectApproval = { approval -> selectedApproval = approval },
                        onAction = ::runApprovalAction
                    )
                    WorkTab.Settings -> SettingsWorkspace(
                        baseUrl = baseUrl,
                        checking = connectionChecking,
                        session = session,
                        onBaseUrlChange = { baseUrl = it },
                        onSaveBaseUrl = {
                            sessionStore.baseUrl = baseUrl
                            baseUrl = sessionStore.baseUrl
                            showMessage("服务地址已保存")
                        },
                        onCheck = {
                            runConnectionCheck(baseUrl)
                        },
                        onLogout = {
                            sessionStore.clearSession()
                            session = null
                            scanResult = null
                            inboundOrders = emptyList()
                            selectedInbound = null
                            outboundOrders = emptyList()
                            selectedOutbound = null
                            exceptionTickets = emptyList()
                            selectedException = null
                            approvalOrders = emptyList()
                            selectedApproval = null
                            stockItems = emptyList()
                            selectedTake = null
                            selectedTakeItemId = null
                        }
                    )
                }

                if (
                    scanLoading ||
                    inboundLoading ||
                    outboundLoading ||
                    stockLoading ||
                    takeLoading ||
                    exceptionLoading ||
                    approvalLoading
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
private fun BrandMark(
    size: Dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    borderColor: Color = Color.Transparent
) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "WMS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun ModuleNavigationBar(
    tabs: List<WorkTab>,
    selectedTab: WorkTab,
    onSelect: (WorkTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val selected = selectedTab == tab
            Surface(
                onClick = { onSelect(tab) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(82.dp)
                    .height(48.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.26f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(tab.shortLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                        }
                    }
                    Text(
                        tab.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(
    modifier: Modifier,
    loading: Boolean,
    baseUrl: String,
    checking: Boolean,
    onBaseUrlChange: (String) -> Unit,
    onCheck: () -> Unit,
    onLogin: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("admin123") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LoginBrandPanel()

            WorkCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("登录工作台")
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = onBaseUrlChange,
                        label = { Text("服务地址") },
                        singleLine = true,
                        enabled = !loading && !checking,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        enabled = !loading && !checking,
                        onClick = onCheck,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (checking) "检测中" else "检测服务")
                    }
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("账号") },
                        singleLine = true,
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = !loading,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (!loading && !checking) {
                                onLogin(username.trim(), password)
                            }
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        enabled = !loading && !checking,
                        onClick = { onLogin(username.trim(), password) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (loading) "登录中" else "登录")
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginBrandPanel() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandMark(
                size = 54.dp,
                containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                contentColor = MaterialTheme.colorScheme.onPrimary,
                borderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.24f)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("仓库手持扫码枪", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(
                    "移动作业终端",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun AppHeader(session: UserSession?, selectedTab: WorkTab) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandMark(
                    size = 40.dp,
                    containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    borderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                )
                Column {
                    Text(
                        text = selectedTab.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = session.operatorName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            StatusPill(
                text = session?.roleCode.orEmpty().ifBlank { "PDA" },
                containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun ScanWorkspace(
    code: String,
    loading: Boolean,
    result: JsonMap?,
    stockMatches: List<JsonMap>,
    history: List<RecentScan>,
    onCodeChange: (String) -> Unit,
    onLookup: () -> Unit,
    onCameraScan: (String) -> Unit,
    onReuseHistory: (String) -> Unit,
    onClear: () -> Unit,
    onClearHistory: () -> Unit,
    onInboundAction: (InboundAction) -> Unit,
    onOutboundAction: (OutboundAction) -> Unit,
    onOutboundScanItem: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BarcodeCameraScanner(
            enabled = !loading,
            onCodeScanned = onCameraScan
        )

        ScanInputBar(
            code = code,
            buttonText = "查询",
            onCodeChange = onCodeChange,
            onSubmit = onLookup,
            onClear = onClear,
            enabled = !loading
        )

        ScanOverview(
            loading = loading,
            result = result,
            stockMatches = stockMatches,
            historyCount = history.size
        )

        when {
            result != null -> LookupResultCard(
                result = result,
                scanCode = code,
                loading = loading,
                onScanCodeChange = onCodeChange,
                onOutboundScanItem = onOutboundScanItem,
                onInboundAction = onInboundAction,
                onOutboundAction = onOutboundAction
            )
            stockMatches.isNotEmpty() -> StockListCard(title = "库存匹配", stocks = stockMatches)
            else -> EmptyPanel(text = "暂无扫码结果")
        }

        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("最近扫码")
                OutlinedButton(
                    enabled = !loading,
                    onClick = onClearHistory
                ) {
                    Text("清空记录")
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                history.forEach { item ->
                    RecentScanCard(item = item, onClick = { onReuseHistory(item.code) })
                }
            }
        }
    }
}

@Composable
private fun ScanOverview(
    loading: Boolean,
    result: JsonMap?,
    stockMatches: List<JsonMap>,
    historyCount: Int
) {
    val resultText = when {
        loading -> "查询中"
        result != null -> entityTypeLabel(result.string("entityType"))
        stockMatches.isNotEmpty() -> "库存匹配"
        else -> "待扫码"
    }
    val statusText = when {
        result != null -> statusLabel(displayStatus(result))
        stockMatches.isNotEmpty() -> "${stockMatches.size} 条"
        else -> "空闲"
    }
    val resultAccent = when {
        result != null -> entityAccentColor(result.string("entityType"))
        stockMatches.isNotEmpty() -> entityAccentColor("PRODUCT_STOCK")
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricTile(
            label = "识别结果",
            value = resultText,
            modifier = Modifier.weight(1f),
            accentColor = resultAccent
        )
        MetricTile(
            label = "当前状态",
            value = statusText,
            modifier = Modifier.weight(1f),
            accentColor = statusColor(displayStatus(result ?: emptyMap()))
        )
        MetricTile(
            label = "历史",
            value = "${historyCount} 条",
            modifier = Modifier.weight(1f),
            accentColor = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                value.ifBlank { "-" },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentScanCard(item: RecentScan, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.width(154.dp),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.26f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .width(4.dp)
                    .height(38.dp),
                shape = RoundedCornerShape(8.dp),
                color = entityAccentColor(item.entityType),
                content = {}
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    entityTypeLabel(item.entityType),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ScanInputBar(
    code: String,
    buttonText: String,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    enabled: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = code,
                onValueChange = onCodeChange,
                label = { Text("编码") },
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = enabled,
                    onClick = onSubmit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(buttonText)
                }
                OutlinedButton(onClick = onClear) {
                    Text("清空")
                }
            }
        }
    }
}

@Composable
private fun ResultHeader(result: JsonMap) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, statusColor(displayStatus(result)).copy(alpha = 0.28f))
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entityTypeLabel(result.string("entityType")),
                    color = statusColor(displayStatus(result)),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    primaryCode(result),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusPill(
                text = statusLabel(displayStatus(result)),
                containerColor = statusColor(displayStatus(result)).copy(alpha = 0.14f),
                contentColor = statusColor(displayStatus(result))
            )
        }
    }
}

@Composable
private fun LookupResultCard(
    result: JsonMap,
    scanCode: String,
    loading: Boolean,
    onScanCodeChange: (String) -> Unit,
    onOutboundScanItem: (String) -> Unit,
    onInboundAction: (InboundAction) -> Unit,
    onOutboundAction: (OutboundAction) -> Unit
) {
    WorkCard {
        val entityType = result.string("entityType")
        ResultHeader(result)

        InfoGrid(
            rows = productInfoRows(result) + listOf(
                "作业编码" to primaryCode(result),
                "仓库" to result.string("warehouseName"),
                "状态" to statusLabel(result.string("status")),
                "取件状态" to statusLabel(result.string("pickupStatus")),
                "客户" to result.string("customerName"),
                "收件人" to result.string("receiverName"),
                "电话" to result.string("receiverPhone"),
                "库位" to result.string("locationCode"),
                "区域" to result.string("zoneName"),
                "截止" to result.string("pickupDueAt")
            )
        )

        val items = result.mapList("items")
        if (items.isNotEmpty()) {
            SectionTitle("明细")
            items.forEach { item ->
                OrderItemRow(item)
            }
        }

        when (entityType) {
            "INBOUND_ORDER" -> InboundActions(result, onInboundAction)
            "OUTBOUND_ORDER" -> {
                SectionTitle("扫码出库")
                OutlinedTextField(
                    value = scanCode,
                    onValueChange = onScanCodeChange,
                    label = { Text("商品条码 / SKU / 二维码内容") },
                    singleLine = true,
                    enabled = !loading && result.string("status") != "SHIPPED",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onOutboundScanItem(scanCode) })
                )
                Button(
                    enabled = !loading && result.string("status") != "SHIPPED",
                    onClick = { onOutboundScanItem(scanCode) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("确认扫码")
                }
                OutboundActions(result, onOutboundAction)
            }
            "PRODUCT_STOCK" -> StockListCard(title = "库存", stocks = result.mapList("stocks"))
        }
    }
}

private fun productInfoRows(result: JsonMap): List<Pair<String, String>> {
    if (result.string("entityType") != "PRODUCT_STOCK") {
        return emptyList()
    }

    return listOf(
        "商品名称" to result.string("productName"),
        "SKU" to result.string("skuCode"),
        "条码" to result.string("barcode"),
        "单位" to result.string("unitName"),
        "库存记录" to result.string("stockCount"),
        "总库存" to result.decimal("totalQuantity").cleanText(),
        "可用库存" to result.decimal("totalAvailableQty").cleanText()
    )
}

@Composable
private fun InboundActions(result: JsonMap, onAction: (InboundAction) -> Unit) {
    val status = result.string("status")
    val pickupStatus = result.string("pickupStatus")
    SectionTitle("入库动作")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            enabled = status != "RECEIVED" && status != "PUTAWAY_COMPLETED",
            onClick = { onAction(InboundAction.Receive) }
        ) {
            Text("收货")
        }
        Button(
            enabled = status != "PUTAWAY_COMPLETED",
            onClick = { onAction(InboundAction.Putaway) }
        ) {
            Text("上架")
        }
        Button(
            enabled = status == "PUTAWAY_COMPLETED" && pickupStatus != "PICKED_UP",
            onClick = { onAction(InboundAction.Pickup) }
        ) {
            Text("取件完成")
        }
        OutlinedButton(
            enabled = status == "PUTAWAY_COMPLETED" && pickupStatus != "PICKED_UP",
            onClick = { onAction(InboundAction.Delay) }
        ) {
            Text("延迟")
        }
        OutlinedButton(
            enabled = pickupStatus != "PICKED_UP",
            onClick = { onAction(InboundAction.Refuse) }
        ) {
            Text("拒收")
        }
    }
}

@Composable
private fun OutboundActions(result: JsonMap, onAction: (OutboundAction) -> Unit) {
    val status = result.string("status")
    SectionTitle("出库动作")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            enabled = status != "PICKING_COMPLETED" && status != "SHIPPED",
            onClick = { onAction(OutboundAction.Picking) }
        ) {
            Text("拣货完成")
        }
        Button(
            enabled = status != "SHIPPED",
            onClick = { onAction(OutboundAction.Ship) }
        ) {
            Text("发运")
        }
    }
}

@Composable
private fun InboundWorkspace(
    loading: Boolean,
    orders: List<JsonMap>,
    selectedOrder: JsonMap?,
    onRefresh: () -> Unit,
    onSelectOrder: (JsonMap) -> Unit,
    onAction: (InboundAction) -> Unit
) {
    OrderWorkspace(
        loading = loading,
        orders = orders,
        selectedOrder = selectedOrder,
        emptyText = "暂无入库单",
        refreshText = "刷新入库单",
        titleForOrder = { it.string("orderNo") },
        subtitleForOrder = { order ->
            listOf(
                order.string("warehouseName"),
                order.string("supplierName").ifBlank { order.string("customerName") },
                statusLabel(order.string("status"))
            ).filter { it.isNotBlank() }.joinToString(" · ")
        },
        onRefresh = onRefresh,
        onSelectOrder = onSelectOrder
    ) { order ->
        InboundOrderDetail(order = order, loading = loading, onAction = onAction)
    }
}

@Composable
private fun OutboundWorkspace(
    loading: Boolean,
    orders: List<JsonMap>,
    selectedOrder: JsonMap?,
    onRefresh: () -> Unit,
    onSelectOrder: (JsonMap) -> Unit,
    onAction: (OutboundAction) -> Unit,
    scanCode: String,
    onScanCodeChange: (String) -> Unit,
    onScanItem: () -> Unit
) {
    OrderWorkspace(
        loading = loading,
        orders = orders,
        selectedOrder = selectedOrder,
        emptyText = "暂无出库单",
        refreshText = "刷新出库单",
        titleForOrder = { it.string("orderNo") },
        subtitleForOrder = { order ->
            listOf(
                order.string("warehouseName"),
                order.string("customerName"),
                statusLabel(order.string("status"))
            ).filter { it.isNotBlank() }.joinToString(" · ")
        },
        onRefresh = onRefresh,
        onSelectOrder = onSelectOrder
    ) { order ->
        OutboundOrderDetail(
            order = order,
            loading = loading,
            onAction = onAction,
            scanCode = scanCode,
            onScanCodeChange = onScanCodeChange,
            onScanItem = onScanItem
        )
    }
}

@Composable
private fun OrderWorkspace(
    loading: Boolean,
    orders: List<JsonMap>,
    selectedOrder: JsonMap?,
    emptyText: String,
    refreshText: String,
    titleForOrder: (JsonMap) -> String,
    subtitleForOrder: (JsonMap) -> String,
    onRefresh: () -> Unit,
    onSelectOrder: (JsonMap) -> Unit,
    detail: @Composable (JsonMap) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                enabled = !loading,
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(refreshText)
            }
        }

        if (orders.isEmpty()) {
            item { EmptyPanel(text = emptyText) }
        } else {
            item { SectionTitle("单据列表") }
            items(orders, key = { it.long("id") }) { order ->
                SelectableRow(
                    selected = selectedOrder?.long("id") == order.long("id"),
                    title = titleForOrder(order),
                    subtitle = subtitleForOrder(order),
                    onClick = { onSelectOrder(order) }
                )
            }
        }

        if (selectedOrder != null) {
            item { SectionTitle("单据详情") }
            item { detail(selectedOrder) }
        }
    }
}

@Composable
private fun InboundOrderDetail(
    order: JsonMap,
    loading: Boolean,
    onAction: (InboundAction) -> Unit
) {
    WorkCard {
        ResultHeader(order + mapOf("entityType" to "INBOUND_ORDER"))
        InfoGrid(
            rows = listOf(
                "作业编码" to primaryCode(order),
                "仓库" to order.string("warehouseName"),
                "供应商" to order.string("supplierName"),
                "客户" to order.string("customerName"),
                "来源单号" to order.string("sourceNo"),
                "预计到达" to order.string("expectedArrivalTime"),
                "实际到达" to order.string("actualArrivalTime"),
                "收货时间" to order.string("receivedAt"),
                "上架时间" to order.string("putawayCompletedAt"),
                "应收" to order.decimal("totalExpectedQty").cleanText(),
                "实收" to order.decimal("totalActualQty").cleanText(),
                "取件状态" to statusLabel(order.string("pickupStatus")),
                "截止" to order.string("pickupDueAt"),
                "备注" to order.string("remark")
            )
        )
        InboundActions(order, onAction)
        val items = order.mapList("items")
        if (items.isNotEmpty()) {
            SectionTitle("明细")
            items.forEach { item ->
                OrderItemRow(item)
            }
        }
        if (loading) {
            Text("处理中...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
        }
    }
}

@Composable
private fun OutboundOrderDetail(
    order: JsonMap,
    loading: Boolean,
    onAction: (OutboundAction) -> Unit,
    scanCode: String,
    onScanCodeChange: (String) -> Unit,
    onScanItem: () -> Unit
) {
    WorkCard {
        ResultHeader(order + mapOf("entityType" to "OUTBOUND_ORDER"))
        InfoGrid(
            rows = listOf(
                "仓库" to order.string("warehouseName"),
                "客户" to order.string("customerName"),
                "来源单号" to order.string("sourceNo"),
                "优先级" to order.string("priorityLevel"),
                "计划发运" to order.string("plannedShipTime"),
                "拣货时间" to order.string("pickingCompletedAt"),
                "发运时间" to order.string("shippedAt"),
                "计划数量" to order.decimal("totalPlannedQty").cleanText(),
                "已扫数量" to order.decimal("totalShippedQty").cleanText(),
                "作业编码" to primaryCode(order),
                "备注" to order.string("remark")
            )
        )
        SectionTitle("扫码出库")
        OutlinedTextField(
            value = scanCode,
            onValueChange = onScanCodeChange,
            label = { Text("商品条码 / SKU / 二维码内容") },
            singleLine = true,
            enabled = !loading && order.string("status") != "SHIPPED",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onScanItem() })
        )
        Button(
            enabled = !loading && order.string("status") != "SHIPPED",
            onClick = onScanItem,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("确认扫码")
        }
        OutboundActions(order, onAction)
        val items = order.mapList("items")
        if (items.isNotEmpty()) {
            SectionTitle("明细")
            items.forEach { item ->
                OrderItemRow(item)
            }
        }
        if (loading) {
            Text("处理中...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
        }
    }
}

@Composable
private fun StockWorkspace(
    keyword: String,
    loading: Boolean,
    stocks: List<JsonMap>,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScanInputBar(
            code = keyword,
            buttonText = "查询库存",
            onCodeChange = onKeywordChange,
            onSubmit = onSearch,
            onClear = onClear,
            enabled = !loading
        )
        if (stocks.isEmpty()) {
            EmptyPanel(text = "暂无库存数据")
        } else {
            StockListCard(title = "库存列表", stocks = stocks)
        }
    }
}

@Composable
private fun ExceptionWorkspace(
    loading: Boolean,
    tickets: List<JsonMap>,
    selectedTicket: JsonMap?,
    onRefresh: () -> Unit,
    onSelectTicket: (JsonMap) -> Unit,
    onAction: (ExceptionAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                enabled = !loading,
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("刷新异常")
            }
        }

        if (tickets.isEmpty()) {
            item { EmptyPanel(text = "暂无异常工单") }
        } else {
            item { SectionTitle("异常列表") }
            items(tickets, key = { it.long("id") }) { ticket ->
                SelectableRow(
                    selected = selectedTicket?.long("id") == ticket.long("id"),
                    title = ticket.string("ticketNo").ifBlank { ticket.string("ticketTitle") },
                    subtitle = listOf(
                        ticket.string("warehouseName"),
                        exceptionSeverityLabel(ticket.string("severity")),
                        exceptionStatusLabel(ticket.string("status"))
                    ).filter { it.isNotBlank() }.joinToString(" · "),
                    onClick = { onSelectTicket(ticket) }
                )
            }
        }

        if (selectedTicket != null) {
            item { SectionTitle("异常详情") }
            item {
                ExceptionTicketDetail(
                    ticket = selectedTicket,
                    loading = loading,
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
private fun ExceptionTicketDetail(
    ticket: JsonMap,
    loading: Boolean,
    onAction: (ExceptionAction) -> Unit
) {
    WorkCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(ticket.string("ticketTitle"), fontWeight = FontWeight.Bold)
                Text(
                    ticket.string("ticketNo"),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                )
            }
            StatusPill(exceptionStatusLabel(ticket.string("status")))
        }
        InfoGrid(
            rows = listOf(
                "仓库" to ticket.string("warehouseName"),
                "业务类型" to bizTypeLabel(ticket.string("bizType")),
                "业务单号" to ticket.string("bizNo"),
                "严重度" to exceptionSeverityLabel(ticket.string("severity")),
                "处理人" to ticket.string("assigneeName"),
                "上报人" to ticket.string("reporterName"),
                "审批" to approvalStatusLabel(ticket.string("approvalStatus")),
                "上报时间" to ticket.string("reportedAt"),
                "解决时间" to ticket.string("resolvedAt"),
                "关闭时间" to ticket.string("closedAt"),
                "内容" to ticket.string("ticketContent")
            )
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                enabled = !loading && ticket.string("status") == "OPEN",
                onClick = { onAction(ExceptionAction.Assign) }
            ) {
                Text("指派给我")
            }
            Button(
                enabled = !loading && ticket.string("status") != "RESOLVED" && ticket.string("status") != "CLOSED",
                onClick = { onAction(ExceptionAction.Resolve) }
            ) {
                Text("解决")
            }
            OutlinedButton(
                enabled = !loading && ticket.string("status") != "CLOSED",
                onClick = { onAction(ExceptionAction.Close) }
            ) {
                Text("关闭")
            }
            OutlinedButton(
                enabled = !loading && ticket.string("approvalStatus") != "PENDING",
                onClick = { onAction(ExceptionAction.SubmitApproval) }
            ) {
                Text("提交审批")
            }
        }
    }
}

@Composable
private fun ApprovalWorkspace(
    loading: Boolean,
    approvals: List<JsonMap>,
    selectedApproval: JsonMap?,
    onRefresh: () -> Unit,
    onSelectApproval: (JsonMap) -> Unit,
    onAction: (ApprovalAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                enabled = !loading,
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("刷新审批")
            }
        }

        if (approvals.isEmpty()) {
            item { EmptyPanel(text = "暂无审批单") }
        } else {
            item { SectionTitle("审批列表") }
            items(approvals, key = { it.long("id") }) { approval ->
                SelectableRow(
                    selected = selectedApproval?.long("id") == approval.long("id"),
                    title = approval.string("approvalNo").ifBlank { approval.string("bizNo") },
                    subtitle = listOf(
                        approvalTypeLabel(approval.string("approvalType")),
                        bizTypeLabel(approval.string("bizType")),
                        approvalStatusLabel(approval.string("status"))
                    ).filter { it.isNotBlank() }.joinToString(" · "),
                    onClick = { onSelectApproval(approval) }
                )
            }
        }

        if (selectedApproval != null) {
            item { SectionTitle("审批详情") }
            item {
                ApprovalOrderDetail(
                    approval = selectedApproval,
                    loading = loading,
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
private fun ApprovalOrderDetail(
    approval: JsonMap,
    loading: Boolean,
    onAction: (ApprovalAction) -> Unit
) {
    WorkCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(approval.string("approvalNo"), fontWeight = FontWeight.Bold)
                Text(
                    approval.string("bizNo"),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                )
            }
            StatusPill(approvalStatusLabel(approval.string("status")))
        }
        InfoGrid(
            rows = listOf(
                "审批类型" to approvalTypeLabel(approval.string("approvalType")),
                "业务类型" to bizTypeLabel(approval.string("bizType")),
                "申请人" to approval.string("applicantName"),
                "审批人" to approval.string("approverName"),
                "节点" to approval.string("currentNode"),
                "申请原因" to approval.string("applyReason"),
                "审批意见" to approval.string("approvalComment"),
                "申请时间" to approval.string("appliedAt"),
                "决定时间" to approval.string("decidedAt")
            )
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                enabled = !loading && approval.string("status") == "PENDING",
                onClick = { onAction(ApprovalAction.Approve) }
            ) {
                Text("通过")
            }
            OutlinedButton(
                enabled = !loading && approval.string("status") == "PENDING",
                onClick = { onAction(ApprovalAction.Reject) }
            ) {
                Text("驳回")
            }
        }

        val records = approval.mapList("records")
        if (records.isNotEmpty()) {
            SectionTitle("审批记录")
            records.forEach { record ->
                InfoGrid(
                    rows = listOf(
                        "动作" to approvalRecordActionLabel(record.string("actionType")),
                        "操作人" to record.string("operatorName"),
                        "意见" to record.string("actionComment")
                    )
                )
            }
        }
    }
}

@Composable
private fun StockTakeWorkspace(
    loading: Boolean,
    orders: List<JsonMap>,
    selectedTake: JsonMap?,
    selectedItemId: Long?,
    scanCode: String,
    actualQty: String,
    remark: String,
    onRefreshOrders: () -> Unit,
    onSelectOrder: (JsonMap) -> Unit,
    onScanCodeChange: (String) -> Unit,
    onLocateItem: () -> Unit,
    onSelectItem: (JsonMap) -> Unit,
    onActualQtyChange: (String) -> Unit,
    onRemarkChange: (String) -> Unit,
    onSubmitCount: () -> Unit,
    onOrderAction: (StockTakeOrderAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !loading,
                    onClick = onRefreshOrders,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("刷新盘点单")
                }
                OutlinedButton(
                    enabled = selectedTake != null,
                    onClick = { onOrderAction(StockTakeOrderAction.Finish) }
                ) {
                    Text("结束")
                }
                OutlinedButton(
                    enabled = selectedTake != null,
                    onClick = { onOrderAction(StockTakeOrderAction.Adjust) }
                ) {
                    Text("调整")
                }
            }
        }

        if (orders.isNotEmpty()) {
            item { SectionTitle("盘点单") }
            items(orders, key = { it.long("id") }) { order ->
                SelectableRow(
                    selected = selectedTake?.long("id") == order.long("id"),
                    title = order.string("takeNo"),
                    subtitle = "${order.string("warehouseName")} · ${statusLabel(order.string("status"))}",
                    onClick = { onSelectOrder(order) }
                )
            }
        }

        if (selectedTake != null) {
            item {
                ElevatedCard {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(selectedTake.string("takeNo"), fontWeight = FontWeight.Bold)
                                Text(
                                    selectedTake.string("warehouseName"),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                                )
                            }
                            StatusPill(statusLabel(selectedTake.string("status")))
                        }
                        ScanInputBar(
                            code = scanCode,
                            buttonText = "定位",
                            onCodeChange = onScanCodeChange,
                            onSubmit = onLocateItem,
                            onClear = { onScanCodeChange("") },
                            enabled = !loading
                        )
                        OutlinedTextField(
                            value = actualQty,
                            onValueChange = onActualQtyChange,
                            label = { Text("实盘数量") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = remark,
                            onValueChange = onRemarkChange,
                            label = { Text("备注") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            enabled = selectedItemId != null && !loading,
                            onClick = onSubmitCount,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("提交盘点数量")
                        }
                    }
                }
            }

            item { SectionTitle("盘点明细") }
            items(selectedTake.mapList("items"), key = { it.long("id") }) { item ->
                SelectableStockTakeItem(
                    item = item,
                    selected = item.long("id") == selectedItemId,
                    onClick = { onSelectItem(item) }
                )
            }
        } else {
            item { EmptyPanel(text = "暂无盘点单") }
        }
    }
}

@Composable
private fun SettingsWorkspace(
    baseUrl: String,
    checking: Boolean,
    session: UserSession?,
    onBaseUrlChange: (String) -> Unit,
    onSaveBaseUrl: () -> Unit,
    onCheck: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionTitle("服务")
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = onBaseUrlChange,
                    label = { Text("服务地址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !checking,
                        onClick = onSaveBaseUrl,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("保存")
                    }
                    OutlinedButton(
                        enabled = !checking,
                        onClick = onCheck,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (checking) "检测中" else "检测")
                    }
                }
            }
        }

        ElevatedCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionTitle("账号")
                InfoGrid(
                    rows = listOf(
                        "姓名" to session.operatorName(),
                        "账号" to session?.username.orEmpty(),
                        "角色" to session?.roleCode.orEmpty()
                    )
                )
                OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                    Text("退出登录")
                }
            }
        }
    }
}

@Composable
private fun StockListCard(title: String, stocks: List<JsonMap>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(title)
        stocks.forEach { stock ->
            StockRow(stock)
        }
    }
}

@Composable
private fun StockRow(stock: JsonMap) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stock.string("skuCode"), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stock.string("productName"),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(stock.decimal("availableQty").cleanText())
            }
            InfoGrid(
                rows = listOf(
                    "仓库" to stock.string("warehouseName"),
                    "库位" to stock.string("locationCode"),
                    "批次" to stock.string("batchNo"),
                    "库存" to stock.decimal("quantity").cleanText(),
                    "锁定" to stock.decimal("lockedQty").cleanText(),
                    "条码" to stock.string("barcode")
                )
            )
        }
    }
}

@Composable
private fun OrderItemRow(item: JsonMap) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(item.string("skuCode"), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                item.string("productName"),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            InfoGrid(
                rows = listOfNotNull(
                    "批次" to item.string("batchNo"),
                    quantityRow(item, "expectedQty", "应收"),
                    quantityRow(item, "actualQty", "实收"),
                    quantityRow(item, "qualifiedQty", "合格"),
                    quantityRow(item, "plannedQty", "计划"),
                    quantityRow(item, "shippedQty", "已扫"),
                    "库位ID" to item.string("locationId")
                )
            )
        }
    }
}

@Composable
private fun SelectableStockTakeItem(
    item: JsonMap,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.string("skuCode"), fontWeight = FontWeight.Bold)
                    Text(item.string("productName"))
                }
                StatusPill(statusLabel(item.string("status")))
            }
            InfoGrid(
                rows = listOf(
                    "条码" to item.string("barcode"),
                    "系统" to item.decimal("systemQty").cleanText(),
                    "实盘" to item.decimal("actualQty").cleanText(),
                    "差异" to item.decimal("diffQty").cleanText(),
                    "备注" to item.string("remark")
                )
            )
        }
    }
}

@Composable
private fun SelectableRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.72f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
        ),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.07f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                content = {}
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title.ifBlank { "-" },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle.ifBlank { "-" },
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun InfoGrid(rows: List<Pair<String, String>>) {
    val visibleRows = rows.filter { it.second.isNotBlank() }
    if (visibleRows.isEmpty()) {
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        visibleRows.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    label,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    modifier = Modifier.weight(0.38f)
                )
                Text(
                    value,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(0.62f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.18f))
    ) {
        Text(
            text = text.ifBlank { "-" },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun EmptyPanel(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
        }
    }
}

private fun primaryCode(result: JsonMap): String =
    result.string("code")
        .ifBlank { result.string("orderNo") }
        .ifBlank { result.string("pickupCode") }
        .ifBlank { result.string("scanCode") }
        .ifBlank { result.string("logisticsNo") }
        .ifBlank { result.string("locationCode") }
        .ifBlank { result.string("skuCode") }
        .ifBlank { "-" }

private fun extractLookupCode(raw: String): String {
    val text = raw.trim()
    if (text.isBlank()) {
        return text
    }

    extractCodeFromJson(text)?.let { return it }
    extractCodeFromUri(text)?.let { return it }
    extractCodeFromTextPair(text)?.let { return it }

    return text
}

private fun extractCodeFromJson(text: String): String? {
    if (!text.startsWith("{")) {
        return null
    }

    return runCatching {
        val json = JSONObject(text)
        LOOKUP_CODE_KEYS.firstNotNullOfOrNull { key ->
            json.optString(key).trim().takeIf { it.isNotBlank() }
        }
    }.getOrNull()
}

private fun extractCodeFromUri(text: String): String? = runCatching {
    val uri = Uri.parse(text)
    LOOKUP_CODE_KEYS.firstNotNullOfOrNull { key ->
        uri.getQueryParameter(key)?.trim()?.takeIf { it.isNotBlank() }
    } ?: uri.lastPathSegment
        ?.trim()
        ?.takeIf { uri.scheme == "http" || uri.scheme == "https" }
        ?.takeIf { it.isNotBlank() }
}.getOrNull()

private fun extractCodeFromTextPair(text: String): String? {
    LOOKUP_CODE_KEYS.forEach { key ->
        val equalsPrefix = "$key="
        val colonPrefix = "$key:"
        if (text.startsWith(equalsPrefix, ignoreCase = true)) {
            return text.substring(equalsPrefix.length).trim().takeIf { it.isNotBlank() }
        }
        if (text.startsWith(colonPrefix, ignoreCase = true)) {
            return text.substring(colonPrefix.length).trim().takeIf { it.isNotBlank() }
        }
    }
    return null
}

private fun displayStatus(result: JsonMap): String =
    result.string("pickupStatus").ifBlank { result.string("status") }

private fun entityTypeLabel(type: String): String = when (type) {
    "INBOUND_ORDER" -> "入库单"
    "OUTBOUND_ORDER" -> "出库单"
    "LOCATION" -> "库位"
    "PRODUCT_STOCK" -> "商品库存"
    else -> type.ifBlank { "对象" }
}

private fun statusLabel(status: String): String = when (status) {
    "CREATED" -> "已创建"
    "RECEIVED" -> "已收货"
    "PUTAWAY_COMPLETED" -> "已上架"
    "WAITING_PUTAWAY" -> "待上架"
    "PENDING_PICKUP" -> "待取件"
    "PICKED_UP" -> "已取件"
    "DELAY_REQUESTED" -> "已延迟"
    "REFUSED" -> "已拒收"
    "PICKING_COMPLETED" -> "已拣货"
    "SHIPPED" -> "已发运"
    "COUNTING" -> "盘点中"
    "PENDING" -> "待盘"
    "MATCHED" -> "相符"
    "DIFF" -> "差异"
    "REVIEW_REQUIRED" -> "待复核"
    "FINISHED" -> "已结束"
    "ADJUSTED" -> "已调整"
    "ACTIVE" -> "启用"
    else -> status.ifBlank { "-" }
}

private fun exceptionStatusLabel(status: String): String = when (status) {
    "OPEN" -> "待处理"
    "IN_PROGRESS" -> "处理中"
    "RESOLVED" -> "已解决"
    "CLOSED" -> "已关闭"
    else -> status.ifBlank { "-" }
}

private fun exceptionSeverityLabel(severity: String): String = when (severity) {
    "LOW" -> "低"
    "MEDIUM" -> "中"
    "HIGH" -> "高"
    else -> severity.ifBlank { "-" }
}

private fun approvalStatusLabel(status: String): String = when (status) {
    "NOT_SUBMITTED" -> "未提交"
    "PENDING" -> "待审批"
    "APPROVED" -> "已通过"
    "REJECTED" -> "已驳回"
    else -> status.ifBlank { "-" }
}

private fun approvalTypeLabel(type: String): String = when (type) {
    "ORDER_APPROVAL" -> "订单审批"
    "EXCEPTION_APPROVAL" -> "异常审批"
    "BUSINESS_APPROVAL" -> "业务审批"
    else -> type.ifBlank { "-" }
}

private fun approvalRecordActionLabel(action: String): String = when (action) {
    "SUBMIT" -> "提交"
    "APPROVE" -> "通过"
    "REJECT" -> "驳回"
    else -> action.ifBlank { "-" }
}

private fun bizTypeLabel(type: String): String = when (type) {
    "INBOUND_ORDER" -> "入库单"
    "OUTBOUND_ORDER" -> "出库单"
    "STOCK_TAKE_ORDER" -> "盘点单"
    "EXCEPTION_TICKET" -> "异常工单"
    "MANUAL" -> "手工"
    else -> type.ifBlank { "-" }
}

@Composable
private fun statusColor(status: String): Color = when (status) {
    "PICKED_UP", "SHIPPED", "MATCHED", "FINISHED", "ADJUSTED", "ACTIVE" -> Color(0xFF16835F)
    "REFUSED", "DIFF", "REVIEW_REQUIRED" -> Color(0xFFB42318)
    "PENDING_PICKUP", "WAITING_PUTAWAY", "COUNTING", "PENDING", "PICKING_COMPLETED" -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun entityAccentColor(entityType: String): Color = when (entityType) {
    "INBOUND_ORDER" -> MaterialTheme.colorScheme.primary
    "OUTBOUND_ORDER" -> MaterialTheme.colorScheme.tertiary
    "LOCATION" -> MaterialTheme.colorScheme.secondary
    "PRODUCT_STOCK" -> Color(0xFF16835F)
    else -> statusColor(entityType)
}

private fun quantityRow(item: JsonMap, key: String, label: String): Pair<String, String>? =
    if (item.containsKey(key) && item[key] != null) label to item.decimal(key).cleanText() else null

private fun UserSession?.operatorName(): String =
    this?.displayName?.ifBlank { username }?.ifBlank { "PDA Device" } ?: "PDA Device"

private fun BigDecimal.cleanText(): String = stripTrailingZeros().toPlainString()

private val LOOKUP_CODE_KEYS = listOf(
    "code",
    "barcode",
    "barCode",
    "sku",
    "skuCode",
    "productCode",
    "orderNo",
    "scanCode",
    "pickupCode",
    "logisticsNo"
)

private enum class WorkTab(val title: String, val shortLabel: String) {
    Scan("扫码", "扫"),
    Inbound("入库", "入"),
    Outbound("出库", "出"),
    Stock("库存", "库"),
    StockTake("盘点", "盘"),
    Exception("异常", "异"),
    Approval("审批", "审"),
    Settings("设置", "设")
}

private enum class InboundAction(val doneMessage: String) {
    Receive("收货完成"),
    Putaway("上架完成"),
    Pickup("取件完成"),
    Delay("已延迟取件"),
    Refuse("已登记拒收")
}

private enum class OutboundAction(val doneMessage: String) {
    Picking("拣货完成"),
    Ship("发运完成")
}

private enum class StockTakeOrderAction(val doneMessage: String) {
    Finish("盘点已结束"),
    Adjust("库存已调整")
}

private enum class ExceptionAction(val doneMessage: String) {
    Assign("工单已指派"),
    Resolve("工单已解决"),
    Close("工单已关闭"),
    SubmitApproval("审批已提交")
}

private enum class ApprovalAction(val doneMessage: String) {
    Approve("审批已通过"),
    Reject("审批已驳回")
}
