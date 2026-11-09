/**
 * Firebase web-app configuration.
 *
 * Get these values from Firebase Console → Project settings → General →
 * Your apps → Web app → SDK setup and configuration.
 *
 * These are public client identifiers, not service-account credentials.
 * Never put a service-account JSON/private key in the website or Android app.
 */
export const firebaseConfig = {
  apiKey: "",
  authDomain: "",
  projectId: "",
  appId: "",
  messagingSenderId: "",
};

export function isFirebaseConfigured() {
  return Boolean(
    firebaseConfig.apiKey
      && firebaseConfig.authDomain
      && firebaseConfig.projectId
      && firebaseConfig.appId,
  );
}
