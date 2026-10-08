// Firestore side of the migration: write + verify (CHE-50).
import { applicationDefault, initializeApp } from 'firebase-admin/app';
import { Timestamp, getFirestore } from 'firebase-admin/firestore';
import { countByCollection } from './transform.js';

/** Uses ADC (`gcloud auth application-default login`) — or the emulator when FIRESTORE_EMULATOR_HOST is set. */
export function connect(projectId) {
  const emulator = process.env.FIRESTORE_EMULATOR_HOST;
  const app = initializeApp(emulator ? { projectId } : { projectId, credential: applicationDefault() });
  return getFirestore(app);
}

/**
 * Overwrites each document with `set()` (no merge), so re-running yields the same state and never
 * duplicates: IDs come from the RTDB keys. Documents that only exist in Firestore are left untouched.
 */
export async function writeDocs(db, docs, log = console.log) {
  const writer = db.bulkWriter();
  let written = 0;
  writer.onWriteResult(() => {
    written += 1;
    if (written % 500 === 0) log(`  ${written}/${docs.length} written`);
  });
  for (const { path, data } of docs) writer.set(db.doc(path), data);
  await writer.close();
  return written;
}

/** Firestore value → plain JSON, Timestamps/Dates as ms precision ISO strings, for comparison. */
export function normalise(value) {
  if (value instanceof Timestamp) return value.toDate().toISOString();
  if (value instanceof Date) return value.toISOString();
  if (Array.isArray(value)) return value.map(normalise);
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.keys(value).sort().map((k) => [k, normalise(value[k])]));
  }
  return value;
}

async function actualCounts(db, expectedKeys) {
  const counts = {};
  for (const key of expectedKeys) {
    // "users/chat_history" → collection group "chat_history" scoped by its parent collection name.
    const segments = key.split('/');
    const group = segments[segments.length - 1];
    const query = segments.length === 1 ? db.collection(group) : db.collectionGroup(group);
    counts[key] = (await query.count().get()).data().count;
  }
  return counts;
}

/**
 * Compares per-collection counts and deep-compares `sampleSize` random documents
 * (or every document when sampleSize is Infinity).
 */
export async function verify(db, docs, sampleSize) {
  const problems = [];

  const expected = countByCollection(docs);
  const actual = await actualCounts(db, Object.keys(expected));
  for (const key of Object.keys(expected)) {
    if (expected[key] !== actual[key]) {
      problems.push(`count ${key}: expected ${expected[key]}, found ${actual[key]}`);
    }
  }

  const sample = sampleSize >= docs.length ? docs : [...docs].sort(() => Math.random() - 0.5).slice(0, sampleSize);
  const refs = sample.map(({ path }) => db.doc(path));
  const snapshots = refs.length ? await db.getAll(...refs) : [];
  snapshots.forEach((snapshot, i) => {
    const { path, data } = sample[i];
    if (!snapshot.exists) {
      problems.push(`missing ${path}`);
      return;
    }
    const want = JSON.stringify(normalise(data));
    const got = JSON.stringify(normalise(snapshot.data()));
    if (want !== got) problems.push(`mismatch ${path}\n    expected ${want}\n    found    ${got}`);
  });

  return { counts: { expected, actual }, checked: sample.length, problems };
}

