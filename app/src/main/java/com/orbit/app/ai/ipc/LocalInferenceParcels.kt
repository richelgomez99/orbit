package com.orbit.app.ai.ipc

import android.os.Parcel
import android.os.Parcelable

/**
 * Spec 022 M1 — AIDL parcels for [ILocalInference]. Single `payloadJson`
 * carries the kotlinx.serialization output of [LocalInferenceRequest] /
 * [LocalInferenceResponse]. Manual `writeToParcel`/`CREATOR` (no Parcelize),
 * mirroring the `:net` gateway parcels.
 */
data class LocalInferenceRequestParcel(
    val payloadJson: String,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        payloadJson = requireNotNull(parcel.readString()) { "payloadJson MUST be non-null" },
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(payloadJson)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<LocalInferenceRequestParcel> {
        override fun createFromParcel(parcel: Parcel) = LocalInferenceRequestParcel(parcel)
        override fun newArray(size: Int) = arrayOfNulls<LocalInferenceRequestParcel>(size)
    }
}

data class LocalInferenceResponseParcel(
    val payloadJson: String,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        payloadJson = requireNotNull(parcel.readString()) { "payloadJson MUST be non-null" },
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(payloadJson)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<LocalInferenceResponseParcel> {
        override fun createFromParcel(parcel: Parcel) = LocalInferenceResponseParcel(parcel)
        override fun newArray(size: Int) = arrayOfNulls<LocalInferenceResponseParcel>(size)
    }
}
