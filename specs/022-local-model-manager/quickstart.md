# Quickstart: Local Model Manager

## Local Validation

```bash
./gradlew :app:testDebugUnitTest --tests "com.orbit.app.ai.local.*" --tests "com.orbit.app.ai.LlmProviderRouterTest"
./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :build-logic:lint:test :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin
git diff --check
```

## Demo Flow

1. Evaluate a 4GB device profile with a downloaded Speed model.
2. Verify local selection exposes extraction/basic capabilities but not generative UI.
3. Evaluate a Vulkan-capable 6GB+ profile with downloaded Intelligence model.
4. Verify Intelligence selection exposes Ask/generative UI capability.
5. Disable cloud routing with no local model and verify the route is unavailable.

## Validation Log

2026-06-14:

- Created Spec 022 artifacts. Scope is policy/router seam first; native LiteRT-LM/MLC binaries, downloads, and JNI are deferred.
