package com.orbit.app.data.ipc

import android.os.Parcel
import android.os.Parcelable

data class AgentPlanParcel(
    val planId: String,
    val outcome: String,
    val title: String,
    val summary: String?,
    val steps: List<AgentPlanStepParcel>,
    val questions: List<AgentQuestionParcel>,
    val evidence: List<AgentEvidenceParcel>,
    val limitations: List<String>,
    val modelLabel: String?,
    val createdAtMillis: Long
) : Parcelable {
    constructor(parcel: Parcel) : this(
        planId = parcel.readString()!!,
        outcome = parcel.readString()!!,
        title = parcel.readString()!!,
        summary = parcel.readString(),
        steps = buildList {
            repeat(parcel.readInt()) {
                add(parcel.readParcelable(AgentPlanStepParcel::class.java.classLoader)!!)
            }
        },
        questions = buildList {
            repeat(parcel.readInt()) {
                add(parcel.readParcelable(AgentQuestionParcel::class.java.classLoader)!!)
            }
        },
        evidence = buildList {
            repeat(parcel.readInt()) {
                add(parcel.readParcelable(AgentEvidenceParcel::class.java.classLoader)!!)
            }
        },
        limitations = parcel.createStringArrayList().orEmpty(),
        modelLabel = parcel.readString(),
        createdAtMillis = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(planId)
        parcel.writeString(outcome)
        parcel.writeString(title)
        parcel.writeString(summary)
        parcel.writeInt(steps.size)
        steps.forEach { parcel.writeParcelable(it, flags) }
        parcel.writeInt(questions.size)
        questions.forEach { parcel.writeParcelable(it, flags) }
        parcel.writeInt(evidence.size)
        evidence.forEach { parcel.writeParcelable(it, flags) }
        parcel.writeStringList(limitations)
        parcel.writeString(modelLabel)
        parcel.writeLong(createdAtMillis)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AgentPlanParcel> {
        override fun createFromParcel(parcel: Parcel) = AgentPlanParcel(parcel)
        override fun newArray(size: Int) = arrayOfNulls<AgentPlanParcel>(size)
    }
}
