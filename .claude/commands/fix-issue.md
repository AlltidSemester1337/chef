Fix a reported GitHub issue (bug, UI/UX feedback, beta-tester report) in a worktree, then open a PR for review with a manual test plan.

Usage: /fix-issue <github-issue-number-or-url>  (e.g. /fix-issue 44)

This is the issue-driven sibling of `/implement-feature` (which works from Linear feature issues). Reported issues are often short, informal, written in Swedish, and carry a screenshot — so the emphasis here is on understanding the report precisely before touching code.

---

## 1. Read and analyze the issue

- Resolve owner/repo from `git remote get-url origin`. Use the GitHub MCP tools (`mcp__github__issue_read`), never the `gh` CLI (it is unauthenticated).
- Fetch the issue (`method: get`), its comments (`method: get_comments`) and labels. A `triaged` label plus an **Implementation plan** comment means a plan was already agreed — follow it unless the code contradicts it.
- **Look at every attached screenshot.** Attachment URLs (`https://github.com/user-attachments/assets/...`) can be downloaded with `curl -sL -o <scratchpad>/issueN.jpg <url>` and viewed with `Read`. The screenshot usually identifies the exact screen/component better than the text.
- Restate the problem in English as a short list of concrete, checkable symptoms (e.g. "no close (X) in top-right corner", "bottom padding larger than top padding").
- Distinguish **bug** (behaviour is wrong — needs reproduction and a regression test) from **UI/UX polish** (layout/visual — verified manually on device).

## 2. Clarify before implementing

- If a symptom is ambiguous, the expected behaviour is unclear, or the fix has more than one reasonable interpretation that changes the outcome, ask with `AskUserQuestion` before writing code. Don't ask about things the issue, screenshot, triage comment, or codebase already answer.
- If the issue turns out to need no code change (not reproducible, already fixed on `main`, config/data problem), report that to the user and propose a comment on the issue instead of opening a PR.

## 3. Analyze the codebase and locate the root cause

- Read the `.ai/` docs (mandatory per `CLAUDE.md`) and `.ai/frontend-architecture.md` for UI issues (design tokens — never hardcode hex colours).
- Grep for visible strings from the screenshot (titles, button labels, `contentDescription`) to find the composable/class responsible.
- Find the **root cause**, not just the symptom site. Check whether sibling components share the same defect (e.g. other dialogs built the same way) and mention them — fix them only if in scope or the user agrees.
- Reuse existing patterns (e.g. the close-icon `IconButton` in `AddToListScreen`) rather than inventing new ones.
- Keep the fix minimal and confined to what the issue asks for.

## 4. Worktree

- If the session is already in a harness-provided worktree (`.claude/worktrees/<name>`, see the environment note), work there — **do not** create a nested `.trees/` worktree.
- Otherwise create one: `git worktree add .trees/<branch> -b fix/issue-<N>-<slug>` from an up-to-date `main`.
- Copy the gitignored files needed to build from the main checkout (`/home/kalle/code/chef`): `local.properties`, `vertexai/app/google-services.json`, and `vertexai/app/src/main/assets/{gcp.json,imagen-google-services.json,chat_system_prompt.txt}`. Verify `chat_system_prompt.txt` exists — the app crashes on launch without it. Never commit these files.

## 5. Tests first, then fix

Follow the testing policy in `CLAUDE.md`:
- **Logic bugs** (models, ViewModels, UiState, parsers, formatters): write a failing unit test that reproduces the reported behaviour, then fix until green. Extract pure logic from composables/ViewModels into testable functions where that makes a real regression test possible.
- **UI/UX issues**: prefer a small Compose UI test in `androidTest` (`createComposeRule`, `androidx.compose.ui:ui-test-junit4` is already a dependency) asserting the behaviour that was reported missing (e.g. close button exists and invokes `onDismiss`). Pure spacing/visual fixes are verified in the manual test plan instead — don't write brittle pixel assertions.
- Never add automated tests that call AI model APIs (Gemini/Imagen/Berget.ai).

## 6. Verify locally (pre-commit)

Always prefix Gradle with `JAVA_HOME=/home/kalle/.jdks/jdk-17.0.12` and run from inside the worktree:

```bash
JAVA_HOME=/home/kalle/.jdks/jdk-17.0.12 ./gradlew ktlintCheck --rerun-tasks
JAVA_HOME=/home/kalle/.jdks/jdk-17.0.12 ./gradlew :vertexai:app:assembleDebug
JAVA_HOME=/home/kalle/.jdks/jdk-17.0.12 ./gradlew :vertexai:app:testDebugUnitTest
```

If `androidTest` files were added or changed, also compile them:

```bash
JAVA_HOME=/home/kalle/.jdks/jdk-17.0.12 ./gradlew :vertexai:app:compileDebugAndroidTestKotlin
```

All must pass. Fix lint with `./gradlew ktlintFormat` if needed and re-run.

## 7. Commit

- Stage only relevant files (`git status` first — no secrets, no gitignored assets).
- Conventional commit: `fix(<scope>): <description> (#<N>)`, body explains root cause and fix, ending with the co-author line from the current attribution instructions.

## 8. Write the manual test plan

- Write exact steps to reproduce the original report on device and what should now be observed (one checkbox per symptom from step 1), plus a quick regression check of neighbouring behaviour, and any new `connectedAndroidTest` classes to run.
- **Do not wait for manual verification before pushing** — the manual test plan goes into the PR and is executed during review. **Do not use ADB proactively** (see `CLAUDE.md`).

## 9. Push and open the PR

```bash
git push -u origin <branch>
```

- If the push is rejected as non-fast-forward, do not rebase/force-push — ask the user.
- Open the PR with `mcp__github__create_pull_request` targeting `main`:
  - **Title:** `fix: <short description> (#<N>)`
  - **Body:** `## Summary` (the reported symptoms and what changed, bullets), `## Root cause`, `## Tests` (automated tests added + commands run), `## Manual test plan` (unticked markdown checklist from step 8, including the APK install command), then `Closes #<N>` and the PR attribution line.
  - Request review from the issue author/repo owner via `reviewers` when they are not the PR author.
- Comment on the issue (`mcp__github__add_issue_comment`) linking the PR.
- Report the PR link to the user and ask them to run the manual test plan during review.
