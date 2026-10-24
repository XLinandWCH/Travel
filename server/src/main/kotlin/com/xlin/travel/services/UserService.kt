package com.xlin.travel.services

import com.xlin.travel.db.Users
import com.xlin.travel.models.LoginRequest
import com.xlin.travel.models.RegisterRequest
import com.xlin.travel.models.UserDto
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 登录 / 注册业务：字段校验、BCrypt 密码哈希、users 表读写、会话 token 签发。
 * 目前只有账号服务接入数据库，模型密钥与反馈服务后续在此层扩展。
 */
object UserService {

    private val EMAIL_REGEX = Regex("^[\\w.%+-]+@[\\w-]+(\\.[\\w-]+)+$")
    private val PHONE_REGEX = Regex("^1\\d{10}$")

    /** 业务结果：成功携带 UserDto，失败携带 HTTP 状态码与提示 */
    sealed interface AuthResult {
        data class Success(val user: UserDto) : AuthResult
        data class Failed(val status: Int, val message: String) : AuthResult
    }

    // ==================== 注册 ====================

    fun register(request: RegisterRequest): AuthResult {
        val username = request.username.trim()
        val phone = request.phone.trim()
        val email = request.email.trim().lowercase()
        val password = request.password

        // 与客户端 LoginState 同规则的兜底校验（防止绕过前端直接调接口）
        when {
            username.length !in 2..32 -> return AuthResult.Failed(400, "用户名长度需为 2~32 位")
            !EMAIL_REGEX.matches(email) -> return AuthResult.Failed(400, "邮箱格式不正确")
            !PHONE_REGEX.matches(phone) -> return AuthResult.Failed(400, "手机号格式不正确")
            !isPasswordValid(password) -> return AuthResult.Failed(400, "密码需为 6~32 位且包含字母和数字")
        }

        return try {
            transaction {
                val duplicated = Users.selectAll().where {
                    (Users.username eq username) or (Users.phone eq phone) or (Users.email eq email)
                }.firstOrNull()
                if (duplicated != null) {
                    val field = when (duplicated[Users.username]) {
                        username -> "用户名"
                        else -> if (duplicated[Users.phone] == phone) "手机号" else "邮箱"
                    }
                    return@transaction AuthResult.Failed(409, "该${field}已被注册")
                }
                val newId = Users.insert {
                    it[Users.username] = username
                    it[Users.phone] = phone
                    it[Users.email] = email
                    it[Users.passwordHash] = BCrypt.hashpw(password, BCrypt.gensalt(10))
                }[Users.id]
                // 回读完整行（含默认值列），保证返回的注册时间 / 使用天数准确
                val row = Users.selectAll().where { Users.id eq newId }.single()
                AuthResult.Success(row.toUserDto())
            }
        } catch (e: Exception) {
            AuthResult.Failed(500, "注册失败：${e.message}")
        }
    }

    // ==================== 登录 ====================

    /** account 同时支持邮箱与手机号 */
    fun login(request: LoginRequest): AuthResult {
        val account = request.account.trim()
        if (account.isBlank() || request.password.isBlank()) {
            return AuthResult.Failed(400, "账号与密码不能为空")
        }
        return try {
            transaction {
                val row = Users.selectAll().where {
                    (Users.email eq account.lowercase()) or (Users.phone eq account)
                }.firstOrNull()
                    ?: return@transaction AuthResult.Failed(401, "账号不存在或密码错误")
                if (!BCrypt.checkpw(request.password, row[Users.passwordHash])) {
                    // 不区分“账号错”与“密码错”，避免账号探测
                    return@transaction AuthResult.Failed(401, "账号不存在或密码错误")
                }
                AuthResult.Success(row.toUserDto())
            }
        } catch (e: Exception) {
            AuthResult.Failed(500, "登录失败：${e.message}")
        }
    }

    // ==================== token（HMAC 签名，暂不含会话存储） ====================

    private val tokenSecret: ByteArray =
        (System.getenv("SERVER_SECRET") ?: "travel-dev-secret-change-me").toByteArray()

    /** 签发 token：base64url(userId.exp秒) + "." + base64url(HMAC-SHA256) */
    fun issueToken(userId: Long, expireSeconds: Long = 30 * 24 * 3600): String {
        val payload = "$userId.${System.currentTimeMillis() / 1000 + expireSeconds}"
        val body = payload.base64Url()
        return "$body.${hmac(body).base64Url()}"
    }

    /** 校验 token 并返回 userId，无效返回 null（后续受保护接口使用） */
    fun verifyToken(token: String): Long? {
        val parts = token.split(".")
        if (parts.size != 2) return null
        if (!hmac(parts[0]).contentEquals(parts[1].base64UrlDecode())) return null
        val payload = runCatching { String(Base64.getUrlDecoder().decode(parts[0])) }.getOrNull() ?: return null
        val fields = payload.split(".")
        if (fields.size != 2) return null
        val expireAt = fields[1].toLongOrNull() ?: return null
        if (expireAt < System.currentTimeMillis() / 1000) return null
        return fields[0].toLongOrNull()
    }

    // ==================== 内部工具 ====================

    private fun isPasswordValid(text: String): Boolean =
        text.length in 6..32 && text.any { it.isLetter() } && text.any { it.isDigit() }

    private fun hmac(text: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(tokenSecret, "HmacSHA256"))
        return mac.doFinal(text.toByteArray())
    }

    private fun String.base64Url(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray())

    private fun ByteArray.base64Url(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(this)

    private fun String.base64UrlDecode(): ByteArray =
        Base64.getUrlDecoder().decode(this)

    private fun ResultRow.toUserDto() = UserDto(
        id = this[Users.id],
        username = this[Users.username],
        phone = this[Users.phone],
        email = this[Users.email],
        usageDays = this[Users.usageDays],
        registeredAt = this[Users.registeredAt].toString(),
    )
}
