// Synthetic RTDB export covering every node type and the edge cases seen in real data.
const PUSH_CHARS = '-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz';

/** Builds a valid RTDB push ID for a given time (inverse of pushIdToDate). */
export function pushId(millis, suffix = 'abcdefghijkl') {
  let time = '';
  for (let i = 0; i < 8; i++) {
    time = PUSH_CHARS[millis % 64] + time;
    millis = Math.floor(millis / 64);
  }
  return time + suffix;
}

export const T1 = Date.UTC(2025, 1, 12, 13, 58, 18, 650);
export const T2 = Date.UTC(2026, 3, 5, 8, 0, 0, 0);

export const RECIPE_A = pushId(T1, 'recipeAAAAAA');
export const RECIPE_ORPHAN = pushId(T1, 'recipeORPHAN');
export const VARIANT = pushId(T2, 'variantVVVVV');
export const CHAT_1 = pushId(T1, 'chat11111111');
export const CHAT_2 = pushId(T2, 'chat22222222');
export const LIKED = pushId(T2, 'likedLLLLLLL');
export const LIST = pushId(T2, 'listLLLLLLLL');
export const ROTM = pushId(T2, 'rotmRRRRRRRR');

export function rtdbExport() {
  return {
    recipes: {
      [RECIPE_A]: {
        id: RECIPE_A,
        uid: 'alice',
        title: 'Moussaka',
        summary: '',
        difficulty: 'MEDIUM',
        cookingTime: '1-2 hours',
        prepTime: '',
        servings: '4 servings',
        imageUrl: 'https://storage.googleapis.com/x/recipes/a.png',
        isFavourite: true,
        tipsAndTricks: '',
        updatedAt: '2025-02-12T13:58:18.650875+00:00', // Python format, µs precision
        ingredients: [{ name: 'lamm', quantity: '1/2', unit: 'kg' }],
        instructions: ['Bryn', 'Grädda'],
        nutrientsPerServing: [{ name: 'Calories', quantity: '550', unit: 'kcal' }],
        tags: ['lamb', 'greek']
      },
      [RECIPE_ORPHAN]: {
        // uid removed (orphaned), no tags/isFavourite, sparse array, Kotlin timestamp format
        title: 'Föräldralös',
        updatedAt: '2026-04-05T08:00:00Z[UTC]',
        ingredients: { 0: { name: 'salt', quantity: '1', unit: 'tsp' }, 2: { name: 'peppar', quantity: '', unit: '' } },
        instructions: ['Salta']
      }
    },
    recipe_variants: {
      [RECIPE_A]: {
        [VARIANT]: {
          id: VARIANT,
          label: 'Vegansk',
          createdAt: '2026-04-05T08:00:00Z',
          isPinned: true,
          title: 'Vegansk moussaka',
          summary: '',
          ingredients: [{ name: 'linser', quantity: '400', unit: 'g' }],
          instructions: ['Koka']
        }
      }
    },
    recipe_of_the_month: {
      [ROTM]: { recipeId: RECIPE_A, recipeTitle: 'Moussaka', videoUrl: 'https://v', monthOf: '2026-04', createdAt: 'not a date' }
    },
    video_generation_history: { [RECIPE_A]: true },
    users: {
      alice: {
        betaInteractionCount: 7,
        preferences: { summary: 'Gillar chili', updatedAt: '2026-04-05T08:00:00Z' },
        cooking_resources: {
          resources: [{ title: 'Serious Eats', url: 'https://x', type: 'site', description: 'd' }],
          updatedAt: '2026-04-05T08:00:00Z'
        },
        chat_history: {
          [CHAT_1]: { role: 'user', parts: [{ text: 'Hej' }] },
          [CHAT_2]: { role: 'model', parts: [{ text: '**Svar**' }] }
        },
        liked_messages: { [LIKED]: { text: '**Svar**', likedAt: '2026-04-05T08:00:00Z' } },
        lists: { [LIST]: { id: LIST, name: 'Vardag', recipeIds: [RECIPE_A] } }
      },
      bob: {
        lists: { [LIST]: { name: 'Tom lista' } } // no id, no recipeIds
      }
    }
  };
}
