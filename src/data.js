export const profile = {
  name: 'Neal',
  college: 'NMIET',
  year: 'FE 2026–27',
  division: 'B',
  batch: 'B2',
  rollNo: 34,
};

// Demo schedule: replace this file with your real NMIET timetable as you collect it.
export const timetable = {
  Monday: [
    { id: 'mon-1', start: '08:00', end: '09:00', subject: 'Engineering Mathematics', short: 'Maths', type: 'Lecture', room: 'C-204', teacher: 'Prof. Kulkarni' },
    { id: 'mon-2', start: '09:00', end: '10:00', subject: 'Engineering Physics', short: 'Physics', type: 'Lecture', room: 'C-204', teacher: 'Prof. Patil' },
    { id: 'mon-3', start: '10:15', end: '12:15', subject: 'Programming Lab', short: 'Programming', type: 'Practical', room: 'Lab 3', teacher: 'Prof. Jadhav', batch: 'B2' },
    { id: 'mon-4', start: '13:15', end: '14:15', subject: 'Elements of Electrical Engineering', short: 'Electrical', type: 'Lecture', room: 'C-205', teacher: 'Prof. More' },
    { id: 'mon-5', start: '14:15', end: '15:15', subject: 'Engineering Graphics', short: 'Graphics', type: 'Lecture', room: 'C-206', teacher: 'Prof. Shinde' }
  ],
  Tuesday: [
    { id: 'tue-1', start: '08:00', end: '09:00', subject: 'Elements of Electrical Engineering', short: 'Electrical', type: 'Lecture', room: 'C-204', teacher: 'Prof. More' },
    { id: 'tue-2', start: '09:00', end: '10:00', subject: 'Engineering Mathematics', short: 'Maths', type: 'Lecture', room: 'C-204', teacher: 'Prof. Kulkarni' },
    { id: 'tue-3', start: '10:15', end: '11:15', subject: 'Engineering Chemistry', short: 'Chemistry', type: 'Lecture', room: 'C-205', teacher: 'Prof. Deshmukh' },
    { id: 'tue-4', start: '11:15', end: '13:15', subject: 'Electronics Lab', short: 'Electronics', type: 'Practical', room: 'ELX Lab', teacher: 'Prof. Pawar', batch: 'B2' },
    { id: 'tue-5', start: '14:00', end: '15:00', subject: 'Communication Skills', short: 'Communication', type: 'Tutorial', room: 'C-302', teacher: 'Prof. Joshi', batch: 'B2' }
  ],
  Wednesday: [
    { id: 'wed-1', start: '08:00', end: '09:00', subject: 'Engineering Mathematics', short: 'Maths', type: 'Lecture', room: 'C-204', teacher: 'Prof. Kulkarni' },
    { id: 'wed-2', start: '09:00', end: '10:00', subject: 'Engineering Chemistry', short: 'Chemistry', type: 'Lecture', room: 'C-204', teacher: 'Prof. Deshmukh' },
    { id: 'wed-3', start: '10:15', end: '11:15', subject: 'Engineering Physics', short: 'Physics', type: 'Lecture', room: 'C-205', teacher: 'Prof. Patil' },
    { id: 'wed-4', start: '11:15', end: '13:15', subject: 'Engineering Graphics', short: 'Graphics', type: 'Practical', room: 'Drawing Hall', teacher: 'Prof. Shinde', batch: 'B2' }
  ],
  Thursday: [
    { id: 'thu-1', start: '08:00', end: '09:00', subject: 'Engineering Physics', short: 'Physics', type: 'Lecture', room: 'C-204', teacher: 'Prof. Patil' },
    { id: 'thu-2', start: '09:00', end: '10:00', subject: 'Engineering Mathematics', short: 'Maths', type: 'Lecture', room: 'C-204', teacher: 'Prof. Kulkarni' },
    { id: 'thu-3', start: '10:15', end: '11:15', subject: 'Elements of Electrical Engineering', short: 'Electrical', type: 'Lecture', room: 'C-205', teacher: 'Prof. More' },
    { id: 'thu-4', start: '11:15', end: '13:15', subject: 'Programming Lab', short: 'Programming', type: 'Practical', room: 'Lab 3', teacher: 'Prof. Jadhav', batch: 'B2' },
    { id: 'thu-5', start: '14:00', end: '15:00', subject: 'Engineering Chemistry', short: 'Chemistry', type: 'Lecture', room: 'C-206', teacher: 'Prof. Deshmukh' }
  ],
  Friday: [
    { id: 'fri-1', start: '08:00', end: '09:00', subject: 'Engineering Mathematics', short: 'Maths', type: 'Lecture', room: 'C-204', teacher: 'Prof. Kulkarni' },
    { id: 'fri-2', start: '09:00', end: '10:00', subject: 'Elements of Electrical Engineering', short: 'Electrical', type: 'Lecture', room: 'C-204', teacher: 'Prof. More' },
    { id: 'fri-3', start: '10:15', end: '11:15', subject: 'Communication Skills', short: 'Communication', type: 'Tutorial', room: 'C-302', teacher: 'Prof. Joshi', batch: 'B2' },
    { id: 'fri-4', start: '11:15', end: '13:15', subject: 'Electronics Lab', short: 'Electronics', type: 'Practical', room: 'ELX Lab', teacher: 'Prof. Pawar', batch: 'B2' }
  ],
  Saturday: [
    { id: 'sat-1', start: '09:00', end: '10:00', subject: 'Engineering Physics', short: 'Physics', type: 'Lecture', room: 'C-204', teacher: 'Prof. Patil' },
    { id: 'sat-2', start: '10:15', end: '11:15', subject: 'Engineering Chemistry', short: 'Chemistry', type: 'Lecture', room: 'C-205', teacher: 'Prof. Deshmukh' }
  ],
  Sunday: []
};

export const announcements = [
  {
    tag: 'INFO',
    title: 'Welcome to NextBell',
    text: 'This MVP uses demo timetable data. Update src/data.js with your class schedule.',
    time: 'Just now'
  },
  {
    tag: 'TODO',
    title: 'Next step',
    text: 'Add the real B Division timetable, then connect Supabase for shared updates.',
    time: 'Today'
  }
];
