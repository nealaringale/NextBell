package com.nealaringale.nextbell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/** Regression coverage for the official NMIET FE Semester I division timetables. */
public class FirstYearTimetableDataTest {
    private static final String[] DIVISIONS = {
            "A", "B", "C", "D", "E", "F", "G", "H",
            "I", "J", "K", "L", "M", "N", "O", "P"
    };

    private static final int[] SAMPLE_ROLLS = {
            1, 25, 26, 35, 36, 50, 51, 74, 75, 120
    };

    private static boolean hasLimitedRollRange(String division) {
        return "J".equals(division) || "K".equals(division)
                || "L".equals(division) || "M".equals(division)
                || "P".equals(division);
    }

    @Test
    public void allSixteenDivisionsAreSupportedAndBatchBoundariesAreCorrect() {
        for (String division : DIVISIONS) {
            assertTrue("Unsupported division " + division,
                    TimetableData.isSupportedDivision(division));

            assertTrue(division + " roll 1 should have a batch",
                    !TimetableData.batchForDivisionRoll(division, 1).isEmpty());
            assertTrue(division + " roll 25 should have a batch",
                    !TimetableData.batchForDivisionRoll(division, 25).isEmpty());
            assertTrue(division + " roll 26 should have a batch",
                    !TimetableData.batchForDivisionRoll(division, 26).isEmpty());
            assertTrue(division + " roll 50 should have a batch",
                    !TimetableData.batchForDivisionRoll(division, 50).isEmpty());
            assertTrue(division + " roll 51 should have a batch",
                    !TimetableData.batchForDivisionRoll(division, 51).isEmpty());

            if (hasLimitedRollRange(division)) {
                assertTrue(division + " roll 74 should be valid",
                        !TimetableData.batchForDivisionRoll(division, 74).isEmpty());
                assertEquals(division + " roll 75 must be rejected", "",
                        TimetableData.batchForDivisionRoll(division, 75));
            } else {
                assertTrue(division + " roll 75 should remain in its last batch",
                        !TimetableData.batchForDivisionRoll(division, 75).isEmpty());
            }
        }
    }

    private static void assertEntry(String division, int roll, String day,
                                    String start, String end, String code, String audience) {
        boolean found = false;
        for (TimetableData.ClassItem item :
                TimetableData.forDivisionRollAndDay(division, roll, day)) {
            if (item.start.equals(start) && item.end.equals(end)
                    && item.code.equals(code) && item.audience.equals(audience)) {
                found = true;
                break;
            }
        }
        assertTrue("Missing official entry: division " + division
                + ", roll " + roll + ", " + day + " " + start + "-" + end
                + ", " + code + ", audience " + audience, found);
    }

    @Test
    public void representativeOfficialCellsAndMergedSlotDurationsMatchAllNewDivisions() {
        assertEntry("A", 1, "Monday", "10:40", "12:40", "FPL", "A-1");
        assertEntry("C", 51, "Tuesday", "13:30", "15:30", "FPL", "C-3");
        assertEntry("D", 51, "Thursday", "13:30", "15:30", "ELE", "D-3");
        assertEntry("E", 26, "Monday", "13:30", "15:30", "EG", "E-2");
        assertEntry("F", 51, "Wednesday", "13:30", "15:30", "PHY", "F-3");
        assertEntry("G", 26, "Monday", "13:30", "15:30", "LIBRARY", "G2");
        assertEntry("H", 26, "Wednesday", "13:30", "15:30", "WS", "H2");
        assertEntry("I", 51, "Wednesday", "13:30", "15:30", "FPL", "I-3");
        assertEntry("J", 26, "Thursday", "13:30", "15:30", "WS", "J2");
        assertEntry("K", 51, "Monday", "13:30", "15:30", "FPL", "K3");
        assertEntry("L", 51, "Wednesday", "13:30", "14:30", "WS", "L3");
        assertEntry("M", 51, "Thursday", "13:30", "14:30", "FPL", "M3");
        assertEntry("N", 1, "Wednesday", "13:30", "14:30", "FPL", "N-1");
        assertEntry("O", 26, "Monday", "13:30", "15:30", "FPL", "O-2");
        assertEntry("O", 26, "Tuesday", "13:30", "15:30", "WS", "O-2");
        assertEntry("P", 26, "Friday", "13:30", "15:30", "FPL", "P2");
    }

    @Test
    public void everyDivisionHasWeekdayEntriesForRollBoundaries() {
        for (String division : DIVISIONS) {
            for (int roll : SAMPLE_ROLLS) {
                if (hasLimitedRollRange(division) && roll > 74) continue;

                for (String day : TimetableData.DAYS) {
                    List<TimetableData.ClassItem> items =
                            TimetableData.forDivisionRollAndDay(division, roll, day);
                    assertNotNull(division + " " + day + " returned null", items);

                    boolean hasAcademicEntry = false;
                    for (TimetableData.ClassItem item : items) {
                        if (item.isAcademic()) hasAcademicEntry = true;
                    }
                    assertTrue(division + " roll " + roll + " has no academic entries on " + day,
                            hasAcademicEntry);
                }
            }
        }
    }

    @Test
    public void noStudentReceivesOverlappingEventsOrDuplicateEventIds() {
        for (String division : DIVISIONS) {
            for (int roll : SAMPLE_ROLLS) {
                for (String day : TimetableData.DAYS) {
                    List<TimetableData.ClassItem> items =
                            TimetableData.forDivisionRollAndDay(division, roll, day);
                    Set<String> ids = new HashSet<>();
                    String previousEnd = "";

                    for (TimetableData.ClassItem item : items) {
                        assertTrue(division + " " + day + " duplicate event " + item.id,
                                ids.add(item.id));
                        assertTrue(division + " " + day + " invalid time range for " + item.id,
                                item.start.compareTo(item.end) < 0);
                        assertTrue(division + " " + day + " unsorted timetable for " + item.id,
                                previousEnd.isEmpty() || previousEnd.compareTo(item.start) <= 0);
                        previousEnd = item.end;
                    }
                }
            }
        }
    }

    @Test
    public void invalidRollsDoNotReceiveBatchOrTutorialAllocations() {
        for (String division : DIVISIONS) {
            if (!hasLimitedRollRange(division)) continue;

            for (String day : TimetableData.DAYS) {
                List<TimetableData.ClassItem> items =
                        TimetableData.forDivisionRollAndDay(division, 75, day);
                for (TimetableData.ClassItem item : items) {
                    assertEquals(division + " roll 75 must only see common entries on " + day,
                            "COMMON", item.audience);
                }
            }
        }
    }

    @Test
    public void divisionBStillUsesTheOriginalBundledTimetable() {
        for (int roll : new int[]{1, 25, 26, 35, 36, 50, 51, 75, 120}) {
            for (String day : TimetableData.DAYS) {
                List<TimetableData.ClassItem> original = TimetableData.forRollAndDay(roll, day);
                List<TimetableData.ClassItem> routed =
                        TimetableData.forDivisionRollAndDay("B", roll, day);
                assertEquals(original.size(), routed.size());
                for (int i = 0; i < original.size(); i++) {
                    assertEquals(original.get(i).id, routed.get(i).id);
                    assertEquals(original.get(i).start, routed.get(i).start);
                    assertEquals(original.get(i).end, routed.get(i).end);
                }
            }
        }
    }

    @Test
    public void newDivisionResultsAreImmutable() {
        List<TimetableData.ClassItem> items =
                TimetableData.forDivisionRollAndDay("A", 1, "Monday");
        assertFalse(items.isEmpty());
        try {
            items.clear();
            throw new AssertionError("Division timetable should be immutable");
        } catch (UnsupportedOperationException expected) {
            // Shared immutable data prevents one screen mutating another screen's schedule.
        }
    }
}
