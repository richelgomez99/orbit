package com.orbit.app.understanding

import com.orbit.app.understanding.domain.UnderstandingResult
import com.orbit.app.understanding.engine.ActiveIntentResolver
import com.orbit.app.understanding.engine.BasicCaptureInput
import com.orbit.app.understanding.engine.BasicUnderstandingEngine

class ActiveIntentSaveCoordinator(
    private val basicUnderstandingEngine: BasicUnderstandingEngine,
    private val understandingRepository: UnderstandingRepository,
    private val activeIntentRepository: ActiveIntentRepository,
    private val activeIntentResolver: ActiveIntentResolver = ActiveIntentResolver()
) {
    suspend fun understandAndSave(input: BasicCaptureInput): UnderstandingResult {
        val understanding = basicUnderstandingEngine.understand(input)
        understandingRepository.saveUnderstanding(understanding)
        activeIntentResolver.resolve(input, understanding)?.let { activeIntent ->
            activeIntentRepository.upsert(activeIntent)
        }
        return understanding
    }
}
