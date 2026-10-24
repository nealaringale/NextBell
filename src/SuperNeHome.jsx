import { useEffect, useState } from 'react';

const read = (key, fallback) => {
  try { const value = localStorage.getItem(key); return value ? JSON.parse(value) : fallback; }
  catch { return fallback; }
};

export default function SuperNeHome({ onOpenTimetable }) {
  const [tasks, setTasks] = useState(() => read('superne.tasks', []));
  const [taskText, setTaskText] = useState('');
  const [water, setWater] = useState(() => read('superne.water', 0));
  const [focusSeconds, setFocusSeconds] = useState(25 * 60);
  const [running, setRunning] = useState(false);

  useEffect(() => { localStorage.setItem('superne.tasks', JSON.stringify(tasks)); }, [tasks]);
  useEffect(() => { localStorage.setItem('superne.water', JSON.stringify(water)); }, [water]);
  useEffect(() => {
    if (!running) return undefined;
    const id = window.setInterval(() => setFocusSeconds((seconds) => {
      if (seconds <= 1) { setRunning(false); return 25 * 60; }
      return seconds - 1;
    }), 1000);
    return () => window.clearInterval(id);
  }, [running]);

  const addTask = (event) => {
    event.preventDefault();
    const title = taskText.trim();
    if (!title) return;
    setTasks((items) => [{ id: Date.now(), title, done: false }, ...items]);
    setTaskText('');
  };
  const completed = tasks.filter((task) => task.done).length;
  const clock = (seconds) => `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;

  return (
    <div className="superne-home">
      <section className="sn-welcome">
        <div>
          <div className="sn-kicker">YOUR PERSONAL OPERATING SYSTEM</div>
          <h2>One place for your whole day.</h2>
          <p>College, focused work, and wellbeing — together in Super Ne.</p>
        </div>
        <button className="sn-primary" onClick={onOpenTimetable}>Open timetable <span aria-hidden="true">↗</span></button>
      </section>

      <section className="sn-metrics" aria-label="Daily overview">
        <article className="sn-metric"><span className="sn-icon">▦</span><span className="sn-muted">Tasks completed</span><strong>{completed}<small> / {tasks.length}</small></strong><span className="sn-progress"><i style={{ width: `${tasks.length ? completed / tasks.length * 100 : 0}%` }} /></span></article>
        <article className="sn-metric"><span className="sn-icon">⌁</span><span className="sn-muted">Water today</span><strong>{water}<small> / 8 glasses</small></strong><button className="sn-inline" onClick={() => setWater((value) => Math.min(8, value + 1))} disabled={water >= 8}>+ Add a glass</button></article>
        <article className="sn-metric"><span className="sn-icon">◷</span><span className="sn-muted">Focus session</span><strong className="sn-clock">{clock(focusSeconds)}</strong><button className="sn-inline" onClick={() => setRunning((value) => !value)}>{running ? 'Pause timer' : 'Start focus'}</button></article>
      </section>

      <section className="sn-workspace">
        <article className="sn-panel sn-tasks">
          <div className="sn-panel-heading"><div><div className="sn-kicker">MAKE IT HAPPEN</div><h3>Today’s priorities</h3></div><span className="sn-count">{tasks.length} items</span></div>
          <form className="sn-add-task" onSubmit={addTask}>
            <input value={taskText} onChange={(event) => setTaskText(event.target.value)} placeholder="Add a task or study goal…" aria-label="New task" maxLength={120}/>
            <button type="submit" aria-label="Add task">+</button>
          </form>
          <div className="sn-task-list">
            {tasks.length === 0 && <p className="sn-empty">Your list is clear. Add one small priority to get started.</p>}
            {tasks.map((task) => <div className={`sn-task ${task.done ? 'is-done' : ''}`} key={task.id}>
              <label><input type="checkbox" checked={task.done} onChange={() => setTasks((items) => items.map((item) => item.id === task.id ? { ...item, done: !item.done } : item))}/><span>{task.title}</span></label>
              <button className="sn-delete" aria-label={`Delete ${task.title}`} onClick={() => setTasks((items) => items.filter((item) => item.id !== task.id))}>×</button>
            </div>)}
          </div>
        </article>

        <article className="sn-panel sn-modules">
          <div className="sn-kicker">YOUR SPACE</div><h3>Everything connected.</h3>
          <button className="sn-module" onClick={onOpenTimetable}><span className="sn-module-icon">▦</span><span><strong>College & timetable</strong><small>Classes, weekly schedule and announcements</small></span><b>→</b></button>
          <div className="sn-module"><span className="sn-module-icon">◎</span><span><strong>Study & focus</strong><small>Use the task list and built-in focus timer</small></span><b>•</b></div>
          <div className="sn-module"><span className="sn-module-icon">♡</span><span><strong>Wellbeing</strong><small>Daily water tracking stored on this device</small></span><b>•</b></div>
          <p className="sn-note">Your tasks and water count are saved locally in this browser. No account or cloud sync is required.</p>
        </article>
      </section>
    </div>
  );
}
