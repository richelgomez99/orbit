package com.orbit.app.data.ipc

import android.os.Parcel
import android.os.Parcelable

/**
 * Compact Binder projection for Orbit's action workspace.
 *
 * This joins an ActionProposal with source-envelope display metadata and
 * AppFunction side-effect labels. It intentionally carries no raw envelope
 * body beyond the existing proposal args needed by the approval sheet.
 */
data class ActionDraftParcel(
    val proposalId: String,
    val sourceEnvelopeId: String,
    val functionId: String,
    val schemaVersion: Int,
    val argsJson: String,
    val previewTitle: String,
    val previewSubtitle: String?,
    val confidence: Float,
    val provenance: String,
    val state: String,
    val sensitivityScope: String,
    val createdAtMillis: Long,
    val stateChangedAtMillis: Long,
    val displayName: String,
    val sideEffects: String,
    val reversibility: String,
    val sourceTitle: String,
    val sourceAppLabel: String?,
    val sourceDayLocal: String
) : Parcelable {

    constructor(parcel: Parcel) : this(
        proposalId = parcel.readString()!!,
        sourceEnvelopeId = parcel.readString()!!,
        functionId = parcel.readString()!!,
        schemaVersion = parcel.readInt(),
        argsJson = parcel.readString()!!,
        previewTitle = parcel.readString()!!,
        previewSubtitle = parcel.readString(),
        confidence = parcel.readFloat(),
        provenance = parcel.readString()!!,
        state = parcel.readString()!!,
        sensitivityScope = parcel.readString()!!,
        createdAtMillis = parcel.readLong(),
        stateChangedAtMillis = parcel.readLong(),
        displayName = parcel.readString()!!,
        sideEffects = parcel.readString()!!,
        reversibility = parcel.readString()!!,
        sourceTitle = parcel.readString()!!,
        sourceAppLabel = parcel.readString(),
        sourceDayLocal = parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(proposalId)
        parcel.writeString(sourceEnvelopeId)
        parcel.writeString(functionId)
        parcel.writeInt(schemaVersion)
        parcel.writeString(argsJson)
        parcel.writeString(previewTitle)
        parcel.writeString(previewSubtitle)
        parcel.writeFloat(confidence)
        parcel.writeString(provenance)
        parcel.writeString(state)
        parcel.writeString(sensitivityScope)
        parcel.writeLong(createdAtMillis)
        parcel.writeLong(stateChangedAtMillis)
        parcel.writeString(displayName)
        parcel.writeString(sideEffects)
        parcel.writeString(reversibility)
        parcel.writeString(sourceTitle)
        parcel.writeString(sourceAppLabel)
        parcel.writeString(sourceDayLocal)
    }

    override fun describeContents(): Int = 0

    fun toProposalParcel(): ActionProposalParcel = ActionProposalParcel(
        id = proposalId,
        envelopeId = sourceEnvelopeId,
        functionId = functionId,
        schemaVersion = schemaVersion,
        argsJson = argsJson,
        previewTitle = previewTitle,
        previewSubtitle = previewSubtitle,
        confidence = confidence,
        provenance = provenance,
        state = state,
        sensitivityScope = sensitivityScope,
        createdAtMillis = createdAtMillis,
        stateChangedAtMillis = stateChangedAtMillis
    )

    companion object CREATOR : Parcelable.Creator<ActionDraftParcel> {
        override fun createFromParcel(parcel: Parcel) = ActionDraftParcel(parcel)
        override fun newArray(size: Int) = arrayOfNulls<ActionDraftParcel>(size)
    }
}
