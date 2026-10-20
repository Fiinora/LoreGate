package com.loregate.app

import androidx.compose.ui.window.ComposeUIViewController
import com.loregate.app.di.commonModule
import com.loregate.app.di.platformModule
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

fun initKoin() {
    startKoin {
        modules(commonModule, platformModule())
    }
}

fun MainViewController(): UIViewController = ComposeUIViewController {
    App()
}
