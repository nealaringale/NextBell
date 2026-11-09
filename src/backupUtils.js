// NextBell Backup & Restore — 100% compatible with Android LocalDataBackup

export const BACKUP_FORMAT = 'com.nealaringale.nextbell.backup';
export const SCHEMA_VERSION = 1;

export function createBackupJson({ profile, attendance, tasks, expenses, themeIndex }) {
  const preferences = {
    name: profile.name || 'Student',
    roll_number: Number(profile.rollNo) || 34,
    theme: Number(themeIndex) || 0,
    attendance_target_percent: 75,
    attendance_sessions_v1: JSON.stringify(attendance || []),
    student_planner_items_v1: JSON.stringify(tasks || []),
    expenses_v1: JSON.stringify(expenses || []),
    expense_daily_limit: 50000, // 500 Rs in paise
  };

  const backupDoc = {
    format: BACKUP_FORMAT,
    schemaVersion: SCHEMA_VERSION,
    createdAt: new Date().toISOString(),
    appVersion: '0.5.0-web',
    preferences,
  };

  return JSON.stringify(backupDoc, null, 2);
}

export function downloadBackupFile(backupString, fileName = 'nextbell-backup.json') {
  const blob = new Blob([backupString], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export function parseBackupFile(jsonString) {
  try {
    const doc = JSON.parse(jsonString);
    if (doc.format !== BACKUP_FORMAT && !doc.preferences) {
      throw new Error('Not a recognized NextBell backup format.');
    }

    const prefs = doc.preferences || doc;
    
    // Parse Profile
    const profile = {
      name: prefs.name || 'Student',
      rollNo: Number(prefs.roll_number) || 34,
      division: 'B',
      college: 'NMIET',
      year: 'FE 2026–27',
    };

    // Parse Theme
    const themeIndex = Number(prefs.theme) || 0;

    // Parse Attendance
    let attendance = [];
    if (prefs.attendance_sessions_v1) {
      attendance = typeof prefs.attendance_sessions_v1 === 'string'
        ? JSON.parse(prefs.attendance_sessions_v1)
        : prefs.attendance_sessions_v1;
    }

    // Parse Tasks
    let tasks = [];
    if (prefs.student_planner_items_v1) {
      tasks = typeof prefs.student_planner_items_v1 === 'string'
        ? JSON.parse(prefs.student_planner_items_v1)
        : prefs.student_planner_items_v1;
    }

    // Parse Expenses
    let expenses = [];
    if (prefs.expenses_v1) {
      expenses = typeof prefs.expenses_v1 === 'string'
        ? JSON.parse(prefs.expenses_v1)
        : prefs.expenses_v1;
    }

    return {
      success: true,
      data: {
        profile,
        themeIndex,
        attendance,
        tasks,
        expenses,
        dailyLimitPaise: Number(prefs.expense_daily_limit) || 50000,
      }
    };
  } catch (err) {
    return {
      success: false,
      error: err.message || 'Invalid backup JSON file'
    };
  }
}
