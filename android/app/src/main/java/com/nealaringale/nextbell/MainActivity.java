package com.nealaringale.nextbell;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS_NAME = "nextbell_profile";
    private static final String KEY_NAME = "name";
    private static final String KEY_ROLL_NUMBER = "roll_number";
    private static final String KEY_NOTIFICATION_PROMPTED = "notification_prompted";
    private static final String KEY_THEME = "theme";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH);
    private final DateTimeFormatter clockFormatter =
            DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH);
    private final ZoneId zone = ZoneId.of("Asia/Kolkata");

    private SharedPreferences preferences;
    private FrameLayout root;
    private FrameLayout contentHost;
    private LinearLayout bottomNav;

    private TextView liveClock;
    private TextView liveDate;
    private TextView greetingText;
    private TextView profileBadge;

    private LinearLayout homeHero;
    private TextView heroLabel;
    private TextView heroSubject;
    private TextView heroMeta;
    private TextView heroCountdown;
    private TextView heroCountdownLabel;
    private LinearLayout homeSchedule;
    private TextView homeDayLabel;

    private LinearLayout weekSchedule;
    private TextView weekDayTitle;
    private String weekSelectedDay;

    private int themeIndex;
    private String selectedScreen = "home";
    private Runnable refreshRunnable;

    private static final String[] THEME_NAMES = {
            "Midnight", "Ocean", "Sakura", "Forest", "Solar"
    };

    private static final int[][] THEMES = {
            {Color.rgb(10, 12, 17), Color.rgb(17, 20, 27), Color.rgb(24, 28, 37), Color.rgb(42, 48, 59), Color.rgb(245, 247, 251), Color.rgb(148, 157, 175), Color.rgb(112, 233, 130), Color.rgb(31, 58, 39)},
            {Color.rgb(7, 13, 24), Color.rgb(12, 22, 37), Color.rgb(18, 32, 51), Color.rgb(32, 54, 76), Color.rgb(241, 248, 255), Color.rgb(151, 174, 201), Color.rgb(88, 201, 255), Color.rgb(17, 52, 72)},
            {Color.rgb(20, 12, 20), Color.rgb(31, 18, 32), Color.rgb(43, 23, 44), Color.rgb(66, 38, 66), Color.rgb(255, 246, 252), Color.rgb(187, 158, 181), Color.rgb(255, 132, 193), Color.rgb(70, 31, 56)},
            {Color.rgb(8, 16, 14), Color.rgb(13, 26, 22), Color.rgb(20, 38, 31), Color.rgb(38, 61, 50), Color.rgb(239, 251, 245), Color.rgb(153, 185, 170), Color.rgb(101, 220, 163), Color.rgb(24, 65, 47)},
            {Color.rgb(18, 15, 10), Color.rgb(29, 23, 14), Color.rgb(42, 32, 17), Color.rgb(70, 53, 28), Color.rgb(255, 250, 238), Color.rgb(193, 177, 145), Color.rgb(255, 184, 77), Color.rgb(78, 53, 18)}
    };

    private int BG;
    private int SURFACE;
    private int SURFACE_2;
    private int BORDER;
    private int TEXT;
    private int MUTED;
    private int SUBTLE;
    private int ACCENT;
    private int ACCENT_BG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        themeIndex = clampTheme(preferences.getInt(KEY_THEME, 0));
        applyTheme();

        if (!hasSavedProfile()) {
            showSetupScreen();
        } else {
            selectedScreen = "home";
            weekSelectedDay = todayNameOrMonday();
            openApp();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (refreshRunnable != null) {
            handler.removeCallbacks(refreshRunnable);
        }
    }

    @Override
    public void onBackPressed() {
        if (!"home".equals(selectedScreen)) {
            navigate("home");
            return;
        }
        super.onBackPressed();
    }

    private int clampTheme(int value) {
        return Math.max(0, Math.min(THEME_NAMES.length - 1, value));
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
        SUBTLE = blend(MUTED, BG, 0.55f);
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
        buildShell();
        navigate(selectedScreen);
        requestNotificationPermissionIfNeeded();

        if (refreshRunnable != null) {
            handler.removeCallbacks(refreshRunnable);
        }

        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshLiveUi();
                handler.postDelayed(this, 1_000);
            }
        };
        handler.postDelayed(refreshRunnable, 1_000);
    }

    private void buildShell() {
        root = new FrameLayout(this);
        root.setBackgroundColor(BG);

        contentHost = new FrameLayout(this);
        FrameLayout.LayoutParams contentParams =
                new FrameLayout.LayoutParams(-1, -1);
        contentParams.bottomMargin = dp(76);
        root.addView(contentHost, contentParams);

        bottomNav = buildBottomNav();
        FrameLayout.LayoutParams navParams =
                new FrameLayout.LayoutParams(-1, dp(76), Gravity.BOTTOM);
        root.addView(bottomNav, navParams);

        setContentView(root);
    }

    private LinearLayout buildBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(12), dp(9), dp(12), dp(9));
        nav.setBackground(round(SURFACE, BORDER, 24));

        nav.addView(navItem("home", "HOME"), navWeight());
        nav.addView(navItem("week", "WEEK"), navWeight());
        nav.addView(navItem("settings", "SETTINGS"), navWeight());

        return nav;
    }

    private LinearLayout.LayoutParams navWeight() {
        return new LinearLayout.LayoutParams(0, -1, 1f);
    }

    private TextView navItem(String id, String label) {
        TextView item = text(label, 10, MUTED);
        item.setGravity(Gravity.CENTER);
        item.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        item.setLetterSpacing(0.08f);
        item.setPadding(dp(8), dp(10), dp(8), dp(10));
        item.setClickable(true);
        item.setFocusable(true);
        item.setContentDescription(label);
        item.setOnClickListener(v -> navigate(id));
        return item;
    }

    private void updateBottomNav() {
        if (bottomNav == null) return;
        for (int i = 0; i < bottomNav.getChildCount(); i++) {
            TextView item = (TextView) bottomNav.getChildAt(i);
            String id = i == 0 ? "home" : (i == 1 ? "week" : "settings");
            boolean selected = id.equals(selectedScreen);
            item.setTextColor(selected ? TEXT : MUTED);
            item.setTypeface(
                    Typeface.create("sans-serif-medium",
                            selected ? Typeface.BOLD : Typeface.NORMAL)
            );
            item.setBackground(
                    round(selected ? ACCENT_BG : Color.TRANSPARENT,
                            selected ? ACCENT : Color.TRANSPARENT,
                            18)
            );
        }
    }

    private void navigate(String screen) {
        if (root == null) return;

        selectedScreen = screen;
        if ("week".equals(screen)) {
            String today = todayName();
            if (weekSelectedDay == null || weekSelectedDay.isEmpty()) {
                weekSelectedDay = today.isEmpty() ? "Monday" : today;
            }
        }

        if ("home".equals(screen)) {
            buildHomeScreen();
        } else if ("week".equals(screen)) {
            buildWeekScreen();
        } else {
            buildSettingsScreen();
        }

        updateBottomNav();
        refreshLiveUi();
    }

    private void showSetupScreen() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setPadding(dp(24), dp(28), dp(24), dp(28));
        page.setBackgroundColor(BG);

        LinearLayout logo = new LinearLayout(this);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(round(ACCENT, ACCENT, 20));
        TextView logoText = text("NB", 22, BG);
        logoText.setTypeface(Typeface.DEFAULT_BOLD);
        logo.addView(logoText);

        LinearLayout.LayoutParams logoParams =
                new LinearLayout.LayoutParams(dp(68), dp(68));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        page.addView(logo, logoParams);

        TextView title = text("Welcome to NextBell", 29, TEXT);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(18), 0, 0);
        page.addView(title);

        TextView subtitle = text(
                "Your college day, without the mental math.",
                13,
                MUTED
        );
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(8), 0, dp(26));
        page.addView(subtitle);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setBackground(round(SURFACE, BORDER, 22));

        card.addView(sectionLabel("YOUR NAME"));
        EditText nameInput = field("e.g. Neal", false);
        LinearLayout.LayoutParams fieldParams1 = fieldParams();
        fieldParams1.setMargins(0, dp(8), 0, 0);
        card.addView(nameInput, fieldParams1);

        TextView rollLabel = sectionLabel("ROLL NUMBER");
        LinearLayout.LayoutParams rollLabelParams =
                new LinearLayout.LayoutParams(-1, -2);
        rollLabelParams.setMargins(0, dp(18), 0, 0);
        card.addView(rollLabel, rollLabelParams);

        EditText rollInput = field("e.g. 34", true);
        LinearLayout.LayoutParams fieldParams2 = fieldParams();
        fieldParams2.setMargins(0, dp(8), 0, 0);
        card.addView(rollInput, fieldParams2);

        TextView batchHint = text(
                "B1  1–25     B2  26–50     B3  51+",
                10,
                MUTED
        );
        batchHint.setGravity(Gravity.CENTER);
        batchHint.setPadding(0, dp(9), 0, dp(18));
        card.addView(batchHint);

        TextView continueButton = actionButton("Continue", true);
        card.addView(continueButton, new LinearLayout.LayoutParams(-1, dp(50)));

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
                weekSelectedDay = todayNameOrMonday();
                selectedScreen = "home";
                openApp();
            } catch (NumberFormatException e) {
                rollInput.setError("Enter your roll number.");
            }
        });

        page.addView(card, new LinearLayout.LayoutParams(-1, -2));

        TextView privacy = text(
                "Saved locally on this phone.",
                10,
                MUTED
        );
        privacy.setGravity(Gravity.CENTER);
        privacy.setPadding(0, dp(14), 0, 0);
        page.addView(privacy);

        setContentView(page);
    }

    private void buildHomeScreen() {
        LinearLayout page = scrollPage();

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        top.addView(titleBlock, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView brand = text("NextBell", 25, TEXT);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        titleBlock.addView(brand);

        greetingText = text(
                greeting() + ", " + getName(),
                13,
                MUTED
        );
        greetingText.setPadding(0, dp(4), 0, 0);
        titleBlock.addView(greetingText);

        LinearLayout liveBlock = new LinearLayout(this);
        liveBlock.setOrientation(LinearLayout.VERTICAL);
        liveBlock.setGravity(Gravity.END);

        liveClock = text("--:--", 12, TEXT);
        liveClock.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        liveBlock.addView(liveClock);

        liveDate = text("--", 9, MUTED);
        liveDate.setGravity(Gravity.END);
        liveDate.setPadding(0, dp(2), 0, 0);
        liveBlock.addView(liveDate);

        TextView avatar = avatarText();
        LinearLayout.LayoutParams avatarParams =
                new LinearLayout.LayoutParams(dp(44), dp(44));
        avatarParams.setMargins(dp(12), 0, 0, 0);
        top.addView(liveBlock);
        top.addView(avatar, avatarParams);

        page.addView(top);

        profileBadge = text(
                getBatch() + "  •  Roll " + getRollNumber(),
                10,
                ACCENT
        );
        profileBadge.setGravity(Gravity.CENTER);
        profileBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        profileBadge.setPadding(dp(12), dp(7), dp(12), dp(7));
        profileBadge.setBackground(round(ACCENT_BG, Color.TRANSPARENT, 999));

        LinearLayout.LayoutParams badgeParams =
                new LinearLayout.LayoutParams(-2, -2);
        badgeParams.setMargins(0, dp(12), 0, dp(18));
        page.addView(profileBadge, badgeParams);

        homeHero = new LinearLayout(this);
        homeHero.setOrientation(LinearLayout.VERTICAL);
        homeHero.setPadding(dp(20), dp(20), dp(20), dp(20));
        LinearLayout.LayoutParams heroParams =
                new LinearLayout.LayoutParams(-1, -2);
        heroParams.setMargins(0, 0, 0, dp(16));
        page.addView(homeHero, heroParams);

        heroLabel = sectionLabel("NEXT CLASS");
        heroLabel.setTextColor(ACCENT);
        homeHero.addView(heroLabel);

        heroSubject = text("Loading…", 28, TEXT);
        heroSubject.setTypeface(Typeface.DEFAULT_BOLD);
        heroSubject.setPadding(0, dp(8), 0, 0);
        homeHero.addView(heroSubject);

        heroMeta = text("", 11, MUTED);
        heroMeta.setPadding(0, dp(7), 0, 0);
        homeHero.addView(heroMeta);

        LinearLayout heroLocation = new LinearLayout(this);
        heroLocation.setOrientation(LinearLayout.HORIZONTAL);
        heroLocation.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams heroLocationParams =
                new LinearLayout.LayoutParams(-1, dp(62));
        heroLocationParams.setMargins(0, dp(14), 0, 0);

        TextView heroRoom = text("ROOM\n—", 11, TEXT);
        heroRoom.setGravity(Gravity.CENTER_VERTICAL);
        heroRoom.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        heroRoom.setPadding(dp(12), 0, dp(12), 0);
        heroRoom.setBackground(round(SURFACE_2, Color.TRANSPARENT, 16));
        heroRoom.setContentDescription("Classroom");

        TextView heroWing = text("WING\n—", 11, ACCENT);
        heroWing.setGravity(Gravity.CENTER_VERTICAL);
        heroWing.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        heroWing.setPadding(dp(12), 0, dp(12), 0);
        heroWing.setBackground(round(ACCENT_BG, Color.TRANSPARENT, 16));
        heroWing.setContentDescription("College wing");

        LinearLayout.LayoutParams locationHalf =
                new LinearLayout.LayoutParams(0, -1, 1f);
        heroLocation.addView(heroRoom, locationHalf);

        LinearLayout.LayoutParams wingParams =
                new LinearLayout.LayoutParams(0, -1, 1f);
        wingParams.setMargins(dp(8), 0, 0, 0);
        heroLocation.addView(heroWing, wingParams);

        homeHero.addView(heroLocation, heroLocationParams);

        LinearLayout countdownRow = new LinearLayout(this);
        countdownRow.setGravity(Gravity.BOTTOM | Gravity.CENTER_VERTICAL);
        countdownRow.setPadding(0, dp(18), 0, 0);

        heroCountdown = text("--:--", 36, TEXT);
        heroCountdown.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        countdownRow.addView(heroCountdown, new LinearLayout.LayoutParams(0, -2, 1f));

        heroCountdownLabel = text(
                "until it starts",
                10,
                MUTED
        );
        heroCountdownLabel.setGravity(Gravity.END | Gravity.BOTTOM);
        countdownRow.addView(heroCountdownLabel);

        homeHero.addView(countdownRow);

        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        quickRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView todayChip = chipButton("TODAY", ACCENT_BG, ACCENT);
        quickRow.addView(todayChip, new LinearLayout.LayoutParams(dp(86), dp(38)));

        TextView viewWeek = chipButton("VIEW WEEK  →", SURFACE_2, TEXT);
        LinearLayout.LayoutParams weekButtonParams =
                new LinearLayout.LayoutParams(0, dp(38), 1f);
        weekButtonParams.setMargins(dp(8), 0, 0, 0);
        quickRow.addView(viewWeek, weekButtonParams);
        viewWeek.setOnClickListener(v -> navigate("week"));

        LinearLayout.LayoutParams quickParams =
                new LinearLayout.LayoutParams(-1, -2);
        quickParams.setMargins(0, 0, 0, dp(18));
        page.addView(quickRow, quickParams);

        LinearLayout sectionHeader = new LinearLayout(this);
        sectionHeader.setGravity(Gravity.CENTER_VERTICAL);

        homeDayLabel = text("TODAY", 13, TEXT);
        homeDayLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        sectionHeader.addView(homeDayLabel, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView smallBatch = text(getBatch(), 10, MUTED);
        sectionHeader.addView(smallBatch);

        page.addView(sectionHeader);

        TextView helper = text(
                "Your full day, in time order.",
                10,
                MUTED
        );
        helper.setPadding(0, dp(4), 0, dp(11));
        page.addView(helper);

        homeSchedule = new LinearLayout(this);
        homeSchedule.setOrientation(LinearLayout.VERTICAL);
        page.addView(homeSchedule);

        addBottomSpace(page);
    }

    private void buildWeekScreen() {
        LinearLayout page = scrollPage();

        TextView title = text("Your week", 27, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(title);

        TextView subtitle = text(
                getBatch() + "  •  Roll " + getRollNumber(),
                11,
                MUTED
        );
        subtitle.setPadding(0, dp(5), 0, dp(18));
        page.addView(subtitle);

        HorizontalScrollView dayScroller = new HorizontalScrollView(this);
        dayScroller.setHorizontalScrollBarEnabled(false);
        dayScroller.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout dayRow = new LinearLayout(this);
        dayRow.setOrientation(LinearLayout.HORIZONTAL);

        for (String day : TimetableData.DAYS) {
            TextView chip = dayChip(day);
            dayRow.addView(chip, new LinearLayout.LayoutParams(dp(78), dp(46)));
            if (!day.equals(TimetableData.DAYS[TimetableData.DAYS.length - 1])) {
                dayRow.addView(spacer(dp(8), dp(1)));
            }
        }

        dayScroller.addView(dayRow);
        LinearLayout.LayoutParams scrollerParams =
                new LinearLayout.LayoutParams(-1, dp(46));
        scrollerParams.setMargins(0, 0, 0, dp(19));
        page.addView(dayScroller, scrollerParams);

        weekDayTitle = text(weekSelectedDay.toUpperCase(), 12, MUTED);
        weekDayTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        weekDayTitle.setLetterSpacing(0.1f);
        page.addView(weekDayTitle);

        TextView dayHelper = text(
                "Tap another day above to browse your timetable.",
                10,
                MUTED
        );
        dayHelper.setPadding(0, dp(4), 0, dp(12));
        page.addView(dayHelper);

        weekSchedule = new LinearLayout(this);
        weekSchedule.setOrientation(LinearLayout.VERTICAL);
        page.addView(weekSchedule);

        addBottomSpace(page);

        updateWeekDayChips(dayRow);
        refreshWeekSchedule();
    }

    private void buildSettingsScreen() {
        LinearLayout page = scrollPage();

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView back = text("‹", 34, TEXT);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("Back");
        back.setClickable(true);
        back.setFocusable(true);
        back.setBackground(round(SURFACE_2, Color.TRANSPARENT, 16));
        back.setOnClickListener(v -> navigate("home"));
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = text("Settings", 27, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(0, -2, 1f);
        titleParams.setMargins(dp(10), 0, 0, 0);
        header.addView(title, titleParams);

        page.addView(header);

        TextView subtitle = text(
                "Make NextBell feel like yours.",
                12,
                MUTED
        );
        subtitle.setPadding(dp(58), dp(2), 0, dp(22));
        page.addView(subtitle);

        page.addView(sectionLabel("PROFILE"));

        LinearLayout profileCard = settingCard();
        TextView initials = avatarText();
        profileCard.addView(initials, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout profileText = new LinearLayout(this);
        profileText.setOrientation(LinearLayout.VERTICAL);
        TextView name = text(getName(), 15, TEXT);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        profileText.addView(name);

        TextView info = text(
                getBatch() + "  •  Roll " + getRollNumber(),
                10,
                MUTED
        );
        info.setPadding(0, dp(3), 0, 0);
        profileText.addView(info);

        LinearLayout.LayoutParams profileTextParams =
                new LinearLayout.LayoutParams(0, -2, 1f);
        profileTextParams.setMargins(dp(12), 0, dp(8), 0);
        profileCard.addView(profileText, profileTextParams);

        TextView edit = actionButton("Edit", false);
        profileCard.addView(edit, new LinearLayout.LayoutParams(dp(72), dp(40)));
        edit.setOnClickListener(v -> showEditProfile());

        page.addView(profileCard, cardMargin());

        page.addView(sectionLabel("APPEARANCE"));

        TextView appearanceHint = text(
                "Choose a visual style. It stays saved on this phone.",
                10,
                MUTED
        );
        appearanceHint.setPadding(0, dp(5), 0, dp(11));
        page.addView(appearanceHint);

        LinearLayout themes = new LinearLayout(this);
        themes.setOrientation(LinearLayout.VERTICAL);
        page.addView(themes);

        for (int i = 0; i < THEME_NAMES.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            row.addView(themeCard(i), new LinearLayout.LayoutParams(0, dp(94), 1f));

            if (i + 1 < THEME_NAMES.length) {
                View gap = spacer(dp(9), 1);
                row.addView(gap);
                row.addView(themeCard(i + 1), new LinearLayout.LayoutParams(0, dp(94), 1f));
            }

            LinearLayout.LayoutParams rowParams =
                    new LinearLayout.LayoutParams(-1, dp(94));
            rowParams.setMargins(0, 0, 0, dp(9));
            themes.addView(row, rowParams);
        }

        page.addView(sectionLabel("NOTIFICATIONS"));

        LinearLayout notificationCard = settingCard();
        TextView notificationDot = text(
                isNotificationGranted() ? "●" : "○",
                20,
                isNotificationGranted() ? ACCENT : MUTED
        );
        notificationCard.addView(
                notificationDot,
                new LinearLayout.LayoutParams(dp(40), dp(40))
        );

        LinearLayout notifText = new LinearLayout(this);
        notifText.setOrientation(LinearLayout.VERTICAL);

        TextView notifTitle = text(
                "Class reminders",
                14,
                TEXT
        );
        notifTitle.setTypeface(Typeface.DEFAULT_BOLD);
        notifText.addView(notifTitle);

        TextView notifDescription = text(
                isNotificationGranted()
                        ? "15 minutes before your personalized classes."
                        : "Tap to enable 15-minute class reminders.",
                10,
                MUTED
        );
        notifDescription.setPadding(0, dp(3), 0, 0);
        notifText.addView(notifDescription);

        notificationCard.addView(
                notifText,
                new LinearLayout.LayoutParams(0, -2, 1f)
        );

        if (!isNotificationGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            TextView enable = actionButton("Enable", false);
            notificationCard.addView(
                    enable,
                    new LinearLayout.LayoutParams(dp(82), dp(40))
            );
            enable.setOnClickListener(v -> requestNotificationAccess());
        }

        page.addView(notificationCard, cardMargin());

        page.addView(sectionLabel("ABOUT"));

        LinearLayout aboutCard = settingCard();
        LinearLayout aboutText = new LinearLayout(this);
        aboutText.setOrientation(LinearLayout.VERTICAL);

        TextView aboutTitle = text("NextBell", 14, TEXT);
        aboutTitle.setTypeface(Typeface.DEFAULT_BOLD);
        aboutText.addView(aboutTitle);

        TextView aboutSubtitle = text(
                "FE Division B  •  Academic Year 2026–27",
                10,
                MUTED
        );
        aboutSubtitle.setPadding(0, dp(3), 0, 0);
        aboutText.addView(aboutSubtitle);

        TextView aboutVersion = text(
                "Timetable source: official schedule, W.E.F. 16/09/2026.",
                9,
                SUBTLE
        );
        aboutVersion.setPadding(0, dp(5), 0, dp(12));
        aboutText.addView(aboutVersion);

        TextView creator = text(
                "Made by Neal Aringale",
                11,
                ACCENT
        );
        creator.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        aboutText.addView(creator);

        TextView whatsapp = text(
                "WhatsApp  ·  7499517574",
                11,
                TEXT
        );
        whatsapp.setPadding(0, dp(10), 0, 0);
        whatsapp.setClickable(true);
        whatsapp.setFocusable(true);
        whatsapp.setOnClickListener(v -> openWhatsApp());
        aboutText.addView(whatsapp);

        TextView email = text(
                "Email  ·  nealaringale@gmail.com",
                11,
                TEXT
        );
        email.setPadding(0, dp(8), 0, 0);
        email.setClickable(true);
        email.setFocusable(true);
        email.setOnClickListener(v -> openEmail());
        aboutText.addView(email);

        TextView contactHint = text(
                "Updates, feedback & bug fixes",
                9,
                MUTED
        );
        contactHint.setPadding(0, dp(4), 0, 0);
        aboutText.addView(contactHint);

        aboutCard.addView(aboutText);
        page.addView(aboutCard, cardMargin());

        addBottomSpace(page);
    }

    private LinearLayout scrollPage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(22), dp(20), dp(30));

        scroll.addView(body);
        contentHost.removeAllViews();
        contentHost.addView(scroll);
        return body;
    }

    private void addBottomSpace(LinearLayout page) {
        View spacer = spacer(1, dp(32));
        page.addView(spacer);
    }

    private LinearLayout settingCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(13), dp(14), dp(13));
        card.setBackground(round(SURFACE, Color.TRANSPARENT, 18));
        return card;
    }

    private LinearLayout.LayoutParams cardMargin() {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(9), 0, dp(20));
        return params;
    }

    private TextView themeCard(int index) {
        TextView card = text(
                THEME_NAMES[index] + (index == themeIndex ? "   ✓" : ""),
                12,
                index == themeIndex ? ACCENT : TEXT
        );
        card.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), 0, dp(10), 0);
        card.setBackground(round(
                index == themeIndex ? ACCENT_BG : SURFACE,
                index == themeIndex ? ACCENT : Color.TRANSPARENT,
                18
        ));
        card.setContentDescription(
                "Theme " + THEME_NAMES[index] +
                        (index == themeIndex ? ", selected" : "")
        );
        card.setClickable(true);
        card.setFocusable(true);

        View.OnClickListener listener = v -> {
            if (themeIndex == index) return;
            themeIndex = index;
            preferences.edit().putInt(KEY_THEME, themeIndex).apply();
            applyTheme();

            String keepScreen = selectedScreen;
            buildShell();
            selectedScreen = keepScreen;
            navigate("settings");
        };
        card.setOnClickListener(listener);
        return card;
    }

    private void showEditProfile() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(2), 0, dp(2), 0);

        EditText nameInput = field(getName(), false);
        nameInput.setText(getName());
        nameInput.setSelection(nameInput.length());

        EditText rollInput = field(String.valueOf(getRollNumber()), true);
        rollInput.setText(String.valueOf(getRollNumber()));
        rollInput.setSelection(rollInput.length());

        box.addView(nameInput, fieldParams());
        box.addView(rollInput, fieldParamsWithTop());

        new AlertDialog.Builder(this)
                .setTitle("Edit profile")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String rollRaw = rollInput.getText().toString().trim();

                    if (name.isEmpty()) {
                        name = getName();
                    }

                    try {
                        int roll = Integer.parseInt(rollRaw);
                        if (roll < 1 || roll > 999) {
                            throw new NumberFormatException();
                        }

                        saveProfile(name, roll);
                        weekSelectedDay = todayNameOrMonday();
                        NotificationScheduler.scheduleUpcoming(this);
                        navigate("settings");
                    } catch (NumberFormatException ignored) {
                        rollInput.setError("Enter a valid roll number.");
                    }
                })
                .show();
    }

    private void refreshLiveUi() {
        updateClock();

        if ("home".equals(selectedScreen)) {
            refreshHome();
        } else if ("week".equals(selectedScreen)) {
            refreshWeekSchedule();
        }
    }

    private void refreshHome() {
        if (homeSchedule == null || homeHero == null) return;

        String today = todayName();
        boolean isWeekend = today.isEmpty();
        String day = isWeekend ? "Monday" : today;

        List<TimetableData.ClassItem> entries =
                TimetableData.forRollAndDay(getRollNumber(), day);

        int now = isWeekend ? -1 : nowSeconds();
        TimetableData.ClassItem current = null;
        TimetableData.ClassItem next = null;

        for (TimetableData.ClassItem item : entries) {
            if (!item.isAcademic()) continue;

            int start = toSeconds(item.start);
            int end = toSeconds(item.end);

            if (!isWeekend && start <= now && now < end) {
                current = item;
            } else if (isWeekend && next == null) {
                next = item;
            } else if (!isWeekend && start > now && next == null) {
                next = item;
            }
        }

        if (current != null) {
            homeHero.setBackground(round(SURFACE, ACCENT, 22));
            heroLabel.setText("HAPPENING NOW");
            heroSubject.setText(current.subject);
            heroMeta.setText(formatTeacher(current));
            setHeroLocation(current);
            heroCountdown.setText(
                    formatCountdown(Math.max(0, toSeconds(current.end) - now))
            );
            heroCountdownLabel.setText("until it ends");
        } else if (next != null) {
            homeHero.setBackground(round(SURFACE, Color.TRANSPARENT, 22));
            heroLabel.setText("NEXT CLASS");
            heroSubject.setText(next.subject);
            heroMeta.setText(formatTeacher(next));
            setHeroLocation(next);

            if (isWeekend) {
                heroCountdown.setText(formatTime(next.start));
                heroCountdownLabel.setText(day + " · " + getBatch());
            } else {
                heroCountdown.setText(
                        formatCountdown(Math.max(0, toSeconds(next.start) - now))
                );
                heroCountdownLabel.setText("until it starts");
            }
        } else {
            homeHero.setBackground(round(SURFACE, Color.TRANSPARENT, 22));
            heroLabel.setText("DAY COMPLETE");
            heroSubject.setText("You're done for today.");
            heroMeta.setText("No more classes scheduled");
            clearHeroLocation();
            heroCountdown.setText("✓");
            heroCountdownLabel.setText("nothing else scheduled");
        }

        homeDayLabel.setText(
                isWeekend
                        ? "MONDAY PREVIEW"
                        : "TODAY  ·  " + formatTodayDate()
        );

        profileBadge.setText(getBatch() + "  •  Roll " + getRollNumber());

        homeSchedule.removeAllViews();
        for (TimetableData.ClassItem item : entries) {
            boolean isCurrent = current != null && current.id.equals(item.id);
            boolean isDone = !isWeekend
                    && item.isAcademic()
                    && toSeconds(item.end) <= now;

            if (item.isBreak()) {
                homeSchedule.addView(breakRow(item));
            } else {
                homeSchedule.addView(classRow(item, isCurrent, false, isDone));
            }
        }
    }

    private void refreshWeekSchedule() {
        if (weekSchedule == null) return;

        List<TimetableData.ClassItem> entries =
                TimetableData.forRollAndDay(getRollNumber(), weekSelectedDay);

        weekSchedule.removeAllViews();

        String today = todayName();
        boolean viewingToday = weekSelectedDay.equals(today) && !today.isEmpty();
        int now = viewingToday ? nowSeconds() : -1;

        TimetableData.ClassItem current = null;
        if (viewingToday) {
            for (TimetableData.ClassItem item : entries) {
                if (!item.isAcademic()) continue;
                int start = toSeconds(item.start);
                int end = toSeconds(item.end);
                if (start <= now && now < end) {
                    current = item;
                    break;
                }
            }
        }

        if (entries.isEmpty()) {
            TextView empty = text(
                    "No classes scheduled.",
                    13,
                    MUTED
            );
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(30), 0, dp(30));
            weekSchedule.addView(empty);
            return;
        }

        for (TimetableData.ClassItem item : entries) {
            boolean isCurrent = current != null && current.id.equals(item.id);
            boolean isDone = viewingToday
                    && item.isAcademic()
                    && toSeconds(item.end) <= now;

            weekSchedule.addView(
                    item.isBreak()
                            ? breakRow(item)
                            : classRow(item, isCurrent, false, isDone)
            );
        }
    }

    private void updateWeekDayChips(LinearLayout row) {
        for (int i = 0; i < TimetableData.DAYS.length; i++) {
            String day = TimetableData.DAYS[i];
            TextView chip = (TextView) row.getChildAt(i * 2);
            if (chip == null) continue;

            boolean selected = day.equals(weekSelectedDay);
            chip.setTextColor(selected ? BG : TEXT);
            chip.setTypeface(
                    Typeface.create("sans-serif-medium",
                            selected ? Typeface.BOLD : Typeface.NORMAL)
            );
            chip.setBackground(round(
                    selected ? TEXT : SURFACE,
                    Color.TRANSPARENT,
                    16
            ));

            chip.setOnClickListener(v -> {
                weekSelectedDay = day;
                updateWeekDayChips(row);
                if (weekDayTitle != null) {
                    weekDayTitle.setText(weekSelectedDay.toUpperCase());
                }
                refreshWeekSchedule();
            });
        }
    }

    private TextView dayChip(String day) {
        TextView chip = text(day.substring(0, 3), 11, TEXT);
        chip.setGravity(Gravity.CENTER);
        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setContentDescription("View " + day + " timetable");
        return chip;
    }

    private LinearLayout classRow(
            TimetableData.ClassItem item,
            boolean isCurrent,
            boolean isNext,
            boolean isDone
    ) {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.HORIZONTAL);
        outer.setGravity(Gravity.CENTER_VERTICAL);
        outer.setPadding(0, 0, 0, 0);

        if (isCurrent) {
            View indicator = new View(this);
            indicator.setBackground(round(ACCENT, Color.TRANSPARENT, 999));
            outer.addView(
                    indicator,
                    new LinearLayout.LayoutParams(dp(4), dp(54))
            );
        } else {
            outer.addView(spacer(dp(4), 1));
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(13), dp(12), dp(12), dp(12));
        card.setBackground(round(
                isCurrent ? ACCENT_BG : SURFACE,
                Color.TRANSPARENT,
                18
        ));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(0, -2, 1f);
        cardParams.setMargins(dp(7), 0, 0, dp(8));
        outer.addView(card, cardParams);

        LinearLayout timeBlock = new LinearLayout(this);
        timeBlock.setOrientation(LinearLayout.VERTICAL);

        TextView start = text(
                formatTime(item.start),
                11,
                isDone ? MUTED : TEXT
        );
        start.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        timeBlock.addView(start);

        TextView end = text(
                formatTime(item.end),
                9,
                SUBTLE
        );
        end.setPadding(0, dp(2), 0, 0);
        timeBlock.addView(end);

        card.addView(timeBlock, new LinearLayout.LayoutParams(dp(73), -2));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);

        TextView subject = text(
                item.subject,
                13,
                isDone ? MUTED : TEXT
        );
        subject.setTypeface(Typeface.DEFAULT_BOLD);
        main.addView(subject);

        TextView details = text(
                detailsFor(item),
                9,
                MUTED
        );
        details.setPadding(0, dp(4), 0, 0);
        main.addView(details);

        card.addView(main, new LinearLayout.LayoutParams(0, -2, 1f));

        if (!roomNumber(item).isEmpty() || !wing(item).isEmpty()) {
            LinearLayout locationBlock = new LinearLayout(this);
            locationBlock.setOrientation(LinearLayout.VERTICAL);
            locationBlock.setGravity(Gravity.END);

            TextView room = text(
                    roomNumber(item).isEmpty() ? "—" : roomNumber(item),
                    11,
                    TEXT
            );
            room.setGravity(Gravity.END);
            room.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            locationBlock.addView(room);

            TextView wingText = text(
                    wing(item).isEmpty() ? "Wing —" : wing(item),
                    8,
                    ACCENT
            );
            wingText.setGravity(Gravity.END);
            wingText.setPadding(0, dp(2), 0, 0);
            locationBlock.addView(wingText);

            LinearLayout.LayoutParams locationParams =
                    new LinearLayout.LayoutParams(dp(68), -2);
            locationParams.setMargins(dp(6), 0, 0, 0);
            card.addView(locationBlock, locationParams);
        }

        if (isCurrent) {
            TextView now = text("NOW", 8, ACCENT);
            now.setGravity(Gravity.CENTER);
            now.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            now.setPadding(dp(6), dp(5), dp(6), dp(5));
            now.setBackground(round(ACCENT_BG, ACCENT, 999));

            LinearLayout.LayoutParams nowParams =
                    new LinearLayout.LayoutParams(-2, dp(26));
            nowParams.setMargins(dp(7), 0, 0, 0);
            card.addView(now, nowParams);
        }

        card.setAlpha(isDone ? 0.58f : 1f);
        return outer;
    }

    private LinearLayout breakRow(TimetableData.ClassItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(5), dp(5), dp(5), dp(5));

        TextView dash = text("—", 13, MUTED);
        row.addView(dash, new LinearLayout.LayoutParams(dp(34), -2));

        TextView time = text(
                formatTime(item.start) + " – " + formatTime(item.end),
                9,
                MUTED
        );
        row.addView(time, new LinearLayout.LayoutParams(dp(122), -2));

        TextView label = text(item.subject, 10, MUTED);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        row.addView(label);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(5));
        row.setLayoutParams(params);
        return row;
    }

    private String detailsFor(TimetableData.ClassItem item) {
        StringBuilder builder = new StringBuilder();
        if (!item.code.isEmpty()) builder.append(item.code);
        if (!item.teacher.isEmpty()) {
            if (builder.length() > 0) builder.append("  •  ");
            builder.append(item.teacher);
        }
        if (!item.batch.isEmpty()) {
            if (builder.length() > 0) builder.append("  •  ");
            builder.append(item.batch);
        }
        return builder.toString();
    }

    private String formatLocation(TimetableData.ClassItem item) {
        StringBuilder builder = new StringBuilder();
        if (!roomNumber(item).isEmpty()) builder.append("  •  Room ").append(roomNumber(item));
        if (!wing(item).isEmpty()) builder.append("  •  ").append(wing(item));
        return builder.toString();
    }

    private String formatTeacher(TimetableData.ClassItem item) {
        StringBuilder builder = new StringBuilder();
        if (!item.teacher.isEmpty()) builder.append(item.teacher);
        if (!item.batch.isEmpty()) {
            if (builder.length() > 0) builder.append("  •  ");
            builder.append(item.batch);
        }
        return builder.toString();
    }

    private void setHeroLocation(TimetableData.ClassItem item) {
        if (homeHero == null || homeHero.getChildCount() < 5) return;

        View locationView = homeHero.getChildAt(4);
        if (!(locationView instanceof LinearLayout)) return;
        LinearLayout row = (LinearLayout) locationView;

        if (row.getChildCount() < 2) return;
        TextView room = (TextView) row.getChildAt(0);
        TextView wing = (TextView) row.getChildAt(1);

        String roomValue = roomNumber(item);
        String wingValue = wing(item);

        room.setText(roomValue.isEmpty()
                ? "ROOM\nNot listed"
                : "ROOM\n" + roomValue);
        wing.setText(wingValue.isEmpty()
                ? "WING\nNot listed"
                : "WING\n" + wingValue);
        room.setTextColor(TEXT);
        wing.setTextColor(ACCENT);
    }

    private void clearHeroLocation() {
        if (homeHero == null || homeHero.getChildCount() < 5) return;

        View locationView = homeHero.getChildAt(4);
        if (!(locationView instanceof LinearLayout)) return;
        LinearLayout row = (LinearLayout) locationView;
        if (row.getChildCount() < 2) return;

        ((TextView) row.getChildAt(0)).setText("ROOM\n—");
        ((TextView) row.getChildAt(1)).setText("WING\n—");
    }

    private String roomNumber(TimetableData.ClassItem item) {
        if (item.room == null || item.room.isEmpty()) return "";

        String value = item.room;
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("\\b(\\d{3})\\b").matcher(value);
        if (matcher.find()) return matcher.group(1);

        return value.startsWith("Library") ? value : "";
    }

    private String wing(TimetableData.ClassItem item) {
        if (item.room == null || item.room.isEmpty()) return "";

        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("Wing\\s+[A-Z]", java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher(item.room);
        if (matcher.find()) return matcher.group().replace("wing", "Wing");

        // The official timetable is headed Wing C; room-only three-digit
        // common classrooms therefore use Wing C as the displayed default.
        if (item.room.matches("\\d{3}")) return "Wing C";
        return "";
    }

    private void openWhatsApp() {
        String phone = "917499517574";
        String message = "Hi Neal, I have feedback about NextBell.";
        Uri uri = Uri.parse(
                "https://wa.me/" + phone + "?text=" + Uri.encode(message)
        );

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(intent);
        } catch (Exception ignored) {
            // The web URL remains the fallback when WhatsApp is unavailable.
        }
    }

    private void openEmail() {
        Uri uri = Uri.parse(
                "mailto:nealaringale@gmail.com?subject=" +
                        Uri.encode("NextBell feedback / bug report")
        );

        Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
        try {
            startActivity(intent);
        } catch (Exception ignored) {
            // No email client installed; keep the screen unchanged.
        }
    }

    private TextView avatarText() {
        String name = getName();
        String initials = name.isEmpty()
                ? "N"
                : name.substring(0, 1).toUpperCase();

        TextView avatar = text(initials, 15, BG);
        avatar.setGravity(Gravity.CENTER);
        avatar.setTypeface(Typeface.DEFAULT_BOLD);
        avatar.setBackground(round(ACCENT, Color.TRANSPARENT, 999));
        avatar.setContentDescription("Profile and settings");
        avatar.setClickable(true);
        avatar.setFocusable(true);
        avatar.setOnClickListener(v -> navigate("settings"));
        return avatar;
    }

    private TextView chipButton(String label, int fill, int color) {
        TextView chip = text(label, 10, color);
        chip.setGravity(Gravity.CENTER);
        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        chip.setLetterSpacing(0.04f);
        chip.setBackground(round(fill, Color.TRANSPARENT, 999));
        chip.setClickable(true);
        chip.setFocusable(true);
        return chip;
    }

    private TextView actionButton(String label, boolean primary) {
        TextView button = text(
                label,
                11,
                primary ? BG : TEXT
        );
        button.setGravity(Gravity.CENTER);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        button.setBackground(round(
                primary ? ACCENT : SURFACE_2,
                Color.TRANSPARENT,
                14
        ));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private TextView sectionLabel(String value) {
        TextView t = text(value, 9, MUTED);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        t.setLetterSpacing(0.13f);
        t.setPadding(0, 0, 0, 0);
        return t;
    }

    private EditText field(String value, boolean number) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(
                number
                        ? InputType.TYPE_CLASS_NUMBER
                        : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );
        input.setTextSize(16);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setHint(value);
        input.setPadding(dp(13), dp(6), dp(13), dp(6));
        input.setBackground(round(SURFACE_2, Color.TRANSPARENT, 14));
        return input;
    }

    private LinearLayout.LayoutParams fieldParams() {
        return new LinearLayout.LayoutParams(-1, dp(50));
    }

    private LinearLayout.LayoutParams fieldParamsWithTop() {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(50));
        params.setMargins(0, dp(9), 0, 0);
        return params;
    }

    private String greetingForHour(int hour) {
        if (hour < 12) return "Good morning";
        if (hour < 18) return "Good afternoon";
        return "Good evening";
    }

    private void updateClock() {
        if (liveClock == null || liveDate == null) return;

        ZonedDateTime now = ZonedDateTime.now(zone);
        liveClock.setText(now.format(clockFormatter));
        liveDate.setText(now.format(dateFormatter));

        if (greetingText != null) {
            greetingText.setText(greetingForHour(now.getHour()) + ", " + getName());
        }
    }

    private boolean isNotificationGranted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            NotificationScheduler.scheduleUpcoming(this);
            return;
        }

        requestPermissions(
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                7001
        );
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            NotificationScheduler.scheduleUpcoming(this);
            return;
        }

        if (isNotificationGranted()) {
            NotificationScheduler.scheduleUpcoming(this);
            return;
        }

        if (preferences.getBoolean(KEY_NOTIFICATION_PROMPTED, false)) {
            return;
        }

        preferences.edit().putBoolean(KEY_NOTIFICATION_PROMPTED, true).apply();

        new AlertDialog.Builder(this)
                .setTitle("Class reminders")
                .setMessage(
                        "NextBell can remind you 15 minutes before your personalized classes."
                )
                .setNegativeButton("Not now", null)
                .setPositiveButton("Enable", (dialog, which) -> {
                    requestNotificationAccess();
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 7001) {
            NotificationScheduler.scheduleUpcoming(this);
            if ("settings".equals(selectedScreen)) {
                buildSettingsScreen();
                updateBottomNav();
            }
        }
    }

    private String formatTodayDate() {
        return LocalDate.now(zone).format(dateFormatter).toUpperCase();
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

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
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

    private View spacer(int widthPx, int heightPx) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(widthPx, heightPx));
        return view;
    }

    private TextView text(String value, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return t;
    }

    private GradientDrawable round(int fill, int stroke, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        if (stroke != Color.TRANSPARENT) {
            d.setStroke(dp(1), stroke);
        }
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private int blend(int a, int b, float amountB) {
        float amount = Math.max(0f, Math.min(1f, amountB));
        int ar = Color.red(a), ag = Color.green(a), ab = Color.blue(a);
        int br = Color.red(b), bg = Color.green(b), bb = Color.blue(b);
        return Color.rgb(
                Math.round(ar + (br - ar) * amount),
                Math.round(ag + (bg - ag) * amount),
                Math.round(ab + (bb - ab) * amount)
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }


}
