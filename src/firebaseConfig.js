/**
 * Firebase Web app configuration.
 *
 * Local development: copy .env.example to .env.local and fill the VITE_FIREBASE_* values.
 * GitHub Pages: configure the same values as Actions repository secrets.
 *
 * These are public Firebase client identifiers. Never put a service-account
 * JSON/private key or Admin SDK credentials in a client application.
 */
const env = import.meta.env || {};

export const firebaseConfig = {
  apiKey: env.VITE_FIREBASE_API_KEY || '',
  authDomain: env.VITE_FIREBASE_AUTH_DOMAIN || '',
  projectId: env.VITE_FIREBASE_PROJECT_ID || '',
  appId: env.VITE_FIREBASE_APP_ID || '',
  messagingSenderId: env.VITE_FIREBASE_MESSAGING_SENDER_ID || '',
  storageBucket: env.VITE_FIREBASE_STORAGE_BUCKET || '',
};

export function isFirebaseConfigured() {
  return Boolean(
    firebaseConfig.apiKey
      && firebaseConfig.authDomain
      && firebaseConfig.projectId
      && firebaseConfig.appId
      && firebaseConfig.messagingSenderId,
  );
}
