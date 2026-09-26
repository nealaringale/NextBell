# NextBell 🔔

A student-first college timetable app for seeing today's classes, the next lecture, batch-specific sessions, announcements, and a weekly schedule at a glance.

## MVP

- Today view with live next-class calculation
- Weekly timetable view
- Division + roll-number profile context
- Batch-aware practical/tutorial labels
- Announcements page
- Dark/light mode
- Responsive desktop/mobile layout

## Local development

```bash
npm install
npm run dev
```

Build for production:

```bash
npm run build
```

## Timetable data

The demo timetable lives in `src/data.js`. Replace the sample classes with your real NMIET schedule. Keep each class shaped like:

```js
{
  id: 'mon-1',
  start: '08:00',
  end: '09:00',
  subject: 'Engineering Mathematics',
  short: 'Maths',
  type: 'Lecture',
  room: 'C-204',
  teacher: 'Prof. Example',
  batch: 'B2'
}
```

`batch` is optional and is used for practical/tutorial sessions.

## Roadmap

1. Replace demo timetable with the real class schedule.
2. Add Supabase for shared class data and admin edits.
3. Add authentication, attendance, reminders, and timetable-change notifications.
