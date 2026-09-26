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
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
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

    private int themeIndex;
    private String selectedScreen = "home";
    private Runnable refreshRunnable;
    private android.animation.ObjectAnimator heroPulseAnimator;
    private String lastHeroState = "";

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
                28,
                GradientDrawable.Orientation.LEFT_RIGHT
        ));
        nav.setElevation(dp(16));

        nav.addView(navItem("home", "Home", R.drawable.ic_home), navWeight());
        nav.addView(navItem("week", "Week", R.drawable.ic_calendar), navWeight());
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

        item.setOnClickListener(v -> {
            tap(v);
            navigate(id);
        });
        return item;
    }

    private void updateBottomNav() {
        if (bottomNav == null) return;

        for (int i = 0; i < bottomNav.getChildCount(); i++) {
            View child = bottomNav.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;

            String id = i == 0 ? "home" : (i == 1 ? "week" : "settings");
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
                    20
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
            buildHomeScreen();
        } else if ("week".equals(screen)) {
            buildWeekScreen();
        } else {
            buildSettingsScreen();
        }

        updateBottomNav();
        animatePageIn(contentHost);
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

        TextView eyebrow = sectionLabel("SMART TIMETABLE");
        eyebrow.setTextColor(ACCENT);
        titleBlock.addView(eyebrow);

        TextView brand = text("NextBell", 29, TEXT);
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
        profileBadge.setOnClickListener(v -> {
            tap(v);
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
        homeHero.setElevation(dp(10));
        homeHero.setBackground(gradientRound(SURFACE, SURFACE_2, BORDER, 24,
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

        TextView title = text("Your week", 29, TEXT);
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

    private void buildSettingsScreen() {
        LinearLayout page = scrollPage();

        TextView eyebrow = sectionLabel("PERSONALISE");
        eyebrow.setTextColor(ACCENT);
        page.addView(eyebrow);

        TextView title = text("Settings", 29, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(5), 0, 0);
        page.addView(title);

        TextView subtitle = text("Make NextBell feel like yours.", 12, MUTED);
        subtitle.setPadding(0, dp(5), 0, dp(18));
        page.addView(subtitle);

        page.addView(sectionLabel("PROFILE"));

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

        LinearLayout.LayoutParams profileTextParams = new LinearLayout.LayoutParams(0, -2, 1f);
        profileTextParams.setMargins(dp(12), 0, dp(8), 0);
        profileCard.addView(profileText, profileTextParams);

        TextView edit = actionButton("Edit", false);
        profileCard.addView(edit, new LinearLayout.LayoutParams(dp(76), dp(42)));
        edit.setOnClickListener(v -> {
            tap(v);
            showEditProfile();
        });

        LinearLayout.LayoutParams profileParams = new LinearLayout.LayoutParams(-1, -2);
        profileParams.setMargins(0, dp(9), 0, dp(20));
        page.addView(profileCard, profileParams);

        page.addView(sectionLabel("APPEARANCE"));

        TextView appearanceHint = text(
                "A different mood for every kind of day.",
                10,
                MUTED
        );
        appearanceHint.setPadding(0, dp(5), 0, dp(10));
        page.addView(appearanceHint);

        HorizontalScrollView themeScroller = new HorizontalScrollView(this);
        themeScroller.setHorizontalScrollBarEnabled(false);
        themeScroller.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout themeRow = new LinearLayout(this);
        for (int i = 0; i < THEME_NAMES.length; i++) {
            LinearLayout card = themeCard(i);
            themeRow.addView(card, new LinearLayout.LayoutParams(dp(132), dp(108)));
            if (i < THEME_NAMES.length - 1) themeRow.addView(spacer(dp(9), 1));
        }
        themeScroller.addView(themeRow);
        LinearLayout.LayoutParams themeParams = new LinearLayout.LayoutParams(-1, dp(108));
        themeParams.setMargins(0, 0, 0, dp(21));
        page.addView(themeScroller, themeParams);

        page.addView(sectionLabel("NOTIFICATIONS"));

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
        notifDescription.setPadding(0, dp(3), 0, 0);
        notifText.addView(notifDescription);

        notificationCard.addView(notifText, new LinearLayout.LayoutParams(0, -2, 1f));

        if (!isNotificationGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            TextView enable = actionButton("Enable", false);
            notificationCard.addView(enable, new LinearLayout.LayoutParams(dp(82), dp(40)));
            enable.setOnClickListener(v -> {
                tap(v);
                requestNotificationAccess();
            });
        } else {
            TextView status = text("ON", 9, ACCENT);
            status.setGravity(Gravity.CENTER);
            status.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            status.setPadding(dp(10), 0, dp(10), 0);
            status.setBackground(round(ACCENT_BG, Color.TRANSPARENT, 999));
            notificationCard.addView(status, new LinearLayout.LayoutParams(dp(46), dp(32)));
        }

        LinearLayout.LayoutParams notifParams = new LinearLayout.LayoutParams(-1, -2);
        notifParams.setMargins(0, dp(9), 0, dp(20));
        page.addView(notificationCard, notifParams);

        page.addView(sectionLabel("ABOUT & FEEDBACK"));

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

        LinearLayout aboutParams = new LinearLayout(this);
        aboutParams.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        page.addView(aboutCard, new LinearLayout.LayoutParams(-1, -2));

        addBottomSpace(page);
    }

    private LinearLayout scrollPage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setBackgroundColor(BG);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(19), dp(22), dp(19), dp(30));
        scroll.addView(body);

        contentHost.removeAllViews();
        contentHost.addView(scroll);
        return body;
    }

    private void addBottomSpace(LinearLayout page) {
        page.addView(spacer(1, dp(42)));
    }

    private LinearLayout cardColumn() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(17), dp(17), dp(17), dp(17));
        card.setBackground(gradientRound(
                SURFACE,
                SURFACE_2,
                BORDER,
                22,
                GradientDrawable.Orientation.TL_BR
        ));
        card.setElevation(dp(5));
        return card;
    }

    private LinearLayout settingRow(int iconRes, int iconColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(13), dp(13), dp(13), dp(13));
        card.setBackground(ripple(SURFACE, blend(SURFACE, BG, 0.25f), BORDER, 20));
        card.setElevation(dp(3));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(iconColor);
        icon.setPadding(dp(7), dp(7), dp(7), dp(7));
        icon.setBackground(round(ACCENT_BG, Color.TRANSPARENT, 15));
        card.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));
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
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackground(ripple(
                selected ? palette[7] : palette[1],
                blend(palette[7], palette[0], 0.25f),
                selected ? palette[6] : palette[3],
                19
        ));
        card.setElevation(selected ? dp(5) : dp(2));
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription(
                "Theme " + THEME_NAMES[index] + (selected ? ", selected" : "")
        );

        LinearLayout swatches = new LinearLayout(this);
        swatches.setOrientation(LinearLayout.HORIZONTAL);

        int[] swatchColors = {palette[0], palette[1], palette[6], Color.WHITE};
        for (int i = 0; i < swatchColors.length; i++) {
            View swatch = new View(this);
            swatch.setBackground(round(
                    swatchColors[i],
                    Color.TRANSPARENT,
                    999
            ));
            LinearLayout.LayoutParams swatchParams = new LinearLayout.LayoutParams(dp(23), dp(23));
            if (i > 0) swatchParams.setMargins(dp(-4), 0, 0, 0);
            swatches.addView(swatch, swatchParams);
        }
        card.addView(swatches);

        TextView title = text(THEME_NAMES[index], 12, palette[6]);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setPadding(0, dp(10), 0, 0);
        card.addView(title);

        TextView status = text(
                selected ? "Selected  ✓" : "Tap to apply",
                8,
                selected ? palette[4] : palette[5]
        );
        status.setPadding(0, dp(3), 0, 0);
        card.addView(status);

        card.setOnClickListener(v -> {
            tap(v);
            if (themeIndex == index) return;

            themeIndex = index;
            preferences.edit().putInt(KEY_THEME, themeIndex).apply();
            applyTheme();

            String keepScreen = selectedScreen;
            buildShell();
            selectedScreen = keepScreen;
            navigate(keepScreen);
        });

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
            homeHero.setBackground(gradientRound(
                    SURFACE,
                    ACCENT_BG,
                    ACCENT,
                    24,
                    GradientDrawable.Orientation.TL_BR
            ));
            heroLabel.setText("HAPPENING NOW");
            heroSubject.setText(current.subject);
            heroMeta.setText(formatTeacher(current));
            heroTimeRange.setText(formatTime(current.start) + " → " + formatTime(current.end));
            setHeroLocation(current);
            heroCountdown.setText(
                    formatCountdown(Math.max(0, toSeconds(current.end) - now))
            );
            heroCountdownLabel.setText("until it ends");
            animateHero("current:" + current.id);
        } else if (next != null) {
            homeHero.setBackground(gradientRound(
                    SURFACE,
                    SURFACE_2,
                    BORDER,
                    24,
                    GradientDrawable.Orientation.TL_BR
            ));
            heroLabel.setText("NEXT CLASS");
            heroSubject.setText(next.subject);
            heroMeta.setText(formatTeacher(next));
            heroTimeRange.setText(formatTime(next.start) + " → " + formatTime(next.end));
            setHeroLocation(next);

            if (isWeekend) {
                long secondsUntil =
                        secondsUntilNextWeekendClass(next);
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
                    24,
                    GradientDrawable.Orientation.TL_BR
            ));
            heroLabel.setText("DAY COMPLETE");
            heroTimeRange.setText("—");
            heroSubject.setText("You're done for today.");
            heroMeta.setText("No more classes scheduled");
            clearHeroLocation();
            heroCountdown.setText("✓");
            heroCountdownLabel.setText("nothing else scheduled");
            animateHero("done");
        }

        homeDayLabel.setText(
                isWeekend
                        ? "MONDAY PREVIEW"
                        : "TODAY  •  " + formatTodayDate()
        );
        homeSummary.setText(entries.size() + " blocks  •  " + getBatch());

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
        boolean selected = day.equals(weekSelectedDay);
        TextView chip = text(day.substring(0, 3).toUpperCase(), 12, selected ? ACCENT : TEXT);
        chip.setGravity(Gravity.CENTER);
        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        chip.setBackground(ripple(
                selected ? ACCENT_BG : SURFACE,
                blend(ACCENT_BG, BG, 0.35f),
                selected ? ACCENT : Color.TRANSPARENT,
                18
        ));
        chip.setClickable(true);
        chip.setFocusable(true);
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
                isCurrent ? ACCENT : (isDone ? BORDER : ACCENT),
                Color.TRANSPARENT,
                999
        ));
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(isCurrent ? 10 : 7), dp(isCurrent ? 10 : 7));
        dotParams.setMargins(0, dp(7), 0, dp(6));
        timeline.addView(dot, dotParams);

        View line = new View(this);
        line.setBackground(round(BORDER, Color.TRANSPARENT, 999));
        timeline.addView(line, new LinearLayout.LayoutParams(dp(2), dp(44)));

        LinearLayout.LayoutParams timelineParams = new LinearLayout.LayoutParams(dp(64), -1);
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
                19,
                GradientDrawable.Orientation.LEFT_RIGHT
        ));
        card.setElevation(isCurrent ? dp(6) : dp(2));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);

        TextView subject = text(item.subject, 13, isDone ? MUTED : TEXT);
        subject.setTypeface(Typeface.DEFAULT_BOLD);
        main.addView(subject);

        TextView details = text(detailsFor(item), 9, MUTED);
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
        cardParams.setMargins(0, 0, 0, dp(8));
        outer.addView(card, cardParams);

        outer.setAlpha(isDone ? 0.50f : 1f);
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

        return button;
    }

    private LinearLayout.LayoutParams contactParams() {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(62));
        params.setMargins(0, 0, 0, dp(8));
        return params;
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }


}
