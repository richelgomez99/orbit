package com.capsule.app.data.ipc

import android.os.Parcel
import android.os.Parcelable

data class CaptureUnderstandingSummaryParcel(
    val captureId: String,
    val sourceIdentityId: String?,
    val understandingId: String?,
    val currentJobId: String?,
    val sourceLabel: String,
    val secondarySourceLabel: String?,
    val glyphKind: String,
    val title: String?,
    val compactSummary: String?,
    val status: String,
    val confidenceBand: String?,
    val limitationCodes: List<String>,
    val limitationSummary: String?,
    val depthUsed: String,
    val retryEligible: Boolean,
    val correctionAvailable: Boolean,
    val evidenceSummaryCount: Int,
    val evidencePageToken: String?
) : Parcelable {

    init {
        require(sourceLabel.length <= MAX_SOURCE_LABEL)
        require((secondarySourceLabel?.length ?: 0) <= MAX_SOURCE_LABEL)
        require((title?.length ?: 0) <= MAX_TITLE)
        require((compactSummary?.length ?: 0) <= MAX_COMPACT_SUMMARY)
        require((limitationSummary?.length ?: 0) <= MAX_LIMITATION_SUMMARY)
        require(limitationCodes.size <= MAX_LIMITATION_CODES)
        require((evidencePageToken?.length ?: 0) <= MAX_EVIDENCE_PAGE_TOKEN)
    }

    constructor(parcel: Parcel) : this(
        captureId = parcel.readString()!!,
        sourceIdentityId = parcel.readString(),
        understandingId = parcel.readString(),
        currentJobId = parcel.readString(),
        sourceLabel = parcel.readString()!!,
        secondarySourceLabel = parcel.readString(),
        glyphKind = parcel.readString()!!,
        title = parcel.readString(),
        compactSummary = parcel.readString(),
        status = parcel.readString()!!,
        confidenceBand = parcel.readString(),
        limitationCodes = buildList {
            parcel.readStringList(this)
        },
        limitationSummary = parcel.readString(),
        depthUsed = parcel.readString()!!,
        retryEligible = parcel.readInt() != 0,
        correctionAvailable = parcel.readInt() != 0,
        evidenceSummaryCount = parcel.readInt(),
        evidencePageToken = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(captureId)
        parcel.writeString(sourceIdentityId)
        parcel.writeString(understandingId)
        parcel.writeString(currentJobId)
        parcel.writeString(sourceLabel)
        parcel.writeString(secondarySourceLabel)
        parcel.writeString(glyphKind)
        parcel.writeString(title)
        parcel.writeString(compactSummary)
        parcel.writeString(status)
        parcel.writeString(confidenceBand)
        parcel.writeStringList(limitationCodes)
        parcel.writeString(limitationSummary)
        parcel.writeString(depthUsed)
        parcel.writeInt(if (retryEligible) 1 else 0)
        parcel.writeInt(if (correctionAvailable) 1 else 0)
        parcel.writeInt(evidenceSummaryCount)
        parcel.writeString(evidencePageToken)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<CaptureUnderstandingSummaryParcel> {
        const val MAX_SOURCE_LABEL = 80
        const val MAX_TITLE = 160
        const val MAX_COMPACT_SUMMARY = 600
        const val MAX_LIMITATION_SUMMARY = 240
        const val MAX_LIMITATION_CODES = 12
        const val MAX_EVIDENCE_PAGE_TOKEN = 128
        const val MAX_EVIDENCE_PAGE_SIZE = 20
        const val MAX_MARSHALLED_BYTES = 32 * 1024

        fun bounded(
            captureId: String,
            sourceIdentityId: String?,
            understandingId: String?,
            currentJobId: String?,
            sourceLabel: String,
            secondarySourceLabel: String?,
            glyphKind: String,
            title: String?,
            compactSummary: String?,
            status: String,
            confidenceBand: String?,
            limitationCodes: List<String>,
            limitationSummary: String?,
            depthUsed: String,
            retryEligible: Boolean,
            correctionAvailable: Boolean,
            evidenceSummaryCount: Int,
            evidencePageToken: String?
        ): CaptureUnderstandingSummaryParcel = CaptureUnderstandingSummaryParcel(
            captureId = captureId,
            sourceIdentityId = sourceIdentityId,
            understandingId = understandingId,
            currentJobId = currentJobId,
            sourceLabel = sourceLabel.cap(MAX_SOURCE_LABEL),
            secondarySourceLabel = secondarySourceLabel?.cap(MAX_SOURCE_LABEL),
            glyphKind = glyphKind,
            title = title?.cap(MAX_TITLE),
            compactSummary = compactSummary?.cap(MAX_COMPACT_SUMMARY),
            status = status,
            confidenceBand = confidenceBand,
            limitationCodes = limitationCodes.take(MAX_LIMITATION_CODES).map { it.cap(MAX_SOURCE_LABEL) },
            limitationSummary = limitationSummary?.cap(MAX_LIMITATION_SUMMARY),
            depthUsed = depthUsed,
            retryEligible = retryEligible,
            correctionAvailable = correctionAvailable,
            evidenceSummaryCount = evidenceSummaryCount.coerceAtLeast(0),
            evidencePageToken = evidencePageToken?.cap(MAX_EVIDENCE_PAGE_TOKEN)
        )

        override fun createFromParcel(parcel: Parcel) = CaptureUnderstandingSummaryParcel(parcel)
        override fun newArray(size: Int) = arrayOfNulls<CaptureUnderstandingSummaryParcel>(size)

        private fun String.cap(max: Int): String = if (length <= max) this else take(max)
    }
}