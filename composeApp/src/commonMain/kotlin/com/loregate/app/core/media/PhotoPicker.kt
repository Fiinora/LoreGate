package com.loregate.app.core.media

import androidx.compose.runtime.Composable

@Composable
expect fun rememberPhotoPickerLauncher(
    onPhotoPicked: (ByteArray?) -> Unit
): () -> Unit
