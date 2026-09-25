package com.foto6.dailyfocus.core

import org.junit.Assert.*
import org.junit.Test

class WidgetLayoutPolicyTest {
    @Test fun narrowModeSupportsTallOneColumnWidget() {
        val x = WidgetLayoutPolicy.forSize(100, 360)
        assertEquals(WidgetMode.NARROW, x.mode)
        assertFalse(x.showRefresh)
        assertFalse(x.showGoalPill)
        assertTrue(x.showTasksHeader)
        assertTrue(x.maxTasks >= 3)
    }

    @Test fun compactModeKeepsTaskTargetsPriority() {
        val x = WidgetLayoutPolicy.forSize(210, 230)
        assertEquals(WidgetMode.COMPACT, x.mode)
        assertTrue(x.showTasksHeader)
        assertTrue(x.maxTasks >= 1)
        assertFalse(x.showApps)
    }

    @Test fun fullModeUsesRichSectionsWhenThereIsRoom() {
        val x = WidgetLayoutPolicy.forSize(360, 420)
        assertEquals(WidgetMode.FULL, x.mode)
        assertTrue(x.showStatus)
        assertTrue(x.showApps)
        assertTrue(x.maxTasks >= 2)
    }

    @Test fun tinyHeightNeverInventsTaskRows() {
        val x = WidgetLayoutPolicy.forSize(100, 100)
        assertFalse(x.showTasksHeader)
        assertEquals(0, x.maxTasks)
    }
}
