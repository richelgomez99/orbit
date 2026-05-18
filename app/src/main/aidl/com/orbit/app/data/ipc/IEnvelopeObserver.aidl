// IEnvelopeObserver.aidl
package com.orbit.app.data.ipc;

import com.orbit.app.data.ipc.DayPageParcel;

oneway interface IEnvelopeObserver {
    void onDayLoaded(in DayPageParcel page);
}
