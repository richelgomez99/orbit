package com.orbit.app.agent

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.orbit.app.ai.LlmProvider
import com.orbit.app.ai.LlmProviderRouter
import com.orbit.app.ai.model.LlmProvenance
import com.orbit.app.net.NetworkGatewayService
import com.orbit.app.net.ipc.INetworkGateway
import com.orbit.app.settings.PrivacyPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

interface AgentModelAssist {
    suspend fun assist(
        request: AgentRequest,
        deterministicPlan: AgentPlanDraft,
        actionCapabilities: List<AgentActionCapability>,
    ): AgentPlanDraft?
}

class AgentModelAssistPlanner(
    private val llmProvider: LlmProvider,
) : AgentModelAssist {

    override suspend fun assist(
        request: AgentRequest,
        deterministicPlan: AgentPlanDraft,
        actionCapabilities: List<AgentActionCapability>,
    ): AgentPlanDraft? {
        if (!request.allowModelAssist) return null
        if (deterministicPlan.outcome != AgentPlanOutcome.PLAN) return null
        if (deterministicPlan.evidence.isEmpty()) return null

        val response = runCatching {
            llmProvider.summarize(
                text = buildPlanningBrief(request, deterministicPlan, actionCapabilities),
                maxTokens = 180,
            )
        }.getOrNull() ?: return null

        val parsed = parseAssistResponse(response.text) ?: return null
        val candidate = applyParsedAssist(
            base = deterministicPlan,
            parsed = parsed,
            modelLabel = response.provenance.toModelLabel(),
        ) ?: return null

        return candidate.takeIf { AgentPlanValidator.validate(it).isEmpty() }
    }

    private fun buildPlanningBrief(
        request: AgentRequest,
        plan: AgentPlanDraft,
        actionCapabilities: List<AgentActionCapability>,
    ): String = buildString {
        appendLine("Orbit agent copy assist. Rewrite display copy only.")
        appendLine("Use only these local evidence ids, step ids, and function ids.")
        appendLine("Return exact lines: TITLE:, SUMMARY:, STEP <stepId>:, CITE <stepId>:, FUNCTION <stepId>:")
        appendLine("Request: ${request.query.take(MAX_QUERY_CHARS)}")
        appendLine("Evidence:")
        plan.evidence.forEach { evidence ->
            appendLine("- ${evidence.evidenceId}: ${evidence.label.take(MAX_LABEL_CHARS)}")
        }
        appendLine("Steps:")
        plan.steps.forEach { step ->
            appendLine("- ${step.stepId}: ${step.kind.name} ${step.label.take(MAX_LABEL_CHARS)}")
        }
        appendLine("Functions:")
        actionCapabilities.forEach { capability ->
            appendLine("- ${capability.functionId}: ${capability.displayName.take(MAX_LABEL_CHARS)}")
        }
    }.take(MAX_BRIEF_CHARS)

    private fun parseAssistResponse(text: String): ParsedAssist? {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.isEmpty()) return null

        var title: String? = null
        var summary: String? = null
        val stepLabels = linkedMapOf<String, String>()
        val citations = linkedMapOf<String, List<String>>()
        val functions = linkedMapOf<String, String>()

        lines.forEach { line ->
            when {
                line.startsWith("TITLE:", ignoreCase = true) ->
                    title = line.substringAfter(":").trim().take(MAX_LABEL_CHARS)
                line.startsWith("SUMMARY:", ignoreCase = true) ->
                    summary = line.substringAfter(":").trim().take(MAX_DETAIL_CHARS)
                line.uppercase(Locale.US).startsWith("STEP ") -> {
                    val afterPrefix = line.substringAfter(" ").trim()
                    val stepId = afterPrefix.substringBefore(":").trim()
                    val label = afterPrefix.substringAfter(":", missingDelimiterValue = "").trim()
                    if (stepId.isNotBlank() && label.isNotBlank()) {
                        stepLabels[stepId] = label.take(MAX_LABEL_CHARS)
                    }
                }
                line.uppercase(Locale.US).startsWith("CITE ") -> {
                    val afterPrefix = line.substringAfter(" ").trim()
                    val stepId = afterPrefix.substringBefore(":").trim()
                    val ids = afterPrefix
                        .substringAfter(":", missingDelimiterValue = "")
                        .split(",", " ")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    if (stepId.isNotBlank()) citations[stepId] = ids
                }
                line.uppercase(Locale.US).startsWith("FUNCTION ") -> {
                    val afterPrefix = line.substringAfter(" ").trim()
                    val stepId = afterPrefix.substringBefore(":").trim()
                    val functionId = afterPrefix.substringAfter(":", missingDelimiterValue = "").trim()
                    if (stepId.isNotBlank() && functionId.isNotBlank()) functions[stepId] = functionId
                }
            }
        }

        if (title.isNullOrBlank() && summary.isNullOrBlank() && stepLabels.isEmpty()) return null
        return ParsedAssist(
            title = title,
            summary = summary,
            stepLabels = stepLabels,
            citations = citations,
            functions = functions,
        )
    }

    private fun applyParsedAssist(
        base: AgentPlanDraft,
        parsed: ParsedAssist,
        modelLabel: String?,
    ): AgentPlanDraft? {
        val knownEvidenceIds = base.evidence.map { it.evidenceId }.toSet()
        val knownStepIds = base.steps.map { it.stepId }.toSet()
        if (parsed.stepLabels.keys.any { it !in knownStepIds }) return null
        if (parsed.citations.keys.any { it !in knownStepIds }) return null
        if (parsed.functions.keys.any { it !in knownStepIds }) return null
        if (parsed.citations.values.flatten().any { it !in knownEvidenceIds }) return null

        val updatedSteps = base.steps.map { step ->
            val parsedFunction = parsed.functions[step.stepId]
            if (parsedFunction != null && parsedFunction != step.functionId) return null

            val parsedCitations = parsed.citations[step.stepId]
            if (parsedCitations != null && parsedCitations.toSet() != step.evidenceIds.toSet()) return null

            step.copy(label = parsed.stepLabels[step.stepId] ?: step.label)
        }

        return base.copy(
            title = parsed.title ?: base.title,
            summary = parsed.summary ?: base.summary,
            steps = updatedSteps,
            modelLabel = modelLabel,
        )
    }

    private data class ParsedAssist(
        val title: String?,
        val summary: String?,
        val stepLabels: Map<String, String>,
        val citations: Map<String, List<String>>,
        val functions: Map<String, String>,
    )

    private fun LlmProvenance.toModelLabel(): String = when (this) {
        LlmProvenance.LocalNano -> "local:nano"
        is LlmProvenance.LocalByom -> "local:$model"
        is LlmProvenance.OrbitManaged -> model
        is LlmProvenance.Byok -> "$provider:$model"
    }

    private companion object {
        const val MAX_BRIEF_CHARS = 4_000
        const val MAX_QUERY_CHARS = 240
        const val MAX_LABEL_CHARS = 160
        const val MAX_DETAIL_CHARS = 240
    }
}

class AndroidAgentModelAssist(
    private val appContext: Context,
) : AgentModelAssist {

    override suspend fun assist(
        request: AgentRequest,
        deterministicPlan: AgentPlanDraft,
        actionCapabilities: List<AgentActionCapability>,
    ): AgentPlanDraft? {
        if (!PrivacyPreferences(appContext).cloudAiRoutingEnabled) return null

        val bound = bindGateway(appContext) ?: return null
        return try {
            val provider = LlmProviderRouter.create(appContext, bound.gateway)
            AgentModelAssistPlanner(provider).assist(
                request = request,
                deterministicPlan = deterministicPlan,
                actionCapabilities = actionCapabilities,
            )
        } finally {
            runCatching { appContext.unbindService(bound.connection) }
        }
    }

    private suspend fun bindGateway(context: Context): BoundGateway? =
        withContext(Dispatchers.Main) {
            val deferred = CompletableDeferred<BoundGateway?>()
            lateinit var connection: ServiceConnection
            connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    deferred.complete(
                        service?.let {
                            BoundGateway(
                                gateway = INetworkGateway.Stub.asInterface(it),
                                connection = connection,
                            )
                        }
                    )
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    if (!deferred.isCompleted) deferred.complete(null)
                }
            }

            val bound = context.bindService(
                Intent(context, NetworkGatewayService::class.java),
                connection,
                Context.BIND_AUTO_CREATE,
            )
            if (!bound) {
                deferred.complete(null)
            }
            deferred.await()
        }

    private data class BoundGateway(
        val gateway: INetworkGateway,
        val connection: ServiceConnection,
    )
}
