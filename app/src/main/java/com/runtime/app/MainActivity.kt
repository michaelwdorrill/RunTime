package com.runtime.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runtime.app.accessibility.RunTimeService
import com.runtime.app.core.YouTubeShortsDetector
import com.runtime.app.data.GuardStatus
import com.runtime.app.data.RunTimeSettings
import com.runtime.app.data.RunTimeState

class MainActivity : ComponentActivity() {
    private var systemServiceEnabled by mutableStateOf(false)
    private lateinit var settings: RunTimeSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = RunTimeSettings(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent { RunTimeScreen() }
    }

    override fun onResume() {
        super.onResume()
        val name = ComponentName(this, RunTimeService::class.java)
        systemServiceEnabled = getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.let { info -> ComponentName(info.packageName, info.name) == name } }
    }

    private fun openAccessibilitySettings() {
        // Use the public settings action; vendor-specific service-detail intents are not SDK APIs.
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    @Composable
    private fun RunTimeScreen() {
        val colors = darkColorScheme(
            primary = Color(0xFFD4FF73), onPrimary = Color(0xFF233007),
            background = Color(0xFF101610), surface = Color(0xFF192119),
            onBackground = Color(0xFFF1F5E9), onSurface = Color(0xFFF1F5E9),
            onSurfaceVariant = Color(0xFFBCC7B6), outline = Color(0xFF65715F),
        )
        MaterialTheme(colorScheme = colors) {
            val status by RunTimeState.status.collectAsState()
            var revision by remember { mutableIntStateOf(0) }
            DisposableEffect(settings) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
                settings.preferences.registerOnSharedPreferenceChangeListener(listener)
                onDispose { settings.preferences.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            val consent = remember(revision) { settings.consent }
            val enabled = remember(revision) { settings.enabled }
            val youtube = remember(revision) { settings.youtubeEnabled }
            val haptics = remember(revision) { settings.haptics }
            var showDisclosure by remember { mutableStateOf(false) }
            var showDiagnostics by remember { mutableStateOf(false) }
            Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
                Column(
                    Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                        .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("RunTime", fontSize = 38.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("EARLY BUILD", color = colors.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("One video.\nThen back.", fontSize = 32.sp, lineHeight = 37.sp, fontWeight = FontWeight.Medium)
                        Text("Watch what you opened. Skip the endless swipe.", color = colors.onSurfaceVariant, lineHeight = 23.sp)
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = colors.surface), shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // A live connection is authoritative if Android's settings list lags after an update.
                            val serviceAvailable = systemServiceEnabled || status.connected
                            val ready = consent && status.connected
                            val headline = when {
                                !consent || !serviceAvailable -> "A little setup first"
                                !status.connected -> "Waiting for the service"
                                !enabled -> "Protection is paused"
                                !youtube -> "YouTube protection is off"
                                status.guarding -> "One video protected"
                                else -> "Ready for a Short"
                            }
                            Text(headline, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
                            Text(when {
                                !ready -> "Enable RunTime in Android Accessibility to recognize video screens and block scrolling."
                                !enabled -> "Turn protection on when you want to stop after the video you chose."
                                else -> "Open a YouTube Short. The ONE VIDEO pill means the guard is active. Back lets you leave and choose another."
                            }, color = colors.onSurfaceVariant, lineHeight = 22.sp)
                            if (!serviceAvailable || !status.connected || !consent) {
                                Button(onClick = {
                                    if (consent) openAccessibilitySettings() else showDisclosure = true
                                }, modifier = Modifier.fillMaxWidth()) { Text("Set up accessibility") }
                            }
                        }
                    }
                    SettingRow("Protection", "Keep each visit to one video", enabled, consent) { settings.enabled = it }
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.35f))
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text("YOUR APPS", color = colors.onSurfaceVariant, fontSize = 11.sp, letterSpacing = 2.sp)
                        SettingRow("YouTube Shorts", "Regular YouTube videos stay as they are", youtube, consent) { settings.youtubeEnabled = it }
                        PlannedApp("Instagram Reels")
                        PlannedApp("X videos")
                    }
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.35f))
                    SettingRow("A small nudge", "Vibrate when a swipe is blocked", haptics, consent) { settings.haptics = it }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("First test: YouTube Shorts", fontWeight = FontWeight.SemiBold)
                        Text("Portrait, full-screen phone use. Comments and available seek controls need testing on your YouTube version. Automatic next-video playback is not yet covered.", color = colors.onSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp)
                        Text("Interface checks stay on your phone. No account, internet access, or screen recording.", color = colors.onSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp)
                    }
                    OutlinedButton(onClick = { showDiagnostics = !showDiagnostics }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showDiagnostics) "Hide test details" else "Test details")
                    }
                    if (showDiagnostics) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Current state: ${status.screen}", fontWeight = FontWeight.SemiBold)
                            Text(status.detail, color = colors.onSurfaceVariant)
                            Text("Blocked: ${status.blocked} · Taps: ${status.taps} · Seeks: ${status.seeks} · Failed actions: ${status.failures}", fontSize = 12.sp)
                            Text("Last viewer IDs: ${status.lastViewerMarkers.joinToString().ifEmpty { "None detected yet" }}", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("Android 17 Advanced Protection may restrict this service. If setup is blocked, note Android's exact message.", color = colors.onSurfaceVariant, fontSize = 12.sp)
                            TextButton(onClick = { shareDiagnostics(status) }) { Text("Share diagnostic report") }
                            TextButton(onClick = { openAccessibilitySettings() }) { Text("Accessibility settings") }
                            if (status.events.isNotEmpty()) {
                                Text(status.events.joinToString("\n"), color = colors.onSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                    Text("0.1.0-alpha01  /  Built for an intentional stop", color = colors.onSurfaceVariant, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (showDisclosure) {
                AlertDialog(
                    onDismissRequest = { showDisclosure = false },
                    title = { Text("Allow interface access") },
                    text = {
                        Text("RunTime uses Android Accessibility to read YouTube's interface structure and control locations, recognize Shorts, and intercept video-navigation gestures. It can perform allowed taps and seek actions on your behalf.\n\nIt reads foreground app identity to release the guard when you leave. Interface information is processed on this device; it is not uploaded or recorded. Diagnostic sharing is optional and contains no post or comment text.\n\nThis build supports YouTube Shorts only. You can pause protection in RunTime or disable the service in Android settings at any time.")
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            settings.consent = true
                            settings.enabled = true
                            showDisclosure = false
                            openAccessibilitySettings()
                        }) { Text("Agree and continue") }
                    },
                    dismissButton = { TextButton(onClick = { showDisclosure = false }) { Text("Cancel") } },
                )
            }
        }
    }

    @Composable
    private fun SettingRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
        }
    }

    @Composable
    private fun PlannedApp(name: String) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(name, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("PLANNED", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp))
        }
    }

    private fun shareDiagnostics(status: GuardStatus) {
        val youtubeVersion = try { packageManager.getPackageInfo(YouTubeShortsDetector.PACKAGE, 0).versionName } catch (_: Exception) { "Not installed / unavailable" }
        val report = buildString {
            appendLine("RunTime 0.1.0-alpha01 diagnostic report")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("YouTube: $youtubeVersion")
            appendLine("Service connected: ${status.connected}; protection enabled: ${settings.enabled}; YouTube enabled: ${settings.youtubeEnabled}")
            appendLine("State: ${status.screen}; guard installed: ${status.guarding}")
            appendLine("Detail: ${status.detail}")
            appendLine("Last viewer: ${status.lastViewerDetail}")
            appendLine("Allowlisted viewer IDs: ${status.lastViewerMarkers.joinToString()}")
            appendLine("Blocked=${status.blocked}; taps=${status.taps}; seeks=${status.seeks}; failures=${status.failures}")
            appendLine("Recent state transitions (in-memory only):")
            status.events.forEach { appendLine(it) }
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "RunTime device test")
            putExtra(Intent.EXTRA_TEXT, report)
        }, "Share diagnostic report"))
    }
}
