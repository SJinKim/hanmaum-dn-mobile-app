package com.hanmaum.dn.mobile.features.training.presentation.detail

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.presentation.components.DnBackground
import com.hanmaum.dn.mobile.core.presentation.components.DnErrorState
import com.hanmaum.dn.mobile.core.presentation.components.DnGlows
import com.hanmaum.dn.mobile.core.presentation.components.DnPrimaryButton
import com.hanmaum.dn.mobile.core.presentation.components.DnTopBar
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnPillShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.DnTileShape
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.ministry.presentation.detail.DetailFacts
import com.hanmaum.dn.mobile.features.ministry.presentation.detail.DetailHero
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingApplication
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingCourse
import com.hanmaum.dn.mobile.features.training.domain.model.TrainingDetail
import com.hanmaum.dn.mobile.features.training.presentation.TrainingFormat
import com.hanmaum.dn.mobile.features.training.presentation.components.NurtureCancelDialog
import com.hanmaum.dn.mobile.features.training.presentation.components.NurtureOpenBadge
import com.hanmaum.dn.mobile.features.training.presentation.components.NurtureSkeleton
import com.hanmaum.dn.mobile.features.training.presentation.components.NurtureStatusBadge
import com.hanmaum.dn.mobile.features.training.presentation.components.NurtureUnavailableCard
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 양육 detail: what the training is, which 반 are open, and where the member's own
 * application stands.
 *
 * Which courses are open, which of them this member may choose, and the application
 * itself are all decided by the server for this member; the screen only draws them.
 * Figma: section `22 · 양육 신청 · Zustände`, board `양육 상세 · 신청 영역`.
 */
@Composable
fun NurtureDetailScreen(
    publicId: String,
    onBackClick: () -> Unit,
) {
    val viewModel: TrainingDetailViewModel = koinViewModel(parameters = { parametersOf(publicId) })
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    DnBackground(glows = DnGlows.information()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            DnTopBar(title = strings.nurtureTitle, onBack = onBackClick)

            val detail = state.detail
            when {
                state.isLoading -> DetailSkeleton()
                state.isUnavailable -> NurtureUnavailableCard(
                    Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                )
                state.hasError || detail == null -> DnErrorState(onRetry = viewModel::load)
                else -> DetailContent(
                    state = state,
                    detail = detail,
                    onSelectCourse = viewModel::selectCourse,
                    onApply = viewModel::openApplicationForm,
                    onCancel = viewModel::openCancelDialog,
                )
            }
        }
    }

    if (state.showCancelDialog) {
        NurtureCancelDialog(onDismiss = viewModel::dismissCancelDialog)
    }

    state.form?.let { form ->
        ApplicationSheet(
            form = form,
            onValueChange = viewModel::updateField,
            onConsentChange = viewModel::setConsent,
            onSubmit = viewModel::submitApplication,
            onClose = viewModel::closeApplicationForm,
        )
    }
}

@Composable
private fun DetailContent(
    state: TrainingDetailUiState,
    detail: TrainingDetail,
    onSelectCourse: (Int) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        DetailHero(
            icon = DnIcons.Book,
            container = c.blueDim,
            ink = c.blue,
            eyebrow = strings.nurtureEyebrow,
            name = detail.name,
        )

        detail.description?.let {
            Spacer(Modifier.height(14.dp))
            Text(it, style = DnTheme.typography.body, color = c.textSecondary)
        }

        val facts = listOfNotNull(
            TrainingFormat.period(strings, detail)?.let { Triple(DnIcons.Calendar, strings.nurturePeriod, it) },
            TrainingFormat.schedule(strings, detail)?.let { Triple(DnIcons.Clock, strings.nurtureTime, it) },
            detail.location?.let { Triple(DnIcons.MapPin, strings.nurturePlace, it) },
            detail.leaderName?.let { Triple(DnIcons.User, strings.nurtureLeader, it) },
        )
        if (facts.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            DetailFacts(rows = facts)
        }

        if (detail.targetAudience.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            Text(strings.nurtureAudience, style = DnTheme.typography.headline, color = c.textPrimary)
            Spacer(Modifier.height(12.dp))
            detail.targetAudience.forEach { AudienceLine(it) }
        }

        Spacer(Modifier.height(22.dp))

        detail.myApplication?.let { application ->
            ApplicationStatusCard(
                application = application,
                onCancel = onCancel.takeIf { application.status.isActive },
            )
            Spacer(Modifier.height(16.dp))
        }

        if (!state.hasActiveApplication) {
            CourseChoice(
                courses = detail.courses,
                selectedCourseId = state.selectedCourseId,
                onSelect = onSelectCourse,
            )
            Spacer(Modifier.height(16.dp))
            DnPrimaryButton(
                label = if (detail.courses.isEmpty()) strings.nurtureClosed else strings.nurtureApply,
                leading = DnIcons.UserCheck,
                onClick = onApply,
                enabled = state.canApply,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(60.dp))
    }
}

@Composable
private fun AudienceLine(text: String) {
    val c = DnTheme.colors
    Row(
        Modifier.padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(c.limeDim),
            contentAlignment = Alignment.Center,
        ) {
            Icon(DnIcons.Check, null, tint = c.limeInk, modifier = Modifier.size(13.dp))
        }
        Text(text, style = DnTheme.typography.caption, color = c.textSecondary)
    }
}

/**
 * No open course: a closed notice. One: shown, already chosen. Several: the member picks one.
 */
@Composable
private fun CourseChoice(
    courses: List<TrainingCourse>,
    selectedCourseId: Int?,
    onSelect: (Int) -> Unit,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    when {
        courses.isEmpty() -> Card {
            NurtureOpenBadge(open = false)
            Text(strings.nurtureClosedHint, style = DnTheme.typography.caption, color = c.textSecondary)
        }

        courses.size == 1 -> Card(spacing = 4) {
            val course = courses.single()
            Text(strings.nurtureSelectedCourse, style = DnTheme.typography.label, color = c.textTertiary)
            CourseText(course)
        }

        else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(strings.nurtureCourseSelect, style = DnTheme.typography.headline, color = c.textPrimary)
            courses.forEach { course ->
                CourseOption(
                    course = course,
                    selected = course.externalCourseId == selectedCourseId,
                    onClick = { onSelect(course.externalCourseId) },
                )
            }
        }
    }
}

@Composable
private fun CourseOption(course: TrainingCourse, selected: Boolean, onClick: () -> Unit) {
    val c = DnTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(DnTileShape)
            .background(c.surface, DnTileShape)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) c.lime else c.strokeSubtle, DnTileShape)
            .selectable(
                selected = selected,
                enabled = course.isEligible,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .alpha(if (course.isEligible) 1f else DISABLED_ALPHA)
                .size(22.dp)
                .clip(CircleShape)
                .then(
                    if (selected) Modifier.background(c.lime)
                    else Modifier.border(1.5.dp, c.strokeStrong, CircleShape),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(DnIcons.Check, null, tint = c.onLime, modifier = Modifier.size(13.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CourseText(course)
        }
    }
}

@Composable
private fun CourseText(course: TrainingCourse) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    val faded = Modifier.alpha(if (course.isEligible) 1f else DISABLED_ALPHA)

    Text(course.name, style = DnTheme.typography.captionStrong, color = c.textPrimary, modifier = faded)
    course.dateText?.let {
        Text(it, style = DnTheme.typography.caption, color = c.textSecondary, modifier = faded)
    }
    TrainingFormat.window(strings, course.window)?.let {
        MetaLine(
            icon = if (course.window.isAlwaysOpen) DnIcons.Clock else DnIcons.Calendar,
            text = it,
            modifier = faded,
        )
    }
    if (!course.isEligible) {
        MetaLine(icon = DnIcons.AlertTriangle, text = strings.nurtureCourseNotEligible, emphasised = true)
    }
}

/**
 * 신청 현황: the member's own application, not a participant count (#234).
 * [onCancel] is null once the application is no longer running.
 */
@Composable
private fun ApplicationStatusCard(application: TrainingApplication, onCancel: (() -> Unit)?) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    Card {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(strings.nurtureStatusTitle, style = DnTheme.typography.captionStrong, color = c.textPrimary)
            NurtureStatusBadge(application.status)
        }
        FactLine(strings.nurtureStatusCourse, application.courseName)
        application.appliedAt?.let { FactLine(strings.nurtureStatusAppliedOn, TrainingFormat.appliedOn(strings, it)) }
        onCancel?.let {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(DnPillShape)
                    .border(1.dp, c.strokeStrong, DnPillShape)
                    .clickable(onClick = it)
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(DnIcons.X, null, tint = c.textSecondary, modifier = Modifier.size(14.dp))
                Text(strings.nurtureCancel, style = DnTheme.typography.captionStrong, color = c.textSecondary)
            }
        }
    }
}

@Composable
private fun FactLine(key: String, value: String) {
    val c = DnTheme.colors
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(key, style = DnTheme.typography.caption, color = c.textTertiary)
        Text(
            value,
            style = DnTheme.typography.caption,
            color = c.textPrimary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun MetaLine(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    emphasised: Boolean = false,
) {
    val c = DnTheme.colors
    val ink = if (emphasised) c.textSecondary else c.textTertiary
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, null, tint = ink, modifier = Modifier.size(12.dp))
        Text(text, style = DnTheme.typography.label, color = ink)
    }
}

@Composable
private fun Card(spacing: Int = 12, content: @Composable () -> Unit) {
    val c = DnTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(DnCardShape)
            .background(c.surface, DnCardShape)
            .border(1.dp, c.strokeSubtle, DnCardShape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp),
    ) {
        content()
    }
}

@Composable
private fun DetailSkeleton() {
    Column(
        Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NurtureSkeleton(180.dp, 28.dp)
        NurtureSkeleton(300.dp, 14.dp)
        NurtureSkeleton(240.dp, 14.dp)
        Spacer(Modifier.height(8.dp))
        NurtureSkeleton(353.dp, 120.dp)
    }
}

private const val DISABLED_ALPHA = 0.4f
