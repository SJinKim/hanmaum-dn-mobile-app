package com.hanmaum.dn.mobile.features.pending.data.model

import com.hanmaum.dn.mobile.core.domain.model.NavRoute

data class PendingUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val navigateTo: NavRoute? = null
)
