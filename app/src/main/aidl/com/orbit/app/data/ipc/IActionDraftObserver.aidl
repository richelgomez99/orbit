// IActionDraftObserver.aidl
package com.orbit.app.data.ipc;

import com.orbit.app.data.ipc.ActionDraftParcel;

oneway interface IActionDraftObserver {
    void onActionDraftsChanged(in List<ActionDraftParcel> drafts);
}
