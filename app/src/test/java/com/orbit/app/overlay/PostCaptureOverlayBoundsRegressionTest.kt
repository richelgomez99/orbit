package com.orbit.app.overlay

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Spec 017 Phase 0A regression coverage for the physical S24/Tab S9 issue where
 * compact post-capture UI blocked taps on neighboring launcher icons.
 */
class PostCaptureOverlayBoundsRegressionTest {

    private fun resolve(rel: String): File {
        val direct = File(rel)
        if (direct.exists()) return direct
        val fromRepoRoot = File("app/$rel")
        check(fromRepoRoot.exists()) {
            "missing source file $rel (cwd=${File(".").absolutePath})"
        }
        return fromRepoRoot
    }

    @Test
    fun postCaptureRoot_onlyChipRowsFillWidth() {
        val src = resolve("src/main/java/com/orbit/app/overlay/PostCaptureOverlay.kt").readText()

        assertTrue(
            "Chip rows and focused context entry must keep the full-width target row.",
            src.contains("state is PostCaptureUi.ChipRow") &&
                src.contains("state is PostCaptureUi.ReclassifyChipRow") &&
                src.contains("state is PostCaptureUi.ContextEntry") &&
                src.contains("modifier.fillMaxWidth()")
        )
        assertTrue(
            "Silent/undo/confirmation states must wrap content so the Compose root does not block adjacent apps.",
            src.contains("modifier.wrapContentSize()")
        )
    }

    @Test
    fun postCaptureWindow_usesCompactBoundsAndOutsideTapPassthrough() {
        val src = resolve("src/main/java/com/orbit/app/service/OrbitOverlayService.kt").readText()

        assertTrue(
            "Post-capture window must opt into NOT_TOUCH_MODAL so outside compact-pill taps pass through.",
            src.contains("WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL")
        )
        assertTrue(
            "Chip rows and focused context entry must use MATCH_PARENT while compact post-capture states use WRAP_CONTENT.",
            src.contains("is PostCaptureUi.ChipRow -> WindowManager.LayoutParams.MATCH_PARENT") &&
                src.contains("is PostCaptureUi.ReclassifyChipRow -> WindowManager.LayoutParams.MATCH_PARENT") &&
                src.contains("is PostCaptureUi.ContextEntry -> WindowManager.LayoutParams.MATCH_PARENT") &&
                src.contains("else -> WindowManager.LayoutParams.WRAP_CONTENT")
        )
        assertTrue(
            "Focused context entry must be focusable for keyboard input while compact states stay NOT_FOCUSABLE.",
            src.contains("ui is PostCaptureUi.ContextEntry") &&
                src.contains("base or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE")
        )
    }

    @Test
    fun overlayDrag_usesLiveWindowMetrics() {
        val src = resolve("src/main/java/com/orbit/app/service/OrbitOverlayService.kt").readText()

        assertTrue(
            "Drag math must recompute current screen bounds during movement for rotation/landscape changes.",
            src.contains("val bounds = currentScreenBounds()") &&
                src.contains("bounds.width") &&
                src.contains("bounds.height")
        )
        assertTrue(
            "Drag-end snap must use live screen width, not setup-time portrait width.",
            src.contains("onBubbleDragEnd(currentScreenBounds().width, bubbleWidthPx)")
        )
    }
}
