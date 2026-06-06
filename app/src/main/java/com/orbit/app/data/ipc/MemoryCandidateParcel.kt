package com.orbit.app.data.ipc

import android.os.Parcel
import android.os.Parcelable

data class MemoryCandidateParcel(
    val candidateId: String,
    val candidateKind: String,
    val state: String,
    val displayLabel: String,
    val factText: String,
    val confidenceLabel: String,
    val sensitivity: String,
    val sourceCount: Int,
    val primarySourceEnvelopeId: String?,
    val primarySourceTitle: String?,
    val primarySourceDayLocal: String?,
    val askUserCopy: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) : Parcelable {
    constructor(parcel: Parcel) : this(
        candidateId = parcel.readString()!!,
        candidateKind = parcel.readString()!!,
        state = parcel.readString()!!,
        displayLabel = parcel.readString()!!,
        factText = parcel.readString()!!,
        confidenceLabel = parcel.readString()!!,
        sensitivity = parcel.readString()!!,
        sourceCount = parcel.readInt(),
        primarySourceEnvelopeId = parcel.readString(),
        primarySourceTitle = parcel.readString(),
        primarySourceDayLocal = parcel.readString(),
        askUserCopy = parcel.readString(),
        createdAtMillis = parcel.readLong(),
        updatedAtMillis = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(candidateId)
        parcel.writeString(candidateKind)
        parcel.writeString(state)
        parcel.writeString(displayLabel)
        parcel.writeString(factText)
        parcel.writeString(confidenceLabel)
        parcel.writeString(sensitivity)
        parcel.writeInt(sourceCount)
        parcel.writeString(primarySourceEnvelopeId)
        parcel.writeString(primarySourceTitle)
        parcel.writeString(primarySourceDayLocal)
        parcel.writeString(askUserCopy)
        parcel.writeLong(createdAtMillis)
        parcel.writeLong(updatedAtMillis)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<MemoryCandidateParcel> {
        override fun createFromParcel(parcel: Parcel) = MemoryCandidateParcel(parcel)
        override fun newArray(size: Int) = arrayOfNulls<MemoryCandidateParcel>(size)
    }
}
