package com.nealaringale.nextbell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Source: official FE Div B timetable, W.E.F. 16/09/2026, Revision 00.
public final class TimetableData {
    public static final String[] DAYS = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"
    };

    public enum Kind {
        CLASS,
        PRACTICAL,
        TUTORIAL,
        LIBRARY,
        SPECIAL,
        SELF_LEARNING,
        SHORT_BREAK,
        LUNCH
    }

    public static final class ClassItem {
        public final String id;
        public final String start;
        public final String end;
        public final String subject;
        public final String code;
        public final String teacher;
        public final String room;
        public final String batch;
        public final String audience;
        public final Kind kind;

        public ClassItem(
                String id,
                String start,
                String end,
                String subject,
                String code,
                String teacher,
                String room,
                String batch,
                String audience,
                Kind kind
        ) {
            this.id = id;
            this.start = start;
            this.end = end;
            this.subject = subject;
            this.code = code;
            this.teacher = teacher;
            this.room = room;
            this.batch = batch;
            this.audience = audience;
            this.kind = kind;
        }

        public boolean isAcademic() {
            return kind != Kind.SHORT_BREAK && kind != Kind.LUNCH;
        }

        public boolean isBreak() {
            return kind == Kind.SHORT_BREAK || kind == Kind.LUNCH;
        }
    }

    private TimetableData() {}

    public static String batchForRoll(int rollNumber) {
        if (rollNumber >= 1 && rollNumber <= 25) return "B1";
        if (rollNumber >= 26 && rollNumber <= 50) return "B2";
        if (rollNumber >= 51) return "B3";
        return "";
    }

    public static String rollRangeForBatch(String batch) {
        switch (batch) {
            case "B1":
                return "Roll 1–25";
            case "B2":
                return "Roll 26–50";
            case "B3":
                return "Roll 51 onwards";
            default:
                return "";
        }
    }

    public static List<ClassItem> forRollAndDay(int rollNumber, String day) {
        String batch = batchForRoll(rollNumber);
        List<ClassItem> all = allEntries(day);
        List<ClassItem> result = new ArrayList<>();

        for (ClassItem item : all) {
            if (appliesToStudent(item, rollNumber, batch)) {
                result.add(item);
            }
        }

        Collections.sort(result, (a, b) -> a.start.compareTo(b.start));
        return result;
    }

    private static boolean appliesToStudent(ClassItem item, int rollNumber, String batch) {
        if (item.audience.equals("COMMON")) return true;
        if (item.audience.equals("B1") || item.audience.equals("B2") || item.audience.equals("B3")) {
            return item.audience.equals(batch);
        }
        if (item.audience.equals("ROLL_1_35")) {
            return rollNumber >= 1 && rollNumber <= 35;
        }
        if (item.audience.equals("ROLL_36_PLUS")) {
            return rollNumber >= 36;
        }
        return false;
    }

    private static List<ClassItem> allEntries(String day) {
        switch (day) {
            case "Monday":
                return Arrays.asList(
                        // 8:20–10:20: one block, split by batch.
                        entry("mon-b1", "08:20", "10:20",
                                "Library", "", "", "Library B1", "B1", "B1",
                                Kind.LIBRARY),

                        entry("mon-b2", "08:20", "10:20",
                                "Fundamental of Programming Languages", "FPL",
                                "YR (as printed)", "109 · Wing B", "B2", "B2",
                                Kind.PRACTICAL),

                        entry("mon-b3", "08:20", "10:20",
                                "Engineering Graphics", "EG",
                                "Prof. Santosh Dabhole", "Wing B · 312", "B3", "B3",
                                Kind.PRACTICAL),

                        entry("mon-break", "10:20", "10:40",
                                "Short Break", "", "", "", "", "COMMON",
                                Kind.SHORT_BREAK),

                        entry("mon-cs", "10:40", "12:40",
                                "Communication Skills in Corporate World", "CS",
                                "Prof. Amol Bade", "201", "", "COMMON",
                                Kind.CLASS),

                        entry("mon-lunch", "12:40", "13:30",
                                "Lunch Break", "", "", "", "", "COMMON",
                                Kind.LUNCH),

                        entry("mon-ele", "13:30", "14:30",
                                "Electrical Engineering", "ELE",
                                "Prof. Arti S. Bindu", "201", "", "COMMON",
                                Kind.CLASS),

                        entry("mon-lam-36", "14:30", "15:30",
                                "Linear Algebra & Multivariable Calculus Tutorial", "LAM · TU",
                                "Prof. Vrushali Gujar", "201", "", "ROLL_36_PLUS",
                                Kind.TUTORIAL)
                );

            case "Tuesday":
                return Arrays.asList(
                        entry("tue-fpl", "08:20", "09:20",
                                "Fundamental of Programming Languages", "FPL",
                                "Prof. Sanket Sontakke", "201", "", "COMMON",
                                Kind.CLASS),

                        entry("tue-eg", "09:20", "10:20",
                                "Engineering Graphics", "EG",
                                "Prof. Santosh Dabhole", "201", "", "COMMON",
                                Kind.CLASS),

                        entry("tue-break", "10:20", "10:40",
                                "Short Break", "", "", "", "", "COMMON",
                                Kind.SHORT_BREAK),

                        entry("tue-b1", "10:40", "12:40",
                                "Fundamental of Programming Languages", "FPL",
                                "Prof. Sanket Sontakke", "109 · Wing B", "B1", "B1",
                                Kind.PRACTICAL),

                        entry("tue-b2", "10:40", "12:40",
                                "Engineering Physics", "PHY",
                                "Prof. Rupali Jagnade", "115 · Wing B", "B2", "B2",
                                Kind.PRACTICAL),

                        entry("tue-b3", "10:40", "12:40",
                                "Library", "", "", "Library B3", "B3", "B3",
                                Kind.LIBRARY),

                        entry("tue-lunch", "12:40", "13:30",
                                "Lunch Break", "", "", "", "", "COMMON",
                                Kind.LUNCH),

                        entry("tue-ls", "13:30", "15:30",
                                "LS-1 / LS-2", "LS",
                                "", "", "", "COMMON",
                                Kind.SPECIAL)
                );

            case "Wednesday":
                return Arrays.asList(
                        entry("wed-phy", "08:20", "09:20",
                                "Engineering Physics", "PHY",
                                "Prof. Rupali Jagnade", "202", "", "COMMON",
                                Kind.CLASS),

                        entry("wed-eg", "09:20", "10:20",
                                "Engineering Graphics", "EG",
                                "Prof. Santosh Dabhole", "202", "", "COMMON",
                                Kind.CLASS),

                        entry("wed-break", "10:20", "10:40",
                                "Short Break", "", "", "", "", "COMMON",
                                Kind.SHORT_BREAK),

                        entry("wed-ele", "10:40", "11:40",
                                "Electrical Engineering", "ELE",
                                "Prof. Arti S. Bindu", "202", "", "COMMON",
                                Kind.CLASS),

                        entry("wed-lam", "11:40", "12:40",
                                "Linear Algebra & Multivariable Calculus", "LAM",
                                "Prof. Vrushali Gujar", "202", "", "COMMON",
                                Kind.CLASS),

                        entry("wed-lunch", "12:40", "13:30",
                                "Lunch Break", "", "", "", "", "COMMON",
                                Kind.LUNCH),

                        entry("wed-lam-35", "13:30", "14:30",
                                "Linear Algebra & Multivariable Calculus Tutorial", "LAM · TU",
                                "Prof. Vrushali Gujar", "205", "", "ROLL_1_35",
                                Kind.TUTORIAL),

                        entry("wed-self", "14:30", "15:30",
                                "Self Learning", "", "", "", "", "COMMON",
                                Kind.SELF_LEARNING)
                );

            case "Thursday":
                return Arrays.asList(
                        entry("thu-b1", "08:20", "10:20",
                                "Engineering Graphics", "EG",
                                "Prof. Santosh Dabhole", "312 · Wing B", "B1", "B1",
                                Kind.PRACTICAL),

                        entry("thu-b2", "08:20", "10:20",
                                "Electrical Engineering", "ELE",
                                "Prof. Arti S. Bindu", "101 · Wing C", "B2", "B2",
                                Kind.PRACTICAL),

                        entry("thu-b3", "08:20", "10:20",
                                "Engineering Physics", "PHY",
                                "Prof. Rupali Jagnade", "115 · Wing B", "B3", "B3",
                                Kind.PRACTICAL),

                        entry("thu-break", "10:20", "10:40",
                                "Short Break", "", "", "", "", "COMMON",
                                Kind.SHORT_BREAK),

                        entry("thu-lam", "10:40", "11:40",
                                "Linear Algebra & Multivariable Calculus", "LAM",
                                "Prof. Vrushali Gujar", "202", "", "COMMON",
                                Kind.CLASS),

                        entry("thu-fpl", "11:40", "12:40",
                                "Fundamental of Programming Languages", "FPL",
                                "Prof. Sanket Sontakke", "202", "", "COMMON",
                                Kind.CLASS),

                        entry("thu-lunch", "12:40", "13:30",
                                "Lunch Break", "", "", "", "", "COMMON",
                                Kind.LUNCH),

                        entry("thu-pm-b1", "13:30", "15:30",
                                "Engineering Physics", "PHY",
                                "Prof. Rupali Jagnade", "115 · Wing B", "B1", "B1",
                                Kind.PRACTICAL),

                        entry("thu-pm-b2", "13:30", "15:30",
                                "Engineering Graphics", "EG",
                                "Prof. Santosh Dabhole", "310 · Wing C", "B2", "B2",
                                Kind.PRACTICAL),

                        entry("thu-pm-b3", "13:30", "15:30",
                                "Electrical Engineering", "ELE",
                                "Prof. Arti S. Bindu", "101 · Wing C", "B3", "B3",
                                Kind.PRACTICAL)
                );

            case "Friday":
                return Arrays.asList(
                        entry("fri-b1", "08:20", "10:20",
                                "Electrical Engineering", "ELE",
                                "Prof. Arti S. Bindu", "101 · Wing C", "B1", "B1",
                                Kind.PRACTICAL),

                        entry("fri-b2", "08:20", "10:20",
                                "Library", "", "", "Library B2", "B2", "B2",
                                Kind.LIBRARY),

                        entry("fri-b3", "08:20", "10:20",
                                "Fundamental of Programming Languages", "FPL",
                                "Prof. Sanket Sontakke", "109 · Wing B", "B3", "B3",
                                Kind.PRACTICAL),

                        entry("fri-break", "10:20", "10:40",
                                "Short Break", "", "", "", "", "COMMON",
                                Kind.SHORT_BREAK),

                        entry("fri-dti", "10:40", "11:40",
                                "Design Thinking and Innovation", "DTI",
                                "Prof. Mujiaid Shaikh", "202", "", "COMMON",
                                Kind.CLASS),

                        entry("fri-lam", "11:40", "12:40",
                                "Linear Algebra & Multivariable Calculus", "LAM",
                                "Prof. Vrushali Gujar", "202", "", "COMMON",
                                Kind.CLASS),

                        entry("fri-lunch", "12:40", "13:30",
                                "Lunch Break", "", "", "", "", "COMMON",
                                Kind.LUNCH),

                        entry("fri-phy", "13:30", "14:30",
                                "Engineering Physics", "PHY",
                                "Prof. Rupali Jagnade", "202", "", "COMMON",
                                Kind.CLASS),

                        entry("fri-self", "14:30", "15:30",
                                "Self Learning", "", "", "", "", "COMMON",
                                Kind.SELF_LEARNING)
                );

            default:
                return Collections.emptyList();
        }
    }

    private static ClassItem entry(
            String id,
            String start,
            String end,
            String subject,
            String code,
            String teacher,
            String room,
            String batch,
            String audience,
            Kind kind
    ) {
        return new ClassItem(
                id, start, end, subject, code, teacher, room, batch, audience, kind
        );
    }
}
