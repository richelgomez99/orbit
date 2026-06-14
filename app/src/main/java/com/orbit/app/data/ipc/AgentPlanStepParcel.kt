package com.orbit.app.data.ipc

import android.os.Parcel
import android.os.Parcelable

data class AgentPlanStepParcel(
    val stepId: String,
    val kind: String,
    val label: String,
    val detail: String?,
    val requiredApproval: Boolean,
    val actionProposalId: String?,
    val functionId: String?,
    val evidenceIds: List<String>
) : Parcelable {
    constructor(parcel: Parcel) : this(
        stepId = parcel.readString()!!,
        kind = parcel.readString()!!,
        label = parcel.readString()!!,
        detail = parcel.readString(),
        requiredApproval = parcel.readInt() == 1,
        actionProposalId = parcel.readString(),
        functionId = parcel.readString(),
        evidenceIds = parcel.createStringArrayList().orEmpty()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(stepId)
        parcel.writeString(kind)
        parcel.writeString(label)
        parcel.writeString(detail)
        parcel.writeInt(if (requiredApproval) 1 else 0)
        parcel.writeString(actionProposalId)
        parcel.writeString(functionId)
        parcel.writeStringList(evidenceIds)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AgentPlanStepParcel> {
        override fun createFromParcel(parcel: Parcel) = AgentPlanStepParcel(parcel)
        override fun newArray(size: Int) = arrayOfNulls<AgentPlanStepParcel>(size)
    }
}
