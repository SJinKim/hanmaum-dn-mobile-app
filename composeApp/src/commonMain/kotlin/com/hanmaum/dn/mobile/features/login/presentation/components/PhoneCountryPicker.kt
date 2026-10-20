package com.hanmaum.dn.mobile.features.login.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hanmaum.dn.mobile.core.i18n.LocalStrings
import com.hanmaum.dn.mobile.core.platform.localizedCountryName
import com.hanmaum.dn.mobile.core.presentation.components.DnTextField
import com.hanmaum.dn.mobile.core.presentation.icons.DnIcons
import com.hanmaum.dn.mobile.core.presentation.theme.AppMotion
import com.hanmaum.dn.mobile.core.presentation.theme.DnCardShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnInnerShape
import com.hanmaum.dn.mobile.core.presentation.theme.DnTheme
import com.hanmaum.dn.mobile.core.presentation.theme.typography
import com.hanmaum.dn.mobile.features.login.domain.model.Countries
import com.hanmaum.dn.mobile.features.login.domain.model.Country

/**
 * The `🇩🇪 +49 ▾` prefix inside the phone field (#280). The number alone is
 * ambiguous; this is what decides which country it is sent for.
 */
@Composable
internal fun PhoneCountryPrefix(country: Country, onClick: () -> Unit) {
    val c = DnTheme.colors
    Row(
        Modifier.clip(DnInnerShape).pressClick(onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(country.flag, style = DnTheme.typography.body)
        Text("+${country.dialCode}", style = DnTheme.typography.captionStrong, color = c.textPrimary)
        Icon(DnIcons.ChevronUp, null, tint = c.textTertiary, modifier = Modifier.size(16.dp).rotate(180f))
    }
}

/**
 * 국가번호 선택 — DE and KR are pinned first because nearly every member dials
 * one of them; the rest follow sorted by their name in the app language. A
 * search filters the pinned rows like any other. Tapping a row applies it.
 */
@Composable
internal fun PhoneCountryPickerDialog(
    selectedIso: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = DnTheme.colors
    val strings = LocalStrings.current
    val language = strings.languageTag
    val nameOf: (Country) -> String = remember(language) {
        { localizedCountryName(it.iso, language) ?: it.name }
    }
    val rest = remember(language) { Countries.unpinnedSortedBy(nameOf) }
    var query by rememberSaveable { mutableStateOf("") }
    val pinned = Countries.pinned.filter { Countries.matches(it, query, nameOf) }
    val others = rest.filter { Countries.matches(it, query, nameOf) }
    val selectedName = nameOf(Countries.byIsoOrDefault(selectedIso))

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(DnCardShape)
                .background(c.surface, DnCardShape)
                .border(1.dp, c.strokeSubtle, DnCardShape)
                .padding(horizontal = 18.dp, vertical = 22.dp),
        ) {
            Text(strings.phoneCountryPickerTitle, style = DnTheme.typography.title, color = c.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(strings.phoneCountryPickerPreselected(selectedName), style = DnTheme.typography.caption, color = c.textSecondary)
            Spacer(Modifier.height(14.dp))
            DnTextField(
                label = null,
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = strings.phoneCountrySearchPlaceholder,
            )
            Spacer(Modifier.height(10.dp))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                if (pinned.isNotEmpty()) {
                    item { GroupHeader(strings.phoneCountryFrequent) }
                    items(pinned, key = { "p-${it.iso}" }) { country ->
                        CountryRow(country, nameOf(country), country.iso == selectedIso) { onSelect(country.iso) }
                    }
                }
                if (others.isNotEmpty()) {
                    item { GroupHeader(strings.phoneCountryAll) }
                    items(others, key = { it.iso }) { country ->
                        CountryRow(country, nameOf(country), country.iso == selectedIso) { onSelect(country.iso) }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                strings.phoneCountryTapToApply,
                style = DnTheme.typography.caption,
                color = c.textTertiary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun GroupHeader(text: String) {
    Text(
        text.uppercase(),
        style = DnTheme.typography.label,
        color = DnTheme.colors.textTertiary,
        modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 6.dp),
    )
}

@Composable
private fun CountryRow(country: Country, name: String, selected: Boolean, onClick: () -> Unit) {
    val c = DnTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(DnInnerShape)
            .background(if (selected) c.surface2 else c.surface)
            .pressClick(onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(country.flag, style = DnTheme.typography.body)
        Text(
            name,
            style = if (selected) DnTheme.typography.bodyStrong else DnTheme.typography.body,
            color = c.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            "+${country.dialCode}",
            style = DnTheme.typography.captionStrong,
            color = if (selected) c.limeInk else c.textSecondary,
        )
    }
}

internal fun Modifier.pressClick(onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) AppMotion.PRESS_SCALE else 1f, AppMotion.press)
    scale(scale).clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onClick)
}
