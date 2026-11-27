package com.nealaringale.nextbell;

import java.time.LocalDate;
import java.util.List;

/** Routes timetable queries to the selected college's own academic dataset. */
public final class CollegeScheduleData {
    private static final String[] NMIET_DAYS = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"
    };
    private static final String[] DYPIT_DAYS = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };
    private static final String[] NMIET_DIVISIONS = {
            "A", "B", "C", "D", "E", "F", "G", "H",
            "I", "J", "K", "L", "M", "N", "O", "P"
    };

    private CollegeScheduleData() {}

    public static String[] days(String collegeId) {
        return CollegeCatalog.DYPIIT_PIMPRI.equals(CollegeCatalog.normalize(collegeId))
                ? DYPIT_DAYS.clone() : NMIET_DAYS.clone();
    }

    public static String[] divisions(String collegeId) {
        if (CollegeCatalog.DYPIIT_PIMPRI.equals(CollegeCatalog.normalize(collegeId))) {
            return DypitTimetableData.divisions();
        }
        return NMIET_DIVISIONS.clone();
    }

    public static boolean supportsDivision(String collegeId, String division) {
        if (division == null) return false;
        String value = division.trim().toUpperCase(java.util.Locale.ENGLISH);
        if (CollegeCatalog.DYPIIT_PIMPRI.equals(CollegeCatalog.normalize(collegeId))) {
            return value.matches("[A-T]");
        }
        return TimetableData.isSupportedDivision(value);
    }

    public static String batchForRoll(String collegeId, String division, int rollNumber) {
        if (CollegeCatalog.DYPIIT_PIMPRI.equals(CollegeCatalog.normalize(collegeId))) {
            return "";
        }
        return TimetableData.batchForDivisionRoll(division, rollNumber);
    }

    public static String rollRangeForBatch(String collegeId, String division, String batch) {
        if (CollegeCatalog.DYPIIT_PIMPRI.equals(CollegeCatalog.normalize(collegeId))) {
            return "";
        }
        return TimetableData.rollRangeForDivisionBatch(division, batch);
    }

    public static int maximumRoll(String collegeId, String division) {
        if (CollegeCatalog.DYPIIT_PIMPRI.equals(CollegeCatalog.normalize(collegeId))) {
            return 999;
        }
        return FirstYearTimetableData.maximumRoll(division);
    }

    public static boolean isValidRoll(String collegeId, String division, int rollNumber) {
        if (rollNumber < 1 || rollNumber > 999 || !supportsDivision(collegeId, division)) {
            return false;
        }
        if (CollegeCatalog.DYPIIT_PIMPRI.equals(CollegeCatalog.normalize(collegeId))) {
            return true;
        }
        return !batchForRoll(collegeId, division, rollNumber).isEmpty();
    }

    public static List<TimetableData.ClassItem> forDate(
            String collegeId, String division, int rollNumber, LocalDate date) {
        if (date == null) return java.util.Collections.emptyList();
        if (CollegeCatalog.DYPIIT_PIMPRI.equals(CollegeCatalog.normalize(collegeId))) {
            return DypitTimetableData.forDate(division, date);
        }
        String day = dayName(date);
        return TimetableData.forDivisionRollAndDay(division, rollNumber, day);
    }

    public static List<TimetableData.ClassItem> forDay(
            String collegeId, String division, int rollNumber, String day, LocalDate anchorDate) {
        if (CollegeCatalog.DYPIIT_PIMPRI.equals(CollegeCatalog.normalize(collegeId))) {
            return DypitTimetableData.forDay(division, day, anchorDate);
        }
        return TimetableData.forDivisionRollAndDay(division, rollNumber, day);
    }

    public static LocalDate dateForDay(String collegeId, String day, LocalDate anchorDate) {
        if (!CollegeCatalog.DYPIIT_PIMPRI.equals(CollegeCatalog.normalize(collegeId))) {
            return null;
        }
        return DypitTimetableData.dateForDay(day, anchorDate);
    }

    public static LocalDate nextPublishedDate(String collegeId, LocalDate anchorDate) {
        if (!CollegeCatalog.DYPIIT_PIMPRI.equals(CollegeCatalog.normalize(collegeId))) {
            return null;
        }
        return DypitTimetableData.nextPublishedDate(anchorDate);
    }

    public static String dayName(LocalDate date) {
        if (date == null) return "";
        switch (date.getDayOfWeek()) {
            case MONDAY: return "Monday";
            case TUESDAY: return "Tuesday";
            case WEDNESDAY: return "Wednesday";
            case THURSDAY: return "Thursday";
            case FRIDAY: return "Friday";
            case SATURDAY: return "Saturday";
            case SUNDAY: return "Sunday";
            default: return "";
        }
    }

    public static String studentGroupLabel(String collegeId, String division, int rollNumber) {
        String batch = batchForRoll(collegeId, division, rollNumber);
        String value = "Division " + division;
        if (!batch.isEmpty()) value += "  •  " + batch;
        return value + "  •  Roll " + rollNumber;
    }
}
