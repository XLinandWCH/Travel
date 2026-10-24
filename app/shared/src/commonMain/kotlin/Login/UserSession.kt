package com.xlin.travel.login

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.xlin.travel.db.ChatRepository
import kotlinx.serialization.json.Json

/**
 * 登录会话（进程内单例，由 App 创建并持有）。
 *
 * - 用户名 / 手机号 / 邮箱 / token 持久化在本地 app_setting，重启后可恢复登录态；
 * - 头像仅在本次进程内保存（无跨平台 PNG 编解码，重启后回退到用户名首字母）；
 * - 手机号对外一律以 [maskedPhone] 脱敏展示：保留前 3 位与后 2 位，中间打星。
 */
@Stable
class UserSession {

    var loggedIn by mutableStateOf(false)
        private set
    var token by mutableStateOf("")
        private set
    var username by mutableStateOf("")
        private set
    var phone by mutableStateOf("")
        private set
    var email by mutableStateOf("")
        private set

    /** 头像：仅本次运行有效 */
    var avatar by mutableStateOf<ImageBitmap?>(null)

    /** 脱敏手机号：前 3 后 2，其余用 * 替换；长度不足则原样返回 */
    val maskedPhone: String
        get() = if (phone.length <= 5) phone
        else phone.take(3) + "*".repeat(phone.length - 5) + phone.takeLast(2)

    /** 头像缺省占位字：用户名首字母 */
    val avatarInitial: String
        get() = username.firstOrNull()?.uppercaseChar()?.toString() ?: "用"

    private val json = Json { ignoreUnknownKeys = true }

    /** 启动时从数据库恢复登录态 */
    suspend fun load(repository: ChatRepository) {
        val savedToken = repository.getSetting(KEY_TOKEN).orEmpty()
        val savedUser = repository.getSetting(KEY_USER).orEmpty()
        if (savedToken.isBlank() || savedUser.isBlank()) return
        val user = runCatching { json.decodeFromString<UserDto>(savedUser) }.getOrNull() ?: return
        applyUser(user)
        token = savedToken
        loggedIn = true
    }

    /** 登录 / 注册成功：写入内存与数据库 */
    suspend fun signIn(user: UserDto, token: String, repository: ChatRepository) {
        applyUser(user)
        this.token = token
        loggedIn = true
        repository.setSetting(KEY_TOKEN, token)
        repository.setSetting(KEY_USER, json.encodeToString(UserDto.serializer(), user))
    }

    /** 用户名等服务端返回最新资料后刷新并持久化 */
    suspend fun updateUser(user: UserDto, repository: ChatRepository) {
        applyUser(user)
        repository.setSetting(KEY_USER, json.encodeToString(UserDto.serializer(), user))
    }

    /** 退出登录：清除登录态与缓存的账号信息，保留本地聊天记录 */
    suspend fun signOut(repository: ChatRepository) {
        resetInMemory()
        repository.setSetting(KEY_TOKEN, "")
        repository.setSetting(KEY_USER, "")
    }

    /** 注销账号后：彻底清空内存会话（数据库清空由 repository.clearAllLocalData 负责） */
    fun erase() = resetInMemory()

    private fun resetInMemory() {
        loggedIn = false
        token = ""
        username = ""
        phone = ""
        email = ""
        avatar = null
    }

    private fun applyUser(user: UserDto) {
        username = user.username
        phone = user.phone
        email = user.email
    }

    private companion object {
        const val KEY_TOKEN = "auth_token"
        const val KEY_USER = "auth_user"
    }
}
