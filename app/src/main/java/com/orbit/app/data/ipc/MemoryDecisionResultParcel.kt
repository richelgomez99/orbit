package com.orbit.app.data.ipc

import android.os.Parcel
import android.os.Parcelable

data class MemoryDecisionResultParcel(
    val ok: Boolean,
    val candidateId: String?,
    val memoryId: String?,
    val status: String,
    val message: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        ok = parcel.readInt() == 1,
        candidateId = parcel.readString(),
        memoryId = parcel.readString(),
        status = parcel.readString()!!,
        message = parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(if (ok) 1 else 0)
        parcel.writeString(candidateId)
        parcel.writeString(memoryId)
        parcel.writeString(status)
        parcel.writeString(message)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<MemoryDecisionResultParcel> {
        override fun createFromParcel(parcel: Parcel) = MemoryDecisionResultParcel(parcel)
        override fun newArray(size: Int) = arrayOfNulls<MemoryDecisionResultParcel>(size)
    }
}
