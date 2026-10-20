package com.loregate.app

import androidx.compose.runtime.Composable
import com.loregate.app.core.designsystem.LoreGateTheme
import com.loregate.app.core.navigation.LoreGateNavHost
import com.loregate.app.domain.repository.AuthRepository
import org.koin.compose.koinInject

@Composable
fun App(
    authRepository: AuthRepository = koinInject()
) {
    LoreGateTheme {
        LoreGateNavHost(
            isUserAuthenticated = authRepository.getCurrentUser() != null
        )
    }
}
