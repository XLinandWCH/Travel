package com.xlin.travel.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.ChatBackground
import com.xlin.travel.color.FormErrorText
import com.xlin.travel.color.TextGray500
import kotlinx.coroutines.launch

/**
 * 传统登录 / 注册页面：
 * - 默认展示登录表单，邮箱 / 手机号两种方式切换；
 * - 登录 / 注册两种表单通过底部“去注册 / 去登录”文字链切换，不放分段开关；
 * - 本地校验通过后直接调用 server 的 /api/auth 接口，成功即进入应用（onLoggedIn）；
 * - 配色使用 AppColors 全局表，深浅主题自动切换；背景铺满整屏，内容避开系统栏。
 */
@Composable
fun LoginScreen(
    onSkip: () -> Unit,
    onLoggedIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 必须 remember：否则每次重组都会新建实例，表单内容与模式全部被重置
    val state = remember { LoginState() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            .safeContentPadding()
            .imePadding(),
    ) {
        // 顶栏：暂不登录入口
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "暂不登录",
                color = TextGray500,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onSkip)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(36.dp))
            Text("登录后可享受完整服务", color = TextGray500, fontSize = 14.sp)
            Spacer(Modifier.height(30.dp))

            when (state.mode) {
                AuthMode.Login -> LoginForm(state)
                AuthMode.Register -> RegisterForm(state)
            }

            state.notice?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(message, color = FormErrorText, fontSize = 13.sp)
            }
            Spacer(Modifier.height(20.dp))

            PrimaryButton(
                text = if (state.mode == AuthMode.Login) "登 录" else "注 册",
                enabled = !state.submitting,
                onClick = { submit(state, scope, onLoggedIn) },
            )
            if (state.submitting) {
                Spacer(Modifier.height(14.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = AccentOrange,
                    strokeWidth = 2.dp,
                )
            }

            Spacer(Modifier.height(18.dp))
            if (state.mode == AuthMode.Login) {
                SwitchHintRow(hint = "还没有账号？", action = "去注册") {
                    state.switchMode(AuthMode.Register)
                }
            } else {
                SwitchHintRow(hint = "已有账号？", action = "去登录") {
                    state.switchMode(AuthMode.Login)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

/** 提交：本地校验通过后调用后端 /api/auth 注册 / 登录接口，成功直接进入应用 */
private fun submit(
    state: LoginState,
    scope: kotlinx.coroutines.CoroutineScope,
    onLoggedIn: () -> Unit,
) {
    state.notice = null
    if (!state.validate()) return
    state.submitting = true
    scope.launch {
        val result = when (state.mode) {
            AuthMode.Login -> AuthApi.login(state.account, state.password)
            AuthMode.Register -> AuthApi.register(
                username = state.username,
                phone = state.regPhone,
                email = state.regEmail,
                password = state.regPassword,
            )
        }
        state.submitting = false
        when (result) {
            is AuthApiResult.Success -> onLoggedIn()
            is AuthApiResult.Failed -> state.notice = result.message
        }
    }
}
