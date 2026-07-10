package com.wickedcoder.myadvisor.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * MyAdvisor motion tokens.
 *
 * Durations and easings drawn from the M3 expressive motion spec. Motion here is
 * meaningful, never decorative: results reveal with [emphasizedDecelerate] (content
 * arriving), affordances settle with a gentle spring, and the shimmer skeleton uses
 * [standard]. Centralizing the specs keeps every animation in the app on the same
 * rhythm so transitions feel like one system.
 */
object MotionTokens {
    // Durations (ms)
    const val DurationShort = 150
    const val DurationMedium = 300
    const val DurationLong = 450
    const val DurationExtraLong = 700

    // Easings
    val emphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val emphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val standard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /** Content arriving on screen (staggered result reveal). */
    fun <T> enter(): FiniteAnimationSpec<T> =
        tween(durationMillis = DurationLong, easing = emphasizedDecelerate)

    /** Content leaving. */
    fun <T> exit(): FiniteAnimationSpec<T> =
        tween(durationMillis = DurationMedium, easing = emphasizedAccelerate)

    /** Tactile settle for taps, add/remove morphs. */
    fun <T> spatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
}
