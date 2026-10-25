package com.xlin.travel.login

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.xlin.travel.agent.ModelConfigStore
import com.xlin.travel.db.ChatRepository
import com.xlin.travel.db.SettingKeys
import com.xlin.travel.image_picker.decodeAvatar
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

    /** 是否管理员（users.type == "admin"）：随登录/资料刷新同步，决定消息中心展示视图 */
    var isAdmin by mutableStateOf(false)
        private set

    /** 头像预览位图：由 [avatarBase64] 解码得到，仅本次进程内使用（不直接持久化） */
    var avatar by mutableStateOf<ImageBitmap?>(null)
        private set

    /** 头像 base64（PNG，已缩放到 ≤512px）：持久化到本地 app_setting，服务器另存一份用于跨设备同步 */
    var avatarBase64 by mutableStateOf<String?>(null)
        private set

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
        val savedToken = repository.getSetting(SettingKeys.AuthToken).orEmpty()
        val savedUser = repository.getSetting(KEY_USER).orEmpty()
        if (savedToken.isBlank() || savedUser.isBlank()) return
        val user = runCatching { json.decodeFromString<UserDto>(savedUser) }.getOrNull() ?: return
        applyUser(user)
        token = savedToken
        loggedIn = true
        // 优先用本地缓存的头像 base64，缺失时回退服务端随登录下发的 user.avatar
        val savedAvatar = repository.getSetting(KEY_AVATAR)
        restoreAvatar(savedAvatar?.takeIf { it.isNotBlank() } ?: user.avatar)
    }

    /** 登录 / 注册成功：写入内存与数据库 */
    suspend fun signIn(user: UserDto, token: String, repository: ChatRepository) {
        applyUser(user)
        this.token = token
        loggedIn = true
        repository.setSetting(SettingKeys.AuthToken, token)
        repository.setSetting(KEY_USER, json.encodeToString(UserDto.serializer(), user))
        // 服务端随登录下发的头像（若有）缓存到本地，离线也能立即显示
        restoreAvatar(user.avatar)
        user.avatar?.let { repository.setSetting(KEY_AVATAR, it) }
    }

    /** 设置新头像：本地立即生效 + 持久化（服务器上传由调用方负责，失败不影响本地） */
    suspend fun setAvatar(base64: String, repository: ChatRepository) {
        restoreAvatar(base64)
        repository.setSetting(KEY_AVATAR, base64)
    }

    /** 用户名等服务端返回最新资料后刷新并持久化 */
    suspend fun updateUser(user: UserDto, repository: ChatRepository) {
        applyUser(user)
        repository.setSetting(KEY_USER, json.encodeToString(UserDto.serializer(), user))
    }

    /** 退出登录：清除登录态与缓存的账号信息，保留本地聊天记录 */
    suspend fun signOut(repository: ChatRepository) {
        resetInMemory()
        repository.setSetting(SettingKeys.AuthToken, "")
        repository.setSetting(KEY_USER, "")
        repository.setSetting(KEY_AVATAR, "")
        // 密钥类缓存一并作废：内存副本清空 + 本地缓存置空，下次登录重新向服务端拉取
        ModelConfigStore.forget()
        repository.setSetting(SettingKeys.ModelConfigCache, "")
    }

    /** 注销账号后：彻底清空内存会话（数据库清空由 repository.clearAllLocalData 负责） */
    fun erase() {
        resetInMemory()
        ModelConfigStore.forget()
    }

    private fun resetInMemory() {
        loggedIn = false
        token = ""
        username = ""
        phone = ""
        email = ""
        isAdmin = false
        avatar = null
        avatarBase64 = null
    }

    /** 解码 base64 为预览位图并更新头像状态 */
    private fun restoreAvatar(base64: String?) {
        avatarBase64 = base64?.takeIf { it.isNotBlank() }
        avatar = avatarBase64?.let { runCatching { decodeAvatar(it) }.getOrNull() }
    }

    private fun applyUser(user: UserDto) {
        username = user.username
        phone = user.phone
        email = user.email
        isAdmin = user.type == "admin"
    }

    private companion object {
        const val KEY_USER = "auth_user"
        const val KEY_AVATAR = "auth_avatar"
    }
}
