package com.nealaringale.nextbell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class TimetableDataTest {
    @Test
    public void batchBoundariesAreStable() {
        assertEquals("", TimetableData.batchForRoll(0));
        assertEquals("B1", TimetableData.batchForRoll(1));
        assertEquals("B1", TimetableData.batchForRoll(25));
        assertEquals("B2", TimetableData.batchForRoll(26));
        assertEquals("B2", TimetableData.batchForRoll(50));
        assertEquals("B3", TimetableData.batchForRoll(51));
        assertEquals("B3", TimetableData.batchForRoll(999));
    }

    @Test
    public void mondayPracticalAndTutorialRowsFollowRollRules() {
        assertTrue(containsId(TimetableData.forRollAndDay(1, "Monday"), "mon-b1"));
        assertFalse(containsId(TimetableData.forRollAndDay(1, "Monday"), "mon-b2"));
        assertTrue(containsId(TimetableData.forRollAndDay(26, "Monday"), "mon-b2"));
        assertTrue(containsId(TimetableData.forRollAndDay(51, "Monday"), "mon-b3"));
        assertFalse(containsId(TimetableData.forRollAndDay(35, "Monday"), "mon-lam-36"));
        assertTrue(containsId(TimetableData.forRollAndDay(36, "Monday"), "mon-lam-36"));
    }

    @Test
    public void timetableIsOrderedNonOverlappingAndReadOnly() {
        List<Integer> rolls = Arrays.asList(1, 25, 26, 35, 36, 50, 51, 75, 999);
        for (String day : TimetableData.DAYS) {
            for (int roll : rolls) {
                List<TimetableData.ClassItem> items = TimetableData.forRollAndDay(roll, day);
                String previousEnd = "";
                for (TimetableData.ClassItem item : items) {
                    assertTrue(item.start.compareTo(item.end) < 0);
                    assertTrue("Overlap on " + day + " for roll " + roll,
                            previousEnd.isEmpty() || previousEnd.compareTo(item.start) <= 0);
                    previousEnd = item.end;
                }
            }
        }

        List<TimetableData.ClassItem> monday = TimetableData.forRollAndDay(1, "Monday");
        try {
            monday.clear();
            throw new AssertionError("Timetable cache must not be mutable by callers.");
        } catch (UnsupportedOperationException expected) {
            // Cached entries are shared between screens and the reminder scheduler.
        }
    }

    private static boolean containsId(List<TimetableData.ClassItem> items, String id) {
        for (TimetableData.ClassItem item : items) {
            if (item.id.equals(id)) return true;
        }
        return false;
    }
}
