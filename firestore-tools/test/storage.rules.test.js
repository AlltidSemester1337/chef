// Security rule tests for ../storage.rules against the Storage emulator (CHE-62).
// Run: npm test  (starts the emulators via `firebase emulators:exec`)
import { randomUUID } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, it } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { deleteObject, getBytes, ref, updateMetadata, uploadBytes } from 'firebase/storage';

const ALICE = 'alice';
const UUID = '0b7d8a3e-5c1f-4e2a-9d6b-1f2e3d4c5b6a';
const IMAGE = `recipes/${UUID}.jpg`;
const JPEG = { contentType: 'image/jpeg' };
const bytes = (n) => new Uint8Array(n).fill(0xff);

let env;

const storage = (uid) => (uid ? env.authenticatedContext(uid) : env.unauthenticatedContext()).storage();

const seed = (path, data = bytes(10), metadata = JPEG) =>
  env.withSecurityRulesDisabled((ctx) => uploadBytes(ref(ctx.storage(), path), data, metadata));

before(async () => {
  env = await initializeTestEnvironment({
    // "demo-" prefix: the emulator never talks to a real project.
    projectId: 'demo-chef',
    storage: { rules: readFileSync(new URL('../../storage.rules', import.meta.url), 'utf8') }
  });
});

beforeEach(() => env.clearStorage());

after(() => env.cleanup());

describe('recipes/ read', () => {
  it('allows public read of recipe images, including older names', async () => {
    await seed(IMAGE);
    await seed(`recipes/${UUID}`);
    await seed('recipes/1739388695424/sample_0.jpg');
    await assertSucceeds(getBytes(ref(storage(), IMAGE)));
    await assertSucceeds(getBytes(ref(storage(), `recipes/${UUID}`)));
    await assertSucceeds(getBytes(ref(storage(ALICE), 'recipes/1739388695424/sample_0.jpg')));
  });
});

describe('recipes/{uuid}.jpg create', () => {
  // A fresh name per upload: creating at an existing path is an overwrite, which is denied.
  const freshImage = () => `recipes/${randomUUID()}.jpg`;
  const upload = (uid, path = freshImage(), data = bytes(1024), metadata = JPEG) =>
    uploadBytes(ref(storage(uid), path), data, metadata);

  it('allows a signed-in user to upload a JPEG at a UUID name', () => assertSucceeds(upload(ALICE)));
  it('allows up to 5 MiB', () => assertSucceeds(upload(ALICE, freshImage(), bytes(5 * 1024 * 1024))));

  it('denies unauthenticated upload', () => assertFails(upload(null)));
  it('denies a second upload to the same name (no overwrite)', async () => {
    const path = freshImage();
    await assertSucceeds(upload(ALICE, path));
    await assertFails(upload(ALICE, path));
    await assertFails(upload('bob', path));
  });
  it('denies other content types', async () => {
    for (const contentType of ['image/png', 'text/html', 'image/svg+xml', 'application/javascript']) {
      await assertFails(upload(ALICE, freshImage(), bytes(10), { contentType }), contentType);
    }
  });
  it('accepts metadata-less uploads from release 4.2.0 (transitional)', () =>
    assertSucceeds(upload(ALICE, freshImage(), bytes(10), { contentType: 'application/octet-stream' })));
  it('denies more than 5 MiB and empty files', async () => {
    await assertFails(upload(ALICE, freshImage(), bytes(5 * 1024 * 1024 + 1)));
    await assertFails(upload(ALICE, freshImage(), bytes(0)));
  });
  it('denies names other than <uuid>.jpg', async () => {
    for (const path of [
      `recipes/${UUID}`,
      `recipes/${UUID}.png`,
      `recipes/${UUID.toUpperCase()}.jpg`,
      'recipes/evil.jpg',
      `recipes/x/${UUID}.jpg`,
      `recipes/${UUID}.jpg.html`
    ]) {
      await assertFails(upload(ALICE, path), path);
    }
  });
});

describe('recipes/ overwrite and delete', () => {
  beforeEach(() => seed(IMAGE));

  it('denies overwriting an existing image', () => assertFails(uploadBytes(ref(storage(ALICE), IMAGE), bytes(10), JPEG)));
  it('denies changing metadata', () =>
    assertFails(updateMetadata(ref(storage(ALICE), IMAGE), { contentType: 'text/html' })));
  it('denies deleting', async () => {
    await assertFails(deleteObject(ref(storage(ALICE), IMAGE)));
    await assertFails(deleteObject(ref(storage(), IMAGE)));
  });
});

describe('videos/', () => {
  it('allows public read, denies client writes', async () => {
    await seed('videos/rotw/2026-09.mp4', bytes(10), { contentType: 'video/mp4' });
    await assertSucceeds(getBytes(ref(storage(), 'videos/rotw/2026-09.mp4')));
    await assertFails(uploadBytes(ref(storage(ALICE), 'videos/rotw/2026-10.mp4'), bytes(10), { contentType: 'video/mp4' }));
    await assertFails(deleteObject(ref(storage(ALICE), 'videos/rotw/2026-09.mp4')));
  });
});

describe('everything else', () => {
  it('denies read and write outside recipes/ and videos/', async () => {
    await seed('private/x.jpg');
    await assertFails(getBytes(ref(storage(ALICE), 'private/x.jpg')));
    await assertFails(uploadBytes(ref(storage(ALICE), `other/${UUID}.jpg`), bytes(10), JPEG));
    await assertFails(uploadBytes(ref(storage(ALICE), `${UUID}.jpg`), bytes(10), JPEG));
  });
});
