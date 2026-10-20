package com.loregate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.loregate.app.core.designsystem.LoreGateTheme
import com.loregate.app.core.navigation.LoreGateNavHost
import com.loregate.app.domain.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LoreGateTheme {
                LoreGateNavHost(
                    isUserAuthenticated = authRepository.getCurrentUser() != null
                )
            }
        }
    }
}
