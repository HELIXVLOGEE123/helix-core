package com.helix.ui.neuralstrand

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test

/**
 * Instrumentation test covering the one behavior most likely to silently
 * regress: TalkBack semantics must be natural-language prose, not raw enum
 * names (this was a Critical panel-review finding against v1.0 — "HELIX
 * state: ThoughtState" is not acceptable; "HELIX is thinking" is required).
 * Kept intentionally small for this pass; full coverage (animation state
 * gating, reduce-motion behavior, seam continuity) is tracked separately
 * rather than implied to be complete here.
 */
class NeuralStrandSemanticsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun thinkingState_exposesNaturalLanguageDescription_notRawEnumName() {
        composeTestRule.setContent {
            NeuralStrand(state = HelixState.ThoughtState, isTerminal = true)
        }

        composeTestRule.onNodeWithContentDescription("HELIX is thinking").assertExists()
    }

    @Test
    fun errorState_exposesNaturalLanguageDescription() {
        composeTestRule.setContent {
            NeuralStrand(state = HelixState.Error, isTerminal = true)
        }

        composeTestRule.onNodeWithContentDescription("HELIX encountered an issue").assertExists()
    }
}
