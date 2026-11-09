package com.nealaringale.nextbell;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class StudentPlannerStoreTest {
    @Test
    public void sortsOpenDeadlinesBeforeCompletedAndUndatedItems() {
        List<StudentPlannerStore.Item> items = new ArrayList<>(Arrays.asList(
                new StudentPlannerStore.Item(1, StudentPlannerStore.TASK,
                        "No date", "", "", null, false),
                new StudentPlannerStore.Item(2, StudentPlannerStore.ASSIGNMENT,
                        "Later", "Maths", "", LocalDate.of(2026, 12, 1), false),
                new StudentPlannerStore.Item(3, StudentPlannerStore.EXAM,
                        "Soon", "Physics", "", LocalDate.of(2026, 11, 8), false),
                new StudentPlannerStore.Item(4, StudentPlannerStore.TASK,
                        "Done", "", "", LocalDate.of(2026, 11, 7), true)
        ));

        StudentPlannerStore.sort(items);
        assertEquals("Soon", items.get(0).title);
        assertEquals("Later", items.get(1).title);
        assertEquals("No date", items.get(2).title);
        assertEquals("Done", items.get(3).title);
    }

    @Test
    public void itemFieldsAreTrimmedAndCapped() {
        String title = new String(new char[140]).replace('\0', 'x');
        StudentPlannerStore.Item item = new StudentPlannerStore.Item(
                1, "unknown", " " + title + " ", null, null, null, false
        );

        assertEquals(120, item.title.length());
        assertEquals(StudentPlannerStore.TASK, item.type);
        assertEquals("", item.course);
    }
}
