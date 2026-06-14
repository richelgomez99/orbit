package com.orbit.app.data.ipc

import android.os.Parcel
import android.os.Parcelable

data class AgentChoiceParcel(
    val choiceId: String,
    val label: String,
    val evidenceIds: List<String>
) : Parcelable {
    constructor(parcel: Parcel) : this(
        choiceId = parcel.readString()!!,
        label = parcel.readString()!!,
        evidenceIds = parcel.createStringArrayList().orEmpty()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(choiceId)
        parcel.writeString(label)
        parcel.writeStringList(evidenceIds)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AgentChoiceParcel> {
        override fun createFromParcel(parcel: Parcel) = AgentChoiceParcel(parcel)
        override fun newArray(size: Int) = arrayOfNulls<AgentChoiceParcel>(size)
    }
}
