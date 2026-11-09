package com.nealaringale.nextbell;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.view.WindowInsets;
import android.graphics.Insets;
import android.content.Context;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS_NAME = "nextbell_profile";
    private static final String KEY_NAME = "name";
    private static final String KEY_ROLL_NUMBER = "roll_number";
    private static final String KEY_NOTIFICATION_PROMPTED = "notification_prompted";
    private static final String KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested";
    private static final String KEY_THEME = "theme";
    private static final String KEY_REDUCED_MOTION = "reduced_motion";
    private static final String KEY_PENDING_PAYMENT_ALERT_SETUP = "pending_payment_alert_setup";
    private static final int REQUEST_EXPORT_BACKUP = 8101;
    private static final int REQUEST_IMPORT_BACKUP = 8102;

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
    private TextView expenseWeeklySpend;
    private TextView expenseVsLastMonth;
    private TextView expenseStreak;
    private TextView expenseMonthlyIncome;
    private TextView expenseDailyLimitStatus;
    private TextView expenseWeeklyAllowanceStatus;
    private TextView expenseDailySafeSpend;
    private TextView expenseDailyAlertStatus;
    private TextView expenseRecurringStatus;
    private LocalDate expenseMonth = LocalDate.now(zone).withDayOfMonth(1);

    private String lastHomeScheduleSignature = "";
    private String lastWeekScheduleSignature = "";
    private int lastScreenIndex = 0;

    private int themeIndex;
    private boolean reducedMotion;
    private String selectedScreen = "home";
    private Runnable refreshRunnable;
    private boolean openedNotificationSettings;
    private android.animation.ObjectAnimator heroPulseAnimator;
    private String lastHeroState = "";

    private static final String[] THEME_NAMES = {
            "Midnight", "Tide", "Petal", "Grove", "Amber", "Porcelain"
    };

    private static final String[] THEME_TAGS = {
            "AFTER DARK", "COOL LIGHT", "SOFT ROSE", "BOTANICAL", "WARM LIGHT", "PAPER"
    };

    private static final String[] THEME_DESCRIPTIONS = {
            "Ink surfaces, cool glass and a mint signal color",
            "Deep blue glass with a clear, icy accent",
            "Muted rose surfaces with warm, quiet contrast",
            "Forest glass with a fresh botanical accent",
            "Warm charcoal with a restrained amber highlight",
            "A bright, paper-like surface with ink and indigo"
    };

    // BG, SURFACE, SURFACE_2, BORDER, TEXT, MUTED, ACCENT, ACCENT_BG, ACCENT_2
    private static final int[][] THEMES = {
            {Color.rgb(8, 11, 17), Color.rgb(17, 22, 31), Color.rgb(26, 34, 46), Color.rgb(55, 68, 86), Color.rgb(245, 248, 252), Color.rgb(157, 170, 189), Color.rgb(139, 240, 190), Color.rgb(24, 66, 52), Color.rgb(112, 210, 230)},
            {Color.rgb(8, 15, 28), Color.rgb(16, 27, 46), Color.rgb(25, 41, 66), Color.rgb(49, 70, 101), Color.rgb(242, 247, 255), Color.rgb(159, 177, 204), Color.rgb(126, 205, 255), Color.rgb(23, 59, 95), Color.rgb(169, 168, 255)},
            {Color.rgb(24, 15, 23), Color.rgb(35, 23, 34), Color.rgb(50, 33, 47), Color.rgb(78, 54, 73), Color.rgb(252, 244, 249), Color.rgb(188, 160, 181), Color.rgb(242, 151, 194), Color.rgb(79, 43, 66), Color.rgb(248, 194, 135)},
            {Color.rgb(10, 19, 16), Color.rgb(20, 32, 27), Color.rgb(30, 47, 39), Color.rgb(54, 77, 65), Color.rgb(242, 248, 243), Color.rgb(163, 184, 171), Color.rgb(142, 220, 174), Color.rgb(30, 70, 52), Color.rgb(175, 211, 193)},
            {Color.rgb(22, 18, 13), Color.rgb(34, 28, 20), Color.rgb(49, 40, 28), Color.rgb(79, 64, 43), Color.rgb(250, 246, 237), Color.rgb(190, 176, 148), Color.rgb(238, 183, 91), Color.rgb(77, 57, 28), Color.rgb(232, 142, 105)},
            {Color.rgb(242, 244, 248), Color.rgb(255, 255, 255), Color.rgb(233, 237, 244), Color.rgb(207, 215, 228), Color.rgb(29, 38, 54), Color.rgb(101, 113, 132), Color.rgb(77, 103, 204), Color.rgb(225, 232, 252), Color.rgb(36, 151, 143)}
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
        reducedMotion = preferences.getBoolean(KEY_REDUCED_MOTION, false);
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
    protected void onResume() {
        super.onResume();
        if (preferences != null && openedNotificationSettings && isNotificationGranted()) {
            openedNotificationSettings = false;
            NotificationScheduler.scheduleUpcoming(this);
        }
        if (preferences != null
                && preferences.getBoolean(KEY_PENDING_PAYMENT_ALERT_SETUP, false)) {
            preferences.edit().remove(KEY_PENDING_PAYMENT_ALERT_SETUP).apply();
            if (TransactionNotificationAccess.hasAccess(this)) {
                SmsTransactionService.setEnabled(preferences, true);
                Toast.makeText(this, "Payment alert tracking is connected.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Notification access was not enabled. Tracking remains off.",
                        Toast.LENGTH_LONG).show();
            }
        }
        if (preferences != null && "settings".equals(selectedScreen)) {
            buildSettingsScreen();
            updateBottomNav();
        } else if (preferences != null && "expenses".equals(selectedScreen)) {
            buildExpensesScreen();
            updateBottomNav();
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
                CARD_ELEVATION = 5;
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
                CARD_ELEVATION = 5;
                CONTROL_HEIGHT = 48;
                break;
        }

        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int systemUi = Build.VERSION.SDK_INT < Build.VERSION_CODES.R
                    ? View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    : 0;
            if (themeIndex == 5) {
                systemUi |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            window.getDecorView().setSystemUiVisibility(systemUi);
        }
    }

    private void openApp() {
        buildShell();
        navigate(selectedScreen);
        requestNotificationPermissionIfNeeded();
        StudentPlannerScheduler.scheduleAll(this);
        NextClassWidgetProvider.refreshAll(this);

        if (refreshRunnable != null) {
            handler.removeCallbacks(refreshRunnable);
        }

        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if ("home".equals(selectedScreen)) {
                    updateClock();
                    refreshHome();
                }
                handler.postDelayed(this, 1_000);
            }
        };
        handler.postDelayed(refreshRunnable, 1_000);
    }

    private void buildShell() {
        root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        applySystemBarInsets(root);

        contentHost = new FrameLayout(this);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(-1, -1);
        contentParams.bottomMargin = dp(94);
        root.addView(contentHost, contentParams);

        bottomNav = buildBottomNav();
        FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(-1, dp(74), Gravity.BOTTOM);
        navParams.setMargins(dp(15), 0, dp(15), dp(12));
        root.addView(bottomNav, navParams);

        setContentView(root);
        root.requestApplyInsets();
    }

    private void applySystemBarInsets(View view) {
        int baseLeft = view.getPaddingLeft();
        int baseTop = view.getPaddingTop();
        int baseRight = view.getPaddingRight();
        int baseBottom = view.getPaddingBottom();
        view.setOnApplyWindowInsetsListener((target, insets) -> {
            int left;
            int top;
            int right;
            int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()
                );
                Insets ime = insets.getInsets(WindowInsets.Type.ime());
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = Math.max(bars.bottom, ime.bottom);
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            target.setPadding(
                    baseLeft + left,
                    baseTop + top,
                    baseRight + right,
                    baseBottom + bottom
            );
            return insets;
        });
    }

    private LinearLayout buildBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(7), dp(8), dp(7));
        nav.setBackground(glassGradient(
                SURFACE_2,
                blend(SURFACE_2, BG, 0.45f),
                BORDER,
                NAV_RADIUS,
                GradientDrawable.Orientation.LEFT_RIGHT
        ));
        nav.setElevation(dp(9));

        nav.addView(navItem("home", "Home", R.drawable.ic_home), navWeight());
        nav.addView(navItem("week", "Week", R.drawable.ic_calendar), navWeight());
        nav.addView(navItem("expenses", "Money", R.drawable.ic_wallet), navWeight());
        nav.addView(navItem("study", "Study", R.drawable.ic_study), navWeight());
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
        item.setTag(id);

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

            String id = String.valueOf(child.getTag());
            boolean selected = id.equals(selectedScreen);
            LinearLayout item = (LinearLayout) child;
            item.setSelected(selected);
            item.setContentDescription(labelForScreen(id) + (selected ? ", selected" : ""));
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
            if (reducedMotion) {
                item.setScaleX(1f);
                item.setScaleY(1f);
            } else {
                item.animate()
                        .scaleX(selected ? 1.03f : 1f)
                        .scaleY(selected ? 1.03f : 1f)
                        .setDuration(170)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        }
    }

    private String labelForScreen(String id) {
        switch (id) {
            case "week": return "Week";
            case "expenses": return "Money";
            case "study": return "Study";
            case "settings": return "Settings";
            default: return "Home";
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
        } else if ("study".equals(screen)) {
            buildStudyScreen();
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
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setBackgroundColor(BG);

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

        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        applySystemBarInsets(scroll);
        setContentView(scroll);
        scroll.requestApplyInsets();
        animatePageIn(scroll);
        if (!reducedMotion) {
            logo.animate().rotationBy(360f)
                    .setDuration(700)
                    .setStartDelay(120)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void buildHomeScreen() {
        lastHeroState = "";
        LinearLayout page = scrollPage();

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        top.addView(titleBlock, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView eyebrow = sectionLabel("YOUR DAY");
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

        TextView liveTag = text("●  LOCAL TIME", 8, SUBTLE);
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
        homeHero.setBackground(glassGradient(SURFACE, SURFACE_2, BORDER, CARD_RADIUS,
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

        TextView helper = text("Classes and breaks, in order.", 10, MUTED);
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

    private void buildStudyScreen() {
        LinearLayout page = scrollPage();

        TextView eyebrow = sectionLabel("STUDENT DESK");
        eyebrow.setTextColor(ACCENT);
        page.addView(eyebrow);

        TextView title = text("Study & plans", themeHeadingSize(), TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(5), 0, 0);
        page.addView(title);

        TextView subtitle = text(
                "Keep attendance, deadlines and small next steps in one place.", 11, MUTED
        );
        subtitle.setPadding(0, dp(5), 0, dp(18));
        page.addView(subtitle);

        page.addView(sectionLabel("ATTENDANCE"), sectionParams(0, 8));
        LinearLayout attendanceCard = cardColumn();
        LinearLayout attendanceHeader = new LinearLayout(this);
        attendanceHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout attendanceCopy = new LinearLayout(this);
        attendanceCopy.setOrientation(LinearLayout.VERTICAL);
        TextView attendanceTitle = text("Your running record", 15, TEXT);
        attendanceTitle.setTypeface(Typeface.DEFAULT_BOLD);
        attendanceCopy.addView(attendanceTitle);
        TextView attendanceHint = text(
                "Mark each scheduled class. Bunk guidance uses only sessions you record.", 9, MUTED
        );
        attendanceHint.setPadding(0, dp(4), 0, 0);
        attendanceCopy.addView(attendanceHint);
        attendanceHeader.addView(attendanceCopy,
                new LinearLayout.LayoutParams(0, -2, 1f));

        int target = AttendanceStore.targetPercent(preferences);
        TextView targetButton = actionButton(target + "% TARGET", false);
        attendanceHeader.addView(targetButton,
                new LinearLayout.LayoutParams(dp(102), dp(38)));
        targetButton.setOnClickListener(v -> {
            tap(v);
            String[] values = {"65%", "70%", "75%", "80%", "85%", "90%"};
            int selected = java.util.Arrays.asList(values).indexOf(target + "%");
            new AlertDialog.Builder(this)
                    .setTitle("Attendance target")
                    .setSingleChoiceItems(values, selected, (dialog, which) -> {
                        AttendanceStore.setTargetPercent(preferences,
                                Integer.parseInt(values[which].replace("%", "")));
                        dialog.dismiss();
                        buildStudyScreen();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        attendanceCard.addView(attendanceHeader);

        LinearLayout attendanceActions = new LinearLayout(this);
        attendanceActions.setGravity(Gravity.CENTER_VERTICAL);
        TextView markDate = actionButton("MARK A CLASS", true);
        TextView history = actionButton("CHOOSE DATE", false);
        attendanceActions.addView(markDate,
                new LinearLayout.LayoutParams(0, dp(44), 1f));
        LinearLayout.LayoutParams historyParams =
                new LinearLayout.LayoutParams(0, dp(44), 1f);
        historyParams.setMargins(dp(8), 0, 0, 0);
        attendanceActions.addView(history, historyParams);
        markDate.setOnClickListener(v -> {
            tap(v);
            showAttendanceForDate(LocalDate.now(zone));
        });
        history.setOnClickListener(v -> {
            tap(v);
            LocalDate today = LocalDate.now(zone);
            new DatePickerDialog(this, (picker, year, month, day) ->
                    showAttendanceForDate(LocalDate.of(year, month + 1, day)),
                    today.getYear(), today.getMonthValue() - 1, today.getDayOfMonth()).show();
        });
        attendanceCard.addView(attendanceActions,
                sectionParams(0, 12));

        List<AttendanceStore.Session> sessions = AttendanceStore.load(preferences);
        java.util.Map<String, TimetableData.ClassItem> courses = new java.util.LinkedHashMap<>();
        for (String day : TimetableData.DAYS) {
            for (TimetableData.ClassItem item :
                    TimetableData.forRollAndDay(getRollNumber(), day)) {
                if (item.isAcademic() && !item.subject.trim().isEmpty()) {
                    courses.putIfAbsent(item.subject, item);
                }
            }
        }
        if (courses.isEmpty()) {
            attendanceCard.addView(text(
                    "No course list is available for this roll number yet.", 10, MUTED
            ));
        } else {
            for (String course : courses.keySet()) {
                AttendanceStore.Stats stats = AttendanceStore.stats(sessions, course, target);
                LinearLayout row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(9), 0, dp(9));
                LinearLayout copy = new LinearLayout(this);
                copy.setOrientation(LinearLayout.VERTICAL);
                TextView courseTitle = text(course, 11, TEXT);
                courseTitle.setTypeface(Typeface.DEFAULT_BOLD);
                courseTitle.setMaxLines(2);
                copy.addView(courseTitle);
                String status;
                if (stats.held == 0) {
                    status = "No sessions marked yet";
                } else if (stats.classesToTarget() > 0) {
                    status = stats.attended + " / " + stats.held + " attended · "
                            + stats.classesToTarget() + " consecutive classes to reach " + target + "%";
                } else {
                    status = stats.attended + " / " + stats.held + " attended · "
                            + stats.safeAbsences() + " safe absences at " + target + "%";
                }
                TextView courseMeta = text(status, 8, MUTED);
                courseMeta.setPadding(0, dp(3), 0, 0);
                copy.addView(courseMeta);
                row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
                TextView percentage = text(
                        stats.held == 0 ? "—" : stats.percentage() + "%", 15,
                        stats.held > 0 && stats.percentage() >= target ? ACCENT : ACCENT_2
                );
                percentage.setTypeface(Typeface.DEFAULT_BOLD);
                row.addView(percentage);
                attendanceCard.addView(row);
                if (courses.size() > 1 && !course.equals(
                        new java.util.ArrayList<>(courses.keySet()).get(courses.size() - 1))) {
                    attendanceCard.addView(divider());
                }
            }
        }
        page.addView(attendanceCard, cardMargin());

        page.addView(sectionLabel("ASSIGNMENTS & TASKS"), sectionParams(0, 8));
        LinearLayout plannerCard = cardColumn();
        TextView plannerHint = text(
                "Deadlines stay on this phone. NextBell can remind you the day before.", 9, MUTED
        );
        plannerHint.setPadding(0, 0, 0, dp(12));
        plannerCard.addView(plannerHint);

        LinearLayout addRows = new LinearLayout(this);
        addRows.setGravity(Gravity.CENTER_VERTICAL);
        String[] kinds = {StudentPlannerStore.ASSIGNMENT, StudentPlannerStore.EXAM,
                StudentPlannerStore.TASK};
        String[] labels = {"+ ASSIGNMENT", "+ EXAM", "+ TASK"};
        for (int i = 0; i < kinds.length; i++) {
            final String kind = kinds[i];
            TextView add = actionButton(labels[i], i == 0);
            LinearLayout.LayoutParams addParams =
                    new LinearLayout.LayoutParams(0, dp(42), 1f);
            if (i > 0) addParams.setMargins(dp(6), 0, 0, 0);
            addRows.addView(add, addParams);
            add.setOnClickListener(v -> {
                tap(v);
                showStudentItemEditor(kind, null);
            });
        }
        plannerCard.addView(addRows);

        List<StudentPlannerStore.Item> plannerItems = StudentPlannerStore.load(preferences);
        if (plannerItems.isEmpty()) {
            TextView empty = text(
                    "Nothing on the list. Add a deadline or one small next step.", 10, MUTED
            );
            empty.setPadding(0, dp(14), 0, dp(3));
            plannerCard.addView(empty);
        } else {
            for (StudentPlannerStore.Item item : plannerItems) {
                plannerCard.addView(studentPlannerRow(item));
            }
        }
        page.addView(plannerCard, cardMargin());

        LinearLayout infoCard = cardColumn();
        TextView infoTitle = text("Class details", 13, TEXT);
        infoTitle.setTypeface(Typeface.DEFAULT_BOLD);
        infoCard.addView(infoTitle);
        TextView info = text(
                "Your timetable includes published classroom and faculty details. Check Week for the full schedule; NextBell does not invent campus map locations.",
                9, MUTED
        );
        info.setPadding(0, dp(4), 0, 0);
        infoCard.addView(info);
        page.addView(infoCard, sectionParams(0, 18));
        addBottomSpace(page);
    }

    private void showAttendanceForDate(LocalDate date) {
        String day = date.getDayOfWeek().getDisplayName(
                java.time.format.TextStyle.FULL, Locale.ENGLISH
        );
        List<TimetableData.ClassItem> classes = TimetableData.forRollAndDay(getRollNumber(), day);
        classes = classes.stream().filter(TimetableData.ClassItem::isAcademic)
                .collect(java.util.stream.Collectors.toList());
        if (classes.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("No scheduled classes")
                    .setMessage("The published timetable has no classes for " + day + ". Choose a weekday with classes to record attendance.")
                    .setPositiveButton("Choose date", (dialog, which) -> {
                        new DatePickerDialog(this, (picker, year, month, dayOfMonth) ->
                                showAttendanceForDate(LocalDate.of(year, month + 1, dayOfMonth)),
                                date.getYear(), date.getMonthValue() - 1,
                                date.getDayOfMonth()).show();
                    })
                    .setNegativeButton("Done", null)
                    .show();
            return;
        }

        LinearLayout sheet = dialogSheet();
        TextView title = text("Attendance · " + formatLongDate(date), 20, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);
        TextView hint = text("Only mark a meeting once it has happened.", 9, MUTED);
        hint.setPadding(0, dp(4), 0, dp(12));
        sheet.addView(hint);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        List<AttendanceStore.Session> sessions = AttendanceStore.load(preferences);
        for (TimetableData.ClassItem item : classes) {
            LinearLayout row = cardColumn();
            row.setPadding(dp(12), dp(12), dp(12), dp(12));
            TextView subject = text(item.subject, 12, TEXT);
            subject.setTypeface(Typeface.DEFAULT_BOLD);
            row.addView(subject);
            String details = formatTime(item.start) + " – " + formatTime(item.end);
            if (!item.room.trim().isEmpty()) details += " · " + item.room;
            if (!item.teacher.trim().isEmpty()) details += "\n" + item.teacher;
            TextView detail = text(details, 8, MUTED);
            detail.setPadding(0, dp(3), 0, dp(8));
            row.addView(detail);

            LinearLayout choices = new LinearLayout(this);
            choices.setGravity(Gravity.CENTER_VERTICAL);
            TextView present = actionButton("PRESENT", true);
            TextView absent = actionButton("ABSENT", false);
            TextView clear = actionButton("CLEAR", false);
            TextView[] actions = {present, absent, clear};
            for (TextView action : actions) {
                LinearLayout.LayoutParams actionParams =
                        new LinearLayout.LayoutParams(0, dp(36), 1f);
                if (choices.getChildCount() > 0) actionParams.setMargins(dp(5), 0, 0, 0);
                choices.addView(action, actionParams);
            }
            String status = AttendanceStore.statusFor(sessions, date, item);
            present.setAlpha("PRESENT".equals(status) ? 1f : 0.72f);
            absent.setAlpha("ABSENT".equals(status) ? 1f : 0.72f);
            clear.setEnabled(!status.isEmpty());
            present.setOnClickListener(v -> {
                AttendanceStore.mark(preferences, date, item, "PRESENT");
                buildStudyScreen();
                present.setAlpha(1f);
                absent.setAlpha(0.72f);
                clear.setEnabled(true);
            });
            absent.setOnClickListener(v -> {
                AttendanceStore.mark(preferences, date, item, "ABSENT");
                buildStudyScreen();
                present.setAlpha(0.72f);
                absent.setAlpha(1f);
                clear.setEnabled(true);
            });
            clear.setOnClickListener(v -> {
                AttendanceStore.clear(preferences, date, item);
                buildStudyScreen();
                present.setAlpha(0.72f);
                absent.setAlpha(0.72f);
                clear.setEnabled(false);
            });
            row.addView(choices);
            LinearLayout.LayoutParams rowParams =
                    new LinearLayout.LayoutParams(-1, -2);
            rowParams.setMargins(0, 0, 0, dp(8));
            list.addView(row, rowParams);
        }
        scroll.addView(list);
        sheet.addView(scroll, new LinearLayout.LayoutParams(-1, dp(390)));
        TextView done = dialogAction("DONE", true);
        LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(-1, dp(44));
        doneParams.setMargins(0, dp(10), 0, 0);
        sheet.addView(done, doneParams);
        Dialog dialog = showBottomSheet(sheet);
        done.setOnClickListener(v -> dialog.dismiss());
    }

    private View divider() {
        View view = new View(this);
        view.setBackgroundColor(BORDER);
        view.setAlpha(0.55f);
        view.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1)));
        return view;
    }

    private View studentPlannerRow(StudentPlannerStore.Item item) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(11), 0, dp(11));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(item.title, 11, item.completed ? MUTED : TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setMaxLines(2);
        copy.addView(title);
        String meta = item.type + (item.course.isEmpty() ? "" : " · " + item.course);
        if (item.dueDate != null) meta += " · " + formatLongDate(item.dueDate);
        TextView details = text(meta, 8, MUTED);
        details.setPadding(0, dp(3), 0, 0);
        copy.addView(details);
        if (!item.details.isEmpty()) {
            TextView note = text(item.details, 8, MUTED);
            note.setMaxLines(2);
            note.setPadding(0, dp(2), 0, 0);
            copy.addView(note);
        }
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView done = actionButton(item.completed ? "UNDO" : "DONE", !item.completed);
        row.addView(done, new LinearLayout.LayoutParams(dp(66), dp(36)));
        row.setOnClickListener(v -> showStudentItemActions(item));
        done.setOnClickListener(v -> {
            StudentPlannerStore.Item updated = new StudentPlannerStore.Item(
                    item.id, item.type, item.title, item.course, item.details,
                    item.dueDate, !item.completed
            );
            StudentPlannerStore.upsert(preferences, updated);
            if (updated.completed) StudentPlannerScheduler.cancel(this, updated.id);
            else StudentPlannerScheduler.schedule(this, updated);
            buildStudyScreen();
        });
        return row;
    }

    private void showStudentItemActions(StudentPlannerStore.Item item) {
        String[] actions = {item.completed ? "Mark as open" : "Mark complete", "Edit", "Delete"};
        new AlertDialog.Builder(this)
                .setTitle(item.title)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        StudentPlannerStore.Item updated = new StudentPlannerStore.Item(
                                item.id, item.type, item.title, item.course, item.details,
                                item.dueDate, !item.completed
                        );
                        StudentPlannerStore.upsert(preferences, updated);
                        if (updated.completed) StudentPlannerScheduler.cancel(this, updated.id);
                        else StudentPlannerScheduler.schedule(this, updated);
                        buildStudyScreen();
                    } else if (which == 1) {
                        showStudentItemEditor(item.type, item);
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("Delete this item?")
                                .setMessage("This removes it from your local study list.")
                                .setNegativeButton("Keep", null)
                                .setPositiveButton("Delete", (confirm, ignored) -> {
                                    StudentPlannerStore.delete(preferences, item.id);
                                    StudentPlannerScheduler.cancel(this, item.id);
                                    buildStudyScreen();
                                }).show();
                    }
                }).show();
    }

    private void showStudentItemEditor(String type, StudentPlannerStore.Item existing) {
        EditText itemTitle = field("e.g. Complete lab report", false);
        itemTitle.setSingleLine(true);
        EditText course = field("Course or class (optional)", false);
        course.setSingleLine(true);
        EditText details = field("What needs to be done? (optional)", false);
        details.setMinLines(2);
        details.setGravity(Gravity.TOP | Gravity.START);
        LocalDate initialDate = LocalDate.now(zone);
        final LocalDate[] dueDate = {existing == null ? null : existing.dueDate};
        TextView dateButton = actionButton("NO DUE DATE", false);
        if (existing != null) {
            itemTitle.setText(existing.title);
            course.setText(existing.course);
            details.setText(existing.details);
        }
        updatePlannerDateButton(dateButton, dueDate[0]);
        dateButton.setOnClickListener(v -> {
            LocalDate seed = dueDate[0] == null ? initialDate : dueDate[0];
            new DatePickerDialog(this, (picker, year, month, day) -> {
                dueDate[0] = LocalDate.of(year, month + 1, day);
                updatePlannerDateButton(dateButton, dueDate[0]);
            }, seed.getYear(), seed.getMonthValue() - 1, seed.getDayOfMonth()).show();
        });

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(4), dp(6), dp(4), dp(2));
        form.addView(itemTitle, sectionParams(0, 8));
        form.addView(course, sectionParams(0, 8));
        form.addView(dateButton, sectionParams(0, 8));
        form.addView(details, sectionParams(0, 2));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "New " + type.toLowerCase(Locale.ENGLISH) : "Edit " + type.toLowerCase(Locale.ENGLISH))
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(existing == null ? "Add" : "Save", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String cleanTitle = itemTitle.getText().toString().trim();
                    if (cleanTitle.isEmpty()) {
                        itemTitle.setError("Add a short title first");
                        return;
                    }
                    long id = existing == null
                            ? StudentPlannerStore.nextId(preferences) : existing.id;
                    StudentPlannerStore.Item updated = new StudentPlannerStore.Item(
                            id, type, cleanTitle, course.getText().toString(),
                            details.getText().toString(), dueDate[0],
                            existing != null && existing.completed
                    );
                    StudentPlannerStore.upsert(preferences, updated);
                    StudentPlannerScheduler.schedule(this, updated);
                    dialog.dismiss();
                    buildStudyScreen();
                }));
        dialog.show();
    }

    private void updatePlannerDateButton(TextView button, LocalDate date) {
        button.setText(date == null ? "NO DUE DATE  ·  TAP TO SET" : "DUE  ·  " + formatLongDate(date));
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

        TextView title = text("Money", themeHeadingSize(), TEXT);
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

        LinearLayout autoStatus = new LinearLayout(this);
        autoStatus.setGravity(Gravity.CENTER_VERTICAL);
        autoStatus.setPadding(dp(13), dp(10), dp(13), dp(10));
        autoStatus.setBackground(round(
                SURFACE_2,
                Color.TRANSPARENT,
                SMALL_RADIUS
        ));

        TextView autoStatusTitle = text(
                "AUTOMATIC PAYMENT ALERTS",
                9,
                MUTED
        );
        autoStatusTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        autoStatusTitle.setLetterSpacing(0.06f);
        autoStatus.addView(autoStatusTitle, new LinearLayout.LayoutParams(0, -2, 1f));

        boolean paymentAccess = TransactionNotificationAccess.hasAccess(this);
        boolean paymentTracking = SmsTransactionService.isEnabled(preferences);
        TextView syncStatus = text(
                paymentAccess && paymentTracking ? "CONNECTED"
                        : paymentAccess ? "READY" : "NOT CONNECTED",
                8,
                paymentAccess && paymentTracking ? ACCENT : SUBTLE
        );
        autoStatus.addView(syncStatus);

        TextView syncButton = actionButton(paymentAccess ? "MANAGE" : "CONNECT", false);
        LinearLayout.LayoutParams syncButtonParams =
                new LinearLayout.LayoutParams(dp(84), dp(34));
        syncButtonParams.setMargins(dp(9), 0, 0, 0);
        autoStatus.addView(syncButton, syncButtonParams);
        syncButton.setOnClickListener(v -> {
            tap(v);
            showPaymentAlertTrackingManager();
        });

        autoStatus.setClickable(true);
        autoStatus.setFocusable(true);
        autoStatus.setBackground(ripple(SURFACE_2, blend(SURFACE_2, BG, 0.20f),
                BORDER, SMALL_RADIUS));
        autoStatus.setOnClickListener(v -> {
            tap(v);
            showPaymentAlertTrackingManager();
        });

        page.addView(autoStatus, sectionParams(0, 8));
        TextView paymentStatusHint = text(
                TransactionNotificationAccess.shortStatus(this, preferences), 8, MUTED
        );
        paymentStatusHint.setPadding(dp(4), 0, dp(4), 0);
        page.addView(paymentStatusHint, sectionParams(0, 12));

        LinearLayout toolsCard = cardColumn();
        TextView toolsTitle = text("Daily controls", 15, TEXT);
        toolsTitle.setTypeface(Typeface.DEFAULT_BOLD);
        toolsCard.addView(toolsTitle);

        TextView toolsHint = text(
                "Set daily and weekly student budgets, alerts, or recurring bills.",
                9,
                MUTED
        );
        toolsHint.setPadding(0, dp(4), 0, dp(12));
        toolsCard.addView(toolsHint);

        LinearLayout limitRow = new LinearLayout(this);
        limitRow.setGravity(Gravity.CENTER_VERTICAL);
        expenseDailyLimitStatus = text("Daily limit  •  Not set", 10, TEXT);
        expenseDailyLimitStatus.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        limitRow.addView(expenseDailyLimitStatus, new LinearLayout.LayoutParams(0, dp(42), 1f));
        TextView limitButton = actionButton("SET LIMIT", false);
        limitRow.addView(limitButton, new LinearLayout.LayoutParams(dp(96), dp(42)));
        limitButton.setOnClickListener(v -> {
            tap(v);
            showDailyLimitEditor();
        });
        toolsCard.addView(limitRow);

        LinearLayout allowanceRow = new LinearLayout(this);
        allowanceRow.setGravity(Gravity.CENTER_VERTICAL);
        expenseWeeklyAllowanceStatus = text("Weekly allowance  •  Not set", 10, TEXT);
        expenseWeeklyAllowanceStatus.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        allowanceRow.addView(expenseWeeklyAllowanceStatus,
                new LinearLayout.LayoutParams(0, dp(42), 1f));
        TextView allowanceButton = actionButton("SET WEEK", false);
        allowanceRow.addView(allowanceButton, new LinearLayout.LayoutParams(dp(94), dp(42)));
        allowanceButton.setOnClickListener(v -> {
            tap(v);
            showWeeklyAllowanceEditor();
        });
        toolsCard.addView(allowanceRow);

        LinearLayout alertRow = new LinearLayout(this);
        alertRow.setGravity(Gravity.CENTER_VERTICAL);
        expenseDailyAlertStatus = text("Daily alerts  •  ₹50", 10, TEXT);
        expenseDailyAlertStatus.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        alertRow.addView(expenseDailyAlertStatus, new LinearLayout.LayoutParams(0, dp(42), 1f));
        TextView alertButton = actionButton("ALERT", false);
        alertRow.addView(alertButton, new LinearLayout.LayoutParams(dp(82), dp(42)));
        alertButton.setOnClickListener(v -> {
            tap(v);
            showDailyAlertEditor();
        });
        toolsCard.addView(alertRow, new LinearLayout.LayoutParams(-1, dp(42)));

        LinearLayout recurringRow = new LinearLayout(this);
        recurringRow.setGravity(Gravity.CENTER_VERTICAL);
        expenseRecurringStatus = text("Recurring  •  0 bills", 10, TEXT);
        expenseRecurringStatus.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        recurringRow.addView(expenseRecurringStatus, new LinearLayout.LayoutParams(0, dp(42), 1f));
        TextView recurringButton = actionButton("MANAGE", false);
        recurringRow.addView(recurringButton, new LinearLayout.LayoutParams(dp(90), dp(42)));
        recurringButton.setOnClickListener(v -> {
            tap(v);
            showRecurringManager();
        });
        toolsCard.addView(recurringRow, new LinearLayout.LayoutParams(-1, dp(42)));

        page.addView(toolsCard, sectionParams(0, 14));

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

        expenseDailySafeSpend = text("Suggested max today  •  Set a budget to get an estimate.",
                9, ACCENT_2);
        expenseDailySafeSpend.setPadding(0, dp(8), 0, 0);
        summary.addView(expenseDailySafeSpend);

        page.addView(summary, cardMargin());

        page.addView(sectionLabel("INSIGHTS"), sectionParams(0, 8));

        LinearLayout insightsCard = cardColumn();
        TextView insightsTitle = text("Your spending pulse", 15, TEXT);
        insightsTitle.setTypeface(Typeface.DEFAULT_BOLD);
        insightsCard.addView(insightsTitle);

        TextView insightsHint = text(
                "Quick comparisons for the current week and month.",
                9,
                MUTED
        );
        insightsHint.setPadding(0, dp(4), 0, dp(12));
        insightsCard.addView(insightsHint);

        LinearLayout row1 = new LinearLayout(this);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        expenseWeeklySpend = metricText("THIS WEEK", "₹0", TEXT);
        expenseVsLastMonth = metricText("VS LAST MONTH", "—", ACCENT);
        row1.addView(expenseWeeklySpend, new LinearLayout.LayoutParams(0, dp(58), 1f));
        row1.addView(expenseVsLastMonth, new LinearLayout.LayoutParams(0, dp(58), 1f));
        insightsCard.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        expenseStreak = metricText("STREAK", "—", TEXT);
        expenseMonthlyIncome = metricText("RECEIVED THIS MONTH", "₹0", ACCENT_2);
        row2.addView(expenseStreak, new LinearLayout.LayoutParams(0, dp(58), 1f));
        row2.addView(expenseMonthlyIncome, new LinearLayout.LayoutParams(0, dp(58), 1f));
        insightsCard.addView(row2);

        page.addView(insightsCard, sectionParams(0, 20));

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

        // Generate due recurring expenses once their billing day arrives.
        RecurringExpenseStore.generateDue(
                preferences,
                LocalDate.now(zone)
        );

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

        if (expenseDailySafeSpend != null) {
            LocalDate today = LocalDate.now(zone);
            if (!expenseMonth.equals(today.withDayOfMonth(1))) {
                expenseDailySafeSpend.setText("Suggested max today  •  Available in the current month view.");
            } else {
                long dailyLimit = ExpenseStore.dailyLimitPaise(preferences);
                long suggested = Long.MAX_VALUE;
                boolean hasEstimate = false;
                if (budget > 0L) {
                    long remaining = Math.max(0L, budget - total);
                    int daysLeft = Math.max(1, today.lengthOfMonth() - today.getDayOfMonth() + 1);
                    suggested = remaining / daysLeft;
                    hasEstimate = true;
                }
                if (dailyLimit > 0L) {
                    suggested = hasEstimate ? Math.min(suggested, dailyLimit) : dailyLimit;
                    hasEstimate = true;
                }
                expenseDailySafeSpend.setText(hasEstimate
                        ? "Suggested max today  •  " + formatRupees(suggested)
                                + "  ·  Based on your limits"
                        : "Suggested max today  •  Set a daily or monthly limit to get an estimate.");
            }
        }

        refreshMoneyInsights();

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

    private void refreshMoneyInsights() {
        if (expenseWeeklySpend == null || expenseVsLastMonth == null || expenseStreak == null) {
            return;
        }

        LocalDate today = LocalDate.now(zone);
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        long weekTotal = ExpenseStore.totalBetween(
                preferences,
                weekStart,
                today.plusDays(1)
        );
        long currentMonthTotal = ExpenseStore.totalPaise(
                ExpenseStore.forMonth(preferences, expenseMonth)
        );
        long previousMonthTotal = ExpenseStore.totalPaise(
                ExpenseStore.forMonth(preferences, expenseMonth.minusMonths(1))
        );

        expenseWeeklySpend.setText("THIS WEEK\n" + formatRupees(weekTotal));
        long weeklyAllowance = ExpenseStore.weeklyAllowancePaise(preferences);
        if (expenseWeeklyAllowanceStatus != null) {
            expenseWeeklyAllowanceStatus.setText(weeklyAllowance > 0L
                    ? "Weekly allowance  •  " + formatRupees(Math.max(0L, weeklyAllowance - weekTotal))
                            + " left"
                    : "Weekly allowance  •  Not set");
        }
        if (expenseMonthlyIncome != null) {
            expenseMonthlyIncome.setText("RECEIVED THIS MONTH\n"
                    + formatRupees(ExpenseStore.totalReceivedPaise(
                            ExpenseStore.forMonth(preferences, expenseMonth))));
        }

        if (previousMonthTotal <= 0L) {
            expenseVsLastMonth.setText("VS LAST MONTH\nNew");
        } else {
            long diff = currentMonthTotal - previousMonthTotal;
            long percent = Math.round(
                    (diff * 100.0d) / (double) previousMonthTotal
            );
            String prefix = percent > 0 ? "+" : "";
            expenseVsLastMonth.setText(
                    "VS LAST MONTH\n" + prefix + percent + "%"
            );
            expenseVsLastMonth.setTextColor(percent > 0 ? ACCENT_2 : ACCENT);
        }

        long limit = ExpenseStore.dailyLimitPaise(preferences);
        if (limit > 0L) {
            int streak = ExpenseStore.spendingStreakDays(preferences, today);
            expenseStreak.setText(
                    "STREAK\n" + streak + (streak == 1 ? " day" : " days")
            );
        } else {
            expenseStreak.setText("STREAK\nSet daily limit");
        }

        expenseDailyLimitStatus.setText(
                limit > 0L
                        ? "Today  •  " + formatRupees(ExpenseStore.totalForDate(preferences, today))
                                + " / " + formatRupees(limit)
                        : "Spent today  •  " + formatRupees(
                                ExpenseStore.totalForDate(preferences, today))
        );

        boolean alerts = ExpenseStore.dailyAlertsEnabled(preferences);
        long threshold = ExpenseStore.dailyAlertThresholdPaise(preferences);
        expenseDailyAlertStatus.setText(
                alerts && threshold > 0L
                        ? "Daily alerts  •  " + formatRupees(threshold)
                        : "Daily alerts  •  OFF"
        );

        List<RecurringExpenseStore.Rule> rules = RecurringExpenseStore.load(preferences);
        int activeCount = 0;
        for (RecurringExpenseStore.Rule rule : rules) {
            if (rule.active) activeCount++;
        }
        int dueCount = RecurringExpenseStore.dueCount(preferences, today);
        int detectedCount = RecurringExpenseStore.detectMonthlyCandidates(
                ExpenseStore.load(preferences)).size();
        expenseRecurringStatus.setText(
                "Recurring  •  " + activeCount + " " + (activeCount == 1 ? "bill" : "bills")
                        + (detectedCount > 0 ? "  ·  " + detectedCount + " pattern" : "")
                        + (dueCount > 0 ? "  •  " + dueCount + " due" : "")
        );
    }

    private String categoryTrendLabel(long current, long previous) {
        if (previous <= 0L) return "NEW";
        long diff = current - previous;
        long percent = Math.round((diff * 100.0d) / (double) previous);
        if (percent == 0L) return "FLAT";
        return (percent > 0 ? "+" : "") + percent + "% vs last month";
    }

    private int trendColor(long current, long previous) {
        if (previous <= 0L) return ACCENT_2;
        return current > previous ? ACCENT_2 : ACCENT;
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
            value.setGravity(Gravity.END);
            long previousAmount = ExpenseStore.categoryTotalsForMonth(
                    preferences,
                    expenseMonth.minusMonths(1)
            ).get(category);
            TextView trend = text(categoryTrendLabel(amount, previousAmount), 8, trendColor(amount, previousAmount));
            trend.setGravity(Gravity.END);
            trend.setPadding(0, dp(2), 0, 0);

            LinearLayout valueBox = new LinearLayout(this);
            valueBox.setOrientation(LinearLayout.VERTICAL);
            valueBox.setGravity(Gravity.END);
            valueBox.addView(value);
            valueBox.addView(trend);
            top.addView(valueBox);
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
                expense.category.startsWith("Food") || expense.category.equals("Tea & Coffee")
                        ? ACCENT : ACCENT_2,
                Color.TRANSPARENT,
                999
        ));
        row.addView(categoryMark, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(11), 0, 0, 0);

        String primaryText = expense.merchant.trim().isEmpty()
                ? expense.category
                : expense.merchant.trim();
        TextView category = text(primaryText, 11, TEXT);
        category.setTypeface(Typeface.DEFAULT_BOLD);
        category.setMaxLines(1);
        category.setEllipsize(android.text.TextUtils.TruncateAt.END);
        copy.addView(category);

        String detail = expense.category;
        if (!expense.paymentMode.isEmpty()) detail += "  •  " + expense.paymentMode;
        if ("SMS".equals(expense.source) || "NOTIFICATION".equals(expense.source)) detail += "  •  AUTO";
        else if (!expense.note.trim().isEmpty()) detail += "  •  " + expense.note.trim();
        if (expense.availableBalancePaise >= 0L) {
            detail += "  •  Balance " + formatRupees(expense.availableBalancePaise);
        }

        TextView note = text(detail, 8, MUTED);
        note.setMaxLines(1);
        note.setEllipsize(android.text.TextUtils.TruncateAt.END);
        note.setPadding(0, dp(3), 0, 0);
        copy.addView(note);

        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));

        boolean incoming = SmsTransactionParser.CREDIT.equals(expense.direction);
        TextView amount = text((incoming ? "+" : "") + formatRupees(expense.amountPaise),
                12, incoming ? ACCENT : TEXT);
        amount.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        amount.setGravity(Gravity.END);
        row.addView(amount);

        return row;
    }

    private void showExpenseActions(ExpenseStore.Expense expense) {
        LinearLayout sheet = dialogSheet();

        ImageView icon = dialogIcon(R.drawable.ic_wallet, ACCENT);
        sheet.addView(icon, dialogMarginParams(48, 0, 0, 14));

        boolean incoming = SmsTransactionParser.CREDIT.equals(expense.direction);
        TextView title = text((incoming ? "+" : "") + formatRupees(expense.amountPaise),
                26, incoming ? ACCENT : TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        TextView meta = text(
                expense.category + "  •  " + formatLongDate(expense.date)
                        + (expense.paymentMode.isEmpty() ? "" : "  •  " + expense.paymentMode),
                10,
                MUTED
        );
        meta.setPadding(0, dp(4), 0, 18);
        sheet.addView(meta);
        if (expense.availableBalancePaise >= 0L) {
            TextView balance = text(
                    "Available balance at alert · " + formatRupees(expense.availableBalancePaise),
                    9, MUTED
            );
            balance.setPadding(0, 0, 0, dp(12));
            sheet.addView(balance);
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        TextView edit = dialogAction("EDIT EXPENSE", false);
        TextView delete = dialogAction("DELETE", false);
        actions.addView(edit, new LinearLayout.LayoutParams(0, dp(48), 1f));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        deleteParams.setMargins(dp(9), 0, 0, 0);
        actions.addView(delete, deleteParams);
        sheet.addView(actions);

        TextView cancel = dialogTextAction("CANCEL");
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(-1, dp(42));
        cancelParams.setMargins(0, dp(8), 0, 0);
        sheet.addView(cancel, cancelParams);

        Dialog dialog = showBottomSheet(sheet);
        edit.setOnClickListener(v -> {
            tap(v);
            dialog.dismiss();
            showExpenseEditor(expense);
        });
        delete.setOnClickListener(v -> {
            tap(v);
            dialog.dismiss();
            showDeleteExpenseConfirmation(expense);
        });
        cancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void showDeleteExpenseConfirmation(ExpenseStore.Expense expense) {
        LinearLayout sheet = dialogSheet();

        ImageView icon = dialogIcon(R.drawable.ic_wallet, ACCENT_2);
        sheet.addView(icon, dialogMarginParams(48, 0, 0, 14));

        TextView title = text("Delete expense?", 23, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        TextView message = text(
                (SmsTransactionParser.CREDIT.equals(expense.direction) ? "+" : "")
                        + formatRupees(expense.amountPaise) + "  •  " + expense.category
                        + "\\n" + formatLongDate(expense.date),
                10,
                MUTED
        );
        message.setPadding(0, dp(6), 0, dp(19));
        sheet.addView(message);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        TextView cancel = dialogAction("KEEP IT", false);
        TextView confirm = dialogAction("DELETE", true);
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(48), 1f));
        LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        confirmParams.setMargins(dp(9), 0, 0, 0);
        actions.addView(confirm, confirmParams);
        sheet.addView(actions);

        Dialog dialog = showBottomSheet(sheet);
        cancel.setOnClickListener(v -> dialog.dismiss());
        confirm.setOnClickListener(v -> {
            tap(v);
            ExpenseStore.delete(preferences, expense.id);
            dialog.dismiss();
            refreshExpensesScreen();
        });
    }

    private void showExpenseEditor(ExpenseStore.Expense existing) {
        final String[] selectedCategory = {
                existing == null ? "Food & Drinks" : existing.category
        };
        final String[] selectedPayment = {
                existing == null ? "UPI" : existing.paymentMode
        };
        final LocalDate[] selectedDate = {
                existing == null ? LocalDate.now(zone) : existing.date
        };

        LinearLayout sheet = dialogSheet();

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = dialogIcon(R.drawable.ic_wallet, ACCENT);
        heading.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout headingCopy = new LinearLayout(this);
        headingCopy.setOrientation(LinearLayout.VERTICAL);
        headingCopy.setPadding(dp(11), 0, 0, 0);

        TextView title = text(
                existing == null ? "Add expense" : "Edit expense",
                23,
                TEXT
        );
        title.setTypeface(Typeface.DEFAULT_BOLD);
        headingCopy.addView(title);

        TextView subtitle = text(
                "Log it in seconds. Everything stays on this phone.",
                9,
                MUTED
        );
        subtitle.setPadding(0, dp(3), 0, 0);
        headingCopy.addView(subtitle);

        heading.addView(headingCopy, new LinearLayout.LayoutParams(0, -2, 1f));
        sheet.addView(heading);

        EditText amount = field("0.00", true);
        amount.setInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        amount.setTextSize(30);
        amount.setGravity(Gravity.CENTER);
        amount.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        amount.setHintTextColor(blend(MUTED, BG, 0.08f));
        amount.setPadding(dp(15), 0, dp(15), 0);
        amount.setBackground(gradientRound(
                SURFACE_2,
                blend(SURFACE_2, ACCENT_BG, 0.35f),
                Color.TRANSPARENT,
                SMALL_RADIUS,
                themeGradientOrientation()
        ));
        if (existing != null) amount.setText(amountRupeesInput(existing.amountPaise));
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(-1, dp(72));
        amountParams.setMargins(0, dp(16), 0, dp(15));
        sheet.addView(amount, amountParams);

        final String[] splitSummary = {""};
        TextView splitStatus = dialogAction("SPLIT EXPENSE", false);
        LinearLayout.LayoutParams splitStatusParams = new LinearLayout.LayoutParams(-1, dp(42));
        splitStatusParams.setMargins(0, 0, 0, dp(14));
        if (existing == null || !SmsTransactionParser.CREDIT.equals(existing.direction)) {
            sheet.addView(splitStatus, splitStatusParams);
        }
        splitStatus.setOnClickListener(v -> {
            tap(v);
            showSplitExpenseDialog(amount, splitStatus, splitSummary);
        });

        TextView catLabel = dialogSectionLabel("CATEGORY");
        sheet.addView(catLabel);

        HorizontalScrollView categoryScroll = new HorizontalScrollView(this);
        categoryScroll.setHorizontalScrollBarEnabled(false);
        categoryScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout catGrid = new LinearLayout(this);
        catGrid.setOrientation(LinearLayout.HORIZONTAL);
        List<TextView> categoryChips = new java.util.ArrayList<>();

        for (int j = 0; j < ExpenseStore.CATEGORIES.length; j++) {
            int index = j;
            String label = ExpenseStore.CATEGORIES[j];
            TextView chip = dialogChoiceChip(label, label.equals(selectedCategory[0]));
            categoryChips.add(chip);
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(dp(142), dp(40));
            if (j > 0) chipParams.setMargins(dp(7), 0, 0, 0);
            catGrid.addView(chip, chipParams);
            chip.setOnClickListener(v -> {
                selectedCategory[0] = ExpenseStore.CATEGORIES[index];
                for (TextView cchip : categoryChips) {
                    String value = cchip.getText().toString();
                    cchip.setBackground(choiceBackground(value.equals(selectedCategory[0])));
                    cchip.setTextColor(value.equals(selectedCategory[0]) ? ACCENT : TEXT);
                }
                tap(v);
            });
        }
        categoryScroll.addView(catGrid);
        sheet.addView(categoryScroll, new LinearLayout.LayoutParams(-1, dp(40)));

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView date = dialogAction(
                "DATE  •  " + (selectedDate[0].equals(LocalDate.now(zone))
                        ? "TODAY" : formatLongDate(selectedDate[0])),
                false
        );
        dateRow.addView(date, new LinearLayout.LayoutParams(0, dp(44), 1f));

        TextView dateChange = dialogTextAction("CHANGE");
        LinearLayout.LayoutParams dateChangeParams = new LinearLayout.LayoutParams(dp(74), dp(44));
        dateChangeParams.setMargins(dp(8), 0, 0, 0);
        dateRow.addView(dateChange, dateChangeParams);
        LinearLayout.LayoutParams dateRowParams = new LinearLayout.LayoutParams(-1, dp(44));
        dateRowParams.setMargins(0, dp(15), 0, 0);
        sheet.addView(dateRow, dateRowParams);

        date.setOnClickListener(v -> showExpenseDatePicker(selectedDate, date));
        dateChange.setOnClickListener(v -> showExpenseDatePicker(selectedDate, date));

        TextView paymentLabel = dialogSectionLabel("PAID WITH");
        LinearLayout.LayoutParams paymentLabelParams = new LinearLayout.LayoutParams(-1, -2);
        paymentLabelParams.setMargins(0, dp(15), 0, dp(7));
        sheet.addView(paymentLabel, paymentLabelParams);

        HorizontalScrollView paymentScroll = new HorizontalScrollView(this);
        paymentScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout paymentRow = new LinearLayout(this);
        paymentScroll.addView(paymentRow);

        List<TextView> paymentChips = new java.util.ArrayList<>();
        for (int i = 0; i < ExpenseStore.PAYMENT_MODES.length; i++) {
            int index = i;
            String mode = ExpenseStore.PAYMENT_MODES[i];
            TextView chip = dialogChoiceChip(mode, mode.equals(selectedPayment[0]));
            paymentChips.add(chip);
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(dp(92), dp(38));
            if (i > 0) chipParams.setMargins(dp(7), 0, 0, 0);
            paymentRow.addView(chip, chipParams);
            chip.setOnClickListener(v -> {
                selectedPayment[0] = ExpenseStore.PAYMENT_MODES[index];
                for (TextView pchip : paymentChips) {
                    boolean active = pchip.getText().toString().equals(selectedPayment[0]);
                    pchip.setBackground(choiceBackground(active));
                    pchip.setTextColor(active ? ACCENT : TEXT);
                }
                tap(v);
            });
        }
        LinearLayout.LayoutParams paymentScrollParams = new LinearLayout.LayoutParams(-1, dp(38));
        sheet.addView(paymentScroll, paymentScrollParams);

        EditText note = field("Optional note — e.g. lunch at college", false);
        note.setSingleLine(true);
        if (existing != null) note.setText(existing.note);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, dp(48));
        noteParams.setMargins(0, dp(14), 0, 0);
        sheet.addView(note, noteParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        TextView cancel = dialogTextAction("CANCEL");
        TextView save = dialogAction(
                existing == null ? "SAVE EXPENSE" : "SAVE CHANGES",
                true
        );
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(48), 1f));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, dp(48), 1.45f);
        saveParams.setMargins(dp(8), 0, 0, 0);
        actions.addView(save, saveParams);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, dp(48));
        actionsParams.setMargins(0, dp(17), 0, 0);
        sheet.addView(actions, actionsParams);

        Dialog dialog = showBottomSheet(sheet);
        amount.requestFocus();
        amount.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(amount, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 220);

        cancel.setOnClickListener(v -> {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(amount.getWindowToken(), 0);
            dialog.dismiss();
        });

        save.setOnClickListener(v -> {
            try {
                long paise = ExpenseStore.parseAmountToPaise(
                        amount.getText().toString()
                );
                if (paise <= 0) {
                    amount.setError("Enter an amount greater than ₹0");
                    shake(v);
                    return;
                }

                long id = existing == null ? System.currentTimeMillis() : existing.id;
                String merchant = existing == null ? "" : existing.merchant;
                String source = existing == null ? "MANUAL" : existing.source;
                float confidence = existing == null ? 1.0f : existing.confidence;
                String fingerprint = existing == null ? "" : existing.fingerprint;

                if (!SmsTransactionParser.CREDIT.equals(existing == null
                        ? SmsTransactionParser.DEBIT : existing.direction)) {
                    ExpenseStore.learnMerchantCategory(preferences, merchant, selectedCategory[0]);
                }

                ExpenseStore.upsert(preferences, new ExpenseStore.Expense(
                        id,
                        paise,
                        selectedDate[0],
                        selectedCategory[0],
                        buildExpenseNote(note.getText().toString().trim(), splitSummary[0]),
                        selectedPayment[0],
                        merchant,
                        source,
                        confidence,
                        fingerprint,
                        existing == null ? SmsTransactionParser.DEBIT : existing.direction,
                        existing == null ? -1L : existing.availableBalancePaise
                ));

                ExpenseDailyAlertReceiver.maybeNotifyNow(this);

                expenseMonth = selectedDate[0].withDayOfMonth(1);
                hideKeyboard();
                dialog.dismiss();
                refreshExpensesScreen();
            } catch (Exception ignored) {
                amount.setError("Enter a valid amount, e.g. 120.50");
                shake(v);
            }
        });
    }

    private void showDailyLimitEditor() {
        LinearLayout sheet = dialogSheet();

        ImageView icon = dialogIcon(R.drawable.ic_wallet, ACCENT);
        sheet.addView(icon, dialogMarginParams(48, 0, 0, 14));

        TextView title = text("Daily spending limit", 23, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        TextView subtitle = text(
                "Your streak counts days that stay at or below this limit.",
                10,
                MUTED
        );
        subtitle.setPadding(0, dp(5), 0, dp(15));
        sheet.addView(subtitle);

        EditText input = field("e.g. 350", true);
        input.setInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        input.setTextSize(28);
        input.setGravity(Gravity.CENTER);
        long current = ExpenseStore.dailyLimitPaise(preferences);
        if (current > 0L) input.setText(amountRupeesInput(current));
        sheet.addView(input, new LinearLayout.LayoutParams(-1, dp(68)));

        TextView hint = text(
                "Examples: ₹250 for a strict day • ₹350 for a flexible student budget",
                9,
                SUBTLE
        );
        hint.setPadding(0, dp(8), 0, 0);
        sheet.addView(hint);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        TextView clear = dialogTextAction("CLEAR");
        TextView cancel = dialogTextAction("CANCEL");
        TextView save = dialogAction("SAVE LIMIT", true);
        actions.addView(clear, new LinearLayout.LayoutParams(0, dp(48), 0.82f));
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(48), 0.82f));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, dp(48), 1.12f);
        saveParams.setMargins(dp(7), 0, 0, 0);
        actions.addView(save, saveParams);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, dp(48));
        actionsParams.setMargins(0, dp(18), 0, 0);
        sheet.addView(actions, actionsParams);

        Dialog dialog = showBottomSheet(sheet);
        input.requestFocus();

        clear.setOnClickListener(v -> {
            ExpenseStore.setDailyLimitPaise(preferences, 0L);
            hideKeyboard();
            dialog.dismiss();
            refreshExpensesScreen();
        });
        cancel.setOnClickListener(v -> {
            hideKeyboard();
            dialog.dismiss();
        });
        save.setOnClickListener(v -> {
            try {
                long paise = ExpenseStore.parseAmountToPaise(input.getText().toString());
                ExpenseStore.setDailyLimitPaise(preferences, paise);
                hideKeyboard();
                dialog.dismiss();
                refreshExpensesScreen();
            } catch (Exception ignored) {
                input.setError("Enter a valid daily limit.");
                shake(v);
            }
        });
    }

    private void showDailyAlertEditor() {
        LinearLayout sheet = dialogSheet();

        ImageView icon = dialogIcon(R.drawable.ic_calendar, ACCENT);
        sheet.addView(icon, dialogMarginParams(48, 0, 0, 14));

        TextView title = text("Daily spending alerts", 23, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        TextView subtitle = text(
                "At 8 PM, NextBell can tell you when today's spending reaches the alert threshold or exceeds your daily limit.",
                10,
                MUTED
        );
        subtitle.setPadding(0, dp(5), 0, dp(15));
        sheet.addView(subtitle);

        long current = ExpenseStore.dailyAlertThresholdPaise(preferences);
        boolean enabled = ExpenseStore.dailyAlertsEnabled(preferences);
        final long[] selected = { enabled ? current : 0L };

        TextView state = text(
                selected[0] > 0L ? "ALERT AT  " + formatRupees(selected[0]) : "ALERTS OFF",
                16,
                TEXT
        );
        state.setTypeface(Typeface.DEFAULT_BOLD);
        state.setGravity(Gravity.CENTER);
        state.setBackground(round(SURFACE_2, Color.TRANSPARENT, SMALL_RADIUS));
        sheet.addView(state, new LinearLayout.LayoutParams(-1, dp(64)));

        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = {"OFF", "₹50", "₹100"};
        long[] values = {0L, 5000L, 10000L};
        for (int i = 0; i < labels.length; i++) {
            int index = i;
            TextView chip = dialogChoiceChip(labels[i], values[i] == selected[0]);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            if (i > 0) cp.setMargins(dp(7), 0, 0, 0);
            choices.addView(chip, cp);
            chip.setOnClickListener(v -> {
                selected[0] = values[index];
                state.setText(selected[0] > 0L
                        ? "ALERT AT  " + formatRupees(selected[0])
                        : "ALERTS OFF");
                for (int j = 0; j < choices.getChildCount(); j++) {
                    TextView other = (TextView) choices.getChildAt(j);
                    boolean activeChoice = j == index;
                    other.setBackground(choiceBackground(activeChoice));
                    other.setTextColor(activeChoice ? ACCENT : TEXT);
                }
                tap(v);
            });
        }
        LinearLayout.LayoutParams choicesParams = new LinearLayout.LayoutParams(-1, dp(44));
        choicesParams.setMargins(0, dp(14), 0, 0);
        sheet.addView(choices, choicesParams);

        TextView note = text(
                "The alert is local-only and uses your normal NextBell notification permission.",
                8,
                SUBTLE
        );
        note.setPadding(0, dp(9), 0, 0);
        sheet.addView(note);

        TextView save = dialogAction("SAVE ALERT SETTINGS", true);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(-1, dp(48));
        saveParams.setMargins(0, dp(18), 0, 0);
        sheet.addView(save, saveParams);

        Dialog dialog = showBottomSheet(sheet);
        save.setOnClickListener(v -> {
            tap(v);
            ExpenseStore.setDailyAlertsEnabled(preferences, selected[0] > 0L);
            ExpenseStore.setDailyAlertThresholdPaise(preferences, selected[0]);
            ExpenseStore.setLastAlertNotifiedDate(
                    preferences,
                    selected[0] > 0L ? "" : ExpenseStore.lastAlertNotifiedDate(preferences)
            );
            NotificationScheduler.scheduleUpcoming(this);
            dialog.dismiss();
            refreshExpensesScreen();
        });
    }

    private void showWeeklyAllowanceEditor() {
        LinearLayout sheet = dialogSheet();
        ImageView icon = dialogIcon(R.drawable.ic_wallet, ACCENT_2);
        sheet.addView(icon, dialogMarginParams(48, 0, 0, 14));
        TextView title = text("Weekly allowance", 22, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);
        TextView subtitle = text(
                "Track a pocket-money target from Monday through Sunday. This is a guide, not an account balance.",
                9, MUTED
        );
        subtitle.setPadding(0, dp(5), 0, dp(14));
        sheet.addView(subtitle);

        EditText input = field("e.g. 2,500", true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setTextSize(27);
        input.setGravity(Gravity.CENTER);
        long current = ExpenseStore.weeklyAllowancePaise(preferences);
        if (current > 0L) input.setText(amountRupeesInput(current));
        sheet.addView(input, new LinearLayout.LayoutParams(-1, dp(64)));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        TextView clear = dialogTextAction("CLEAR");
        TextView cancel = dialogTextAction("CANCEL");
        TextView save = dialogAction("SAVE ALLOWANCE", true);
        actions.addView(clear, new LinearLayout.LayoutParams(0, dp(46), 0.85f));
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(46), 0.85f));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, dp(46), 1.3f);
        saveParams.setMargins(dp(7), 0, 0, 0);
        actions.addView(save, saveParams);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, dp(46));
        actionsParams.setMargins(0, dp(14), 0, 0);
        sheet.addView(actions, actionsParams);

        Dialog dialog = showBottomSheet(sheet);
        clear.setOnClickListener(v -> {
            ExpenseStore.setWeeklyAllowancePaise(preferences, 0L);
            dialog.dismiss();
            refreshExpensesScreen();
        });
        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            try {
                ExpenseStore.setWeeklyAllowancePaise(preferences,
                        ExpenseStore.parseAmountToPaise(input.getText().toString()));
                hideKeyboard();
                dialog.dismiss();
                refreshExpensesScreen();
            } catch (Exception ignored) {
                input.setError("Enter a valid weekly amount.");
                shake(v);
            }
        });
    }

    private void showRecurringManager() {
        LinearLayout sheet = dialogSheet();

        ImageView icon = dialogIcon(R.drawable.ic_wallet, ACCENT_2);
        sheet.addView(icon, dialogMarginParams(48, 0, 0, 14));

        TextView title = text("Recurring expenses", 23, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        TextView subtitle = text(
                "Rent, subscriptions and bills are generated once per month on their due day.",
                10,
                MUTED
        );
        subtitle.setPadding(0, dp(5), 0, dp(12));
        sheet.addView(subtitle);

        final Dialog[] managerDialog = {null};

        LinearLayout quick = new LinearLayout(this);
        quick.setGravity(Gravity.CENTER_VERTICAL);

        TextView rent = dialogAction("+ RENT", false);
        TextView bill = dialogAction("+ BILL", false);
        quick.addView(rent, new LinearLayout.LayoutParams(0, dp(42), 1f));
        LinearLayout.LayoutParams billParams = new LinearLayout.LayoutParams(0, dp(42), 1f);
        billParams.setMargins(dp(7), 0, 0, 0);
        quick.addView(bill, billParams);
        sheet.addView(quick, new LinearLayout.LayoutParams(-1, dp(42)));

        rent.setOnClickListener(v -> {
            tap(v);
            if (managerDialog[0] != null) managerDialog[0].dismiss();
            showRecurringEditor(null, "Rent", "Home & Bills", 5);
        });
        bill.setOnClickListener(v -> {
            tap(v);
            if (managerDialog[0] != null) managerDialog[0].dismiss();
            showRecurringEditor(null, "Internet / Wi-Fi", "Home & Bills", 10);
        });

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        List<RecurringExpenseStore.Rule> rules = RecurringExpenseStore.load(preferences);
        List<RecurringExpenseStore.Candidate> candidates =
                RecurringExpenseStore.detectMonthlyCandidates(ExpenseStore.load(preferences));

        if (rules.isEmpty() && candidates.isEmpty()) {
            TextView empty = text(
                    "No recurring expenses yet. Start with rent or a regular bill above.",
                    9,
                    MUTED
            );
            empty.setPadding(0, dp(15), 0, dp(15));
            list.addView(empty);
        } else {
            for (RecurringExpenseStore.Rule rule : rules) {
                LinearLayout row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(9), 0, dp(9));

                LinearLayout copy = new LinearLayout(this);
                copy.setOrientation(LinearLayout.VERTICAL);

                TextView rowTitle = text(rule.title, 11, TEXT);
                rowTitle.setTypeface(Typeface.DEFAULT_BOLD);
                copy.addView(rowTitle);

                TextView rowMeta = text(
                        formatRupees(rule.amountPaise) + "  •  Day " + rule.dayOfMonth
                                + "  •  " + rule.category
                                + (rule.active ? "" : "  •  PAUSED"),
                        8,
                        rule.active ? MUTED : SUBTLE
                );
                rowMeta.setPadding(0, dp(3), 0, 0);
                copy.addView(rowMeta);

                row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));

                TextView edit = dialogTextAction("EDIT");
                row.addView(edit, new LinearLayout.LayoutParams(dp(62), dp(42)));
                edit.setOnClickListener(v -> {
                    tap(v);
                    if (managerDialog[0] != null) managerDialog[0].dismiss();
                    showRecurringEditor(rule, null, null, rule.dayOfMonth);
                });

                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
                rp.setMargins(0, dp(2), 0, 0);
                list.addView(row, rp);
            }
        }

        if (!candidates.isEmpty()) {
            TextView detectedTitle = text("Possible monthly repeats", 12, TEXT);
            detectedTitle.setTypeface(Typeface.DEFAULT_BOLD);
            detectedTitle.setPadding(0, dp(13), 0, dp(2));
            list.addView(detectedTitle);
            TextView detectedHint = text(
                    "Patterns are suggestions. Review an item before turning it into a recurring bill.",
                    8, MUTED
            );
            detectedHint.setPadding(0, 0, 0, dp(6));
            list.addView(detectedHint);
            for (RecurringExpenseStore.Candidate candidate : candidates) {
                LinearLayout row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(7), 0, dp(7));
                LinearLayout copy = new LinearLayout(this);
                copy.setOrientation(LinearLayout.VERTICAL);
                TextView name = text(candidate.merchant, 10, TEXT);
                name.setTypeface(Typeface.DEFAULT_BOLD);
                copy.addView(name);
                TextView meta = text(
                        formatRupees(candidate.suggestedAmountPaise) + "  ·  "
                                + candidate.occurrences + " monthly payments",
                        8, MUTED
                );
                copy.addView(meta);
                row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
                TextView addCandidate = dialogTextAction("REVIEW");
                row.addView(addCandidate, new LinearLayout.LayoutParams(dp(70), dp(38)));
                addCandidate.setOnClickListener(v -> {
                    tap(v);
                    if (managerDialog[0] != null) managerDialog[0].dismiss();
                    showRecurringEditor(null, candidate.merchant, candidate.category,
                            Math.min(28, candidate.latestDate.getDayOfMonth()),
                            candidate.suggestedAmountPaise);
                });
                list.addView(row);
            }
        }

        sheet.addView(list);

        TextView add = dialogAction("+ ADD RECURRING", true);
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(-1, dp(48));
        addParams.setMargins(0, dp(12), 0, 0);
        sheet.addView(add, addParams);

        TextView done = dialogTextAction("DONE");
        LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(-1, dp(42));
        doneParams.setMargins(0, dp(6), 0, 0);
        sheet.addView(done, doneParams);

        Dialog dialog = showBottomSheet(sheet);
        managerDialog[0] = dialog;
        add.setOnClickListener(v -> {
            tap(v);
            dialog.dismiss();
            showRecurringEditor(null, null, null, 1);
        });
        done.setOnClickListener(v -> dialog.dismiss());
    }

    private void showRecurringEditor(
            RecurringExpenseStore.Rule existing,
            String presetTitle,
            String presetCategory,
            int presetDay
    ) {
        showRecurringEditor(existing, presetTitle, presetCategory, presetDay, 0L);
    }

    private void showRecurringEditor(
            RecurringExpenseStore.Rule existing,
            String presetTitle,
            String presetCategory,
            int presetDay,
            long presetAmountPaise
    ) {
        LinearLayout sheet = dialogSheet();

        ImageView icon = dialogIcon(R.drawable.ic_wallet, ACCENT);
        sheet.addView(icon, dialogMarginParams(48, 0, 0, 14));

        TextView title = text(
                existing == null ? "Add recurring expense" : "Edit recurring expense",
                23,
                TEXT
        );
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        TextView subtitle = text(
                "Generated once per month on the selected day. Day 28 is the latest supported due date.",
                9,
                MUTED
        );
        subtitle.setPadding(0, dp(5), 0, dp(15));
        sheet.addView(subtitle);

        EditText name = field("Name — e.g. Rent", false);
        if (existing != null) name.setText(existing.title);
        else if (presetTitle != null) name.setText(presetTitle);
        sheet.addView(name, new LinearLayout.LayoutParams(-1, dp(50)));

        EditText amount = field("Amount", true);
        amount.setInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        if (existing != null) amount.setText(amountRupeesInput(existing.amountPaise));
        else if (presetAmountPaise > 0L) amount.setText(amountRupeesInput(presetAmountPaise));
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(-1, dp(50));
        amountParams.setMargins(0, dp(9), 0, 0);
        sheet.addView(amount, amountParams);

        EditText day = field("Day of month (1–28)", true);
        day.setText(String.valueOf(existing == null ? presetDay : existing.dayOfMonth));
        LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(-1, dp(50));
        dayParams.setMargins(0, dp(9), 0, 0);
        sheet.addView(day, dayParams);

        TextView category = choiceField(
                "Category",
                existing != null ? existing.category
                        : (presetCategory == null ? "Home & Bills" : presetCategory)
        );
        LinearLayout.LayoutParams categoryParams = fieldLikeParams();
        categoryParams.setMargins(0, dp(9), 0, 0);
        sheet.addView(category, categoryParams);

        TextView payment = choiceField(
                "Payment method",
                existing == null ? "UPI" : existing.paymentMode
        );
        LinearLayout.LayoutParams paymentParams = fieldLikeParams();
        paymentParams.setMargins(0, dp(9), 0, 0);
        sheet.addView(payment, paymentParams);

        EditText note = field("Optional note", false);
        if (existing != null) note.setText(existing.note);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, dp(48));
        noteParams.setMargins(0, dp(9), 0, 0);
        sheet.addView(note, noteParams);

        final boolean[] active = { existing == null || existing.active };
        TextView state = dialogAction(active[0] ? "ACTIVE" : "PAUSED", false);
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(-1, dp(42));
        stateParams.setMargins(0, dp(10), 0, 0);
        sheet.addView(state, stateParams);
        state.setOnClickListener(v -> {
            active[0] = !active[0];
            state.setText(active[0] ? "ACTIVE" : "PAUSED");
            tap(v);
        });

        category.setOnClickListener(v ->
                showExpenseChoice(
                        "Choose category",
                        ExpenseStore.CATEGORIES,
                        category
                )
        );
        payment.setOnClickListener(v ->
                showExpenseChoice(
                        "Paid with",
                        ExpenseStore.PAYMENT_MODES,
                        payment
                )
        );

        final Dialog[] editorDialog = {null};

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        TextView cancel = dialogTextAction("CANCEL");
        TextView save = dialogAction("SAVE RECURRING", true);
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(48), 0.9f));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, dp(48), 1.25f);
        saveParams.setMargins(dp(8), 0, 0, 0);
        actions.addView(save, saveParams);
        if (existing != null) {
            TextView remove = dialogTextAction("DELETE");
            LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(0, dp(48), 0.72f);
            removeParams.setMargins(dp(8), 0, 0, 0);
            actions.addView(remove, removeParams);
            remove.setOnClickListener(v -> {
                tap(v);
                RecurringExpenseStore.delete(preferences, existing.id);
                if (editorDialog[0] != null) editorDialog[0].dismiss();
                showRecurringManager();
            });
        }
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, dp(48));
        actionsParams.setMargins(0, dp(16), 0, 0);
        sheet.addView(actions, actionsParams);

        Dialog dialog = showBottomSheet(sheet);
        editorDialog[0] = dialog;
        cancel.setOnClickListener(v -> dialog.dismiss());

        save.setOnClickListener(v -> {
            try {
                String titleValue = name.getText().toString().trim();
                if (titleValue.isEmpty()) throw new IllegalArgumentException("name");

                long paise = ExpenseStore.parseAmountToPaise(amount.getText().toString());
                int dayValue = Integer.parseInt(day.getText().toString().trim());
                if (paise <= 0L || dayValue < 1 || dayValue > 28) {
                    throw new IllegalArgumentException("value");
                }

                long id = existing == null
                        ? System.currentTimeMillis()
                        : existing.id;

                RecurringExpenseStore.upsert(
                        preferences,
                        new RecurringExpenseStore.Rule(
                                id,
                                titleValue,
                                paise,
                                dayValue,
                                category.getText().toString(),
                                payment.getText().toString(),
                                note.getText().toString().trim(),
                                active[0],
                                existing == null ? "" : existing.lastGeneratedMonth
                        )
                );

                hideKeyboard();
                dialog.dismiss();
                refreshExpensesScreen();
            } catch (Exception ignored) {
                name.setError("Check the recurring expense details.");
                shake(v);
            }
        });
    }

    private void showSplitExpenseDialog(
            EditText amountTarget,
            TextView statusTarget,
            String[] splitSummary
    ) {
        LinearLayout sheet = dialogSheet();

        ImageView icon = dialogIcon(R.drawable.ic_wallet, ACCENT_2);
        sheet.addView(icon, dialogMarginParams(48, 0, 0, 14));

        TextView title = text("Split expense", 23, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        TextView subtitle = text(
                "Record only your share while keeping the original total in the note.",
                10,
                MUTED
        );
        subtitle.setPadding(0, dp(5), 0, dp(14));
        sheet.addView(subtitle);

        EditText total = field("Total amount", true);
        total.setInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        total.setText(amountTarget.getText().toString());
        total.setTextSize(22);
        total.setGravity(Gravity.CENTER);
        sheet.addView(total, new LinearLayout.LayoutParams(-1, dp(58)));

        EditText people = field("Number of people", true);
        people.setText("2");
        LinearLayout.LayoutParams peopleParams = new LinearLayout.LayoutParams(-1, dp(50));
        peopleParams.setMargins(0, dp(9), 0, 0);
        sheet.addView(people, peopleParams);

        TextView result = text("YOUR SHARE  •  —", 13, ACCENT);
        result.setTypeface(Typeface.DEFAULT_BOLD);
        result.setGravity(Gravity.CENTER);
        result.setPadding(0, dp(15), 0, dp(12));
        sheet.addView(result);

        TextView apply = dialogAction("USE MY SHARE", true);
        sheet.addView(apply, new LinearLayout.LayoutParams(-1, dp(48)));

        Dialog dialog = showBottomSheet(sheet);
        apply.setOnClickListener(v -> {
            try {
                long totalPaise = ExpenseStore.parseAmountToPaise(total.getText().toString());
                int count = Integer.parseInt(people.getText().toString().trim());
                if (totalPaise <= 0L || count < 2 || count > 100) {
                    throw new IllegalArgumentException("split");
                }

                long sharePaise = Math.round(totalPaise / (double) count);
                result.setText("YOUR SHARE  •  " + formatRupees(sharePaise));
                amountTarget.setText(amountRupeesInput(sharePaise));
                splitSummary[0] = "Split " + count + " ways • total " + formatRupees(totalPaise);
                statusTarget.setText("SPLIT  •  " + count + " ways");
                tap(v);
                dialog.dismiss();
            } catch (Exception ignored) {
                total.setError("Enter a valid total.");
                shake(v);
            }
        });
    }

    private String buildExpenseNote(String note, String splitSummary) {
        String base = note == null ? "" : note.trim();
        if (splitSummary == null || splitSummary.trim().isEmpty()) return base;
        if (base.isEmpty()) return splitSummary.trim();
        return base + "  •  " + splitSummary.trim();
    }

    private void showExpenseChoice(
            String title,
            String[] options,
            TextView target
    ) {
        final String[] selected = { target.getText().toString() };
        LinearLayout sheet = dialogSheet();

        TextView heading = text(title, 23, TEXT);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(heading);

        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.VERTICAL);
        for (String option : options) {
            TextView row = dialogChoiceChip(option, option.equals(selected[0]));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(46));
            rowParams.setMargins(0, dp(7), 0, 0);
            choices.addView(row, rowParams);
            row.setOnClickListener(v -> {
                target.setText(option);
                tap(v);
                ((Dialog) v.getTag()).dismiss();
            });
            row.setTag(null);
        }
        sheet.addView(choices);

        TextView cancel = dialogTextAction("CANCEL");
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(-1, dp(42));
        cancelParams.setMargins(0, dp(9), 0, 0);
        sheet.addView(cancel, cancelParams);

        Dialog dialog = showBottomSheet(sheet);
        for (int i = 0; i < choices.getChildCount(); i++) {
            choices.getChildAt(i).setTag(dialog);
        }
        cancel.setOnClickListener(v -> dialog.dismiss());
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
        LinearLayout sheet = dialogSheet();

        ImageView icon = dialogIcon(R.drawable.ic_wallet, ACCENT);
        sheet.addView(icon, dialogMarginParams(48, 0, 0, 14));

        TextView title = text("Monthly budget", 23, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        TextView subtitle = text(
                "How much do you want to allow yourself for " +
                        expenseMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)) + "?",
                10,
                MUTED
        );
        subtitle.setPadding(0, dp(5), 0, dp(15));
        sheet.addView(subtitle);

        EditText input = field("0", true);
        input.setInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        input.setTextSize(29);
        input.setGravity(Gravity.CENTER);
        input.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        long current = ExpenseStore.budgetPaise(preferences, expenseMonth);
        if (current > 0) input.setText(amountRupeesInput(current));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(68));
        sheet.addView(input, inputParams);

        TextView presetLabel = dialogSectionLabel("QUICK LIMIT");
        LinearLayout.LayoutParams presetLabelParams = new LinearLayout.LayoutParams(-1, -2);
        presetLabelParams.setMargins(0, dp(16), 0, dp(7));
        sheet.addView(presetLabel, presetLabelParams);

        HorizontalScrollView presetsScroll = new HorizontalScrollView(this);
        presetsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout presets = new LinearLayout(this);
        presetsScroll.addView(presets);
        int[] values = {3000, 5000, 8000, 10000, 15000, 20000};
        for (int value : values) {
            TextView chip = dialogChoiceChip("₹" + value, false);
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(dp(82), dp(38));
            if (presets.getChildCount() > 0) chipParams.setMargins(dp(7), 0, 0, 0);
            presets.addView(chip, chipParams);
            chip.setOnClickListener(v -> {
                input.setText(String.valueOf(value));
                input.setSelection(input.length());
                tap(v);
            });
        }
        sheet.addView(presetsScroll, new LinearLayout.LayoutParams(-1, dp(38)));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        TextView clear = dialogTextAction("CLEAR");
        TextView cancel = dialogTextAction("CANCEL");
        TextView save = dialogAction("SAVE BUDGET", true);
        actions.addView(clear, new LinearLayout.LayoutParams(0, dp(48), 0.85f));
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(48), 0.85f));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, dp(48), 1.25f);
        saveParams.setMargins(dp(7), 0, 0, 0);
        actions.addView(save, saveParams);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, dp(48));
        actionsParams.setMargins(0, dp(18), 0, 0);
        sheet.addView(actions, actionsParams);

        Dialog dialog = showBottomSheet(sheet);
        input.requestFocus();

        clear.setOnClickListener(v -> {
            ExpenseStore.setBudgetPaise(preferences, expenseMonth, 0L);
            hideKeyboard();
            dialog.dismiss();
            refreshExpensesScreen();
        });
        cancel.setOnClickListener(v -> {
            hideKeyboard();
            dialog.dismiss();
        });
        save.setOnClickListener(v -> {
            try {
                long paise = ExpenseStore.parseAmountToPaise(input.getText().toString());
                ExpenseStore.setBudgetPaise(preferences, expenseMonth, paise);
                hideKeyboard();
                dialog.dismiss();
                refreshExpensesScreen();
            } catch (Exception ignored) {
                input.setError("Enter a valid budget.");
                shake(v);
            }
        });
    }

    private LinearLayout dialogSheet() {
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(20), dp(18), dp(20), dp(16));
        sheet.setBackground(gradientRound(
                SURFACE,
                SURFACE_2,
                BORDER,
                CARD_RADIUS,
                GradientDrawable.Orientation.TL_BR
        ));
        sheet.setElevation(dp(CARD_ELEVATION + 8));
        return sheet;
    }

    private Dialog showBottomSheet(View sheet) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(sheet);
        dialog.setCanceledOnTouchOutside(true);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.width = WindowManager.LayoutParams.MATCH_PARENT;
            attrs.height = WindowManager.LayoutParams.WRAP_CONTENT;
            attrs.gravity = Gravity.BOTTOM;
            attrs.dimAmount = 0.52f;
            window.setAttributes(attrs);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            );
        }

        if (reducedMotion) {
            sheet.setAlpha(1f);
            sheet.setTranslationY(0f);
            sheet.setScaleX(1f);
            sheet.setScaleY(1f);
        } else {
            sheet.setAlpha(0f);
            sheet.setTranslationY(dp(90));
            sheet.setScaleX(0.985f);
            sheet.setScaleY(0.985f);
            dialog.setOnDismissListener(d -> sheet.animate()
                    .alpha(0f)
                    .translationY(dp(55))
                    .setDuration(170)
                    .start());
        }

        dialog.show();
        if (!reducedMotion) {
            sheet.animate()
                    .alpha(1f)
                    .translationY(0)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(340)
                    .setInterpolator(new OvershootInterpolator(0.75f))
                    .start();
        }
        return dialog;
    }

    private ImageView dialogIcon(int resId, int tint) {
        ImageView icon = new ImageView(this);
        icon.setImageResource(resId);
        icon.setColorFilter(tint);
        icon.setPadding(dp(10), dp(10), dp(10), dp(10));
        icon.setBackground(round(ACCENT_BG, Color.TRANSPARENT, 999));
        return icon;
    }

    private LinearLayout.LayoutParams dialogMarginParams(
            int size, int left, int top, int bottom
    ) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(dp(size), dp(size));
        params.setMargins(dp(left), dp(top), dp(left), dp(bottom));
        return params;
    }

    private TextView dialogSectionLabel(String value) {
        TextView label = sectionLabel(value);
        label.setTextColor(MUTED);
        return label;
    }

    private TextView dialogAction(String value, boolean primary) {
        TextView button = actionButton(value, primary);
        button.setTextSize(10);
        return button;
    }

    private TextView dialogTextAction(String value) {
        TextView button = text(value, 10, ACCENT);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        button.setLetterSpacing(0.06f);
        button.setBackground(ripple(
                Color.TRANSPARENT,
                blend(ACCENT_BG, BG, 0.22f),
                Color.TRANSPARENT,
                SMALL_RADIUS
        ));
        installPressAnimation(button);
        return button;
    }

    private TextView dialogChoiceChip(String value, boolean selected) {
        TextView chip = text(value, 9, selected ? ACCENT : TEXT);
        chip.setGravity(Gravity.CENTER);
        chip.setTypeface(Typeface.create(
                "sans-serif-medium",
                Typeface.BOLD
        ));
        chip.setSingleLine(true);
        chip.setEllipsize(android.text.TextUtils.TruncateAt.END);
        chip.setPadding(dp(7), dp(4), dp(7), dp(4));
        chip.setBackground(choiceBackground(selected));
        chip.setClickable(true);
        chip.setFocusable(true);
        installPressAnimation(chip);
        return chip;
    }

    private GradientDrawable choiceBackground(boolean selected) {
        return round(
                selected ? ACCENT_BG : SURFACE_2,
                selected ? blend(ACCENT, BG, 0.48f) : Color.TRANSPARENT,
                selected ? BUTTON_RADIUS : SMALL_RADIUS
        );
    }

    private void showExpenseDatePicker(final LocalDate[] selectedDate, final TextView target) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout sheet = dialogSheet();

        TextView title = text("Choose date", 22, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        DatePicker picker = new DatePicker(this);
        picker.init(
                selectedDate[0].getYear(),
                selectedDate[0].getMonthValue() - 1,
                selectedDate[0].getDayOfMonth(),
                null
        );
        sheet.addView(picker, new LinearLayout.LayoutParams(-1, dp(300)));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        TextView cancel = dialogTextAction("CANCEL");
        TextView done = dialogAction("USE DATE", true);
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(48), 1f));
        LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(0, dp(48), 1.2f);
        doneParams.setMargins(dp(8), 0, 0, 0);
        actions.addView(done, doneParams);
        sheet.addView(actions, new LinearLayout.LayoutParams(-1, dp(48)));

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.width = WindowManager.LayoutParams.MATCH_PARENT;
            attrs.height = WindowManager.LayoutParams.WRAP_CONTENT;
            attrs.gravity = Gravity.BOTTOM;
            attrs.dimAmount = 0.52f;
            window.setAttributes(attrs);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        if (reducedMotion) {
            sheet.setAlpha(1f);
            sheet.setTranslationY(0f);
        } else {
            sheet.setAlpha(0f);
            sheet.setTranslationY(dp(60));
            dialog.setOnShowListener(d -> sheet.animate()
                    .alpha(1f).translationY(0)
                    .setDuration(280)
                    .setInterpolator(new DecelerateInterpolator())
                    .start());
        }

        cancel.setOnClickListener(v -> dialog.dismiss());
        done.setOnClickListener(v -> {
            selectedDate[0] = LocalDate.of(
                    picker.getYear(),
                    picker.getMonth() + 1,
                    picker.getDayOfMonth()
            );
            target.setText(
                    "DATE  •  " + (selectedDate[0].equals(LocalDate.now(zone))
                            ? "TODAY" : formatLongDate(selectedDate[0]))
            );
            dialog.dismiss();
        });

        dialog.show();
    }

    private void shake(View view) {
        if (view == null || reducedMotion) return;
        view.animate().cancel();
        view.animate()
                .translationX(dp(8))
                .setDuration(55)
                .withEndAction(() -> view.animate()
                        .translationX(-dp(8))
                        .setDuration(55)
                        .withEndAction(() -> view.animate()
                                .translationX(dp(4))
                                .setDuration(45)
                                .withEndAction(() -> view.animate()
                                        .translationX(0)
                                        .setDuration(55)
                                        .start())
                                .start())
                        .start())
                .start();
    }

    private void hideKeyboard() {
        View current = getCurrentFocus();
        if (current != null) {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
            current.clearFocus();
        }
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
                "Make your profile, colours and reminders your own.",
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

        page.addView(sectionLabel("COLOURS & THEMES"), sectionParams(0, 6));

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

        page.addView(sectionLabel("MOTION"), sectionParams(0, 7));

        LinearLayout motionCard = settingRow(R.drawable.ic_settings, ACCENT_2);
        LinearLayout motionText = new LinearLayout(this);
        motionText.setOrientation(LinearLayout.VERTICAL);

        TextView motionTitle = text("Reduce motion", 14, TEXT);
        motionTitle.setTypeface(Typeface.DEFAULT_BOLD);
        motionText.addView(motionTitle);

        TextView motionDescription = text(
                "Use quieter transitions and shorter list animations.",
                10,
                MUTED
        );
        motionDescription.setPadding(0, dp(4), 0, 0);
        motionText.addView(motionDescription);
        motionCard.addView(motionText, new LinearLayout.LayoutParams(0, -2, 1f));

        Switch motionSwitch = new Switch(this);
        motionSwitch.setChecked(reducedMotion);
        motionSwitch.setContentDescription("Reduce motion");
        motionSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(
                reducedMotion ? ACCENT : MUTED
        ));
        motionSwitch.setTrackTintList(android.content.res.ColorStateList.valueOf(
                reducedMotion ? ACCENT_BG : BORDER
        ));
        motionSwitch.setOnCheckedChangeListener((button, checked) -> {
            reducedMotion = checked;
            preferences.edit().putBoolean(KEY_REDUCED_MOTION, checked).apply();
            if (checked) {
                stopHeroPulse();
                if (homeHero != null) {
                    homeHero.animate().cancel();
                    homeHero.setAlpha(1f);
                    homeHero.setTranslationY(0f);
                }
            }
            motionSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(
                    checked ? ACCENT : MUTED
            ));
            motionSwitch.setTrackTintList(android.content.res.ColorStateList.valueOf(
                    checked ? ACCENT_BG : BORDER
            ));
        });
        motionCard.addView(motionSwitch, new LinearLayout.LayoutParams(dp(54), dp(42)));
        page.addView(motionCard, sectionParams(0, 18));

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

        if (!isNotificationGranted()) {
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

        page.addView(notificationCard, sectionParams(0, 16));

        page.addView(sectionLabel("MONEY"), sectionParams(0, 7));

        LinearLayout autoCard = settingRow(R.drawable.ic_wallet, ACCENT_2);

        LinearLayout autoText = new LinearLayout(this);
        autoText.setOrientation(LinearLayout.VERTICAL);

        TextView autoTitle = text("Payment tracking", 14, TEXT);
        autoTitle.setTypeface(Typeface.DEFAULT_BOLD);
        autoText.addView(autoTitle);

        TextView autoDescription = text(
                TransactionNotificationAccess.hasAccess(this)
                        && SmsTransactionService.isEnabled(preferences)
                        ? "Connected. Supported bank and UPI notification alerts are parsed locally; raw notification text is not saved."
                        : "Connect notification access to detect supported bank and UPI alerts automatically.",
                10,
                MUTED
        );
        autoDescription.setMaxLines(3);
        autoDescription.setPadding(0, dp(4), 0, 0);
        autoText.addView(autoDescription);

        TextView safeBadge = text(
                TransactionNotificationAccess.hasAccess(this)
                        && SmsTransactionService.isEnabled(preferences) ? "AUTO ON" : "CONNECT",
                8, ACCENT
        );
        safeBadge.setGravity(Gravity.CENTER);
        safeBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        safeBadge.setPadding(dp(9), 0, dp(9), 0);
        safeBadge.setBackground(round(ACCENT_BG, Color.TRANSPARENT, PILL_RADIUS()));

        autoCard.addView(autoText, new LinearLayout.LayoutParams(0, -2, 1f));
        autoCard.addView(safeBadge, new LinearLayout.LayoutParams(-2, dp(32)));
        autoCard.setClickable(true);
        autoCard.setFocusable(true);
        autoCard.setOnClickListener(v -> {
            tap(v);
            showPaymentAlertTrackingManager();
        });

        page.addView(autoCard, sectionParams(0, 22));

        page.addView(sectionLabel("BACKUP & RESTORE"), sectionParams(0, 7));

        LinearLayout backupCard = cardColumn();
        TextView backupTitle = text("Your NextBell data", 15, TEXT);
        backupTitle.setTypeface(Typeface.DEFAULT_BOLD);
        backupCard.addView(backupTitle);

        TextView backupDescription = text(
                "Save your profile, study records, themes and money data to a file you choose. Anyone with that file can read it.",
                10,
                MUTED
        );
        backupDescription.setPadding(0, dp(5), 0, dp(13));
        backupCard.addView(backupDescription);

        LinearLayout backupActions = new LinearLayout(this);
        backupActions.setGravity(Gravity.CENTER_VERTICAL);
        TextView exportBackup = actionButton("Export backup", true);
        TextView importBackup = actionButton("Restore backup", false);
        backupActions.addView(exportBackup, new LinearLayout.LayoutParams(0, dp(46), 1f));
        LinearLayout.LayoutParams importParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        importParams.setMargins(dp(9), 0, 0, 0);
        backupActions.addView(importBackup, importParams);
        backupCard.addView(backupActions);

        exportBackup.setOnClickListener(v -> {
            tap(v);
            startBackupExport();
        });
        importBackup.setOnClickListener(v -> {
            tap(v);
            startBackupImport();
        });
        page.addView(backupCard, sectionParams(0, 22));

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

        TextView footer = text("Your profile and spending stay on this phone.", 8, SUBTLE);
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
        card.setBackground(glassGradient(
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
                glassColor(SURFACE),
                blend(glassColor(SURFACE), BG, 0.10f),
                glassBorder(BORDER),
                CARD_RADIUS
        ));
        card.setElevation(dp(Math.max(1, CARD_ELEVATION - 1)));

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
        LinearLayout sheet = dialogSheet();

        ImageView icon = dialogIcon(R.drawable.ic_settings, ACCENT);
        sheet.addView(icon, dialogMarginParams(48, 0, 0, 14));

        TextView title = text("Edit profile", 23, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        sheet.addView(title);

        TextView subtitle = text(
                "Update the details NextBell uses for your timetable.",
                10,
                MUTED
        );
        subtitle.setPadding(0, dp(5), 0, dp(15));
        sheet.addView(subtitle);

        EditText nameInput = field("Your name", false);
        nameInput.setText(getName());
        nameInput.setSelection(nameInput.length());
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, dp(50));
        sheet.addView(nameInput, nameParams);

        EditText rollInput = field("Roll number", true);
        rollInput.setText(String.valueOf(getRollNumber()));
        LinearLayout.LayoutParams rollParams = new LinearLayout.LayoutParams(-1, dp(50));
        rollParams.setMargins(0, dp(9), 0, 0);
        sheet.addView(rollInput, rollParams);

        TextView batch = text(
                "Your batch: " + getBatch() + "  •  " +
                        TimetableData.rollRangeForBatch(getBatch()),
                9,
                MUTED
        );
        batch.setPadding(0, dp(8), 0, 0);
        sheet.addView(batch);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        TextView cancel = dialogTextAction("CANCEL");
        TextView save = dialogAction("SAVE CHANGES", true);
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(48), 1f));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, dp(48), 1.35f);
        saveParams.setMargins(dp(8), 0, 0, 0);
        actions.addView(save, saveParams);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, dp(48));
        actionsParams.setMargins(0, dp(17), 0, 0);
        sheet.addView(actions, actionsParams);

        Dialog dialog = showBottomSheet(sheet);
        nameInput.requestFocus();
        nameInput.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(nameInput, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 220);

        cancel.setOnClickListener(v -> {
            hideKeyboard();
            dialog.dismiss();
        });

        save.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                nameInput.setError("Enter your name.");
                shake(save);
                return;
            }

            try {
                int roll = Integer.parseInt(rollInput.getText().toString().trim());
                if (roll < 1 || roll > 999) throw new NumberFormatException();

                saveProfile(name, roll);
                weekSelectedDay = todayNameOrMonday();
                NotificationScheduler.scheduleUpcoming(this);
                hideKeyboard();
                dialog.dismiss();
                navigate("settings");
            } catch (NumberFormatException ignored) {
                rollInput.setError("Enter a valid roll number.");
                shake(save);
            }
        });
    }

    private void refreshLiveUi() {
        if ("home".equals(selectedScreen)) {
            updateClock();
        }

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
            animateHero(
                    "break:" + currentBreak.id,
                    blend(SURFACE_2, breakAccent, 0.22f),
                    breakAccent
            );
        } else if (currentClass != null) {
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
            animateHero("current:" + currentClass.id, ACCENT_BG, ACCENT);
        } else if (next != null) {
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
            animateHero("next:" + next.id, SURFACE_2, BORDER);
        } else {
            heroLabel.setText("DAY COMPLETE");
            heroLabel.setTextColor(ACCENT);
            heroTimeRange.setText("—");
            heroSubject.setText("You're done for today.");
            heroMeta.setText("No more classes scheduled");
            clearHeroLocation();
            heroCountdown.setText("✓");
            heroCountdownLabel.setText("nothing else scheduled");
            animateHero("done", SURFACE_2, BORDER);
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
        if (reducedMotion) {
            view.setAlpha(1f);
            view.setTranslationY(0f);
            return;
        }
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
        if (view == null || reducedMotion) return;
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

    private void animateHero(String state, int endColor, int borderColor) {
        if (state.equals(lastHeroState)) return;
        lastHeroState = state;

        if (homeHero != null) {
            homeHero.setBackground(glassGradient(
                    SURFACE,
                    endColor,
                    borderColor,
                    CARD_RADIUS,
                    themeGradientOrientation()
            ));
        }

        boolean current = state.startsWith("current:");
        if (current && !reducedMotion) {
            startHeroPulse();
        } else {
            stopHeroPulse();
        }

        if (homeHero != null) {
            if (reducedMotion) {
                homeHero.animate().cancel();
                homeHero.setAlpha(1f);
                homeHero.setTranslationY(0f);
                return;
            }
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
        if (reducedMotion || heroPulseDot == null || heroPulseAnimator != null) return;
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
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(this, "No browser or WhatsApp app is available.", Toast.LENGTH_LONG).show();
        }
    }

    private void openCall() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:7499517574"));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(this, "No dialer app is available.", Toast.LENGTH_LONG).show();
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
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(this, "No email app is available.", Toast.LENGTH_LONG).show();
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
        NotificationManager manager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || !manager.areNotificationsEnabled()) return false;
        NotificationChannel channel = manager.getNotificationChannel(
                NotificationScheduler.CHANNEL_ID
        );
        if (channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            openNotificationSettings();
            return;
        }

        boolean requestedBefore = preferences.getBoolean(
                KEY_NOTIFICATION_PERMISSION_REQUESTED, false
        );
        if (requestedBefore && !shouldShowRequestPermissionRationale(
                Manifest.permission.POST_NOTIFICATIONS
        )) {
            openNotificationSettings();
            return;
        }

        preferences.edit()
                .putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true)
                .apply();

        requestPermissions(
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                7001
        );
    }

    private void openNotificationSettings() {
        openedNotificationSettings = true;
        Intent settingsIntent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        try {
            startActivity(settingsIntent);
        } catch (ActivityNotFoundException exception) {
            Intent appSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            try {
                startActivity(appSettings);
            } catch (ActivityNotFoundException ignored) {
                Toast.makeText(this, "Open NextBell in system settings to enable reminders.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showPaymentAlertTrackingManager() {
        boolean access = TransactionNotificationAccess.hasAccess(this);
        boolean enabled = SmsTransactionService.isEnabled(preferences);
        boolean active = access && enabled;

        long lastSeenAt = preferences.getLong(
                SmsTransactionService.KEY_LAST_NOTIFICATION_AT, 0L
        );
        String lastSeen = lastSeenAt <= 0L ? "Never"
                : java.time.Instant.ofEpochMilli(lastSeenAt).atZone(zone)
                        .format(DateTimeFormatter.ofPattern("dd MMM, hh:mm a", Locale.ENGLISH));
        String lastPackage = preferences.getString(
                SmsTransactionService.KEY_LAST_NOTIFICATION_PACKAGE, ""
        );
        String lastResult = preferences.getString(
                SmsTransactionService.KEY_LAST_NOTIFICATION_RESULT, "No alert processed yet"
        );
        long importedCount = preferences.getLong(
                SmsTransactionService.KEY_NOTIFICATION_IMPORTED_COUNT, 0L
        );
        long seenCount = preferences.getLong(
                SmsTransactionService.KEY_NOTIFICATION_SEEN_COUNT, 0L
        );

        String diagnostics = "Notification access: " + (access ? "GRANTED" : "NOT GRANTED")
                + "\\nTracking: " + (active ? "ON" : enabled ? "WAITING FOR ACCESS" : "OFF")
                + "\\nSupported-app notifications seen: " + seenCount
                + "\\nTransactions added: " + importedCount
                + "\\nLast alert: " + lastSeen
                + (lastPackage.isEmpty() ? "" : " (" + lastPackage + ")")
                + "\\nLast result: " + lastResult;

        String disclosure = "NextBell watches notification previews from supported messaging, bank "
                + "and UPI apps after you explicitly enable Android notification access. Matching "
                + "transaction details are saved on this phone; notification text is not retained "
                + "or uploaded. This does not scan old SMS messages. Your bank or messaging app "
                + "must show transaction details in its notifications, and notifications hidden "
                + "by Android cannot be read.\\n\\n"
                + diagnostics;

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(active ? "Payment alerts connected" : "Automatic payment tracking")
                .setMessage(disclosure)
                .setNegativeButton(enabled ? "Turn off" : "Close", (dialog, which) -> {
                    if (enabled) {
                        SmsTransactionService.setEnabled(preferences, false);
                        refreshMoneyAndSettings();
                        Toast.makeText(this, "Automatic payment tracking is off.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .setPositiveButton(access ? (enabled ? "Done" : "Enable tracking")
                                : "Open notification settings", (dialog, which) -> {
                    if (access) {
                        SmsTransactionService.setEnabled(preferences, true);
                        refreshMoneyAndSettings();
                        Toast.makeText(this, "Payment alert tracking is enabled.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        openPaymentNotificationSettings();
                    }
                })
                .setNeutralButton("Manage access", (dialog, which) -> openPaymentNotificationSettings());
        builder.show();
    }

    private void openPaymentNotificationSettings() {
        preferences.edit().putBoolean(KEY_PENDING_PAYMENT_ALERT_SETUP, true).apply();
        try {
            startActivity(TransactionNotificationAccess.settingsIntent());
        } catch (ActivityNotFoundException exception) {
            preferences.edit().remove(KEY_PENDING_PAYMENT_ALERT_SETUP).apply();
            new AlertDialog.Builder(this)
                    .setTitle("Notification access")
                    .setMessage("Open Android Settings, search for Notification access, and enable NextBell.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void refreshMoneyAndSettings() {
        if ("expenses".equals(selectedScreen)) buildExpensesScreen();
        else if ("settings".equals(selectedScreen)) buildSettingsScreen();
        updateBottomNav();
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
            return;
        }

    }

    private void startBackupExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(LocalDataBackup.MIME_TYPE);
        intent.putExtra(
                Intent.EXTRA_TITLE,
                "NextBell-backup-" + LocalDate.now(zone) + ".json"
        );
        try {
            startActivityForResult(intent, REQUEST_EXPORT_BACKUP);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, "No file app is available to save a backup.", Toast.LENGTH_LONG).show();
        }
    }

    private void startBackupImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(LocalDataBackup.MIME_TYPE);
        try {
            startActivityForResult(intent, REQUEST_IMPORT_BACKUP);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, "No file app is available to open a backup.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        Uri uri = data.getData();
        if (requestCode == REQUEST_EXPORT_BACKUP) {
            try (java.io.OutputStream output = getContentResolver().openOutputStream(uri)) {
                if (output == null) throw new java.io.IOException("Could not open the backup file.");
                LocalDataBackup.write(preferences, output);
                Toast.makeText(this, "Backup saved.", Toast.LENGTH_LONG).show();
            } catch (Exception exception) {
                Toast.makeText(this, "Could not save that backup file.", Toast.LENGTH_LONG).show();
            }
            return;
        }

        if (requestCode == REQUEST_IMPORT_BACKUP) {
            try (java.io.InputStream input = getContentResolver().openInputStream(uri)) {
                if (input == null) throw new java.io.IOException("Could not open the backup file.");
                Map<String, Object> backup = LocalDataBackup.read(input);
                confirmBackupRestore(backup);
            } catch (Exception exception) {
                Toast.makeText(this, "That file is not a readable NextBell backup.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void confirmBackupRestore(Map<String, Object> backup) {
        new AlertDialog.Builder(this)
                .setTitle("Replace this phone’s NextBell data?")
                .setMessage(
                        "Restoring replaces the profile, theme, reminders and spending currently in NextBell. "
                                + "The backup has been checked and your current data is not changed unless you continue."
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Restore backup", (dialog, which) -> {
                    try {
                        LocalDataBackup.restore(preferences, backup);
                        themeIndex = clampTheme(preferences.getInt(KEY_THEME, 0));
                        reducedMotion = preferences.getBoolean(KEY_REDUCED_MOTION, false);
                        selectedScreen = "home";
                        lastScreenIndex = 0;
                        lastHeroState = "";
                        lastHomeScheduleSignature = "";
                        lastWeekScheduleSignature = "";
                        applyTheme();
                        if (hasSavedProfile()) {
                            weekSelectedDay = todayNameOrMonday();
                            openApp();
                        } else {
                            showSetupScreen();
                        }
                        Toast.makeText(this, "Backup restored.", Toast.LENGTH_LONG).show();
                    } catch (Exception exception) {
                        Toast.makeText(this, "Could not restore that backup.", Toast.LENGTH_LONG).show();
                    }
                })
                .show();
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

    private GradientDrawable glassGradient(
            int start,
            int end,
            int stroke,
            int radiusDp,
            GradientDrawable.Orientation orientation
    ) {
        GradientDrawable d = new GradientDrawable(
                orientation,
                new int[]{glassColor(blend(start, TEXT, 0.025f)),
                        glassColor(blend(end, TEXT, 0.045f))}
        );
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), glassBorder(stroke));
        return d;
    }

    private int glassColor(int color) {
        return Color.argb(themeIndex == 5 ? 246 : 226,
                Color.red(color), Color.green(color), Color.blue(color));
    }

    private int glassBorder(int color) {
        return Color.argb(themeIndex == 5 ? 145 : 125,
                Color.red(color), Color.green(color), Color.blue(color));
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
        if ("study".equals(screen)) return 3;
        return 4;
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

        if (reducedMotion) {
            view.animate().cancel();
            view.setAlpha(1f);
            view.setScaleX(1f);
            view.setScaleY(1f);
            view.setTranslationX(0f);
            view.setTranslationY(0f);
            return;
        }
        view.setAlpha(0f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setTranslationX(forward ? dp(14) : -dp(14));
        view.setTranslationY(0);

        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationX(0)
                .translationY(0)
                .setDuration(190)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }

    private void animateThemeRefresh() {
        if (root == null) return;
        if (reducedMotion) return;
        root.setAlpha(0.94f);
        root.animate()
                .alpha(1f)
                .setDuration(150)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void animateListItem(View view, int index) {
        if (view == null) return;
        if (reducedMotion) {
            view.setAlpha(1f);
            view.setTranslationY(0f);
            view.setScaleX(1f);
            view.setScaleY(1f);
            return;
        }
        view.setAlpha(0f);
        view.setTranslationY(dp(6));
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.animate()
                .alpha(1f)
                .translationY(0)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(Math.min(120, index * 20L))
                .setDuration(170)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void installPressAnimation(View view) {
        if (view == null) return;
        if (reducedMotion) return;
        view.setClickable(true);
        view.setFocusable(true);
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().cancel();
                    v.animate()
                            .scaleX(0.985f)
                            .scaleY(0.985f)
                            .alpha(0.96f)
                            .setDuration(65)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().cancel();
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(110)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    break;
            }
            return false;
        });
    }

    private class SpendingChartView extends View {
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
