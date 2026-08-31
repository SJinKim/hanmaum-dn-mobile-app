package com.hanmaum.dn.mobile.features.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.i18n.AppLocale
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.DnErrorState
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnPrimaryButton
import com.hanmaum.dn.mobile.core.presentation.components.DnTintedButton
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnInnerShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.DnTileShape
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.member.data.model.MemberResponse
import org.koin.compose.viewmodel.koinViewModel

/**
 * 프로필.
 *
 * Viewing and editing stay one screen driven by the existing `isEditing`
 * flag — splitting them would have meant a new route and a second view model
 * for no behavioural gain. Everything the member configures now lives behind
 * the 설정 row — theme, language, sign-in and location in one place, instead of
 * a growing list of toggles wedged between the profile and the logout button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val loggedOut by viewModel.loggedOut.collectAsState()
    val c = DnTheme.colors

    // Without this the screen sits on ProfileUiState.Loading for ever: nothing
    // else asks the ViewModel to load, and it has no init that would. Keyed on
    // Unit rather than on a lifecycle event because loadProfile() refreshes
    // silently when data is already there — it is cheap to repeat and stale
    // data (a profile edited in the web app) is the thing being avoided.
    LaunchedEffect(Unit) { viewModel.loadProfile() }

    LaunchedEffect(loggedOut) { if (loggedOut) onLogout() }

    DnBackground(glows = DnGlows.action()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            val editing = (uiState as? ProfileUiState.Success)?.isEditing == true
            DnTopBar(
                title = if (editing) "프로필 수정" else "프로필",
                onBack = { if (editing) viewModel.cancelEditing() else onBack() },
            )

            when (val state = uiState) {
                is ProfileUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = c.lime)
                }

                is ProfileUiState.Error ->
                    DnErrorState(onRetry = viewModel::loadProfile)

                is ProfileUiState.Success -> {
                    if (state.isEditing) {
                        ProfileEditContent(state = state, viewModel = viewModel)
                    } else {
                        ProfileViewContent(
                            profile = state.profile,
                            onEdit = viewModel::startEditing,
                            onSettings = onSettings,
                            onLogout = viewModel::logout,
                        )
                    }
                }
            }
        }
    }

}

// ─────────────────────────── view mode ───────────────────────────

@Composable
private fun ProfileViewContent(
    profile: MemberResponse,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))

        Box(
            Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(c.surface2)
                .border(1.5.dp, c.strokeStrong, RoundedCornerShape(percent = 50)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(DnIcons.User, null, tint = c.textPrimary, modifier = Modifier.size(44.dp))
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "${profile.lastName}${profile.firstName}",
            style = DnTheme.typography.titleLg,
            color = c.textPrimary,
        )

        val role = listOfNotNull(profile.division, profile.churchRole)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        if (role.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(c.limeDim, RoundedCornerShape(percent = 50))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(role, style = DnTheme.typography.label, color = c.limeInk)
            }
        }

        Spacer(Modifier.height(22.dp))

        // TODO(hanmaum-dn-server#114, #117): attendance summary and joinedAt
        // are not available yet — the third tile is the one that exists today.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile("올해 출석", "–", c.limeInk, Modifier.weight(1f))
            StatTile("소속 사역", "–", c.blue, Modifier.weight(1f))
            StatTile("소속 그룹", profile.groupName ?: "–", c.amber, Modifier.weight(1f))
        }

        Spacer(Modifier.height(22.dp))

        MenuRow(DnIcons.User, "프로필 수정", null, onEdit)
        Spacer(Modifier.height(10.dp))
        MenuRow(DnIcons.More, strings.settingsTitle, null, onSettings)

        Spacer(Modifier.height(18.dp))

        DnTintedButton("로그아웃", onLogout, Modifier.fillMaxWidth())

        Spacer(Modifier.height(60.dp))
    }
}

@Composable
private fun StatTile(label: String, value: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    val c = DnTheme.colors
    Column(
        modifier
            .clip(DnTileShape)
            .background(c.surface, DnTileShape)
            .border(1.dp, c.strokeSubtle, DnTileShape)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(value, style = DnTheme.typography.stat, color = accent, maxLines = 1)
        Text(label, style = DnTheme.typography.label, color = c.textTertiary)
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, value: String?, onClick: () -> Unit) {
    val c = DnTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(DnTileShape)
            .background(c.surface, DnTileShape)
            .border(1.dp, c.strokeSubtle, DnTileShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(c.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = c.textSecondary, modifier = Modifier.size(17.dp))
        }
        Text(label, style = DnTheme.typography.captionStrong, color = c.textPrimary)
        Spacer(Modifier.weight(1f))
        if (value != null) {
            Text(value, style = DnTheme.typography.caption, color = c.textTertiary)
        }
        Icon(DnIcons.ChevronRight, null, tint = c.textTertiary, modifier = Modifier.size(18.dp))
    }
}

// ─────────────────────────── edit mode ───────────────────────────

@Composable
private fun ProfileEditContent(
    state: ProfileUiState.Success,
    viewModel: ProfileViewModel,
) {
    val c = DnTheme.colors
    val p = state.profile

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))

        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(c.surface2)
                    .border(1.5.dp, c.strokeStrong, RoundedCornerShape(percent = 50)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(DnIcons.User, null, tint = c.textPrimary, modifier = Modifier.size(42.dp))
            }
            // picking from the gallery replaces the old URL field
            Row(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(c.surface2, RoundedCornerShape(percent = 50))
                    .border(1.dp, c.strokeSubtle, RoundedCornerShape(percent = 50))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(DnIcons.Image, null, tint = c.textSecondary, modifier = Modifier.size(15.dp))
                Text("사진 변경", style = DnTheme.typography.captionStrong, color = c.textSecondary)
            }
        }

        Spacer(Modifier.height(40.dp))

        LockedGroup(
            rows = listOf(
                "이름" to "${p.lastName}${p.firstName}",
                "이메일" to (p.email ?: "—"),
                "부서" to (p.division ?: "—"),
                "그룹" to (p.groupName ?: "—"),
                "직분" to (p.churchRole ?: "—"),
            )
        )

        Spacer(Modifier.height(24.dp))

        Text("직접 수정할 수 있는 정보", style = DnTheme.typography.label, color = c.textTertiary)
        Spacer(Modifier.height(16.dp))

        EditField("전화번호", state.editPhone, viewModel::updatePhone)
        Spacer(Modifier.height(16.dp))
        EditField("도로명", state.editStreet, viewModel::updateStreet)
        Spacer(Modifier.height(16.dp))
        EditField("우편번호", state.editZipCode, viewModel::updateZipCode)
        Spacer(Modifier.height(16.dp))
        EditField("도시", state.editCity, viewModel::updateCity)

        state.saveError?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = DnTheme.typography.caption, color = c.red)
        }

        Spacer(Modifier.height(28.dp))

        // two separate actions, not a segmented pair — a toggle look would
        // suggest switching between them rather than choosing one
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DnTintedButton("취소", viewModel::cancelEditing, Modifier.weight(1f))
            DnPrimaryButton(
                label = if (state.isSaving) "저장 중…" else "저장",
                onClick = viewModel::saveProfile,
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(60.dp))
    }
}

/**
 * Fields the church office owns. Three signals together, because one is easy
 * to miss: a recessed surface, a padlock per row, and muted values.
 */
@Composable
private fun LockedGroup(rows: List<Pair<String, String>>) {
    val c = DnTheme.colors
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(DnIcons.Lock, null, tint = c.textTertiary, modifier = Modifier.size(13.dp))
            Text("교회에서 관리하는 정보", style = DnTheme.typography.label, color = c.textTertiary)
        }
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(DnTileShape)
                .background(c.surface2, DnTileShape)
                .border(1.dp, c.strokeSubtle, DnTileShape)
                .padding(horizontal = 16.dp),
        ) {
            rows.forEachIndexed { i, (label, value) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(label, style = DnTheme.typography.label, color = c.textTertiary)
                        Text(value, style = DnTheme.typography.captionStrong, color = c.textSecondary)
                    }
                    Icon(DnIcons.Lock, null, tint = c.textTertiary, modifier = Modifier.size(15.dp))
                }
                if (i < rows.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.strokeSubtle))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "변경이 필요하면 교회 사무실에 문의해 주세요.",
            style = DnTheme.typography.caption,
            color = c.textTertiary,
        )
    }
}

@Composable
private fun EditField(label: String, value: String, onChange: (String) -> Unit) {
    val c = DnTheme.colors
    Column {
        Text(label, style = DnTheme.typography.label, color = c.textTertiary)
        Spacer(Modifier.height(7.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(DnInnerShape)
                .background(c.surface, DnInnerShape)
                .border(1.dp, c.strokeStrong, DnInnerShape)
                .padding(horizontal = 16.dp, vertical = 15.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = DnTheme.typography.captionStrong.copy(color = c.textPrimary),
                cursorBrush = SolidColor(c.lime),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────── sheets ───────────────────────────

internal data class SheetOption(
    val label: String,
    val note: String?,
    val selected: Boolean,
    val onSelect: () -> Unit,
)

@Composable
internal fun SheetOptions(title: String, subtitle: String?, options: List<SheetOption>) {
    val c = DnTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        Text(title, style = DnTheme.typography.title, color = c.textPrimary)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = DnTheme.typography.caption, color = c.textSecondary)
        }
        Spacer(Modifier.height(16.dp))
        options.forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(DnTileShape)
                    .background(if (option.selected) c.limeDim else c.surface2, DnTileShape)
                    .border(
                        1.dp,
                        if (option.selected) c.lime else c.strokeSubtle,
                        DnTileShape,
                    )
                    .clickable(onClick = option.onSelect)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        option.label,
                        style = DnTheme.typography.captionStrong,
                        color = if (option.selected) c.limeInk else c.textPrimary,
                    )
                    option.note?.let {
                        Text(it, style = DnTheme.typography.caption, color = c.textTertiary)
                    }
                }
                if (option.selected) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(c.lime),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(DnIcons.Check, null, tint = c.onLime, modifier = Modifier.size(15.dp))
                    }
                } else {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .border(1.5.dp, c.strokeStrong, RoundedCornerShape(percent = 50))
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(26.dp))
    }
}
