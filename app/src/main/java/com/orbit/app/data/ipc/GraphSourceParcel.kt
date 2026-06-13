package com.orbit.app.data.ipc

import android.os.Parcel
import android.os.Parcelable

data class GraphSourceParcel(
    val sourceType: String,
    val sourceId: String,
    val label: String,
    val dayLocal: String?,
    val createdAtMillis: Long?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        sourceType = parcel.readString()!!,
        sourceId = parcel.readString()!!,
        label = parcel.readString()!!,
        dayLocal = parcel.readString(),
        createdAtMillis = parcel.readLong().let { if (it < 0L) null else it }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(sourceType)
        parcel.writeString(sourceId)
        parcel.writeString(label)
        parcel.writeString(dayLocal)
        parcel.writeLong(createdAtMillis ?: -1L)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<GraphSourceParcel> {
        override fun createFromParcel(parcel: Parcel) = GraphSourceParcel(parcel)
        override fun newArray(size: Int) = arrayOfNulls<GraphSourceParcel>(size)
    }
}
