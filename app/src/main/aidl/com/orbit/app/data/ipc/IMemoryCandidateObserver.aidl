package com.orbit.app.data.ipc;

import com.orbit.app.data.ipc.MemoryCandidateParcel;

oneway interface IMemoryCandidateObserver {
    void onMemoryCandidatesChanged(in List<MemoryCandidateParcel> candidates);
}
