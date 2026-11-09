// Official FE Division B Timetable — W.E.F. 16/09/2026, Revision 00
// Source: com.nealaringale.nextbell.TimetableData

export const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'];

export const SUBJECTS_MAP = {
  FPL: { name: 'Fundamental of Programming Languages', short: 'FPL', teacher: 'Prof. Sanket Sontakke', color: '#10b981' },
  EG: { name: 'Engineering Graphics', short: 'EG', teacher: 'Prof. Santosh Dabhole', color: '#f59e0b' },
  CS: { name: 'Communication Skills in Corporate World', short: 'CS', teacher: 'Prof. Amol Bade', color: '#8b5cf6' },
  ELE: { name: 'Electrical Engineering', short: 'ELE', teacher: 'Prof. Arti S. Bindu', color: '#3b82f6' },
  LAM: { name: 'Linear Algebra & Multivariable Calculus', short: 'LAM', teacher: 'Prof. Vrushali Gujar', color: '#ec4899' },
  PHY: { name: 'Engineering Physics', short: 'PHY', teacher: 'Prof. Rupali Jagnade', color: '#06b6d4' },
  DTI: { name: 'Design Thinking and Innovation', short: 'DTI', teacher: 'Prof. Mujiaid Shaikh', color: '#14b8a6' },
  LIBRARY: { name: 'Library Reading & Research', short: 'Library', teacher: 'Librarian', color: '#6366f1' },
  LS: { name: 'Life Skills (LS-1 / LS-2)', short: 'LS', teacher: 'Faculty Coordinator', color: '#eab308' },
  SELF: { name: 'Self Learning Session', short: 'Self Learning', teacher: 'Self Study', color: '#64748b' },
};

export const RAW_TIMETABLE = {
  Monday: [
    { id: 'mon-b1', start: '08:20', end: '10:20', subject: 'Library', code: 'LIBRARY', teacher: 'Faculty', room: 'Library B1', batch: 'B1', audience: 'B1', kind: 'LIBRARY' },
    { id: 'mon-b2', start: '08:20', end: '10:20', subject: 'Fundamental of Programming Languages', code: 'FPL', teacher: 'Prof. Sanket Sontakke', room: '109 · Wing B', batch: 'B2', audience: 'B2', kind: 'PRACTICAL' },
    { id: 'mon-b3', start: '08:20', end: '10:20', subject: 'Engineering Graphics', code: 'EG', teacher: 'Prof. Santosh Dabhole', room: 'Wing B · 312', batch: 'B3', audience: 'B3', kind: 'PRACTICAL' },
    { id: 'mon-break', start: '10:20', end: '10:40', subject: 'Short Break', code: 'BREAK', teacher: '', room: 'Campus', batch: '', audience: 'COMMON', kind: 'SHORT_BREAK' },
    { id: 'mon-cs', start: '10:40', end: '12:40', subject: 'Communication Skills in Corporate World', code: 'CS', teacher: 'Prof. Amol Bade', room: 'Room 201', batch: '', audience: 'COMMON', kind: 'CLASS' },
    { id: 'mon-lunch', start: '12:40', end: '13:30', subject: 'Lunch Break', code: 'LUNCH', teacher: '', room: 'Cafeteria', batch: '', audience: 'COMMON', kind: 'LUNCH' },
    { id: 'mon-ele', start: '13:30', end: '14:30', subject: 'Electrical Engineering', code: 'ELE', teacher: 'Prof. Arti S. Bindu', room: 'Room 201', batch: '', audience: 'COMMON', kind: 'CLASS' },
    { id: 'mon-lam-36', start: '14:30', end: '15:30', subject: 'Linear Algebra & Multivariable Calculus Tutorial', code: 'LAM', teacher: 'Prof. Vrushali Gujar', room: 'Room 201', batch: 'Roll 36+', audience: 'ROLL_36_PLUS', kind: 'TUTORIAL' },
  ],
  Tuesday: [
    { id: 'tue-fpl', start: '08:20', end: '09:20', subject: 'Fundamental of Programming Languages', code: 'FPL', teacher: 'Prof. Sanket Sontakke', room: 'Room 201', batch: '', audience: 'COMMON', kind: 'CLASS' },
    { id: 'tue-eg', start: '09:20', end: '10:20', subject: 'Engineering Graphics', code: 'EG', teacher: 'Prof. Santosh Dabhole', room: 'Room 201', batch: '', audience: 'COMMON', kind: 'CLASS' },
    { id: 'tue-break', start: '10:20', end: '10:40', subject: 'Short Break', code: 'BREAK', teacher: '', room: 'Campus', batch: '', audience: 'COMMON', kind: 'SHORT_BREAK' },
    { id: 'tue-b1', start: '10:40', end: '12:40', subject: 'Fundamental of Programming Languages', code: 'FPL', teacher: 'Prof. Sanket Sontakke', room: '109 · Wing B', batch: 'B1', audience: 'B1', kind: 'PRACTICAL' },
    { id: 'tue-b2', start: '10:40', end: '12:40', subject: 'Engineering Physics', code: 'PHY', teacher: 'Prof. Rupali Jagnade', room: '115 · Wing B', batch: 'B2', audience: 'B2', kind: 'PRACTICAL' },
    { id: 'tue-b3', start: '10:40', end: '12:40', subject: 'Library', code: 'LIBRARY', teacher: 'Faculty', room: 'Library B3', batch: 'B3', audience: 'B3', kind: 'LIBRARY' },
    { id: 'tue-lunch', start: '12:40', end: '13:30', subject: 'Lunch Break', code: 'LUNCH', teacher: '', room: 'Cafeteria', batch: '', audience: 'COMMON', kind: 'LUNCH' },
    { id: 'tue-ls', start: '13:30', end: '15:30', subject: 'Life Skills (LS-1 / LS-2)', code: 'LS', teacher: 'Faculty Coordinator', room: 'Seminar Hall', batch: '', audience: 'COMMON', kind: 'SPECIAL' },
  ],
  Wednesday: [
    { id: 'wed-phy', start: '08:20', end: '09:20', subject: 'Engineering Physics', code: 'PHY', teacher: 'Prof. Rupali Jagnade', room: 'Room 202', batch: '', audience: 'COMMON', kind: 'CLASS' },
    { id: 'wed-eg', start: '09:20', end: '10:20', subject: 'Engineering Graphics', code: 'EG', teacher: 'Prof. Santosh Dabhole', room: 'Room 202', batch: '', audience: 'COMMON', kind: 'CLASS' },
    { id: 'wed-break', start: '10:20', end: '10:40', subject: 'Short Break', code: 'BREAK', teacher: '', room: 'Campus', batch: '', audience: 'COMMON', kind: 'SHORT_BREAK' },
    { id: 'wed-ele', start: '10:40', end: '11:40', subject: 'Electrical Engineering', code: 'ELE', teacher: 'Prof. Arti S. Bindu', room: 'Room 202', batch: '', audience: 'COMMON', kind: 'CLASS' },
    { id: 'wed-lam', start: '11:40', end: '12:40', subject: 'Linear Algebra & Multivariable Calculus', code: 'LAM', teacher: 'Prof. Vrushali Gujar', room: 'Room 202', batch: '', audience: 'COMMON', kind: 'CLASS' },
    { id: 'wed-lunch', start: '12:40', end: '13:30', subject: 'Lunch Break', code: 'LUNCH', teacher: '', room: 'Cafeteria', batch: '', audience: 'COMMON', kind: 'LUNCH' },
    { id: 'wed-lam-35', start: '13:30', end: '14:30', subject: 'Linear Algebra & Multivariable Calculus Tutorial', code: 'LAM', teacher: 'Prof. Vrushali Gujar', room: 'Room 205', batch: 'Roll 1–35', audience: 'ROLL_1_35', kind: 'TUTORIAL' },
    { id: 'wed-self', start: '14:30', end: '15:30', subject: 'Self Learning Session', code: 'SELF', teacher: 'Mentors', room: 'Room 202', batch: '', audience: 'COMMON', kind: 'SELF_LEARNING' },
  ],
  Thursday: [
    { id: 'thu-b1', start: '08:20', end: '10:20', subject: 'Engineering Graphics', code: 'EG', teacher: 'Prof. Santosh Dabhole', room: '312 · Wing B', batch: 'B1', audience: 'B1', kind: 'PRACTICAL' },
    { id: 'thu-b2', start: '08:20', end: '10:20', subject: 'Electrical Engineering', code: 'ELE', teacher: 'Prof. Arti S. Bindu', room: '101 · Wing C', batch: 'B2', audience: 'B2', kind: 'PRACTICAL' },
    { id: 'thu-b3', start: '08:20', end: '10:20', subject: 'Engineering Physics', code: 'PHY', teacher: 'Prof. Rupali Jagnade', room: '115 · Wing B', batch: 'B3', audience: 'B3', kind: 'PRACTICAL' },
    { id: 'thu-break', start: '10:20', end: '10:40', subject: 'Short Break', code: 'BREAK', teacher: '', room: 'Campus', batch: '', audience: 'COMMON', kind: 'SHORT_BREAK' },
    { id: 'thu-lam', start: '10:40', end: '11:40', subject: 'Linear Algebra & Multivariable Calculus', code: 'LAM', teacher: 'Prof. Vrushali Gujar', room: 'Room 202', batch: '', audience: 'COMMON', kind: 'CLASS' },
    { id: 'thu-fpl', start: '11:40', end: '12:40', subject: 'Fundamental of Programming Languages', code: 'FPL', teacher: 'Prof. Sanket Sontakke', room: 'Room 202', batch: '', audience: 'COMMON', kind: 'CLASS' },
    { id: 'thu-lunch', start: '12:40', end: '13:30', subject: 'Lunch Break', code: 'LUNCH', teacher: '', room: 'Cafeteria', batch: '', audience: 'COMMON', kind: 'LUNCH' },
    { id: 'thu-pm-b1', start: '13:30', end: '15:30', subject: 'Engineering Physics', code: 'PHY', teacher: 'Prof. Rupali Jagnade', room: '115 · Wing B', batch: 'B1', audience: 'B1', kind: 'PRACTICAL' },
    { id: 'thu-pm-b2', start: '13:30', end: '15:30', subject: 'Engineering Graphics', code: 'EG', teacher: 'Prof. Santosh Dabhole', room: '210 · Wing C', batch: 'B2', audience: 'B2', kind: 'PRACTICAL' },
    { id: 'thu-pm-b3', start: '13:30', end: '15:30', subject: 'Electrical Engineering', code: 'ELE', teacher: 'Prof. Arti S. Bindu', room: '101 · Wing C', batch: 'B3', audience: 'B3', kind: 'PRACTICAL' },
  ],
  Friday: [
    { id: 'fri-b1', start: '08:20', end: '10:20', subject: 'Electrical Engineering', code: 'ELE', teacher: 'Prof. Arti S. Bindu', room: '101 · Wing C', batch: 'B1', audience: 'B1', kind: 'PRACTICAL' },
    { id: 'fri-b2', start: '08:20', end: '10:20', subject: 'Library', code: 'LIBRARY', teacher: 'Faculty', room: 'Library B2', batch: 'B2', audience: 'B2', kind: 'LIBRARY' },
    { id: 'fri-b3', start: '08:20', end: '10:20', subject: 'Fundamental of Programming Languages', code: 'FPL', teacher: 'Prof. Sanket Sontakke', room: '109 · Wing B', batch: 'B3', audience: 'B3', kind: 'PRACTICAL' },
    { id: 'fri-break', start: '10:20', end: '10:40', subject: 'Short Break', code: 'BREAK', teacher: '', room: 'Campus', batch: '', audience: 'COMMON', kind: 'SHORT_BREAK' },
    { id: 'fri-dti', start: '10:40', end: '11:40', subject: 'Design Thinking and Innovation', code: 'DTI', teacher: 'Prof. Mujiaid Shaikh', room: 'Room 202', batch: '', audience: 'COMMON', kind: 'CLASS' },
    { id: 'fri-lam', start: '11:40', end: '12:40', subject: 'Linear Algebra & Multivariable Calculus', code: 'LAM', teacher: 'Prof. Vrushali Gujar', room: 'Room 202', batch: '', audience: 'COMMON', kind: 'CLASS' },
    { id: 'fri-lunch', start: '12:40', end: '13:30', subject: 'Lunch Break', code: 'LUNCH', teacher: '', room: 'Cafeteria', batch: '', audience: 'COMMON', kind: 'LUNCH' },
    { id: 'fri-phy', start: '13:30', end: '14:30', subject: 'Engineering Physics', code: 'PHY', teacher: 'Prof. Rupali Jagnade', room: 'Room 202', batch: '', audience: 'COMMON', kind: 'CLASS' },
    { id: 'fri-self', start: '14:30', end: '15:30', subject: 'Self Learning Session', code: 'SELF', teacher: 'Faculty', room: 'Room 202', batch: '', audience: 'COMMON', kind: 'SELF_LEARNING' },
  ]
};

export function batchForRoll(rollNumber) {
  const roll = Number(rollNumber);
  if (roll >= 1 && roll <= 25) return 'B1';
  if (roll >= 26 && roll <= 50) return 'B2';
  if (roll >= 51) return 'B3';
  return 'B2';
}

export function rollRangeForBatch(batch) {
  if (batch === 'B1') return 'Roll 1–25';
  if (batch === 'B2') return 'Roll 26–50';
  if (batch === 'B3') return 'Roll 51+';
  return 'All Students';
}

export function filterTimetableForRoll(rollNumber, day) {
  const dayClasses = RAW_TIMETABLE[day] || [];
  const roll = Number(rollNumber);
  const studentBatch = batchForRoll(roll);

  return dayClasses.filter((item) => {
    if (item.audience === 'COMMON') return true;
    if (item.audience === 'B1' || item.audience === 'B2' || item.audience === 'B3') {
      return item.audience === studentBatch;
    }
    if (item.audience === 'ROLL_1_35') {
      return roll >= 1 && roll <= 35;
    }
    if (item.audience === 'ROLL_36_PLUS') {
      return roll >= 36;
    }
    return false;
  });
}

export const THEMES = [
  { id: 'cyber', name: 'Cyber Neon', tag: 'CYBERPUNK', primary: '#2bd4b4', bg: '#090d16', surface: '#111726', text: '#f3f6fc' },
  { id: 'midnight', name: 'Midnight Violet', tag: 'DEEP NIGHT', primary: '#a78bfa', bg: '#0a0d1e', surface: '#121735', text: '#f5f3ff' },
  { id: 'emerald', name: 'Emerald Campus', tag: 'ACADEMIC', primary: '#10b981', bg: '#07120e', surface: '#0d221a', text: '#ecfdf5' },
  { id: 'sunset', name: 'Sunset Crimson', tag: 'WARM DUSK', primary: '#fb7185', bg: '#140a12', surface: '#241220', text: '#fff1f2' },
  { id: 'light', name: 'Clean Light', tag: 'PAPER DAY', primary: '#3b82f6', bg: '#f8fafc', surface: '#ffffff', text: '#0f172a' },
];

export const EXPENSE_CATEGORIES = [
  { id: 'Food & Drinks', icon: '☕', color: '#f59e0b' },
  { id: 'Travel', icon: '🚌', color: '#3b82f6' },
  { id: 'College', icon: '🎓', color: '#10b981' },
  { id: 'Home & Bills', icon: '🏠', color: '#8b5cf6' },
  { id: 'Mobile', icon: '📱', color: '#06b6d4' },
  { id: 'Personal', icon: '🧴', color: '#ec4899' },
  { id: 'Fun & Social', icon: '🍿', color: '#f43f5e' },
  { id: 'Shopping', icon: '🛍️', color: '#14b8a6' },
  { id: 'Health', icon: '💊', color: '#ef4444' },
  { id: 'Other', icon: '📦', color: '#64748b' },
];

export const DEFAULT_PROFILE = {
  name: 'Neal Aringale',
  college: 'NMIET',
  year: 'FE 2026–27',
  division: 'B',
  rollNo: 34,
  themeIndex: 0,
};

export const INITIAL_TASKS = [
  { id: 1731154000001, title: 'Linear Algebra Tutorial 3 Submission', course: 'LAM', type: 'ASSIGNMENT', dueDate: '2026-11-12', completed: false, priority: 'HIGH' },
  { id: 1731154000002, title: 'FPL Lab Journal Code Review & Output Snips', course: 'FPL', type: 'TASK', dueDate: '2026-11-13', completed: false, priority: 'MEDIUM' },
  { id: 1731154000003, title: 'Physics Unit Test 2 (Wave Optics & Laser)', course: 'PHY', type: 'EXAM', dueDate: '2026-11-18', completed: false, priority: 'HIGH' },
  { id: 1731154000004, title: 'Engineering Graphics Sheet 4 (Isometric Projections)', course: 'EG', type: 'ASSIGNMENT', dueDate: '2026-11-10', completed: true, priority: 'MEDIUM' },
];

export const INITIAL_EXPENSES = [
  { id: 1731154100001, amountPaise: 4500, category: 'Food & Drinks', note: 'Chai & Samosa at Canteen', date: '2026-11-09', paymentMode: 'UPI' },
  { id: 1731154100002, amountPaise: 3000, category: 'Travel', note: 'Auto rickshaw to college gate', date: '2026-11-09', paymentMode: 'Cash' },
  { id: 1731154100003, amountPaise: 12000, category: 'College', note: 'Engineering Graphics drawing sheet booklet', date: '2026-11-08', paymentMode: 'UPI' },
  { id: 1731154100004, amountPaise: 7500, category: 'Food & Drinks', note: 'Dosa & Thums Up with friends', date: '2026-11-07', paymentMode: 'UPI' },
  { id: 1731154100005, amountPaise: 29900, category: 'Mobile', note: 'Monthly 5G Data Pack Recharge', date: '2026-11-05', paymentMode: 'UPI' },
];

export const INITIAL_ATTENDANCE = [
  { key: 'LAM_1', subject: 'Linear Algebra & Multivariable Calculus', date: '2026-11-04', status: 'PRESENT' },
  { key: 'LAM_2', subject: 'Linear Algebra & Multivariable Calculus', date: '2026-11-05', status: 'PRESENT' },
  { key: 'LAM_3', subject: 'Linear Algebra & Multivariable Calculus', date: '2026-11-06', status: 'PRESENT' },
  { key: 'PHY_1', subject: 'Engineering Physics', date: '2026-11-03', status: 'PRESENT' },
  { key: 'PHY_2', subject: 'Engineering Physics', date: '2026-11-04', status: 'ABSENT' },
  { key: 'PHY_3', subject: 'Engineering Physics', date: '2026-11-06', status: 'PRESENT' },
  { key: 'FPL_1', subject: 'Fundamental of Programming Languages', date: '2026-11-03', status: 'PRESENT' },
  { key: 'FPL_2', subject: 'Fundamental of Programming Languages', date: '2026-11-05', status: 'PRESENT' },
  { key: 'EG_1', subject: 'Engineering Graphics', date: '2026-11-03', status: 'PRESENT' },
  { key: 'EG_2', subject: 'Engineering Graphics', date: '2026-11-05', status: 'PRESENT' },
  { key: 'ELE_1', subject: 'Electrical Engineering', date: '2026-11-04', status: 'PRESENT' },
  { key: 'ELE_2', subject: 'Electrical Engineering', date: '2026-11-06', status: 'PRESENT' },
  { key: 'CS_1', subject: 'Communication Skills in Corporate World', date: '2026-11-02', status: 'PRESENT' },
];

export const CAMPUS_LOCATIONS = [
  { room: 'Room 201', type: 'Classroom', desc: 'Main FE Div B classroom for common lectures' },
  { room: 'Room 202', type: 'Classroom', desc: 'Secondary lecture hall (Wed, Thu, Fri)' },
  { room: 'Room 205', type: 'Tutorial Room', desc: 'Tutorials for Roll 1–35' },
  { room: '109 · Wing B', type: 'Computer Lab', desc: 'FPL Programming Laboratory' },
  { room: '115 · Wing B', type: 'Science Lab', desc: 'Engineering Physics Lab' },
  { room: 'Wing B · 312', type: 'Drawing Hall', desc: 'Engineering Graphics Drawing Tables' },
  { room: '101 · Wing C', type: 'Electrical Lab', desc: 'Electrical Machines & Circuits Lab' },
  { room: '210 · Wing C', type: 'Drawing Hall', desc: 'EG Practical Hall (Thu PM B2)' },
  { room: 'Library', type: 'Study Resource', desc: 'Central College Library & Reading Hall' },
];
