package com.orbit.app.data.ipc

import android.os.Parcel
import android.os.Parcelable

data class AgentQuestionParcel(
    val questionId: String,
    val text: String,
    val choices: List<AgentChoiceParcel>,
    val evidenceIds: List<String>
) : Parcelable {
    constructor(parcel: Parcel) : this(
        questionId = parcel.readString()!!,
        text = parcel.readString()!!,
        choices = buildList {
            repeat(parcel.readInt()) {
                add(parcel.readParcelable(AgentChoiceParcel::class.java.classLoader)!!)
            }
        },
        evidenceIds = parcel.createStringArrayList().orEmpty()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(questionId)
        parcel.writeString(text)
        parcel.writeInt(choices.size)
        choices.forEach { parcel.writeParcelable(it, flags) }
        parcel.writeStringList(evidenceIds)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AgentQuestionParcel> {
        override fun createFromParcel(parcel: Parcel) = AgentQuestionParcel(parcel)
        override fun newArray(size: Int) = arrayOfNulls<AgentQuestionParcel>(size)
    }
}
