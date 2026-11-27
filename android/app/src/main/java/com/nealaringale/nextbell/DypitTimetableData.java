package com.nealaringale.nextbell;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Date-scoped timetable transcribed from the D. Y. Patil Institute of Technology,
 * Pimpri, FY Engineering Sem-I timetable supplied for 1–8 December 2026.
 * The source contains five published dates; it is intentionally not repeated weekly.
 */
public final class DypitTimetableData {
    private static final String[] DIVISIONS = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
            "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T"
    };
    private static final String[] SOURCE_ROWS = {
            "2026-12-01|A|A 704|PHY-RJ,EM-SSD,FPL-VZP,BXE-PHK,MATH-BS",
            "2026-12-01|B|B 711|MATH-ABB,FPL-DD,EM-NNS,BXE-NUK,PHY-VD",
            "2026-12-01|C|A 604|MATH-DK,EM-NY,PHY-SAK,BXE-SMD,FPL-PMP",
            "2026-12-01|D|B 212|MATH-HP,FPL-RB,PHY-GK,BXE-MMK,EM-SMB",
            "2026-12-01|E|B 309|BXE-GGB,MATH-RY,EM-RP,FPL-DD,PHY-SSB",
            "2026-12-01|F|B 311|BXE-PHK,FPL-PMP,MATH-BS,PHY-SSB,EM-SSW",
            "2026-12-01|G|B 609|FPL-BVK,EM-NNS,BXE-NUK,MATH-ABB,PHY-GK",
            "2026-12-01|H|B 611|PHY-SAK,FPL-BVK,MATH-DK,EM-NY,BXE-SMD",
            "2026-12-01|I|B 612|MATH-RY,FPL-VZP,PHY-RJ,EM-SMB,BXE-MMK",
            "2026-12-01|J|B 112|EM-RP,PHY-VD,FPL-RB,BXE-GGB,MATH-HP",
            "2026-12-01|K|B 201|MATH-SNS,CHEM-KAG,BEE-GHP,FPL-SB,EG-SH",
            "2026-12-01|L|B 202|FPL-SSM,BEE-GHP,EG-PSK,MATH-PY,CHEM-KAG",
            "2026-12-01|M|A 603|EG-ASK,MATH-PY,CHEM-AP,FPL-SRP,BEE-JMK",
            "2026-12-01|N|B 509|MATH-DS,BEE-JMK,FPL-SSM,EG-MCI,CHEM-SSS",
            "2026-12-01|O|B 511|MATH-AKM,EG-MCI,BEE-VM,CHEM-BN,FPL-KK",
            "2026-12-01|P|B 512|FPL-SPV,EG-SVD,MATH-RN,BEE-PM,CHEM-AJ",
            "2026-12-01|Q|B 702|CHEM-AP,FPL-SB,MATH-SNS,BEE-ASP,EG-KGM",
            "2026-12-01|R|B 709|CHEM-BN,MATH-RN,BEE-ASP,FPL-SPV,EG-PPP",
            "2026-12-01|S|B 706|BEE-PM,CHEM-AJ,EG-SW,MATH-DS,FPL-SRP",
            "2026-12-01|T|B 712|BEE-VM,FPL-KK,EG-PPP,CHEM-SSS,MATH-AKM",
            "2026-12-03|A|A 704|FPL-VZP,BXE-PHK,MATH-BS,PHY-RJ,EM-SSD",
            "2026-12-03|B|B 711|EM-NNS,BXE-NUK,PHY-VD,MATH-ABB,FPL-DD",
            "2026-12-03|C|A 604|PHY-SAK,BXE-SMD,FPL-PMP,MATH-DK,EM-NY",
            "2026-12-03|D|B 212|PHY-GK,BXE-MMK,EM-SMB,MATH-HP,FPL-RB",
            "2026-12-03|E|B 309|EM-RP,FPL-DD,PHY-SSB,BXE-GGB,MATH-RY",
            "2026-12-03|F|B 311|MATH-BS,PHY-SSB,EM-SSW,BXE-PHK,FPL-PMP",
            "2026-12-03|G|B 609|BXE-NUK,MATH-ABB,PHY-GK,FPL-BVK,EM-NNS",
            "2026-12-03|H|B 611|MATH-DK,EM-NY,BXE-SMD,PHY-SAK,FPL-BVK",
            "2026-12-03|I|B 612|PHY-RJ,EM-SMB,BXE-MMK,MATH-RY,FPL-VZP",
            "2026-12-03|J|B 112|BXE-GGB,FPL-RB,MATH-HP,EM-RP,PHY-VD",
            "2026-12-03|K|B 201|BEE-GHP,FPL-SB,EG-SH,MATH-SNS,CHEM-KAG",
            "2026-12-03|L|B 202|EG-PSK,MATH-PY,CHEM-KAG,FPL-SSM,BEE-GHP",
            "2026-12-03|M|A 603|CHEM-AP,FPL-SRP,BEE-JMK,EG-ASK,MATH-PY",
            "2026-12-03|N|B 509|FPL-SSM,EG-MCI,CHEM-SSS,MATH-DS,BEE-JMK",
            "2026-12-03|O|B 511|BEE-VM,CHEM-BN,FPL-KK,MATH-AKM,EG-MCI",
            "2026-12-03|P|B 512|MATH-RN,BEE-PM,CHEM-AJ,FPL-SPV,EG-SVD",
            "2026-12-03|Q|B 702|MATH-SNS,BEE-ASP,EG-KGM,CHEM-AP,FPL-SB",
            "2026-12-03|R|B 709|BEE-ASP,FPL-SPV,EG-PPP,CHEM-BN,MATH-RN",
            "2026-12-03|S|B 706|EG-SW,MATH-DS,FPL-SRP,BEE-PM,CHEM-AJ",
            "2026-12-03|T|B 712|EG-PPP,CHEM-SSS,MATH-AKM,BEE-VM,FPL-KK",
            "2026-12-05|A|A 704|MATH-BS,PHY-RJ,EM-SSD,FPL-VZP,BXE-PHK",
            "2026-12-05|B|B 711|PHY-VD,MATH-ABB,FPL-DD,EM-NNS,BXE-NUK",
            "2026-12-05|C|A 604|FPL-PMP,MATH-DK,EM-NY,PHY-SAK,BXE-SMD",
            "2026-12-05|D|B 212|EM-SMB,MATH-HP,FPL-RB,PHY-GK,BXE-MMK",
            "2026-12-05|E|B 309|PHY-SSB,BXE-GGB,MATH-RY,EM-RP,FPL-DD",
            "2026-12-05|F|B 311|EM-SSW,BXE-PHK,FPL-PMP,MATH-BS,PHY-SSB",
            "2026-12-05|G|B 609|PHY-GK,FPL-BVK,EM-NNS,BXE-NUK,MATH-ABB",
            "2026-12-05|H|B 611|BXE-SMD,PHY-SAK,FPL-BVK,MATH-DK,EM-NY",
            "2026-12-05|I|B 612|BXE-MMK,MATH-RY,FPL-VZP,PHY-RJ,EM-SMB",
            "2026-12-05|J|B 112|MATH-HP,EM-RP,PHY-VD,BXE-GGB,FPL-RB",
            "2026-12-05|K|B 201|EG-SH,MATH-SNS,CHEM-KAG,BEE-GHP,FPL-SB",
            "2026-12-05|L|B 202|CHEM-KAG,FPL-SSM,BEE-GHP,EG-PSK,MATH-PY",
            "2026-12-05|M|A 603|BEE-JMK,EG-ASK,MATH-PY,CHEM-AP,FPL-SRP",
            "2026-12-05|N|B 509|CHEM-SSS,MATH-DS,BEE-JMK,FPL-SSM,EG-MCI",
            "2026-12-05|O|B 511|FPL-KK,MATH-AKM,EG-MCI,BEE-VM,CHEM-BN",
            "2026-12-05|P|B 512|CHEM-AJ,FPL-SPV,EG-SVD,MATH-RN,BEE-PM",
            "2026-12-05|Q|B 702|EG-KGM,BEE-ASP,FPL-SB,MATH-SNS,CHEM-AP",
            "2026-12-05|R|B 709|EG-PPP,CHEM-BN,MATH-RN,BEE-ASP,FPL-SPV",
            "2026-12-05|S|B 706|FPL-SRP,BEE-PM,CHEM-AJ,EG-SW,MATH-DS",
            "2026-12-05|T|B 712|MATH-AKM,BEE-VM,FPL-KK,EG-PPP,CHEM-SSS",
            "2026-12-06|A|A 704|FPL-VZP,BXE-PHK,MATH-BS,EM-SSD,PHY-RJ",
            "2026-12-06|B|B 711|EM-NNS,BXE-NUK,PHY-VD,FPL-DD,MATH-ABB",
            "2026-12-06|C|A 604|PHY-SAK,BXE-SMD,FPL-PMP,EM-NY,MATH-DK",
            "2026-12-06|D|B 212|PHY-GK,BXE-MMK,EM-SMB,FPL-RB,MATH-HP",
            "2026-12-06|E|B 309|EM-RP,FPL-DD,PHY-SSB,MATH-RY,BXE-GGB",
            "2026-12-06|F|B 311|MATH-BS,PHY-SSB,EM-SSW,FPL-PMP,BXE-PHK",
            "2026-12-06|G|B 609|BXE-NUK,MATH-ABB,PHY-GK,EM-NNS,FPL-BVK",
            "2026-12-06|H|B 611|MATH-DK,EM-NY,BXE-SMD,FPL-BVK,PHY-SAK",
            "2026-12-06|I|B 612|PHY-RJ,EM-SMB,BXE-MMK,FPL-VZP,MATH-RY",
            "2026-12-06|J|B 112|BXE-GGB,FPL-RB,MATH-HP,PHY-VD,EM-RP",
            "2026-12-06|K|B 201|BEE-GHP,FPL-SB,EG-SH,CHEM-KAG,MATH-SNS",
            "2026-12-06|L|B 202|EG-PSK,MATH-PY,CHEM-KAG,BEE-GHP,FPL-SSM",
            "2026-12-06|M|A 603|CHEM-AP,FPL-SRP,BEE-JMK,MATH-PY,EG-ASK",
            "2026-12-06|N|B 509|FPL-SSM,EG-MCI,CHEM-SSS,BEE-JMK,MATH-DS",
            "2026-12-06|O|B 511|BEE-VM,CHEM-BN,FPL-KK,EG-MCI,MATH-AKM",
            "2026-12-06|P|B 512|MATH-RN,BEE-PM,CHEM-AJ,EG-SVD,FPL-SPV",
            "2026-12-06|Q|B 702|MATH-SNS,CHEM-AP,EG-KGM,FPL-SB,BEE-ASP",
            "2026-12-06|R|B 709|BEE-ASP,FPL-SPV,EG-PPP,MATH-RN,CHEM-BN",
            "2026-12-06|S|B 706|EG-SW,MATH-DS,FPL-SRP,CHEM-AJ,BEE-PM",
            "2026-12-06|T|B 712|EG-PPP,CHEM-SSS,MATH-AKM,FPL-KK,BEE-VM",
            "2026-12-08|A|A 704|PHY-RJ,EM-SSD,FPL-VZP,BXE-PHK,MATH-BS",
            "2026-12-08|B|B 711|MATH-ABB,FPL-DD,EM-NNS,BXE-NUK,PHY-VD",
            "2026-12-08|C|A 604|MATH-DK,EM-NY,PHY-SAK,BXE-SMD,FPL-PMP",
            "2026-12-08|D|B 212|MATH-HP,FPL-RB,PHY-GK,BXE-MMK,EM-SMB",
            "2026-12-08|E|B 309|BXE-GGB,MATH-RY,EM-RP,FPL-DD,PHY-SSB",
            "2026-12-08|F|B 311|BXE-PHK,FPL-PMP,MATH-BS,PHY-SSB,EM-SSW",
            "2026-12-08|G|B 609|FPL-BVK,EM-NNS,BXE-NUK,MATH-ABB,PHY-GK",
            "2026-12-08|H|B 611|PHY-SAK,FPL-BVK,MATH-DK,EM-NY,BXE-SMD",
            "2026-12-08|I|B 612|MATH-RY,FPL-VZP,PHY-RJ,EM-SMB,BXE-MMK",
            "2026-12-08|J|B 112|EM-RP,PHY-VD,FPL-RB,BXE-GGB,MATH-HP",
            "2026-12-08|K|B 201|MATH-SNS,CHEM-KAG,BEE-GHP,FPL-SB,EG-SH",
            "2026-12-08|L|B 202|FPL-SSM,BEE-GHP,EG-PSK,MATH-PY,CHEM-KAG",
            "2026-12-08|M|A 603|EG-ASK,MATH-PY,CHEM-AP,FPL-SRP,BEE-JMK",
            "2026-12-08|N|B 509|MATH-DS,BEE-JMK,FPL-SSM,EG-MCI,CHEM-SSS",
            "2026-12-08|O|B 511|MATH-AKM,EG-MCI,BEE-VM,CHEM-BN,FPL-KK",
            "2026-12-08|P|B 512|FPL-SPV,EG-SVD,MATH-RN,BEE-PM,CHEM-AJ",
            "2026-12-08|Q|B 702|CHEM-AP,FPL-SB,MATH-SNS,BEE-ASP,EG-KGM",
            "2026-12-08|R|B 709|CHEM-BN,MATH-RN,BEE-ASP,FPL-SPV,EG-PPP",
            "2026-12-08|S|B 706|BEE-PM,CHEM-AJ,EG-SW,MATH-DS,FPL-SRP",
            "2026-12-08|T|B 712|BEE-VM,FPL-KK,EG-PPP,CHEM-SSS,MATH-AKM",
    };

    private static final Map<LocalDate, Map<String, List<TimetableData.ClassItem>>> SCHEDULES =
            buildSchedules();

    private DypitTimetableData() {}

    private static Map<LocalDate, Map<String, List<TimetableData.ClassItem>>> buildSchedules() {
        Map<LocalDate, Map<String, List<TimetableData.ClassItem>>> result = new TreeMap<>();
        for (String row : SOURCE_ROWS) {
            String[] parts = row.split("\\|", 4);
            if (parts.length != 4) throw new IllegalStateException("Invalid DYPIT timetable row: " + row);
            LocalDate date = LocalDate.parse(parts[0]);
            String division = parts[1];
            String room = parts[2];
            String[] codes = parts[3].split(",");
            if (codes.length != 5 || !Arrays.asList(DIVISIONS).contains(division)) {
                throw new IllegalStateException("Invalid DYPIT division timetable: " + row);
            }

            List<TimetableData.ClassItem> classes = new ArrayList<>();
            boolean sunday = date.getDayOfWeek() == DayOfWeek.SUNDAY;
            String[] starts = sunday
                    ? new String[]{"09:00", "10:00", "11:30", "12:30", "14:30"}
                    : new String[]{"09:00", "09:45", "11:00", "11:45", "12:30"};
            String[] ends = sunday
                    ? new String[]{"10:00", "11:00", "12:30", "13:30", "15:30"}
                    : new String[]{"09:45", "10:30", "11:45", "12:30", "13:15"};

            for (int i = 0; i < codes.length; i++) {
                String code = codes[i].trim();
                int separator = code.indexOf('-');
                String subjectCode = separator > 0 ? code.substring(0, separator) : code;
                String teacherCode = separator > 0 ? code.substring(separator + 1) : "";
                String id = "dypit-" + date.format(DateTimeFormatter.BASIC_ISO_DATE)
                        + "-" + division.toLowerCase(Locale.ENGLISH) + "-p" + (i + 1);
                classes.add(new TimetableData.ClassItem(
                        id, starts[i], ends[i], subjectName(subjectCode), subjectCode,
                        teacherCode, room, "", "COMMON", TimetableData.Kind.CLASS
                ));

                if (i == 1) {
                    classes.add(new TimetableData.ClassItem(
                            "dypit-" + date.format(DateTimeFormatter.BASIC_ISO_DATE)
                                    + "-" + division.toLowerCase(Locale.ENGLISH) + "-short-break",
                            sunday ? "11:00" : "10:30",
                            sunday ? "11:30" : "11:00",
                            "Short Break", "", "", "", "", "COMMON",
                            TimetableData.Kind.SHORT_BREAK
                    ));
                }
                if (sunday && i == 3) {
                    classes.add(new TimetableData.ClassItem(
                            "dypit-" + date.format(DateTimeFormatter.BASIC_ISO_DATE)
                                    + "-" + division.toLowerCase(Locale.ENGLISH) + "-lunch",
                            "13:30", "14:30", "Lunch Break", "", "", "", "",
                            "COMMON", TimetableData.Kind.LUNCH
                    ));
                }
            }

            classes.sort(Comparator.comparing(item -> item.start));
            Map<String, List<TimetableData.ClassItem>> byDivision =
                    result.computeIfAbsent(date, ignored -> new LinkedHashMap<>());
            if (byDivision.put(division, Collections.unmodifiableList(classes)) != null) {
                throw new IllegalStateException("Duplicate DYPIT timetable division: " + row);
            }
        }

        for (Map<String, List<TimetableData.ClassItem>> byDivision : result.values()) {
            if (byDivision.size() != DIVISIONS.length) {
                throw new IllegalStateException("Incomplete DYPIT timetable date.");
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static String subjectName(String code) {
        // The supplied PDF contains only these course abbreviations and no legend.
        // Keep the source codes intact rather than guessing their full titles.
        return code;
    }

    public static List<TimetableData.ClassItem> forDate(String division, LocalDate date) {
        if (date == null || division == null) return Collections.emptyList();
        String normalized = division.trim().toUpperCase(Locale.ENGLISH);
        Map<String, List<TimetableData.ClassItem>> byDivision = SCHEDULES.get(date);
        if (byDivision == null) return Collections.emptyList();
        List<TimetableData.ClassItem> result = byDivision.get(normalized);
        return result == null ? Collections.emptyList() : result;
    }

    public static List<TimetableData.ClassItem> forDay(
            String division, String day, LocalDate anchorDate) {
        LocalDate date = dateForDay(day, anchorDate);
        return date == null ? Collections.emptyList() : forDate(division, date);
    }

    /** Finds the next matching published date; after the final date, returns the most recent match. */
    public static LocalDate dateForDay(String day, LocalDate anchorDate) {
        if (day == null || anchorDate == null) return null;
        for (LocalDate date : SCHEDULES.keySet()) {
            if (!date.isBefore(anchorDate)
                    && date.getDayOfWeek().getDisplayName(
                            java.time.format.TextStyle.FULL, Locale.ENGLISH).equalsIgnoreCase(day)) {
                return date;
            }
        }
        LocalDate previous = null;
        for (LocalDate date : SCHEDULES.keySet()) {
            if (!date.isAfter(anchorDate)
                    && date.getDayOfWeek().getDisplayName(
                            java.time.format.TextStyle.FULL, Locale.ENGLISH).equalsIgnoreCase(day)) {
                previous = date;
            }
        }
        return previous;
    }

    /** Returns only an upcoming published date, or null when the supplied timetable has ended. */
    public static LocalDate nextPublishedDate(LocalDate anchorDate) {
        if (anchorDate == null) return null;
        for (LocalDate date : SCHEDULES.keySet()) {
            if (!date.isBefore(anchorDate)) return date;
        }
        return null;
    }

    public static LocalDate[] dates() {
        return SCHEDULES.keySet().toArray(new LocalDate[0]);
    }

    public static String[] divisions() {
        return DIVISIONS.clone();
    }

    public static int publishedDateCount() {
        return SCHEDULES.size();
    }

    public static int divisionCount(LocalDate date) {
        Map<String, List<TimetableData.ClassItem>> byDivision = SCHEDULES.get(date);
        return byDivision == null ? 0 : byDivision.size();
    }
}
