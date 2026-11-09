import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  DAYS,
  DEFAULT_PROFILE,
  EXPENSE_CATEGORIES,
  INITIAL_ATTENDANCE,
  INITIAL_EXPENSES,
  INITIAL_TASKS,
  RAW_TIMETABLE,
  SUBJECTS_MAP,
  THEMES,
  batchForRoll,
  filterTimetableForRoll,
  rollRangeForBatch,
  CAMPUS_LOCATIONS
} from './data.js';
import { createBackupJson, downloadBackupFile, parseBackupFile } from './backupUtils.js';
import {
  createCloudAccount,
  getFirebaseServices,
  isFirebaseConfigured,
  NextBellCloudSync,
  signInCloudAccount,
  signOutCloudAccount,
} from './cloudSync.js';

function parseMinutes(timeStr) {
  if (!timeStr) return 0;
  const [h, m] = timeStr.split(':').map(Number);
  return h * 60 + m;
}

function formatTime(timeStr) {
  if (!timeStr) return '';
  const [h, m] = timeStr.split(':').map(Number);
  const suffix = h >= 12 ? 'PM' : 'AM';
  const hour = h % 12 || 12;
  return `${hour}:${String(m).padStart(2, '0')} ${suffix}`;
}

function getNowMinutesIST() {
  const parts = new Intl.DateTimeFormat('en-GB', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
    timeZone: 'Asia/Kolkata'
  }).formatToParts(new Date());

  const h = Number(parts.find((p) => p.type === 'hour')?.value ?? 0);
  const m = Number(parts.find((p) => p.type === 'minute')?.value ?? 0);
  return h * 60 + m;
}

function getTodayDayNameIST() {
  return new Intl.DateTimeFormat('en-US', {
    weekday: 'long',
    timeZone: 'Asia/Kolkata'
  }).format(new Date());
}

function getTodayDateStringIST() {
  const parts = new Intl.DateTimeFormat('en-CA', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    timeZone: 'Asia/Kolkata'
  }).formatToParts(new Date());

  const y = parts.find((p) => p.type === 'year')?.value;
  const m = parts.find((p) => p.type === 'month')?.value;
  const d = parts.find((p) => p.type === 'day')?.value;
  return `${y}-${m}-${d}`;
}

export default function App() {
  const persistedLocalDataRef = useRef({
    profile: localStorage.getItem('nextbell_profile') !== null,
    theme: localStorage.getItem('nextbell_theme') !== null,
    attendance: localStorage.getItem('nextbell_attendance') !== null,
    attendanceTarget: localStorage.getItem('nextbell_attendance_target') !== null,
    tasks: localStorage.getItem('nextbell_tasks') !== null,
    expenses: localStorage.getItem('nextbell_expenses') !== null,
    dailyLimit: localStorage.getItem('nextbell_daily_limit') !== null,
  });

  // --- Persistent State ---
  const [profile, setProfile] = useState(() => {
    try {
      const saved = localStorage.getItem('nextbell_profile');
      return saved ? JSON.parse(saved) : DEFAULT_PROFILE;
    } catch {
      return DEFAULT_PROFILE;
    }
  });

  const [themeIndex, setThemeIndex] = useState(() => {
    try {
      const saved = localStorage.getItem('nextbell_theme');
      return saved !== null ? Number(saved) : 0;
    } catch {
      return 0;
    }
  });

  const [attendance, setAttendance] = useState(() => {
    try {
      const saved = localStorage.getItem('nextbell_attendance');
      return saved ? JSON.parse(saved) : INITIAL_ATTENDANCE;
    } catch {
      return INITIAL_ATTENDANCE;
    }
  });

  const [attendanceTarget, setAttendanceTarget] = useState(() => {
    try {
      const saved = localStorage.getItem('nextbell_attendance_target');
      return saved ? Number(saved) : 75;
    } catch {
      return 75;
    }
  });

  const [tasks, setTasks] = useState(() => {
    try {
      const saved = localStorage.getItem('nextbell_tasks');
      return saved ? JSON.parse(saved) : INITIAL_TASKS;
    } catch {
      return INITIAL_TASKS;
    }
  });

  const [expenses, setExpenses] = useState(() => {
    try {
      const saved = localStorage.getItem('nextbell_expenses');
      return saved ? JSON.parse(saved) : INITIAL_EXPENSES;
    } catch {
      return INITIAL_EXPENSES;
    }
  });

  const [dailyLimitPaise, setDailyLimitPaise] = useState(() => {
    try {
      const saved = localStorage.getItem('nextbell_daily_limit');
      return saved ? Number(saved) : 50000; // Rs 500 default
    } catch {
      return 50000;
    }
  });

  const handleCloudSignIn = async (createAccount = false) => {
    if (!cloudEmail.trim() || !cloudPassword) {
      showToast('Enter your email and password first.');
      return;
    }
    setCloudBusy(true);
    try {
      if (createAccount) await createCloudAccount(cloudEmail, cloudPassword);
      else await signInCloudAccount(cloudEmail, cloudPassword);
      setCloudPassword('');
      showToast(createAccount ? 'NextBell cloud account created.' : 'Signed in to NextBell.');
    } catch (error) {
      const messages = {
        'auth/email-already-in-use': 'This email already has a NextBell account. Sign in instead.',
        'auth/invalid-email': 'Enter a valid email address.',
        'auth/weak-password': 'Use a password with at least 6 characters.',
        'auth/invalid-credential': 'Email or password is incorrect.',
        'auth/network-request-failed': 'Could not reach Firebase. Check your internet connection.',
        'auth/operation-not-allowed': 'Enable Email/Password sign-in in Firebase Authentication.',
      };
      showToast(messages[error?.code] || error?.message || 'Could not connect to Firebase.');
    } finally {
      setCloudBusy(false);
    }
  };

  const handleCloudSignOut = async () => {
    try {
      await signOutCloudAccount();
      showToast('Signed out. Your local data remains on this device.');
    } catch (error) {
      showToast(error?.message || 'Could not sign out.');
    }
  };

  // --- UI Navigation State ---
  const [activeTab, setActiveTab] = useState('Today');
  const todayName = getTodayDayNameIST();
  const [selectedDay, setSelectedDay] = useState(DAYS.includes(todayName) ? todayName : 'Monday');
  const [nowMinutes, setNowMinutes] = useState(getNowMinutesIST());
  const [clockSeconds, setClockSeconds] = useState('');
  const [weekBatchFilter, setWeekBatchFilter] = useState('ALL');
  const [taskFilter, setTaskFilter] = useState('ALL');
  const [toastMessage, setToastMessage] = useState(null);
  const [cloudUser, setCloudUser] = useState(null);
  const [cloudStatus, setCloudStatus] = useState('Not connected');
  const [cloudEmail, setCloudEmail] = useState('');
  const [cloudPassword, setCloudPassword] = useState('');
  const [cloudBusy, setCloudBusy] = useState(false);
  const cloudSyncRef = useRef(null);
  const latestCloudStateRef = useRef(null);
  latestCloudStateRef.current = {
    profile, themeIndex, attendance, attendanceTarget, tasks, expenses, dailyLimitPaise,
  };

  // --- Dialog References ---
  const profileDialogRef = useRef(null);
  const taskDialogRef = useRef(null);
  const expenseDialogRef = useRef(null);
  const backupDialogRef = useRef(null);
  const fileInputRef = useRef(null);

  // Form states for modals
  const [editProfileForm, setEditProfileForm] = useState(profile);
  const [newTaskForm, setNewTaskForm] = useState({
    title: '',
    course: 'FPL',
    type: 'ASSIGNMENT',
    dueDate: getTodayDateStringIST(),
    priority: 'MEDIUM',
  });
  const [newExpenseForm, setNewExpenseForm] = useState({
    amountRupees: '',
    category: 'Food & Drinks',
    note: '',
    paymentMode: 'UPI',
    date: getTodayDateStringIST(),
  });

  // Sync profile edits
  useEffect(() => {
    setEditProfileForm(profile);
  }, [profile]);

  // Save to LocalStorage
  useEffect(() => {
    localStorage.setItem('nextbell_profile', JSON.stringify(profile));
  }, [profile]);

  useEffect(() => {
    localStorage.setItem('nextbell_theme', String(themeIndex));
    const activeTheme = THEMES[themeIndex] || THEMES[0];
    document.documentElement.setAttribute('data-theme', activeTheme.id);
  }, [themeIndex]);

  useEffect(() => {
    localStorage.setItem('nextbell_attendance', JSON.stringify(attendance));
  }, [attendance]);

  useEffect(() => {
    localStorage.setItem('nextbell_attendance_target', String(attendanceTarget));
  }, [attendanceTarget]);

  useEffect(() => {
    localStorage.setItem('nextbell_tasks', JSON.stringify(tasks));
  }, [tasks]);

  useEffect(() => {
    localStorage.setItem('nextbell_expenses', JSON.stringify(expenses));
  }, [expenses]);

  useEffect(() => {
    localStorage.setItem('nextbell_daily_limit', String(dailyLimitPaise));
  }, [dailyLimitPaise]);

  // One Firebase account is shared by the website and Android app.
  useEffect(() => {
    const client = getFirebaseServices();
    if (!client) {
      setCloudStatus(isFirebaseConfigured()
        ? 'Firebase SDK could not load. Check your network and reload.'
        : 'Firebase setup required');
      return undefined;
    }

    const unsubscribeAuth = client.auth.onAuthStateChanged((user) => {
      cloudSyncRef.current?.stop();
      cloudSyncRef.current = null;
      setCloudUser(user || null);

      if (!user) {
        setCloudStatus('Not connected');
        return;
      }

      setCloudStatus('Connecting to your NextBell data…');
      const sync = new NextBellCloudSync({
        uid: user.uid,
        getLocalState: () => ({
          ...latestCloudStateRef.current,
          persisted: persistedLocalDataRef.current,
        }),
        onStatus: setCloudStatus,
        onInitialState: (state) => {
          if (state.profile) setProfile(state.profile);
          setAttendance(Array.isArray(state.attendance) ? state.attendance : []);
          setTasks(Array.isArray(state.tasks) ? state.tasks : []);
          setExpenses(Array.isArray(state.expenses) ? state.expenses : []);
          if (state.settings) {
            if (Number.isFinite(Number(state.settings.themeIndex))) {
              setThemeIndex(Number(state.settings.themeIndex));
            }
            if (Number.isFinite(Number(state.settings.attendanceTarget))) {
              setAttendanceTarget(Number(state.settings.attendanceTarget));
            }
            if (Number.isFinite(Number(state.settings.dailyLimitPaise))) {
              setDailyLimitPaise(Number(state.settings.dailyLimitPaise));
            }
          }
        },
        onRemoteRecord: (collection, id, payload, deleted) => {
          const updateArray = (previous, keyName) => {
            const list = Array.isArray(previous) ? previous : [];
            const next = list.filter((item) => String(item?.[keyName]) !== String(id));
            if (!deleted && payload) next.unshift(payload);
            return next;
          };

          if (collection === 'expenses') {
            setExpenses((previous) => updateArray(previous, 'id'));
          } else if (collection === 'attendance') {
            setAttendance((previous) => updateArray(previous, 'key'));
          } else if (collection === 'tasks') {
            setTasks((previous) => updateArray(previous, 'id'));
          } else if (collection === 'profile' && !deleted && payload) {
            setProfile(payload);
          } else if (collection === 'settings' && !deleted && payload) {
            if (Number.isFinite(Number(payload.themeIndex))) setThemeIndex(Number(payload.themeIndex));
            if (Number.isFinite(Number(payload.attendanceTarget))) {
              setAttendanceTarget(Number(payload.attendanceTarget));
            }
            if (Number.isFinite(Number(payload.dailyLimitPaise))) {
              setDailyLimitPaise(Number(payload.dailyLimitPaise));
            }
          }
        },
      });
      cloudSyncRef.current = sync;
      sync.start();
    });

    return () => {
      unsubscribeAuth();
      cloudSyncRef.current?.stop();
      cloudSyncRef.current = null;
    };
  }, []);

  // Each record is synced independently. Firestore's offline queue retries
  // writes when the browser reconnects; a snapshot updates the other device.
  useEffect(() => {
    cloudSyncRef.current?.pushCollection('expenses', expenses);
  }, [expenses]);

  useEffect(() => {
    cloudSyncRef.current?.pushCollection('attendance', attendance);
  }, [attendance]);

  useEffect(() => {
    cloudSyncRef.current?.pushCollection('tasks', tasks);
  }, [tasks]);

  useEffect(() => {
    cloudSyncRef.current?.pushProfile(profile);
  }, [profile]);

  useEffect(() => {
    cloudSyncRef.current?.pushSettings({
      themeIndex,
      attendanceTarget,
      dailyLimitPaise,
    });
  }, [themeIndex, attendanceTarget, dailyLimitPaise]);

  // Live Clock & Minutes Tick
  useEffect(() => {
    const updateTime = () => {
      setNowMinutes(getNowMinutesIST());
      const now = new Intl.DateTimeFormat('en-US', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: true,
        timeZone: 'Asia/Kolkata'
      }).format(new Date());
      setClockSeconds(now);
    };
    updateTime();
    const interval = setInterval(updateTime, 1000);
    return () => clearInterval(interval);
  }, []);

  // Toast Helper
  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3200);
  };

  // Modern Web Guidance: Light Dismiss Fallback for <dialog>
  useEffect(() => {
    const dialogs = [profileDialogRef.current, taskDialogRef.current, expenseDialogRef.current, backupDialogRef.current];
    if (!('closedBy' in HTMLDialogElement.prototype)) {
      dialogs.forEach((dlg) => {
        if (!dlg) return;
        const clickHandler = (event) => {
          if (event.target !== dlg) return;
          const rect = dlg.getBoundingClientRect();
          const inDialog =
            rect.top <= event.clientY &&
            event.clientY <= rect.top + rect.height &&
            rect.left <= event.clientX &&
            event.clientX <= rect.left + rect.width;
          if (!inDialog) dlg.close();
        };
        dlg.addEventListener('click', clickHandler);
        return () => dlg.removeEventListener('click', clickHandler);
      });
    }
  }, []);

  // Compute Active Batch from Roll
  const studentBatch = useMemo(() => batchForRoll(profile.rollNo), [profile.rollNo]);

  // Classes for Selected Day
  const daySchedule = useMemo(() => {
    return filterTimetableForRoll(profile.rollNo, selectedDay);
  }, [profile.rollNo, selectedDay]);

  // Current and Next class calculations
  const academicClasses = useMemo(() => {
    return daySchedule.filter((c) => c.kind !== 'SHORT_BREAK' && c.kind !== 'LUNCH');
  }, [daySchedule]);

  const currentClass = useMemo(() => {
    return daySchedule.find((c) => {
      const s = parseMinutes(c.start);
      const e = parseMinutes(c.end);
      return s <= nowMinutes && nowMinutes < e;
    });
  }, [daySchedule, nowMinutes]);

  const nextClass = useMemo(() => {
    return daySchedule.find((c) => {
      const s = parseMinutes(c.start);
      return s > nowMinutes;
    });
  }, [daySchedule, nowMinutes]);

  const heroClass = currentClass || nextClass;
  const heroIsCurrent = Boolean(currentClass);

  const heroMinutesLeft = useMemo(() => {
    if (!heroClass) return 0;
    if (heroIsCurrent) {
      return Math.max(0, parseMinutes(heroClass.end) - nowMinutes);
    }
    return Math.max(0, parseMinutes(heroClass.start) - nowMinutes);
  }, [heroClass, heroIsCurrent, nowMinutes]);

  // Attendance Calculations & Bunk Intelligence
  const attendanceStats = useMemo(() => {
    const subjectMap = {};

    // Group sessions by subject
    attendance.forEach((sess) => {
      if (!subjectMap[sess.subject]) {
        subjectMap[sess.subject] = { held: 0, attended: 0, missed: 0 };
      }
      subjectMap[sess.subject].held += 1;
      if (sess.status === 'PRESENT') {
        subjectMap[sess.subject].attended += 1;
      } else {
        subjectMap[sess.subject].missed += 1;
      }
    });

    let totalHeld = 0;
    let totalAttended = 0;
    Object.values(subjectMap).forEach((s) => {
      totalHeld += s.held;
      totalAttended += s.attended;
    });

    const overallPct = totalHeld === 0 ? 0 : Math.round((totalAttended * 100) / totalHeld);

    // Bunk formula: (attended * 100) / target - held
    const target = attendanceTarget || 75;
    let safeBunks = 0;
    let neededClasses = 0;

    if (totalHeld > 0) {
      if (overallPct >= target) {
        safeBunks = Math.floor((totalAttended * 100) / target - totalHeld);
      } else {
        neededClasses = Math.ceil((target * totalHeld - 100 * totalAttended) / (100 - target));
      }
    }

    return {
      totalHeld,
      totalAttended,
      overallPct,
      safeBunks: Math.max(0, safeBunks),
      neededClasses: Math.max(0, neededClasses),
      subjectMap
    };
  }, [attendance, attendanceTarget]);

  // Expenses Calculations
  const todayDateStr = getTodayDateStringIST();
  const spentTodayPaise = useMemo(() => {
    return expenses
      .filter((e) => e.date === todayDateStr)
      .reduce((sum, e) => sum + (Number(e.amountPaise) || 0), 0);
  }, [expenses, todayDateStr]);

  const spentThisMonthPaise = useMemo(() => {
    const curMonth = todayDateStr.slice(0, 7);
    return expenses
      .filter((e) => (e.date || '').startsWith(curMonth))
      .reduce((sum, e) => sum + (Number(e.amountPaise) || 0), 0);
  }, [expenses, todayDateStr]);

  // Category breakdown
  const categorySpending = useMemo(() => {
    const curMonth = todayDateStr.slice(0, 7);
    const map = {};
    expenses
      .filter((e) => (e.date || '').startsWith(curMonth))
      .forEach((e) => {
        const cat = e.category || 'Other';
        map[cat] = (map[cat] || 0) + (Number(e.amountPaise) || 0);
      });
    return Object.entries(map).sort((a, b) => b[1] - a[1]);
  }, [expenses, todayDateStr]);

  // Attendance Mark Quick Action
  const handleMarkAttendance = (subject, status) => {
    const date = getTodayDateStringIST();
    const key = `${subject}_${date}_${nowMinutes}`;
    setAttendance((prev) => [{ key, subject, date, status }, ...prev]);
    showToast(`Marked ${status === 'PRESENT' ? 'Present' : 'Absent'} for ${subject}`);
  };

  // Toggle Task Completion
  const handleToggleTask = (taskId) => {
    setTasks((prev) =>
      prev.map((t) => (t.id === taskId ? { ...t, completed: !t.completed } : t))
    );
  };

  // Delete Task
  const handleDeleteTask = (taskId) => {
    setTasks((prev) => prev.filter((t) => t.id !== taskId));
    showToast('Task removed');
  };

  // Delete Expense
  const handleDeleteExpense = (expId) => {
    setExpenses((prev) => prev.filter((e) => e.id !== expId));
    showToast('Expense removed');
  };

  // Submit Profile Form
  const handleSaveProfile = (e) => {
    e.preventDefault();
    const roll = Number(editProfileForm.rollNo) || 34;
    setProfile({
      ...editProfileForm,
      rollNo: roll,
    });
    profileDialogRef.current?.close();
    showToast('Profile updated!');
  };

  // Submit Task Form
  const handleCreateTask = (e) => {
    e.preventDefault();
    if (!newTaskForm.title.trim()) return;
    const taskItem = {
      id: Date.now(),
      title: newTaskForm.title.trim(),
      course: newTaskForm.course,
      type: newTaskForm.type,
      dueDate: newTaskForm.dueDate,
      priority: newTaskForm.priority,
      completed: false
    };
    setTasks((prev) => [taskItem, ...prev]);
    setNewTaskForm({
      title: '',
      course: 'FPL',
      type: 'ASSIGNMENT',
      dueDate: getTodayDateStringIST(),
      priority: 'MEDIUM',
    });
    taskDialogRef.current?.close();
    showToast('Task added to planner!');
  };

  // Submit Expense Form
  const handleCreateExpense = (e) => {
    e.preventDefault();
    const rupees = parseFloat(newExpenseForm.amountRupees);
    if (!rupees || rupees <= 0) return;
    const expenseItem = {
      id: Date.now(),
      amountPaise: Math.round(rupees * 100),
      category: newExpenseForm.category,
      note: newExpenseForm.note.trim() || newExpenseForm.category,
      paymentMode: newExpenseForm.paymentMode,
      date: newExpenseForm.date || getTodayDateStringIST(),
    };
    setExpenses((prev) => [expenseItem, ...prev]);
    setNewExpenseForm({
      amountRupees: '',
      category: 'Food & Drinks',
      note: '',
      paymentMode: 'UPI',
      date: getTodayDateStringIST(),
    });
    expenseDialogRef.current?.close();
    showToast(`Added ₹${rupees} expense`);
  };

  // Export JSON Backup
  const handleExportBackup = () => {
    const jsonStr = createBackupJson({
      profile,
      attendance,
      tasks,
      expenses,
      themeIndex
    });
    downloadBackupFile(jsonStr, `nextbell-backup-${profile.rollNo}-${getTodayDateStringIST()}.json`);
    showToast('Backup file downloaded! 📦');
  };

  // Import JSON Backup
  const handleFileImport = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      const res = parseBackupFile(event.target.result);
      if (res.success) {
        setProfile(res.data.profile);
        setThemeIndex(res.data.themeIndex);
        setAttendance(res.data.attendance);
        setTasks(res.data.tasks);
        setExpenses(res.data.expenses);
        if (res.data.dailyLimitPaise) setDailyLimitPaise(res.data.dailyLimitPaise);
        backupDialogRef.current?.close();
        showToast('Backup restored successfully! 🎉');
      } else {
        alert('Could not restore backup: ' + res.error);
      }
    };
    reader.readAsText(file);
    e.target.value = '';
  };

  // Reset to Demo Data
  const handleResetDemoData = () => {
    if (confirm('Reset all data to default demo state? This replaces your current local changes.')) {
      setProfile(DEFAULT_PROFILE);
      setThemeIndex(0);
      setAttendance(INITIAL_ATTENDANCE);
      setTasks(INITIAL_TASKS);
      setExpenses(INITIAL_EXPENSES);
      setDailyLimitPaise(50000);
      backupDialogRef.current?.close();
      showToast('Reset to fresh demo data');
    }
  };

  return (
    <div className="app-shell">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="live-toast" role="status" aria-live="polite">
          <span>🔔</span> {toastMessage}
        </div>
      )}

      {/* Sidebar Desktop */}
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">⌁</div>
          <div>
            <div className="brand-name">NextBell</div>
            <div className="brand-sub">NMIET · FE Div B</div>
          </div>
        </div>

        <nav className="nav-list" aria-label="Main Navigation">
          {[
            ['Today', '◷', 'Live Schedule'],
            ['Week', '▦', 'Full Timetable'],
            ['Attendance', '✓', 'Bunk Target'],
            ['Planner', '✦', 'Tasks & Exams'],
            ['Money', '₹', 'Student Expenses'],
            ['Guide', '⌖', 'Campus Rooms'],
            ['Settings', '⚙', 'Theme & Backup'],
          ].map(([tab, icon, desc]) => (
            <button
              key={tab}
              className={`nav-item ${activeTab === tab ? 'active' : ''}`}
              onClick={() => setActiveTab(tab)}
            >
              <span className="nav-icon">{icon}</span>
              <div className="nav-text">
                <span className="nav-label">{tab}</span>
                <span className="nav-desc">{desc}</span>
              </div>
            </button>
          ))}
        </nav>

        {/* Companion Android App Download Banner */}
        <a
          href="https://github.com/nealaringale/NextBell/actions"
          target="_blank"
          rel="noopener noreferrer"
          className="apk-link-banner"
          title="Download the native Android App for offline access, widgets & SMS tracking"
        >
          <span className="apk-icon">🤖</span>
          <div className="apk-text">
            <strong>NextBell for Android</strong>
            <span>v0.5.0 · Get APK</span>
          </div>
          <span className="apk-arrow">↗</span>
        </a>

        {/* Profile Card Widget */}
        <div className="profile-card" onClick={() => profileDialogRef.current?.showModal()}>
          <div className="avatar">{profile.name ? profile.name[0].toUpperCase() : 'N'}</div>
          <div className="profile-copy">
            <strong>{profile.name}</strong>
            <span>Roll {profile.rollNo} · Batch {studentBatch}</span>
          </div>
          <span className="profile-edit-badge" title="Edit Profile">✎</span>
        </div>

        {/* Theme Quick Switcher */}
        <div className="theme-bar">
          <div className="theme-dots">
            {THEMES.map((th, idx) => (
              <button
                key={th.id}
                className={`theme-dot ${themeIndex === idx ? 'selected' : ''}`}
                style={{ background: th.primary }}
                title={th.name}
                onClick={() => setThemeIndex(idx)}
              />
            ))}
          </div>
          <span className="theme-name">{THEMES[themeIndex]?.name}</span>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="main-content">
        {/* Top Header Bar */}
        <header className="topbar">
          <div>
            <div className="eyebrow">
              {profile.college} · {profile.year} · {profile.division} DIVISION
            </div>
            <h1>{activeTab === 'Today' ? selectedDay : activeTab}</h1>
          </div>

          <div className="topbar-right">
            {/* Live Clock IST */}
            <div className="clock-chip" title="Indian Standard Time">
              <span className="pulse-dot" />
              <span className="clock-time">{clockSeconds || 'Loading...'}</span>
            </div>

            {/* Quick Batch Pill */}
            <div
              className="class-pill"
              onClick={() => profileDialogRef.current?.showModal()}
              title="Click to change your Roll Number or Batch"
            >
              <b>Batch {studentBatch}</b>
              <span>({rollRangeForBatch(studentBatch)})</span>
            </div>
          </div>
        </header>

        {/* ===================== TAB 1: TODAY ===================== */}
        {activeTab === 'Today' && (
          <div className="tab-pane">
            {/* Hero Class Banner */}
            <section className="hero-grid">
              <div className={`hero-card ${heroIsCurrent ? 'ongoing' : 'upcoming'}`}>
                <div className="hero-header-row">
                  <div className="hero-label">
                    {heroIsCurrent ? '⚡ HAPPENING NOW' : '⏳ UP NEXT'}
                  </div>
                  {heroClass && (
                    <div className="hero-countdown">
                      <b>{heroMinutesLeft}</b> min {heroIsCurrent ? 'remaining' : 'away'}
                    </div>
                  )}
                </div>

                <div className="hero-title">
                  {heroClass ? heroClass.subject : 'No more classes today! 🎉'}
                </div>

                {heroClass ? (
                  <>
                    <div className="hero-meta">
                      <span>🕒 {formatTime(heroClass.start)} – {formatTime(heroClass.end)}</span>
                      <span>📍 {heroClass.room}</span>
                      {heroClass.teacher && <span>👨‍🏫 {heroClass.teacher}</span>}
                    </div>

                    <div className="hero-bottom">
                      <div className="type-badge-group">
                        <span className="type-chip">{heroClass.kind}</span>
                        {heroClass.batch && <span className="batch-chip">Batch {heroClass.batch}</span>}
                        {heroClass.code && <span className="code-chip">{heroClass.code}</span>}
                      </div>

                      {/* Quick Attendance Check-in on Hero Class */}
                      {heroClass.kind !== 'SHORT_BREAK' && heroClass.kind !== 'LUNCH' && (
                        <div className="quick-attendance-buttons">
                          <button
                            className="btn-attend-present"
                            onClick={() => handleMarkAttendance(heroClass.subject, 'PRESENT')}
                          >
                            ✓ Attended
                          </button>
                          <button
                            className="btn-attend-absent"
                            onClick={() => handleMarkAttendance(heroClass.subject, 'ABSENT')}
                          >
                            ✕ Missed
                          </button>
                        </div>
                      )}
                    </div>
                  </>
                ) : (
                  <div className="hero-meta">
                    You have finished all scheduled classes for {selectedDay}. Rest up or catch up in the library!
                  </div>
                )}
              </div>

              {/* Day Pulse Card */}
              <div className="stat-card">
                <div className="stat-top">
                  <span>DAY PULSE</span>
                  <span className="highlight-tag">{selectedDay}</span>
                </div>
                <div className="stat-number">{academicClasses.length}</div>
                <div className="stat-label">academic lectures & labs</div>

                <div className="stat-divider" />

                <div className="stat-row">
                  <span>Attendance Target</span>
                  <b>{attendanceStats.overallPct}% / {attendanceTarget}%</b>
                </div>

                <div className="stat-row">
                  <span>Spent Today</span>
                  <b>₹{(spentTodayPaise / 100).toFixed(0)}</b>
                </div>

                <div className="stat-row">
                  <span>Pending Tasks</span>
                  <b>{tasks.filter((t) => !t.completed).length} items</b>
                </div>
              </div>
            </section>

            {/* Schedule Section Heading & Day Selector */}
            <section className="section-heading">
              <div>
                <div className="eyebrow">YOUR TIMETABLE</div>
                <h2>Schedule for {selectedDay}</h2>
              </div>

              <div className="mini-tabs" role="tablist">
                {DAYS.map((day) => (
                  <button
                    key={day}
                    className={`day-tab ${selectedDay === day ? 'selected' : ''}`}
                    onClick={() => setSelectedDay(day)}
                  >
                    {day.slice(0, 3)}
                  </button>
                ))}
              </div>
            </section>

            {/* List of Classes */}
            <div className="class-list">
              {daySchedule.length === 0 ? (
                <div className="empty-state">No classes scheduled for {selectedDay}. Enjoy the free day!</div>
              ) : (
                daySchedule.map((item) => {
                  const sMin = parseMinutes(item.start);
                  const eMin = parseMinutes(item.end);
                  const isDone = eMin <= nowMinutes;
                  const isCurrent = sMin <= nowMinutes && nowMinutes < eMin;
                  const isBreak = item.kind === 'SHORT_BREAK' || item.kind === 'LUNCH';

                  return (
                    <article
                      key={item.id}
                      className={`class-row ${isCurrent ? 'current' : ''} ${isDone ? 'done' : ''} ${isBreak ? 'break-row' : ''}`}
                    >
                      <div className="time-col">
                        <strong>{formatTime(item.start)}</strong>
                        <span>{formatTime(item.end)}</span>
                      </div>

                      <div className="type-icon" title={item.kind}>
                        {isBreak ? '☕' : item.kind === 'PRACTICAL' ? '⚙' : item.kind === 'TUTORIAL' ? '✎' : item.kind === 'LIBRARY' ? '📖' : '🏛'}
                      </div>

                      <div className="class-main">
                        <div className="class-title-line">
                          <h3>{item.subject}</h3>
                          {isCurrent && <span className="status-badge now">NOW</span>}
                          {isDone && <span className="status-badge done">DONE</span>}
                          {item.batch && <span className="batch-badge">{item.batch}</span>}
                        </div>
                        <p>
                          {item.teacher ? `${item.teacher} · ` : ''}
                          {item.kind}
                          {item.code ? ` (${item.code})` : ''}
                        </p>
                      </div>

                      <div className="room-col">
                        <span>⌖ {item.room || 'Campus'}</span>
                        {!isBreak && (
                          <div className="row-mark-actions">
                            <button
                              className="btn-mark-mini present"
                              title="Mark Present"
                              onClick={() => handleMarkAttendance(item.subject, 'PRESENT')}
                            >
                              ✓
                            </button>
                            <button
                              className="btn-mark-mini absent"
                              title="Mark Absent"
                              onClick={() => handleMarkAttendance(item.subject, 'ABSENT')}
                            >
                              ✕
                            </button>
                          </div>
                        )}
                      </div>
                    </article>
                  );
                })
              )}
            </div>
          </div>
        )}

        {/* ===================== TAB 2: WEEK MATRIX ===================== */}
        {activeTab === 'Week' && (
          <div className="tab-pane">
            <div className="pane-header-actions">
              <p className="section-desc">
                Official FE Div B master schedule. Filter by student batch or tap any day to review.
              </p>
              <div className="filter-pill-group">
                {['ALL', 'B1', 'B2', 'B3'].map((b) => (
                  <button
                    key={b}
                    className={`filter-pill ${weekBatchFilter === b ? 'active' : ''}`}
                    onClick={() => setWeekBatchFilter(b)}
                  >
                    {b === 'ALL' ? 'All Classes' : `Batch ${b}`}
                  </button>
                ))}
              </div>
            </div>

            <div className="week-grid">
              {DAYS.map((day) => {
                const dayEntries = (RAW_TIMETABLE[day] || []).filter((item) => {
                  if (weekBatchFilter === 'ALL') return true;
                  if (item.audience === 'COMMON') return true;
                  return item.audience === weekBatchFilter;
                });

                return (
                  <div key={day} className="week-day-column">
                    <div className="week-day-header">
                      <h3>{day}</h3>
                      <span className="count-pill">{dayEntries.filter((e) => e.kind !== 'SHORT_BREAK' && e.kind !== 'LUNCH').length} classes</span>
                    </div>

                    <div className="week-day-items">
                      {dayEntries.map((item) => (
                        <div
                          key={item.id}
                          className={`week-class-card ${item.kind === 'SHORT_BREAK' || item.kind === 'LUNCH' ? 'break' : ''}`}
                        >
                          <div className="week-time">{formatTime(item.start)} - {formatTime(item.end)}</div>
                          <div className="week-subject">{item.subject}</div>
                          <div className="week-room">📍 {item.room} {item.batch ? `(${item.batch})` : ''}</div>
                        </div>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* ===================== TAB 3: ATTENDANCE & BUNK INTELLIGENCE ===================== */}
        {activeTab === 'Attendance' && (
          <div className="tab-pane">
            {/* Bunk Alert Banner */}
            <div className={`bunk-banner ${attendanceStats.overallPct >= attendanceTarget ? 'safe' : 'danger'}`}>
              <div className="bunk-banner-icon">
                {attendanceStats.overallPct >= attendanceTarget ? '🛡️' : '⚠️'}
              </div>
              <div className="bunk-banner-content">
                <h3>
                  {attendanceStats.overallPct >= attendanceTarget
                    ? `Safe to bunk ${attendanceStats.safeBunks} more classes!`
                    : `Attendance warning: ${attendanceStats.overallPct}% is below ${attendanceTarget}% target!`}
                </h3>
                <p>
                  {attendanceStats.overallPct >= attendanceTarget
                    ? `You have attended ${attendanceStats.totalAttended} of ${attendanceStats.totalHeld} classes. You can miss ${attendanceStats.safeBunks} lectures and still maintain ≥ ${attendanceTarget}%.`
                    : `You need to attend ${attendanceStats.neededClasses} consecutive classes without missing to climb back to ${attendanceTarget}%.`}
                </p>
              </div>

              <div className="bunk-target-slider">
                <label>Target: <b>{attendanceTarget}%</b></label>
                <input
                  type="range"
                  min="60"
                  max="90"
                  step="5"
                  value={attendanceTarget}
                  onChange={(e) => setAttendanceTarget(Number(e.target.value))}
                />
              </div>
            </div>

            {/* Attendance Overview Cards */}
            <div className="attendance-grid">
              {Object.keys(SUBJECTS_MAP).map((code) => {
                const sub = SUBJECTS_MAP[code];
                const stats = attendanceStats.subjectMap[sub.name] || { held: 0, attended: 0, missed: 0 };
                const pct = stats.held === 0 ? 100 : Math.round((stats.attended * 100) / stats.held);

                return (
                  <div key={code} className="attendance-card">
                    <div className="att-card-header">
                      <div>
                        <span className="code-tag" style={{ color: sub.color }}>{sub.short}</span>
                        <h4>{sub.name}</h4>
                      </div>
                      <div className={`pct-bubble ${pct >= attendanceTarget ? 'safe' : 'warn'}`}>
                        {pct}%
                      </div>
                    </div>

                    <p className="att-teacher">👨‍🏫 {sub.teacher}</p>

                    <div className="progress-track">
                      <div
                        className="progress-fill"
                        style={{ width: `${pct}%`, background: pct >= attendanceTarget ? '#10b981' : '#f43f5e' }}
                      />
                    </div>

                    <div className="att-meta-row">
                      <span>Attended: <b>{stats.attended}</b></span>
                      <span>Held: <b>{stats.held}</b></span>
                      <span>Missed: <b>{stats.missed}</b></span>
                    </div>

                    <div className="att-actions">
                      <button
                        className="btn-att-action present"
                        onClick={() => handleMarkAttendance(sub.name, 'PRESENT')}
                      >
                        + Present
                      </button>
                      <button
                        className="btn-att-action absent"
                        onClick={() => handleMarkAttendance(sub.name, 'ABSENT')}
                      >
                        + Absent
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Attendance History Log */}
            <div className="history-section">
              <div className="history-header">
                <h3>Attendance Ledger ({attendance.length} records)</h3>
                <button
                  className="btn-ghost-danger"
                  onClick={() => {
                    if (confirm('Clear attendance history?')) setAttendance([]);
                  }}
                >
                  Clear History
                </button>
              </div>

              <div className="history-list">
                {attendance.length === 0 ? (
                  <p className="empty-text">No attendance records yet. Tap + Present or + Absent above to begin tracking.</p>
                ) : (
                  attendance.slice(0, 10).map((item, idx) => (
                    <div key={item.key || idx} className="history-item">
                      <span className={`status-dot ${item.status.toLowerCase()}`} />
                      <div className="history-title">
                        <b>{item.subject}</b>
                        <span>{item.date}</span>
                      </div>
                      <span className={`history-status-tag ${item.status.toLowerCase()}`}>
                        {item.status}
                      </span>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        )}

        {/* ===================== TAB 4: STUDENT PLANNER ===================== */}
        {activeTab === 'Planner' && (
          <div className="tab-pane">
            <div className="pane-header-actions">
              <div className="filter-pill-group">
                {['ALL', 'ASSIGNMENT', 'EXAM', 'TASK', 'COMPLETED'].map((f) => (
                  <button
                    key={f}
                    className={`filter-pill ${taskFilter === f ? 'active' : ''}`}
                    onClick={() => setTaskFilter(f)}
                  >
                    {f}
                  </button>
                ))}
              </div>

              <button
                className="btn-primary"
                onClick={() => taskDialogRef.current?.showModal()}
              >
                + New Item
              </button>
            </div>

            <div className="task-list">
              {tasks.filter((t) => {
                if (taskFilter === 'ALL') return true;
                if (taskFilter === 'COMPLETED') return t.completed;
                return t.type === taskFilter;
              }).length === 0 ? (
                <div className="empty-state">No items found in this view. Enjoy the calm or add a new assignment!</div>
              ) : (
                tasks
                  .filter((t) => {
                    if (taskFilter === 'ALL') return true;
                    if (taskFilter === 'COMPLETED') return t.completed;
                    return t.type === taskFilter;
                  })
                  .map((task) => (
                    <div key={task.id} className={`task-card ${task.completed ? 'completed' : ''}`}>
                      <input
                        type="checkbox"
                        checked={task.completed}
                        onChange={() => handleToggleTask(task.id)}
                        className="task-checkbox"
                        aria-label={`Mark ${task.title} as completed`}
                      />

                      <div className="task-content">
                        <div className="task-topline">
                          <span className={`task-tag ${task.type.toLowerCase()}`}>{task.type}</span>
                          <span className="task-course">{task.course}</span>
                          {task.dueDate && <span className="task-due">📅 Due {task.dueDate}</span>}
                          {task.priority && <span className={`priority-tag ${task.priority.toLowerCase()}`}>{task.priority}</span>}
                        </div>
                        <h4 className="task-title">{task.title}</h4>
                      </div>

                      <button
                        className="btn-delete-task"
                        onClick={() => handleDeleteTask(task.id)}
                        title="Delete task"
                      >
                        🗑
                      </button>
                    </div>
                  ))
              )}
            </div>
          </div>
        )}

        {/* ===================== TAB 5: MONEY & EXPENSE TRACKER ===================== */}
        {activeTab === 'Money' && (
          <div className="tab-pane">
            {/* Money Stats Grid */}
            <div className="hero-grid">
              <div className="hero-card">
                <div className="hero-header-row">
                  <div className="hero-label">THIS MONTH'S SPENDING</div>
                  <span className="code-chip">₹ Daily Limit: {(dailyLimitPaise / 100).toFixed(0)}</span>
                </div>
                <div className="hero-title" style={{ fontSize: '38px' }}>
                  ₹{(spentThisMonthPaise / 100).toFixed(0)}
                </div>
                <div className="hero-meta">
                  Spent today: <b>₹{(spentTodayPaise / 100).toFixed(0)}</b> · Remaining today: <b>₹{Math.max(0, dailyLimitPaise - spentTodayPaise) / 100}</b>
                </div>

                <div className="hero-bottom">
                  <button
                    className="btn-primary"
                    onClick={() => expenseDialogRef.current?.showModal()}
                  >
                    + Record Expense
                  </button>
                  <button
                    className="btn-secondary"
                    onClick={() => {
                      const newLim = prompt('Enter new daily spending limit (₹):', String(dailyLimitPaise / 100));
                      if (newLim && !isNaN(newLim)) setDailyLimitPaise(Math.round(Number(newLim) * 100));
                    }}
                  >
                    Set Daily Limit
                  </button>
                </div>
              </div>

              {/* Category Breakdown Card */}
              <div className="stat-card">
                <div className="stat-top">
                  <span>TOP SPENDING</span>
                  <span className="highlight-tag">This Month</span>
                </div>

                <div className="category-bars">
                  {categorySpending.slice(0, 4).map(([cat, paise]) => {
                    const catObj = EXPENSE_CATEGORIES.find((c) => c.id === cat);
                    return (
                      <div key={cat} className="cat-bar-item">
                        <div className="cat-bar-label">
                          <span>{catObj?.icon || '📦'} {cat}</span>
                          <b>₹{(paise / 100).toFixed(0)}</b>
                        </div>
                        <div className="cat-track">
                          <div
                            className="cat-fill"
                            style={{
                              width: `${Math.min(100, (paise / Math.max(1, spentThisMonthPaise)) * 100)}%`,
                              background: catObj?.color || '#3b82f6'
                            }}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* Recent Transactions List */}
            <div className="history-section">
              <div className="history-header">
                <h3>Transactions ({expenses.length})</h3>
                <button
                  className="btn-primary"
                  onClick={() => expenseDialogRef.current?.showModal()}
                >
                  + Add Expense
                </button>
              </div>

              <div className="transaction-list">
                {expenses.length === 0 ? (
                  <p className="empty-text">No expenses logged yet. Tap + Record Expense to keep track of spending.</p>
                ) : (
                  expenses.map((exp) => {
                    const cat = EXPENSE_CATEGORIES.find((c) => c.id === exp.category);
                    return (
                      <div key={exp.id} className="transaction-row">
                        <div className="trans-icon" style={{ background: `${cat?.color || '#3b82f6'}22`, color: cat?.color || '#3b82f6' }}>
                          {cat?.icon || '₹'}
                        </div>
                        <div className="trans-info">
                          <h4>{exp.note}</h4>
                          <p>{exp.category} · {exp.paymentMode} · {exp.date}</p>
                        </div>
                        <div className="trans-amount">
                          ₹{(exp.amountPaise / 100).toFixed(0)}
                        </div>
                        <button
                          className="btn-delete-trans"
                          onClick={() => handleDeleteExpense(exp.id)}
                          title="Delete expense"
                        >
                          ✕
                        </button>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          </div>
        )}

        {/* ===================== TAB 6: CAMPUS GUIDE ===================== */}
        {activeTab === 'Guide' && (
          <div className="tab-pane">
            <p className="section-desc">
              Classrooms, drawing halls, and laboratories for FE Division B at NMIET campus.
            </p>

            <div className="guide-grid">
              {CAMPUS_LOCATIONS.map((loc) => (
                <div key={loc.room} className="guide-card">
                  <div className="guide-room-badge">{loc.room}</div>
                  <div className="guide-type">{loc.type}</div>
                  <p className="guide-desc">{loc.desc}</p>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* ===================== TAB 7: SETTINGS & BACKUP ===================== */}
        {activeTab === 'Settings' && (
          <div className="tab-pane">
            <div className="settings-section">
              <h3>Student Profile</h3>
              <div className="settings-card">
                <div className="settings-row">
                  <div>
                    <strong>{profile.name}</strong>
                    <p>{profile.college} · {profile.year} · Division {profile.division}</p>
                  </div>
                  <button
                    className="btn-primary"
                    onClick={() => profileDialogRef.current?.showModal()}
                  >
                    Edit Profile
                  </button>
                </div>
                <div className="settings-detail-chips">
                  <span>Roll No: <b>{profile.rollNo}</b></span>
                  <span>Calculated Batch: <b>{studentBatch}</b></span>
                  <span>Tutorial Wing: <b>{profile.rollNo <= 35 ? 'Roll 1–35 (Room 205)' : 'Roll 36+ (Room 201)'}</b></span>
                </div>
              </div>
            </div>

            <div className="settings-section">
              <h3>Cloud Sync · Android + Website</h3>
              <div className="settings-card cloud-sync-card">
                <div className="settings-row">
                  <div>
                    <strong>{cloudUser ? cloudUser.email : 'One NextBell account everywhere'}</strong>
                    <p>{cloudStatus}</p>
                  </div>
                  {cloudUser && (
                    <button className="btn-secondary" onClick={handleCloudSignOut} disabled={cloudBusy}>
                      Sign out
                    </button>
                  )}
                </div>

                {!cloudUser && (
                  <>
                    {!isFirebaseConfigured() && (
                      <p className="cloud-sync-warning">
                        Firebase is not configured yet. Add the Web app values to
                        <code> src/firebaseConfig.js </code> and enable Email/Password in Firebase Authentication.
                      </p>
                    )}
                    <div className="form-row cloud-auth-fields">
                      <label className="form-group">
                        <span>Email</span>
                        <input
                          type="email"
                          autoComplete="email"
                          placeholder="you@example.com"
                          value={cloudEmail}
                          onChange={(event) => setCloudEmail(event.target.value)}
                        />
                      </label>
                      <label className="form-group">
                        <span>Password</span>
                        <input
                          type="password"
                          autoComplete="current-password"
                          placeholder="At least 6 characters"
                          value={cloudPassword}
                          onChange={(event) => setCloudPassword(event.target.value)}
                        />
                      </label>
                    </div>
                    <div className="backup-actions">
                      <button className="btn-primary" disabled={cloudBusy || !isFirebaseConfigured()}
                        onClick={() => handleCloudSignIn(false)}>
                        {cloudBusy ? 'Connecting…' : 'Sign in'}
                      </button>
                      <button className="btn-secondary" disabled={cloudBusy || !isFirebaseConfigured()}
                        onClick={() => handleCloudSignIn(true)}>
                        Create account
                      </button>
                    </div>
                  </>
                )}
                <p className="cloud-sync-footnote">
                  Use the same email and password in NextBell Android. Expenses, attendance,
                  planner tasks and profile/settings sync automatically; offline edits queue until
                  the device reconnects.
                </p>
              </div>
            </div>

            <div className="settings-section">
              <h3>Theme System (5 Styles)</h3>
              <div className="theme-grid">
                {THEMES.map((th, idx) => (
                  <div
                    key={th.id}
                    className={`theme-card ${themeIndex === idx ? 'active' : ''}`}
                    onClick={() => setThemeIndex(idx)}
                  >
                    <div className="theme-color-preview" style={{ background: th.primary }} />
                    <div className="theme-meta">
                      <strong>{th.name}</strong>
                      <span>{th.tag}</span>
                    </div>
                    {themeIndex === idx && <span className="theme-check">✓ Active</span>}
                  </div>
                ))}
              </div>
            </div>

            <div className="settings-section">
              <h3>Backup & Restore (Compatible with NextBell Android App)</h3>
              <div className="settings-card">
                <p>
                  Export your complete attendance, planner tasks, expenses, and profile as a clean JSON file.
                  You can restore this file on your NextBell Android app or on any other browser anytime.
                </p>

                <div className="backup-actions">
                  <button className="btn-primary" onClick={handleExportBackup}>
                    📥 Export JSON Backup
                  </button>

                  <button className="btn-secondary" onClick={() => fileInputRef.current?.click()}>
                    📤 Restore from File
                  </button>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept=".json,application/json"
                    style={{ display: 'none' }}
                    onChange={handleFileImport}
                  />

                  <button className="btn-ghost-danger" onClick={handleResetDemoData}>
                    ↺ Reset to Demo Data
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Global Footer */}
        <footer>
          <span>NextBell v0.5.0 · College Day, Under Control</span>
          <span>FE Div B · W.E.F. 16 Sep 2026 Revision 00</span>
        </footer>
      </main>

      {/* ===================== MODAL 1: EDIT PROFILE ===================== */}
      {/* Modern Web Guidance: closedby="any" with accessible name */}
      <dialog
        ref={profileDialogRef}
        className="modal-dialog"
        closedby="any"
        aria-labelledby="profile-dialog-title"
      >
        <div className="dialog-content">
          <div className="dialog-header">
            <h2 id="profile-dialog-title">Edit Student Profile</h2>
            <button
              type="button"
              className="btn-close-dialog"
              onClick={() => profileDialogRef.current?.close()}
              aria-label="Close dialog"
            >
              ✕
            </button>
          </div>

          <form onSubmit={handleSaveProfile} className="dialog-form">
            <label className="form-group">
              <span>Full Name</span>
              <input
                type="text"
                value={editProfileForm.name}
                onChange={(e) => setEditProfileForm({ ...editProfileForm, name: e.target.value })}
                required
              />
            </label>

            <div className="form-row">
              <label className="form-group">
                <span>Roll Number</span>
                <input
                  type="number"
                  min="1"
                  max="120"
                  value={editProfileForm.rollNo}
                  onChange={(e) => setEditProfileForm({ ...editProfileForm, rollNo: e.target.value })}
                  required
                />
              </label>

              <label className="form-group">
                <span>Auto-Batch</span>
                <input
                  type="text"
                  value={batchForRoll(editProfileForm.rollNo)}
                  disabled
                />
              </label>
            </div>

            <label className="form-group">
              <span>College Name</span>
              <input
                type="text"
                value={editProfileForm.college}
                onChange={(e) => setEditProfileForm({ ...editProfileForm, college: e.target.value })}
              />
            </label>

            <div className="dialog-actions">
              <button
                type="button"
                className="btn-secondary"
                onClick={() => profileDialogRef.current?.close()}
              >
                Cancel
              </button>
              <button type="submit" className="btn-primary">
                Save Profile
              </button>
            </div>
          </form>
        </div>
      </dialog>

      {/* ===================== MODAL 2: ADD TASK ===================== */}
      <dialog
        ref={taskDialogRef}
        className="modal-dialog"
        closedby="any"
        aria-labelledby="task-dialog-title"
      >
        <div className="dialog-content">
          <div className="dialog-header">
            <h2 id="task-dialog-title">Add Study Task / Deadline</h2>
            <button
              type="button"
              className="btn-close-dialog"
              onClick={() => taskDialogRef.current?.close()}
              aria-label="Close dialog"
            >
              ✕
            </button>
          </div>

          <form onSubmit={handleCreateTask} className="dialog-form">
            <label className="form-group">
              <span>Task Title</span>
              <input
                type="text"
                placeholder="e.g. LAM Assignment 3 Submission"
                value={newTaskForm.title}
                onChange={(e) => setNewTaskForm({ ...newTaskForm, title: e.target.value })}
                required
              />
            </label>

            <div className="form-row">
              <label className="form-group">
                <span>Course</span>
                <select
                  value={newTaskForm.course}
                  onChange={(e) => setNewTaskForm({ ...newTaskForm, course: e.target.value })}
                >
                  {Object.keys(SUBJECTS_MAP).map((code) => (
                    <option key={code} value={SUBJECTS_MAP[code].short}>
                      {SUBJECTS_MAP[code].short} — {SUBJECTS_MAP[code].name}
                    </option>
                  ))}
                </select>
              </label>

              <label className="form-group">
                <span>Type</span>
                <select
                  value={newTaskForm.type}
                  onChange={(e) => setNewTaskForm({ ...newTaskForm, type: e.target.value })}
                >
                  <option value="ASSIGNMENT">Assignment</option>
                  <option value="EXAM">Exam / Unit Test</option>
                  <option value="TASK">Study Task</option>
                </select>
              </label>
            </div>

            <div className="form-row">
              <label className="form-group">
                <span>Due Date</span>
                <input
                  type="date"
                  value={newTaskForm.dueDate}
                  onChange={(e) => setNewTaskForm({ ...newTaskForm, dueDate: e.target.value })}
                />
              </label>

              <label className="form-group">
                <span>Priority</span>
                <select
                  value={newTaskForm.priority}
                  onChange={(e) => setNewTaskForm({ ...newTaskForm, priority: e.target.value })}
                >
                  <option value="HIGH">High</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="LOW">Low</option>
                </select>
              </label>
            </div>

            <div className="dialog-actions">
              <button
                type="button"
                className="btn-secondary"
                onClick={() => taskDialogRef.current?.close()}
              >
                Cancel
              </button>
              <button type="submit" className="btn-primary">
                Add to Planner
              </button>
            </div>
          </form>
        </div>
      </dialog>

      {/* ===================== MODAL 3: ADD EXPENSE ===================== */}
      <dialog
        ref={expenseDialogRef}
        className="modal-dialog"
        closedby="any"
        aria-labelledby="expense-dialog-title"
      >
        <div className="dialog-content">
          <div className="dialog-header">
            <h2 id="expense-dialog-title">Record Student Expense</h2>
            <button
              type="button"
              className="btn-close-dialog"
              onClick={() => expenseDialogRef.current?.close()}
              aria-label="Close dialog"
            >
              ✕
            </button>
          </div>

          <form onSubmit={handleCreateExpense} className="dialog-form">
            <label className="form-group">
              <span>Amount (₹)</span>
              <input
                type="number"
                step="1"
                min="1"
                placeholder="0"
                value={newExpenseForm.amountRupees}
                onChange={(e) => setNewExpenseForm({ ...newExpenseForm, amountRupees: e.target.value })}
                required
                autoFocus
              />
            </label>

            {/* Quick Amount Chips */}
            <div className="amount-chips">
              {[20, 50, 100, 200, 500].map((amt) => (
                <button
                  key={amt}
                  type="button"
                  className="amt-chip"
                  onClick={() => setNewExpenseForm({ ...newExpenseForm, amountRupees: String(amt) })}
                >
                  ₹{amt}
                </button>
              ))}
            </div>

            <div className="form-row">
              <label className="form-group">
                <span>Category</span>
                <select
                  value={newExpenseForm.category}
                  onChange={(e) => setNewExpenseForm({ ...newExpenseForm, category: e.target.value })}
                >
                  {EXPENSE_CATEGORIES.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.icon} {c.id}
                    </option>
                  ))}
                </select>
              </label>

              <label className="form-group">
                <span>Payment Mode</span>
                <select
                  value={newExpenseForm.paymentMode}
                  onChange={(e) => setNewExpenseForm({ ...newExpenseForm, paymentMode: e.target.value })}
                >
                  <option value="UPI">UPI / GPay</option>
                  <option value="Cash">Cash</option>
                  <option value="Card">Card</option>
                  <option value="Bank Transfer">Bank Transfer</option>
                  <option value="Other">Other</option>
                </select>
              </label>
            </div>

            <label className="form-group">
              <span>Description / Note</span>
              <input
                type="text"
                placeholder="e.g. Canteen lunch, auto fare, Xerox copies"
                value={newExpenseForm.note}
                onChange={(e) => setNewExpenseForm({ ...newExpenseForm, note: e.target.value })}
              />
            </label>

            <div className="dialog-actions">
              <button
                type="button"
                className="btn-secondary"
                onClick={() => expenseDialogRef.current?.close()}
              >
                Cancel
              </button>
              <button type="submit" className="btn-primary">
                Add Expense
              </button>
            </div>
          </form>
        </div>
      </dialog>
    </div>
  );
}
