// Security rule tests for ../firestore.rules against the Firestore emulator (CHE-50).
// Run: npm test  (starts the emulator via `firebase emulators:exec`)
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, it } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment
} from '@firebase/rules-unit-testing';
import { deleteDoc, deleteField, doc, getDoc, increment, setDoc, updateDoc } from 'firebase/firestore';

const ALICE = 'alice';
const BOB = 'bob';

let env;

const db = (uid) => (uid ? env.authenticatedContext(uid) : env.unauthenticatedContext()).firestore();

const seed = (path, data) =>
  env.withSecurityRulesDisabled((ctx) => setDoc(doc(ctx.firestore(), path), data));

const recipe = (uid, extra = {}) => ({ id: 'r1', uid, title: 'Soppa', isFavourite: true, ...extra });

before(async () => {
  env = await initializeTestEnvironment({
    // "demo-" prefix: the emulator never talks to a real project.
    projectId: 'demo-chef',
    firestore: { rules: readFileSync(new URL('../../firestore.rules', import.meta.url), 'utf8') }
  });
});

beforeEach(() => env.clearFirestore());

after(() => env.cleanup());

describe('recipes', () => {
  beforeEach(() => seed('recipes/r1', recipe(ALICE)));

  it('denies unauthenticated read', () => assertFails(getDoc(doc(db(), 'recipes/r1'))));
  it("allows any signed-in user to read others' recipes", () =>
    assertSucceeds(getDoc(doc(db(BOB), 'recipes/r1'))));

  it('allows create with own uid', () => assertSucceeds(setDoc(doc(db(BOB), 'recipes/r2'), recipe(BOB))));
  it('allows create with null uid', () => assertSucceeds(setDoc(doc(db(BOB), 'recipes/r2'), recipe(null))));
  it("denies create with someone else's uid", () =>
    assertFails(setDoc(doc(db(BOB), 'recipes/r2'), recipe(ALICE))));

  it('allows owner update', () => assertSucceeds(updateDoc(doc(db(ALICE), 'recipes/r1'), { title: 'Gryta' })));
  it('denies non-owner update', () => assertFails(updateDoc(doc(db(BOB), 'recipes/r1'), { title: 'Gryta' })));
  it('allows owner to orphan (uid -> null)', () =>
    assertSucceeds(updateDoc(doc(db(ALICE), 'recipes/r1'), { uid: null })));
  it('allows owner to orphan by deleting the uid field', () =>
    assertSucceeds(updateDoc(doc(db(ALICE), 'recipes/r1'), { uid: deleteField() })));
  it('denies owner transferring to another uid', () =>
    assertFails(updateDoc(doc(db(ALICE), 'recipes/r1'), { uid: BOB })));

  it('allows owner delete', () => assertSucceeds(deleteDoc(doc(db(ALICE), 'recipes/r1'))));
  it('denies non-owner delete', () => assertFails(deleteDoc(doc(db(BOB), 'recipes/r1'))));

  it('denies everyone writing an orphaned recipe', async () => {
    const { uid, ...orphan } = recipe(ALICE);
    await seed('recipes/orphan', orphan);
    await assertFails(updateDoc(doc(db(ALICE), 'recipes/orphan'), { title: 'x' }));
    await assertFails(updateDoc(doc(db(BOB), 'recipes/orphan'), { uid: BOB }));
  });
});

describe('recipes/{id}/variants', () => {
  beforeEach(async () => {
    await seed('recipes/r1', recipe(ALICE));
    await seed('recipes/r1/variants/v1', { id: 'v1', label: 'Vegansk', isPinned: false });
  });

  it('allows any signed-in user to read', () =>
    assertSucceeds(getDoc(doc(db(BOB), 'recipes/r1/variants/v1'))));
  it('denies unauthenticated read', () => assertFails(getDoc(doc(db(), 'recipes/r1/variants/v1'))));
  it('allows recipe owner to write', () =>
    assertSucceeds(setDoc(doc(db(ALICE), 'recipes/r1/variants/v2'), { label: 'Glutenfri' })));
  it('allows recipe owner to pin and delete', async () => {
    await assertSucceeds(updateDoc(doc(db(ALICE), 'recipes/r1/variants/v1'), { isPinned: true }));
    await assertSucceeds(deleteDoc(doc(db(ALICE), 'recipes/r1/variants/v1')));
  });
  it('denies non-owner write', () =>
    assertFails(setDoc(doc(db(BOB), 'recipes/r1/variants/v2'), { label: 'Spam' })));
  it('denies write when the parent recipe does not exist', () =>
    assertFails(setDoc(doc(db(ALICE), 'recipes/missing/variants/v1'), { label: 'x' })));
});

describe('recipe_of_the_month and video_generation_history', () => {
  beforeEach(async () => {
    await seed('recipe_of_the_month/m1', { recipeId: 'r1', monthOf: '2026-10' });
    await seed('video_generation_history/r1', { selectedAt: null });
  });

  it('allows public read', async () => {
    await assertSucceeds(getDoc(doc(db(), 'recipe_of_the_month/m1')));
    await assertSucceeds(getDoc(doc(db(), 'video_generation_history/r1')));
  });
  it('denies client writes', async () => {
    await assertFails(setDoc(doc(db(ALICE), 'recipe_of_the_month/m2'), { recipeId: 'r1' }));
    await assertFails(setDoc(doc(db(ALICE), 'video_generation_history/r2'), { selectedAt: null }));
  });
});

describe('users/{uid}', () => {
  it('allows owner read, denies others', async () => {
    await seed(`users/${ALICE}`, { betaInteractionCount: 1 });
    await assertSucceeds(getDoc(doc(db(ALICE), `users/${ALICE}`)));
    await assertFails(getDoc(doc(db(BOB), `users/${ALICE}`)));
    await assertFails(getDoc(doc(db(), `users/${ALICE}`)));
  });

  it('allows owner to create with preferences', () =>
    assertSucceeds(setDoc(doc(db(ALICE), `users/${ALICE}`), { preferences: { summary: 'Gillar chili' } })));
  it("denies writing someone else's doc", () =>
    assertFails(setDoc(doc(db(BOB), `users/${ALICE}`), { preferences: { summary: 'x' } })));
  it('denies unknown top-level fields', () =>
    assertFails(setDoc(doc(db(ALICE), `users/${ALICE}`), { isAdmin: true })));
  it('denies deleting the user doc', async () => {
    await seed(`users/${ALICE}`, { betaInteractionCount: 3 });
    await assertFails(deleteDoc(doc(db(ALICE), `users/${ALICE}`)));
  });

  describe('subcollections', () => {
    for (const sub of ['chat_history', 'liked_messages', 'lists']) {
      it(`allows owner read/write on ${sub}, denies others`, async () => {
        await assertSucceeds(setDoc(doc(db(ALICE), `users/${ALICE}/${sub}/x`), { text: 'hej' }));
        await assertSucceeds(getDoc(doc(db(ALICE), `users/${ALICE}/${sub}/x`)));
        await assertFails(getDoc(doc(db(BOB), `users/${ALICE}/${sub}/x`)));
        await assertFails(setDoc(doc(db(BOB), `users/${ALICE}/${sub}/y`), { text: 'hej' }));
      });
    }
    it('denies unknown subcollections', () =>
      assertFails(setDoc(doc(db(ALICE), `users/${ALICE}/secrets/x`), { a: 1 })));
  });

  describe('betaInteractionCount', () => {
    const ref = () => doc(db(ALICE), `users/${ALICE}`);

    it('allows create with count 1', () => assertSucceeds(setDoc(ref(), { betaInteractionCount: 1 })));
    it('denies create with count 2', () => assertFails(setDoc(ref(), { betaInteractionCount: 2 })));
    it('allows first increment on a doc without the field', async () => {
      await seed(`users/${ALICE}`, { preferences: { summary: 'x' } });
      await assertSucceeds(updateDoc(ref(), { betaInteractionCount: increment(1) }));
    });
    it('allows +1 via FieldValue.increment', async () => {
      await seed(`users/${ALICE}`, { betaInteractionCount: 4 });
      await assertSucceeds(updateDoc(ref(), { betaInteractionCount: increment(1) }));
    });
    it('denies +2', async () => {
      await seed(`users/${ALICE}`, { betaInteractionCount: 4 });
      await assertFails(updateDoc(ref(), { betaInteractionCount: increment(2) }));
    });
    it('denies decrement', async () => {
      await seed(`users/${ALICE}`, { betaInteractionCount: 4 });
      await assertFails(updateDoc(ref(), { betaInteractionCount: 3 }));
    });
    it('denies non-numeric value', async () => {
      await seed(`users/${ALICE}`, { betaInteractionCount: 4 });
      await assertFails(updateDoc(ref(), { betaInteractionCount: '5' }));
    });
    it('allows updating preferences while leaving the count untouched', async () => {
      await seed(`users/${ALICE}`, { betaInteractionCount: 4 });
      await assertSucceeds(updateDoc(ref(), { preferences: { summary: 'Vegetariskt' } }));
    });
    it('allows merge-set of preferences', async () => {
      await seed(`users/${ALICE}`, { betaInteractionCount: 4 });
      await assertSucceeds(setDoc(ref(), { preferences: { summary: 'x' } }, { merge: true }));
    });
    it('denies a non-merge set that would wipe the count', async () => {
      await seed(`users/${ALICE}`, { betaInteractionCount: 4 });
      await assertFails(setDoc(ref(), { preferences: { summary: 'x' } }));
    });
  });
});
