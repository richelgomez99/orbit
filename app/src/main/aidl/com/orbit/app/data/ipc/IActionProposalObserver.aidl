// IActionProposalObserver.aidl
package com.orbit.app.data.ipc;

import com.orbit.app.data.ipc.ActionProposalParcel;

/**
 * UI-side callback delivered to {@link IEnvelopeRepository#observeProposalsForEnvelope}.
 *
 * The repository emits the full current set on every change so observers
 * never need to reconstruct state from deltas (mirrors IEnvelopeObserver).
 */
oneway interface IActionProposalObserver {
    void onProposals(in List<ActionProposalParcel> proposals);
}
