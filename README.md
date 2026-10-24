# Super Ne

Super Ne is the unified web experience for college planning, study productivity and personal wellbeing. This repository uses the existing NextBell React + Vite web app as its base and keeps the original NextBell timetable views available.

## Current web experience

- **College:** the existing NextBell Today, Week and Announcements screens.
- **Daily dashboard:** a unified overview with priorities, water tracking, and a focus timer.
- **Tasks:** add, complete and delete daily tasks. Tasks are stored in browser local storage.
- **Water tracker:** quick-add up to eight glasses per browser-local counter.
- **Focus timer:** 25-minute focus timer with pause/resume.
- **Theme:** existing dark/light theme toggle.

## Run locally

```bash
npm install
npm run dev
```

Build for production:

```bash
npm run build
```

## Source projects and migration notes

- [NextBell](https://github.com/nealaringale/NextBell) — source of the timetable web experience and Android implementation.
- [Super-me-](https://github.com/nealaringale/Super-me-) — source Android app with study/project roadmaps, Pomodoro options, flashcards, notes, fasting, weight, water and food diary features.

The original Super Me project is native Android Java/Gradle, not a web application. The web dashboard implements a first shared layer (tasks, focus timer, and water counter). Porting the remaining native modules—flashcards, notes, fasting history, food diary, data import/export and reminders—requires dedicated web implementations and regression tests. Both original repositories remain unchanged.

## Data and privacy

The web dashboard stores tasks and water count in browser local storage. It does not currently provide cross-device sync, authentication, cloud backup, or medical guidance.
