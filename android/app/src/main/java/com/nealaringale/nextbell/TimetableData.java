package com.nealaringale.nextbell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TimetableData {
    public static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

    public static final class ClassItem {
        public final String id;
        public final String start;
        public final String end;
        public final String subject;
        public final String teacher;
        public final String room;
        public final String type;
        public final String batch;

        public ClassItem(String id, String start, String end, String subject, String teacher,
                         String room, String type, String batch) {
            this.id = id;
            this.start = start;
            this.end = end;
            this.subject = subject;
            this.teacher = teacher;
            this.room = room;
            this.type = type;
            this.batch = batch;
        }
    }

    private TimetableData() {}

    public static List<ClassItem> forDay(String day) {
        switch (day) {
            case "Monday":
                return Arrays.asList(
                        c("mon-1", "08:00", "09:00", "Engineering Mathematics", "Prof. Kulkarni", "C-204", "Lecture", ""),
                        c("mon-2", "09:00", "10:00", "Engineering Physics", "Prof. Patil", "C-204", "Lecture", ""),
                        c("mon-3", "10:15", "12:15", "Programming Lab", "Prof. Jadhav", "Lab 3", "Practical", "B2"),
                        c("mon-4", "13:15", "14:15", "Elements of Electrical Engineering", "Prof. More", "C-205", "Lecture", ""),
                        c("mon-5", "14:15", "15:15", "Engineering Graphics", "Prof. Shinde", "C-206", "Lecture", "")
                );
            case "Tuesday":
                return Arrays.asList(
                        c("tue-1", "08:00", "09:00", "Elements of Electrical Engineering", "Prof. More", "C-204", "Lecture", ""),
                        c("tue-2", "09:00", "10:00", "Engineering Mathematics", "Prof. Kulkarni", "C-204", "Lecture", ""),
                        c("tue-3", "10:15", "11:15", "Engineering Chemistry", "Prof. Deshmukh", "C-205", "Lecture", ""),
                        c("tue-4", "11:15", "13:15", "Electronics Lab", "Prof. Pawar", "ELX Lab", "Practical", "B2"),
                        c("tue-5", "14:00", "15:00", "Communication Skills", "Prof. Joshi", "C-302", "Tutorial", "B2")
                );
            case "Wednesday":
                return Arrays.asList(
                        c("wed-1", "08:00", "09:00", "Engineering Mathematics", "Prof. Kulkarni", "C-204", "Lecture", ""),
                        c("wed-2", "09:00", "10:00", "Engineering Chemistry", "Prof. Deshmukh", "C-204", "Lecture", ""),
                        c("wed-3", "10:15", "11:15", "Engineering Physics", "Prof. Patil", "C-205", "Lecture", ""),
                        c("wed-4", "11:15", "13:15", "Engineering Graphics", "Prof. Shinde", "Drawing Hall", "Practical", "B2")
                );
            case "Thursday":
                return Arrays.asList(
                        c("thu-1", "08:00", "09:00", "Engineering Physics", "Prof. Patil", "C-204", "Lecture", ""),
                        c("thu-2", "09:00", "10:00", "Engineering Mathematics", "Prof. Kulkarni", "C-204", "Lecture", ""),
                        c("thu-3", "10:15", "11:15", "Elements of Electrical Engineering", "Prof. More", "C-205", "Lecture", ""),
                        c("thu-4", "11:15", "13:15", "Programming Lab", "Prof. Jadhav", "Lab 3", "Practical", "B2"),
                        c("thu-5", "14:00", "15:00", "Engineering Chemistry", "Prof. Deshmukh", "C-206", "Lecture", "")
                );
            case "Friday":
                return Arrays.asList(
                        c("fri-1", "08:00", "09:00", "Engineering Mathematics", "Prof. Kulkarni", "C-204", "Lecture", ""),
                        c("fri-2", "09:00", "10:00", "Elements of Electrical Engineering", "Prof. More", "C-204", "Lecture", ""),
                        c("fri-3", "10:15", "11:15", "Communication Skills", "Prof. Joshi", "C-302", "Tutorial", "B2"),
                        c("fri-4", "11:15", "13:15", "Electronics Lab", "Prof. Pawar", "ELX Lab", "Practical", "B2")
                );
            case "Saturday":
                return Arrays.asList(
                        c("sat-1", "09:00", "10:00", "Engineering Physics", "Prof. Patil", "C-204", "Lecture", ""),
                        c("sat-2", "10:15", "11:15", "Engineering Chemistry", "Prof. Deshmukh", "C-205", "Lecture", "")
                );
            default:
                return new ArrayList<>();
        }
    }

    private static ClassItem c(String id, String start, String end, String subject, String teacher,
                               String room, String type, String batch) {
        return new ClassItem(id, start, end, subject, teacher, room, type, batch);
    }
}
