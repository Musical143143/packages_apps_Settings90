/*
 * Copyright (C) 2026 The Evolution X Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo.firmwareversion

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.android.settings.widget.setSettingsContent
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider

/** One row on the About phone page, built from the existing preference metadata. */
private data class AboutRow(
    val title: String,
    val summary: String?,
    val onClick: (() -> Unit)?,
)

/** Builds the rows from the same metadata the preference screen uses, so visibility and taps match. */
private fun buildAboutRows(context: Context): List<AboutRow> {
    val items: List<PreferenceMetadata> = listOf(
        AboutDeviceNamePreference(),
        FirmwareVersionDetailPreference(),
        LineageVersionDetailPreference(),
        SecurityPatchLevelPreference(),
        LineageVendorSecurityPatchLevelPreference(),
        MainlineModuleVersionPreference(),
        BasebandVersionPreference(),
        KernelVersionPreference(),
        LineageBuildDatePreference(),
        SimpleBuildNumberPreference(),
    )
    return items
        .filter { it.isAvailable(context) }
        .map { meta ->
            val intent = meta.intent(context)
            AboutRow(
                title = context.getString(meta.title),
                summary = (meta as? PreferenceSummaryProvider)?.getSummary(context)?.toString(),
                onClick = intent?.let { i ->
                    {
                        try {
                            context.startActivity(i)
                        } catch (e: Exception) {
                            Log.w(TAG, "Unable to open ${meta.key}", e)
                        }
                    }
                },
            )
        }
}

/** Compose About phone page: a hero card with grouped, tappable rows. Works in light and dark. */
class AboutPhoneComposeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = androidx.compose.ui.platform.ComposeView(requireContext()).apply {
        setSettingsContent { AboutPhoneContent() }
    }

    companion object {
        private const val TAG = "AboutPhoneCompose"
    }
}

@Composable
private fun AboutPhoneContent() {
    val context = LocalContext.current
    val rows = remember(context) { buildAboutRows(context) }
    val scheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surface),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { AboutHero() }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = scheme.surfaceContainerLow,
            ) {
                Column {
                    rows.forEachIndexed { index, row ->
                        AboutRowItem(row)
                        if (index < rows.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                color = scheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutHero() {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = scheme.primaryContainer,
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Evolution X",
                style = MaterialTheme.typography.headlineMediumEmphasized,
                color = scheme.onPrimaryContainer,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = "Android ${Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY}",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun AboutRowItem(row: AboutRow) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = row.onClick != null) { row.onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(scheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = scheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            row.summary?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}
