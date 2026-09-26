package com.nealaringale.nextbell;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainActivity extends Activity {
    private static final String PREFS_NAME = "nextbell_profile";
    private static final String KEY_ROLL_NUMBER = "roll_number";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final ZoneId zone = ZoneId.of("Asia/Kolkata");

    private SharedPreferences preferences;

    private LinearLayout scheduleContainer;
    private TextView focusLabel;
    private TextView focusSubject;
    private TextView focusMeta;
    private TextView focusMinutes;
    private TextView dayPulse;
    private TextView completedText;
    private TextView freePeriodsText;
    private TextView profileChip;

    private String selectedDay;
    private Runnable refreshRunnable;

    private final int BG = Color.rgb(11, 13, 18);
    private final int SURFACE = Color.rgb(18, 21, 28);
    private final int SURFACE_2 = Color.rgb(23, 27, 35);
    private final int BORDER = Color.rgb(42, 48, 58);
    private final int TEXT = Color.rgb(245, 247, 251);
    private final int MUTED = Color.rgb(146, 154, 170);
    private final int SUBTLE = Color.rgb(110, 118, 134);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        if (!hasSavedRoll()) {
            showSetupScreen();
        } else {
            openApp();
        }
    }

    private boolean hasSavedRoll() {
        return preferences.getInt(KEY_ROLL_NUMBER, -1) > 0;
    }

    private int getRollNumber() {
        return preferences.getInt(KEY_ROLL_NUMBER, 34);
    }

    private void saveRollNumber(int rollNumber) {
        preferences.edit().putInt(KEY_ROLL_NUMBER, rollNumber).apply();
    }

    private void openApp() {
        selectedDay = todayNameOrMonday();
        buildUi();
        refreshSchedule();

        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshSchedule();
                handler.postDelayed(this, 30_000);
            }
        };
        handler.postDelayed(refreshRunnable, 30_000);
    }

    private void showSetupScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));
        root.setBackgroundColor(BG);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(24), dp(26), dp(24), dp(24));
        card.setBackground(round(SURFACE, BORDER, 22));

        TextView mark = text("⌁", 34, TEXT);
        mark.setGravity(Gravity.CENTER);
        mark.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(-1, -2);
        card.addView(mark, markParams);

        TextView title = text("Welcome to NextBell", 25, TEXT);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(0, dp(8), 0, 0);
        card.addView(title);

        TextView subtitle = text("Let's personalize your timetable.", 13, MUTED);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(7), 0, dp(22));
        card.addView(subtitle);

        TextView label = text("ROLL NUMBER", 10, MUTED);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setLetterSpacing(0.12f);
        card.addView(label);

        EditText rollInput = new EditText(this);
        rollInput.setSingleLine(true);
        rollInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        rollInput.setTextSize(17);
        rollInput.setTextColor(TEXT);
        rollInput.setHintTextColor(SUBTLE);
        rollInput.setHint("e.g. 34");
        rollInput.setPadding(dp(14), dp(10), dp(14), dp(10));
        rollInput.setBackground(round(SURFACE_2, BORDER, 12));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(52));
        inputParams.setMargins(0, dp(8), 0, dp(14));
        card.addView(rollInput, inputParams);

        TextView note = text("We'll remember this on this phone. You can change it anytime from ⚙ Settings.", 11, SUBTLE);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, 0, 0, dp(17));
        card.addView(note);

        Button continueButton = new Button(this);
        continueButton.setText("Continue");
        continueButton.setTextSize(13);
        continueButton.setTextColor(BG);
        continueButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        continueButton.setAllCaps(false);
        continueButton.setBackground(round(TEXT, TEXT, 13));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(50));
        card.addView(continueButton, buttonParams);

        continueButton.setOnClickListener(v -> {
            String raw = rollInput.getText().toString().trim();
            try {
                int roll = Integer.parseInt(raw);
                if (roll < 1 || roll > 999) {
                    rollInput.setError("Enter a valid roll number.");
                    return;
                }
                saveRollNumber(roll);
                openApp();
            } catch (NumberFormatException e) {
                rollInput.setError("Enter your roll number.");
            }
        });

        root.addView(card, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(20), dp(20), dp(10));

        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams headerTextParams = new LinearLayout.LayoutParams(0, -2, 1f);

        TextView brand = text("NextBell", 24, TEXT);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        headerText.addView(brand);

        TextView subtitle = text("NMIET · FE 2026–27", 12, MUTED);
        subtitle.setPadding(0, dp(3), 0, 0);
        headerText.addView(subtitle);

        header.addView(headerText, headerTextParams);

        profileChip = text("B · B2\nRoll " + getRollNumber(), 11, MUTED);
        profileChip.setGravity(Gravity.CENTER);
        profileChip.setPadding(dp(10), dp(7), dp(10), dp(7));
        profileChip.setBackground(round(SURFACE_2, BORDER, 999));
        header.addView(profileChip);

        TextView settingsButton = text("⚙", 22, TEXT);
        settingsButton.setGravity(Gravity.CENTER);
        settingsButton.setPadding(dp(10), dp(8), dp(6), dp(8));
        settingsButton.setContentDescription("Settings");
        settingsButton.setClickable(true);
        settingsButton.setFocusable(true);
        settingsButton.setOnClickListener(v -> showRollSettings());
        header.addView(settingsButton, new LinearLayout.LayoutParams(dp(52), dp(52)));

        root.addView(header);

        HorizontalScrollView daysScroll = new HorizontalScrollView(this);
        daysScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout dayRow = new LinearLayout(this);
        dayRow.setOrientation(LinearLayout.HORIZONTAL);
        dayRow.setPadding(dp(16), dp(4), dp(16), dp(16));

        for (String day : TimetableData.DAYS) {
            TextView dayButton = text(day.substring(0, 3), 11, MUTED);
            dayButton.setGravity(Gravity.CENTER);
            dayButton.setPadding(dp(15), dp(9), dp(15), dp(9));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
            p.setMargins(0, 0, dp(7), 0);
            dayRow.addView(dayButton, p);
            dayButton.setOnClickListener(v -> {
                selectedDay = day;
                highlightDayButtons(dayRow);
                refreshSchedule();
            });
        }

        daysScroll.addView(dayRow);
        root.addView(daysScroll);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(20), dp(20), dp(20));
        hero.setBackground(round(SURFACE, BORDER, 20));

        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, -2);
        heroParams.setMargins(dp(16), 0, dp(16), dp(12));

        focusLabel = text("NEXT CLASS", 10, MUTED);
        focusLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        focusLabel.setLetterSpacing(0.12f);

        focusSubject = text("Loading…", 28, TEXT);
        focusSubject.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        focusSubject.setPadding(0, dp(7), 0, 0);

        focusMeta = text("", 12, MUTED);
        focusMeta.setPadding(0, dp(7), 0, 0);

        focusMinutes = text("", 39, TEXT);
        focusMinutes.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        focusMinutes.setPadding(0, dp(22), 0, 0);

        hero.addView(focusLabel);
        hero.addView(focusSubject);
        hero.addView(focusMeta);
        hero.addView(focusMinutes);
        root.addView(hero, heroParams);

        LinearLayout pulse = new LinearLayout(this);
        pulse.setOrientation(LinearLayout.HORIZONTAL);
        pulse.setPadding(dp(18), dp(14), dp(18), dp(14));
        pulse.setGravity(Gravity.CENTER_VERTICAL);
        pulse.setBackground(round(SURFACE, BORDER, 16));
        LinearLayout.LayoutParams pulseParams = new LinearLayout.LayoutParams(-1, -2);
        pulseParams.setMargins(dp(16), 0, dp(16), dp(14));

        LinearLayout statA = statBlock();
        LinearLayout statB = statBlock();
        LinearLayout statC = statBlock();

        dayPulse = statValue(statA, "—");
        addStatLabel(statA, "classes today");

        completedText = statValue(statB, "—");
        addStatLabel(statB, "completed");

        freePeriodsText = statValue(statC, "—");
        addStatLabel(statC, "free periods");

        pulse.addView(statA, new LinearLayout.LayoutParams(0, -2, 1f));
        pulse.addView(statB, new LinearLayout.LayoutParams(0, -2, 1f));
        pulse.addView(statC, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(pulse, pulseParams);

        TextView scheduleTitle = text("YOUR SCHEDULE", 11, MUTED);
        scheduleTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        scheduleTitle.setLetterSpacing(0.10f);
        scheduleTitle.setPadding(dp(20), dp(3), dp(20), dp(9));
        root.addView(scheduleTitle);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scheduleContainer = new LinearLayout(this);
        scheduleContainer.setOrientation(LinearLayout.VERTICAL);
        scheduleContainer.setPadding(dp(16), 0, dp(16), dp(24));
        scroll.addView(scheduleContainer);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        highlightDayButtons(dayRow);
        setContentView(root);
    }

    private void showRollSettings() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(getRollNumber()));
        input.setSelectAllOnFocus(true);
        input.setTextSize(17);

        int padding = dp(6);
        input.setPadding(padding, 0, padding, 0);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Profile settings")
                .setMessage("Change your roll number for this phone.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            try {
                int roll = Integer.parseInt(input.getText().toString().trim());
                if (roll < 1 || roll > 999) {
                    input.setError("Enter a valid roll number.");
                    return;
                }

                saveRollNumber(roll);
                if (profileChip != null) {
                    profileChip.setText("B · B2\nRoll " + roll);
                }
                dialog.dismiss();
            } catch (NumberFormatException e) {
                input.setError("Enter your roll number.");
            }
        }));

        dialog.show();
    }

    private void refreshSchedule() {
        List<TimetableData.ClassItem> classes = TimetableData.forDay(selectedDay);
        int nowMinutes = nowMinutes();

        TimetableData.ClassItem current = null;
        TimetableData.ClassItem next = null;
        int completed = 0;

        for (TimetableData.ClassItem item : classes) {
            int start = toMinutes(item.start);
            int end = toMinutes(item.end);
            if (end <= nowMinutes) completed++;
            if (start <= nowMinutes && nowMinutes < end) {
                current = item;
            } else if (start > nowMinutes && next == null) {
                next = item;
            }
        }

        TimetableData.ClassItem focus = current != null ? current : next;
        if (focus == null) {
            focusLabel.setText("DAY COMPLETE");
            focusSubject.setText("You’re done for today 🎉");
            focusMeta.setText("No more scheduled classes.");
            focusMinutes.setText("");
        } else {
            focusLabel.setText(current != null ? "HAPPENING NOW" : "NEXT CLASS");
            focusSubject.setText(focus.subject);
            String batch = focus.batch.isEmpty() ? "" : " · " + focus.batch;
            focusMeta.setText(formatTime(focus.start) + " – " + formatTime(focus.end)
                    + "  ·  " + focus.room + "  ·  " + focus.type + batch);

            int remaining = current != null
                    ? Math.max(0, toMinutes(focus.end) - nowMinutes)
                    : Math.max(0, toMinutes(focus.start) - nowMinutes);
            focusMinutes.setText(remaining + " min");
        }

        dayPulse.setText(String.valueOf(classes.size()));
        completedText.setText(completed + "/" + classes.size());
        freePeriodsText.setText(String.valueOf(calculateFreePeriods(classes)));

        scheduleContainer.removeAllViews();
        for (TimetableData.ClassItem item : classes) {
            boolean isCurrent = current != null && current.id.equals(item.id);
            boolean isNext = current == null && next != null && next.id.equals(item.id);
            boolean isDone = toMinutes(item.end) <= nowMinutes;
            scheduleContainer.addView(classCard(item, isCurrent, isNext, isDone));
        }
    }

    private View classCard(TimetableData.ClassItem item, boolean isCurrent, boolean isNext, boolean isDone) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(13), dp(13), dp(13), dp(13));

        int cardColor = (isCurrent || isNext) ? SURFACE_2 : SURFACE;
        card.setBackground(round(cardColor, (isCurrent || isNext) ? TEXT : BORDER, 15));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(9));
        card.setLayoutParams(params);

        TextView time = text(formatTime(item.start), 12, isDone ? SUBTLE : TEXT);
        time.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(dp(70), -2);
        card.addView(time, timeParams);

        TextView type = text(typeLetter(item.type), 10, TEXT);
        type.setGravity(Gravity.CENTER);
        type.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        type.setBackground(round(Color.rgb(36, 41, 50), Color.TRANSPARENT, 8));
        LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(dp(30), dp(30));
        typeParams.setMargins(0, 0, dp(10), 0);
        card.addView(type, typeParams);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);

        TextView subject = text(item.subject, 13, isDone ? SUBTLE : TEXT);
        subject.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        main.addView(subject);

        String batch = item.batch.isEmpty() ? "" : " · " + item.batch;
        TextView details = text(item.teacher + " · " + item.type + batch, 10, SUBTLE);
        details.setPadding(0, dp(4), 0, 0);
        main.addView(details);

        LinearLayout.LayoutParams mainParams = new LinearLayout.LayoutParams(0, -2, 1f);
        card.addView(main, mainParams);

        TextView room = text("⌖ " + item.room, 10, MUTED);
        room.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        card.addView(room, new LinearLayout.LayoutParams(dp(65), -2));

        if (isCurrent || isNext) {
            TextView badge = text(isCurrent ? "NOW" : "NEXT", 9, TEXT);
            badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            badge.setPadding(dp(5), dp(4), dp(5), dp(4));
            badge.setBackground(round(Color.TRANSPARENT, BORDER, 999));
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(38), -2);
            badgeParams.setMargins(dp(7), 0, 0, 0);
            card.addView(badge, badgeParams);
        }

        card.setAlpha(isDone ? 0.48f : 1f);
        return card;
    }

    private void highlightDayButtons(LinearLayout dayRow) {
        for (int i = 0; i < dayRow.getChildCount(); i++) {
            TextView button = (TextView) dayRow.getChildAt(i);
            String day = TimetableData.DAYS[i];
            boolean selected = day.equals(selectedDay);
            button.setTextColor(selected ? BG : MUTED);
            button.setBackground(round(selected ? TEXT : SURFACE_2, selected ? TEXT : BORDER, 999));
        }
    }

    private LinearLayout statBlock() {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        return block;
    }

    private TextView statValue(LinearLayout block, String value) {
        TextView t = text(value, 22, TEXT);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        block.addView(t);
        return t;
    }

    private void addStatLabel(LinearLayout block, String label) {
        TextView t = text(label, 9, SUBTLE);
        t.setPadding(0, dp(2), 0, 0);
        block.addView(t);
    }

    private TextView text(String value, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    private GradientDrawable round(int fill, int stroke, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private int toMinutes(String value) {
        LocalTime t = LocalTime.parse(value, timeFormatter);
        return t.getHour() * 60 + t.getMinute();
    }

    private int nowMinutes() {
        LocalTime now = LocalTime.now(zone);
        return now.getHour() * 60 + now.getMinute();
    }

    private String formatTime(String value) {
        LocalTime t = LocalTime.parse(value, timeFormatter);
        int h = t.getHour();
        String suffix = h >= 12 ? "PM" : "AM";
        int hour = h % 12 == 0 ? 12 : h % 12;
        return hour + ":" + String.format("%02d", t.getMinute()) + " " + suffix;
    }

    private int calculateFreePeriods(List<TimetableData.ClassItem> classes) {
        int result = 0;
        for (int i = 1; i < classes.size(); i++) {
            int gap = toMinutes(classes.get(i).start) - toMinutes(classes.get(i - 1).end);
            if (gap >= 45) result++;
        }
        return result;
    }

    private String typeLetter(String type) {
        if ("Practical".equals(type)) return "P";
        if ("Tutorial".equals(type)) return "T";
        return "L";
    }

    private String todayNameOrMonday() {
        DayOfWeek day = LocalDate.now(zone).getDayOfWeek();
        switch (day) {
            case MONDAY: return "Monday";
            case TUESDAY: return "Tuesday";
            case WEDNESDAY: return "Wednesday";
            case THURSDAY: return "Thursday";
            case FRIDAY: return "Friday";
            case SATURDAY: return "Saturday";
            default: return "Monday";
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
