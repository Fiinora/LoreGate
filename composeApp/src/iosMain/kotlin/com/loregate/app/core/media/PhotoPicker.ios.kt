package com.loregate.app.core.media

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPhotoPickerLauncher(
    onPhotoPicked: (ByteArray?) -> Unit
): () -> Unit {
    return {
        // iOS PhotoPicker implementation placeholder
        onPhotoPicked(null)
    }
}
