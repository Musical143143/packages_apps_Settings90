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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build as BuildIcon
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import com.android.settings.widget.setSettingsContent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.android.settings.R
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import com.android.axion.compose.theme.AxionTheme

/**
 * One row on the About phone page, built from the existing preference metadata.
 * Taps go through the same listener the preference screen used, so the kernel
 * row still toggles its version text and the easter-egg taps still work.
 */
private class AboutRowModel(
    val title: String,
    initialSummary: String?,
    private val context: Context,
    private val meta: PreferenceMetadata,
    private val pref: Preference,
    val role: String,
) {
    var summary: String? by mutableStateOf(initialSummary)

    val clickable: Boolean
        get() = meta is Preference.OnPreferenceClickListener || meta.intent(context) != null

    fun onClick() {
        val listener = meta as? Preference.OnPreferenceClickListener
        // true swallows the click; false lets the row open its intent.
        val swallowed = listener?.onPreferenceClick(pref) ?: false
        if (listener != null) {
            pref.summary?.toString()?.let { summary = it }
        }
        if (!swallowed) {
            try {
                meta.intent(context)?.let { context.startActivity(it) }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to open ${meta.key}", e)
            }
        }
    }

    companion object {
        private const val TAG = "AboutPhoneCompose"
    }
}

/** Builds the rows from the same metadata the preference screen uses, so visibility and taps match. */
private fun buildAboutRows(context: Context): List<AboutRowModel> {
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
        .filter { (it as? PreferenceAvailabilityProvider)?.isAvailable(context) ?: true }
        .map { meta ->
            AboutRowModel(
                title = context.getString(meta.title),
                initialSummary = (meta as? PreferenceSummaryProvider)?.getSummary(context)?.toString(),
                context = context,
                meta = meta,
                pref = Preference(context),
                role = meta::class.java.simpleName,
            )
        }
}

/** Compose About phone page, laid out to match the approved design. */
class AboutPhoneComposeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = androidx.compose.ui.platform.ComposeView(requireContext()).apply {
        setSettingsContent {
            AxionTheme {
                AboutPhoneContent(onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() })
            }
        }
    }
}

@Composable
private fun AboutPhoneContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val rows = remember(context) { buildAboutRows(context) }
    fun row(role: String): AboutRowModel? = rows.firstOrNull { it.role == role }
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    val glass = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.7f)
    val rim = if (dark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.9f)
    val ink = if (dark) Color.White else scheme.onSurface
    val soft = ink.copy(alpha = 0.7f)

    Box(modifier = Modifier.fillMaxSize()) {
        // Backdrop: deep gradient with soft violet and blue glows.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        if (dark) {
                            listOf(Color(0xFF05060F), Color(0xFF0C0A24), Color(0xFF05060F))
                        } else {
                            listOf(Color(0xFFF6F3FF), Color(0xFFEDE8FF), Color(0xFFF6F3FF))
                        }
                    )
                )
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x66A855F7), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.12f),
                    radius = size.width * 0.75f,
                ),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x5538BDF8), Color.Transparent),
                    center = Offset(size.width * 0.9f, size.height * 0.3f),
                    radius = size.width * 0.6f,
                ),
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Hero: the design artwork, faded into the page for a layered look.
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(852f / 310f)
                        .clip(RoundedCornerShape(32.dp)),
                ) {
                    Image(
                        painter = painterResource(R.drawable.about_hero_bg),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Dark mode: fade the artwork into the page. Light mode keeps it as a rounded banner.
                    if (dark) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xFF05060F)),
                                    )
                                ),
                        )
                    }
                }
            }

            // Device card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = glass,
                    shadowElevation = 14.dp,
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            listOf(Color(0x99D946EF), Color(0x6638BDF8), Color(0x33FFFFFF)),
                        ),
                    ),
                ) {
                    Row(
                        modifier = Modifier.height(150.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.about_device_photo),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                            modifier = Modifier.fillMaxHeight().width(128.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Box(modifier = Modifier.width(1.dp).height(72.dp).background(rim))
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Phone, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(text = "Device", style = MaterialTheme.typography.titleMedium, color = ink)
                            }
                            Text(
                                text = row("AboutDeviceNamePreference")?.summary ?: Build.MODEL,
                                style = MaterialTheme.typography.bodyMedium,
                                color = soft,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF7E22CE).copy(alpha = if (dark) 0.55f else 0.2f),
                                                Color(0xFF2563EB).copy(alpha = if (dark) 0.45f else 0.15f),
                                            )
                                        )
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFD946EF), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(text = "Powered by Evolution X", style = MaterialTheme.typography.labelMedium, color = ink)
                            }
                        }
                    }
                }
            }

            // Info rows
            val infoRows = listOf(
                "FirmwareVersionDetailPreference" to Pair(R.drawable.about_icon_android, Color(0xFF4ADE80)),
                "LineageVersionDetailPreference" to Pair(R.drawable.about_icon_evolution, Color(0xFFD946EF)),
                "SecurityPatchLevelPreference" to Pair(R.drawable.about_icon_security, Color(0xFF22D3EE)),
                "LineageVendorSecurityPatchLevelPreference" to Pair(R.drawable.about_icon_vendor, Color(0xFFC026D3)),
                "BasebandVersionPreference" to Pair(R.drawable.about_icon_baseband, Color(0xFF3B82F6)),
                "KernelVersionPreference" to Pair(R.drawable.about_icon_kernel, Color(0xFF2563EB)),
                "LineageBuildDatePreference" to Pair(R.drawable.about_icon_date, Color(0xFF3B82F6)),
                "SimpleBuildNumberPreference" to Pair(R.drawable.about_icon_build, Color(0xFF2563EB)),
            )
            items(infoRows) { (role, styled) ->
                val (iconRes, tint) = styled
                val model = row(role) ?: return@items
                val rowShape = RoundedCornerShape(32.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(rowShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    tint.copy(alpha = if (dark) 0.26f else 0.16f),
                                    glass,
                                )
                            ),
                            rowShape,
                        )
                        .border(1.dp, rim, rowShape)
                        .clickable(enabled = model.clickable) { model.onClick() },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = model.title, style = MaterialTheme.typography.titleMedium, color = ink)
                            model.summary?.takeIf { it.isNotBlank() }?.let {
                                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = soft)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(ink.copy(alpha = 0.10f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = ink)
                        }
                    }
                }
            }

            // Footer
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF38BDF8), Color.Transparent)))
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = if (dark) Color(0xFFF0ABFC) else Color(0xFFA21CAF), fontWeight = FontWeight.SemiBold)) {
                                append("Evolution X")
                            }
                            withStyle(SpanStyle(color = soft)) { append("  •  ") }
                            withStyle(SpanStyle(color = if (dark) Color(0xFF67E8F9) else Color(0xFF0E7490), fontWeight = FontWeight.SemiBold)) {
                                append("Android ${Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY}")
                            }
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassCircle(
    fill: Color,
    rim: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, rim, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
