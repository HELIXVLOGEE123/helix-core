package com.helix.ui.neuralstrand

/**
 * ============================================================================
 * PLACEHOLDER — NOT WIRED TO HELIX CORE.
 * ============================================================================
 *
 * This is a deliberate, explicitly-approved boundary, not an oversight.
 *
 * Per ARCHITECTURAL_RECONNAISSANCE.md (Section 8: Event Ownership) and the
 * build decision that authorized this module: no subsystem in HELIX Core
 * currently owns "conversation" or "cognitive state" as a concept, so there
 * is nothing real for NeuralStrand to subscribe to yet. Rather than invent a
 * fake Core dependency or a guessed event shape, NeuralStrand is built
 * against this local, standalone state type.
 *
 * When HELIX Core defines a real cognitive-state event source and that
 * dependency is added to this module's build.gradle.kts, the call sites
 * that currently construct [HelixState] locally (previews, demo/test code)
 * are exactly what needs to change — [NeuralStrand] itself, which only
 * consumes [HelixState] as a parameter, should not need to change at all.
 *
 * Do not delete this file's warning comment when the real wiring lands;
 * move it to whatever replaces this file so the history of the decision
 * isn't lost.
 */
public enum class HelixState {
    Empty,
    Resting,
    Breathing,
    ListeningState,
    ThoughtState,
    Speaking,
    Error
}
