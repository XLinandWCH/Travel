package com.xlin.travel.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xlin.travel.color.AccentOrange
import com.xlin.travel.color.FormErrorText
import com.xlin.travel.color.InputCardBackground
import com.xlin.travel.color.InputPlaceholderColor
import com.xlin.travel.color.InputTextColor
import com.xlin.travel.color.SettingItemPressed
import com.xlin.travel.color.TextGray500
import com.xlin.travel.color.WhiteBtnBackground
import com.xlin.travel.color.WhiteBtnIcon
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import travel.app.shared.generated.resources.Res

/**
 * 登录 / 注册页共用表单组件：
 * 卡片风输入框（前导图标 + 密码可见切换 + 字段错误红字）、分段切换、主按钮。
 * 配色全部取自 AppColors，跟随深浅主题。
 */

/** 表单输入框：单行，左侧图标，密码类型支持“显示 / 隐藏”切换 */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    icon: DrawableResource,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorText: String? = null,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(shape)
                .background(InputCardBackground)
                .border(
                    width = if (errorText != null) 1.5.dp else 1.dp,
                    color = if (errorText != null) FormErrorText else Color.Transparent,
                    shape = shape,
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(TextGray500),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 14.dp),
                singleLine = true,
                textStyle = TextStyle(color = InputTextColor, fontSize = 15.sp),
                cursorBrush = SolidColor(AccentOrange),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                ),
                visualTransformation = when {
                    !isPassword -> VisualTransformation.None
                    passwordVisible -> VisualTransformation.None
                    else -> PasswordVisualTransformation()
                },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(placeholder, color = InputPlaceholderColor, fontSize = 15.sp)
                        }
                        inner()
                    }
                },
            )
            // 密码可见切换（无独立图标资源，用文字按钮）
            if (isPassword) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (passwordVisible) "隐藏" else "显示",
                    color = TextGray500,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { passwordVisible = !passwordVisible }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
        if (errorText != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = errorText,
                color = FormErrorText,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

/** 分段切换（登录 / 注册，邮箱 / 手机号共用）：选中段用主按钮反色底 */
@Composable
fun SegmentSwitch(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(InputCardBackground)
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Text(
                text = label,
                color = if (selected) WhiteBtnIcon else TextGray500,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) WhiteBtnBackground else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 18.dp, vertical = 7.dp),
            )
        }
    }
}

/** 主按钮：整宽，按压反馈沿用 SettingItemPressed 规则；提交中降低透明度 */
@Composable
fun PrimaryButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(12.dp))
            .background(WhiteBtnBackground)
            .background(if (pressed && enabled) SettingItemPressed else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = WhiteBtnIcon,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** 底部辅助行：如“还没有账号？去注册”，链接部分染强调色 */
@Composable
fun SwitchHintRow(
    hint: String,
    action: String,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(hint, color = TextGray500, fontSize = 13.sp)
        Text(
            text = action,
            color = AccentOrange,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onAction)
                .padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}
