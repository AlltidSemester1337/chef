#!/usr/bin/env node
// RTDB export → Firestore migration CLI (CHE-50). Usage in firestore-tools/README.md.
import { readFileSync } from 'node:fs';
import { parseArgs } from 'node:util';
import { connect, verify, writeDocs } from './firestore-io.js';
import { countByCollection, transform } from './transform.js';

const { values: args } = parseArgs({
  options: {
    export: { type: 'string' },
    project: { type: 'string' },
    apply: { type: 'boolean', default: false },
    verify: { type: 'boolean', default: false },
    sample: { type: 'string', default: '25' },
    full: { type: 'boolean', default: false }
  }
});

if (!args.export || !args.project) {
  console.error('Usage: node migration/migrate.js --export <rtdb-export.json> --project <id> [--apply] [--verify [--sample N | --full]]');
  process.exit(2);
}

const target = process.env.FIRESTORE_EMULATOR_HOST
  ? `EMULATOR ${process.env.FIRESTORE_EMULATOR_HOST} (project ${args.project})`
  : `PRODUCTION project ${args.project}`;

const { docs, warnings } = transform(JSON.parse(readFileSync(args.export, 'utf8')));

console.log(`Source: ${args.export}`);
console.log(`Target: ${target}`);
console.log(`Mode:   ${args.apply ? 'APPLY' : 'dry-run'}${args.verify ? ' + verify' : ''}\n`);
console.log('Documents per collection:');
for (const [key, count] of Object.entries(countByCollection(docs))) console.log(`  ${key.padEnd(28)} ${count}`);
console.log(`  ${'TOTAL'.padEnd(28)} ${docs.length}\n`);

if (warnings.length) {
  console.log(`Warnings (${warnings.length}):`);
  for (const warning of warnings) console.log(`  - ${warning}`);
  console.log();
}

if (!args.apply && !args.verify) {
  console.log('Dry-run only — nothing written. Re-run with --apply to write.');
  process.exit(0);
}

const db = connect(args.project);

if (args.apply) {
  const written = await writeDocs(db, docs);
  console.log(`Wrote ${written} documents.\n`);
}

if (args.verify) {
  const sampleSize = args.full ? Infinity : Number(args.sample);
  const result = await verify(db, docs, sampleSize);
  console.log(`Verified counts for ${Object.keys(result.counts.expected).length} collections and ${result.checked} documents.`);
  if (result.problems.length) {
    console.log(`FAILED (${result.problems.length} problems):`);
    for (const problem of result.problems) console.log(`  - ${problem}`);
    process.exit(1);
  }
  console.log('OK — counts match and all checked documents are identical.');
}
