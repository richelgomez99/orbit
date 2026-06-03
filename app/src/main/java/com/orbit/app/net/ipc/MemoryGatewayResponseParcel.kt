package com.orbit.app.net.ipc

import android.os.Parcel
import android.os.Parcelable

/**
 * Spec 005 — JSON-in-String response parcel for compact memory gateway calls.
 */
data class MemoryGatewayResponseParcel(
    val payloadJson: String,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        payloadJson = requireNotNull(parcel.readString()) { "payloadJson MUST be non-null" },
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(payloadJson)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<MemoryGatewayResponseParcel> {
        override fun createFromParcel(parcel: Parcel) = MemoryGatewayResponseParcel(parcel)
        override fun newArray(size: Int) = arrayOfNulls<MemoryGatewayResponseParcel>(size)
    }
}
