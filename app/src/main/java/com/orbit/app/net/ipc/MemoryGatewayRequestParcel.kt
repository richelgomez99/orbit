package com.orbit.app.net.ipc

import android.os.Parcel
import android.os.Parcelable

/**
 * Spec 005 — JSON-in-String request parcel for compact memory gateway calls.
 */
data class MemoryGatewayRequestParcel(
    val payloadJson: String,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        payloadJson = requireNotNull(parcel.readString()) { "payloadJson MUST be non-null" },
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(payloadJson)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<MemoryGatewayRequestParcel> {
        override fun createFromParcel(parcel: Parcel) = MemoryGatewayRequestParcel(parcel)
        override fun newArray(size: Int) = arrayOfNulls<MemoryGatewayRequestParcel>(size)
    }
}
