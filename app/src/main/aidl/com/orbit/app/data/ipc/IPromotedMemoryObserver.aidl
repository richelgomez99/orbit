package com.orbit.app.data.ipc;

import com.orbit.app.data.ipc.PromotedMemoryParcel;

oneway interface IPromotedMemoryObserver {
    void onPromotedMemoriesChanged(in List<PromotedMemoryParcel> memories);
}
