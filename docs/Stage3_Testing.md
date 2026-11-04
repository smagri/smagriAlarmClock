# Stage 3 — Persistent Storage: Pixel 8 Pro checklist

Stage 3 accepts saving alarm configurations, loading saved alarms, and retaining
them across app restarts (REQUIREMENTS.md, Stage 3; configuration portion of
REQ-028). The basic storage was introduced during Stage 2. This stage checks and
hardens that implementation. Scheduling and ringing are later stages.

## Prepare

1. Build and run the debug app from Android Studio on the Pixel 8 Pro, or run
   `./gradlew :app:installDebug` with USB debugging enabled and the phone unlocked.
2. Keep any existing alarms. Do not uninstall the app or clear its storage during
   these checks: those actions remove local data and are not normal restarts.
3. Use the temporary labels below so you can distinguish test alarms from your own.
   Wait for Save to return to the alarm list before closing the app.

## Manual acceptance checks

- [*] **Save multiple configurations.** Create and save these three alarms:
  - `Stage3 A`: 07:15, Monday and Thursday, enabled.
  - `Stage3 B`: 21:40, Tuesday and Sunday, disabled.
  - A 12:05 alarm with an empty label and no weekdays (one-time), enabled.
  Use the equivalent AM/PM times if the phone uses 12-hour format. Record the
  starting alarm count. Open each alarm again and check its time, label, days,
  and enabled state. They should match exactly.
- [*] **Close and reopen.** Swipe the app away in Recents, then reopen it from its
  launcher icon. Check the count, all three configurations, and time ordering.
  No alarm should be missing or duplicated, and disabled alarms must stay disabled.
- [*] **Persist edits independently.** Change A to 06:50, select Wednesday and
  Saturday instead, change its label to `Stage3 A edited`, and disable it. Save.
  Enable B using its list switch. Close and reopen. A and B must retain their
  changes; the unnamed alarm and any pre-existing alarms must be unchanged.
- [*] **Discard unsaved edits.** Open A, change its label, then Cancel. Also start
  creating a new alarm and Cancel. Close and reopen. A must still have its saved
  label, and no extra alarm should have been stored.
- [*] **Persist confirmed deletion.** Delete B, first cancelling the confirmation
  and checking it remains. Delete it again and confirm. Close and reopen. B must
  stay deleted; the other alarms must remain unchanged.
- [*] **Restart the process.** In Android Settings, open Apps → smagriAlarmClock →
  Force stop and confirm, then reopen the app manually. Saved configurations and
  the deletion must remain intact. Force stop does not clear app storage.
- [*] **Normal phone restart.** Restart the Pixel normally, unlock it, and open the
  app. Check the saved times, labels, exact weekdays, enabled states, and count.
  This checks stored configuration only; alarms do not ring or reschedule yet.
- [*] **Offline loading.** Temporarily enable Airplane mode, reopen the app, and
  verify the saved configurations still load. Restore your previous network settings.
- [*] **Cleanup.** Delete only the temporary alarms you created. Reopen the app and
  verify they stay deleted. If there were no other alarms, the empty state should
  appear. Otherwise, your original alarms should remain.

## Automated checks on Luqa

With an authorised, unlocked Pixel connected, run:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew -I tools/isolated-android-tests.init.gradle :app:connectedDebugAndroidTest
```

The suite covers storage round trips, independent configurations, invalid data,
rejected writes, unfinished atomic writes, UI storage-error recovery, existing
editing controls, and activity recreation/relaunch. Tests use temporary fixtures.
The isolation script (added in Stage 4) targets the separate application ID
`com.smagri.alarmclock.stage4tests`, protecting the normal installed app and its
data from test cleanup. Do not run the plain connectedDebugAndroidTest command
against your personal installation. Use ordinary installDebug without the
isolation script to update your normal app before the manual checklist.

Storage failures and corrupt-file recovery are exercised automatically; no manual
file corruption or storage permission changes are required. A physical reboot is
a separate manual check, not implied by the activity-relaunch tests.

Record the date, app version/commit, Android version, and any failed checkbox when
reporting results. Passing this checklist does not verify scheduling, audio,
snooze, notifications, or missed-alarm recovery.
