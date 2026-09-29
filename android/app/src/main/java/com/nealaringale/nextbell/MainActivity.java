package com.nealaringale.nextbell;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.List;
import java.text.NumberFormat;
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
    private TextView heroRoomText;
    private TextView heroWingText;
    private TextView heroTimeRange;
    private View heroPulseDot;
    private LinearLayout homeSchedule;
    private TextView homeDayLabel;
    private TextView homeSummary;

    private LinearLayout weekSchedule;
    private TextView weekDayTitle;
    private String weekSelectedDay;

    private LinearLayout expenseListHost;
    private TextView expenseMonthTitle;
    private TextView expenseMonthTotal;
    private TextView expenseBudgetLabel;
    private TextView expenseBudgetMeta;
    private TextView expenseTransactionCount;
    private TextView expenseDailyAverage;
    private SpendingChartView expenseChart;
    private LocalDate expenseMonth = LocalDate.now(zone).withDayOfMonth(1);

    private String lastHomeScheduleSignature = "";
    private String lastWeekScheduleSignature = "";
    private int lastScreenIndex = 0;

    private int themeIndex;
    private String selectedScreen = "home";
    private Runnable refreshRunnable;
    private android.animation.ObjectAnimator heroPulseAnimator;
    private String lastHeroState = "";

    private static final String[] THEME_NAMES = {
            "Midnight", "Ocean", "Sakura", "Forest", "Solar"
    };

    private static final String[] THEME_TAGS = {
            "NEO GLASS", "FLOW", "EDITORIAL", "ORGANIC", "STUDIO"
    };

    private static final String[] THEME_DESCRIPTIONS = {
            "Dark glass, luminous edges, soft depth",
            "Cool gradients, fluid controls, crisp focus",
            "Soft editorial cards, refined contrast, calm motion",
            "Natural surfaces, spacious rhythm, grounded controls",
            "Warm studio contrast, sharper geometry, bold hierarchy"
    };

    // BG, SURFACE, SURFACE_2, BORDER, TEXT, MUTED, ACCENT, ACCENT_BG, ACCENT_2
    private static final int[][] THEMES = {
            {Color.rgb(7, 9, 13), Color.rgb(14, 18, 24), Color.rgb(21, 27, 37), Color.rgb(43, 51, 66), Color.rgb(246, 248, 252), Color.rgb(145, 155, 173), Color.rgb(139, 255, 176), Color.rgb(19, 57, 36), Color.rgb(99, 230, 255)},
            {Color.rgb(5, 11, 20), Color.rgb(10, 19, 32), Color.rgb(17, 30, 48), Color.rgb(31, 53, 76), Color.rgb(241, 248, 255), Color.rgb(146, 169, 199), Color.rgb(84, 209, 255), Color.rgb(12, 52, 76), Color.rgb(124, 111, 255)},
            {Color.rgb(19, 10, 18), Color.rgb(31, 17, 29), Color.rgb(45, 23, 42), Color.rgb(69, 38, 64), Color.rgb(255, 247, 252), Color.rgb(188, 158, 181), Color.rgb(255, 130, 194), Color.rgb(77, 29, 58), Color.rgb(255, 194, 102)},
            {Color.rgb(6, 15, 12), Color.rgb(11, 25, 20), Color.rgb(19, 39, 31), Color.rgb(38, 64, 51), Color.rgb(240, 252, 245), Color.rgb(151, 184, 169), Color.rgb(103, 231, 170), Color.rgb(21, 69, 49), Color.rgb(120, 221, 255)},
            {Color.rgb(18, 13, 7), Color.rgb(31, 22, 12), Color.rgb(45, 32, 16), Color.rgb(72, 53, 29), Color.rgb(255, 250, 238), Color.rgb(193, 175, 140), Color.rgb(255, 191, 77), Color.rgb(80, 52, 15), Color.rgb(255, 119, 86)}
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
    private int ACCENT_2;
    private int CARD_RADIUS;
    private int BUTTON_RADIUS;
    private int SMALL_RADIUS;
    private int NAV_RADIUS;
    private int CARD_ELEVATION;
    private int CONTROL_HEIGHT;

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
        if (refreshRunnable != null) handler.removeCallbacks(refreshRunnable);
        stopHeroPulse();
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
        ACCENT_2 = palette[8];

        switch (themeIndex) {
            case 1: // Ocean — flowing, slightly tighter geometry.
                CARD_RADIUS = 21;
                BUTTON_RADIUS = 18;
                SMALL_RADIUS = 15;
                NAV_RADIUS = 24;
                CARD_ELEVATION = 7;
                CONTROL_HEIGHT = 48;
                break;
            case 2: // Sakura — editorial, calmer and less rounded.
                CARD_RADIUS = 16;
                BUTTON_RADIUS = 12;
                SMALL_RADIUS = 11;
                NAV_RADIUS = 18;
                CARD_ELEVATION = 3;
                CONTROL_HEIGHT = 46;
                break;
            case 3: // Forest — organic, roomy, soft geometry.
                CARD_RADIUS = 27;
                BUTTON_RADIUS = 20;
                SMALL_RADIUS = 17;
                NAV_RADIUS = 30;
                CARD_ELEVATION = 5;
                CONTROL_HEIGHT = 50;
                break;
            case 4: // Solar — studio, defined edges, flatter surfaces.
                CARD_RADIUS = 15;
                BUTTON_RADIUS = 10;
                SMALL_RADIUS = 9;
                NAV_RADIUS = 18;
                CARD_ELEVATION = 2;
                CONTROL_HEIGHT = 46;
                break;
            default: // Midnight — neo-glass.
                CARD_RADIUS = 24;
                BUTTON_RADIUS = 15;
                SMALL_RADIUS = 13;
                NAV_RADIUS = 28;
                CARD_ELEVATION = 9;
                CONTROL_HEIGHT = 48;
                break;
        }

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
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(-1, -1);
        contentParams.bottomMargin = dp(94);
        root.addView(contentHost, contentParams);

        bottomNav = buildBottomNav();
        FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(-1, dp(74), Gravity.BOTTOM);
        navParams.setMargins(dp(15), 0, dp(15), dp(12));
        root.addView(bottomNav, navParams);

        setContentView(root);
    }

    private LinearLayout buildBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(7), dp(8), dp(7));
        nav.setBackground(gradientRound(
                SURFACE_2,
                blend(SURFACE_2, BG, 0.45f),
                BORDER,
                NAV_RADIUS,
                GradientDrawable.Orientation.LEFT_RIGHT
        ));
        nav.setElevation(dp(16));

        nav.addView(navItem("home", "Home", R.drawable.ic_home), navWeight());
        nav.addView(navItem("week", "Week", R.drawable.ic_calendar), navWeight());
        nav.addView(navItem("expenses", "Money", R.drawable.ic_wallet), navWeight());
        nav.addView(navItem("settings", "Settings", R.drawable.ic_settings), navWeight());
        return nav;
    }

    private LinearLayout.LayoutParams navWeight() {
        return new LinearLayout.LayoutParams(0, -1, 1f);
    }

    private LinearLayout navItem(String id, String label, int iconRes) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(6), dp(5), dp(6), dp(5));
        item.setClickable(true);
        item.setFocusable(true);
        item.setContentDescription(label);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        item.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        TextView caption = text(label, 9, MUTED);
        caption.setGravity(Gravity.CENTER);
        caption.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        caption.setPadding(0, dp(3), 0, 0);
        item.addView(caption);

        installPressAnimation(item);
        item.setOnClickListener(v -> {
            navigate(id);
        });
        return item;
    }

    private void updateBottomNav() {
        if (bottomNav == null) return;

        for (int i = 0; i < bottomNav.getChildCount(); i++) {
            View child = bottomNav.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;

            String id;
            if (i == 0) {
                id = "home";
            } else if (i == 1) {
                id = "week";
            } else if (i == 2) {
                id = "expenses";
            } else {
                id = "settings";
            }
            boolean selected = id.equals(selectedScreen);
            LinearLayout item = (LinearLayout) child;
            ImageView icon = (ImageView) item.getChildAt(0);
            TextView caption = (TextView) item.getChildAt(1);

            icon.setColorFilter(selected ? ACCENT : MUTED);
            caption.setTextColor(selected ? TEXT : MUTED);
            item.setBackground(ripple(
                    selected ? ACCENT_BG : Color.TRANSPARENT,
                    blend(ACCENT_BG, BG, 0.32f),
                    selected ? ACCENT : Color.TRANSPARENT,
                    Math.max(10, SMALL_RADIUS)
            ));
            item.animate()
                    .scaleX(selected ? 1.03f : 1f)
                    .scaleY(selected ? 1.03f : 1f)
                    .setDuration(170)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
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
            lastHomeScheduleSignature = "";
            buildHomeScreen();
        } else if ("week".equals(screen)) {
            lastWeekScheduleSignature = "";
            buildWeekScreen();
        } else if ("expenses".equals(screen)) {
            buildExpensesScreen();
        } else {
            buildSettingsScreen();
        }

        updateBottomNav();
        boolean forward = screenIndex(screen) >= lastScreenIndex;
        if (screen.equals(selectedScreen) && "home".equals(screen)) forward = false;
        animateScreenTransition(contentHost, forward);
        lastScreenIndex = screenIndex(screen);
        refreshLiveUi();
    }

    private void showSetupScreen() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setPadding(dp(24), dp(28), dp(24), dp(28));
        page.setBackgroundColor(BG);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_nextbell_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        logo.setContentDescription("NextBell logo");

        LinearLayout.LayoutParams logoParams =
                new LinearLayout.LayoutParams(dp(82), dp(82));
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
        animatePageIn(page);
        logo.animate().rotationBy(360f)
                .setDuration(700)
                .setStartDelay(120)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void buildHomeScreen() {
        LinearLayout page = scrollPage();

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        top.addView(titleBlock, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView eyebrow = sectionLabel("SMART TIMETABLE");
        eyebrow.setTextColor(ACCENT);
        titleBlock.addView(eyebrow);

        TextView brand = text("NextBell", themeHeadingSize(), TEXT);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        brand.setPadding(0, dp(3), 0, 0);
        titleBlock.addView(brand);

        greetingText = text(
                greetingForHour(ZonedDateTime.now(zone).getHour()) + ", " + getName(),
                12,
                MUTED
        );
        greetingText.setPadding(0, dp(4), 0, 0);
        titleBlock.addView(greetingText);

        LinearLayout clockBlock = new LinearLayout(this);
        clockBlock.setOrientation(LinearLayout.VERTICAL);
        clockBlock.setGravity(Gravity.END);

        liveClock = text("--:--", 15, TEXT);
        liveClock.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        liveClock.setGravity(Gravity.END);
        clockBlock.addView(liveClock);

        liveDate = text("--", 9, MUTED);
        liveDate.setGravity(Gravity.END);
        liveDate.setPadding(0, dp(3), 0, 0);
        clockBlock.addView(liveDate);

        TextView avatar = avatarText();
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(46), dp(46));
        avatarParams.setMargins(dp(11), 0, 0, 0);
        top.addView(clockBlock);
        top.addView(avatar, avatarParams);
        page.addView(top);

        LinearLayout profileRow = new LinearLayout(this);
        profileRow.setGravity(Gravity.CENTER_VERTICAL);

        profileBadge = text(
                getBatch() + "  •  Roll " + getRollNumber(),
                10,
                ACCENT
        );
        profileBadge.setGravity(Gravity.CENTER);
        profileBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        profileBadge.setPadding(dp(13), dp(7), dp(13), dp(7));
        profileBadge.setBackground(ripple(ACCENT_BG, blend(ACCENT_BG, BG, 0.35f), Color.TRANSPARENT, 999));
        profileBadge.setClickable(true);
        installPressAnimation(profileBadge);
        profileBadge.setOnClickListener(v -> {
            navigate("settings");
        });
        profileRow.addView(profileBadge);

        TextView liveTag = text("●  LIVE • INDIA", 8, SUBTLE);
        liveTag.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        liveTag.setLetterSpacing(0.08f);
        liveTag.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams liveTagParams = new LinearLayout.LayoutParams(0, -2, 1f);
        profileRow.addView(liveTag, liveTagParams);

        LinearLayout.LayoutParams profileParams = new LinearLayout.LayoutParams(-1, -2);
        profileParams.setMargins(0, dp(13), 0, dp(17));
        page.addView(profileRow, profileParams);

        homeHero = new LinearLayout(this);
        homeHero.setOrientation(LinearLayout.VERTICAL);
        homeHero.setPadding(dp(19), dp(18), dp(19), dp(18));
        homeHero.setElevation(dp(CARD_ELEVATION + 1));
        homeHero.setBackground(gradientRound(SURFACE, SURFACE_2, BORDER, CARD_RADIUS,
                GradientDrawable.Orientation.TL_BR));

        LinearLayout heroTop = new LinearLayout(this);
        heroTop.setGravity(Gravity.CENTER_VERTICAL);

        heroPulseDot = new View(this);
        heroPulseDot.setBackground(round(ACCENT, Color.TRANSPARENT, 999));
        heroTop.addView(heroPulseDot, new LinearLayout.LayoutParams(dp(9), dp(9)));

        heroLabel = sectionLabel("NEXT CLASS");
        heroLabel.setTextColor(ACCENT);
        heroLabel.setPadding(dp(8), 0, 0, 0);
        heroTop.addView(heroLabel, new LinearLayout.LayoutParams(0, -2, 1f));

        heroTimeRange = text("—", 10, MUTED);
        heroTimeRange.setGravity(Gravity.END);
        heroTop.addView(heroTimeRange);
        homeHero.addView(heroTop);

        heroSubject = text("Loading…", 27, TEXT);
        heroSubject.setTypeface(Typeface.DEFAULT_BOLD);
        heroSubject.setPadding(0, dp(10), 0, 0);
        homeHero.addView(heroSubject);

        heroMeta = text("", 11, MUTED);
        heroMeta.setPadding(0, dp(6), 0, 0);
        homeHero.addView(heroMeta);

        LinearLayout locationRow = new LinearLayout(this);
        locationRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams locationParams = new LinearLayout.LayoutParams(-1, dp(56));
        locationParams.setMargins(0, dp(14), 0, 0);

        heroRoomText = metricText("ROOM", "—", TEXT);
        heroRoomText.setBackground(round(SURFACE_2, Color.TRANSPARENT, 16));
        locationRow.addView(heroRoomText, new LinearLayout.LayoutParams(0, -1, 1f));

        heroWingText = metricText("WING", "—", ACCENT);
        heroWingText.setBackground(round(ACCENT_BG, Color.TRANSPARENT, 16));
        LinearLayout.LayoutParams wingParams = new LinearLayout.LayoutParams(0, -1, 1f);
        wingParams.setMargins(dp(8), 0, 0, 0);
        locationRow.addView(heroWingText, wingParams);
        homeHero.addView(locationRow, locationParams);

        LinearLayout countdownRow = new LinearLayout(this);
        countdownRow.setGravity(Gravity.BOTTOM | Gravity.CENTER_VERTICAL);
        countdownRow.setPadding(0, dp(17), 0, 0);

        heroCountdown = text("--:--", 38, TEXT);
        heroCountdown.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        countdownRow.addView(heroCountdown, new LinearLayout.LayoutParams(0, -2, 1f));

        heroCountdownLabel = text("until it starts", 10, MUTED);
        heroCountdownLabel.setGravity(Gravity.END | Gravity.BOTTOM);
        heroCountdownLabel.setPadding(0, 0, 0, dp(3));
        countdownRow.addView(heroCountdownLabel);
        homeHero.addView(countdownRow);

        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, -2);
        heroParams.setMargins(0, 0, 0, dp(15));
        page.addView(homeHero, heroParams);

        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView todayButton = actionButton("TODAY", true);
        quickRow.addView(todayButton, new LinearLayout.LayoutParams(dp(88), dp(40)));

        TextView weekButton = actionButton("VIEW WEEK  →", false);
        LinearLayout.LayoutParams weekParams = new LinearLayout.LayoutParams(0, dp(40), 1f);
        weekParams.setMargins(dp(9), 0, 0, 0);
        quickRow.addView(weekButton, weekParams);
        weekButton.setOnClickListener(v -> {
            tap(v);
            navigate("week");
        });

        LinearLayout.LayoutParams quickParams = new LinearLayout.LayoutParams(-1, -2);
        quickParams.setMargins(0, 0, 0, dp(19));
        page.addView(quickRow, quickParams);

        LinearLayout sectionHeader = new LinearLayout(this);
        sectionHeader.setGravity(Gravity.CENTER_VERTICAL);

        homeDayLabel = text("TODAY", 14, TEXT);
        homeDayLabel.setTypeface(Typeface.DEFAULT_BOLD);
        sectionHeader.addView(homeDayLabel, new LinearLayout.LayoutParams(0, -2, 1f));

        homeSummary = text("", 9, MUTED);
        sectionHeader.addView(homeSummary);
        page.addView(sectionHeader);

        TextView helper = text("Your day, in time order.", 10, MUTED);
        helper.setPadding(0, dp(4), 0, dp(12));
        page.addView(helper);

        homeSchedule = new LinearLayout(this);
        homeSchedule.setOrientation(LinearLayout.VERTICAL);
        page.addView(homeSchedule);

        addBottomSpace(page);
    }

    private void buildWeekScreen() {
        LinearLayout page = scrollPage();

        TextView eyebrow = sectionLabel("TIMETABLE");
        eyebrow.setTextColor(ACCENT);
        page.addView(eyebrow);

        TextView title = text("Your week", themeHeadingSize(), TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(5), 0, 0);
        page.addView(title);

        TextView subtitle = text(
                getBatch() + "  •  Roll " + getRollNumber(),
                11,
                MUTED
        );
        subtitle.setPadding(0, dp(5), 0, dp(17));
        page.addView(subtitle);

        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout dayRow = new LinearLayout(this);
        dayRow.setOrientation(LinearLayout.HORIZONTAL);
        for (String day : TimetableData.DAYS) {
            dayRow.addView(dayChip(day), new LinearLayout.LayoutParams(dp(78), dp(56)));
            if (!day.equals(TimetableData.DAYS[TimetableData.DAYS.length - 1])) {
                dayRow.addView(spacer(dp(8), 1));
            }
        }

        scroller.addView(dayRow);
        LinearLayout.LayoutParams scrollerParams = new LinearLayout.LayoutParams(-1, dp(56));
        scrollerParams.setMargins(0, 0, 0, dp(20));
        page.addView(scroller, scrollerParams);

        LinearLayout selectedHeader = new LinearLayout(this);
        selectedHeader.setGravity(Gravity.CENTER_VERTICAL);

        weekDayTitle = text(weekSelectedDay.toUpperCase(), 13, TEXT);
        weekDayTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        weekDayTitle.setLetterSpacing(0.09f);
        selectedHeader.addView(weekDayTitle, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView hours = text("08:20  →  15:30", 9, MUTED);
        selectedHeader.addView(hours);
        page.addView(selectedHeader);

        TextView helper = text("Tap a day above to browse the full schedule.", 10, MUTED);
        helper.setPadding(0, dp(4), 0, dp(12));
        page.addView(helper);

        weekSchedule = new LinearLayout(this);
        weekSchedule.setOrientation(LinearLayout.VERTICAL);
        page.addView(weekSchedule);

        addBottomSpace(page);
        updateWeekDayChips(dayRow);
        refreshWeekSchedule();
    }

    private void buildExpensesScreen() {
        expenseMonth = expenseMonth.withDayOfMonth(1);

        LinearLayout page = scrollPage();

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);
        header.addView(headerText, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView eyebrow = sectionLabel("MONEY");
        eyebrow.setTextColor(ACCENT);
        headerText.addView(eyebrow);

        TextView title = text("Expenses", 29, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(5), 0, 0);
        headerText.addView(title);

        TextView subtitle = text(
                "A clear view of where your student money goes.",
                10,
                MUTED
        );
        subtitle.setPadding(0, dp(5), 0, 0);
        headerText.addView(subtitle);

        TextView add = actionButton("+  ADD", true);
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(dp(86), dp(42));
        addParams.setMargins(dp(10), 0, 0, 0);
        header.addView(add, addParams);
        add.setOnClickListener(v -> {
            tap(v);
            showExpenseEditor(null);
        });

        page.addView(header);

        LinearLayout monthRow = new LinearLayout(this);
        monthRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams monthParams = new LinearLayout.LayoutParams(-1, dp(48));
        monthParams.setMargins(0, dp(18), 0, dp(12));

        TextView previous = actionButton("‹", false);
        TextView next = actionButton("›", false);
        monthRow.addView(previous, new LinearLayout.LayoutParams(dp(46), dp(42)));

        expenseMonthTitle = text("", 14, TEXT);
        expenseMonthTitle.setGravity(Gravity.CENTER);
        expenseMonthTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        monthRow.addView(expenseMonthTitle, new LinearLayout.LayoutParams(0, -1, 1f));

        monthRow.addView(next, new LinearLayout.LayoutParams(dp(46), dp(42)));

        previous.setOnClickListener(v -> {
            tap(v);
            expenseMonth = expenseMonth.minusMonths(1);
            refreshExpensesScreen();
        });
        next.setOnClickListener(v -> {
            tap(v);
            expenseMonth = expenseMonth.plusMonths(1);
            refreshExpensesScreen();
        });
        page.addView(monthRow, monthParams);

        LinearLayout summary = cardColumn();
        summary.setPadding(dp(18), dp(17), dp(18), dp(17));

        TextView summaryLabel = sectionLabel("SPENT THIS MONTH");
        summary.addView(summaryLabel);

        LinearLayout spendHero = new LinearLayout(this);
        spendHero.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout spendCopy = new LinearLayout(this);
        spendCopy.setOrientation(LinearLayout.VERTICAL);

        expenseMonthTotal = text("₹0", 34, TEXT);
        expenseMonthTotal.setTypeface(Typeface.DEFAULT_BOLD);
        expenseMonthTotal.setPadding(0, dp(6), 0, 0);
        spendCopy.addView(expenseMonthTotal);

        TextView spendCaption = text("monthly spend", 9, MUTED);
        spendCaption.setPadding(0, dp(2), 0, 0);
        spendCopy.addView(spendCaption);

        spendHero.addView(spendCopy, new LinearLayout.LayoutParams(0, -2, 1f));

        expenseChart = new SpendingChartView(this);
        expenseChart.setContentDescription("Monthly spending by category");
        spendHero.addView(expenseChart, new LinearLayout.LayoutParams(dp(118), dp(118)));

        summary.addView(spendHero);

        LinearLayout summaryMeta = new LinearLayout(this);
        summaryMeta.setGravity(Gravity.CENTER_VERTICAL);
        expenseTransactionCount = text("0 transactions", 9, MUTED);
        summaryMeta.addView(expenseTransactionCount, new LinearLayout.LayoutParams(0, -2, 1f));

        expenseDailyAverage = text("₹0 / day", 9, MUTED);
        expenseDailyAverage.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        expenseDailyAverage.setPadding(0, 0, dp(8), 0);
        summaryMeta.addView(expenseDailyAverage);

        TextView budget = actionButton("SET BUDGET", false);
        summaryMeta.addView(budget, new LinearLayout.LayoutParams(dp(108), dp(36)));
        budget.setOnClickListener(v -> {
            tap(v);
            showBudgetEditor();
        });
        summary.addView(summaryMeta, new LinearLayout.LayoutParams(-1, dp(36)));

        expenseBudgetLabel = text("No monthly budget set", 10, TEXT);
        expenseBudgetLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        expenseBudgetLabel.setPadding(0, dp(13), 0, 0);
        summary.addView(expenseBudgetLabel);

        expenseBudgetMeta = text("Set one to track your remaining allowance.", 9, MUTED);
        expenseBudgetMeta.setPadding(0, dp(3), 0, 0);
        summary.addView(expenseBudgetMeta);

        page.addView(summary, cardMargin());

        page.addView(sectionLabel("BREAKDOWN"), sectionParams(0, 8));

        LinearLayout breakdown = cardColumn();
        TextView breakdownTitle = text("Where it's going", 15, TEXT);
        breakdownTitle.setTypeface(Typeface.DEFAULT_BOLD);
        breakdown.addView(breakdownTitle);

        TextView breakdownHint = text(
                "Your biggest categories this month.",
                9,
                MUTED
        );
        breakdownHint.setPadding(0, dp(4), 0, dp(12));
        breakdown.addView(breakdownHint);

        LinearLayout categoryHost = new LinearLayout(this);
        categoryHost.setOrientation(LinearLayout.VERTICAL);
        breakdown.addView(categoryHost);
        // Stored in the same host as the list container through a tag to keep the class lightweight.
        breakdown.setTag(categoryHost);

        page.addView(breakdown, sectionParams(0, 20));

        LinearLayout txHeader = new LinearLayout(this);
        txHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView txTitle = text("Transactions", 15, TEXT);
        txTitle.setTypeface(Typeface.DEFAULT_BOLD);
        txHeader.addView(txTitle, new LinearLayout.LayoutParams(0, -2, 1f));

        expenseListHost = new LinearLayout(this);
        expenseListHost.setOrientation(LinearLayout.VERTICAL);

        page.addView(txHeader, sectionParams(0, 8));

        LinearLayout listCard = cardColumn();
        listCard.setPadding(dp(13), dp(13), dp(13), dp(13));
        listCard.addView(expenseListHost);
        page.addView(listCard, sectionParams(0, 8));

        addBottomSpace(page);
        refreshExpensesScreen();
    }

    private void refreshExpensesScreen() {
        if (expenseMonthTitle == null || expenseListHost == null) return;

        expenseMonth = expenseMonth.withDayOfMonth(1);
        String monthText = expenseMonth.format(
                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
        );
        expenseMonthTitle.setText(monthText.toUpperCase(Locale.ENGLISH));

        List<ExpenseStore.Expense> expenses =
                ExpenseStore.forMonth(preferences, expenseMonth);
        long total = ExpenseStore.totalPaise(expenses);
        long budget = ExpenseStore.budgetPaise(preferences, expenseMonth);

        expenseMonthTotal.setText(formatRupees(total));
        expenseTransactionCount.setText(
                expenses.size() + (expenses.size() == 1 ? " transaction" : " transactions")
        );

        int daysElapsed = expenseMonth.equals(LocalDate.now(zone).withDayOfMonth(1))
                ? Math.max(1, LocalDate.now(zone).getDayOfMonth())
                : expenseMonth.lengthOfMonth();
        long dailyAverage = total / daysElapsed;
        expenseDailyAverage.setText(formatRupees(dailyAverage) + " / day");

        if (expenseChart != null) {
            expenseChart.setData(ExpenseStore.categoryTotals(expenses), total);
        }

        if (budget > 0) {
            long remaining = budget - total;
            if (remaining >= 0) {
                expenseBudgetLabel.setText("Budget  •  " + formatRupees(budget));
                expenseBudgetMeta.setText(
                        formatRupees(remaining) + " remaining"
                );
            } else {
                expenseBudgetLabel.setText("Budget  •  " + formatRupees(budget));
                expenseBudgetMeta.setText(
                        formatRupees(-remaining) + " over budget"
                );
            }
        } else {
            expenseBudgetLabel.setText("No monthly budget set");
            expenseBudgetMeta.setText("Set one to track your remaining allowance.");
        }

        // The breakdown card is immediately before the transactions header.
        expenseListHost.removeAllViews();

        if (expenses.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER_HORIZONTAL);
            empty.setPadding(0, dp(18), 0, dp(18));

            ImageView icon = new ImageView(this);
            icon.setImageResource(R.drawable.ic_wallet);
            icon.setColorFilter(ACCENT);
            icon.setPadding(dp(8), dp(8), dp(8), dp(8));
            icon.setBackground(round(ACCENT_BG, Color.TRANSPARENT, 999));
            empty.addView(icon, new LinearLayout.LayoutParams(dp(50), dp(50)));

            TextView title = text(
                    "No expenses yet",
                    13,
                    TEXT
            );
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setPadding(0, dp(9), 0, 0);
            empty.addView(title);

            TextView hint = text(
                    "Tap + ADD to record your first expense.",
                    9,
                    MUTED
            );
            hint.setPadding(0, dp(4), 0, 0);
            empty.addView(hint);

            expenseListHost.addView(empty);
        } else {
            LocalDate previousDate = null;
            for (ExpenseStore.Expense expense : expenses) {
                if (previousDate == null || !previousDate.equals(expense.date)) {
                    TextView date = text(dateLabel(expense.date), 9, SUBTLE);
                    date.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                    date.setLetterSpacing(0.08f);
                    date.setPadding(dp(3), dp(7), dp(3), dp(7));
                    expenseListHost.addView(date);
                    previousDate = expense.date;
                }
                expenseListHost.addView(expenseRow(expense));
            }
        }

        refreshExpenseBreakdown(expenses);
    }

    private void refreshExpenseBreakdown(List<ExpenseStore.Expense> expenses) {
        // Locate the breakdown host from the card structure.
        if (contentHost == null || contentHost.getChildCount() == 0) return;

        View rootView = contentHost.getChildAt(0);
        if (!(rootView instanceof ScrollView)) return;

        LinearLayout body = (LinearLayout) ((ScrollView) rootView).getChildAt(0);
        LinearLayout categoryHost = null;

        for (int i = 0; i < body.getChildCount(); i++) {
            View child = body.getChildAt(i);
            if (child instanceof LinearLayout) {
                Object tag = child.getTag();
                if (tag instanceof LinearLayout) {
                    categoryHost = (LinearLayout) tag;
                    break;
                }
            }
        }

        if (categoryHost == null) return;
        categoryHost.removeAllViews();

        java.util.Map<String, Long> totals = ExpenseStore.categoryTotals(expenses);
        long max = 0L;
        for (long value : totals.values()) max = Math.max(max, value);

        if (max == 0L) {
            TextView empty = text(
                    "Add expenses to see your spending pattern.",
                    10,
                    MUTED
            );
            empty.setPadding(0, dp(8), 0, dp(2));
            categoryHost.addView(empty);
            return;
        }

        for (int i = 0; i < ExpenseStore.CATEGORIES.length; i++) {
            String category = ExpenseStore.CATEGORIES[i];
            long amount = totals.get(category);
            if (amount <= 0) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(4), 0, dp(7));

            LinearLayout top = new LinearLayout(this);
            top.setGravity(Gravity.CENTER_VERTICAL);

            TextView name = text(category, 10, TEXT);
            name.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            top.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));

            TextView value = text(formatRupees(amount), 10, MUTED);
            value.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            top.addView(value);
            row.addView(top);

            LinearLayout track = new LinearLayout(this);
            track.setOrientation(LinearLayout.HORIZONTAL);
            track.setBackground(round(blend(SURFACE_2, BG, 0.10f), Color.TRANSPARENT, 999));

            float ratio = Math.max(0.03f, Math.min(1f, (float) amount / (float) max));
            View fill = new View(this);
            fill.setBackground(round(
                    i % 2 == 0 ? ACCENT : ACCENT_2,
                    Color.TRANSPARENT,
                    999
            ));
            track.addView(fill, new LinearLayout.LayoutParams(0, dp(6), ratio));
            track.addView(spacer(1, dp(6)), new LinearLayout.LayoutParams(0, dp(6), 1f - ratio));
            row.addView(track, new LinearLayout.LayoutParams(-1, dp(6)));

            categoryHost.addView(row);
        }
    }

    private LinearLayout expenseRow(ExpenseStore.Expense expense) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(5), dp(10), dp(5), dp(10));
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(ripple(
                Color.TRANSPARENT,
                blend(SURFACE_2, BG, 0.20f),
                Color.TRANSPARENT,
                SMALL_RADIUS
        ));
        row.setOnClickListener(v -> {
            tap(v);
            showExpenseActions(expense);
        });

        TextView categoryMark = text(
                expense.category.substring(0, 1).toUpperCase(Locale.ENGLISH),
                11,
                BG
        );
        categoryMark.setGravity(Gravity.CENTER);
        categoryMark.setTypeface(Typeface.DEFAULT_BOLD);
        categoryMark.setBackground(round(
                expense.category.equals("Food") ? ACCENT : ACCENT_2,
                Color.TRANSPARENT,
                999
        ));
        row.addView(categoryMark, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(11), 0, 0, 0);

        TextView category = text(expense.category, 11, TEXT);
        category.setTypeface(Typeface.DEFAULT_BOLD);
        copy.addView(category);

        String detail = expense.note.trim();
        if (detail.isEmpty()) detail = expense.paymentMode;
        else detail = detail + "  •  " + expense.paymentMode;

        TextView note = text(detail, 8, MUTED);
        note.setMaxLines(1);
        note.setEllipsize(android.text.TextUtils.TruncateAt.END);
        note.setPadding(0, dp(3), 0, 0);
        copy.addView(note);

        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView amount = text(formatRupees(expense.amountPaise), 12, TEXT);
        amount.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        amount.setGravity(Gravity.END);
        row.addView(amount);

        return row;
    }

    private void showExpenseActions(ExpenseStore.Expense expense) {
        new AlertDialog.Builder(this)
                .setTitle(expense.category + "  •  " + formatRupees(expense.amountPaise))
                .setItems(
                        new String[]{"Edit expense", "Delete expense"},
                        (dialog, which) -> {
                            if (which == 0) {
                                showExpenseEditor(expense);
                            } else {
                                new AlertDialog.Builder(this)
                                        .setTitle("Delete expense?")
                                        .setMessage(
                                                formatRupees(expense.amountPaise)
                                                        + " • " + expense.category
                                        )
                                        .setNegativeButton("Cancel", null)
                                        .setPositiveButton("Delete", (d, w) -> {
                                            ExpenseStore.delete(preferences, expense.id);
                                            refreshExpensesScreen();
                                        })
                                        .show();
                            }
                        }
                )
                .show();
    }

    private void showExpenseEditor(ExpenseStore.Expense existing) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(2), 0, dp(2), 0);

        EditText amount = field("0.00", true);
        amount.setInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        amount.setHint("0.00");
        if (existing != null) {
            amount.setText(amountRupeesInput(existing.amountPaise));
        }
        box.addView(amount, fieldParams());

        TextView category = choiceField(
                "Category",
                existing == null ? "Food" : existing.category
        );
        LinearLayout.LayoutParams choiceParams = fieldLikeParams();
        choiceParams.setMargins(0, dp(9), 0, 0);
        box.addView(category, choiceParams);

        TextView payment = choiceField(
                "Payment",
                existing == null ? "UPI" : existing.paymentMode
        );
        LinearLayout.LayoutParams paymentParams = fieldLikeParams();
        paymentParams.setMargins(0, dp(9), 0, 0);
        box.addView(payment, paymentParams);

        LocalDate[] selectedDate = {
                existing == null ? LocalDate.now(zone) : existing.date
        };
        TextView date = choiceField("Date", formatLongDate(selectedDate[0]));
        LinearLayout.LayoutParams dateParams = fieldLikeParams();
        dateParams.setMargins(0, dp(9), 0, 0);
        box.addView(date, dateParams);

        EditText note = field("e.g. Lunch at college", false);
        if (existing != null) note.setText(existing.note);
        LinearLayout.LayoutParams noteParams = fieldParams();
        noteParams.setMargins(0, dp(9), 0, 0);
        box.addView(note, noteParams);

        category.setOnClickListener(v ->
                showExpenseChoice("Category", ExpenseStore.CATEGORIES, category)
        );
        payment.setOnClickListener(v ->
                showExpenseChoice("Payment", ExpenseStore.PAYMENT_MODES, payment)
        );
        date.setOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(
                    this,
                    (view, year, month, day) -> {
                        selectedDate[0] = LocalDate.of(year, month + 1, day);
                        date.setText(formatLongDate(selectedDate[0]));
                    },
                    selectedDate[0].getYear(),
                    selectedDate[0].getMonthValue() - 1,
                    selectedDate[0].getDayOfMonth()
            );
            picker.show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add expense" : "Edit expense")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(existing == null ? "Add" : "Save", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    try {
                        long paise = ExpenseStore.parseAmountToPaise(
                                amount.getText().toString()
                        );
                        if (paise <= 0) {
                            amount.setError("Enter an amount greater than ₹0");
                            return;
                        }

                        long id = existing == null
                                ? System.currentTimeMillis()
                                : existing.id;

                        ExpenseStore.Expense updated = new ExpenseStore.Expense(
                                id,
                                paise,
                                selectedDate[0],
                                category.getText().toString(),
                                note.getText().toString().trim(),
                                payment.getText().toString()
                        );

                        ExpenseStore.upsert(preferences, updated);
                        expenseMonth = selectedDate[0].withDayOfMonth(1);
                        dialog.dismiss();
                        refreshExpensesScreen();
                    } catch (Exception ignored) {
                        amount.setError("Enter a valid amount, e.g. 120.50");
                    }
                }));

        dialog.show();
    }

    private void showExpenseChoice(
            String title,
            String[] options,
            TextView target
    ) {
        String current = target.getText().toString();
        int selected = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(current)) {
                selected = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(options, selected, (dialog, which) -> {
                    target.setText(options[which]);
                    dialog.dismiss();
                })
                .show();
    }

    private TextView choiceField(String label, String value) {
        TextView field = text(value, 13, TEXT);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        field.setPadding(dp(13), 0, dp(13), 0);
        field.setBackground(ripple(
                SURFACE_2,
                blend(SURFACE_2, BG, 0.18f),
                Color.TRANSPARENT,
                SMALL_RADIUS
        ));
        field.setContentDescription(label + ": " + value);
        field.setClickable(true);
        field.setFocusable(true);
        return field;
    }

    private LinearLayout.LayoutParams fieldLikeParams() {
        return new LinearLayout.LayoutParams(-1, dp(50));
    }

    private void showBudgetEditor() {
        EditText input = field("0.00", true);
        input.setInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        long current = ExpenseStore.budgetPaise(preferences, expenseMonth);
        if (current > 0) input.setText(amountRupeesInput(current));

        new AlertDialog.Builder(this)
                .setTitle("Monthly budget")
                .setMessage("Set a spending limit for " +
                        expenseMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)) + ".")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Clear", (dialog, which) -> {
                    ExpenseStore.setBudgetPaise(preferences, expenseMonth, 0L);
                    refreshExpensesScreen();
                })
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        long paise = ExpenseStore.parseAmountToPaise(input.getText().toString());
                        ExpenseStore.setBudgetPaise(preferences, expenseMonth, paise);
                        refreshExpensesScreen();
                    } catch (Exception ignored) {
                        input.setError("Enter a valid budget.");
                    }
                })
                .show();
    }

    private String dateLabel(LocalDate date) {
        LocalDate today = LocalDate.now(zone);
        if (date.equals(today)) return "TODAY";
        if (date.equals(today.minusDays(1))) return "YESTERDAY";
        return date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH))
                .toUpperCase(Locale.ENGLISH);
    }

    private String formatLongDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH));
    }

    private String amountRupeesInput(long paise) {
        BigDecimal value = BigDecimal.valueOf(paise, 2);
        return value.stripTrailingZeros().toPlainString();
    }

    private String formatRupees(long paise) {
        long whole = Math.abs(paise) / 100L;
        int cents = (int) (Math.abs(paise) % 100L);
        String value = NumberFormat.getIntegerInstance(Locale.ENGLISH).format(whole);
        if (cents > 0) {
            value += "." + (cents < 10 ? "0" : "") + cents;
        }
        return (paise < 0 ? "-₹" : "₹") + value;
    }

    private void buildSettingsScreen() {
        LinearLayout page = scrollPage();

        TextView eyebrow = sectionLabel(THEME_TAGS[themeIndex] + "  •  PERSONALISE");
        eyebrow.setTextColor(ACCENT);
        page.addView(eyebrow);

        TextView title = text("Settings", themeHeadingSize(), TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(5), 0, 0);
        page.addView(title);

        TextView subtitle = text(
                "Your profile, visual system and reminders — all in one place.",
                12,
                MUTED
        );
        subtitle.setPadding(0, dp(5), 0, dp(21));
        page.addView(subtitle);

        page.addView(sectionLabel("PROFILE"), sectionParams(0, 9));

        LinearLayout profileCard = cardColumn();
        profileCard.setOrientation(LinearLayout.HORIZONTAL);
        profileCard.setGravity(Gravity.CENTER_VERTICAL);

        TextView initials = avatarText();
        profileCard.addView(initials, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout profileText = new LinearLayout(this);
        profileText.setOrientation(LinearLayout.VERTICAL);

        TextView name = text(getName(), 16, TEXT);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        profileText.addView(name);

        TextView batch = text(
                getBatch() + "  •  Roll " + getRollNumber(),
                10,
                MUTED
        );
        batch.setPadding(0, dp(4), 0, 0);
        profileText.addView(batch);

        LinearLayout.LayoutParams profileTextParams =
                new LinearLayout.LayoutParams(0, -2, 1f);
        profileTextParams.setMargins(dp(12), 0, dp(8), 0);
        profileCard.addView(profileText, profileTextParams);

        TextView edit = actionButton("Edit", false);
        profileCard.addView(edit, new LinearLayout.LayoutParams(dp(76), CONTROL_HEIGHT_DP()));
        edit.setOnClickListener(v -> {
            tap(v);
            showEditProfile();
        });

        LinearLayout.LayoutParams profileParams = new LinearLayout.LayoutParams(-1, -2);
        profileParams.setMargins(0, 0, 0, dp(21));
        page.addView(profileCard, profileParams);

        page.addView(sectionLabel("APPEARANCE"), sectionParams(0, 6));

        TextView appearanceHint = text(
                THEME_DESCRIPTIONS[themeIndex] + ".",
                10,
                MUTED
        );
        appearanceHint.setPadding(0, dp(4), 0, dp(10));
        page.addView(appearanceHint);

        HorizontalScrollView themeScroller = new HorizontalScrollView(this);
        themeScroller.setHorizontalScrollBarEnabled(false);
        themeScroller.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout themeRow = new LinearLayout(this);
        for (int i = 0; i < THEME_NAMES.length; i++) {
            themeRow.addView(themeCard(i), new LinearLayout.LayoutParams(dp(148), dp(128)));
            if (i < THEME_NAMES.length - 1) themeRow.addView(spacer(dp(10), 1));
        }
        themeScroller.addView(themeRow);
        page.addView(themeScroller, sectionParams(0, 22));

        page.addView(sectionLabel("NOTIFICATIONS"), sectionParams(0, 7));

        LinearLayout notificationCard = settingRow(R.drawable.ic_calendar, ACCENT);

        LinearLayout notifText = new LinearLayout(this);
        notifText.setOrientation(LinearLayout.VERTICAL);

        TextView notifTitle = text("Class reminders", 14, TEXT);
        notifTitle.setTypeface(Typeface.DEFAULT_BOLD);
        notifText.addView(notifTitle);

        TextView notifDescription = text(
                isNotificationGranted()
                        ? "15 minutes before your personalized classes."
                        : "Enable reminders for your personalized timetable.",
                10,
                MUTED
        );
        notifDescription.setPadding(0, dp(4), 0, 0);
        notifText.addView(notifDescription);

        notificationCard.addView(notifText, new LinearLayout.LayoutParams(0, -2, 1f));

        if (!isNotificationGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            TextView enable = actionButton("Enable", false);
            notificationCard.addView(enable, new LinearLayout.LayoutParams(dp(84), CONTROL_HEIGHT_DP()));
            enable.setOnClickListener(v -> {
                tap(v);
                requestNotificationAccess();
            });
        } else {
            TextView status = text("ON", 9, ACCENT);
            status.setGravity(Gravity.CENTER);
            status.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            status.setPadding(dp(10), 0, dp(10), 0);
            status.setBackground(round(ACCENT_BG, Color.TRANSPARENT, PILL_RADIUS()));
            notificationCard.addView(status, new LinearLayout.LayoutParams(dp(48), dp(32)));
        }

        page.addView(notificationCard, sectionParams(0, 22));

        page.addView(sectionLabel("ABOUT & FEEDBACK"), sectionParams(0, 7));

        LinearLayout aboutCard = cardColumn();

        TextView aboutTitle = text("NextBell", 16, TEXT);
        aboutTitle.setTypeface(Typeface.DEFAULT_BOLD);
        aboutCard.addView(aboutTitle);

        TextView aboutSubtitle = text(
                "FE Division B  •  Academic Year 2026–27",
                10,
                MUTED
        );
        aboutSubtitle.setPadding(0, dp(4), 0, dp(2));
        aboutCard.addView(aboutSubtitle);

        TextView source = text(
                "Official timetable • W.E.F. 16/09/2026 • Revision 00",
                9,
                SUBTLE
        );
        source.setPadding(0, 0, 0, dp(16));
        aboutCard.addView(source);

        TextView creator = text("Made by Neal Aringale", 12, ACCENT);
        creator.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        aboutCard.addView(creator);

        TextView hint = text(
                "Suggestions, bugs and improvements — reach me directly.",
                9,
                MUTED
        );
        hint.setPadding(0, dp(4), 0, dp(12));
        aboutCard.addView(hint);

        LinearLayout whatsapp = contactButton(
                R.drawable.ic_whatsapp, "WhatsApp", "Chat with Neal", "7499517574");
        aboutCard.addView(whatsapp, contactParams());
        whatsapp.setOnClickListener(v -> {
            tap(v);
            openWhatsApp();
        });

        LinearLayout email = contactButton(
                R.drawable.ic_gmail, "Gmail", "Email feedback & bugs", "nealaringale@gmail.com");
        aboutCard.addView(email, contactParams());
        email.setOnClickListener(v -> {
            tap(v);
            openEmail();
        });

        LinearLayout call = contactButton(
                R.drawable.ic_call, "Call", "Talk to Neal directly", "7499517574");
        aboutCard.addView(call, contactParams());
        call.setOnClickListener(v -> {
            tap(v);
            openCall();
        });

        TextView footer = text("No account • No cloud profile • Local-first", 8, SUBTLE);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(10), 0, 0);
        aboutCard.addView(footer);

        LinearLayout.LayoutParams aboutParams = new LinearLayout.LayoutParams(-1, -2);
        aboutParams.setMargins(0, 0, 0, dp(8));
        page.addView(aboutCard, aboutParams);

        addBottomSpace(page);
    }

    private LinearLayout scrollPage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setBackground(gradientRound(
                BG,
                blend(BG, ACCENT, themeIndex == 4 ? 0.035f : 0.055f),
                Color.TRANSPARENT,
                0,
                themeGradientOrientation()
        ));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int horizontalPadding = themeContentPadding();
        body.setPadding(
                dp(horizontalPadding),
                dp(themeTopPadding()),
                dp(horizontalPadding),
                dp(30)
        );
        scroll.addView(body);

        contentHost.removeAllViews();
        contentHost.addView(scroll);
        return body;
    }

    private void addBottomSpace(LinearLayout page) {
        page.addView(spacer(1, dp(48)));
    }

    private LinearLayout.LayoutParams sectionParams(int top, int bottom) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(top), 0, dp(bottom));
        return params;
    }


    private LinearLayout cardColumn() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(17), dp(17), dp(17), dp(17));
        card.setBackground(gradientRound(
                SURFACE,
                SURFACE_2,
                BORDER,
                CARD_RADIUS,
                themeGradientOrientation()
        ));
        card.setElevation(dp(CARD_ELEVATION));
        return card;
    }

    private LinearLayout settingRow(int iconRes, int iconColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(13), dp(12), dp(13), dp(12));
        card.setBackground(ripple(
                SURFACE,
                blend(SURFACE, BG, 0.24f),
                BORDER,
                CARD_RADIUS
        ));
        card.setElevation(dp(Math.max(1, CARD_ELEVATION - 2)));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(iconColor);
        icon.setPadding(dp(7), dp(7), dp(7), dp(7));
        icon.setBackground(round(ACCENT_BG, Color.TRANSPARENT, SMALL_RADIUS));
        card.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));
        installPressAnimation(card);
        return card;
    }

    private LinearLayout.LayoutParams cardMargin() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(9), 0, dp(20));
        return params;
    }

    private LinearLayout themeCard(int index) {
        boolean selected = index == themeIndex;
        int[] palette = THEMES[index];

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.TOP);
        card.setPadding(dp(11), dp(11), dp(11), dp(10));
        card.setBackground(ripple(
                selected ? palette[7] : palette[1],
                blend(palette[7], palette[0], 0.22f),
                selected ? palette[6] : palette[3],
                themeRadius(index)
        ));
        card.setElevation(selected ? dp(7) : dp(2));
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription(
                "Theme " + THEME_NAMES[index] + " — " + THEME_DESCRIPTIONS[index] +
                        (selected ? ", selected" : "")
        );

        LinearLayout preview = new LinearLayout(this);
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setPadding(dp(8), dp(8), dp(8), dp(8));
        preview.setBackground(round(palette[0], Color.TRANSPARENT, Math.max(8, themeRadius(index) - 4)));

        View accentBar = new View(this);
        accentBar.setBackground(round(palette[6], Color.TRANSPARENT, 999));
        preview.addView(accentBar, new LinearLayout.LayoutParams(dp(52), dp(6)));

        LinearLayout previewRow = new LinearLayout(this);
        previewRow.setGravity(Gravity.CENTER_VERTICAL);
        View block1 = new View(this);
        block1.setBackground(round(palette[1], Color.TRANSPARENT, themeControlRadius(index)));
        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(0, dp(24), 1f);
        b1.setMargins(0, dp(7), dp(5), 0);
        previewRow.addView(block1, b1);

        View block2 = new View(this);
        block2.setBackground(round(palette[6], Color.TRANSPARENT, 999));
        LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(dp(32), dp(10));
        b2.setMargins(0, dp(7), 0, 0);
        previewRow.addView(block2, b2);
        preview.addView(previewRow);

        View footerLine = new View(this);
        footerLine.setBackground(round(palette[3], Color.TRANSPARENT, 999));
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(dp(70), dp(3));
        footerParams.setMargins(0, dp(7), 0, 0);
        preview.addView(footerLine, footerParams);

        card.addView(preview, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView tag = text(THEME_TAGS[index], 8, palette[6]);
        tag.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tag.setLetterSpacing(0.13f);
        tag.setPadding(0, dp(9), 0, 0);
        card.addView(tag);

        TextView title = text(THEME_NAMES[index], 13, palette[4]);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(2), 0, 0);
        card.addView(title);

        TextView description = text(THEME_DESCRIPTIONS[index], 8, palette[5]);
        description.setMaxLines(2);
        description.setPadding(0, dp(3), 0, 0);
        card.addView(description);

        TextView selectedText = text(
                selected ? "ACTIVE  ✓" : "TAP TO APPLY",
                7,
                selected ? palette[6] : palette[5]
        );
        selectedText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        selectedText.setLetterSpacing(0.08f);
        selectedText.setPadding(0, dp(5), 0, 0);
        card.addView(selectedText);

        installPressAnimation(card);
        card.setOnClickListener(v -> {
            if (themeIndex == index) return;

            themeIndex = index;
            preferences.edit().putInt(KEY_THEME, themeIndex).apply();
            applyTheme();

            String keepScreen = selectedScreen;
            lastScreenIndex = screenIndex(keepScreen);
            buildShell();
            selectedScreen = keepScreen;
            animateThemeRefresh();
            navigate(keepScreen);
        });

        return card;
    }

    private int themeRadius(int index) {
        switch (index) {
            case 1: return 21;
            case 2: return 16;
            case 3: return 27;
            case 4: return 15;
            default: return 24;
        }
    }

    private int themeControlRadius(int index) {
        switch (index) {
            case 1: return 13;
            case 2: return 8;
            case 3: return 16;
            case 4: return 7;
            default: return 14;
        }
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
        } else if ("expenses".equals(selectedScreen)) {
            refreshExpensesScreen();
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
        TimetableData.ClassItem currentClass = null;
        TimetableData.ClassItem currentBreak = null;
        TimetableData.ClassItem next = null;

        for (TimetableData.ClassItem item : entries) {
            int start = toSeconds(item.start);
            int end = toSeconds(item.end);

            if (!isWeekend && start <= now && now < end) {
                if (item.isBreak()) {
                    currentBreak = item;
                } else if (item.isAcademic()) {
                    currentClass = item;
                }
            } else if (isWeekend && item.isAcademic() && next == null) {
                next = item;
            } else if (!isWeekend && item.isAcademic() && start > now && next == null) {
                next = item;
            }
        }

        if (currentBreak != null) {
            int minutes = Math.max(1,
                    (toSeconds(currentBreak.end) - toSeconds(currentBreak.start)) / 60);
            boolean lunch = currentBreak.kind == TimetableData.Kind.LUNCH;
            int breakAccent = lunch ? ACCENT_2 : ACCENT;

            homeHero.setBackground(gradientRound(
                    SURFACE,
                    blend(SURFACE_2, breakAccent, 0.22f),
                    breakAccent,
                    CARD_RADIUS,
                    themeGradientOrientation()
            ));
            heroLabel.setText(lunch ? "LUNCH BREAK" : "SHORT BREAK");
            heroLabel.setTextColor(breakAccent);
            heroSubject.setText(lunch ? "Take your lunch." : "Recharge for the next class.");
            heroMeta.setText(
                    formatTime(currentBreak.start) + " → " + formatTime(currentBreak.end)
                            + "  •  " + minutes + " min"
            );
            heroTimeRange.setText("BREAK NOW");
            heroRoomText.setText(lunch ? "TYPE\\nLUNCH" : "TYPE\\nBREAK");
            heroWingText.setText("DURATION\\n" + minutes + " MIN");
            heroWingText.setTextColor(breakAccent);
            heroCountdown.setText(
                    formatCountdown(Math.max(0, toSeconds(currentBreak.end) - now))
            );
            heroCountdownLabel.setText("until break ends");
            animateHero("break:" + currentBreak.id);
        } else if (currentClass != null) {
            homeHero.setBackground(gradientRound(
                    SURFACE,
                    ACCENT_BG,
                    ACCENT,
                    CARD_RADIUS,
                    themeGradientOrientation()
            ));
            heroLabel.setText("HAPPENING NOW");
            heroLabel.setTextColor(ACCENT);
            heroSubject.setText(currentClass.subject);
            heroMeta.setText(formatTeacher(currentClass));
            heroTimeRange.setText(
                    formatTime(currentClass.start) + " → " + formatTime(currentClass.end)
            );
            setHeroLocation(currentClass);
            heroCountdown.setText(
                    formatCountdown(Math.max(0, toSeconds(currentClass.end) - now))
            );
            heroCountdownLabel.setText("until it ends");
            animateHero("current:" + currentClass.id);
        } else if (next != null) {
            homeHero.setBackground(gradientRound(
                    SURFACE,
                    SURFACE_2,
                    BORDER,
                    CARD_RADIUS,
                    themeGradientOrientation()
            ));
            heroLabel.setText("NEXT CLASS");
            heroLabel.setTextColor(ACCENT);
            heroSubject.setText(next.subject);
            heroMeta.setText(formatTeacher(next));
            heroTimeRange.setText(formatTime(next.start) + " → " + formatTime(next.end));
            setHeroLocation(next);

            if (isWeekend) {
                long secondsUntil = secondsUntilNextWeekendClass(next);
                heroCountdown.setText(formatHumanCountdown(secondsUntil));
                heroCountdownLabel.setText("until Monday · " + getBatch());
            } else {
                heroCountdown.setText(
                        formatCountdown(Math.max(0, toSeconds(next.start) - now))
                );
                heroCountdownLabel.setText("until it starts");
            }
            animateHero("next:" + next.id);
        } else {
            homeHero.setBackground(gradientRound(
                    SURFACE,
                    SURFACE_2,
                    BORDER,
                    CARD_RADIUS,
                    themeGradientOrientation()
            ));
            heroLabel.setText("DAY COMPLETE");
            heroLabel.setTextColor(ACCENT);
            heroTimeRange.setText("—");
            heroSubject.setText("You're done for today.");
            heroMeta.setText("No more classes scheduled");
            clearHeroLocation();
            heroCountdown.setText("✓");
            heroCountdownLabel.setText("nothing else scheduled");
            animateHero("done");
        }

        homeDayLabel.setText(
                isWeekend ? "MONDAY PREVIEW" : "TODAY  •  " + formatTodayDate()
        );

        int academicBlocks = 0;
        int breakBlocks = 0;
        for (TimetableData.ClassItem item : entries) {
            if (item.isBreak()) {
                breakBlocks++;
            } else {
                academicBlocks++;
            }
        }

        homeSummary.setText(
                academicBlocks + " sessions  •  " + breakBlocks + " breaks  •  " + getBatch()
        );
        profileBadge.setText(getBatch() + "  •  Roll " + getRollNumber());

        StringBuilder signatureBuilder = new StringBuilder();
        signatureBuilder.append(getRollNumber()).append('|').append(day).append('|');
        if (currentClass != null) signatureBuilder.append("C:").append(currentClass.id);
        if (currentBreak != null) signatureBuilder.append("B:").append(currentBreak.id);
        for (TimetableData.ClassItem item : entries) {
            signatureBuilder.append(';').append(item.id).append(':').append(item.isBreak());
            if (!isWeekend && item.isAcademic()) {
                signatureBuilder.append(':').append(toSeconds(item.end) <= now);
            }
        }
        String scheduleSignature = signatureBuilder.toString();

        if (!scheduleSignature.equals(lastHomeScheduleSignature)) {
            lastHomeScheduleSignature = scheduleSignature;
            homeSchedule.removeAllViews();

            int index = 0;
            for (TimetableData.ClassItem item : entries) {
                boolean isCurrent = currentClass != null && currentClass.id.equals(item.id);
                boolean isDone = !isWeekend
                        && item.isAcademic()
                        && toSeconds(item.end) <= now;

                View row = item.isBreak()
                        ? breakRow(item)
                        : classRow(item, isCurrent, false, isDone);

                homeSchedule.addView(row);
                animateListItem(row, index++);
            }
        }
    }

    private void refreshWeekSchedule() {
        if (weekSchedule == null) return;

        List<TimetableData.ClassItem> entries =
                TimetableData.forRollAndDay(getRollNumber(), weekSelectedDay);

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

        StringBuilder signatureBuilder = new StringBuilder();
        signatureBuilder.append(getRollNumber()).append('|').append(weekSelectedDay).append('|');
        if (current != null) signatureBuilder.append("C:").append(current.id);
        for (TimetableData.ClassItem item : entries) {
            signatureBuilder.append(';').append(item.id).append(':').append(item.isBreak());
            if (viewingToday && item.isAcademic()) {
                signatureBuilder.append(':').append(toSeconds(item.end) <= now);
            }
        }
        String scheduleSignature = signatureBuilder.toString();

        if (scheduleSignature.equals(lastWeekScheduleSignature)) return;
        lastWeekScheduleSignature = scheduleSignature;

        weekSchedule.removeAllViews();

        if (entries.isEmpty()) {
            TextView empty = text("No classes scheduled.", 13, MUTED);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(30), 0, dp(30));
            weekSchedule.addView(empty);
            animateListItem(empty, 0);
            return;
        }

        int index = 0;
        for (TimetableData.ClassItem item : entries) {
            boolean isCurrent = current != null && current.id.equals(item.id);
            boolean isDone = viewingToday
                    && item.isAcademic()
                    && toSeconds(item.end) <= now;

            View row = item.isBreak()
                    ? breakRow(item)
                    : classRow(item, isCurrent, false, isDone);

            weekSchedule.addView(row);
            animateListItem(row, index++);
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
            chip.setBackground(ripple(
                    selected ? ACCENT_BG : SURFACE,
                    blend(ACCENT_BG, BG, 0.30f),
                    selected ? ACCENT : Color.TRANSPARENT,
                    SMALL_RADIUS
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
        boolean selected = day.equals(weekSelectedDay);
        TextView chip = text(day.substring(0, 3).toUpperCase(), 12, selected ? ACCENT : TEXT);
        chip.setGravity(Gravity.CENTER);
        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        chip.setBackground(ripple(
                selected ? ACCENT_BG : SURFACE,
                blend(ACCENT_BG, BG, 0.35f),
                selected ? ACCENT : Color.TRANSPARENT,
                SMALL_RADIUS
        ));
        chip.setClickable(true);
        chip.setFocusable(true);
        installPressAnimation(chip);
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

        LinearLayout timeline = new LinearLayout(this);
        timeline.setOrientation(LinearLayout.VERTICAL);
        timeline.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView start = text(formatTime(item.start), 10, isDone ? MUTED : TEXT);
        start.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        start.setGravity(Gravity.CENTER);
        timeline.addView(start);

        TextView end = text(formatTime(item.end), 8, SUBTLE);
        end.setGravity(Gravity.CENTER);
        end.setPadding(0, dp(3), 0, 0);
        timeline.addView(end);

        View dot = new View(this);
        dot.setBackground(round(
                isCurrent ? ACCENT : (isDone ? BORDER : ACCENT_2),
                Color.TRANSPARENT,
                999
        ));
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                dp(isCurrent ? 10 : 7),
                dp(isCurrent ? 10 : 7)
        );
        dotParams.setMargins(0, dp(7), 0, dp(6));
        timeline.addView(dot, dotParams);

        View line = new View(this);
        line.setBackground(round(BORDER, Color.TRANSPARENT, 999));
        timeline.addView(line, new LinearLayout.LayoutParams(dp(2), dp(44)));

        LinearLayout.LayoutParams timelineParams =
                new LinearLayout.LayoutParams(dp(64), -1);
        timelineParams.setMargins(0, 0, dp(8), 0);
        outer.addView(timeline, timelineParams);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(13), dp(12), dp(11), dp(12));
        card.setBackground(gradientRound(
                isCurrent ? ACCENT_BG : SURFACE,
                isCurrent ? blend(ACCENT_BG, SURFACE, 0.60f) : SURFACE_2,
                isCurrent ? ACCENT : Color.TRANSPARENT,
                CARD_RADIUS,
                GradientDrawable.Orientation.LEFT_RIGHT
        ));
        card.setElevation(isCurrent ? dp(CARD_ELEVATION + 1) : dp(Math.max(1, CARD_ELEVATION - 2)));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);

        TextView subject = text(item.subject, 13, isDone ? MUTED : TEXT);
        subject.setTypeface(Typeface.DEFAULT_BOLD);
        subject.setMaxLines(2);
        main.addView(subject);

        TextView details = text(detailsFor(item), 9, MUTED);
        details.setMaxLines(2);
        details.setPadding(0, dp(4), 0, 0);
        main.addView(details);

        card.addView(main, new LinearLayout.LayoutParams(0, -2, 1f));

        if (!roomNumber(item).isEmpty() || !wing(item).isEmpty()) {
            LinearLayout location = new LinearLayout(this);
            location.setOrientation(LinearLayout.VERTICAL);
            location.setGravity(Gravity.END);

            TextView room = text(roomNumber(item).isEmpty() ? "—" : roomNumber(item), 12, TEXT);
            room.setGravity(Gravity.END);
            room.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            location.addView(room);

            TextView wing = text(wing(item).isEmpty() ? "Wing —" : wing(item), 8, ACCENT);
            wing.setGravity(Gravity.END);
            wing.setPadding(0, dp(2), 0, 0);
            location.addView(wing);

            LinearLayout.LayoutParams locParams = new LinearLayout.LayoutParams(dp(66), -2);
            locParams.setMargins(dp(6), 0, 0, 0);
            card.addView(location, locParams);
        }

        if (isCurrent) {
            TextView now = text("NOW", 8, BG);
            now.setGravity(Gravity.CENTER);
            now.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            now.setPadding(dp(6), dp(5), dp(6), dp(5));
            now.setBackground(round(ACCENT, Color.TRANSPARENT, 999));
            LinearLayout.LayoutParams nowParams = new LinearLayout.LayoutParams(-2, dp(24));
            nowParams.setMargins(dp(7), 0, 0, 0);
            card.addView(now, nowParams);
        }

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(0, -2, 1f);
        cardParams.setMargins(0, 0, 0, dp(9));
        outer.addView(card, cardParams);

        outer.setAlpha(isDone ? 0.50f : 1f);
        return outer;
    }

    private LinearLayout breakRow(TimetableData.ClassItem item) {
        boolean lunch = item.kind == TimetableData.Kind.LUNCH;
        int accent = lunch ? ACCENT_2 : ACCENT;
        int minutes = Math.max(1, (toSeconds(item.end) - toSeconds(item.start)) / 60);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(10));
        row.setBackgroundColor(Color.TRANSPARENT);

        View rail = new View(this);
        rail.setBackground(round(
                blend(accent, BG, 0.58f),
                Color.TRANSPARENT,
                999
        ));
        LinearLayout.LayoutParams railParams = new LinearLayout.LayoutParams(dp(3), dp(36));
        railParams.setMargins(0, 0, dp(9), 0);
        row.addView(rail, railParams);

        ImageView icon = new ImageView(this);
        icon.setImageResource(lunch ? R.drawable.ic_lunch : R.drawable.ic_coffee);
        icon.setColorFilter(accent);
        icon.setPadding(dp(6), dp(6), dp(6), dp(6));
        icon.setBackground(round(
                blend(accent, BG, 0.86f),
                Color.TRANSPARENT,
                999
        ));
        row.addView(icon, new LinearLayout.LayoutParams(dp(34), dp(34)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(10), 0, 0, 0);

        TextView title = text(
                lunch ? "LUNCH BREAK" : "SHORT BREAK",
                10,
                accent
        );
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setLetterSpacing(0.08f);
        copy.addView(title);

        TextView info = text(
                formatTime(item.start) + " → " + formatTime(item.end)
                        + "  •  " + minutes + " min",
                9,
                MUTED
        );
        info.setPadding(0, dp(3), 0, 0);
        copy.addView(info);

        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -2, 1f);
        row.addView(copy, copyParams);

        TextView badge = text(lunch ? "MEAL" : "RECHARGE", 7, accent);
        badge.setGravity(Gravity.CENTER);
        badge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        badge.setLetterSpacing(0.07f);
        badge.setPadding(dp(8), dp(6), dp(8), dp(6));
        badge.setBackground(round(
                blend(accent, BG, 0.84f),
                Color.TRANSPARENT,
                999
        ));
        row.addView(badge);

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
        if (heroRoomText == null || heroWingText == null) return;

        String roomValue = roomNumber(item);
        String wingValue = wing(item);

        heroRoomText.setText(
                roomValue.isEmpty() ? "ROOM\nNot listed" : "ROOM\n" + roomValue
        );
        heroWingText.setText(
                wingValue.isEmpty() ? "WING\nNot listed" : "WING\n" + wingValue
        );
    }

    private void clearHeroLocation() {
        if (heroRoomText == null || heroWingText == null) return;

        heroRoomText.setText("ROOM\n—");
        heroWingText.setText("WING\n—");
    }


    private TextView metricText(String label, String value, int color) {
        TextView t = text(label + "\n" + value, 11, color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        t.setPadding(dp(12), 0, dp(12), 0);
        return t;
    }

    private void animatePageIn(View view) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(dp(12));
        view.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(340)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void tap(View view) {
        if (view == null) return;
        view.animate()
                .scaleX(0.97f)
                .scaleY(0.97f)
                .setDuration(70)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(110)
                        .setInterpolator(new DecelerateInterpolator())
                        .start())
                .start();
    }

    private void animateHero(String state) {
        if (state.equals(lastHeroState)) return;
        lastHeroState = state;

        boolean current = state.startsWith("current:");
        if (current) {
            startHeroPulse();
        } else {
            stopHeroPulse();
        }

        if (homeHero != null) {
            homeHero.setAlpha(0.94f);
            homeHero.setTranslationY(dp(4));
            homeHero.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setDuration(260)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void startHeroPulse() {
        if (heroPulseDot == null || heroPulseAnimator != null) return;
        heroPulseAnimator = android.animation.ObjectAnimator.ofFloat(
                heroPulseDot,
                "alpha",
                0.35f,
                1f
        );
        heroPulseAnimator.setDuration(900);
        heroPulseAnimator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        heroPulseAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        heroPulseAnimator.start();
    }

    private void stopHeroPulse() {
        if (heroPulseAnimator != null) {
            heroPulseAnimator.cancel();
            heroPulseAnimator = null;
        }
        if (heroPulseDot != null) heroPulseDot.setAlpha(1f);
    }

    private long secondsUntilNextWeekendClass(TimetableData.ClassItem next) {
        LocalDate today = LocalDate.now(zone);
        LocalDate monday;

        if (today.getDayOfWeek() == DayOfWeek.SATURDAY) {
            monday = today.plusDays(2);
        } else {
            monday = today.plusDays(1);
        }

        LocalTime start = LocalTime.parse(next.start, timeFormatter);
        ZonedDateTime target = monday.atTime(start).atZone(zone);
        ZonedDateTime now = ZonedDateTime.now(zone);

        return Math.max(0L, target.toEpochSecond() - now.toEpochSecond());
    }

    private String formatHumanCountdown(long totalSeconds) {
        long safe = Math.max(0L, totalSeconds);
        long days = safe / 86400L;
        long hours = (safe % 86400L) / 3600L;
        long minutes = (safe % 3600L) / 60L;

        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
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

    private void openCall() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:7499517574"));
        try {
            startActivity(intent);
        } catch (Exception ignored) {
            // No dialer available; keep the screen unchanged.
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

    private LinearLayout contactButton(
            int iconRes,
            String title,
            String subtitle,
            String detail
    ) {
        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(12), dp(10), dp(14), dp(10));
        button.setBackground(round(SURFACE_2, BORDER, 16));
        button.setClickable(true);
        button.setFocusable(true);
        button.setContentDescription(title + ": " + detail);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setContentDescription(title + " logo");
        button.addView(icon, new LinearLayout.LayoutParams(dp(32), dp(32)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);

        TextView titleText = text(title, 12, TEXT);
        titleText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        copy.addView(titleText);

        TextView subtitleText = text(subtitle, 9, MUTED);
        subtitleText.setPadding(0, dp(2), 0, 0);
        copy.addView(subtitleText);

        TextView detailText = text(detail, 9, SUBTLE);
        detailText.setPadding(0, dp(2), 0, 0);
        copy.addView(detailText);

        LinearLayout.LayoutParams copyParams =
                new LinearLayout.LayoutParams(0, -2, 1f);
        copyParams.setMargins(dp(11), 0, 0, 0);
        button.addView(copy, copyParams);

        TextView arrow = text("›", 25, MUTED);
        arrow.setGravity(Gravity.CENTER);
        button.addView(arrow, new LinearLayout.LayoutParams(dp(22), dp(32)));

        installPressAnimation(button);
        return button;
    }

    private LinearLayout.LayoutParams contactParams() {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(62));
        params.height = dp(Math.max(64, CONTROL_HEIGHT + 16));
        params.setMargins(0, 0, 0, dp(9));
        return params;
    }

    private TextView actionButton(String label, boolean primary) {
        int fill = primary ? ACCENT : SURFACE_2;
        int textColor = primary ? BG : TEXT;

        TextView button = text(label, 10, textColor);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        button.setLetterSpacing(0.05f);
        button.setMinHeight(dp(CONTROL_HEIGHT));
        button.setBackground(ripple(
                fill,
                blend(fill, BG, 0.28f),
                primary ? ACCENT : BORDER,
                BUTTON_RADIUS
        ));
        button.setClickable(true);
        button.setFocusable(true);
        installPressAnimation(button);
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

    private StateListDrawable ripple(int normal, int pressed, int stroke, int radiusDp) {
        StateListDrawable states = new StateListDrawable();
        states.addState(
                new int[]{android.R.attr.state_pressed},
                round(pressed, stroke, radiusDp)
        );
        states.addState(
                new int[]{android.R.attr.state_focused},
                round(pressed, stroke, radiusDp)
        );
        states.addState(new int[]{}, round(normal, stroke, radiusDp));
        return states;
    }

    private GradientDrawable gradientRound(
            int start,
            int end,
            int stroke,
            int radiusDp,
            GradientDrawable.Orientation orientation
    ) {
        GradientDrawable d = new GradientDrawable(orientation, new int[]{start, end});
        d.setCornerRadius(dp(radiusDp));
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke);
        return d;
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


    private int CARD_RADIUS() { return CARD_RADIUS; }
    private int BUTTON_RADIUS() { return BUTTON_RADIUS; }
    private int PILL_RADIUS() { return 999; }
    private int CONTROL_HEIGHT_DP() { return CONTROL_HEIGHT; }

    private int screenIndex(String screen) {
        if ("home".equals(screen)) return 0;
        if ("week".equals(screen)) return 1;
        if ("expenses".equals(screen)) return 2;
        return 3;
    }

    private int themeHeadingSize() {
        switch (themeIndex) {
            case 1: return 30;
            case 2: return 28;
            case 3: return 30;
            case 4: return 31;
            default: return 29;
        }
    }

    private int themeContentPadding() {
        switch (themeIndex) {
            case 1: return 18;
            case 2: return 22;
            case 3: return 19;
            case 4: return 21;
            default: return 20;
        }
    }

    private int themeTopPadding() {
        switch (themeIndex) {
            case 1: return 20;
            case 2: return 24;
            case 3: return 17;
            case 4: return 22;
            default: return 18;
        }
    }

    private GradientDrawable.Orientation themeGradientOrientation() {
        switch (themeIndex) {
            case 1: return GradientDrawable.Orientation.LEFT_RIGHT;
            case 2: return GradientDrawable.Orientation.TOP_BOTTOM;
            case 3: return GradientDrawable.Orientation.TL_BR;
            case 4: return GradientDrawable.Orientation.BL_TR;
            default: return GradientDrawable.Orientation.TR_BL;
        }
    }

    private void animateScreenTransition(View view, boolean forward) {
        if (view == null) return;

        view.setAlpha(0f);
        view.setScaleX(0.985f);
        view.setScaleY(0.985f);
        view.setTranslationX(forward ? dp(36) : -dp(36));
        view.setTranslationY(dp(4));

        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationX(0)
                .translationY(0)
                .setDuration(420)
                .setInterpolator(new OvershootInterpolator(0.75f))
                .start();
    }

    private void animateThemeRefresh() {
        if (root == null) return;
        root.setAlpha(0.90f);
        root.setScaleX(0.985f);
        root.setScaleY(0.985f);
        root.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(420)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void animateListItem(View view, int index) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(dp(12));
        view.setScaleX(0.985f);
        view.setScaleY(0.985f);
        view.animate()
                .alpha(1f)
                .translationY(0)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(Math.min(320, index * 55L))
                .setDuration(360)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void installPressAnimation(View view) {
        if (view == null) return;
        view.setClickable(true);
        view.setFocusable(true);
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().cancel();
                    v.animate()
                            .scaleX(0.965f)
                            .scaleY(0.965f)
                            .translationY(dp(1))
                            .alpha(0.92f)
                            .setDuration(90)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().cancel();
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .translationY(0)
                            .alpha(1f)
                            .setDuration(240)
                            .setInterpolator(new OvershootInterpolator(1.15f))
                            .start();
                    break;
            }
            return false;
        });
    }

    private static final class SpendingChartView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bounds = new RectF();
        private java.util.Map<String, Long> totals = new java.util.HashMap<>();
        private long total;

        SpendingChartView(android.content.Context context) {
            super(context);
            paint.setStrokeCap(Paint.Cap.ROUND);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void setData(java.util.Map<String, Long> totals, long total) {
            this.totals = totals == null
                    ? new java.util.HashMap<>()
                    : new java.util.HashMap<>(totals);
            this.total = total;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) * 0.39f;
            bounds.set(cx - radius, cy - radius, cx + radius, cy + radius);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dpValue(10));
            paint.setColor(Color.argb(55, 255, 255, 255));
            canvas.drawArc(bounds, 0, 360, false, paint);

            if (total > 0) {
                float startAngle = -90f;
                for (int i = 0; i < ExpenseStore.CATEGORIES.length; i++) {
                    String category = ExpenseStore.CATEGORIES[i];
                    long amount = totals.containsKey(category) ? totals.get(category) : 0L;
                    if (amount <= 0) continue;

                    float sweep = 360f * ((float) amount / (float) total);
                    paint.setColor(chartColor(i));
                    canvas.drawArc(bounds, startAngle, Math.max(4f, sweep - 3f), false, paint);
                    startAngle += sweep;
                }
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(TEXT);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            paint.setTextSize(dpValue(16));
            canvas.drawText(total > 0 ? compactRupees(total) : "₹0", cx, cy + dpValue(5), paint);

            paint.setColor(MUTED);
            paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            paint.setTextSize(dpValue(8));
            canvas.drawText("TOTAL", cx, cy + dpValue(19), paint);
        }

        private int chartColor(int index) {
            if (index % 2 == 0) return ACCENT;
            if (index % 3 == 0) return ACCENT_2;
            return blend(ACCENT, ACCENT_2, 0.48f);
        }

        private String compactRupees(long paise) {
            double rupees = paise / 100d;
            if (rupees >= 100000d) return "₹" + String.format(Locale.ENGLISH, "%.1fL", rupees / 100000d);
            if (rupees >= 1000d) return "₹" + String.format(Locale.ENGLISH, "%.1fK", rupees / 1000d);
            return formatRupees(paise);
        }

        private float dpValue(float dp) {
            return dp * getResources().getDisplayMetrics().density;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }


}
