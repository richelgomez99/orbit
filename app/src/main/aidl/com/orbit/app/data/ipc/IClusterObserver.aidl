// IClusterObserver.aidl
package com.orbit.app.data.ipc;

import com.orbit.app.data.ipc.ClusterCardParcel;

oneway interface IClusterObserver {
    void onClustersChanged(in List<ClusterCardParcel> clusters);
}
