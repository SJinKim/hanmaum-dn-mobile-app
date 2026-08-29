package com.hanmaum.dn.mobile.features.login.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnPrimaryButton
import com.hanmaum.dn.mobile.core.presentation.components.DnTextField
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.i18n.AppStrings
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import org.koin.compose.viewmodel.koinViewModel

/**
 * 회원가입 — the fields the register endpoint actually accepts. Optional
 * extras (baptism, gender) were dropped from the first screen; they can be
 * filled in later from the profile.
 */
@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onNavigateToPending: () -> Unit,
) {
    val viewModel: RegisterViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val c = DnTheme.colors

    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let {
            onNavigateToPending()
            viewModel.onNavigationHandled()
        }
    }

    DnBackground(glows = DnGlows.action()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(title = "회원가입", onBack = onBackClick, actionIcon = null)

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                Spacer(Modifier.height(18.dp))
                Text("함께 시작해요", style = DnTheme.typography.titleLg, color = c.textPrimary)
                Spacer(Modifier.height(6.dp))
                Text(
                    "가입 신청 후 담당자의 승인을 거쳐 이용하실 수 있습니다.",
                    style = DnTheme.typography.caption,
                    color = c.textSecondary,
                )

                Spacer(Modifier.height(22.dp))

                DnTextField("성", state.lastName, viewModel::onLastNameChange,
                    Modifier.fillMaxWidth(), placeholder = "성")
                FieldError(strings.messageFor(state.lastNameError))
                Spacer(Modifier.height(16.dp))
                DnTextField("이름", state.firstName, viewModel::onFirstNameChange,
                    Modifier.fillMaxWidth(), placeholder = "이름")
                FieldError(strings.messageFor(state.firstNameError))
                Spacer(Modifier.height(16.dp))
                DnTextField("이메일", state.email, viewModel::onEmailChange,
                    Modifier.fillMaxWidth(), placeholder = "hello@hanmaum.de",
                    leading = DnIcons.Mail, keyboardType = KeyboardType.Email)
                FieldError(strings.messageFor(state.emailError))
                Spacer(Modifier.height(16.dp))
                DnTextField("전화번호", state.phoneNumber, viewModel::onPhoneChange,
                    Modifier.fillMaxWidth(), placeholder = "+49 …", keyboardType = KeyboardType.Phone)
                Spacer(Modifier.height(16.dp))
                DnTextField("비밀번호", state.password, viewModel::onPasswordChange,
                    Modifier.fillMaxWidth(), placeholder = "••••••••",
                    leading = DnIcons.Lock, trailing = DnIcons.Eye,
                    isPassword = true, keyboardType = KeyboardType.Password)
                FieldError(strings.messageFor(state.passwordError))
                Spacer(Modifier.height(16.dp))
                DnTextField("생년월일", state.birthDate, viewModel::onBirthDateChange,
                    Modifier.fillMaxWidth(), placeholder = "2000.01.01",
                    trailing = DnIcons.Calendar, keyboardType = KeyboardType.Number)
                FieldError(strings.messageFor(state.birthDateError))
                Spacer(Modifier.height(16.dp))
                DnTextField("도로명", state.street, viewModel::onStreetChange,
                    Modifier.fillMaxWidth(), placeholder = "Musterstraße 12")
                FieldError(strings.messageFor(state.streetError))
                Spacer(Modifier.height(16.dp))
                DnTextField("우편번호", state.zipCode, viewModel::onZipChange,
                    Modifier.fillMaxWidth(), placeholder = "40210", keyboardType = KeyboardType.Number)
                FieldError(strings.messageFor(state.zipCodeError))
                Spacer(Modifier.height(16.dp))
                DnTextField("도시", state.city, viewModel::onCityChange,
                    Modifier.fillMaxWidth(), placeholder = "Düsseldorf")

                FieldError(strings.messageFor(state.cityError))
                state.bannerError?.let { banner ->
                    // RegisteredPleaseLogin is a success that needs one more
                    // step, not a failure — it must not read as red.
                    val isPositive = banner is RegisterBanner.RegisteredPleaseLogin
                    Spacer(Modifier.height(12.dp))
                    Text(
                        when (banner) {
                            is RegisterBanner.ServerMessage -> banner.text
                            RegisterBanner.Generic -> strings.registerFailed
                            RegisterBanner.RegisteredPleaseLogin -> strings.registerSuccessLogin
                        },
                        style = DnTheme.typography.caption,
                        color = if (isPositive) c.limeInk else c.red,
                    )
                }

                Spacer(Modifier.height(26.dp))
                DnPrimaryButton(
                    label = if (state.isLoading) "신청 중…" else "가입 신청하기",
                    onClick = viewModel::register,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

/**
 * One field's validation message, or nothing at all.
 *
 * A composable rather than an inline `let` so every field renders the same
 * way — the redesign originally showed a message only for the birth date,
 * which quietly dropped the other seven the ViewModel already produces.
 */
@Composable
private fun FieldError(message: String?) {
    if (message == null) return
    Spacer(Modifier.height(6.dp))
    Text(message, style = DnTheme.typography.caption, color = DnTheme.colors.red)
}

/** Resolves a language-independent field error to localized text. */
private fun AppStrings.messageFor(error: RegisterFieldError?): String? = when (error) {
    RegisterFieldError.REQUIRED -> errorRequired
    RegisterFieldError.INVALID_EMAIL -> errorInvalidEmail
    RegisterFieldError.PASSWORD_REQUIREMENTS -> errorPasswordRequirements
    RegisterFieldError.DATE_INCOMPLETE -> errorDateIncomplete
    RegisterFieldError.DATE_INVALID -> errorDateInvalid
    RegisterFieldError.INVALID_POSTCODE -> errorInvalidPostcode
    RegisterFieldError.INVALID_CITY -> errorInvalidCity
    RegisterFieldError.INVALID_HOUSE_NUMBER -> errorInvalidHouseNumber
    null -> null
}
