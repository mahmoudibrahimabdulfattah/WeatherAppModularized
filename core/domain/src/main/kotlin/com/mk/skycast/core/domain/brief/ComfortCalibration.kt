package com.mk.skycast.core.domain.brief

import com.mk.skycast.core.model.ComfortFeedback
import com.mk.skycast.core.model.ComfortVote

/**
 * Turns "felt cold / just right / felt hot" votes into a feels-like offset for
 * clothing advice. Recent votes count more; the offset is bounded so one odd
 * day can't flip the advice.
 */
object ComfortCalibration {
    const val MAX_VOTES = 10
    private const val STEP_C = 3.0
    private const val MAX_OFFSET_C = 6.0
    private const val DECAY = 0.85

    /** [feedback] most recent first. Negative when the user runs cold (dress warmer). */
    fun offsetC(feedback: List<ComfortFeedback>): Double {
        val recent = feedback.take(MAX_VOTES)
        if (recent.isEmpty()) return 0.0
        var weighted = 0.0
        var weights = 0.0
        recent.forEachIndexed { index, item ->
            val weight = Math.pow(DECAY, index.toDouble())
            weighted += weight * item.vote.sign()
            weights += weight
        }
        return (weighted / weights * STEP_C * minOf(recent.size, 3) / 3.0).coerceIn(-MAX_OFFSET_C, MAX_OFFSET_C)
    }

    private fun ComfortVote.sign(): Int = when (this) {
        ComfortVote.COLD -> -1
        ComfortVote.RIGHT -> 0
        ComfortVote.HOT -> 1
    }
}
