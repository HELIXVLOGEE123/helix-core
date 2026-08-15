package com.helix.ui.neuralstrand

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Static Compose previews for each [HelixState], against
 * [PlaceholderHelixState] — see that file before treating this as a
 * demonstration of live HELIX Core behavior. Useful for visual QA of the
 * design tokens and animation states in isolation, ahead of any host app.
 *
 * Background is Void (#0B0F14) per the HELIX Design System color tokens,
 * reproduced locally rather than imported, since this module intentionally
 * has no dependency on a shared theme module yet.
 */
private val PreviewVoid = Color(0xFF0B0F14)

@Preview(name = "Resting", showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun NeuralStrandRestingPreview() {
    NeuralStrand(
        state = HelixState.Resting,
        isTerminal = true,
        modifier = Modifier
            .height(200.dp)
            .padding(24.dp)
            .background(PreviewVoid)
    )
}

@Preview(name = "Thinking", showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun NeuralStrandThinkingPreview() {
    NeuralStrand(
        state = HelixState.ThoughtState,
        isTerminal = true,
        modifier = Modifier
            .height(200.dp)
            .padding(24.dp)
            .background(PreviewVoid)
    )
}

@Preview(name = "Error", showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun NeuralStrandErrorPreview() {
    NeuralStrand(
        state = HelixState.Error,
        isTerminal = true,
        modifier = Modifier
            .height(200.dp)
            .padding(24.dp)
            .background(PreviewVoid)
    )
}

@Preview(name = "Empty", showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun NeuralStrandEmptyPreview() {
    NeuralStrand(
        state = HelixState.Empty,
        isTerminal = true,
        modifier = Modifier
            .height(200.dp)
            .padding(24.dp)
            .background(PreviewVoid)
    )
}
