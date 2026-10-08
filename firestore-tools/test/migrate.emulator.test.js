// End-to-end: write the fixture into the Firestore emulator twice and verify (CHE-50).
// Own project ID so it never clashes with rules.test.js running in parallel.
import assert from 'node:assert/strict';
import { before, describe, it } from 'node:test';
import { connect, verify, writeDocs } from '../migration/firestore-io.js';
import { transform } from '../migration/transform.js';
import { RECIPE_A, rtdbExport } from './fixture.js';

const PROJECT = 'demo-chef-migration';

describe('migration against the emulator', { skip: !process.env.FIRESTORE_EMULATOR_HOST && 'emulator not running' }, () => {
  let db;
  const { docs } = transform(rtdbExport());
  const silent = () => {};

  before(async () => {
    db = connect(PROJECT);
    await fetch(`http://${process.env.FIRESTORE_EMULATOR_HOST}/emulator/v1/projects/${PROJECT}/databases/(default)/documents`, {
      method: 'DELETE'
    });
  });

  it('writes every document and verifies all of them', async () => {
    assert.equal(await writeDocs(db, docs, silent), docs.length);
    const result = await verify(db, docs, Infinity);
    assert.deepEqual(result.problems, []);
    assert.equal(result.checked, docs.length);
  });

  it('is idempotent: a second run creates no duplicates', async () => {
    await writeDocs(db, docs, silent);
    const result = await verify(db, docs, Infinity);
    assert.deepEqual(result.problems, []);
    assert.deepEqual(result.counts.actual, result.counts.expected);
  });

  it('stores timestamps as Firestore Timestamps', async () => {
    const snapshot = await db.doc(`recipes/${RECIPE_A}`).get();
    assert.equal(snapshot.get('updatedAt').constructor.name, 'Timestamp');
  });

  it('reports drift: a changed document and an extra document', async () => {
    await db.doc(`recipes/${RECIPE_A}`).update({ title: 'Ändrad' });
    await db.doc('recipes/extra').set({ title: 'Bara i Firestore' });
    const { problems } = await verify(db, docs, Infinity);
    assert.ok(problems.some((p) => p.startsWith('count recipes: expected 2, found 3')));
    assert.ok(problems.some((p) => p.startsWith(`mismatch recipes/${RECIPE_A}`)));
  });
});
