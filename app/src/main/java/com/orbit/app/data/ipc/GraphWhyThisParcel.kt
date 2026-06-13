package com.orbit.app.data.ipc

import android.os.Parcel
import android.os.Parcelable

data class GraphWhyThisParcel(
    val targetType: String,
    val targetId: String,
    val title: String,
    val summary: String?,
    val sources: List<GraphSourceParcel>
) : Parcelable {
    constructor(parcel: Parcel) : this(
        targetType = parcel.readString()!!,
        targetId = parcel.readString()!!,
        title = parcel.readString()!!,
        summary = parcel.readString(),
        sources = buildList {
            repeat(parcel.readInt()) {
                add(parcel.readParcelable(GraphSourceParcel::class.java.classLoader)!!)
            }
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(targetType)
        parcel.writeString(targetId)
        parcel.writeString(title)
        parcel.writeString(summary)
        parcel.writeInt(sources.size)
        sources.forEach { parcel.writeParcelable(it, flags) }
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<GraphWhyThisParcel> {
        override fun createFromParcel(parcel: Parcel) = GraphWhyThisParcel(parcel)
        override fun newArray(size: Int) = arrayOfNulls<GraphWhyThisParcel>(size)
    }
}
