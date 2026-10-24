package com.xlin.travel.login

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 页面模式：登录 / 注册 */
enum class AuthMode { Login, Register }

/** 登录方式：邮箱 / 手机号（暂时只支持这两种） */
enum class AccountType { Email, Phone }

/**
 * 登录 / 注册表单状态。
 * 只负责字段缓存与本地校验，网络对接后端 /api/auth 接口在 LoginScreen 提交回调里进行。
 */
@Stable
class LoginState {

    var mode by mutableStateOf(AuthMode.Login)
        private set
    var accountType by mutableStateOf(AccountType.Email)
        private set

    // ---- 登录字段 ----
    var account by mutableStateOf("")
    var password by mutableStateOf("")

    // ---- 注册字段 ----
    var username by mutableStateOf("")
    var regEmail by mutableStateOf("")
    var regPhone by mutableStateOf("")
    var regPassword by mutableStateOf("")
    var regPasswordAgain by mutableStateOf("")

    /** 每个字段的错误提示（key 为字段名），为空表示无错误 */
    var errors by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    /** 提交结果提示（成功 / 失败原因），null 不显示 */
    var notice by mutableStateOf<String?>(null)

    /** 正在提交（对接后端时置为 loading） */
    var submitting by mutableStateOf(false)

    fun switchMode(target: AuthMode) {
        if (mode == target) return
        mode = target
        notice = null
        errors = emptyMap()
    }

    fun switchAccountType(target: AccountType) {
        if (accountType == target) return
        accountType = target
        errors = emptyMap()
    }

    // ==================== 校验 ====================

    /** 邮箱：简单格式校验即可，注册与登录共用 */
    private fun isValidEmail(text: String): Boolean =
        Regex("^[\\w.%+-]+@[\\w-]+(\\.[\\w-]+)+$").matches(text.trim())

    /** 手机号：中国大陆 11 位，1 开头 */
    private fun isValidPhone(text: String): Boolean =
        Regex("^1\\d{10}$").matches(text.trim())

    /** 密码：6~32 位，至少包含字母与数字 */
    private fun isValidPassword(text: String): Boolean =
        text.length in 6..32 &&
            text.any { it.isLetter() } && text.any { it.isDigit() }

    /** 提交前整表校验，错误写入 [errors]，返回是否通过 */
    fun validate(): Boolean {
        val map = mutableMapOf<String, String>()
        if (mode == AuthMode.Login) {
            when (accountType) {
                AccountType.Email ->
                    if (account.isBlank()) map["account"] = "请输入邮箱"
                    else if (!isValidEmail(account)) map["account"] = "邮箱格式不正确"
                AccountType.Phone ->
                    if (account.isBlank()) map["account"] = "请输入手机号"
                    else if (!isValidPhone(account)) map["account"] = "手机号格式不正确"
            }
            if (password.isBlank()) map["password"] = "请输入密码"
        } else {
            if (username.isBlank()) map["username"] = "请输入用户名"
            else if (username.length !in 2..32) map["username"] = "用户名长度 2~32 位"
            if (regEmail.isBlank()) map["email"] = "请输入邮箱"
            else if (!isValidEmail(regEmail)) map["email"] = "邮箱格式不正确"
            if (regPhone.isBlank()) map["phone"] = "请输入手机号"
            else if (!isValidPhone(regPhone)) map["phone"] = "手机号格式不正确"
            if (regPassword.isBlank()) map["password"] = "请设置密码"
            else if (!isValidPassword(regPassword)) map["password"] = "密码 6~32 位，需含字母和数字"
            if (regPasswordAgain != regPassword) map["passwordAgain"] = "两次输入的密码不一致"
        }
        errors = map
        return map.isEmpty()
    }

    /** 输入过程中清掉当前字段的错误，避免红字常驻 */
    fun clearError(field: String) {
        if (errors.containsKey(field)) errors = errors - field
    }

    fun fieldError(field: String): String? = errors[field]
}
