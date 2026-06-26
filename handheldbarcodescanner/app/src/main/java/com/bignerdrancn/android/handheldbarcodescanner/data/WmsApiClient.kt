package com.bignerdrancn.android.handheldbarcodescanner.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import javax.net.ssl.SSLHandshakeException

typealias JsonMap = Map<String, Any?>

class ApiException(
    override val message: String,
    val statusCode: Int? = null
) : Exception(message)

data class ConnectionCheck(
    val reachable: Boolean,
    val statusCode: Int?,
    val message: String
)

class WmsApiClient(
    baseUrl: String,
    private val tokenProvider: () -> String?
) {
    private val baseUrl = SessionStore.normalizeBaseUrl(baseUrl)

    suspend fun login(username: String, password: String): UserSession {
        val payload = JSONObject()
            .put("username", username)
            .put("password", password)
        val data = post("/auth/login", payload).asMap()
        return UserSession(
            token = data.string("token"),
            userId = data.long("userId"),
            username = data.string("username"),
            displayName = data.string("displayName").ifBlank { data.string("username") },
            roleCode = data.string("roleCode"),
            menuPaths = data.list("menuPaths").mapNotNull { it?.toString() }
        )
    }

    suspend fun lookup(code: String): JsonMap =
        get("/scan/lookup", mapOf("code" to code)).asMap()

    suspend fun recordScan(
        rawContent: String,
        scanFormat: String = "AUTO",
        sourceDevice: String = "ANDROID_HANDHELD",
        operatorName: String
    ): JsonMap {
        val payload = JSONObject()
            .put("rawContent", rawContent)
            .put("scanFormat", scanFormat)
            .put("sourceDevice", sourceDevice)
            .put("operatorName", operatorName)
        return post("/scan/records", payload).asMap()
    }

    suspend fun inboundDetail(id: Long): JsonMap =
        get("/inbounds/$id").asMap()

    suspend fun inbounds(): List<JsonMap> =
        get("/inbounds").asMapList()

    suspend fun receiveInbound(id: Long): JsonMap =
        post("/inbounds/$id/receive").asMap()

    suspend fun putawayInbound(id: Long): JsonMap =
        post("/inbounds/$id/putaway").asMap()

    suspend fun scanInboundPutaway(
        rawContent: String,
        scanFormat: String = "AUTO",
        sourceDevice: String = "ANDROID_HANDHELD",
        operatorName: String
    ): JsonMap {
        val payload = JSONObject()
            .put("rawContent", rawContent)
            .put("scanFormat", scanFormat)
            .put("sourceDevice", sourceDevice)
            .put("operatorName", operatorName)
            .put("scannerInterface", "ANDROID_CAMERA")
            .put("remark", "手持扫码枪入库贴码扫码确认")
        return post("/inbounds/scan-putaway", payload).asMap()
    }

    suspend fun scanTransferOutbound(
        rawContent: String,
        scanFormat: String = "AUTO",
        sourceDevice: String = "ANDROID_HANDHELD",
        operatorName: String
    ): JsonMap {
        val payload = JSONObject()
            .put("rawContent", rawContent)
            .put("scanFormat", scanFormat)
            .put("sourceDevice", sourceDevice)
            .put("operatorName", operatorName)
            .put("scannerInterface", "ANDROID_CAMERA")
            .put("remark", "手持扫码枪从入库货物转出库")
        return post("/outbounds/scan-transfer", payload).asMap()
    }

    suspend fun pickupAction(
        id: Long,
        action: String,
        operatorName: String,
        extendMinutes: Int?,
        remark: String
    ): JsonMap {
        val payload = JSONObject()
            .put("action", action)
            .put("operatorName", operatorName)
            .put("remark", remark)
        if (extendMinutes != null) {
            payload.put("extendMinutes", extendMinutes)
        }
        return post("/inbounds/$id/pickup-action", payload).asMap()
    }

    suspend fun outboundDetail(id: Long): JsonMap =
        get("/outbounds/$id").asMap()

    suspend fun outbounds(): List<JsonMap> =
        get("/outbounds").asMapList()

    suspend fun pickingOutbound(id: Long): JsonMap =
        post("/outbounds/$id/picking").asMap()

    suspend fun shipOutbound(id: Long): JsonMap =
        post("/outbounds/$id/ship").asMap()

    suspend fun scanShipOutbound(
        id: Long,
        rawContent: String,
        scanFormat: String = "AUTO",
        sourceDevice: String = "ANDROID_HANDHELD",
        operatorName: String
    ): JsonMap {
        val payload = JSONObject()
            .put("rawContent", rawContent)
            .put("scanFormat", scanFormat)
            .put("sourceDevice", sourceDevice)
            .put("operatorName", operatorName)
        return post("/outbounds/$id/scan-ship", payload).asMap()
    }

    suspend fun stocks(keyword: String = "", warehouseId: Long? = null): List<JsonMap> =
        get(
            "/stocks",
            mapOf(
                "keyword" to keyword.takeIf { it.isNotBlank() },
                "warehouseId" to warehouseId?.toString()
            )
        ).asMapList()

    suspend fun stockTakes(): List<JsonMap> =
        get("/stock-takes").asMapList()

    suspend fun stockTakeDetail(id: Long): JsonMap =
        get("/stock-takes/$id").asMap()

    suspend fun countStockTakeItem(
        stockTakeId: Long,
        itemId: Long,
        actualQty: BigDecimal,
        remark: String
    ): JsonMap {
        val item = JSONObject()
            .put("id", itemId)
            .put("actualQty", actualQty)
            .put("remark", remark)
        val payload = JSONObject().put("items", JSONArray().put(item))
        return post("/stock-takes/$stockTakeId/count", payload).asMap()
    }

    suspend fun finishStockTake(id: Long): JsonMap =
        post("/stock-takes/$id/finish").asMap()

    suspend fun adjustStockTake(id: Long): JsonMap =
        post("/stock-takes/$id/adjust").asMap()

    suspend fun exceptions(): List<JsonMap> =
        get("/exceptions").asMapList()

    suspend fun assignException(id: Long, assigneeName: String): JsonMap =
        post("/exceptions/$id/assign", mapOf("assigneeName" to assigneeName)).asMap()

    suspend fun resolveException(id: Long, operatorName: String): JsonMap =
        post("/exceptions/$id/resolve", mapOf("operatorName" to operatorName)).asMap()

    suspend fun closeException(id: Long): JsonMap =
        post("/exceptions/$id/close").asMap()

    suspend fun submitExceptionApproval(
        ticketId: Long,
        ticketNo: String,
        applicantName: String,
        approverName: String,
        reason: String
    ): JsonMap {
        val payload = JSONObject()
            .put("approvalType", "EXCEPTION_APPROVAL")
            .put("bizType", "EXCEPTION_TICKET")
            .put("bizId", ticketId)
            .put("bizNo", ticketNo)
            .put("applicantName", applicantName)
            .put("approverName", approverName)
            .put("applyReason", reason)
        return post("/approvals/submit", payload).asMap()
    }

    suspend fun approvals(): List<JsonMap> =
        get("/approvals").asMapList()

    suspend fun approve(id: Long, operatorName: String, comment: String): JsonMap {
        val payload = JSONObject()
            .put("operatorName", operatorName)
            .put("comment", comment)
        return post("/approvals/$id/approve", payload).asMap()
    }

    suspend fun reject(id: Long, operatorName: String, comment: String): JsonMap {
        val payload = JSONObject()
            .put("operatorName", operatorName)
            .put("comment", comment)
        return post("/approvals/$id/reject", payload).asMap()
    }

    suspend fun checkAuthenticated(): Boolean = try {
        get("/lookups")
        true
    } catch (exception: ApiException) {
        false
    }

    suspend fun testConnection(): ConnectionCheck = withContext(Dispatchers.IO) {
        try {
            val connection = buildUrl("/lookups", emptyMap()).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                connection.setRequestProperty("Accept", "application/json")
                tokenProvider()?.takeIf { it.isNotBlank() }?.let { token ->
                    connection.setRequestProperty("Authorization", "Bearer $token")
                }

                val statusCode = connection.responseCode
                val raw = readBody(if (statusCode in 200..299) connection.inputStream else connection.errorStream)
                val serverMessage = parseServerMessage(raw)
                when (statusCode) {
                    in 200..299 -> ConnectionCheck(true, statusCode, "服务连接正常")
                    HttpURLConnection.HTTP_UNAUTHORIZED -> ConnectionCheck(true, statusCode, "服务可达，请登录")
                    HttpURLConnection.HTTP_FORBIDDEN -> ConnectionCheck(true, statusCode, "服务可达，当前账号无权限")
                    HttpURLConnection.HTTP_NOT_FOUND -> ConnectionCheck(false, statusCode, "地址已连通，但未找到 /api 接口，请检查服务地址")
                    else -> ConnectionCheck(false, statusCode, serverMessage.ifBlank { "服务响应异常 HTTP $statusCode" })
                }
            } finally {
                connection.disconnect()
            }
        } catch (exception: UnknownHostException) {
            ConnectionCheck(false, null, connectionFailureMessage(exception))
        } catch (exception: ConnectException) {
            ConnectionCheck(false, null, connectionFailureMessage(exception))
        } catch (exception: SocketTimeoutException) {
            ConnectionCheck(false, null, connectionFailureMessage(exception))
        } catch (exception: SSLHandshakeException) {
            ConnectionCheck(false, null, connectionFailureMessage(exception))
        } catch (exception: IOException) {
            ConnectionCheck(false, null, connectionFailureMessage(exception))
        }
    }

    private suspend fun get(path: String, query: Map<String, String?> = emptyMap()): Any? =
        request("GET", path, query, null)

    private suspend fun post(path: String, body: JSONObject? = null): Any? =
        post(path, emptyMap(), body)

    private suspend fun post(path: String, query: Map<String, String?>, body: JSONObject? = null): Any? =
        request("POST", path, query, body ?: JSONObject())

    private suspend fun request(
        method: String,
        path: String,
        query: Map<String, String?>,
        body: JSONObject?
    ): Any? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = buildUrl(path, query).openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.name())

            val token = tokenProvider()
            if (!token.isNullOrBlank() && !path.startsWith("/auth/")) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }

            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { stream ->
                    stream.write(body.toString().toByteArray(StandardCharsets.UTF_8))
                }
            }

            val statusCode = connection.responseCode
            val raw = readBody(if (statusCode in 200..299) connection.inputStream else connection.errorStream)

            if (raw.isBlank()) {
                if (statusCode in 200..299) {
                    return@withContext null
                }
                throw ApiException("请求失败 HTTP $statusCode", statusCode)
            }

            val envelope = try {
                JSONObject(raw)
            } catch (exception: Exception) {
                throw ApiException("服务端返回不是 JSON: ${exception.message}", statusCode)
            }

            val message = envelope.optString("message")
            if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw ApiException(message.ifBlank { "登录已过期，请重新登录" }, statusCode)
            }
            if (statusCode !in 200..299) {
                throw ApiException(message.ifBlank { "请求失败 HTTP $statusCode" }, statusCode)
            }
            if (!envelope.optBoolean("success", true)) {
                throw ApiException(message.ifBlank { "请求失败" }, statusCode)
            }

            jsonToKotlin(envelope.opt("data"))
        } catch (exception: ApiException) {
            throw exception
        } catch (exception: MalformedURLException) {
            throw ApiException(connectionFailureMessage(exception))
        } catch (exception: UnknownHostException) {
            throw ApiException(connectionFailureMessage(exception))
        } catch (exception: ConnectException) {
            throw ApiException(connectionFailureMessage(exception))
        } catch (exception: SocketTimeoutException) {
            throw ApiException(connectionFailureMessage(exception))
        } catch (exception: SSLHandshakeException) {
            throw ApiException(connectionFailureMessage(exception))
        } catch (exception: IOException) {
            throw ApiException(connectionFailureMessage(exception))
        } finally {
            connection?.disconnect()
        }
    }

    private fun buildUrl(path: String, query: Map<String, String?>): URL {
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val queryText = query.entries
            .filter { !it.value.isNullOrBlank() }
            .joinToString("&") { (key, value) ->
                "${key.urlEncode()}=${value.orEmpty().urlEncode()}"
            }
        val suffix = if (queryText.isBlank()) "" else "?$queryText"
        return URL("$baseUrl$normalizedPath$suffix")
    }

    private fun readBody(stream: InputStream?): String {
        if (stream == null) {
            return ""
        }
        return stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }

    private fun parseServerMessage(raw: String): String {
        if (raw.isBlank()) {
            return ""
        }
        return runCatching { JSONObject(raw).optString("message") }.getOrDefault("")
    }

    private fun String.urlEncode(): String =
        URLEncoder.encode(this, StandardCharsets.UTF_8.name())

    private fun connectionFailureMessage(exception: Exception): String = when (exception) {
        is MalformedURLException -> "服务地址格式不正确，请填写类似 http://10.0.2.2:18080/api 的地址"
        is UnknownHostException -> "无法解析服务器域名，请检查服务地址"
        is ConnectException -> "无法连接服务器端口。Android 模拟器访问本机请使用 http://10.0.2.2:18080/api，并确认后端已启动"
        is SocketTimeoutException -> "连接服务器超时，请检查后端服务和网络"
        is SSLHandshakeException -> "HTTPS 证书不被手机信任"
        is IOException -> exception.message ?: "网络连接失败"
        else -> exception.message ?: "请求失败"
    }
}

@Suppress("UNCHECKED_CAST")
fun Any?.asMap(): JsonMap = this as? JsonMap ?: emptyMap()

@Suppress("UNCHECKED_CAST")
fun Any?.asMapList(): List<JsonMap> =
    (this as? List<*>)?.mapNotNull { it.asMap().takeIf { item -> item.isNotEmpty() } }.orEmpty()

fun JsonMap.string(key: String): String = this[key]?.toString().orEmpty()

fun JsonMap.long(key: String): Long = when (val value = this[key]) {
    is Number -> value.toLong()
    is String -> value.toLongOrNull() ?: 0L
    else -> 0L
}

fun JsonMap.decimal(key: String): BigDecimal = when (val value = this[key]) {
    is BigDecimal -> value
    is Number -> BigDecimal(value.toString())
    is String -> value.toBigDecimalOrNull() ?: BigDecimal.ZERO
    else -> BigDecimal.ZERO
}

@Suppress("UNCHECKED_CAST")
fun JsonMap.list(key: String): List<Any?> = this[key] as? List<Any?> ?: emptyList()

fun JsonMap.mapList(key: String): List<JsonMap> = this[key].asMapList()

private fun jsonToKotlin(value: Any?): Any? = when (value) {
    null, JSONObject.NULL -> null
    is JSONObject -> value.toMap()
    is JSONArray -> value.toList()
    else -> value
}

private fun JSONObject.toMap(): JsonMap {
    val result = linkedMapOf<String, Any?>()
    val keys = keys()
    while (keys.hasNext()) {
        val key = keys.next()
        result[key] = jsonToKotlin(opt(key))
    }
    return result
}

private fun JSONArray.toList(): List<Any?> {
    val result = mutableListOf<Any?>()
    for (index in 0 until length()) {
        result += jsonToKotlin(opt(index))
    }
    return result
}
