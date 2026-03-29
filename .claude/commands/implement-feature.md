Implement a feature from a Linear issue using a git worktree and feature branch, then open a PR for review.

Usage: /implement-feature <linear-issue-id>  (e.g. /implement-feature CHE-10)

---

## 1. Read the Linear issue

- Fetch the issue using the Linear MCP tool (`mcp__linear-server__get_issue`).
- Note the issue title, description, acceptance criteria, and the canonical git branch name from the issue metadata.

## 2. Explore the codebase

- Launch Explore agent(s) to understand the files and patterns relevant to this issue.
- Identify existing utilities, services, and abstractions to reuse — avoid writing new code where suitable implementations already exist.

## 3. Plan in plan mode

- Enter plan mode (`EnterPlanMode`) and produce a written implementation plan before touching any code.
- The plan must include: context/motivation, proposed changes per file, what is deferred, and a verification/test plan section.
- Resolve any scope ambiguities with the user via `AskUserQuestion` before exiting plan mode.

## 4. Create a git worktree

- Use the branch name from the Linear issue (e.g. `humlekottekonsult/che-10-...`).
- Create the worktree at `.trees/<branch-name>`:
  ```
  mkdir -p .trees/<namespace>
  git worktree add .trees/<branch-name> -b <branch-name>
  ```
- Copy all required gitignored files into the new worktree:
  - `local.properties` (without any secrets no longer needed by this branch)
  - `vertexai/app/google-services.json`
  - `vertexai/app/src/main/assets/` contents: `gcp.json`, `imagen-google-services.json`, `chat_system_prompt.txt`, and the DB export JSON
  - Verify `chat_system_prompt.txt` is present — the app crashes on launch without it. If missing from the main tree, check other worktrees under `.trees/`.

## 5. Implement

- Make all changes inside the worktree directory.
- Follow the architecture and conventions in `CLAUDE.md` and `.ai/` docs.
- Include tests alongside implementation per the TDD policy in `CLAUDE.md`.

## 6. Pre-push checklist

**Always `cd` into the worktree directory before running Gradle.** Running from the repo root builds the main tree's code and assets — changes in the worktree will not be included in the APK.

```bash
cd .trees/<branch-name>
JAVA_HOME=/home/kalle/.jdks/jdk-17.0.12 ./gradlew ktlintCheck
JAVA_HOME=/home/kalle/.jdks/jdk-17.0.12 ./gradlew :vertexai:app:assembleDebug
JAVA_HOME=/home/kalle/.jdks/jdk-17.0.12 ./gradlew :vertexai:app:testDebugUnitTest
```
All three must pass before proceeding.

## 7. Commit

- Stage only the relevant files (no gitignored files, no accidental secrets).
- Write a conventional commit message: `type(scope): description`.
- Include `Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>` in the commit body.

## 8. Execute the test plan

**Do not open the PR until this step is complete.**

Work through every item in the test plan — automated and manual:
- For device/E2E steps, use the ADB MCP tools or the `/adb-troubleshoot` command.
- If a step requires action the user must take manually (e.g. visual inspection on device), pause and ask the user to complete it before continuing.
- Only proceed to the PR once all test plan items are checked off or explicitly deferred with the user's agreement.

## 9. Push and open PR

```
git push -u origin <branch-name>
```

Then open a PR using the GitHub MCP tool (`mcp__github__create_pull_request`) targeting `main`:
- **Title:** `<ISSUE-ID>: <issue title>`
- **Body:** Summary (bullet points of what changed), setup notes for reviewers (any gitignored files needed), and the test plan as a markdown checklist.
- Link the issue: add `Closes <ISSUE-ID>` at the bottom of the body.