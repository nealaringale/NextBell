package com.nealaringale.nextbell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/** Regression tests for college-specific profiles and the imported DYPIT date-scoped timetable. */
public class CollegeTimetableDataTest {
    private static final String DYP = CollegeCatalog.DYPIIT_PIMPRI;

    @Test
    public void collegeCatalogAndScheduleDaysAreCollegeSpecific() {
        assertEquals(CollegeCatalog.NMIET,
                CollegeCatalog.normalize(CollegeCatalog.NMIET));
        assertEquals(CollegeCatalog.DYPIIT_PIMPRI,
                CollegeCatalog.normalize(CollegeCatalog.DYPIIT_PIMPRI));

        assertEquals(5, CollegeScheduleData.days(CollegeCatalog.NMIET).length);
        assertEquals(7, CollegeScheduleData.days(DYP).length);
        assertEquals(16, CollegeScheduleData.divisions(CollegeCatalog.NMIET).length);
        assertEquals(20, CollegeScheduleData.divisions(DYP).length);
        assertTrue(CollegeScheduleData.supportsDivision(DYP, "T"));
        assertFalse(CollegeScheduleData.supportsDivision(CollegeCatalog.NMIET, "T"));
    }

    @Test
    public void importedPdfContainsExactlyFiveDatesAndTwentyDivisionsPerDate() {
        LocalDate[] dates = DypitTimetableData.dates();
        assertEquals(5, dates.length);
        assertEquals(LocalDate.of(2026, 12, 1), dates[0]);
        assertEquals(LocalDate.of(2026, 12, 3), dates[1]);
        assertEquals(LocalDate.of(2026, 12, 5), dates[2]);
        assertEquals(LocalDate.of(2026, 12, 6), dates[3]);
        assertEquals(LocalDate.of(2026, 12, 8), dates[4]);

        for (LocalDate date : dates) {
            assertEquals(20, DypitTimetableData.divisionCount(date));
            for (String division : DypitTimetableData.divisions()) {
                List<TimetableData.ClassItem> items =
                        DypitTimetableData.forDate(division, date);
                assertEquals(date.getDayOfMonth() == 6 ? 7 : 6, items.size());
                int academic = 0;
                int shortBreaks = 0;
                int lunches = 0;
                Set<String> ids = new HashSet<>();
                String previousEnd = "";
                for (TimetableData.ClassItem item : items) {
                    assertTrue("Duplicate event id " + item.id, ids.add(item.id));
                    assertTrue("Invalid time range " + item.id, item.start.compareTo(item.end) < 0);
                    assertTrue("Events overlap or are unsorted for " + division + " " + date,
                            previousEnd.isEmpty() || previousEnd.compareTo(item.start) <= 0);
                    previousEnd = item.end;
                    if (item.kind == TimetableData.Kind.SHORT_BREAK) shortBreaks++;
                    else if (item.kind == TimetableData.Kind.LUNCH) lunches++;
                    else if (item.isAcademic()) academic++;
                }
                assertEquals(5, academic);
                assertEquals(1, shortBreaks);
                assertEquals(date.getDayOfMonth() == 6 ? 1 : 0, lunches);
            }
        }
    }

    @Test
    public void weekdayRowsPreserveRoomSubjectFacultyCodeAndPublishedTimes() {
        LocalDate date = LocalDate.of(2026, 12, 1);
        List<TimetableData.ClassItem> items = CollegeScheduleData.forDate(DYP, "D", 34, date);
        assertEquals(6, items.size());

        TimetableData.ClassItem first = items.get(0);
        assertEquals("09:00", first.start);
        assertEquals("09:45", first.end);
        assertEquals("MATH", first.subject);
        assertEquals("MATH", first.code);
        assertEquals("HP", first.teacher);
        assertEquals("B 212", first.room);

        TimetableData.ClassItem third = items.get(3);
        assertEquals("11:00", third.start);
        assertEquals("11:45", third.end);
        assertEquals("PHY", third.subject);
        assertEquals("GK", third.teacher);

        TimetableData.ClassItem last = items.get(5);
        assertEquals("12:30", last.start);
        assertEquals("13:15", last.end);
        assertEquals("EM", last.subject);
        assertEquals("SMB", last.teacher);
    }

    @Test
    public void sundayRowsIncludeSundayTimingShortBreakAndLunch() {
        List<TimetableData.ClassItem> items = CollegeScheduleData.forDate(
                DYP, "A", 7, LocalDate.of(2026, 12, 6));
        assertEquals(7, items.size());

        assertEquals("09:00", items.get(0).start);
        assertEquals("10:00", items.get(0).end);
        assertEquals("11:00", items.get(2).start);
        assertEquals("11:30", items.get(2).end);
        assertEquals(TimetableData.Kind.SHORT_BREAK, items.get(2).kind);
        assertEquals("13:30", items.get(5).start);
        assertEquals("14:30", items.get(5).end);
        assertEquals(TimetableData.Kind.LUNCH, items.get(5).kind);
        assertEquals("14:30", items.get(6).start);
        assertEquals("15:30", items.get(6).end);
    }

    @Test
    public void noRecurringScheduleIsInventedForDatesNotPresentInThePdf() {
        assertTrue(CollegeScheduleData.forDate(DYP, "A", 1,
                LocalDate.of(2026, 12, 2)).isEmpty());
        assertTrue(CollegeScheduleData.forDate(DYP, "A", 1,
                LocalDate.of(2026, 12, 7)).isEmpty());
        assertNull(CollegeScheduleData.dateForDay(
                DYP, "Wednesday", LocalDate.of(2026, 11, 27)));
        assertEquals(LocalDate.of(2026, 12, 1),
                CollegeScheduleData.nextPublishedDate(DYP, LocalDate.of(2026, 11, 27)));
        assertEquals(LocalDate.of(2026, 12, 3),
                CollegeScheduleData.nextPublishedDate(DYP, LocalDate.of(2026, 12, 2)));
        assertNull(CollegeScheduleData.nextPublishedDate(
                DYP, LocalDate.of(2026, 12, 9)));
        assertNotNull(CollegeScheduleData.dateForDay(
                DYP, "Tuesday", LocalDate.of(2026, 11, 27)));
    }

    @Test
    public void dypDoesNotInventPracticalBatchesAndRollsAreValidatedSeparately() {
        assertEquals("", CollegeScheduleData.batchForRoll(DYP, "A", 34));
        assertEquals("", CollegeScheduleData.rollRangeForBatch(DYP, "A", ""));
        assertTrue(CollegeScheduleData.isValidRoll(DYP, "T", 34));
        assertFalse(CollegeScheduleData.isValidRoll(DYP, "T", 0));
        assertFalse(CollegeScheduleData.isValidRoll(DYP, "U", 34));
    }

    @Test
    public void nmietStillRoutesToItsExistingWeeklyTimetable() {
        List<TimetableData.ClassItem> legacy =
                TimetableData.forDivisionRollAndDay("B", 34, "Monday");
        List<TimetableData.ClassItem> routed =
                CollegeScheduleData.forDate(CollegeCatalog.NMIET, "B", 34,
                        LocalDate.of(2026, 11, 30));
        assertEquals(legacy.size(), routed.size());
        for (int i = 0; i < legacy.size(); i++) {
            assertEquals(legacy.get(i).id, routed.get(i).id);
            assertEquals(legacy.get(i).start, routed.get(i).start);
            assertEquals(legacy.get(i).subject, routed.get(i).subject);
        }
    }
}
