package com.orbit.app.data.ipc

import android.os.Parcel
import android.os.Parcelable

data class PromotedMemoryParcel(
    val memoryId: String,
    val memoryKind: String,
    val state: String,
    val displayLabel: String,
    val factText: String,
    val confidenceLabel: String,
    val sensitivity: String,
    val sourceCount: Int,
    val lastUsedAtMillis: Long?,
    val useCount: Int,
    val updatedAtMillis: Long
) : Parcelable {
    constructor(parcel: Parcel) : this(
        memoryId = parcel.readString()!!,
        memoryKind = parcel.readString()!!,
        state = parcel.readString()!!,
        displayLabel = parcel.readString()!!,
        factText = parcel.readString()!!,
        confidenceLabel = parcel.readString()!!,
        sensitivity = parcel.readString()!!,
        sourceCount = parcel.readInt(),
        lastUsedAtMillis = parcel.readLong().let { if (it < 0L) null else it },
        useCount = parcel.readInt(),
        updatedAtMillis = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(memoryId)
        parcel.writeString(memoryKind)
        parcel.writeString(state)
        parcel.writeString(displayLabel)
        parcel.writeString(factText)
        parcel.writeString(confidenceLabel)
        parcel.writeString(sensitivity)
        parcel.writeInt(sourceCount)
        parcel.writeLong(lastUsedAtMillis ?: -1L)
        parcel.writeInt(useCount)
        parcel.writeLong(updatedAtMillis)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<PromotedMemoryParcel> {
        override fun createFromParcel(parcel: Parcel) = PromotedMemoryParcel(parcel)
        override fun newArray(size: Int) = arrayOfNulls<PromotedMemoryParcel>(size)
    }
}
