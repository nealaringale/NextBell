import { firebaseConfig, isFirebaseConfigured } from './firebaseConfig.js';

let services;

export function getFirebaseServices() {
  if (services) return services;
  if (!isFirebaseConfigured() || !window.firebase) return null;

  const firebase = window.firebase;
  const app = firebase.apps.length ? firebase.app() : firebase.initializeApp(firebaseConfig);
  const auth = app.auth();
  const db = app.firestore();

  // Firestore's web cache is opt-in. This keeps the dashboard usable offline
  // and queues writes until the browser reconnects.
  db.enablePersistence({ synchronizeTabs: true }).catch((error) => {
    if (error?.code !== 'failed-precondition' && error?.code !== 'unimplemented') {
      console.warn('NextBell Firestore persistence could not be enabled:', error);
    }
  });

  auth.setPersistence(firebase.auth.Auth.Persistence.LOCAL).catch((error) => {
    console.warn('NextBell auth persistence could not be set:', error);
  });

  services = { firebase, app, auth, db };
  return services;
}

export function firebaseSetupAvailable() {
  return Boolean(window.firebase);
}

export async function createCloudAccount(email, password) {
  const client = getFirebaseServices();
  if (!client) throw new Error('Add the Firebase web app configuration before connecting.');
  const result = await client.auth.createUserWithEmailAndPassword(email.trim(), password);
  return result.user;
}

export async function signInCloudAccount(email, password) {
  const client = getFirebaseServices();
  if (!client) throw new Error('Add the Firebase web app configuration before connecting.');
  const result = await client.auth.signInWithEmailAndPassword(email.trim(), password);
  return result.user;
}

export async function signOutCloudAccount() {
  const client = getFirebaseServices();
  if (!client) return;
  await client.auth.signOut();
}

const ARRAY_COLLECTIONS = ['expenses', 'attendance', 'tasks'];

function recordId(collection, item) {
  const id = collection === 'attendance' ? item?.key : item?.id;
  if (id === undefined || id === null || String(id).trim() === '') return '';
  return String(id);
}

function firestoreRecordId(collection, id) {
  return `${collection}__${encodeURIComponent(String(id))}`;
}

function safeJson(value) {
  try {
    return JSON.stringify(value);
  } catch {
    return '';
  }
}

function normalizeRecord(document) {
  const data = document.data() || {};
  const collection = String(data.collection || '');
  const id = String(data.recordId || '');
  if (!collection || !id) return null;
  let payload = null;
  if (typeof data.payloadJson === 'string' && data.payloadJson) {
    try {
      payload = JSON.parse(data.payloadJson);
    } catch {
      payload = null;
    }
  }
  return {
    documentId: document.id,
    collection,
    id,
    payload,
    payloadJson: typeof data.payloadJson === 'string' ? data.payloadJson : '',
    deleted: data.deleted === true,
    updatedAtMs: Number(data.updatedAtMs) || 0,
  };
}

/**
 * Per-record sync, rather than a single whole-app JSON blob:
 * edits to separate expenses/tasks/attendance records do not overwrite each
 * other when the phone and website reconnect at different times.
 *
 * Deletions are tombstones so an offline device cannot resurrect a deleted row.
 */
export class NextBellCloudSync {
  constructor({ uid, getLocalState, onInitialState, onRemoteRecord, onStatus }) {
    const client = getFirebaseServices();
    if (!client) throw new Error('Firebase web configuration is missing.');
    this.uid = uid;
    this.db = client.db;
    this.getLocalState = getLocalState;
    this.onInitialState = onInitialState;
    this.onRemoteRecord = onRemoteRecord;
    this.onStatus = onStatus || (() => {});
    this.recordsRef = this.db.collection('users').doc(uid).collection('records');
    this.known = new Map();
    this.initialized = false;
    this.disposed = false;
    this.listener = null;
  }

  start() {
    this.onStatus('Connecting to Firebase…');
    this.listener = this.recordsRef.onSnapshot(
      { includeMetadataChanges: true },
      (snapshot) => this.handleSnapshot(snapshot),
      (error) => {
        this.onStatus(`Sync error: ${error.message || error.code || 'unknown error'}`);
      },
    );
    return () => this.stop();
  }

  stop() {
    this.disposed = true;
    if (this.listener) this.listener();
    this.listener = null;
  }

  async writeRecord(collection, id, payload, deleted = false) {
    const documentId = firestoreRecordId(collection, id);
    const updatedAtMs = Date.now();
    const payloadJson = deleted ? '' : safeJson(payload);
    const record = {
      collection,
      recordId: String(id),
      payloadJson,
      deleted,
      updatedAtMs,
      schemaVersion: 1,
    };
    this.known.set(documentId, {
      documentId, collection, id: String(id), payload, payloadJson, deleted, updatedAtMs,
    });
    try {
      await this.recordsRef.doc(documentId).set(record, { merge: true });
    } catch (error) {
      this.onStatus(`Waiting to sync: ${error.message || error.code || 'offline'}`);
    }
  }

  async writeValue(collection, payload) {
    await this.writeRecord(collection, '__singleton__', payload, false);
  }

  pushCollection(collection, items) {
    if (!this.initialized || this.disposed || !ARRAY_COLLECTIONS.includes(collection)) return;
    const current = Array.isArray(items) ? items : [];
    const present = new Set();

    current.forEach((item) => {
      const id = recordId(collection, item);
      if (!id) return;
      present.add(id);
      const documentId = firestoreRecordId(collection, id);
      const payloadJson = safeJson(item);
      const known = this.known.get(documentId);
      if (known && !known.deleted && known.payloadJson === payloadJson) return;
      this.writeRecord(collection, id, item, false);
    });

    for (const known of this.known.values()) {
      if (known.collection !== collection || known.id === '__singleton__'
          || known.deleted || present.has(known.id)) continue;
      this.writeRecord(collection, known.id, null, true);
    }
  }

  pushProfile(profile) {
    if (!this.initialized || this.disposed || !profile) return;
    this.writeValue('profile', profile);
  }

  pushSettings(settings) {
    if (!this.initialized || this.disposed || !settings) return;
    this.writeValue('settings', settings);
  }

  handleSnapshot(snapshot) {
    if (this.disposed) return;

    // Do not treat an empty offline cache as a new empty cloud account. Wait
    // until Firestore has confirmed the first server snapshot.
    if (!this.initialized && snapshot.metadata?.fromCache) {
      this.onStatus('Waiting for Firebase connection…');
      return;
    }

    const remote = snapshot.docs.map(normalizeRecord).filter(Boolean);

    if (!this.initialized) {
      this.initializeFromCloud(remote);
      return;
    }

    remote.forEach((record) => {
      const prior = this.known.get(record.documentId);
      const changed = !prior
        || record.updatedAtMs > prior.updatedAtMs
        || (record.updatedAtMs === prior.updatedAtMs
          && (record.deleted !== prior.deleted || record.payloadJson !== prior.payloadJson));
      this.known.set(record.documentId, record);
      if (!changed) return;
      if (record.collection === 'profile' || record.collection === 'settings') {
        if (!record.deleted && record.payload) {
          this.onRemoteRecord(record.collection, record.id, record.payload, false);
        }
        return;
      }
      if (ARRAY_COLLECTIONS.includes(record.collection)) {
        this.onRemoteRecord(record.collection, record.id, record.payload, record.deleted);
      }
    });

    this.onStatus('Synced · live updates enabled');
  }

  initializeFromCloud(remoteRecords) {
    const local = this.getLocalState() || {};
    const merged = {
      profile: local.persisted?.profile ? local.profile : null,
      settings: local.persisted?.settings ? local.settings : null,
      expenses: local.persisted?.expenses ? [...(local.expenses || [])] : [],
      attendance: local.persisted?.attendance ? [...(local.attendance || [])] : [],
      tasks: local.persisted?.tasks ? [...(local.tasks || [])] : [],
    };
    const remoteByCollection = new Map();

    remoteRecords.forEach((record) => {
      this.known.set(record.documentId, record);
      if (record.collection === 'profile' || record.collection === 'settings') {
        if (!record.deleted && record.payload) merged[record.collection] = record.payload;
        return;
      }
      if (!ARRAY_COLLECTIONS.includes(record.collection)) return;
      if (!remoteByCollection.has(record.collection)) {
        remoteByCollection.set(record.collection, new Map());
      }
      remoteByCollection.get(record.collection).set(record.id, record);
    });

    ARRAY_COLLECTIONS.forEach((collection) => {
      const localItems = Array.isArray(merged[collection]) ? merged[collection] : [];
      const byId = new Map(localItems.map((item) => [recordId(collection, item), item]));
      const remoteItems = remoteByCollection.get(collection) || new Map();

      remoteItems.forEach((record, id) => {
        if (record.deleted) byId.delete(id);
        else if (record.payload) byId.set(id, record.payload);
      });

      // Local-only records are migrated to the cloud; remote versions win when
      // the same record ID already exists.
      localItems.forEach((item) => {
        const id = recordId(collection, item);
        if (!id || remoteItems.has(id)) return;
        byId.set(id, item);
        this.writeRecord(collection, id, item, false);
      });
      merged[collection] = [...byId.values()];
    });

    if (merged.profile) {
      const cloudHasProfile = this.known.has(firestoreRecordId('profile', '__singleton__'));
      if (!cloudHasProfile) this.writeValue('profile', merged.profile);
    }
    if (merged.settings) {
      const cloudHasSettings = this.known.has(firestoreRecordId('settings', '__singleton__'));
      if (!cloudHasSettings) this.writeValue('settings', merged.settings);
    }

    this.initialized = true;
    this.onInitialState(merged);
    this.onStatus('Synced · live updates enabled');
  }
}
