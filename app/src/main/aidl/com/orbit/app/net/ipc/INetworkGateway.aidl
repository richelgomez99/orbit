// INetworkGateway.aidl
package com.orbit.app.net.ipc;

import com.orbit.app.net.ipc.FetchResultParcel;
import com.orbit.app.net.ipc.LlmGatewayRequestParcel;
import com.orbit.app.net.ipc.LlmGatewayResponseParcel;
import com.orbit.app.net.ipc.MemoryGatewayRequestParcel;
import com.orbit.app.net.ipc.MemoryGatewayResponseParcel;

interface INetworkGateway {
    FetchResultParcel fetchPublicUrl(String url, long timeoutMs);

    /**
     * Spec 013 (FR-013-005) — sole AI Gateway entry point.
     * Sealed-class JSON-in-String round-trip; see
     * specs/013-cloud-llm-routing/data-model.md §3.
     */
    LlmGatewayResponseParcel callLlmGateway(in LlmGatewayRequestParcel request);

    /**
     * Spec 005 — sole compact memory gateway entry point family.
     * JSON-in-String keeps the AIDL surface stable while Kotlin sealed
     * request/response types evolve behind it. Atlas credentials never enter
     * Android; this only calls Orbit's authenticated backend gateway.
     */
    MemoryGatewayResponseParcel callMemoryGateway(in MemoryGatewayRequestParcel request);
}
