package com.loregate.app.feature.passport

import com.loregate.app.domain.model.HeritageStamp

data class PassportUiState(
    val isLoading: Boolean = false,
    val explorerName: String = "Pengelana Waktu",
    val explorerLevel: Long = 1L,
    val stamps: List<HeritageStamp> = emptyList(),
    val selectedStamp: HeritageStamp? = null,
    val unlockedCount: Int = 0,
    val totalCount: Int = 0
)

sealed interface PassportUiEvent {
    data class OnStampClicked(val stamp: HeritageStamp) : PassportUiEvent
    data object OnDismissStampDetail : PassportUiEvent
    data class OnNavigateToSiteOnMap(val siteId: String) : PassportUiEvent
    data object OnBackClicked : PassportUiEvent
}

sealed interface PassportUiEffect {
    data object NavigateBack : PassportUiEffect
    data class NavigateToMapWithSite(val siteId: String) : PassportUiEffect
}
