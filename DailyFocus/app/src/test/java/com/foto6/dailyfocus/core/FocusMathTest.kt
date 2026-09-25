package com.foto6.dailyfocus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FocusMathTest {
    @Test fun zeroDuration() = assertEquals("0м", FocusMath.formatDuration(0))
    @Test fun subMinuteDuration() = assertEquals("<1м", FocusMath.formatDuration(59_999))
    @Test fun exactMinute() = assertEquals("1м", FocusMath.formatDuration(60_000))
    @Test fun mixedHoursAndMinutes() = assertEquals("2ч 17м", FocusMath.formatDuration((2 * 60 + 17) * 60_000L))
    @Test fun exactHour() = assertEquals("2ч", FocusMath.formatDuration(120 * 60_000L))
    @Test fun negativeDurationClampsToZero() = assertEquals("0м", FocusMath.formatDuration(-1))
    @Test fun progressCapsAtOneHundredPercent() = assertEquals(1000, FocusMath.progressPermille(200 * 60_000L, 120))
    @Test fun progressHalf() = assertEquals(500, FocusMath.progressPermille(60 * 60_000L, 120))
    @Test fun progressHandlesInvalidGoal() = assertEquals(0, FocusMath.progressPermille(60_000L, 0))
    @Test fun overGoalOnlyReturnsExcess() = assertEquals(17 * 60_000L, FocusMath.overGoalMs(137 * 60_000L, 120))
    @Test fun remainingNeverNegative() = assertEquals(0L, FocusMath.remainingMs(137 * 60_000L, 120))
    @Test fun negativeUsageDoesNotIncreaseRemaining() = assertEquals(120 * 60_000L, FocusMath.remainingMs(-60_000L, 120))

    @Test fun stressMathInvariants() {
        val random = Random(0xD411F0C)
        repeat(100_000) {
            val used = random.nextLong(-86_400_000L, 172_800_001L)
            val goal = random.nextInt(-120, 1_561)
            val progress = FocusMath.progressPermille(used, goal)
            val remaining = FocusMath.remainingMs(used, goal)
            val over = FocusMath.overGoalMs(used, goal)
            assertTrue(progress in 0..1000)
            assertTrue(remaining >= 0L)
            assertTrue(over >= 0L)
            if (goal > 0) {
                val safeUsed = used.coerceAtLeast(0L)
                val goalMs = goal * 60_000L
                assertEquals((goalMs - safeUsed).coerceAtLeast(0L), remaining)
                assertEquals((safeUsed - goalMs).coerceAtLeast(0L), over)
                assertTrue(remaining == 0L || over == 0L)
            }
        }
    }
}
