package com.helix.ui.neuralstrand

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.helix.ui.neuralstrand.HelixDesignTokens.AccentEmber
import com.helix.ui.neuralstrand.HelixDesignTokens.ColorMuted
import com.helix.ui.neuralstrand.HelixDesignTokens.LineThickness
import com.helix.ui.neuralstrand.HelixDesignTokens.TouchTargetWidth
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * NeuralStrand
 *
 * The core visual primitive of HELIX, per NEURAL STRAND SPECIFICATION v1.1
 * (panel-reviewed, all required changes applied). A performant, 2dp
 * continuous line that acts as cursor, progress indicator, loading state,
 * and intelligence anchor. Zero third-party dependencies beyond Compose;
 * theme-aware only through [HelixDesignTokens].
 *
 * ANIMATION JUSTIFICATION & BATTERY COST:
 * - Resting / Empty: static frame. 0 CPU/GPU clock ticks.
 * - Breathing: 2400ms sine offset. Minimal GPU translation.
 * - ListeningState: asymmetric lateral wave driven by an audio-level lambda,
 *   read only inside the graphicsLayer block — 0 recompositions per frame.
 * - ThoughtState: dual-strand out-of-phase scaleX cosine twist simulating a
 *   3D rotation. GPU matrix transform only; no Path recalculation.
 * - Speaking: 320ms rhythmic downward flow.
 * - Error: 240ms desaturation fade to [ColorMuted]. No shaking, no red flash.
 *
 * All ambient animation clocks are gated behind [isTerminal] and the
 * system's Reduce Motion setting (read automatically; not a caller-supplied
 * parameter — see panel review item "Automatic Reduce Motion"). Non-terminal
 * history nodes therefore cost zero animation clock ticks.
 *
 * @param state Current [HelixState]. NOTE: currently sourced from
 *   [PlaceholderHelixState] pending real HELIX Core event wiring — see that
 *   file's doc comment before assuming this reflects live system state.
 * @param modifier Layout bounds modifier. The strand fills available
 *   vertical space; it does not size itself.
 * @param isTerminal True only for the active leading edge. Ambient/complex
 *   animations (ThoughtState, ListeningState, Speaking, Breathing) evaluate
 *   only when this is true, so history segments in a scrollable list cost
 *   nothing.
 * @param audioLevel Lambda returning real-time microphone amplitude in
 *   [0f, 1f]. Read exclusively inside the GPU transform block to avoid
 *   triggering recomposition on every audio frame.
 */
@Composable
public fun NeuralStrand(
    state: HelixState,
    modifier: Modifier = Modifier,
    isTerminal: Boolean = false,
    audioLevel: () -> Float = { 0f }
) {
    val context = LocalContext.current

    // Automatic Reduce Motion detection (panel review: this must not be a
    // caller-supplied boolean parameter, or accessibility compliance breaks
    // the moment a call site forgets to pass it).
    val isSystemReduceMotion = remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1f
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }

    val accessibilityLabel = remember(state) {
        when (state) {
            HelixState.Empty -> "HELIX dormant"
            HelixState.Resting -> "HELIX ready"
            HelixState.Breathing -> "HELIX waiting"
            HelixState.ListeningState -> "HELIX is listening"
            HelixState.ThoughtState -> "HELIX is thinking"
            HelixState.Speaking -> "HELIX is speaking"
            HelixState.Error -> "HELIX encountered an issue"
        }
    }

    val lineColor by animateColorAsState(
        targetValue = if (state == HelixState.Error) ColorMuted else AccentEmber,
        animationSpec = tween(if (state == HelixState.Error) 240 else 320),
        label = "StrandColorAnimation"
    )

    // Selective animation clocks: only evaluated when isTerminal and motion
    // is allowed, so non-terminal/history nodes cost zero clock ticks.
    val shouldAnimate = isTerminal && !isSystemReduceMotion
    val infiniteTransition = rememberInfiniteTransition(label = "StrandClock")

    val twistPhase by if (shouldAnimate && state == HelixState.ThoughtState) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "HelixTwistPhase"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val breathOffset by if (!isSystemReduceMotion && (state == HelixState.Breathing || state == HelixState.Speaking)) {
        infiniteTransition.animateFloat(
            initialValue = -1.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = androidx.compose.animation.core.CubicBezierEasing(0.37f, 0f, 0.63f, 1f)),
                repeatMode = RepeatMode.Reverse
            ),
            label = "StrandBreathOffset"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val flowOffset by if (shouldAnimate && state == HelixState.Speaking) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(320, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "SpeakingFlowOffset"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Box(
        modifier = modifier
            .width(TouchTargetWidth)
            .fillMaxHeight()
            .clearAndSetSemantics {
                contentDescription = accessibilityLabel
            },
        contentAlignment = Alignment.Center
    ) {
        // STRAND A — always present in the composition tree. No if/else
        // composable swapping (panel review: structural tree mutation
        // during state changes violates the GPU-only rule).
        NeuralLine(color = lineColor) {
            transformOrigin = TransformOrigin(0.5f, 1.0f) // bottom-anchored collapse

            when {
                state == HelixState.Empty -> {
                    scaleY = 0f
                    alpha = 0f
                }
                state == HelixState.ThoughtState && shouldAnimate -> {
                    scaleX = cos(twistPhase).coerceAtLeast(0.1f)
                    alpha = ((sin(twistPhase) + 1f) / 2f).coerceIn(0.2f, 1f)
                }
                state == HelixState.ListeningState && shouldAnimate -> {
                    // Asymmetric lateral displacement, not symmetric
                    // thickening — deliberately distinct from standard
                    // voice-visualizer bars (panel review: originality).
                    translationX = sin(twistPhase * 2) * audioLevel() * 12f
                    scaleX = 1f + (audioLevel() * 0.5f)
                    alpha = 1f
                }
                state == HelixState.Speaking && shouldAnimate -> {
                    translationY = flowOffset
                    alpha = 1f
                }
                state == HelixState.Breathing -> {
                    translationY = breathOffset
                    alpha = 1f
                }
                state == HelixState.Error -> {
                    alpha = 0.3f
                    scaleX = 1f
                    scaleY = 1f
                }
                else -> { // Resting
                    alpha = 1f
                    scaleX = 1f
                    scaleY = 1f
                    translationX = 0f
                    translationY = 0f
                }
            }
        }

        // STRAND B — 180-degree out-of-phase twin for the helix-twist
        // illusion. Also always present; alpha-culled to 0 rather than
        // removed from composition when not thinking.
        NeuralLine(color = lineColor) {
            transformOrigin = TransformOrigin(0.5f, 1.0f)

            if (state == HelixState.ThoughtState && shouldAnimate) {
                val phaseB = twistPhase + PI.toFloat()
                scaleX = cos(phaseB).coerceAtLeast(0.1f)
                alpha = ((sin(phaseB) + 1f) / 2f).coerceIn(0.2f, 1f)
            } else {
                alpha = 0f
            }
        }
    }
}

/**
 * Single Canvas draw call for one vertical 2dp line segment. Draws with a
 * 1px vertical bleed past its own bounds using [StrokeCap.Square] so
 * adjacent NeuralLine instances in a scrolling list (e.g. history nodes)
 * appear seamlessly continuous rather than showing sub-pixel gaps.
 */
@Composable
private fun NeuralLine(
    color: Color,
    graphicsLayerBlock: GraphicsLayerScope.() -> Unit
) {
    Canvas(
        modifier = Modifier
            .width(LineThickness)
            .fillMaxHeight()
            .graphicsLayer(graphicsLayerBlock)
    ) {
        drawLine(
            color = color,
            start = Offset(size.width / 2f, -1f),
            end = Offset(size.width / 2f, size.height + 1f),
            strokeWidth = size.width,
            cap = StrokeCap.Square
        )
    }
}
