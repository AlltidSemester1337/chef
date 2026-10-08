# firestore-tools

Firestore security-rule tests and the RTDB → Firestore migration script (CHE-50). Data model: `.ai/firestore-schema.md`.

```bash
cd firestore-tools
npm install
npm test            # rules + migration tests in the Firestore emulator (needs Java 11+)
npm run test:unit   # transform tests only, no emulator
```

`firebase-tools` is a local devDependency (pinned to 13.x for Node 18), so use `npx firebase …` from this folder.

## Migration

Input is an RTDB export (`firebase database:get /`). Output goes to Firestore via ADC — no key files:

```bash
gcloud auth application-default login
```

| Command | Effect |
|---|---|
| `npm run migrate -- --export <file> --project <id>` | Dry-run: per-collection counts + warnings, writes nothing |
| `… --apply` | Writes all documents (`set()` without merge, IDs = RTDB keys → safe to re-run, no duplicates) |
| `… --verify [--sample N \| --full]` | Compares counts per collection and deep-compares N random (default 25) or all documents |
| `… --apply --verify --full` | Typical cut-over run |

Set `FIRESTORE_EMULATOR_HOST=127.0.0.1:8080` to target a running emulator instead of production.

Notes:
- Every lossy step is reported as a warning (unknown nodes/fields, unparseable timestamps). Read them before `--apply`.
- Re-running never deletes: documents that exist only in Firestore are left alone and show up as count mismatches in `--verify`.
- Export files contain user data — keep them **outside the repo** (e.g. `~/Downloads/` or `~/chef-backups/`), never in the working tree, so they can't end up in a commit or the APK. Delete them after the cut-over.
