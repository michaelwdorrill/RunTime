package com.runtime.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Only predefined states and allowlisted structural IDs enter this in-memory report. */
data class GuardStatus(
    val connected: Boolean = false,
    val guarding: Boolean = false,
    val screen: String = "Waiting for accessibility setup",
    val detail: String = "Enable the service to begin",
    val markerIds: List<String> = emptyList(),
    val lastViewerMarkers: List<String> = emptyList(),
    val lastViewerDetail: String = "No viewer detected yet",
    val blocked: Int = 0,
    val taps: Int = 0,
    val seeks: Int = 0,
    val failures: Int = 0,
    val events: List<String> = emptyList(),
)

object RunTimeState {
    private val mutableStatus = MutableStateFlow(GuardStatus())
    val status = mutableStatus.asStateFlow()

    fun connection(connected: Boolean) {
        mutableStatus.update { it.copy(connected = connected, guarding = false,
            screen = if (connected) "Ready" else "Service disconnected",
            detail = if (connected) "Waiting for a supported Short" else "Enable the accessibility service") }
    }

    fun screen(name: String, detail: String, guarding: Boolean, markers: List<String> = emptyList()) {
        mutableStatus.update {
            val changed = it.screen != name || it.detail != detail || it.guarding != guarding
            it.copy(screen = name, detail = detail, guarding = guarding, markerIds = markers,
                lastViewerMarkers = markers.ifEmpty { it.lastViewerMarkers },
                lastViewerDetail = if (markers.isNotEmpty()) detail else it.lastViewerDetail,
                events = if (changed) (it.events + "$name: $detail").takeLast(16) else it.events)
        }
    }

    fun blocked() { mutableStatus.update { it.copy(blocked = it.blocked + 1) } }
    fun tapped() { mutableStatus.update { it.copy(taps = it.taps + 1) } }
    fun sought() { mutableStatus.update { it.copy(seeks = it.seeks + 1) } }
    fun failed(reason: String) {
        mutableStatus.update { it.copy(failures = it.failures + 1, events = (it.events + reason).takeLast(16)) }
    }
}
