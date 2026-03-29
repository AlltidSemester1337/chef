Run a structured E2E test session against the Chef app on a connected Android device.

Usage: /adb-device-test [optional: describe what to test]

---

## Learnings from past sessions

Before starting, be aware of these hard-won lessons:

### Coordinate system — physical pixels, not logical dp
`adb exec-out screencap -p` and the MCP screenshot tool both output at the **physical pixel resolution**. Always resolve the device's actual resolution before computing tap targets:

```bash
adb shell wm size     # e.g. "Physical size: 1440x3120"
adb shell wm density  # e.g. "Physical density: 600"
```

Scale factor = density / 160. At 600 dpi: 1 logical dp = 3.75 physical px.
When you see a screenshot displayed at a smaller size (e.g. 360×780), the real coordinates are 4× larger (1440/360 = 4).

### Prefer element-based interaction over coordinates
`mcp__android__tap_element` (by `text`, `content-desc`, or `resource-id`) is far more reliable than coordinate-based taps and survives layout changes. Always prefer it:

```
# Good — finds the element regardless of screen size
mcp__android__tap_element(by="text", value="View Collection")
mcp__android__tap_element(by="content-desc", value="Chat with Chef")

# Use coordinates only for elements with no accessible label (e.g. unlabelled FABs)
# Calculate: physical_x = logical_x * (density / 160)
```

### Use mcp__android__type_text, not adb shell input text
`adb shell input text` breaks on spaces and special characters even with URL-encoding. Always use the MCP tool:

```
# Bad: adb shell input text "What%20can%20I%20cook%3F"
# Good:
mcp__android__type_text(text="What can I cook tonight?")
```

### Wait for elements, don't sleep
For async operations (AI responses, loading screens), use `mcp__android__wait_for_element` instead of arbitrary sleeps:

```
mcp__android__wait_for_element(by="text", value="MODEL", timeout_ms=15000)
```

### Dismiss a bottom sheet
`adb shell input keyevent KEYCODE_BACK` reliably dismisses a `ModalBottomSheet` when the drag gesture is unreliable.

---

## Steps

### 1. Verify device connection

```bash
adb devices
```

If no device is listed, stop and inform the user. Do not proceed.

### 2. Get device metrics

```bash
adb shell wm size && adb shell wm density
```

Note the physical resolution and density. Calculate the dp-to-px scale factor for any coordinate-based taps needed later.

### 3. Install the latest debug build

If working in a git worktree, always build from **inside** the worktree directory — not the repo root. Building from the repo root produces an APK from the main tree and will not include assets or code changes from the worktree:

```bash
# From inside the worktree:
cd .trees/<branch-name>
JAVA_HOME=/home/kalle/.jdks/jdk-17.0.12 ./gradlew :vertexai:app:assembleDebug
```

Then install and launch:

```bash
adb install -r vertexai/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.formulae.chef/.MainActivity
```

> If the app crashes immediately on launch with `FileNotFoundException: chat_system_prompt.txt`, the asset is missing from the worktree. Copy it from another worktree or obtain it from the team:
> ```bash
> cp .trees/<other-branch>/vertexai/app/src/main/assets/chat_system_prompt.txt \
>    .trees/<this-branch>/vertexai/app/src/main/assets/
> ```
> Then rebuild and reinstall.

Wait 2 seconds, then take a screenshot to confirm the app launched:

```bash
adb exec-out screencap -p > /tmp/chef-screen.png
```

Read and display `/tmp/chef-screen.png`.

### 4. Run the test scenarios

Work through the test plan provided by the user (or the standard smoke test below if none is given).

**Standard smoke test:**
- [ ] App launches to Home screen without crash
- [ ] FAB visible on Home → tap opens Chef overlay → send a message → AI responds
- [ ] Overlay dismisses (back key) → returns to Home
- [ ] "View Collection" → recipe list loads → FAB visible
- [ ] Tap a recipe → detail view opens → FAB visible
- [ ] FAB on detail → overlay opens with recipe-loaded greeting
- [ ] Navigate to Chat screen → FAB **not** visible

For each step, use `mcp__android__screenshot` to capture the result, then `mcp__android__tap_element` to interact.

### 5. Check for crashes after each major step

```bash
adb logcat -d -b crash | tail -50
```

If a crash is found, capture the full stack trace and stop — report to the user before continuing.

### 6. Collect logs on failure

If a step behaves unexpectedly:

```bash
adb logcat -c
# reproduce the issue
adb logcat -d -v time | grep -E "com\.formulae\.chef|E/|W/" | tail -200
```

### 7. Report

Summarise:
- Which test steps passed / failed
- Any crashes or error logs found
- Screenshots of key states
- Suggested follow-up if something failed
