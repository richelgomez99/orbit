package com.orbit.app.overlay

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureContextViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun newCaptureContextSaveRejectsBlankBeforeBinderCall() = runTest(dispatcher) {
        val vm = OverlayViewModel()
        var saveCalls = 0
        vm.onSaveContextToEnvelope = { _, _ ->
            saveCalls += 1
            true
        }

        vm.onNewCaptureAddContext("env-1")
        vm.onCaptureContextTextChanged("   ")
        vm.onCaptureContextSave()
        advanceUntilIdle()

        val state = vm.postCaptureUi.value as PostCaptureUi.ContextEntry
        assertEquals("env-1", state.targetEnvelopeId)
        assertEquals(ContextOrigin.NEW_CAPTURE, state.origin)
        assertEquals("Add a short reason, or cancel.", state.errorMessage)
        assertEquals(0, saveCalls)
    }

    @Test
    fun newCaptureContextSaveWritesTrimmedTextAndShowsConfirmation() = runTest(dispatcher) {
        val vm = OverlayViewModel()
        val calls = mutableListOf<Pair<String, String>>()
        vm.onSaveContextToEnvelope = { envelopeId, text ->
            calls += envelopeId to text
            true
        }

        vm.onNewCaptureAddContext("env-1")
        vm.onCaptureContextTextChanged("  reschedule dentist  ")
        vm.onCaptureContextSave()
        advanceUntilIdle()

        assertEquals(listOf("env-1" to "reschedule dentist"), calls)
        assertTrue(vm.postCaptureUi.value is PostCaptureUi.ContextSavedConfirmation)
    }

    @Test
    fun duplicateContextTargetsExistingEnvelopeAndKeepsDraftOnFailure() = runTest(dispatcher) {
        val vm = OverlayViewModel()
        val calls = mutableListOf<Pair<String, String>>()
        vm.onSaveContextToEnvelope = { envelopeId, text ->
            calls += envelopeId to text
            false
        }
        vm.onAlreadySavedAddNote("existing-1")

        vm.onCaptureContextTextChanged("today it is cancelled")
        vm.onCaptureContextSave()
        advanceUntilIdle()

        val state = vm.postCaptureUi.value as PostCaptureUi.ContextEntry
        assertEquals(listOf("existing-1" to "today it is cancelled"), calls)
        assertEquals("existing-1", state.targetEnvelopeId)
        assertEquals(ContextOrigin.DUPLICATE_CAPTURE, state.origin)
        assertFalse(state.isSaving)
        assertEquals(
            "Orbit saved this capture, but could not attach context yet. Try again.",
            state.errorMessage
        )
    }

    @Test
    fun failureCopyDoesNotEchoRawContextText() = runTest(dispatcher) {
        val vm = OverlayViewModel()
        vm.onSaveContextToEnvelope = { _, _ -> false }

        vm.onNewCaptureAddContext("env-1")
        vm.onCaptureContextTextChanged("passport 123456789")
        vm.onCaptureContextSave()
        advanceUntilIdle()

        val state = vm.postCaptureUi.value as PostCaptureUi.ContextEntry
        assertFalse(requireNotNull(state.errorMessage).contains("123456789"))
        assertFalse(state.errorMessage.contains("passport"))
    }

    @Test
    fun cancelReturnsToPriorPill() {
        val vm = OverlayViewModel()
        vm.onNewCaptureAddContext("env-1")

        vm.onCaptureContextCancel()
        assertTrue(vm.postCaptureUi.value is PostCaptureUi.None)
    }

    @Test
    fun openDetailFallbackClearsFocusedContextAndInvokesCallback() {
        val vm = OverlayViewModel()
        val opened = mutableListOf<String>()
        vm.onAddNoteToExistingEnvelope = { opened += it }

        vm.onNewCaptureAddContext("env-1")
        vm.onCaptureContextOpenDetail()

        assertEquals(listOf("env-1"), opened)
        assertTrue(vm.postCaptureUi.value is PostCaptureUi.None)
    }
}
