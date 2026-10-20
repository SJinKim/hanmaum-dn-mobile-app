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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import com.hanmaum.dn.mobile.core.presentation.components.DnTextField
import com.hanmaum.dn.mobile.features.login.domain.model.Countries
import com.hanmaum.dn.mobile.features.login.presentation.FieldError
import com.hanmaum.dn.mobile.features.login.presentation.messageFor
import com.hanmaum.dn.mobile.features.login.presentation.required
import com.hanmaum.dn.mobile.features.login.presentation.components.BirthdayPickerField
import com.hanmaum.dn.mobile.features.login.presentation.components.DEV_TEAM_EMAIL
import com.hanmaum.dn.mobile.features.login.presentation.components.IdentityStatusField
import com.hanmaum.dn.mobile.features.login.presentation.components.IdentityStatusPickerDialog
import com.hanmaum.dn.mobile.features.login.presentation.components.PhoneCountryPickerDialog
import com.hanmaum.dn.mobile.features.login.presentation.components.PhoneCountryPrefix
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hanmaum.dn.mobile.core.i18n.AppLocale
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.dismissKeyboardOnTap
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
import com.hanmaum.dn.mobile.features.verse.domain.model.VerseRecords
import com.hanmaum.dn.mobile.features.member.domain.membershipDuration
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.hanmaum.dn.mobile.features.attendance.domain.model.AttendanceSummary
import com.hanmaum.dn.mobile.features.attendance.presentation.AttendanceViewModel
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
    onMyApplications: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    // The same AttendanceViewModel Home already uses, so the 올해 출석 figure
    // costs no request of its own here.
    val attendanceViewModel: AttendanceViewModel = koinViewModel()
    val attendance by attendanceViewModel.uiState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val loggedOut by viewModel.loggedOut.collectAsState()
    val verseRecords by viewModel.verseRecords.collectAsState()
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
                            summary = attendance.summary,
                            verseRecords = verseRecords,
                            onEdit = viewModel::startEditing,
                            onSettings = onSettings,
                            onMyApplications = onMyApplications,
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
    summary: AttendanceSummary?,
    verseRecords: VerseRecords?,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
    onMyApplications: () -> Unit,
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

        // The chip carries 교구 · 순 · 직분, which is why the 순 has no tile of
        // its own — repeating it below the name would say the same twice.
        val role = listOfNotNull(profile.division, profile.groupName, profile.churchRole)
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

        // Three membership metrics on top, the two 말씀 records below — the
        // grid the Figma profile frame defines. Both rows run on the same
        // three columns, so the second row's tiles line up with the first's
        // and simply leave the third slot empty.
        val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
        val together = remember(profile.registrationDate, today) {
            membershipDuration(profile.registrationDate, today)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Dash, not zero, while the summary is unloaded or failed.
            StatTile(
                "올해 출석",
                summary?.yearAttended?.toString() ?: "–",
                c.limeInk,
                Modifier.weight(1f),
            )
            StatTile("소속 사역", profile.activeMinistries.size.toString(), c.blue, Modifier.weight(1f))
            StatTile(
                label = strings.profileTimeTogether,
                value = together?.let { strings.profileTimeTogetherValue(it.years, it.months) } ?: "–",
                accent = c.amber,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Dash, not zero, while the streaks are unloaded or the call failed —
            // a fresh member's real zero and "we do not know yet" must not look
            // the same.
            StatTile(
                strings.profileQtRecord,
                verseRecords?.quietTime?.let { strings.verseRecordDaysValue(it.totalDays) } ?: "–",
                c.amber,
                Modifier.weight(1f),
            )
            StatTile(
                strings.profileMemorizationRecord,
                verseRecords?.recitation?.let { strings.verseRecordDaysValue(it.totalDays) } ?: "–",
                c.amber,
                Modifier.weight(1f),
            )
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(22.dp))

        MenuRow(DnIcons.User, "프로필 수정", null, onEdit)
        Spacer(Modifier.height(10.dp))
        MenuRow(
            icon = DnIcons.ListBulleted,
            label = strings.myApplicationsTitle,
            value = null,
            onClick = onMyApplications,
            subtitle = strings.myApplicationsMenuSubtitle,
        )
        Spacer(Modifier.height(10.dp))
        MenuRow(DnIcons.Settings, strings.settingsTitle, null, onSettings)

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

/**
 * One row of the account menu.
 *
 * [subtitle] is for a row whose name does not say enough on its own — 나의 신청 확인하기 needs
 * to name what is inside. It grows the icon tile with it, which is how the Figma board draws
 * the two-line variant; the plain rows keep the size they have always had.
 */
@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    value: String?,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
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
                .size(if (subtitle != null) 44.dp else 34.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(c.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                null,
                tint = c.textSecondary,
                modifier = Modifier.size(if (subtitle != null) 20.dp else 17.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = DnTheme.typography.captionStrong, color = c.textPrimary)
            subtitle?.let {
                Text(it, style = DnTheme.typography.label, color = c.textTertiary)
            }
        }
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
    val strings = LocalStrings.current
    val p = state.profile

    var showCountryPicker by rememberSaveable { mutableStateOf(false) }
    if (showCountryPicker) {
        PhoneCountryPickerDialog(
            selectedIso = state.editPhoneCountryIso,
            onSelect = {
                viewModel.updatePhoneCountry(it)
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false },
        )
    }
    var showIdentityPicker by rememberSaveable { mutableStateOf(false) }
    if (showIdentityPicker) {
        IdentityStatusPickerDialog(
            selected = state.editIdentityStatus,
            onSelect = {
                viewModel.updateIdentityStatus(it)
                showIdentityPicker = false
            },
            onDismiss = { showIdentityPicker = false },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // Address fields and a phone number, none of them with a Done
            // key on iOS — tapping off a field is the way out (#218).
            .dismissKeyboardOnTap()
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

        // 교구/순 are what the church calls church_groups.division and .name;
        // the labels live in AppStrings so all three languages say the same (#194).
        LockedGroup(
            rows = listOf(
                strings.labelName to "${p.lastName}${p.firstName}",
                strings.labelEmail to (
                    p.email?.let { if (p.emailVerified == false) "$it · ${strings.profileEmailUnverified}" else it } ?: "—"
                ),
                strings.labelDivision to (p.division ?: "—"),
                strings.labelGroup to (p.groupName ?: "—"),
                strings.labelChurchRole to (p.churchRole ?: "—"),
            )
        )

        Spacer(Modifier.height(24.dp))

        Text("직접 수정할 수 있는 정보", style = DnTheme.typography.label, color = c.textTertiary)
        Spacer(Modifier.height(6.dp))
        Text(strings.registerRequiredLegend, style = DnTheme.typography.caption, color = c.textSecondary)
        Spacer(Modifier.height(16.dp))

        // Same fields, order and rules as registration (#283, #286): what was
        // required to sign up stays required here.
        DnTextField(
            strings.fieldPhone, state.editPhone, viewModel::updatePhone,
            Modifier.fillMaxWidth(), placeholder = "151 23456789",
            keyboardType = KeyboardType.Phone,
            leadingContent = {
                PhoneCountryPrefix(Countries.byIsoOrDefault(state.editPhoneCountryIso)) {
                    showCountryPicker = true
                }
            },
        )

        Spacer(Modifier.height(16.dp))
        BirthdayPickerField(
            required(strings.fieldBirthDate), state.editBirthDate, viewModel::updateBirthDate,
            Modifier.fillMaxWidth(), placeholder = "2000.01.01",
            isError = state.birthDateError != null,
        )
        FieldError(strings.messageFor(state.birthDateError))

        Spacer(Modifier.height(16.dp))
        IdentityStatusField(
            required(strings.fieldIdentityStatus), state.editIdentityStatus,
            onClick = { showIdentityPicker = true },
            modifier = Modifier.fillMaxWidth(),
            isError = state.identityStatusError != null,
        )
        FieldError(strings.messageFor(state.identityStatusError))

        Spacer(Modifier.height(16.dp))
        DnTextField(
            strings.fieldWorkOrSchool, state.editWorkOrSchool, viewModel::updateWorkOrSchool,
            Modifier.fillMaxWidth(), placeholder = strings.workOrSchoolPlaceholder,
        )

        // ── address: both halves of the street or neither ──
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                DnTextField(
                    strings.fieldStreet, state.editStreet, viewModel::updateStreet,
                    Modifier.fillMaxWidth(), placeholder = "Musterstraße",
                    isError = state.streetError != null,
                )
                FieldError(strings.messageFor(state.streetError))
            }
            Column(Modifier.width(104.dp)) {
                DnTextField(
                    strings.fieldHouseNumber, state.editHouseNumber, viewModel::updateHouseNumber,
                    Modifier.fillMaxWidth(), placeholder = "12a",
                    isError = state.houseNumberError != null,
                )
                FieldError(strings.messageFor(state.houseNumberError))
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.width(124.dp)) {
                DnTextField(
                    required(strings.fieldZipCode), state.editZipCode, viewModel::updateZipCode,
                    Modifier.fillMaxWidth(), placeholder = "40210",
                    keyboardType = KeyboardType.Number,
                    isError = state.zipCodeError != null,
                )
                FieldError(strings.messageFor(state.zipCodeError))
            }
            Column(Modifier.weight(1f)) {
                // filled from the postcode, as on registration
                DnTextField(
                    required(strings.fieldCity), state.editCity, viewModel::updateCity,
                    Modifier.fillMaxWidth(), placeholder = "Düsseldorf",
                    isError = state.cityError != null,
                )
                FieldError(strings.messageFor(state.cityError))
            }
        }

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
        // The church office has no inbox the app can point to; the dev team
        // does. A device without a mail app swallows the tap instead of
        // crashing, as on the register dialog (#286).
        val uriHandler = LocalUriHandler.current
        Text(
            buildAnnotatedString {
                append("변경이 필요하면\n")
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "mailto",
                        styles = TextLinkStyles(
                            SpanStyle(color = c.limeInk, textDecoration = TextDecoration.Underline),
                        ),
                    ) { runCatching { uriHandler.openUri("mailto:$DEV_TEAM_EMAIL") } },
                ) { append(DEV_TEAM_EMAIL) }
                append("로 문의해 주세요.")
            },
            style = DnTheme.typography.caption,
            color = c.textTertiary,
        )
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
