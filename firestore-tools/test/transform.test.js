import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { collectionKey, countByCollection, parseIsoDate, pushIdToDate, transform } from '../migration/transform.js';
import {
  CHAT_1, CHAT_2, LIST, RECIPE_A, RECIPE_ORPHAN, ROTM, T1, T2, VARIANT, pushId, rtdbExport
} from './fixture.js';

const byPath = (docs) => Object.fromEntries(docs.map((d) => [d.path, d.data]));

describe('pushIdToDate', () => {
  it('round-trips a generated push ID', () => assert.equal(pushIdToDate(pushId(T1)).getTime(), T1));
  it('decodes a real-looking RTDB key into a plausible year', () => {
    const year = pushIdToDate('-OB2dXa1k2h3j4k5l6m7').getUTCFullYear();
    assert.ok(year >= 2015 && year <= 2030, `year ${year}`);
  });
  it('returns null for non push IDs', () => {
    assert.equal(pushIdToDate('alice'), null);
    assert.equal(pushIdToDate(null), null);
    assert.equal(pushIdToDate('!!!!!!!!abcdefghijkl'), null);
  });
});

describe('parseIsoDate', () => {
  it('parses Python µs + offset', () =>
    assert.equal(parseIsoDate('2025-02-12T13:58:18.650875+00:00').toISOString(), '2025-02-12T13:58:18.650Z'));
  it('parses Kotlin Z and Z[UTC]', () => {
    assert.equal(parseIsoDate('2026-04-05T08:00:00Z').getTime(), T2);
    assert.equal(parseIsoDate('2026-04-05T08:00:00Z[UTC]').getTime(), T2);
  });
  it('returns null for empty or garbage', () => {
    assert.equal(parseIsoDate(''), null);
    assert.equal(parseIsoDate('not a date'), null);
    assert.equal(parseIsoDate(undefined), null);
  });
});

describe('transform', () => {
  const { docs, warnings } = transform(rtdbExport());
  const out = byPath(docs);

  it('keeps RTDB keys as document IDs and produces the expected collections', () => {
    assert.deepEqual(countByCollection(docs), {
      recipes: 2,
      'recipes/variants': 1,
      recipe_of_the_month: 1,
      video_generation_history: 1,
      users: 2,
      'users/chat_history': 2,
      'users/liked_messages': 1,
      'users/lists': 2
    });
  });

  it('maps a full recipe with Timestamp updatedAt', () => {
    const r = out[`recipes/${RECIPE_A}`];
    assert.equal(r.id, RECIPE_A);
    assert.equal(r.uid, 'alice');
    assert.equal(r.updatedAt.toISOString(), '2025-02-12T13:58:18.650Z');
    assert.deepEqual(r.ingredients, [{ name: 'lamm', quantity: '1/2', unit: 'kg' }]);
    assert.deepEqual(r.tags, ['lamb', 'greek']);
  });

  it('normalises an orphaned recipe (null uid, default isFavourite, sparse array)', () => {
    const r = out[`recipes/${RECIPE_ORPHAN}`];
    assert.equal(r.id, RECIPE_ORPHAN);
    assert.equal(r.uid, null);
    assert.equal(r.isFavourite, false);
    assert.equal(r.updatedAt.getTime(), T2);
    assert.deepEqual(r.ingredients.map((i) => i.name), ['salt', 'peppar']);
  });

  it('moves variants under their recipe', () => {
    const v = out[`recipes/${RECIPE_A}/variants/${VARIANT}`];
    assert.equal(v.label, 'Vegansk');
    assert.equal(v.isPinned, true);
    assert.equal(v.createdAt.getTime(), T2);
  });

  it('falls back to push-ID time for an unparseable timestamp, with a warning', () => {
    assert.equal(out[`recipe_of_the_month/${ROTM}`].createdAt.getTime(), T2);
    assert.ok(warnings.some((w) => w.includes(`recipe_of_the_month/${ROTM}`) && w.includes('push-ID')));
  });

  it('maps video_generation_history entries', () =>
    assert.deepEqual(out[`video_generation_history/${RECIPE_A}`], { selectedAt: null }));

  it('builds the user doc from single-object fields', () => {
    const u = out['users/alice'];
    assert.deepEqual(Object.keys(u).sort(), ['betaInteractionCount', 'cooking_resources', 'preferences']);
    assert.equal(u.betaInteractionCount, 7);
    assert.equal(u.preferences.updatedAt.getTime(), T2);
    assert.equal(u.cooking_resources.resources.length, 1);
    assert.deepEqual(out['users/bob'], {});
  });

  it('derives chat createdAt from the push ID', () => {
    assert.equal(out[`users/alice/chat_history/${CHAT_1}`].createdAt.getTime(), T1);
    assert.equal(out[`users/alice/chat_history/${CHAT_2}`].createdAt.getTime(), T2);
    assert.deepEqual(out[`users/alice/chat_history/${CHAT_2}`].parts, [{ text: '**Svar**' }]);
  });

  it('defaults list id and recipeIds', () => {
    assert.deepEqual(out[`users/bob/lists/${LIST}`], { name: 'Tom lista', id: LIST, recipeIds: [] });
    assert.deepEqual(out[`users/alice/lists/${LIST}`].recipeIds, [RECIPE_A]);
  });

  it('warns about unknown nodes instead of silently dropping them', () => {
    const extra = { ...rtdbExport(), legacy: { a: 1 } };
    extra.users.alice.oldField = 1;
    const { warnings: w } = transform(extra);
    assert.ok(w.some((x) => x.includes("unknown root node 'legacy'")));
    assert.ok(w.some((x) => x.includes("users/alice: unknown field 'oldField'")));
  });

  it('only warns about the deliberately broken timestamp in the fixture', () =>
    assert.equal(warnings.length, 1, warnings.join('\n')));
});

describe('collectionKey', () => {
  it('strips document IDs', () => assert.equal(collectionKey('users/u1/chat_history/x'), 'users/chat_history'));
});
