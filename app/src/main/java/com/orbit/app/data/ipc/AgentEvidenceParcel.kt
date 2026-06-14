package com.orbit.app.data.ipc

import android.os.Parcel
import android.os.Parcelable

data class AgentEvidenceParcel(
    val evidenceId: String,
    val sourceType: String,
    val sourceId: String,
    val label: String,
    val dayLocal: String?,
    val whyThisTargetType: String?,
    val whyThisTargetId: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        evidenceId = parcel.readString()!!,
        sourceType = parcel.readString()!!,
        sourceId = parcel.readString()!!,
        label = parcel.readString()!!,
        dayLocal = parcel.readString(),
        whyThisTargetType = parcel.readString(),
        whyThisTargetId = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(evidenceId)
        parcel.writeString(sourceType)
        parcel.writeString(sourceId)
        parcel.writeString(label)
        parcel.writeString(dayLocal)
        parcel.writeString(whyThisTargetType)
        parcel.writeString(whyThisTargetId)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AgentEvidenceParcel> {
        override fun createFromParcel(parcel: Parcel) = AgentEvidenceParcel(parcel)
        override fun newArray(size: Int) = arrayOfNulls<AgentEvidenceParcel>(size)
    }
}
