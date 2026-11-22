/*
 * Copyright (C) 2026 The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView

/**
 * Sets Compose content on a [ComposeView] used by Settings pages.
 *
 * Plain composition only; the Lunaris spring-overscroll effect is intentionally not ported.
 */
fun ComposeView.setSettingsContent(content: @Composable () -> Unit) {
    setContent { content() }
}
