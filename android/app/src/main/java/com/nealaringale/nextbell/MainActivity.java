package com.nealaringale.nextbell;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
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
    private static final String KEY_NAME = "name";
    private static final String KEY_ROLL_NUMBER = "roll_number";
    private static final String KEY_NOTIFICATION_PROMPTED = "notification_prompted";
    private static final String KEY_THEME = "theme";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy");
    private final DateTimeFormatter clockFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
    private final ZoneId zone = ZoneId.of("Asia/Kolkata");

    private SharedPreferences preferences;

    private LinearLayout root;
    private LinearLayout scheduleContainer;
    private LinearLayout nextPanel;
    private LinearLayout heroCard;
    private LinearLayout statsCard;
    private TextView greetingText;
    private TextView profileChip;
    private TextView liveDate;
    private TextView liveClock;
    private TextView dayTitle;
    private TextView focusLabel;
    private TextView focusSubject;
    private TextView focusMeta;
    private TextView focusMinutes;
    private TextView nextSubject;
    private TextView nextMeta;
    private TextView nextCountdown;
    private TextView dayPulse;
    private TextView completedText;
    private TextView freePeriodsText;
    private TextView themeButtonLabel;

    private String selectedDay;
    private boolean followToday = true;
    private int themeIndex;
    private Runnable refreshRunnable;

    private static final String[] THEME_NAMES = {
            "Midnight", "Ocean", "Sakura", "Forest", "Solar"
    };

    private static final int[][] THEMES = {
            {Color.rgb(10, 12, 17), Color.rgb(17, 20, 27), Color.rgb(24, 28, 37), Color.rgb(47, 53, 64), Color.rgb(245, 247, 251), Color.rgb(148, 157, 175), Color.rgb(112, 233, 130), Color.rgb(31, 58, 39)},
            {Color.rgb(7, 13, 24), Color.rgb(12, 22, 37), Color.rgb(18, 32, 51), Color.rgb(34, 56, 78), Color.rgb(241, 248, 255), Color.rgb(151, 174, 201), Color.rgb(88, 201, 255), Color.rgb(17, 52, 72)},
            {Color.rgb(20, 12, 20), Color.rgb(31, 18, 32), Color.rgb(43, 23, 44), Color.rgb(72, 39, 70), Color.rgb(255, 246, 252), Color.rgb(187, 158, 181), Color.rgb(255, 132, 193), Color.rgb(70, 31, 56)},
            {Color.rgb(8, 16, 14), Color.rgb(13, 26, 22), Color.rgb(20, 38, 31), Color.rgb(41, 65, 52), Color.rgb(239, 251, 245), Color.rgb(153, 185, 170), Color.rgb(101, 220, 163), Color.rgb(24, 65, 47)},
            {Color.rgb(18, 15, 10), Color.rgb(29, 23, 14), Color.rgb(42, 32, 17), Color.rgb(74, 55, 28), Color.rgb(255, 250, 238), Color.rgb(193, 177, 145), Color.rgb(255, 184, 77), Color.rgb(78, 53, 18)}
    };

    private int BG;
    private int SURFACE;
    private int SURFACE_2;
    private int BORDER;
    private int TEXT;
    private int MUTED;
    private int ACCENT;
    private int ACCENT_BG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        themeIndex = Math.max(0, Math.min(4, preferences.getInt(KEY_THEME, 0)));
        applyTheme();

        if (!hasSavedProfile()) {
            showSetupScreen();
        } else {
            openApp();
        }
    }

    private boolean hasSavedProfile() {
        return !getName().isEmpty() && preferences.getInt(KEY_ROLL_NUMBER, -1) > 0;
    }

    private String getName() {
        return preferences.getString(KEY_NAME, "").trim();
    }

    private int getRollNumber() {
        return preferences.getInt(KEY_ROLL_NUMBER, 34);
    }

    private String getBatch() {
        return TimetableData.batchForRoll(getRollNumber());
    }

    private void saveProfile(String name, int rollNumber) {
        preferences.edit()
                .putString(KEY_NAME, name.trim())
                .putInt(KEY_ROLL_NUMBER, rollNumber)
                .apply();
    }

    private void applyTheme() {
        int[] palette = THEMES[themeIndex];
        BG = palette[0];
        SURFACE = palette[1];
        SURFACE_2 = palette[2];
        BORDER = palette[3];
        TEXT = palette[4];
        MUTED = palette[5];
        ACCENT = palette[6];
        ACCENT_BG = palette[7];

        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(0);
        }
    }

    private void openApp() {
        selectedDay = todayNameOrMonday();
        followToday = true;
        buildUi();
        refreshSchedule();
        NotificationScheduler.scheduleUpcoming(this);
        requestNotificationPermissionIfNeeded();

        if (refreshRunnable != null) {
            handler.removeCallbacks(refreshRunnable);
        }

        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshSchedule();
                handler.postDelayed(this, 1_000);
            }
        };
        handler.postDelayed(refreshRunnable, 1_000);
    }

    private void showSetupScreen() {
        LinearLayout page = page();

        TextView logo = text("NB", 28, TEXT);
        logo.setGravity(Gravity.CENTER);
        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        logo.setBackground(round(ACCENT, ACCENT, 18));
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(72), dp(72));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        page.addView(logo, logoParams);

        TextView title = text("Welcome to NextBell", 28, TEXT);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(0, dp(16), 0, 0);
        page.addView(title);

        TextView subtitle = text("Your college day, personalized around you.", 13, MUTED);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(7), 0, dp(28));
        page.addView(subtitle);

        LinearLayout card = card();
        card.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView nameLabel = sectionLabel("YOUR NAME");
        card.addView(nameLabel);

        EditText nameInput = field("e.g. Neal", false);
        card.addView(nameInput, fieldParams());

        TextView rollLabel = sectionLabel("ROLL NUMBER");
        LinearLayout.LayoutParams rollLabelParams = new LinearLayout.LayoutParams(-1, -2);
        rollLabelParams.setMargins(0, dp(18), 0, 0);
        card.addView(rollLabel, rollLabelParams);

        EditText rollInput = field("e.g. 34", true);
        card.addView(rollInput, fieldParams());

        TextView batchHint = text("B1 · 1–25    B2 · 26–50    B3 · 51+", 10, MUTED);
        batchHint.setGravity(Gravity.CENTER);
        batchHint.setPadding(0, dp(8), 0, dp(18));
        card.addView(batchHint);

        Button continueButton = primaryButton("Let's go");
        card.addView(continueButton, new LinearLayout.LayoutParams(-1, dp(52)));

        continueButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String raw = rollInput.getText().toString().trim();

            if (name.isEmpty()) {
                nameInput.setError("Enter your name.");
                return;
            }

            try {
                int roll = Integer.parseInt(raw);
                if (roll < 1 || roll > 999) {
                    rollInput.setError("Enter a valid roll number.");
                    return;
                }
                saveProfile(name, roll);
                openApp();
            } catch (NumberFormatException e) {
                rollInput.setError("Enter your roll number.");
            }
        });

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, dp(18), 0, 0);
        page.addView(card, cardParams);

        TextView privacy = text("Your profile is stored only on this phone.", 10, MUTED);
        privacy.setGravity(Gravity.CENTER);
        privacy.setPadding(0, dp(14), 0, 0);
        page.addView(privacy);

        setContentView(page);
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(16), dp(18), dp(8));

        LinearLayout identity = new LinearLayout(this);
        identity.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams identityParams = new LinearLayout.LayoutParams(0, -2, 1f);

        TextView brand = text("NextBell", 23, TEXT);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        identity.addView(brand);

        greetingText = text("Hey, " + getName() + " 👋", 13, TEXT);
        greetingText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        greetingText.setPadding(0, dp(3), 0, 0);
        identity.addView(greetingText);

        header.addView(identity, identityParams);

        LinearLayout live = new LinearLayout(this);
        live.setOrientation(LinearLayout.VERTICAL);
        live.setGravity(Gravity.END);

        liveClock = text("--:--:-- --", 12, TEXT);
        liveClock.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        live.addView(liveClock);

        liveDate = text("Loading…", 9, MUTED);
        liveDate.setGravity(Gravity.END);
        liveDate.setPadding(0, dp(2), 0, 0);
        live.addView(liveDate);

        header.addView(live);

        TextView settings = text("⚙", 23, TEXT);
        settings.setGravity(Gravity.CENTER);
        settings.setContentDescription("Settings");
        settings.setClickable(true);
        settings.setFocusable(true);
        settings.setOnClickListener(v -> showSettings());
        header.addView(settings, new LinearLayout.LayoutParams(dp(48), dp(48)));

        root.addView(header);

        profileChip = text(getBatch() + " · Roll " + getRollNumber(), 9, ACCENT);
        profileChip.setGravity(Gravity.CENTER);
        profileChip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        profileChip.setPadding(dp(11), dp(7), dp(11), dp(7));
        profileChip.setBackground(round(ACCENT_BG, ACCENT, 999));
        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(-2, -2);
        chipParams.setMargins(dp(18), 0, dp(18), dp(10));
        chipParams.gravity = Gravity.START;
        root.addView(profileChip, chipParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content.setPadding(0, 0, 0, 0);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), 0, dp(18), dp(24));

        dayTitle = text(selectedDay.toUpperCase(), 11, MUTED);
        dayTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        dayTitle.setLetterSpacing(0.18f);
        body.addView(dayTitle);

        LinearLayout dayTabs = new LinearLayout(this);
        dayTabs.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams dayTabsParams = new LinearLayout.LayoutParams(-1, -2);
        dayTabsParams.setMargins(0, dp(9), 0, dp(14));
        body.addView(dayTabs, dayTabsParams);

        for (String day : TimetableData.DAYS) {
            TextView tab = text(day.substring(0, 3), 11, MUTED);
            tab.setGravity(Gravity.CENTER);
            tab.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            tab.setPadding(dp(13), dp(9), dp(13), dp(9));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(38), 1f);
            p.setMargins(0, 0, dp(6), 0);
            dayTabs.addView(tab, p);
            tab.setOnClickListener(v -> {
                selectedDay = day;
                followToday = day.equals(todayName());
                highlightDayButtons(dayTabs);
                updateDayHeader();
                refreshSchedule();
            });
        }

        heroCard = new LinearLayout(this);
        heroCard.setOrientation(LinearLayout.VERTICAL);
        heroCard.setPadding(dp(20), dp(19), dp(20), dp(20));
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, -2);
        heroParams.setMargins(0, 0, 0, dp(10));
        body.addView(heroCard, heroParams);

        focusLabel = text("NEXT LECTURE", 9, ACCENT);
        focusLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        focusLabel.setLetterSpacing(0.15f);
        heroCard.addView(focusLabel);

        focusSubject = text("Loading…", 27, TEXT);
        focusSubject.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        focusSubject.setPadding(0, dp(7), 0, 0);
        heroCard.addView(focusSubject);

        focusMeta = text("", 11, MUTED);
        focusMeta.setPadding(0, dp(7), 0, 0);
        heroCard.addView(focusMeta);

        focusMinutes = text("", 38, TEXT);
        focusMinutes.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        focusMinutes.setPadding(0, dp(17), 0, 0);
        heroCard.addView(focusMinutes);

        nextPanel = new LinearLayout(this);
        nextPanel.setOrientation(LinearLayout.VERTICAL);
        nextPanel.setPadding(dp(17), dp(14), dp(17), dp(14));
        nextPanel.setBackground(round(SURFACE_2, BORDER, 17));
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(-1, -2);
        nextParams.setMargins(0, 0, 0, dp(10));
        body.addView(nextPanel, nextParams);

        TextView nextLabel = text("UP NEXT", 9, MUTED);
        nextLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nextLabel.setLetterSpacing(0.14f);
        nextPanel.addView(nextLabel);

        nextSubject = text("Loading…", 15, TEXT);
        nextSubject.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nextSubject.setPadding(0, dp(5), 0, 0);
        nextPanel.addView(nextSubject);

        nextMeta = text("", 10, SUBTLE);
        nextMeta.setPadding(0, dp(3), 0, 0);
        nextPanel.addView(nextMeta);

        nextCountdown = text("", 19, TEXT);
        nextCountdown.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nextCountdown.setPadding(0, dp(6), 0, 0);
        nextPanel.addView(nextCountdown);

        statsCard = card();
        statsCard.setOrientation(LinearLayout.HORIZONTAL);
        statsCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(-1, -2);
        statsParams.setMargins(0, 0, 0, dp(18));
        body.addView(statsCard, statsParams);

        LinearLayout statA = statBlock();
        dayPulse = statValue(statA, "—");
        statLabel(statA, "classes");

        LinearLayout statB = statBlock();
        completedText = statValue(statB, "—");
        statLabel(statB, "done");

        LinearLayout statC = statBlock();
        freePeriodsText = statValue(statC, "—");
        statLabel(statC, "open gaps");

        statsCard.addView(statA, new LinearLayout.LayoutParams(0, -2, 1f));
        statsCard.addView(statB, new LinearLayout.LayoutParams(0, -2, 1f));
        statsCard.addView(statC, new LinearLayout.LayoutParams(0, -2, 1f));

        LinearLayout scheduleHeader = new LinearLayout(this);
        scheduleHeader.setOrientation(LinearLayout.HORIZONTAL);
        scheduleHeader.setGravity(Gravity.CENTER_VERTICAL);
        body.addView(scheduleHeader);

        TextView scheduleHeading = text("TODAY'S FLOW", 10, MUTED);
        scheduleHeading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        scheduleHeading.setLetterSpacing(0.15f);
        scheduleHeader.addView(scheduleHeading, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView rollInfo = text(getBatch(), 9, ACCENT);
        rollInfo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        scheduleHeader.addView(rollInfo);

        scheduleContainer = new LinearLayout(this);
        scheduleContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams scheduleParams = new LinearLayout.LayoutParams(-1, -2);
        scheduleParams.setMargins(0, dp(10), 0, 0);
        body.addView(scheduleContainer, scheduleParams);

        footer(body);

        scroll.addView(body);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));

        highlightDayButtons(dayTabs);
        updateDayHeader();

        setContentView(root);
    }

    private void updateDayHeader() {
        if (dayTitle != null) {
            dayTitle.setText(selectedDay.toUpperCase());
        }
    }

    private void showSettings() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(3), 0, dp(3), 0);

        TextView profileSection = sectionLabel("PROFILE");
        box.addView(profileSection);

        TextView profileDescription = text(
                getName() + " · " + getBatch() + " · Roll " + getRollNumber(),
                12,
                MUTED
        );
        profileDescription.setPadding(0, dp(6), 0, dp(13));
        box.addView(profileDescription);

        Button editProfile = secondaryButton("Edit name & roll number");
        box.addView(editProfile, new LinearLayout.LayoutParams(-1, dp(46)));

        TextView appearance = sectionLabel("APPEARANCE");
        LinearLayout.LayoutParams appearanceParams = new LinearLayout.LayoutParams(-1, -2);
        appearanceParams.setMargins(0, dp(20), 0, 0);
        box.addView(appearance, appearanceParams);

        LinearLayout themeRow = new LinearLayout(this);
        themeRow.setOrientation(LinearLayout.HORIZONTAL);
        themeRow.setGravity(Gravity.CENTER_VERTICAL);
        themeRow.setPadding(0, dp(8), 0, dp(6));

        themeButtonLabel = text(THEME_NAMES[themeIndex], 13, TEXT);
        themeButtonLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        themeRow.addView(themeButtonLabel, new LinearLayout.LayoutParams(0, -2, 1f));

        Button changeTheme = secondaryButton("Change");
        themeRow.addView(changeTheme, new LinearLayout.LayoutParams(dp(92), dp(42)));
        box.addView(themeRow);

        TextView notificationLabel = sectionLabel("NOTIFICATIONS");
        LinearLayout.LayoutParams notificationParams = new LinearLayout.LayoutParams(-1, -2);
        notificationParams.setMargins(0, dp(20), 0, 0);
        box.addView(notificationLabel, notificationParams);

        TextView notificationText = text(
                "15-minute reminders use your personalized timetable.",
                11,
                MUTED
        );
        notificationText.setPadding(0, dp(6), 0, 0);
        box.addView(notificationText);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setView(box)
                .setPositiveButton("Done", null)
                .create();

        editProfile.setOnClickListener(v -> {
            dialog.dismiss();
            showProfileEditor();
        });

        changeTheme.setOnClickListener(v -> {
            showThemePicker();
        });

        dialog.show();
    }

    private void showProfileEditor() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        final EditText nameInput = field("Your name", false);
        nameInput.setText(getName());

        final EditText rollInput = field("Roll number", true);
        rollInput.setText(String.valueOf(getRollNumber()));

        box.addView(sectionLabel("NAME"));
        box.addView(nameInput, fieldParams());
        LinearLayout.LayoutParams rollLabelParams = new LinearLayout.LayoutParams(-1, -2);
        rollLabelParams.setMargins(0, dp(14), 0, 0);
        TextView rollLabel = sectionLabel("ROLL NUMBER");
        box.addView(rollLabel, rollLabelParams);
        box.addView(rollInput, fieldParams());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit profile")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                nameInput.setError("Enter your name.");
                return;
            }

            try {
                int roll = Integer.parseInt(rollInput.getText().toString().trim());
                if (roll < 1 || roll > 999) {
                    rollInput.setError("Enter a valid roll number.");
                    return;
                }

                saveProfile(name, roll);
                profileChip.setText(getBatch() + " · Roll " + roll);
                greetingText.setText("Hey, " + getName() + " 👋");
                NotificationScheduler.scheduleUpcoming(this);
                refreshSchedule();
                dialog.dismiss();
            } catch (NumberFormatException e) {
                rollInput.setError("Enter your roll number.");
            }
        }));

        dialog.show();
    }

    private void showThemePicker() {
        new AlertDialog.Builder(this)
                .setTitle("Choose a theme")
                .setSingleChoiceItems(THEME_NAMES, themeIndex, (dialog, which) -> {
                    themeIndex = which;
                    preferences.edit().putInt(KEY_THEME, themeIndex).apply();
                    applyTheme();
                    dialog.dismiss();
                    rebuildForTheme();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void rebuildForTheme() {
        if (hasSavedProfile()) {
            openApp();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            NotificationScheduler.scheduleUpcoming(this);
            return;
        }

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationScheduler.scheduleUpcoming(this);
            return;
        }

        if (preferences.getBoolean(KEY_NOTIFICATION_PROMPTED, false)) {
            return;
        }

        preferences.edit().putBoolean(KEY_NOTIFICATION_PROMPTED, true).apply();

        new AlertDialog.Builder(this)
                .setTitle("Enable class reminders?")
                .setMessage("NextBell can remind you 15 minutes before your personalized classes.")
                .setNegativeButton("Not now", null)
                .setPositiveButton("Enable", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissions(
                                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                7001
                        );
                    }
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 7001) {
            NotificationScheduler.scheduleUpcoming(this);
        }
    }

    private void refreshSchedule() {
        updateLiveHeader();

        String today = todayName();
        if (followToday && !today.isEmpty() && !today.equals(selectedDay)) {
            selectedDay = today;
            refreshDayButtons();
            updateDayHeader();
        }

        if (scheduleContainer == null) return;

        int rollNumber = getRollNumber();
        List<TimetableData.ClassItem> entries =
                TimetableData.forRollAndDay(rollNumber, selectedDay);

        boolean viewingToday = selectedDay.equals(todayName());
        int nowSeconds = viewingToday ? nowSeconds() : -1;

        TimetableData.ClassItem current = null;
        TimetableData.ClassItem next = null;
        int completed = 0;
        int scheduled = 0;

        for (TimetableData.ClassItem item : entries) {
            if (!item.isBreak()) scheduled++;

            int start = toSeconds(item.start);
            int end = toSeconds(item.end);

            if (viewingToday && item.isAcademic() && end <= nowSeconds) {
                completed++;
            }

            if (item.isAcademic()) {
                if (viewingToday && start <= nowSeconds && nowSeconds < end) {
                    current = item;
                } else if (viewingToday && start > nowSeconds && next == null) {
                    next = item;
                } else if (!viewingToday && next == null) {
                    next = item;
                }
            }
        }

        if (current != null) {
            updateCurrentHero(current, next, nowSeconds);
        } else if (next != null) {
            updateNextHero(next, viewingToday, nowSeconds);
        } else {
            updateFinishedHero(viewingToday);
        }

        dayPulse.setText(String.valueOf(scheduled));
        completedText.setText(viewingToday ? completed + "/" + scheduled : "—");
        freePeriodsText.setText(String.valueOf(calculateOpenGaps(entries)));

        scheduleContainer.removeAllViews();
        for (TimetableData.ClassItem item : entries) {
            boolean isCurrent = current != null && current.id.equals(item.id);
            boolean isNext = current == null && next != null && next.id.equals(item.id);
            boolean isDone = viewingToday && item.isAcademic() && toSeconds(item.end) <= nowSeconds;

            scheduleContainer.addView(
                    item.isBreak()
                            ? breakCard(item)
                            : classCard(item, isCurrent, isNext, isDone)
            );
        }
    }

    private void updateCurrentHero(
            TimetableData.ClassItem current,
            TimetableData.ClassItem next,
            int nowSeconds
    ) {
        heroCard.setBackground(round(SURFACE, ACCENT, 21));
        focusLabel.setText("HAPPENING NOW");
        focusSubject.setText(current.subject);
        focusMeta.setText(
                formatTime(current.start) + " – " + formatTime(current.end)
                        + formatLocationAndBatch(current)
        );
        focusMinutes.setText(formatCountdown(Math.max(0, toSeconds(current.end) - nowSeconds)));

        nextPanel.setVisibility(next == null ? View.GONE : View.VISIBLE);
        if (next != null) {
            nextSubject.setText(next.subject);
            nextMeta.setText(
                    formatTime(next.start) + " – " + formatTime(next.end)
                            + formatLocationAndBatch(next)
            );
            nextCountdown.setText(
                    "Starts in " + formatCountdown(Math.max(0, toSeconds(next.start) - nowSeconds))
            );
        }
    }

    private void updateNextHero(
            TimetableData.ClassItem next,
            boolean viewingToday,
            int nowSeconds
    ) {
        heroCard.setBackground(round(SURFACE, BORDER, 21));
        focusLabel.setText(viewingToday ? "NEXT LECTURE" : "SELECTED DAY");
        focusSubject.setText(next.subject);
        focusMeta.setText(
                formatTime(next.start) + " – " + formatTime(next.end)
                        + formatLocationAndBatch(next)
        );
        focusMinutes.setText(viewingToday
                ? formatCountdown(Math.max(0, toSeconds(next.start) - nowSeconds))
                : "");
        nextPanel.setVisibility(View.GONE);
    }

    private void updateFinishedHero(boolean viewingToday) {
        heroCard.setBackground(round(SURFACE, BORDER, 21));
        focusLabel.setText(viewingToday ? "DAY COMPLETE" : "NO MORE CLASSES");
        focusSubject.setText(viewingToday ? "You're done for today 🎉" : "No more classes");
        focusMeta.setText(selectedDay + " · " + getBatch());
        focusMinutes.setText("");
        nextPanel.setVisibility(View.GONE);
    }

    private String formatLocationAndBatch(TimetableData.ClassItem item) {
        StringBuilder result = new StringBuilder();
        if (!item.room.isEmpty()) result.append("  ·  ").append(item.room);
        if (!item.batch.isEmpty()) result.append("  ·  ").append(item.batch);
        if (!item.teacher.isEmpty()) result.append("  ·  ").append(item.teacher);
        return result.toString();
    }

    private LinearLayout classCard(
            TimetableData.ClassItem item,
            boolean isCurrent,
            boolean isNext,
            boolean isDone
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(13), dp(13), dp(13), dp(13));

        int fill = (isCurrent || isNext) ? SURFACE_2 : SURFACE;
        int stroke = isCurrent ? ACCENT : ((isCurrent || isNext) ? ACCENT : BORDER);
        card.setBackground(round(fill, stroke, 16));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(params);

        TextView time = text(formatTime(item.start), 11, isDone ? MUTED : TEXT);
        time.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(time, new LinearLayout.LayoutParams(dp(68), -2));

        TextView type = text(typeLetter(item), 9, TEXT);
        type.setGravity(Gravity.CENTER);
        type.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        type.setBackground(round(SURFACE_2, BORDER, 8));
        LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(dp(28), dp(28));
        typeParams.setMargins(0, 0, dp(10), 0);
        card.addView(type, typeParams);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);

        LinearLayout titleLine = new LinearLayout(this);
        titleLine.setOrientation(LinearLayout.HORIZONTAL);
        titleLine.setGravity(Gravity.CENTER_VERTICAL);

        TextView subject = text(item.subject, 12, isDone ? MUTED : TEXT);
        subject.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleLine.addView(subject, new LinearLayout.LayoutParams(0, -2, 1f));

        if (isCurrent || isNext) {
            TextView badge = text(isCurrent ? "NOW" : "NEXT", 8, isCurrent ? ACCENT : MUTED);
            badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            badge.setPadding(dp(5), dp(4), dp(5), dp(4));
            badge.setBackground(round(isCurrent ? ACCENT_BG : SURFACE_2, isCurrent ? ACCENT : BORDER, 999));
            titleLine.addView(badge);
        }

        main.addView(titleLine);

        StringBuilder detailsText = new StringBuilder();
        if (!item.code.isEmpty()) detailsText.append(item.code);
        if (!item.teacher.isEmpty()) {
            if (detailsText.length() > 0) detailsText.append(" · ");
            detailsText.append(item.teacher);
        }
        if (!item.batch.isEmpty()) {
            if (detailsText.length() > 0) detailsText.append(" · ");
            detailsText.append(item.batch);
        }

        TextView details = text(detailsText.toString(), 9, MUTED);
        details.setPadding(0, dp(4), 0, 0);
        main.addView(details);

        card.addView(main, new LinearLayout.LayoutParams(0, -2, 1f));

        if (!item.room.isEmpty()) {
            TextView room = text(item.room, 9, MUTED);
            room.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            card.addView(room, new LinearLayout.LayoutParams(dp(76), -2));
        }

        card.setAlpha(isDone ? 0.45f : 1f);
        return card;
    }

    private LinearLayout breakCard(TimetableData.ClassItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(9), dp(14), dp(9));
        card.setBackground(round(SURFACE_2, BORDER, 13));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 0, 0, dp(7));
        card.setLayoutParams(p);

        TextView icon = text("·", 18, MUTED);
        card.addView(icon, new LinearLayout.LayoutParams(dp(20), -2));

        TextView time = text(formatTime(item.start) + " – " + formatTime(item.end), 9, MUTED);
        card.addView(time, new LinearLayout.LayoutParams(dp(125), -2));

        TextView label = text(item.subject, 10, MUTED);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(label);
        return card;
    }

    private int calculateOpenGaps(List<TimetableData.ClassItem> entries) {
        int result = 0;
        TimetableData.ClassItem previousAcademic = null;

        for (TimetableData.ClassItem item : entries) {
            if (!item.isAcademic()) continue;

            if (previousAcademic != null) {
                int gap = toSeconds(item.start) - toSeconds(previousAcademic.end);
                if (gap >= 30 * 60) result++;
            }
            previousAcademic = item;
        }
        return result;
    }

    private String typeLetter(TimetableData.ClassItem item) {
        switch (item.kind) {
            case PRACTICAL: return "P";
            case TUTORIAL: return "T";
            case LIBRARY: return "L";
            case SELF_LEARNING: return "S";
            case SPECIAL: return "X";
            default: return "L";
        }
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

    private void refreshDayButtons() {
        View contentRoot = findViewById(android.R.id.content);
        if (!(contentRoot instanceof LinearLayout)) return;

        LinearLayout rootView = (LinearLayout) contentRoot;
        for (int i = 0; i < rootView.getChildCount(); i++) {
            View child = rootView.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout outer = (LinearLayout) child;
                for (int j = 0; j < outer.getChildCount(); j++) {
                    View nested = outer.getChildAt(j);
                    if (nested instanceof HorizontalScrollView) {
                        HorizontalScrollView scroll = (HorizontalScrollView) nested;
                        if (scroll.getChildCount() > 0 && scroll.getChildAt(0) instanceof LinearLayout) {
                            highlightDayButtons((LinearLayout) scroll.getChildAt(0));
                            return;
                        }
                    }
                }
            }
        }
    }

    private void updateLiveHeader() {
        if (liveClock == null || liveDate == null) return;
        liveClock.setText(LocalTime.now(zone).format(clockFormatter));
        liveDate.setText(LocalDate.now(zone).format(dateFormatter));
    }

    private int toSeconds(String value) {
        return LocalTime.parse(value, timeFormatter).toSecondOfDay();
    }

    private int nowSeconds() {
        return LocalTime.now(zone).toSecondOfDay();
    }

    private String formatCountdown(int totalSeconds) {
        int safe = Math.max(0, totalSeconds);
        int hours = safe / 3600;
        int minutes = (safe % 3600) / 60;
        int seconds = safe % 60;
        if (hours > 0) return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String formatTime(String value) {
        LocalTime t = LocalTime.parse(value, timeFormatter);
        int h = t.getHour();
        String suffix = h >= 12 ? "PM" : "AM";
        int hour = h % 12 == 0 ? 12 : h % 12;
        return hour + ":" + String.format("%02d", t.getMinute()) + " " + suffix;
    }

    private String todayName() {
        DayOfWeek day = LocalDate.now(zone).getDayOfWeek();
        switch (day) {
            case MONDAY: return "Monday";
            case TUESDAY: return "Tuesday";
            case WEDNESDAY: return "Wednesday";
            case THURSDAY: return "Thursday";
            case FRIDAY: return "Friday";
            default: return "";
        }
    }

    private String todayNameOrMonday() {
        String today = todayName();
        return today.isEmpty() ? "Monday" : today;
    }

    private LinearLayout page() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setPadding(dp(24), dp(24), dp(24), dp(24));
        page.setBackgroundColor(BG);
        return page;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(round(SURFACE, BORDER, 18));
        return card;
    }

    private EditText field(String hint, boolean number) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(number
                ? InputType.TYPE_CLASS_NUMBER
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setTextSize(16);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setHint(hint);
        input.setPadding(dp(13), dp(8), dp(13), dp(8));
        input.setBackground(round(SURFACE_2, BORDER, 12));
        return input;
    }

    private LinearLayout.LayoutParams fieldParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(50));
        params.setMargins(0, dp(7), 0, 0);
        return params;
    }

    private TextView sectionLabel(String value) {
        TextView t = text(value, 9, MUTED);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setLetterSpacing(0.13f);
        return t;
    }

    private Button primaryButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(13);
        b.setTextColor(BG);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setBackground(round(ACCENT, ACCENT, 13));
        return b;
    }

    private Button secondaryButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(11);
        b.setTextColor(TEXT);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setBackground(round(SURFACE_2, BORDER, 11));
        return b;
    }

    private LinearLayout statBlock() {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        return block;
    }

    private TextView statValue(LinearLayout block, String value) {
        TextView t = text(value, 21, TEXT);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        block.addView(t);
        return t;
    }

    private void statLabel(LinearLayout block, String value) {
        TextView t = text(value, 9, MUTED);
        t.setPadding(0, dp(2), 0, 0);
        block.addView(t);
    }

    private void footer(LinearLayout body) {
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(0, dp(22), 0, 0);

        TextView left = text("NEXTBELL · " + THEME_NAMES[themeIndex].toUpperCase(), 8, MUTED);
        footer.addView(left, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView right = text("FE DIV B · 2026–27", 8, MUTED);
        right.setGravity(Gravity.END);
        footer.addView(right);

        body.addView(footer);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
