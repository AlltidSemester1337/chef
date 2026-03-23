Use the `chub` CLI (Context Hub) to search and retrieve LLM-optimized docs or skills relevant to the current task.

Do the following:

1. **Identify what to look up**
   - Based on the user's request or the current task, determine what library, API, or topic to search for.
   - If no specific topic is given, ask the user what they want to look up.

2. **Search the registry**
   - Run `chub search "<topic>"` to find relevant docs/skills.
   - If the registry is empty or stale, run `chub update` first, then search again.
   - Use `--json` and `jq` to extract IDs when piping: `chub search "<topic>" --json | jq -r '.results[].id'`

3. **Fetch the relevant doc or skill**
   - Run `chub get <id>` to print the content, optionally with `--lang` (e.g. `--lang py` or `--lang js`) for language-specific variants.
   - Save to a file if it will be referenced repeatedly: `chub get <id> -o .context/<name>.md`
   - Fetch multiple at once if needed: `chub get <id1> <id2> -o .context/`

4. **Apply the content**
   - Read and apply the fetched doc or skill to the current task.
   - If a skill was fetched, follow its instructions.

5. **Annotate if useful**
   - If you learn something worth remembering for future sessions (e.g. a gotcha or correction), save it: `chub annotate <id> "<note>"`
   - Run `chub annotate --list` to view all saved notes.

6. **Create a skill if beneficial**
   - If a fetched doc or the current workflow would benefit from a reusable Claude Code skill, create one at `.claude/commands/<name>.md`.
   - Always do this when the user explicitly requests it, and proactively when the fetched content describes a repeatable process (e.g. a deployment flow, a testing pattern, an API integration checklist) that would be valuable to invoke again in future sessions.
