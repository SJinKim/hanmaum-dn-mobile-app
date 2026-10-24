package com.hanmaum.dn.mobile.core.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hanmaum.dn.mobile.core.i18n.AppLocale
import com.hanmaum.dn.mobile.core.i18n.EnStrings
import com.hanmaum.dn.mobile.core.i18n.KoStrings
import com.hanmaum.dn.mobile.core.presentation.components.AppScreen
import com.hanmaum.dn.mobile.core.presentation.components.DnTintedButton
import com.hanmaum.dn.mobile.core.presentation.theme.AppSpacing
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography

/** Bilingual before a language is known; tapping a language confirms it immediately. */
@Composable
fun LanguageSelectionScreen(onSelect: (AppLocale) -> Unit) {
    AppScreen(title = "${AppLocale.KO.nativeName} / ${AppLocale.EN.nativeName}") { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
        ) {
            Text(
                "${KoStrings.selectLanguage} / ${EnStrings.selectLanguage}",
                style = DnTheme.typography.headline,
                color = DnTheme.colors.textPrimary,
            )
            Text(
                "${KoStrings.initialLanguageHint}\n${EnStrings.initialLanguageHint}",
                style = DnTheme.typography.body,
                color = DnTheme.colors.textSecondary,
            )
            listOf(AppLocale.KO, AppLocale.EN).forEach { locale ->
                DnTintedButton(
                    label = locale.nativeName,
                    onClick = { onSelect(locale) },
                    modifier = Modifier.fillMaxWidth(),
                    // Neutral, not the red default: a choice here is not an error or a warning.
                    tint = DnTheme.colors.textPrimary,
                    container = DnTheme.colors.surface2,
                    showBorder = false,
                )
            }
        }
    }
}
