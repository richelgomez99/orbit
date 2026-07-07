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

    /**
     * Spec 022 — BYOM model download. :net is the sole network egress, so
     * the ~500MB+ model fetch runs here and streams to the app's shared
     * files dir (no bytes cross Binder). Fire-and-forget: progress + the
     * finished bundle land on disk via ModelDownloadStore, which :ml mmaps
     * and the UI polls. `oneway` so the caller never blocks on the
     * multi-minute transfer.
     *
     * `authToken` (nullable) is an optional bearer credential for gated
     * hosts (e.g. a Hugging Face token for license-gated Gemma weights).
     * It is sent only as an `Authorization: Bearer` header on the initial
     * request and never persisted. Credentials belong in :net (sole egress);
     * :ml never sees it.
     */
    oneway void startModelDownload(String modelId, String url, long expectedBytes, String authToken);
}
