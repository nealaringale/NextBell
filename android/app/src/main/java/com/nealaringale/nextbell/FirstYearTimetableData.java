package com.nealaringale.nextbell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Official NMIET FE Semester I timetable data, AY 2026-27, W.E.F. 16-Sep-2026.
 *
 * Division B deliberately remains in TimetableData's existing implementation so
 * existing B-division schedules remain unchanged while the other divisions are
 * added and verified. All new divisions use the same ClassItem model and filter.
 */
public final class FirstYearTimetableData {
    private static final String[] DIVISIONS = {
            "A", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P"
    };

    private static final Map<String, String[]> BATCHES = new HashMap<>();
    private static final Map<String, Integer> MAX_ROLL = new HashMap<>();
    private static final Map<String, List<TimetableData.ClassItem>> ALL = new LinkedHashMap<>();
    private static final Map<String, List<TimetableData.ClassItem>> CACHE = new HashMap<>();
    private static final Map<String, String> SUBJECTS = new HashMap<>();
    private static final Map<String, String> STAFF = new HashMap<>();

    private static int nextId;

    static {
        SUBJECTS.put("LAM", "Linear Algebra and Multivariable Calculus");
        SUBJECTS.put("PHY", "Engineering Physics");
        SUBJECTS.put("ELE", "Electrical Engineering");
        SUBJECTS.put("EG", "Engineering Graphics");
        SUBJECTS.put("FPL", "Fundamental of Programming Languages");
        SUBJECTS.put("DTI", "Design Thinking and Innovation");
        SUBJECTS.put("CS", "Communication Skills in Corporate World");
        SUBJECTS.put("CHY", "Engineering Chemistry");
        SUBJECTS.put("BXE", "Elements of Electronics");
        SUBJECTS.put("EXE", "Elements of Electronics");
        SUBJECTS.put("EM", "Engineering Mechanics");
        SUBJECTS.put("WS", "Workshop");
        SUBJECTS.put("LIBRARY", "Library");
        SUBJECTS.put("SELF", "Self Learning");
        SUBJECTS.put("LS", "Life Skills (LS-1 / LS-2)");

        defineDivision("A", new String[]{"A-1", "A-2", "A-3"}, 0);
        defineDivision("C", new String[]{"C-1", "C-2", "C-3"}, 0);
        defineDivision("D", new String[]{"D-1", "D-2", "D-3"}, 0);
        defineDivision("E", new String[]{"E-1", "E-2", "E-3"}, 0);
        defineDivision("F", new String[]{"F-1", "F-2", "F-3"}, 0);
        defineDivision("G", new String[]{"G1", "G2", "G3"}, 0);
        defineDivision("H", new String[]{"H1", "H2", "H3"}, 0);
        defineDivision("I", new String[]{"I-1", "I-2", "I-3"}, 0);
        defineDivision("J", new String[]{"J1", "J2", "J3"}, 74);
        defineDivision("K", new String[]{"K1", "K2", "K3"}, 74);
        defineDivision("L", new String[]{"L1", "L2", "L3"}, 74);
        defineDivision("M", new String[]{"M1", "M2", "M3"}, 74);
        defineDivision("N", new String[]{"N-1", "N-2", "N-3"}, 0);
        defineDivision("O", new String[]{"O-1", "O-2", "O-3"}, 0);
        defineDivision("P", new String[]{"P1", "P2", "P3"}, 74);

        defineStaff();

        // -----------------------------------------------------------------
        // DIVISION A — official grid, Revision 00.
        // -----------------------------------------------------------------
        c("A", "Monday", "08:20", "09:20", "PHY", "RJ", "201");
        c("A", "Monday", "09:20", "10:20", "ELE", "ASB", "201");
        p("A", "Monday", "10:40", "12:40", "FPL", "SS", "Wing B 109", "A-1");
        p("A", "Monday", "10:40", "12:40", "PHY", "RJ", "Wing B 115", "A-2");
        p("A", "Monday", "10:40", "12:40", "ELE", "ASB", "Wing C 101", "A-3");
        special("A", "Monday", "13:30", "15:30", "LS", "LS-1 / LS-2");

        p("A", "Tuesday", "08:20", "10:20", "LIBRARY", "", "Library", "A-2");
        p("A", "Tuesday", "08:20", "10:20", "PHY", "RJ", "Wing B 115", "A-3");
        p("A", "Tuesday", "08:20", "10:20", "ELE", "ASB", "Wing C 101", "A-1");
        c("A", "Tuesday", "10:40", "11:40", "LAM", "PK", "201");
        c("A", "Tuesday", "11:40", "12:40", "EG", "SSD", "201");
        p("A", "Tuesday", "13:30", "15:30", "FPL", "SS", "Wing B 109", "A-3");
        p("A", "Tuesday", "13:30", "15:30", "EG", "SSD", "Wing C 210", "A-1");
        p("A", "Tuesday", "13:30", "15:30", "ELE", "ASB", "Wing C 101", "A-2");

        c("A", "Wednesday", "08:20", "09:20", "LAM", "PK", "201");
        c("A", "Wednesday", "09:20", "10:20", "FPL", "SS", "201");
        p("A", "Wednesday", "10:40", "12:40", "LIBRARY", "", "Library", "A-3");
        p("A", "Wednesday", "10:40", "12:40", "PHY", "RJ", "Wing B 115", "A-1");
        p("A", "Wednesday", "10:40", "12:40", "EG", "SSD", "Wing C 210", "A-2");
        c("A", "Wednesday", "13:30", "14:30", "CS", "AB", "201");
        c("A", "Wednesday", "14:30", "15:30", "DTI", "MS", "201");

        c("A", "Thursday", "08:20", "09:20", "FPL", "SS", "201");
        c("A", "Thursday", "09:20", "10:20", "LAM", "PK", "201");
        c("A", "Thursday", "10:40", "11:40", "PHY", "RJ", "201");
        c("A", "Thursday", "11:40", "12:40", "ELE", "ASB", "201");
        t("A", "Thursday", "13:30", "14:30", "LAM", "PK", "209", "ROLL_36_PLUS");
        self("A", "Thursday", "14:30", "15:30", "COMMON");

        c("A", "Friday", "08:20", "09:20", "CS", "AB", "201");
        c("A", "Friday", "09:20", "10:20", "EG", "SSD", "201");
        p("A", "Friday", "10:40", "12:40", "FPL", "SS", "Wing B 109", "A-2");
        p("A", "Friday", "10:40", "12:40", "EG", "SSD", "Wing C 210", "A-3");
        p("A", "Friday", "10:40", "12:40", "LIBRARY", "", "Library", "A-1");
        t("A", "Friday", "13:30", "14:30", "LAM", "PK", "209", "ROLL_1_35");
        self("A", "Friday", "14:30", "15:30", "COMMON");

        // -----------------------------------------------------------------
        // DIVISION C — official grid, Revision 00.
        // -----------------------------------------------------------------
        c("C", "Monday", "08:20", "09:20", "PHY", "MG", "202");
        c("C", "Monday", "09:20", "10:20", "LAM", "SU", "202");
        p("C", "Monday", "10:40", "12:40", "FPL", "TG", "Wing B 109", "C-1");
        p("C", "Monday", "10:40", "12:40", "PHY", "MG", "Wing A 019", "C-2");
        p("C", "Monday", "10:40", "12:40", "ELE", "JG", "Wing A 206", "C-3");
        c("C", "Monday", "13:30", "14:30", "EG", "SG", "202");
        t("C", "Monday", "14:30", "15:30", "LAM", "SU", "202", "ROLL_1_35");

        p("C", "Tuesday", "08:20", "10:20", "LIBRARY", "", "Library", "C-2");
        p("C", "Tuesday", "08:20", "10:20", "PHY", "MG", "Wing A 019", "C-3");
        p("C", "Tuesday", "08:20", "10:20", "ELE", "JG", "Wing A 206", "C-1");
        c("C", "Tuesday", "10:40", "11:40", "CS", "AB", "202");
        c("C", "Tuesday", "11:40", "12:40", "CS", "AB", "202");
        p("C", "Tuesday", "13:30", "15:30", "FPL", "TG", "Wing B 109", "C-3");
        p("C", "Tuesday", "13:30", "15:30", "EG", "ML", "Wing B 005", "C-1");
        p("C", "Tuesday", "13:30", "15:30", "ELE", "JG", "Wing A 206", "C-2");

        c("C", "Wednesday", "08:20", "09:20", "ELE", "JG", "209");
        c("C", "Wednesday", "09:20", "10:20", "LAM", "SU", "209");
        p("C", "Wednesday", "10:40", "12:40", "LIBRARY", "", "Library", "C-3");
        p("C", "Wednesday", "10:40", "12:40", "PHY", "MG", "Wing A 019", "C-1");
        p("C", "Wednesday", "10:40", "12:40", "EG", "ML", "Wing B 005", "C-2");
        c("C", "Wednesday", "13:30", "14:30", "FPL", "TG", "209");
        self("C", "Wednesday", "14:30", "15:30", "COMMON");

        c("C", "Thursday", "08:20", "09:20", "LAM", "SU", "203");
        c("C", "Thursday", "09:20", "10:20", "EG", "SG", "203");
        c("C", "Thursday", "10:40", "11:40", "PHY", "MG", "203");
        t("C", "Thursday", "11:40", "12:40", "LAM", "SU", "203", "ROLL_36_PLUS");
        c("C", "Thursday", "13:30", "14:30", "DTI", "MS", "203");
        self("C", "Thursday", "14:30", "15:30", "COMMON");

        c("C", "Friday", "08:20", "09:20", "FPL", "TG", "202");
        c("C", "Friday", "09:20", "10:20", "ELE", "JG", "202");
        p("C", "Friday", "10:40", "12:40", "FPL", "TG", "Wing B 109", "C-2");
        p("C", "Friday", "10:40", "12:40", "EG", "ML", "Wing B 005", "C-3");
        p("C", "Friday", "10:40", "12:40", "LIBRARY", "", "Library", "C-1");
        special("C", "Friday", "13:30", "15:30", "LS", "LS-1 / LS-2");

        // -----------------------------------------------------------------
        // DIVISION D — official grid, Revision 00.
        // -----------------------------------------------------------------
        p("D", "Monday", "08:20", "10:20", "FPL", "TG", "Wing B 109", "D-2");
        p("D", "Monday", "08:20", "10:20", "EG", "SD", "Wing B 005", "D-3");
        p("D", "Monday", "08:20", "10:20", "LIBRARY", "", "Library", "D-1");
        c("D", "Monday", "10:40", "11:40", "LAM", "SU", "203");
        c("D", "Monday", "11:40", "12:40", "EG", "SD", "203");
        t("D", "Monday", "13:30", "14:30", "LAM", "SU", "207", "ROLL_36_PLUS");
        self("D", "Monday", "14:30", "15:30", "COMMON");

        c("D", "Tuesday", "08:20", "09:20", "FPL", "TG", "202");
        c("D", "Tuesday", "09:20", "10:20", "EG", "SD", "202");
        p("D", "Tuesday", "10:40", "12:40", "FPL", "TG", "Wing B 109", "D-1");
        p("D", "Tuesday", "10:40", "12:40", "PHY", "MG", "Wing A 019", "D-2");
        p("D", "Tuesday", "10:40", "12:40", "LIBRARY", "", "Library", "D-3");
        special("D", "Tuesday", "13:30", "15:30", "LS", "LS-1 / LS-2");

        c("D", "Wednesday", "08:20", "09:20", "PHY", "MG", "203");
        c("D", "Wednesday", "09:20", "10:20", "FPL", "TG", "203");
        c("D", "Wednesday", "10:40", "11:40", "ELE", "JG", "203");
        c("D", "Wednesday", "11:40", "12:40", "LAM", "SU", "203");
        t("D", "Wednesday", "13:30", "14:30", "LAM", "SU", "203", "ROLL_1_35");
        self("D", "Wednesday", "14:30", "15:30", "COMMON");

        p("D", "Thursday", "08:20", "10:20", "PHY", "MG", "Wing A 019", "D-3");
        p("D", "Thursday", "08:20", "10:20", "EG", "SD", "Wing B 005", "D-1");
        p("D", "Thursday", "08:20", "10:20", "ELE", "JG", "Wing A 206", "D-2");
        c("D", "Thursday", "10:40", "11:40", "CS", "AB", "210");
        c("D", "Thursday", "11:40", "12:40", "CS", "AB", "210");
        p("D", "Thursday", "13:30", "15:30", "ELE", "JG", "Wing A 206", "D-3");
        p("D", "Thursday", "13:30", "15:30", "PHY", "MG", "Wing A 019", "D-1");
        p("D", "Thursday", "13:30", "15:30", "EG", "SD", "Wing B 005", "D-2");
        p("D", "Friday", "13:30", "15:30", "LIBRARY", "", "Library", "D-2");
        p("D", "Friday", "13:30", "15:30", "FPL", "TG", "Wing B 109", "D-3");
        p("D", "Friday", "13:30", "15:30", "ELE", "JG", "Wing A 206", "D-1");

        c("D", "Friday", "08:20", "09:20", "PHY", "MG", "203");
        c("D", "Friday", "09:20", "10:20", "LAM", "SU", "203");
        c("D", "Friday", "10:40", "11:40", "ELE", "JG", "203");
        c("D", "Friday", "11:40", "12:40", "DTI", "MS", "203");

        // -----------------------------------------------------------------
        // DIVISION E — official grid, Revision 00.
        // -----------------------------------------------------------------
        c("E", "Monday", "08:20", "09:20", "CS", "BM", "203");
        c("E", "Monday", "09:20", "10:20", "CS", "BM", "203");
        c("E", "Monday", "10:40", "11:40", "FPL", "AB", "202");
        c("E", "Monday", "11:40", "12:40", "LAM", "BG", "202");
        p("E", "Monday", "13:30", "15:30", "EG", "SD", "Wing C 208", "E-2");
        p("E", "Monday", "13:30", "15:30", "PHY", "MJ", "Wing B 115", "E-3");
        p("E", "Monday", "13:30", "15:30", "ELE", "SS", "Wing A 206", "E-1");

        c("E", "Tuesday", "08:20", "09:20", "ELE", "SS", "204");
        c("E", "Tuesday", "09:20", "10:20", "PHY", "MJ", "204");
        c("E", "Tuesday", "10:40", "11:40", "EG", "SD", "204");
        c("E", "Tuesday", "11:40", "12:40", "LAM", "BG", "204");
        p("E", "Tuesday", "13:30", "15:30", "FPL", "AB", "Wing B 109", "E-3");
        p("E", "Tuesday", "13:30", "15:30", "PHY", "MJ", "Wing B 115", "E-1");
        p("E", "Tuesday", "13:30", "15:30", "LIBRARY", "", "Library", "E-2");

        p("E", "Wednesday", "08:20", "10:20", "LIBRARY", "", "Library", "E-3");
        p("E", "Wednesday", "08:20", "10:20", "EG", "SD", "Wing B 312", "E-1");
        p("E", "Wednesday", "08:20", "10:20", "ELE", "SS", "Wing A 206", "E-2");
        c("E", "Wednesday", "10:40", "11:40", "DTI", "MS", "201");
        t("E", "Wednesday", "11:40", "12:40", "LAM", "BG", "201", "ROLL_1_35");
        self("E", "Wednesday", "13:30", "15:30", "COMMON");

        c("E", "Thursday", "08:20", "09:20", "LAM", "BG", "204");
        c("E", "Thursday", "09:20", "10:20", "ELE", "SS", "204");
        c("E", "Thursday", "10:40", "11:40", "PHY", "MJ", "204");
        t("E", "Thursday", "11:40", "12:40", "LAM", "BG", "204", "ROLL_36_PLUS");
        special("E", "Thursday", "13:30", "15:30", "LS", "LS-1 / LS-2");

        c("E", "Friday", "08:20", "09:20", "FPL", "AB", "204");
        c("E", "Friday", "09:20", "10:20", "EG", "SD", "204");
        p("E", "Friday", "10:40", "12:40", "FPL", "AB", "Wing B CC 109", "E-1");
        p("E", "Friday", "10:40", "12:40", "PHY", "MJ", "Wing B 115", "E-2");
        p("E", "Friday", "10:40", "12:40", "ELE", "SS", "Wing A 206", "E-3");
        p("E", "Friday", "13:30", "15:30", "FPL", "AB", "Wing B 109", "E-2");
        p("E", "Friday", "13:30", "15:30", "EG", "SD", "Wing C 210", "E-3");
        p("E", "Friday", "13:30", "15:30", "LIBRARY", "", "Library", "E-1");

        // -----------------------------------------------------------------
        // DIVISION F — official grid, Revision 00.
        // -----------------------------------------------------------------
        p("F", "Monday", "08:20", "10:20", "FPL", "AB", "Wing B 109", "F-1");
        p("F", "Monday", "08:20", "10:20", "PHY", "MJ", "Wing B 115", "F-2");
        p("F", "Monday", "08:20", "10:20", "ELE", "SS", "Wing A 206", "F-3");
        c("F", "Monday", "10:40", "11:40", "LAM", "BG", "204");
        c("F", "Monday", "11:40", "12:40", "ELE", "SS", "204");
        c("F", "Monday", "13:30", "14:30", "DTI", "MS", "204");
        t("F", "Monday", "14:30", "15:30", "LAM", "BG", "204", "ROLL_36_PLUS");

        p("F", "Tuesday", "08:20", "10:20", "FPL", "AB", "Wing B 109", "F-2");
        p("F", "Tuesday", "08:20", "10:20", "EG", "SG", "Wing B 005", "F-3");
        p("F", "Tuesday", "08:20", "10:20", "LIBRARY", "", "Library", "F-1");
        c("F", "Tuesday", "10:40", "11:40", "ELE", "SS", "205");
        c("F", "Tuesday", "11:40", "12:40", "FPL", "AB", "205");
        self("F", "Tuesday", "13:30", "15:30", "COMMON");

        c("F", "Wednesday", "08:20", "09:20", "FPL", "AB", "204");
        c("F", "Wednesday", "09:20", "10:20", "LAM", "BG", "204");
        c("F", "Wednesday", "10:40", "11:40", "PHY", "MJ", "204");
        c("F", "Wednesday", "11:40", "12:40", "CS", "AB", "204");
        p("F", "Wednesday", "13:30", "15:30", "PHY", "MJ", "Wing B 115", "F-3");
        p("F", "Wednesday", "13:30", "15:30", "EG", "SG", "Wing B 005", "F-1");
        p("F", "Wednesday", "13:30", "15:30", "ELE", "SS", "Wing A 206", "F-2");

        c("F", "Thursday", "08:20", "09:20", "CS", "AB", "202");
        c("F", "Thursday", "09:20", "10:20", "PHY", "MJ", "202");
        p("F", "Thursday", "10:40", "12:40", "LIBRARY", "", "Library", "F-2");
        p("F", "Thursday", "10:40", "12:40", "FPL", "AB", "Wing B 109", "F-3");
        p("F", "Thursday", "10:40", "12:40", "ELE", "SS", "Wing A 206", "F-1");
        c("F", "Thursday", "13:30", "14:30", "EG", "SG", "202");
        t("F", "Thursday", "14:30", "15:30", "LAM", "BG", "202", "ROLL_1_35");

        special("F", "Friday", "08:20", "10:20", "LS", "LS-1 / LS-2");
        c("F", "Friday", "10:40", "11:40", "EG", "SG", "201");
        c("F", "Friday", "11:40", "12:40", "LAM", "BG", "201");
        p("F", "Friday", "13:30", "15:30", "LIBRARY", "", "Library", "F-3");
        p("F", "Friday", "13:30", "15:30", "PHY", "MJ", "Wing B 115", "F-1");
        p("F", "Friday", "13:30", "15:30", "EG", "SG", "Wing B 005", "F-2");

        // -----------------------------------------------------------------
        // DIVISION G — official grid, Revision 00.
        // -----------------------------------------------------------------
        p("G", "Monday", "08:20", "10:20", "FPL", "RP", "Wing B 109", "G1");
        p("G", "Monday", "08:20", "10:20", "PHY", "AR", "Wing A 019", "G2");
        p("G", "Monday", "08:20", "10:20", "ELE", "RS", "Wing C 101", "G3");
        c("G", "Monday", "10:40", "11:40", "EG", "SG", "205");
        c("G", "Monday", "11:40", "12:40", "LAM", "VS", "205");
        p("G", "Monday", "13:30", "15:30", "LIBRARY", "", "Library", "G2");
        p("G", "Monday", "13:30", "15:30", "PHY", "AR", "Wing A 019", "G3");
        p("G", "Monday", "13:30", "15:30", "ELE", "RS", "Wing C 101", "G1");

        c("G", "Tuesday", "08:20", "09:20", "LAM", "VS", "205");
        c("G", "Tuesday", "09:20", "10:20", "PHY", "AR", "205");
        p("G", "Tuesday", "10:40", "12:40", "FPL", "RP", "Wing B 109", "G3");
        p("G", "Tuesday", "10:40", "12:40", "EG", "SG", "Wing B 312", "G1");
        p("G", "Tuesday", "10:40", "12:40", "ELE", "RS", "Wing C 101", "G2");
        c("G", "Tuesday", "13:30", "14:30", "DTI", "MS", "201");
        t("G", "Tuesday", "14:30", "15:30", "LAM", "VS", "201", "ROLL_1_35");

        c("G", "Wednesday", "08:20", "09:20", "ELE", "RS", "205");
        c("G", "Wednesday", "09:20", "10:20", "EG", "SG", "205");
        c("G", "Wednesday", "10:40", "11:40", "FPL", "RP", "205");
        c("G", "Wednesday", "11:40", "12:40", "PHY", "AR", "205");
        self("G", "Wednesday", "13:30", "15:30", "COMMON");

        c("G", "Thursday", "08:20", "09:20", "FPL", "RP", "205");
        c("G", "Thursday", "09:20", "10:20", "ELE", "RS", "205");
        p("G", "Thursday", "10:40", "12:40", "FPL", "RP", "Wing B 109", "G2");
        p("G", "Thursday", "10:40", "12:40", "EG", "SG", "Wing B 312", "G3");
        p("G", "Thursday", "10:40", "12:40", "LIBRARY", "", "Library", "G1");
        c("G", "Thursday", "13:30", "14:30", "LAM", "VS", "204");
        t("G", "Thursday", "14:30", "15:30", "LAM", "VS", "204", "ROLL_36_PLUS");

        p("G", "Friday", "08:20", "10:20", "LIBRARY", "", "Library", "G3");
        p("G", "Friday", "08:20", "10:20", "PHY", "AR", "Wing A 019", "G1");
        p("G", "Friday", "08:20", "10:20", "EG", "SG", "Wing B 312", "G2");
        c("G", "Friday", "10:40", "11:40", "CS", "AB", "204");
        c("G", "Friday", "11:40", "12:40", "CS", "AB", "204");
        special("G", "Friday", "13:30", "15:30", "LS", "LS-1 / LS-2");

        // -----------------------------------------------------------------
        // DIVISION H — official grid, Revision 00.
        // -----------------------------------------------------------------
        c("H", "Monday", "08:20", "09:20", "FPL", "AG", "205");
        c("H", "Monday", "09:20", "10:20", "EXE", "SN", "205");
        p("H", "Monday", "10:40", "12:40", "WS", "MO", "Wing A", "H3");
        p("H", "Monday", "10:40", "12:40", "CHY", "AY", "Wing B 114", "H1");
        p("H", "Monday", "10:40", "12:40", "EM", "AJY", "Wing B 005", "H2");
        c("H", "Monday", "13:30", "14:30", "LAM", "KT", "205");
        self("H", "Monday", "14:30", "15:30", "COMMON");

        p("H", "Tuesday", "08:20", "10:20", "FPL", "AG", "Wing B 109", "H3");
        p("H", "Tuesday", "08:20", "10:20", "EM", "AJY", "Wing B 005", "H1");
        p("H", "Tuesday", "08:20", "10:20", "EXE", "SN", "Wing C 103", "H2");
        c("H", "Tuesday", "10:40", "11:40", "CS", "BM", "207");
        c("H", "Tuesday", "11:40", "12:40", "EXE", "SN", "207");
        t("H", "Tuesday", "13:30", "14:30", "LAM", "KT", "207", "ROLL_1_35");
        libRoll("H", "Tuesday", "13:30", "14:30", "207", "ROLL_36_PLUS");
        self("H", "Tuesday", "14:30", "15:30", "COMMON");

        c("H", "Wednesday", "08:20", "09:20", "EM", "AJY", "207");
        c("H", "Wednesday", "09:20", "10:20", "LAM", "KT", "207");
        c("H", "Wednesday", "10:40", "11:40", "CHY", "AY", "207");
        c("H", "Wednesday", "11:40", "12:40", "CS", "BM", "207");
        p("H", "Wednesday", "13:30", "15:30", "WS", "MO", "Wing A", "H2");
        p("H", "Wednesday", "13:30", "15:30", "CHY", "AY", "Wing B 114", "H3");
        p("H", "Wednesday", "13:30", "15:30", "EXE", "SN", "Wing C 103", "H1");

        c("H", "Thursday", "08:20", "09:20", "LAM", "KT", "207");
        c("H", "Thursday", "09:20", "10:20", "CHY", "AY", "207");
        p("H", "Thursday", "10:40", "12:40", "FPL", "AG", "Wing B 109", "H2");
        p("H", "Thursday", "10:40", "12:40", "EM", "AJY", "Wing B 005", "H3");
        p("H", "Thursday", "10:40", "12:40", "WS", "MO", "Wing A", "H1");
        special("H", "Thursday", "13:30", "15:30", "LS", "LS-1 / LS-2");

        c("H", "Friday", "08:20", "09:20", "FPL", "AG", "207");
        c("H", "Friday", "09:20", "10:20", "EM", "AJY", "207");
        p("H", "Friday", "10:40", "12:40", "FPL", "AG", "Wing B 109", "H1");
        p("H", "Friday", "10:40", "12:40", "CHY", "AY", "Wing B 114", "H2");
        p("H", "Friday", "10:40", "12:40", "EXE", "SN", "Wing C 103", "H3");
        t("H", "Friday", "13:30", "14:30", "LAM", "KT", "207", "ROLL_36_PLUS");
        libRoll("H", "Friday", "13:30", "14:30", "207", "ROLL_1_35");
        self("H", "Friday", "14:30", "15:30", "COMMON");

        // -----------------------------------------------------------------
        // DIVISION I — official grid, Revision 00.
        // -----------------------------------------------------------------
        c("I", "Monday", "08:20", "09:20", "CS", "AB", "206");
        c("I", "Monday", "09:20", "10:20", "CS", "AB", "206");
        c("I", "Monday", "10:40", "11:40", "LAM", "VS", "206");
        c("I", "Monday", "11:40", "12:40", "FPL", "RP", "206");
        p("I", "Monday", "13:30", "15:30", "FPL", "RP", "Wing B CC109", "I-2");
        p("I", "Monday", "13:30", "15:30", "EG", "ML", "Wing B 005", "I-3");
        p("I", "Monday", "13:30", "15:30", "LIBRARY", "", "Library", "I-1");

        c("I", "Tuesday", "08:20", "09:20", "ELE", "RS", "206");
        c("I", "Tuesday", "09:20", "10:20", "EG", "ML", "206");
        c("I", "Tuesday", "10:40", "11:40", "PHY", "AR", "206");
        t("I", "Tuesday", "11:40", "12:40", "LAM", "VS", "206", "ROLL_1_35");
        special("I", "Tuesday", "13:30", "15:30", "LS", "LS-1 / LS-2");

        p("I", "Wednesday", "08:20", "10:20", "LIBRARY", "", "Library", "I-3");
        p("I", "Wednesday", "08:20", "10:20", "PHY", "AR", "Wing A 019", "I-2");
        p("I", "Wednesday", "08:20", "10:20", "FPL", "RP", "Wing B CC109", "I-1");
        c("I", "Wednesday", "10:40", "11:40", "LAM", "VS", "206");
        c("I", "Wednesday", "11:40", "12:40", "ELE", "RS", "206");
        p("I", "Wednesday", "13:30", "15:30", "FPL", "RP", "Wing B CC109", "I-3");
        p("I", "Wednesday", "13:30", "15:30", "PHY", "AR", "Wing A 019", "I-1");
        p("I", "Wednesday", "13:30", "15:30", "ELE", "RS", "Wing C 101", "I-2");

        self("I", "Thursday", "08:20", "10:20", "COMMON");
        p("I", "Thursday", "10:40", "12:40", "EG", "ML", "Wing B 005", "I-2");
        p("I", "Thursday", "10:40", "12:40", "PHY", "AR", "Wing A 019", "I-3");
        p("I", "Thursday", "10:40", "12:40", "ELE", "RS", "Wing C 101", "I-1");
        c("I", "Thursday", "13:30", "14:30", "PHY", "AR", "201");
        c("I", "Thursday", "14:30", "15:30", "DTI", "MS", "201");

        p("I", "Friday", "08:20", "10:20", "ELE", "RS", "Wing A 206", "I-3");
        p("I", "Friday", "08:20", "10:20", "EG", "ML", "Wing B 005", "I-1");
        p("I", "Friday", "08:20", "10:20", "LIBRARY", "", "Library", "I-2");
        c("I", "Friday", "10:40", "11:40", "FPL", "RP", "206");
        c("I", "Friday", "11:40", "12:40", "LAM", "VS", "206");
        c("I", "Friday", "13:30", "14:30", "EG", "ML", "206");
        t("I", "Friday", "14:30", "15:30", "LAM", "VS", "206", "ROLL_36_PLUS");

        // -----------------------------------------------------------------
        // DIVISION J — official grid, Revision 00.
        // -----------------------------------------------------------------
        c("J", "Monday", "08:20", "09:20", "LAM", "KS", "204");
        c("J", "Monday", "09:20", "10:20", "CHY", "PT", "204");
        p("J", "Monday", "10:40", "12:40", "FPL", "S8", "Wing B 109", "J3");
        p("J", "Monday", "10:40", "12:40", "EM", "AD", "Wing A 003", "J1");
        p("J", "Monday", "10:40", "12:40", "EXE", "NS2", "Wing C 103", "J2");
        c("J", "Monday", "13:30", "14:30", "EXE", "NS2", "203");
        t("J", "Monday", "14:30", "15:30", "LAM", "KS", "203", "ROLL_1_35");
        libRoll("J", "Monday", "14:30", "15:30", "203", "ROLL_36_PLUS");

        c("J", "Tuesday", "08:20", "09:20", "EM", "AD", "207");
        c("J", "Tuesday", "09:20", "10:20", "CS", "BM", "207");
        p("J", "Tuesday", "10:40", "12:40", "FPL", "S8", "Wing B 109", "J1");
        p("J", "Tuesday", "10:40", "12:40", "CHY", "AY", "Wing B 114", "J2");
        p("J", "Tuesday", "10:40", "12:40", "EXE", "NS2", "Wing C 103", "J3");
        t("J", "Tuesday", "13:30", "14:30", "LAM", "KS", "203", "ROLL_36_PLUS");
        libRoll("J", "Tuesday", "13:30", "14:30", "203", "ROLL_1_35");
        self("J", "Tuesday", "14:30", "15:30", "COMMON");

        p("J", "Wednesday", "08:20", "10:20", "FPL", "S8", "Wing B 109", "J2");
        p("J", "Wednesday", "08:20", "10:20", "EM", "AD", "Wing A 003", "J3");
        p("J", "Wednesday", "08:20", "10:20", "WS", "MO", "Wing A", "J1");
        c("J", "Wednesday", "10:40", "11:40", "EXE", "NS2", "209");
        c("J", "Wednesday", "11:40", "12:40", "EM", "AD", "209");
        self("J", "Wednesday", "13:30", "15:30", "COMMON");

        p("J", "Thursday", "08:20", "10:20", "WS", "MO", "Wing A", "J3");
        p("J", "Thursday", "08:20", "10:20", "CHY", "DI", "Wing B 114", "J1");
        p("J", "Thursday", "08:20", "10:20", "EM", "AD", "Wing A 003", "J2");
        c("J", "Thursday", "10:40", "11:40", "FPL", "S8", "205");
        c("J", "Thursday", "11:40", "12:40", "LAM", "KS", "205");
        p("J", "Thursday", "13:30", "15:30", "WS", "MO", "Wing A", "J2");
        p("J", "Thursday", "13:30", "15:30", "CHY", "AY", "Wing B 114", "J3");
        p("J", "Thursday", "13:30", "15:30", "EXE", "NS2", "Wing C 103", "J1");

        c("J", "Friday", "08:20", "09:20", "CS", "BM", "205");
        c("J", "Friday", "09:20", "10:20", "FPL", "S8", "205");
        c("J", "Friday", "10:40", "11:40", "CHY", "PT", "205");
        c("J", "Friday", "11:40", "12:40", "LAM", "KS", "205");
        special("J", "Friday", "13:30", "15:30", "LS", "LS-1 / LS-2");

        // -----------------------------------------------------------------
        // DIVISION K — official grid, Revision 00.
        // -----------------------------------------------------------------
        c("K", "Monday", "08:20", "09:20", "LAM", "SE", "208");
        c("K", "Monday", "09:20", "10:20", "EM", "SR", "208");
        c("K", "Monday", "10:40", "11:40", "FPL", "AG", "208");
        c("K", "Monday", "11:40", "12:40", "CS", "BM", "208");
        p("K", "Monday", "13:30", "15:30", "FPL", "AG", "Wing B 109", "K3");
        p("K", "Monday", "13:30", "15:30", "EM", "SR", "Wing A 003", "K1");
        p("K", "Monday", "13:30", "15:30", "EXE", "SN", "Wing C 103", "K2");

        c("K", "Tuesday", "08:20", "09:20", "FPL", "AG", "203");
        c("K", "Tuesday", "09:20", "10:20", "EXE", "SN", "203");
        p("K", "Tuesday", "10:40", "11:40", "WS", "MO", "Wing A", "K3");
        p("K", "Tuesday", "10:40", "11:40", "CHY", "PT", "Wing A 017", "K1");
        p("K", "Tuesday", "10:40", "11:40", "EM", "SR", "Wing A 003", "K2");
        p("K", "Tuesday", "11:40", "12:40", "FPL", "AG", "Wing B 109", "K1");
        p("K", "Tuesday", "11:40", "12:40", "CHY", "PT", "Wing A 017", "K2");
        p("K", "Tuesday", "11:40", "12:40", "EXE", "SN", "Wing C 103", "K3");
        self("K", "Tuesday", "13:30", "15:30", "COMMON");

        c("K", "Wednesday", "08:20", "09:20", "EXE", "SN", "206");
        c("K", "Wednesday", "09:20", "10:20", "LAM", "SE", "206");
        c("K", "Wednesday", "10:40", "11:40", "LAM", "SE", "208");
        c("K", "Wednesday", "11:40", "12:40", "LAM", "SE", "208");
        special("K", "Wednesday", "13:30", "15:30", "LS", "LS-1 / LS-2");

        c("K", "Thursday", "08:20", "09:20", "CHY", "PT", "208");
        c("K", "Thursday", "09:20", "10:20", "EM", "SR", "208");
        c("K", "Thursday", "10:40", "11:40", "LAM", "SE", "208");
        t("K", "Thursday", "11:40", "12:40", "LAM", "SE", "208", "ROLL_1_35");
        libRoll("K", "Thursday", "11:40", "12:40", "208", "ROLL_36_PLUS");
        p("K", "Thursday", "13:30", "15:30", "FPL", "AG", "Wing B 109", "K2");
        p("K", "Thursday", "13:30", "15:30", "EM", "SR", "Wing A 003", "K3");
        p("K", "Thursday", "13:30", "15:30", "WS", "MO", "Wing A", "K1");

        p("K", "Friday", "08:20", "10:20", "WS", "MO", "Wing A", "K2");
        p("K", "Friday", "08:20", "10:20", "CHY", "AD", "Wing A 017", "K3");
        p("K", "Friday", "08:20", "10:20", "EXE", "SN", "Wing C 103", "K1");
        c("K", "Friday", "10:40", "11:40", "CS", "BM", "208");
        c("K", "Friday", "11:40", "12:40", "CHY", "PT", "208");
        t("K", "Friday", "13:30", "14:30", "LAM", "SE", "203", "ROLL_36_PLUS");
        libRoll("K", "Friday", "13:30", "14:30", "203", "ROLL_1_35");
        self("K", "Friday", "14:30", "15:30", "COMMON");

        // -----------------------------------------------------------------
        // DIVISION L — official grid, Revision 00.
        // -----------------------------------------------------------------
        c("L", "Monday", "08:20", "09:20", "LAM", "VS", "209");
        c("L", "Monday", "09:20", "10:20", "EXE", "PH", "209");
        c("L", "Monday", "10:40", "11:40", "CHY", "PT", "210");
        c("L", "Monday", "11:40", "12:40", "FPL", "YR", "210");
        t("L", "Monday", "13:30", "14:30", "LAM", "VS", "210", "ROLL_1_35");
        libRoll("L", "Monday", "13:30", "14:30", "210", "ROLL_36_PLUS");
        self("L", "Monday", "14:30", "15:30", "COMMON");

        c("L", "Tuesday", "08:20", "09:20", "FPL", "YR", "209");
        c("L", "Tuesday", "09:20", "10:20", "EM", "AD", "209");
        p("L", "Tuesday", "10:40", "12:40", "WS", "MO", "Wing A", "L2");
        self("L", "Tuesday", "10:40", "12:40", "L1");
        self("L", "Tuesday", "10:40", "12:40", "L3");
        p("L", "Tuesday", "13:30", "14:30", "FPL", "YR", "Wing B 109", "L1");
        p("L", "Tuesday", "13:30", "14:30", "CHY", "PT", "Wing A 017", "L2");
        p("L", "Tuesday", "13:30", "14:30", "EM", "AD", "Wing A 003", "L3");
        p("L", "Tuesday", "14:30", "15:30", "FPL", "YR", "Wing B 109", "L2");
        p("L", "Tuesday", "14:30", "15:30", "EXE", "PH", "Wing A 212", "L3");
        p("L", "Tuesday", "14:30", "15:30", "WS", "MO", "Wing A", "L1");

        p("L", "Wednesday", "08:20", "10:20", "SELF", "", "Self Learning", "L2");
        p("L", "Wednesday", "08:20", "10:20", "CHY", "PT", "Wing A 017", "L3");
        p("L", "Wednesday", "08:20", "10:20", "EXE", "PH", "Wing A 215", "L1");
        c("L", "Wednesday", "10:40", "11:40", "CS", "BM", "209");
        t("L", "Wednesday", "11:40", "12:40", "LAM", "VS", "209", "ROLL_36_PLUS");
        libRoll("L", "Wednesday", "11:40", "12:40", "209", "ROLL_1_35");
        p("L", "Wednesday", "13:30", "14:30", "WS", "MO", "Wing A", "L3");
        p("L", "Wednesday", "13:30", "14:30", "CHY", "PT", "Wing A 017", "L1");
        p("L", "Wednesday", "13:30", "14:30", "EM", "AD", "Wing A 003", "L2");

        c("L", "Thursday", "08:20", "09:20", "LAM", "VS", "210");
        c("L", "Thursday", "09:20", "10:20", "CHY", "PT", "210");
        p("L", "Thursday", "10:40", "12:40", "WS", "MO", "Wing A", "L3");
        p("L", "Thursday", "10:40", "12:40", "CHY", "PT", "Wing A 017", "L1");
        p("L", "Thursday", "10:40", "12:40", "EM", "AD", "Wing A 003", "L2");
        special("L", "Thursday", "13:30", "15:30", "LS", "LS-1 / LS-2");

        p("L", "Friday", "08:20", "10:20", "FPL", "YR", "Wing B 109", "L3");
        p("L", "Friday", "08:20", "10:20", "EM", "AD", "Wing A 003", "L1");
        p("L", "Friday", "08:20", "10:20", "EXE", "PH", "Wing A 215", "L2");
        c("L", "Friday", "10:40", "11:40", "EM", "AD", "207");
        c("L", "Friday", "11:40", "12:40", "CS", "BM", "207");
        c("L", "Friday", "13:30", "14:30", "LAM", "VS", "201");
        c("L", "Friday", "14:30", "15:30", "EXE", "PH", "201");

        // -----------------------------------------------------------------
        // DIVISION M — official grid, Revision 00.
        // -----------------------------------------------------------------
        p("M", "Monday", "08:20", "10:20", "WS", "MO", "Wing A", "M3");
        p("M", "Monday", "08:20", "10:20", "CHY", "AD", "Wing A 017", "M1");
        p("M", "Monday", "08:20", "10:20", "EM", "TK", "Wing B 005", "M2");
        c("M", "Monday", "10:40", "11:40", "FPL", "YR", "209");
        c("M", "Monday", "11:40", "12:40", "EXE", "PH", "209");
        c("M", "Monday", "13:30", "14:30", "EM", "TK", "209");
        t("M", "Monday", "14:30", "15:30", "LAM", "SE", "209", "ROLL_1_35");
        libRoll("M", "Monday", "14:30", "15:30", "209", "ROLL_36_PLUS");

        p("M", "Tuesday", "08:20", "10:20", "WS", "MO", "Wing A", "M2");
        p("M", "Tuesday", "08:20", "10:20", "CHY", "AD", "Wing A 017", "M3");
        p("M", "Tuesday", "08:20", "10:20", "EXE", "PH", "Wing A 215", "M1");
        c("M", "Tuesday", "10:40", "11:40", "LAM", "SE", "203");
        c("M", "Tuesday", "11:40", "12:40", "CHY", "AD", "203");
        c("M", "Tuesday", "13:30", "14:30", "CS", "BM", "204");
        c("M", "Tuesday", "14:30", "15:30", "CS", "BM", "204");

        p("M", "Wednesday", "08:20", "10:20", "FPL", "YR", "Wing B 109", "M1");
        self("M", "Wednesday", "08:20", "10:20", "M2");
        p("M", "Wednesday", "08:20", "10:20", "EM", "TK", "Wing B 005", "M3");
        p("M", "Wednesday", "10:40", "12:40", "FPL", "SS", "Wing B 109", "M2");
        p("M", "Wednesday", "10:40", "12:40", "EXE", "SS", "Wing C 101", "M3");
        p("M", "Wednesday", "10:40", "12:40", "WS", "MO", "Wing A", "M1");
        special("M", "Wednesday", "13:30", "15:30", "LS", "LS-1 / LS-2");

        c("M", "Thursday", "08:20", "09:20", "LAM", "SE", "206");
        c("M", "Thursday", "09:20", "10:20", "EXE", "PH", "206");
        c("M", "Thursday", "10:40", "11:40", "FPL", "YR", "209");
        c("M", "Thursday", "11:40", "12:40", "CHY", "AD", "209");
        p("M", "Thursday", "13:30", "14:30", "FPL", "YR", "Wing B 109", "M3");
        p("M", "Thursday", "13:30", "14:30", "CHY", "AD", "Wing A 017", "M2");
        self("M", "Thursday", "13:30", "14:30", "M1");

        c("M", "Friday", "08:20", "09:20", "EM", "TK", "208");
        c("M", "Friday", "09:20", "10:20", "LAM", "SE", "208");
        p("M", "Friday", "10:40", "11:40", "EM", "TK", "Wing B 005", "M1");
        p("M", "Friday", "10:40", "11:40", "EXE", "PH", "Wing A 215", "M2");
        self("M", "Friday", "11:40", "12:40", "M1");
        t("M", "Friday", "13:30", "14:30", "LAM", "SE", "208", "ROLL_36_PLUS");
        libRoll("M", "Friday", "13:30", "14:30", "208", "ROLL_1_35");
        c("M", "Friday", "14:30", "15:30", "LIBRARY", "", "Library");

        // -----------------------------------------------------------------
        // DIVISION N — official grid, Revision 00.
        // -----------------------------------------------------------------
        c("N", "Monday", "08:20", "09:20", "FPL", "NK", "210");
        c("N", "Monday", "09:20", "10:20", "EXE", "SS", "210");
        p("N", "Monday", "10:40", "12:40", "WS", "MO", "Wing A", "N-3");
        p("N", "Monday", "10:40", "12:40", "CHY", "AD", "Wing A 017", "N-1");
        self("N", "Monday", "10:40", "12:40", "N-2");
        t("N", "Monday", "13:30", "14:30", "LAM", "BG", "206", "ROLL_36_PLUS");
        libRoll("N", "Monday", "13:30", "14:30", "206", "ROLL_1_35");
        self("N", "Monday", "14:30", "15:30", "COMMON");

        c("N", "Tuesday", "08:20", "09:20", "EM", "TK", "210");
        c("N", "Tuesday", "09:20", "10:20", "LAM", "SU", "210");
        self("N", "Tuesday", "10:40", "12:40", "N-1");
        p("N", "Tuesday", "10:40", "12:40", "EM", "TK", "Wing B 005", "N-2");
        p("N", "Tuesday", "10:40", "12:40", "EXE", "SS", "Wing A 206", "N-3");
        c("N", "Tuesday", "13:30", "14:30", "CHY", "AD", "209");
        t("N", "Tuesday", "14:30", "15:30", "LAM", "BG", "209", "ROLL_1_35");
        libRoll("N", "Tuesday", "14:30", "15:30", "209", "ROLL_36_PLUS");

        c("N", "Wednesday", "08:20", "09:20", "CHY", "AD", "210");
        c("N", "Wednesday", "09:20", "10:20", "FPL", "NK", "210");
        p("N", "Wednesday", "10:40", "12:40", "FPL", "NK", "Wing B 109", "N-2");
        p("N", "Wednesday", "10:40", "12:40", "EM", "TK", "Wing B 005", "N-3");
        p("N", "Wednesday", "10:40", "12:40", "WS", "MO", "Wing A", "N-1");
        p("N", "Wednesday", "13:30", "14:30", "FPL", "NK", "Wing B 109", "N-1");
        p("N", "Wednesday", "13:30", "14:30", "CHY", "AD", "Wing A 017", "N-2");
        self("N", "Wednesday", "13:30", "14:30", "N-3");

        special("N", "Thursday", "08:20", "10:20", "LS", "LS-1 / LS-2");
        p("N", "Thursday", "10:40", "12:40", "FPL", "NK", "Wing B 109", "N-3");
        p("N", "Thursday", "10:40", "12:40", "EM", "TK", "Wing A 005", "N-1");
        p("N", "Thursday", "10:40", "12:40", "EXE", "PH", "Wing C 103", "N-2");
        c("N", "Thursday", "13:30", "14:30", "LAM", "SU", "205");
        c("N", "Thursday", "14:30", "15:30", "EXE", "SS", "205");

        c("N", "Friday", "08:20", "09:20", "LAM", "SU", "210");
        c("N", "Friday", "09:20", "10:20", "EM", "TK", "210");
        p("N", "Friday", "10:40", "12:40", "WS", "MO", "Wing A", "N-2");
        p("N", "Friday", "10:40", "12:40", "CHY", "AD", "Wing A 017", "N-3");
        p("N", "Friday", "10:40", "12:40", "EXE", "SS", "Wing C 101", "N-1");
        c("N", "Friday", "13:30", "14:30", "CS", "BM", "205");
        c("N", "Friday", "14:30", "15:30", "CS", "BM", "205");

        // -----------------------------------------------------------------
        // DIVISION O — official grid, Revision 00.
        // -----------------------------------------------------------------
        c("O", "Monday", "08:20", "09:20", "LAM", "KT", "207");
        c("O", "Monday", "09:20", "10:20", "EM", "AJY", "207");
        t("O", "Monday", "10:40", "11:40", "LAM", "KT", "B-312", "ROLL_36_PLUS");
        libRoll("O", "Monday", "10:40", "11:40", "B-312", "ROLL_1_35");
        self("O", "Monday", "11:40", "12:40", "COMMON");
        p("O", "Monday", "13:30", "14:30", "FPL", "NK", "Wing B 109", "O-2");
        p("O", "Monday", "13:30", "14:30", "EM", "AJY", "Wing B 005", "O-3");
        p("O", "Monday", "13:30", "14:30", "WS", "MO", "Wing A", "O-1");
        p("O", "Monday", "14:30", "15:30", "WS", "MO", "Wing A", "O-2");
        p("O", "Monday", "14:30", "15:30", "CHY", "DI", "Wing B 114", "O-3");
        p("O", "Monday", "14:30", "15:30", "EXE", "SS", "Wing C 103", "O-1");

        c("O", "Tuesday", "08:20", "09:20", "EXE", "SS", "208");
        c("O", "Tuesday", "09:20", "10:20", "CHY", "DI", "208");
        c("O", "Tuesday", "10:40", "11:40", "FPL", "NK", "208");
        c("O", "Tuesday", "11:40", "12:40", "FPL", "NK", "208");

        c("O", "Wednesday", "08:20", "09:20", "EXE", "SS", "208");
        c("O", "Wednesday", "09:20", "10:20", "EM", "AJY", "208");
        c("O", "Wednesday", "10:40", "11:40", "LAM", "KT", "208");
        c("O", "Wednesday", "11:40", "12:40", "LAM", "KT", "208");
        special("O", "Wednesday", "13:30", "15:30", "LS", "LS-1 / LS-2");

        p("O", "Thursday", "08:20", "10:20", "FPL", "NK", "Wing B 109", "O-3");
        p("O", "Thursday", "08:20", "10:20", "EM", "AJY", "Wing B 005", "O-1");
        p("O", "Thursday", "08:20", "10:20", "EXE", "SS", "Wing C 103", "O-2");
        c("O", "Thursday", "10:40", "11:40", "CS", "BM", "206");
        c("O", "Thursday", "11:40", "12:40", "CS", "BM", "206");
        self("O", "Thursday", "13:30", "15:30", "COMMON");

        p("O", "Friday", "08:20", "10:20", "FPL", "NK", "Wing B 109", "O-1");
        p("O", "Friday", "08:20", "10:20", "CHY", "DI", "Wing B 114", "O-2");
        p("O", "Friday", "08:20", "10:20", "WS", "MO", "Wing A", "O-3");
        t("O", "Friday", "10:40", "11:40", "LAM", "KT", "B-312", "ROLL_1_35");
        libRoll("O", "Friday", "10:40", "11:40", "B-312", "ROLL_36_PLUS");
        c("O", "Friday", "11:40", "12:40", "CHY", "DI", "209");
        p("O", "Friday", "13:30", "15:30", "EXE", "SS", "Wing C 103", "O-3");
        p("O", "Friday", "13:30", "15:30", "CHY", "DI", "Wing B 114", "O-1");
        p("O", "Friday", "13:30", "15:30", "EM", "AJY", "Wing B 005", "O-2");

        // -----------------------------------------------------------------
        // DIVISION P — official grid, Revision 00.
        // -----------------------------------------------------------------
        p("P", "Monday", "08:20", "10:20", "WS", "MO", "Wing A", "P3");
        p("P", "Monday", "08:20", "10:20", "CHY", "DI", "Wing B 114", "P1");
        p("P", "Monday", "08:20", "10:20", "EM", "ML", "Wing A 003", "P2");
        c("P", "Monday", "10:40", "11:40", "CS", "BM", "207");
        c("P", "Monday", "11:40", "12:40", "LAM", "KT", "207");
        self("P", "Monday", "13:30", "15:30", "COMMON");

        p("P", "Tuesday", "08:20", "10:20", "FPL", "S8", "Wing B 109", "P3");
        p("P", "Tuesday", "08:20", "10:20", "EM", "SR", "Wing A 003", "P1");
        p("P", "Tuesday", "08:20", "10:20", "EXE", "NS2", "Wing A 212", "P2");
        c("P", "Tuesday", "10:40", "11:40", "EM", "ML", "210");
        c("P", "Tuesday", "11:40", "12:40", "CHY", "DI", "210");
        c("P", "Tuesday", "13:30", "14:30", "EXE", "NS2", "202");
        t("P", "Tuesday", "14:30", "15:30", "LAM", "KT", "202", "ROLL_1_35");
        libRoll("P", "Tuesday", "14:30", "15:30", "202", "ROLL_36_PLUS");

        p("P", "Wednesday", "08:20", "10:20", "WS", "MO", "Wing A", "P2");
        p("P", "Wednesday", "08:20", "10:20", "CHY", "DI", "Wing B 114", "P3");
        p("P", "Wednesday", "08:20", "10:20", "EXE", "NS2", "Wing A 212", "P1");
        p("P", "Wednesday", "10:40", "12:40", "FPL", "S8", "Wing B 109", "P1");
        p("P", "Wednesday", "10:40", "12:40", "CHY", "DI", "Wing B 114", "P2");
        p("P", "Wednesday", "10:40", "12:40", "EM", "SR", "Wing A 003", "P3");
        c("P", "Wednesday", "13:30", "14:30", "EM", "ML", "202");
        t("P", "Wednesday", "14:30", "15:30", "LAM", "KT", "202", "ROLL_36_PLUS");
        libRoll("P", "Wednesday", "14:30", "15:30", "202", "ROLL_1_35");

        c("P", "Thursday", "08:20", "09:20", "FPL", "S8", "209");
        c("P", "Thursday", "09:20", "10:20", "CS", "BM", "209");
        c("P", "Thursday", "10:40", "11:40", "LAM", "KT", "207");
        c("P", "Thursday", "11:40", "12:40", "CHY", "DI", "207");
        special("P", "Thursday", "13:30", "15:30", "LS", "LS-1 / LS-2");

        c("P", "Friday", "08:20", "09:20", "EXE", "NS2", "209");
        c("P", "Friday", "09:20", "10:20", "LAM", "KT", "209");
        c("P", "Friday", "10:40", "11:40", "FPL", "S8", "209");
        self("P", "Friday", "11:40", "12:40", "COMMON");
        p("P", "Friday", "13:30", "15:30", "FPL", "S8", "Wing B 109", "P2");
        p("P", "Friday", "13:30", "15:30", "EXE", "NS2", "Wing C 101", "P3");
        p("P", "Friday", "13:30", "15:30", "WS", "MO", "Wing A", "P1");

        addDailyBreaks();
        freezeAll();
    }

    private FirstYearTimetableData() {}

    private static void defineDivision(String division, String[] batches, int maxRoll) {
        BATCHES.put(division, batches);
        if (maxRoll > 0) MAX_ROLL.put(division, maxRoll);
    }

    private static void defineStaff() {
        staff("A", "LAM", "PK", "Prof. Premkumar Kolle");
        staff("A", "PHY", "RJ", "Prof. Rupali Jagnade");
        staff("A", "ELE", "ASB", "Prof. Arti S. Bindu");
        staff("A", "EG", "SSD", "Prof. Santosh Dabhole");
        staff("A", "FPL", "SS", "Prof. Sanket Sontakke");
        staff("A", "DTI", "MS", "Prof. Mujiaid Shaikh");
        staff("A", "CS", "AB", "Prof. Amol Bade");

        staff("C", "LAM", "SU", "Prof. Shankarrao Ugale");
        staff("C", "PHY", "MG", "Prof. Mahesh Godse");
        staff("C", "ELE", "JG", "Prof. Joyti Gore");
        staff("C", "EG", "SG", "Prof. Sandip Gore");
        staff("C", "EG", "ML", "Prof. Milind Landge");
        staff("C", "FPL", "TG", "Prof. Tejal Girase");
        staff("C", "DTI", "MS", "Prof. Mujiaid Shaikh");
        staff("C", "CS", "AB", "Prof. Amol Bade");

        staff("D", "LAM", "SU", "Prof. Shankarrao Ugale");
        staff("D", "PHY", "MG", "Prof. Mahesh Godse");
        staff("D", "ELE", "JG", "Prof. Joyti Gore");
        staff("D", "EG", "SD", "Prof. Sagar Deshpande");
        staff("D", "FPL", "TG", "Prof. Tejal Girase");
        staff("D", "DTI", "MS", "Prof. Mujiaid Shaikh");
        staff("D", "CS", "AB", "Prof. Amol Bade");

        staff("E", "LAM", "BG", "Prof. Bhimrao Gaikwad");
        staff("E", "PHY", "MJ", "Prof. Manoj Junnarkar");
        staff("E", "ELE", "SS", "Prof. Sahuraj Sabale");
        staff("E", "EG", "SD", "Prof. Sagar Deshpande");
        staff("E", "FPL", "AB", "Prof. Akshay Bhosale");
        staff("E", "DTI", "MS", "Prof. Mujiaid Shaikh");
        staff("E", "CS", "BM", "Prof. Baldev Machave");

        staff("F", "LAM", "BG", "Prof. Bhimrao Gaikwad");
        staff("F", "PHY", "MJ", "Prof. Manoj Junnarkar");
        staff("F", "ELE", "SS", "Prof. Sahuraj Sabale");
        staff("F", "EG", "SG", "Prof. Sandip Gore");
        staff("F", "FPL", "AB", "Prof. Akshay Bhosale");
        staff("F", "DTI", "MS", "Prof. Mujiaid Shaikh");
        staff("F", "CS", "AB", "Prof. Amol Bade");

        staff("G", "LAM", "VS", "Prof. Vallabh Shinde");
        staff("G", "PHY", "AR", "Prof. Anil Rakshe");
        staff("G", "ELE", "RS", "Prof. Rasmi Singh");
        staff("G", "EG", "SG", "Prof. Sandip Gore");
        staff("G", "FPL", "RP", "Prof. Ramkrishna Patil");
        staff("G", "DTI", "MS", "Prof. Mujiaid Shaikh");
        staff("G", "CS", "AB", "Prof. Amol Bade");
        staff("G", "WS", "MO", "Dr. Milind Ovhal");

        staff("H", "LAM", "KT", "Prof. Kirti Tikale");
        staff("H", "CHY", "AY", "Dr. Archana Yewale");
        staff("H", "EXE", "SN", "Prof. Swati Narule");
        staff("H", "EM", "AJY", "Prof. Anil J. Yadav");
        staff("H", "FPL", "AG", "Prof. Ankita Gadekar");
        staff("H", "WS", "MO", "Dr. Milind Ovhal");
        staff("H", "CS", "BM", "Prof. Baldev Machave");

        staff("I", "LAM", "VS", "Prof. Vallabh Shinde");
        staff("I", "PHY", "AR", "Prof. Anil Rakshe");
        staff("I", "ELE", "RS", "Prof. Rasmi Singh");
        staff("I", "EG", "ML", "Prof. Milind Landge");
        staff("I", "FPL", "RP", "Prof. Ramkrishna Patil");
        staff("I", "DTI", "MS", "Prof. Mujiaid Shaikh");
        staff("I", "CS", "AB", "Prof. Amol Bade");

        staff("J", "LAM", "KS", "Dr. Kavita Shinde");
        staff("J", "CHY", "PT", "Prof. Pratiksha Tanpure");
        staff("J", "CHY", "AY", "Dr. Archana Yewale");
        staff("J", "CHY", "DI", "Prof. Damayanti Ingale");
        staff("J", "EXE", "NS2", "Faculty initials NS2 (as printed)");
        staff("J", "EM", "AD", "Dr. Aniruddh Dubal");
        staff("J", "FPL", "S8", "Faculty initials S8 (as printed)");
        staff("J", "WS", "MO", "Dr. Milind Ovhal");
        staff("J", "CS", "BM", "Prof. Baldev Machave");

        staff("K", "LAM", "SE", "Prof. Shital Ekhande");
        staff("K", "CHY", "PT", "Prof. Pratiksha Tanpure");
        staff("K", "CHY", "AD", "Dr. Anand Daultabad");
        staff("K", "EXE", "SN", "Prof. Swati Narule");
        staff("K", "EM", "SR", "Dr. Shekhar Rahane");
        staff("K", "FPL", "AG", "Prof. Ankita Gadekar");
        staff("K", "WS", "MO", "Dr. Milind Ovhal");
        staff("K", "CS", "BM", "Prof. Baldev Machave");

        staff("L", "LAM", "VS", "Prof. Vallabh Shinde");
        staff("L", "CHY", "PT", "Prof. Pratiksha Tanpure");
        staff("L", "EXE", "PH", "Prof. Priyanka Hiremath");
        staff("L", "EM", "AD", "Dr. Aniruddh Dubal");
        staff("L", "FPL", "YR", "Prof. Yogita Rode");
        staff("L", "WS", "MO", "Dr. Milind Ovhal");
        staff("L", "CS", "BM", "Prof. Baldev Machave");

        staff("M", "LAM", "SE", "Prof. Shital Ekhande");
        staff("M", "CHY", "AD", "Dr. Anand Daultabad");
        staff("M", "EXE", "PH", "Prof. Priyanka Hiremath");
        staff("M", "EM", "TK", "Dr. Tejaswita Kajale");
        staff("M", "FPL", "YR", "Prof. Yogita Rode");
        staff("M", "FPL", "SS", "Prof. Sanket Sontakke (as printed in cell)");
        staff("M", "WS", "MO", "Dr. Milind Ovhal");
        staff("M", "CS", "BM", "Prof. Baldev Machave");

        staff("N", "LAM", "SU", "Prof. Shankarrao Ugale");
        staff("N", "CHY", "AD", "Dr. Anand Daultabad");
        staff("N", "EXE", "SS", "Prof. Sachinkumar Shahare");
        staff("N", "EXE", "PH", "Prof. Priyanka Hiremath");
        staff("N", "EM", "TK", "Dr. Tejaswita Kajale");
        staff("N", "FPL", "NK", "Prof. Neha Kumari");
        staff("N", "WS", "MO", "Dr. Milind Ovhal");
        staff("N", "CS", "BM", "Prof. Amol Bade");

        staff("O", "LAM", "KT", "Prof. Kirti Takale");
        staff("O", "CHY", "DI", "Prof. Damayanti Ingale");
        staff("O", "EXE", "SS", "Prof. Sachinkumar Shahare");
        staff("O", "EM", "AJY", "Prof. Anil J. Yadav");
        staff("O", "FPL", "NK", "Prof. Neha Kumari");
        staff("O", "WS", "MO", "Dr. Milind Ovhal");
        staff("O", "CS", "BM", "Prof. Baldev Machave");

        staff("P", "LAM", "KT", "Prof. Kirti Takale");
        staff("P", "CHY", "DI", "Prof. Damayanti Ingale");
        staff("P", "EXE", "NS2", "Faculty initials NS2 (as printed)");
        staff("P", "EM", "ML", "Prof. Milind Landge");
        staff("P", "EM", "SR", "Dr. Shekhar Rahane");
        staff("P", "FPL", "S8", "Faculty initials S8 (as printed)");
        staff("P", "WS", "MO", "Dr. Milind Ovhal");
        staff("P", "CS", "BM", "Prof. Baldev Machave");
    }

    private static void staff(String division, String subject, String abbreviation, String name) {
        STAFF.put(staffKey(division, subject, abbreviation), name);
    }

    private static String staffKey(String division, String subject, String abbreviation) {
        return division + "|" + subject + "|" + abbreviation;
    }

    private static void add(String division, String day, String start, String end,
                            String code, String abbreviation, String room,
                            String audience, TimetableData.Kind kind) {
        String subject = SUBJECTS.containsKey(code) ? SUBJECTS.get(code) : code;
        String teacher = abbreviation == null || abbreviation.isEmpty() ? ""
                : STAFF.getOrDefault(staffKey(division, code, abbreviation),
                        "Faculty abbreviation " + abbreviation + " (as printed)");
        String batch = "COMMON".equals(audience) ? ""
                : "ROLL_1_35".equals(audience) ? "Roll 1–35"
                : "ROLL_36_PLUS".equals(audience) ? "Roll 36 onwards" : audience;
        String id = division.toLowerCase(Locale.ENGLISH) + "-"
                + day.substring(0, 3).toLowerCase(Locale.ENGLISH) + "-"
                + (++nextId);
        List<TimetableData.ClassItem> rows = ALL.computeIfAbsent(
                division + "|" + day, ignored -> new ArrayList<>());
        rows.add(new TimetableData.ClassItem(
                id, start, end, subject, code, teacher,
                room == null ? "" : room, batch, audience, kind
        ));
    }

    private static void c(String d, String day, String start, String end,
                          String code, String abbr, String room) {
        add(d, day, start, end, code, abbr, room, "COMMON", TimetableData.Kind.CLASS);
    }

    private static void p(String d, String day, String start, String end,
                          String code, String abbr, String room, String batch) {
        TimetableData.Kind kind = "LIBRARY".equals(code)
                ? TimetableData.Kind.LIBRARY
                : "SELF".equals(code) ? TimetableData.Kind.SELF_LEARNING
                : TimetableData.Kind.PRACTICAL;
        add(d, day, start, end, code, abbr, room, batch, kind);
    }

    private static void t(String d, String day, String start, String end,
                          String code, String abbr, String room, String audience) {
        add(d, day, start, end, code, abbr, room, audience, TimetableData.Kind.TUTORIAL);
    }

    private static void libRoll(String d, String day, String start, String end,
                                String room, String audience) {
        add(d, day, start, end, "LIBRARY", "", room, audience, TimetableData.Kind.LIBRARY);
    }

    private static void self(String d, String day, String start, String end, String audience) {
        add(d, day, start, end, "SELF", "", "Self Learning", audience,
                TimetableData.Kind.SELF_LEARNING);
    }

    private static void special(String d, String day, String start, String end,
                                String code, String label) {
        add(d, day, start, end, code, "", label, "COMMON", TimetableData.Kind.SPECIAL);
    }

    private static void addDailyBreaks() {
        for (String division : DIVISIONS) {
            for (String day : TimetableData.DAYS) {
                add(division, day, "10:20", "10:40", "Short Break", "", "Campus",
                        "COMMON", TimetableData.Kind.SHORT_BREAK);
                add(division, day, "12:40", "13:30", "Lunch Break", "", "Cafeteria",
                        "COMMON", TimetableData.Kind.LUNCH);
            }
        }
    }

    private static void freezeAll() {
        Comparator<TimetableData.ClassItem> byTime = Comparator
                .comparing((TimetableData.ClassItem item) -> item.start)
                .thenComparing(item -> item.end)
                .thenComparing(item -> item.id);
        for (Map.Entry<String, List<TimetableData.ClassItem>> entry : ALL.entrySet()) {
            entry.getValue().sort(byTime);
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        }
    }

    public static String batchForRoll(String division, int rollNumber) {
        String key = normalizeDivision(division);
        String[] batches = BATCHES.get(key);
        if (batches == null || rollNumber < 1) return "";
        int max = MAX_ROLL.getOrDefault(key, Integer.MAX_VALUE);
        if (rollNumber > max) return "";
        if (rollNumber <= 25) return batches[0];
        if (rollNumber <= 50) return batches[1];
        return batches[2];
    }

    public static String rollRangeForBatch(String division, String batch) {
        String[] batches = BATCHES.get(normalizeDivision(division));
        if (batches == null || batch == null) return "";
        if (batch.equals(batches[0])) return "Roll 1–25";
        if (batch.equals(batches[1])) return "Roll 26–50";
        if (batch.equals(batches[2])) {
            return MAX_ROLL.containsKey(normalizeDivision(division))
                    ? "Roll 51–" + MAX_ROLL.get(normalizeDivision(division))
                    : "Roll 51 onwards";
        }
        return "";
    }

    public static List<TimetableData.ClassItem> forRollAndDay(
            String division, int rollNumber, String day) {
        String keyDivision = normalizeDivision(division);
        if (!BATCHES.containsKey(keyDivision) || day == null) return Collections.emptyList();
        String cacheKey = keyDivision + "|" + rollNumber + "|" + day;
        synchronized (CACHE) {
            List<TimetableData.ClassItem> cached = CACHE.get(cacheKey);
            if (cached != null) return cached;
        }

        String batch = batchForRoll(keyDivision, rollNumber);
        boolean validRoll = !batch.isEmpty();
        List<TimetableData.ClassItem> source = ALL.getOrDefault(
                keyDivision + "|" + day, Collections.emptyList());
        List<TimetableData.ClassItem> filtered = new ArrayList<>();
        for (TimetableData.ClassItem item : source) {
            if ("COMMON".equals(item.audience)
                    || (validRoll && item.audience.equals(batch))
                    || (validRoll && "ROLL_1_35".equals(item.audience)
                            && rollNumber >= 1 && rollNumber <= 35)
                    || (validRoll && "ROLL_36_PLUS".equals(item.audience)
                            && rollNumber >= 36)) {
                filtered.add(item);
            }
        }
        List<TimetableData.ClassItem> result = Collections.unmodifiableList(filtered);
        synchronized (CACHE) {
            List<TimetableData.ClassItem> existing = CACHE.get(cacheKey);
            if (existing != null) return existing;
            CACHE.put(cacheKey, result);
        }
        return result;
    }

    public static String[] divisions() {
        return new String[]{"A", "B", "C", "D", "E", "F", "G", "H",
                "I", "J", "K", "L", "M", "N", "O", "P"};
    }

    public static int maximumRoll(String division) {
        String normalized = normalizeDivision(division);
        return MAX_ROLL.getOrDefault(normalized, Integer.MAX_VALUE);
    }

    public static boolean supportsDivision(String division) {
        return BATCHES.containsKey(normalizeDivision(division)) || "B".equals(normalizeDivision(division));
    }

    private static String normalizeDivision(String division) {
        return division == null ? "" : division.trim().toUpperCase(Locale.ENGLISH);
    }
}
