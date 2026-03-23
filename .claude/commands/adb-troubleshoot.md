Troubleshoot the running Chef app on the connected Android device using ADB.

Do the following steps in order:

1. **Check device connection**
   - Run `adb devices` to confirm a device is connected and authorized.
   - If no device is found, stop and inform the user.

2. **Take a screenshot**
   - Run `adb exec-out screencap -p > /tmp/chef-screen.png`
   - Inform the user that the screenshot was saved to `/tmp/chef-screen.png` and read the file to display it.

3. **Collect recent app logs**
   - Clear the log buffer first: `adb logcat -c`
   - Wait 2 seconds, then capture 200 lines filtered to the app: `adb logcat -d -v time | grep -E "com\.formulae\.chef|Chef|E/|W/" | tail -200`
   - Look for errors (E/) and warnings (W/) and summarize findings.

4. **Check for crashes**
   - Run `adb logcat -d -b crash | tail -100` to check the crash buffer.
   - If a crash is found, show the full stack trace.

5. **Check app process status**
   - Run `adb shell pidof com.formulae.chef` to confirm the app is running.
   - If no PID is returned, the app may have crashed or not be started.

6. **Report findings**
   - Summarize what was found: current screen state, any errors/warnings, crash status.
   - Suggest next steps or ask the user what they'd like to investigate further.
