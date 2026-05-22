// IActiveIntentObserver.aidl
package com.orbit.app.data.ipc;

import com.orbit.app.data.ipc.ActiveIntentParcel;

oneway interface IActiveIntentObserver {
    void onActiveIntentsChanged(in List<ActiveIntentParcel> intents);
}