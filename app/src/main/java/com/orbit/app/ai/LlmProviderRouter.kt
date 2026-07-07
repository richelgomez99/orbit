package com.orbit.app.ai

import android.content.Context
import com.orbit.app.RuntimeFlags
import com.orbit.app.ai.local.DeviceAiHardware
import com.orbit.app.ai.local.LocalModelRoute
import com.orbit.app.ai.local.LocalModelSelection
import com.orbit.app.ai.local.LocalModelSelectionPolicy
import com.orbit.app.ai.local.LocalModelTier
import com.orbit.app.ai.local.byomProviderForSelection
import com.orbit.app.ai.local.installedLocalModels
import com.orbit.app.net.ipc.INetworkGateway
import com.orbit.app.settings.PrivacyPreferences

/**
 * Spec 013 (FR-013-014, FR-013-015) — single resolution point for the
 * production [LlmProvider] selection.
 *
 * Resolution rules (per data-model + spec 013 acceptance scenarios):
 *  - if [RuntimeFlags.useLocalAi] is `true` and a BYOM local model selection
 *    plus provider are supplied → return that local provider.
 *  - otherwise, if [RuntimeFlags.useLocalAi] is `true` AND
 *    [hasNanoCapableHardware] is `true` → return [NanoLlmProvider]
 *    (current/legacy local mode).
 *  - otherwise → return [CloudLlmProvider] backed by the supplied
 *    [INetworkGateway]. `null` here is a programmer error and surfaces
 *    via [checkNotNull] with a message naming the missing dependency.
 *
 * Day-1: [hasNanoCapableHardware] is a stub returning `false`
 * unconditionally — see TODO. Therefore even with `useLocalAi = true`
 * the router transparently falls through to the cloud impl until a real
 * BYOM/local-model-manager provider or Nano hardware probe is supplied.
 *
 * The router itself does NOT alter [NanoLlmProvider] (FR-013-017): the
 * local-mode impl stays byte-for-byte unchanged. Only the construction
 * site moves from each call site into this object.
 */
object LlmProviderRouter {

    /**
     * @param networkGateway required when the resolution lands on the
     *   cloud branch. May be `null` only in code paths that are
     *   guaranteed to land on the local branch (e.g. the cluster
     *   detection worker, which uses [NanoLlmProvider] directly per
     *   the FR-013-016 carve-out and does not call this object).
     */
    fun create(
        context: Context,
        networkGateway: INetworkGateway?,
    ): LlmProvider {
        val selection = productionSelection(context)
        return resolve(
            useLocalAi = RuntimeFlags.useLocalAi,
            hasNanoCapableHardware = hasNanoCapableHardware(),
            cloudAiRoutingEnabled = PrivacyPreferences(context).cloudAiRoutingEnabled,
            networkGateway = networkGateway,
            localModelSelection = selection,
            byomLocalProvider = byomProviderForSelection(context, selection),
        )
    }

    /**
     * Spec 022 — run the pure [LocalModelSelectionPolicy] against real
     * device hardware and on-disk install state. This is the single place
     * the router learns "is a local model actually usable right now".
     */
    private fun productionSelection(context: Context): LocalModelSelection =
        LocalModelSelectionPolicy.select(
            localFirstEnabled = RuntimeFlags.useLocalAi,
            cloudRoutingEnabled = PrivacyPreferences(context).cloudAiRoutingEnabled,
            hardware = DeviceAiHardware.probe(context),
            installedModels = installedLocalModels(context),
        )

    /**
     * Day-1 sugar for call sites whose cloud migration is deferred.
     * Always returns [NanoLlmProvider] via the resolve() path. To be
     * removed when each call site grows the proper `:net` binding
     * (per-site follow-up specs).
     *
     * Routes through the router so the FR-013-016 grep invariant
     * (no direct `NanoLlmProvider()` outside the carve-outs) is
     * satisfied without each consumer having to plumb an
     * [INetworkGateway] through its constructor today.
     */
    fun createPreferLocal(
        context: Context,
    ): LlmProvider {
        // Spec 022 — prefer a real BYOM local model when one is installed and
        // selected; otherwise preserve the Day-1 behaviour (Nano stub) so the
        // FR-013-016 grep invariant (no direct NanoLlmProvider() outside the
        // carve-outs) still holds and cloud-migration of these sites can defer.
        byomProviderForSelection(context, productionSelection(context))?.let { return it }
        return NanoLlmProvider()
    }

    /**
     * Pure resolution function for unit testing — no Android types.
     * `create` is the production entry point; tests exercise the
     * resolution rules through this.
     */
    internal fun resolve(
        useLocalAi: Boolean,
        hasNanoCapableHardware: Boolean,
        cloudAiRoutingEnabled: Boolean = true,
        networkGateway: INetworkGateway?,
        localModelSelection: LocalModelSelection? = null,
        byomLocalProvider: LlmProvider? = null,
    ): LlmProvider = if (useLocalAi && localModelSelection?.route == LocalModelRoute.LOCAL) {
        when (localModelSelection.tier) {
            LocalModelTier.SPEED,
            LocalModelTier.INTELLIGENCE -> byomLocalProvider ?: if (cloudAiRoutingEnabled) {
                cloudProvider(networkGateway)
            } else {
                UnavailableLlmProvider("local model selected but provider unavailable")
            }
            LocalModelTier.LEGACY_NANO -> NanoLlmProvider()
            LocalModelTier.CLOUD,
            null -> if (cloudAiRoutingEnabled) {
                cloudProvider(networkGateway)
            } else {
                UnavailableLlmProvider("local model selection invalid and cloud disabled")
            }
        }
    } else if (
        useLocalAi &&
        localModelSelection?.route == LocalModelRoute.UNAVAILABLE &&
        !cloudAiRoutingEnabled
    ) {
        UnavailableLlmProvider(localModelSelection.reason)
    } else if (useLocalAi && hasNanoCapableHardware) {
        NanoLlmProvider()
    } else if (!cloudAiRoutingEnabled) {
        UnavailableLlmProvider()
    } else {
        cloudProvider(networkGateway)
    }

    private fun cloudProvider(networkGateway: INetworkGateway?): LlmProvider = CloudLlmProvider(
        checkNotNull(networkGateway) {
            "networkGateway required for cloud mode (LlmProviderRouter)"
        },
    )

    /**
     * TODO: real Pixel 9 Pro / S24 detection — separate spec.
     *
     * Day-1 stub: returns `false` unconditionally so the router always
     * falls through to [CloudLlmProvider]. The Block 10 hardware-probe
     * spec will replace this with `Build.MODEL` / AICore-availability
     * checks.
     */
    private fun hasNanoCapableHardware(): Boolean = false
}
