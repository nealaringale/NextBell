package com.nealaringale.nextbell;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.Test;

public class AttendanceStoreTest {
    @Test
    public void calculatesAttendanceAndSafeAbsences() {
        AttendanceStore.Stats stats = new AttendanceStore.Stats(8, 10, 75);
        assertEquals(80, stats.percentage());
        assertEquals(0, stats.classesToTarget());
        assertEquals(0, stats.safeAbsences());

        AttendanceStore.Stats comfortable = new AttendanceStore.Stats(9, 10, 75);
        assertEquals(2, comfortable.safeAbsences());
    }

    @Test
    public void calculatesClassesNeededToReachTarget() {
        AttendanceStore.Stats stats = new AttendanceStore.Stats(5, 10, 75);
        assertEquals(10, stats.classesToTarget());
        assertEquals(0, stats.safeAbsences());
    }

    @Test
    public void ignoresOtherSubjectsAndCountsRecordedSessionsOnly() {
        AttendanceStore.Session a = new AttendanceStore.Session(
                "a", "Physics", "09:00", "10:00", LocalDate.now(), "PRESENT");
        AttendanceStore.Session b = new AttendanceStore.Session(
                "b", "Maths", "10:00", "11:00", LocalDate.now(), "ABSENT");
        AttendanceStore.Stats stats = AttendanceStore.stats(Arrays.asList(a, b), "Physics", 75);
        assertEquals(100, stats.percentage());
        assertEquals(1, stats.attended);
        assertEquals(1, stats.held);
    }
}
