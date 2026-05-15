package com.orbit.app.understanding.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.orbit.app.understanding.domain.EvidenceLevel
import com.orbit.app.understanding.domain.GroundingConstraints
import com.orbit.app.understanding.domain.UnderstandingMode
import com.orbit.app.understanding.domain.UnderstandingResult
import com.orbit.app.understanding.domain.UnderstandingStatus

fun interface ConnectivityReader {
    fun isOnline(context: Context): Boolean
}

fun interface CloudPreferenceReader {
    fun isCloudEnabled(context: Context): Boolean
}

data class LocalFallbackResult(
    val reason: String,
    val groundingConstraint: String
) {
    fun toUnderstandingResult(captureId: String, mode: UnderstandingMode): UnderstandingResult =
        UnderstandingResult(
            captureId = captureId,
            status = UnderstandingStatus.LIMITED,
            mode = mode,
            title = null,
            summaryText = null,
            groundingConstraints = GroundingConstraints(
                constraints = listOf(groundingConstraint),
                evidenceLevel = EvidenceLevel.METADATA_ONLY
            ),
            sourceIdentityJson = null,
            contentHashHex = null,
            canonicalUrl = null,
            evidenceBundleIds = emptyList(),
            duplicateMatch = null
        )
}

class LocalFallbackGuard(
    private val connectivityReader: ConnectivityReader = AndroidConnectivityReader,
    private val cloudPreferenceReader: CloudPreferenceReader = SharedPreferencesCloudPreferenceReader
) {
    fun check(context: Context): LocalFallbackResult? {
        return checkState(
            isOnline = connectivityReader.isOnline(context),
            isCloudEnabled = cloudPreferenceReader.isCloudEnabled(context)
        )
    }

    fun checkState(isOnline: Boolean, isCloudEnabled: Boolean): LocalFallbackResult? {
        if (!isOnline) {
            return LocalFallbackResult(
                reason = "offline",
                groundingConstraint = "cloud-unavailable"
            )
        }
        if (!isCloudEnabled) {
            return LocalFallbackResult(
                reason = "cloud-disabled",
                groundingConstraint = "cloud-unavailable"
            )
        }
        return null
    }
}

object AndroidConnectivityReader : ConnectivityReader {
    override fun isOnline(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

object SharedPreferencesCloudPreferenceReader : CloudPreferenceReader {
    const val PREFS_NAME = "orbit_understanding"
    const val CLOUD_ENABLED_KEY = "cloud_enabled"

    override fun isCloudEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(CLOUD_ENABLED_KEY, false)
}
