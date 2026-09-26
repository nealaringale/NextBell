import { useEffect, useMemo, useState } from 'react';
import { announcements, profile, timetable } from './data.js';

const days = Object.keys(timetable);
const typeIcons = { Lecture: 'L', Practical: 'P', Tutorial: 'T' };

function minutes(value) {
  const [h, m] = value.split(':').map(Number);
  return h * 60 + m;
}

function formatTime(value) {
  const [h, m] = value.split(':').map(Number);
  const suffix = h >= 12 ? 'PM' : 'AM';
  const hour = h % 12 || 12;
  return `${hour}:${String(m).padStart(2, '0')} ${suffix}`;
}

function getTodayName() {
  return new Intl.DateTimeFormat('en-US', {
    weekday: 'long',
    timeZone: 'Asia/Kolkata'
  }).format(new Date());
}

function getNowMinutes() {
  const parts = new Intl.DateTimeFormat('en-GB', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
    timeZone: 'Asia/Kolkata'
  }).formatToParts(new Date());

  const h = Number(parts.find((part) => part.type === 'hour')?.value ?? 0);
  const m = Number(parts.find((part) => part.type === 'minute')?.value ?? 0);
  return h * 60 + m;
}

function App() {
  const today = getTodayName();
  const [page, setPage] = useState('Today');
  const [selectedDay, setSelectedDay] = useState(days.includes(today) ? today : 'Monday');
  const [now, setNow] = useState(getNowMinutes());
  const [dark, setDark] = useState(true);

  useEffect(() => {
    const timer = window.setInterval(() => setNow(getNowMinutes()), 30000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    document.documentElement.dataset.theme = dark ? 'dark' : 'light';
  }, [dark]);

  const classesForDay = timetable[selectedDay] ?? [];

  const currentClass = useMemo(
    () => classesForDay.find((item) => minutes(item.start) <= now && now < minutes(item.end)),
    [classesForDay, now]
  );

  const nextClass = useMemo(
    () => classesForDay.find((item) => minutes(item.start) > now),
    [classesForDay, now]
  );

  const focusClass = currentClass ?? nextClass;
  const focusIsCurrent = Boolean(currentClass);
  const completed = classesForDay.filter((item) => minutes(item.end) <= now).length;

  const freePeriods = classesForDay.reduce((count, item, index) => {
    if (index === 0) return count;
    const previous = classesForDay[index - 1];
    const gap = minutes(item.start) - minutes(previous.end);
    return count + (gap >= 45 ? 1 : 0);
  }, 0);

  const goToDay = (day) => {
    setSelectedDay(day);
    setPage('Today');
  };

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">⌁</div>
          <div>
            <div className="brand-name">NextBell</div>
            <div className="brand-sub">college, one glance away</div>
          </div>
        </div>

        <nav className="nav-list">
          {[
            ['Today', '◷'],
            ['Week', '▦'],
            ['Announcements', '◉']
          ].map(([label, icon]) => (
            <button
              key={label}
              className={`nav-item ${page === label ? 'active' : ''}`}
              onClick={() => setPage(label)}
            >
              <span>{icon}</span>
              {label}
            </button>
          ))}
        </nav>

        <div className="profile-card">
          <div className="avatar">N</div>
          <div className="profile-copy">
            <strong>{profile.name}</strong>
            <span>Roll {profile.rollNo} · {profile.batch}</span>
          </div>
          <span className="status-dot" title="Profile loaded" />
        </div>

        <button className="theme-toggle" onClick={() => setDark((value) => !value)}>
          <span>{dark ? '☼' : '☾'}</span>
          {dark ? 'Light mode' : 'Dark mode'}
        </button>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div>
            <div className="eyebrow">{profile.college} · {profile.year}</div>
            <h1>{page === 'Today' ? selectedDay : page}</h1>
          </div>
          <div className="class-pill">
            <span /> {profile.division} Division · {profile.batch}
          </div>
        </header>

        {page === 'Today' && (
          <>
            <section className="hero-grid">
              <div className="hero-card">
                <div className="hero-label">{focusIsCurrent ? 'HAPPENING NOW' : 'NEXT CLASS'}</div>
                <div className="hero-title">
                  {focusClass ? focusClass.subject : 'You’re done for today 🎉'}
                </div>

                {focusClass ? (
                  <>
                    <div className="hero-meta">
                      {formatTime(focusClass.start)} – {formatTime(focusClass.end)} · {focusClass.room}
                    </div>

                    <div className="hero-bottom">
                      <div className="countdown">
                        {focusIsCurrent
                          ? Math.max(0, minutes(focusClass.end) - now)
                          : Math.max(0, minutes(focusClass.start) - now)}
                        <span> min</span>
                      </div>
                      <span className="type-chip">
                        <b>{typeIcons[focusClass.type]}</b>
                        {focusClass.type}
                        {focusClass.batch ? ` · ${focusClass.batch}` : ''}
                      </span>
                    </div>
                  </>
                ) : (
                  <div className="hero-meta">No more scheduled classes in this timetable.</div>
                )}
              </div>

              <div className="stat-card">
                <div className="stat-top">
                  <span>DAY PULSE</span>
                  <span>{selectedDay === today ? 'Today' : selectedDay}</span>
                </div>
                <div className="stat-number">{classesForDay.length}</div>
                <div className="stat-label">classes scheduled</div>
                <div className="stat-row">
                  <span>Completed</span>
                  <b>{completed}/{classesForDay.length}</b>
                </div>
                <div className="stat-row">
                  <span>Free periods</span>
                  <b>{freePeriods}</b>
                </div>
              </div>
            </section>

            <section className="section-heading">
              <div>
                <div className="eyebrow">YOUR SCHEDULE</div>
                <h2>Classes for {selectedDay}</h2>
              </div>

              <div className="mini-tabs">
                {days.slice(0, 6).map((day) => (
                  <button
                    key={day}
                    className={selectedDay === day ? 'selected' : ''}
                    onClick={() => setSelectedDay(day)}
                  >
                    {day.slice(0, 3)}
                  </button>
                ))}
              </div>
            </section>

            <div className="class-list">
              {classesForDay.length === 0 ? (
                <div className="empty-state">No classes scheduled. Enjoy the free day.</div>
              ) : (
                classesForDay.map((item) => {
                  const isDone = minutes(item.end) <= now;
                  const isCurrent = currentClass?.id === item.id;
                  const isNext = !currentClass && nextClass?.id === item.id;

                  return (
                    <article
                      key={item.id}
                      className={`class-row ${isCurrent || isNext ? 'next' : ''} ${isDone ? 'done' : ''}`}
                    >
                      <div className="time-col">
                        <strong>{formatTime(item.start)}</strong>
                        <span>{formatTime(item.end)}</span>
                      </div>

                      <div className="type-icon">{typeIcons[item.type]}</div>

                      <div className="class-main">
                        <div className="class-title-line">
                          <h3>{item.subject}</h3>
                          {(isCurrent || isNext) && (
                            <span className="next-badge">{isCurrent ? 'NOW' : 'NEXT'}</span>
                          )}
                        </div>
                        <p>
                          {item.teacher} · {item.type}
                          {item.batch ? ` · ${item.batch}` : ''}
                        </p>
                      </div>

                      <div className="room">⌖ {item.room}</div>
                    </article>
                  );
                })
              )}
            </div>
          </>
        )}

        {page === 'Week' && (
          <section className="week-board">
            <div className="week-note">Tap a day to open its full schedule.</div>

            {days.map((day) => (
              <button className="week-day-card" key={day} onClick={() => goToDay(day)}>
                <div className="day-name">{day}</div>
                <div className="day-count">{timetable[day].length} classes</div>
                <div className="day-preview">
                  {timetable[day].slice(0, 3).map((item) => item.short).join(' · ') || 'Free day'}
                </div>
                <span className="arrow">→</span>
              </button>
            ))}
          </section>
        )}

        {page === 'Announcements' && (
          <section className="announcements">
            {announcements.map((item) => (
              <article className="announcement" key={item.title}>
                <div className="announcement-tag">{item.tag}</div>
                <div>
                  <h3>{item.title}</h3>
                  <p>{item.text}</p>
                  <span>{item.time}</span>
                </div>
              </article>
            ))}
          </section>
        )}

        <footer>
          <span>NextBell v0.1 · MVP</span>
          <span>Built for your class 🤝</span>
        </footer>
      </main>
    </div>
  );
}

export default App;
