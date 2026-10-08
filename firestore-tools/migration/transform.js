// Pure RTDB-export → Firestore document mapping (CHE-50). No I/O; see .ai/firestore-schema.md.
// Timestamps are returned as JS Dates — the Admin SDK stores them as Firestore Timestamps.

const PUSH_CHARS = '-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz';

const KNOWN_ROOT_NODES = ['recipes', 'recipe_variants', 'recipe_of_the_month', 'video_generation_history', 'users'];
const USER_DOC_FIELDS = ['betaInteractionCount', 'preferences', 'cooking_resources'];
const USER_SUBCOLLECTIONS = ['chat_history', 'liked_messages', 'lists'];

/** RTDB push IDs start with 8 chars encoding the creation time in ms. Returns null for non-push IDs. */
export function pushIdToDate(pushId) {
  if (typeof pushId !== 'string' || pushId.length !== 20) return null;
  let millis = 0;
  for (const ch of pushId.slice(0, 8)) {
    const value = PUSH_CHARS.indexOf(ch);
    if (value === -1) return null;
    millis = millis * 64 + value;
  }
  return new Date(millis);
}

/**
 * Parses the ISO variants found in RTDB: Python `…650875+00:00` (µs, truncated to ms) and
 * Kotlin `ZonedDateTime.toString()` `…Z` / `…Z[UTC]`. Returns null for empty/unparseable input.
 */
export function parseIsoDate(value) {
  if (typeof value !== 'string' || value.trim() === '') return null;
  const date = new Date(value.trim().replace(/\[[^\]]*\]$/, ''));
  return Number.isNaN(date.getTime()) ? null : date;
}

/** RTDB exports sparse arrays as {"0": …, "2": …}; normalise those (and null) to arrays. */
function toArray(value) {
  if (value == null) return [];
  if (Array.isArray(value)) return value.filter((v) => v != null);
  if (typeof value === 'object' && Object.keys(value).every((k) => /^\d+$/.test(k))) {
    return Object.keys(value)
      .sort((a, b) => Number(a) - Number(b))
      .map((k) => value[k])
      .filter((v) => v != null);
  }
  return value;
}

function arrayFields(obj, fields) {
  const out = { ...obj };
  for (const field of fields) {
    if (field in out) out[field] = toArray(out[field]);
  }
  return out;
}

/** ISO value → Date; falls back to the push-ID time of `key`, else null. Every fallback is reported. */
function timestamp(value, key, context, warnings) {
  const parsed = parseIsoDate(value);
  if (parsed) return parsed;
  const fallback = pushIdToDate(key);
  const outcome = fallback ? 'used push-ID time' : 'set to null';
  if (value === undefined || value === null || value === '') {
    if (!fallback) warnings.push(`${context}: missing, ${outcome}`);
  } else {
    warnings.push(`${context}: unparseable ${JSON.stringify(value)}, ${outcome}`);
  }
  return fallback;
}

function entries(node) {
  return node && typeof node === 'object' ? Object.entries(node) : [];
}

/**
 * @param {object} rtdb Parsed `firebase database:get /` export.
 * @returns {{ docs: {path: string, data: object}[], warnings: string[] }}
 */
export function transform(rtdb) {
  const docs = [];
  const warnings = [];
  const add = (path, data) => docs.push({ path, data });

  for (const key of Object.keys(rtdb ?? {})) {
    if (!KNOWN_ROOT_NODES.includes(key)) warnings.push(`unknown root node '${key}' was NOT migrated`);
  }

  for (const [id, raw] of entries(rtdb.recipes)) {
    const recipe = arrayFields(raw, ['ingredients', 'instructions', 'nutrientsPerServing', 'tags']);
    add(`recipes/${id}`, {
      ...recipe,
      id,
      uid: recipe.uid ?? null,
      isFavourite: recipe.isFavourite ?? false,
      updatedAt: timestamp(raw.updatedAt, id, `recipes/${id}.updatedAt`, warnings)
    });
  }

  for (const [recipeId, variants] of entries(rtdb.recipe_variants)) {
    if (!rtdb.recipes?.[recipeId]) warnings.push(`recipe_variants/${recipeId}: parent recipe missing (migrated anyway)`);
    for (const [variantId, raw] of entries(variants)) {
      const path = `recipes/${recipeId}/variants/${variantId}`;
      add(path, {
        ...arrayFields(raw, ['ingredients', 'instructions', 'nutrientsPerServing']),
        id: variantId,
        isPinned: raw.isPinned ?? false,
        createdAt: timestamp(raw.createdAt, variantId, `${path}.createdAt`, warnings)
      });
    }
  }

  for (const [id, raw] of entries(rtdb.recipe_of_the_month)) {
    add(`recipe_of_the_month/${id}`, {
      ...raw,
      createdAt: timestamp(raw.createdAt, id, `recipe_of_the_month/${id}.createdAt`, warnings)
    });
  }

  for (const [recipeId] of entries(rtdb.video_generation_history)) {
    add(`video_generation_history/${recipeId}`, { selectedAt: null });
  }

  for (const [uid, user] of entries(rtdb.users)) {
    for (const key of Object.keys(user)) {
      if (!USER_DOC_FIELDS.includes(key) && !USER_SUBCOLLECTIONS.includes(key)) {
        warnings.push(`users/${uid}: unknown field '${key}' was NOT migrated`);
      }
    }

    const userDoc = {};
    if (user.betaInteractionCount !== undefined) userDoc.betaInteractionCount = user.betaInteractionCount;
    if (user.preferences) {
      userDoc.preferences = {
        ...user.preferences,
        updatedAt: timestamp(user.preferences.updatedAt, null, `users/${uid}.preferences.updatedAt`, warnings)
      };
    }
    if (user.cooking_resources) {
      userDoc.cooking_resources = {
        ...user.cooking_resources,
        resources: toArray(user.cooking_resources.resources),
        updatedAt: timestamp(
          user.cooking_resources.updatedAt, null, `users/${uid}.cooking_resources.updatedAt`, warnings
        )
      };
    }
    add(`users/${uid}`, userDoc);

    for (const [id, raw] of entries(user.chat_history)) {
      const path = `users/${uid}/chat_history/${id}`;
      const createdAt = pushIdToDate(id);
      if (createdAt === null) warnings.push(`${path}: key is not a push ID, createdAt unknown`);
      add(path, { ...raw, parts: toArray(raw.parts), createdAt });
    }

    for (const [id, raw] of entries(user.liked_messages)) {
      const path = `users/${uid}/liked_messages/${id}`;
      add(path, { ...raw, likedAt: timestamp(raw.likedAt, id, `${path}.likedAt`, warnings) });
    }

    for (const [id, raw] of entries(user.lists)) {
      add(`users/${uid}/lists/${id}`, { ...raw, id, recipeIds: toArray(raw.recipeIds) });
    }
  }

  return { docs, warnings };
}

/** "users/u1/chat_history/x" → "users/chat_history" — used for per-collection counts. */
export function collectionKey(path) {
  return path.split('/').filter((_, i) => i % 2 === 0).join('/');
}

export function countByCollection(docs) {
  const counts = {};
  for (const { path } of docs) {
    const key = collectionKey(path);
    counts[key] = (counts[key] ?? 0) + 1;
  }
  return counts;
}
