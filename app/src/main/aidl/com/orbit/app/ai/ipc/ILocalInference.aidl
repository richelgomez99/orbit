// ILocalInference.aidl — spec 022 (M1)
package com.orbit.app.ai.ipc;

import com.orbit.app.ai.ipc.LocalInferenceRequestParcel;
import com.orbit.app.ai.ipc.LocalInferenceResponseParcel;

/**
 * Spec 022 M1 — sole entry point for on-device BYOM inference, hosted by
 * LocalInferenceService in the :ml process. Consolidates all local
 * generation onto a SINGLE MediaPipeLlmProvider engine: other processes
 * proxy here rather than each loading their own engine (Principle II/VI —
 * inference lives in :ml; no network; also avoids N engines across
 * processes and the one-LlmInference-per-process constraint).
 *
 * JSON-in-String round-trip (sealed LocalInferenceRequest/Response) keeps
 * the AIDL surface stable, mirroring INetworkGateway. Synchronous: callers
 * invoke off the main thread; the :ml side serializes on the engine lock.
 */
interface ILocalInference {
    LocalInferenceResponseParcel infer(in LocalInferenceRequestParcel request);
}
