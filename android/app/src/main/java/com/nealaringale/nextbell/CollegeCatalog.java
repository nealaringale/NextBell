package com.nealaringale.nextbell;

import java.util.Locale;

/** Supported college profiles and the academic data package selected by each profile. */
public final class CollegeCatalog {
    public static final String NMIET = "NMIET";
    public static final String DYPIIT_PIMPRI = "DYPIIT_PIMPRI";

    private static final String[] IDS = {NMIET, DYPIIT_PIMPRI};

    private CollegeCatalog() {}

    public static String normalize(String collegeId) {
        if (collegeId == null) return NMIET;
        String value = collegeId.trim().toUpperCase(Locale.ENGLISH);
        return DYPIIT_PIMPRI.equals(value) ? DYPIIT_PIMPRI : NMIET;
    }

    public static boolean isSupported(String collegeId) {
        return NMIET.equals(normalize(collegeId))
                || DYPIIT_PIMPRI.equals(normalize(collegeId));
    }

    public static String[] ids() {
        return IDS.clone();
    }

    public static String[] names() {
        return new String[] {
                "Nutan Maharashtra Institute of Engineering and Technology (NMIET)",
                "Dr. D. Y. Patil Institute of Technology, Pimpri"
        };
    }

    public static String name(String collegeId) {
        switch (normalize(collegeId)) {
            case DYPIIT_PIMPRI:
                return "Dr. D. Y. Patil Institute of Technology, Pimpri";
            default:
                return "Nutan Maharashtra Institute of Engineering and Technology";
        }
    }

    public static String shortName(String collegeId) {
        return DYPIIT_PIMPRI.equals(normalize(collegeId)) ? "D. Y. PATIL IT" : "NMIET";
    }

    public static String academicLabel(String collegeId) {
        return "First Year Engineering  •  Semester I  •  AY 2026–27";
    }

    public static String timetableDescription(String collegeId) {
        if (DYPIIT_PIMPRI.equals(normalize(collegeId))) {
            return "Imported from the college timetable you supplied. Published dates: 1, 3, 5, 6 and 8 December 2026. The PDF lists teacher abbreviations, not full faculty names.";
        }
        return "NMIET First Year timetable, Academic Year 2026–27. Division-specific weekly schedules and practical batches are enabled.";
    }
}
