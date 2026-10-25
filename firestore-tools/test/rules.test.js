// Security rule tests for ../firestore.rules against the Firestore emulator (CHE-50).
// Run: npm test  (starts the emulator via `firebase emulators:exec`)
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, it } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment
} from '@firebase/rules-unit-testing';
import {
  deleteDoc,
  deleteField,
  doc,
  getDoc,
  increment,
  setDoc,
  Timestamp,
  updateDoc
} from 'firebase/firestore';

const ALICE = 'alice';
const BOB = 'bob';

let env;

const db = (uid) => (uid ? env.authenticatedContext(uid) : env.unauthenticatedContext()).firestore();

const seed = (path, data) =>
  env.withSecurityRulesDisabled((ctx) => setDoc(doc(ctx.firestore(), path), data));

before(async () => {
  env = await initializeTestEnvironment({
    // "demo-" prefix: the emulator never talks to a real project.
    projectId: 'demo-chef',
    firestore: { rules: readFileSync(new URL('../../firestore.rules', import.meta.url), 'utf8') }
  });
});

beforeEach(() => env.clearFirestore());

after(() => env.cleanup());

const BUCKET = 'idyllic-bloom-425307-r6.firebasestorage.app';
const UUID = '0b7d8a3e-5c1f-4e2a-9d6b-1f2e3d4c5b6a';
const IMAGE_URL = `https://firebasestorage.googleapis.com/v0/b/${BUCKET}/o/recipes%2F${UUID}.jpg?alt=media`;

// Mirrors what the Android app writes for a new recipe (RecipeRepositoryImpl.saveRecipe):
// every Kotlin property is present, unset ones as null.
const recipe = (uid, extra = {}) => ({
  id: 'r1',
  uid,
  title: 'Soppa',
  summary: 'En god soppa',
  servings: '4',
  prepTime: '10 min',
  cookingTime: '30 min',
  nutrientsPerServing: [{ name: 'Calories', quantity: '300', unit: 'kcal' }],
  ingredients: [{ name: 'Morot', quantity: '2', unit: 'st' }],
  difficulty: 'EASY',
  instructions: ['Koka', 'Mixa'],
  tipsAndTricks: null,
  imageUrl: IMAGE_URL,
  videoUrl: null,
  isFavourite: true,
  copyId: null,
  tags: ['vegetarian'],
  updatedAt: Timestamp.fromMillis(1_760_000_000_000),
  ...extra
});

const ADMIN = 'admin';
const adminDb = () => env.authenticatedContext(ADMIN, { admin: true }).firestore();

describe('recipes', () => {
  const create = (uid, extra) => setDoc(doc(db(uid), 'recipes/r1'), recipe(uid, extra));
  const createAs = (uid, data) => setDoc(doc(db(uid), 'recipes/r1'), data);

  describe('read', () => {
    beforeEach(() => seed('recipes/r1', recipe(ALICE)));
    it('denies unauthenticated read', () => assertFails(getDoc(doc(db(), 'recipes/r1'))));
    it("allows any signed-in user to read others' recipes", () =>
      assertSucceeds(getDoc(doc(db(BOB), 'recipes/r1'))));
  });

  describe('create', () => {
    it('allows the app-shaped recipe with own uid', () => assertSucceeds(create(BOB)));
    it('allows a recipe without image (generation failed)', () => assertSucceeds(create(BOB, { imageUrl: null })));
    it('allows the plain GCS form of the image URL', () =>
      assertSucceeds(create(BOB, { imageUrl: `https://storage.googleapis.com/${BUCKET}/recipes/${UUID}.jpg` })));
    it('allows difficulty HARD and null', async () => {
      await assertSucceeds(create(BOB, { difficulty: 'HARD' }));
      await env.clearFirestore();
      await assertSucceeds(create(BOB, { difficulty: null }));
    });
    it('allows omitting optional fields', () => {
      const { servings, prepTime, cookingTime, nutrientsPerServing, difficulty, tipsAndTricks, imageUrl, videoUrl, copyId, ...minimal } =
        recipe(BOB);
      return assertSucceeds(createAs(BOB, minimal));
    });

    it('denies unauthenticated create', () => assertFails(createAs(null, recipe(BOB))));
    it("denies create with someone else's uid", () => assertFails(create(BOB, { uid: ALICE })));
    it('denies create without owner (null or absent uid)', async () => {
      await assertFails(create(BOB, { uid: null }));
      const { uid, ...orphan } = recipe(BOB);
      await assertFails(createAs(BOB, orphan));
    });
    it('denies id different from the document ID', () => assertFails(create(BOB, { id: 'other' })));
    it('denies unknown fields', () => assertFails(create(BOB, { isAdmin: true })));
    for (const key of ['title', 'summary', 'ingredients', 'instructions', 'isFavourite', 'tags', 'updatedAt']) {
      it(`denies missing required field ${key}`, () => {
        const { [key]: _, ...rest } = recipe(BOB);
        return assertFails(createAs(BOB, rest));
      });
    }
    it('denies empty title', () => assertFails(create(BOB, { title: '' })));

    it('denies wrong field types', async () => {
      const wrong = {
        title: 42,
        summary: ['x'],
        servings: 4,
        prepTime: true,
        cookingTime: { min: 30 },
        tipsAndTricks: ['tip'],
        copyId: 1,
        ingredients: 'Morot',
        instructions: 'Koka',
        nutrientsPerServing: 'many',
        tags: 'vegan',
        difficulty: 'IMPOSSIBLE',
        isFavourite: 'yes',
        updatedAt: '2026-10-25T10:00:00Z'
      };
      for (const [key, value] of Object.entries(wrong)) {
        await assertFails(create(BOB, { [key]: value }), `${key} = ${JSON.stringify(value)}`);
      }
    });

    it('denies oversized fields', async () => {
      const long = (n) => 'x'.repeat(n);
      const many = (n) => Array.from({ length: n }, () => 'x');
      const oversized = {
        title: long(201),
        summary: long(2001),
        servings: long(101),
        prepTime: long(101),
        cookingTime: long(101),
        tipsAndTricks: long(5001),
        copyId: long(129),
        ingredients: many(101),
        instructions: many(101),
        nutrientsPerServing: many(51),
        tags: many(31)
      };
      for (const [key, value] of Object.entries(oversized)) {
        await assertFails(create(BOB, { [key]: value }), key);
      }
    });
    it('allows fields at the size limits', () =>
      assertSucceeds(create(BOB, {
        title: 'x'.repeat(200),
        summary: 'x'.repeat(2000),
        tipsAndTricks: 'x'.repeat(5000),
        ingredients: Array.from({ length: 100 }, () => ({ name: 'x', quantity: '1', unit: 'g' })),
        tags: Array.from({ length: 30 }, () => 'x')
      })));

    it('denies a client-set videoUrl', () =>
      assertFails(create(BOB, { videoUrl: `https://storage.googleapis.com/${BUCKET}/videos/x.mp4` })));
    it('denies image URLs outside our recipe image path', async () => {
      const bad = [
        'https://evil.example/recipes%2F' + UUID + '.jpg?alt=media',
        `https://firebasestorage.googleapis.com/v0/b/other-project.firebasestorage.app/o/recipes%2F${UUID}.jpg?alt=media`,
        `https://firebasestorage.googleapis.com/v0/b/${BUCKET}/o/videos%2F${UUID}.jpg?alt=media`,
        `https://firebasestorage.googleapis.com/v0/b/${BUCKET}/o/recipes%2F${UUID}.png?alt=media`,
        `https://firebasestorage.googleapis.com/v0/b/${BUCKET}/o/recipes%2F..%2F${UUID}.jpg?alt=media`,
        `${IMAGE_URL}&token=abc`,
        `http://storage.googleapis.com/${BUCKET}/recipes/${UUID}.jpg`,
        `https://storage.googleapis.com/${BUCKET}/recipes/1739388695424/sample_0.jpg`,
        '',
        42
      ];
      for (const imageUrl of bad) {
        await assertFails(create(BOB, { imageUrl }), String(imageUrl));
      }
    });
  });

  describe('update', () => {
    beforeEach(() => seed('recipes/r1', recipe(ALICE)));
    const update = (uid, data) => updateDoc(doc(db(uid), 'recipes/r1'), data);

    it('allows owner update', () => assertSucceeds(update(ALICE, { title: 'Gryta' })));
    it('allows owner re-saving the whole recipe (set on an existing id)', () =>
      assertSucceeds(setDoc(doc(db(ALICE), 'recipes/r1'), recipe(ALICE, { isFavourite: false }))));
    it('denies non-owner update', () => assertFails(update(BOB, { title: 'Gryta' })));
    it('denies invalid values on update', async () => {
      await assertFails(update(ALICE, { title: 42 }));
      await assertFails(update(ALICE, { tags: 'x' }));
      await assertFails(update(ALICE, { extra: 1 }));
      await assertFails(update(ALICE, { id: 'other' }));
      await assertFails(update(ALICE, { imageUrl: 'https://evil.example/x.jpg' }));
    });
    it('allows replacing the image with a valid URL or clearing it', async () => {
      const other = IMAGE_URL.replace(UUID, '11111111-2222-3333-4444-555555555555');
      await assertSucceeds(update(ALICE, { imageUrl: other }));
      await assertSucceeds(update(ALICE, { imageUrl: null }));
    });
    it('denies the owner changing videoUrl', async () => {
      await assertFails(update(ALICE, { videoUrl: 'https://evil.example/v.mp4' }));
      await seed('recipes/r1', recipe(ALICE, { videoUrl: 'https://storage.googleapis.com/b/videos/v.mp4' }));
      await assertFails(update(ALICE, { videoUrl: null }));
      await assertSucceeds(update(ALICE, { title: 'Gryta' })); // unchanged videoUrl is fine
    });

    it('allows owner to orphan by deleting the uid field', () =>
      assertSucceeds(update(ALICE, { uid: deleteField() })));
    it('denies orphaning by setting uid to null (Recipe.uid is non-null in the app)', () =>
      assertFails(update(ALICE, { uid: null })));
    it('denies orphaning combined with other changes', () =>
      assertFails(update(ALICE, { uid: deleteField(), title: 'x' })));
    it('denies owner transferring to another uid', () => assertFails(update(ALICE, { uid: BOB })));
    it('denies everyone writing an orphaned recipe', async () => {
      const { uid, ...orphan } = recipe(ALICE);
      await seed('recipes/orphan', { ...orphan, id: 'orphan' });
      await assertFails(updateDoc(doc(db(ALICE), 'recipes/orphan'), { title: 'x' }));
      await assertFails(updateDoc(doc(db(BOB), 'recipes/orphan'), { uid: BOB }));
      await assertFails(updateDoc(doc(adminDb(), 'recipes/orphan'), { title: 'x' }));
    });

    it('accepts legacy documents: older image URL formats and missing optional fields', async () => {
      const { instructions, prepTime, tipsAndTricks, nutrientsPerServing, videoUrl, copyId, ...legacy } = recipe(ALICE, {
        imageUrl: `https://storage.googleapis.com/${BUCKET}/recipes/1739388695424/sample_0.jpg`
      });
      await seed('recipes/r1', legacy);
      await assertSucceeds(update(ALICE, { isFavourite: false }));
      await assertSucceeds(update(ALICE, { uid: deleteField() }));
    });
  });

  describe('delete', () => {
    beforeEach(() => seed('recipes/r1', recipe(ALICE)));
    it('allows owner delete', () => assertSucceeds(deleteDoc(doc(db(ALICE), 'recipes/r1'))));
    it('denies non-owner delete', () => assertFails(deleteDoc(doc(db(BOB), 'recipes/r1'))));
    it('allows admin to delete any recipe, including orphans', async () => {
      const { uid, ...orphan } = recipe(ALICE);
      await seed('recipes/orphan', { ...orphan, id: 'orphan' });
      await assertSucceeds(deleteDoc(doc(adminDb(), 'recipes/orphan')));
      await assertSucceeds(deleteDoc(doc(adminDb(), 'recipes/r1')));
    });
    it('denies a non-admin claim value', () =>
      assertFails(deleteDoc(doc(env.authenticatedContext(BOB, { admin: 'true' }).firestore(), 'recipes/r1'))));
  });
});

// Mirrors RecipeVariantRepositoryImpl.saveVariant.
const variant = (id, extra = {}) => ({
  id,
  label: 'Vegansk',
  createdAt: Timestamp.fromMillis(1_760_000_000_000),
  isPinned: false,
  title: 'Soppa',
  summary: 'En god soppa',
  servings: '4',
  prepTime: null,
  cookingTime: '30 min',
  nutrientsPerServing: null,
  ingredients: [{ name: 'Morot', quantity: '2', unit: 'st' }],
  difficulty: 'EASY',
  instructions: ['Koka'],
  tipsAndTricks: null,
  ...extra
});

describe('recipes/{id}/variants', () => {
  beforeEach(async () => {
    await seed('recipes/r1', recipe(ALICE));
    await seed('recipes/r1/variants/v1', variant('v1'));
  });
  const ref = (uid, id = 'v2', recipeId = 'r1') => doc(db(uid), `recipes/${recipeId}/variants/${id}`);

  it('allows any signed-in user to read', () => assertSucceeds(getDoc(ref(BOB, 'v1'))));
  it('denies unauthenticated read', () => assertFails(getDoc(ref(null, 'v1'))));

  it('allows recipe owner to create an app-shaped variant', () => assertSucceeds(setDoc(ref(ALICE), variant('v2'))));
  it('denies non-owner create', () => assertFails(setDoc(ref(BOB), variant('v2'))));
  it('denies create when the parent recipe does not exist', () =>
    assertFails(setDoc(ref(ALICE, 'v1', 'missing'), variant('v1'))));
  it('denies invalid variants', async () => {
    const { label, ...noLabel } = variant('v2');
    await assertFails(setDoc(ref(ALICE), noLabel));
    await assertFails(setDoc(ref(ALICE), variant('v2', { id: 'other' })));
    await assertFails(setDoc(ref(ALICE), variant('v2', { extra: 1 })));
    await assertFails(setDoc(ref(ALICE), variant('v2', { label: 'x'.repeat(101) })));
    await assertFails(setDoc(ref(ALICE), variant('v2', { ingredients: 'Morot' })));
    await assertFails(setDoc(ref(ALICE), variant('v2', { instructions: Array.from({ length: 101 }, () => 'x') })));
    await assertFails(setDoc(ref(ALICE), variant('v2', { difficulty: 'IMPOSSIBLE' })));
    await assertFails(setDoc(ref(ALICE), variant('v2', { createdAt: 'yesterday' })));
  });

  it('allows recipe owner to pin and delete', async () => {
    await assertSucceeds(updateDoc(ref(ALICE, 'v1'), { isPinned: true }));
    await assertSucceeds(deleteDoc(ref(ALICE, 'v1')));
  });
  it('denies updating anything but isPinned', async () => {
    await assertFails(updateDoc(ref(ALICE, 'v1'), { title: 'Annan' }));
    await assertFails(updateDoc(ref(ALICE, 'v1'), { isPinned: 'yes' }));
  });
  it('denies non-owner pin and delete', async () => {
    await assertFails(updateDoc(ref(BOB, 'v1'), { isPinned: true }));
    await assertFails(deleteDoc(ref(BOB, 'v1')));
  });
  it('allows admin to delete variants', () =>
    assertSucceeds(deleteDoc(doc(adminDb(), 'recipes/r1/variants/v1'))));
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

    // Server-owned: chef-backend increments it with the Admin SDK, which bypasses the rules.
    it('denies create with a count', () => assertFails(setDoc(ref(), { betaInteractionCount: 1 })));
    it('denies create with count 0', () => assertFails(setDoc(ref(), { betaInteractionCount: 0 })));
    it('denies adding the field to a doc without it', async () => {
      await seed(`users/${ALICE}`, { preferences: { summary: 'x' } });
      await assertFails(updateDoc(ref(), { betaInteractionCount: increment(1) }));
    });
    it('denies +1 via FieldValue.increment', async () => {
      await seed(`users/${ALICE}`, { betaInteractionCount: 4 });
      await assertFails(updateDoc(ref(), { betaInteractionCount: increment(1) }));
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
    it('allows writing the unchanged count back', async () => {
      await seed(`users/${ALICE}`, { betaInteractionCount: 4 });
      await assertSucceeds(setDoc(ref(), { betaInteractionCount: 4, preferences: { summary: 'x' } }));
    });
    it('denies a non-merge set that would wipe the count', async () => {
      await seed(`users/${ALICE}`, { betaInteractionCount: 4 });
      await assertFails(setDoc(ref(), { preferences: { summary: 'x' } }));
    });
  });
});
