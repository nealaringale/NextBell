const compat = typeof window !== 'undefined' ? window.firebase : null;

const env = import.meta.env || {};
const config = window.__NEXTBELL_FIREBASE_CONFIG__ || {
  apiKey: env.VITE_FIREBASE_API_KEY || '',
  authDomain: env.VITE_FIREBASE_AUTH_DOMAIN || '',
  projectId: env.VITE_FIREBASE_PROJECT_ID || '',
  storageBucket: env.VITE_FIREBASE_STORAGE_BUCKET || '',
  messagingSenderId: env.VITE_FIREBASE_MESSAGING_SENDER_ID || '',
  appId: env.VITE_FIREBASE_APP_ID || '',
};

const requiredFields = ['apiKey', 'authDomain', 'projectId', 'appId'];
export const firebaseConfigured = Boolean(
  compat && requiredFields.every((field) => String(config[field] || '').trim())
);

let app = null;
let auth = null;
let db = null;
let firebaseInitError = '';

if (firebaseConfigured) {
  try {
    app = compat.apps.length ? compat.app() : compat.initializeApp(config);
    auth = app.auth();
    db = app.firestore();

    // This is a personal student app. Enable multi-tab offline persistence so
    // queued changes survive refreshes on the user's trusted computer.
    db.enablePersistence({ synchronizeTabs: true }).catch((error) => {
      // Another tab may already own persistence, or the browser may not support it.
      // Firestore still works online and the UI keeps its LocalStorage fallback.
      if (!['failed-precondition', 'unimplemented'].includes(error?.code)) {
        console.warn('NextBell Firestore persistence:', error);
      }
    });
  } catch (error) {
    firebaseInitError = error?.message || 'Firebase initialization failed';
    console.error('NextBell Firebase initialization failed:', error);
    auth = null;
    db = null;
  }
} else {
  firebaseInitError = compat
    ? 'Add the VITE_FIREBASE_* web configuration values.'
    : 'Firebase SDK did not load. Check the Firebase CDN scripts and connection.';
}

export { auth, db, firebaseInitError };
