# helix-ui

Separate repository from **HELIX Core** (`helix-platform`), by explicit architect decision — see the reconnaissance report (`ARCHITECTURAL_RECONNAISSANCE.md`, produced before this repo existed) and the three build decisions that authorized this pass:

1. **This UI lives in a separate repository**, not as a sibling module inside Core's Gradle project. It will depend on a *published* `helix-platform` artifact once one exists; Core must never depend on this repository.
2. **Jetpack Compose, Android-only** — not Compose Multiplatform.
3. **`NeuralStrand` is wired to `PlaceholderHelixState` (this module, local), not to real Core events.** No subsystem in HELIX Core currently owns "conversation" or "cognitive state," so there was nothing real to subscribe to. This boundary is deliberate and clearly marked in code — see `PlaceholderHelixState.kt`'s doc comment before assuming any state shown here reflects live system behavior.

## What's here

```
helix-ui/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml
└── neuralstrand/                          — Android library module, zero Core dependency today
    ├── build.gradle.kts
    └── src/
        ├── main/kotlin/com/helix/ui/neuralstrand/
        │   ├── HelixDesignTokens.kt        — colors/dimensions, copied from the locked Design System spec
        │   ├── PlaceholderHelixState.kt     — the state enum NeuralStrand renders; explicitly not Core-sourced
        │   ├── NeuralStrand.kt              — the composable itself, per Spec v1.1 (panel-corrected)
        │   └── NeuralStrandPreviews.kt      — static previews per state, for visual QA without a host app
        ├── androidTest/.../NeuralStrandSemanticsTest.kt
        └── test/                            (reserved; no unit tests yet — everything here is Compose-runtime-dependent)
```

## A real bug caught during implementation

The "locked" v1.1 specification's Breathing-state animation referenced `SineEasing` as a Compose easing curve. **That identifier does not exist anywhere in `androidx.compose.animation.core`.** It would not have compiled. It's been replaced here with an equivalent real `CubicBezierEasing` curve. This means the spec, despite being marked final after a full panel review, was never actually compiled before that review concluded — worth keeping in mind for how much trust to place in "locked" status on future specs that haven't touched a compiler yet.

## What is NOT here yet

- No dependency on HELIX Core. See the comment block in `neuralstrand/build.gradle.kts` for exactly where that dependency goes once it's real.
- No demo/host application module — previews (`NeuralStrandPreviews.kt`) are the current visual QA path.
- No `Breathing`/`ListeningState`/`Speaking` animation-gating tests, no reduce-motion test, no seam-continuity screenshot test. Only the TalkBack natural-language semantics (a Critical panel-review finding) has test coverage in this pass — everything else in the Testing Strategy section of the spec is still open.
