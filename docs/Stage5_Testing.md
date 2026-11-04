# Stage 5 — Active Alarms and Dismissal: Pixel 8 Pro checklist

This stage implements scheduled activation, an active alarm screen, Dismiss,
locked-screen presentation where Android permits, and notification/permission
handling (REQUIREMENTS.md Stage 5; REQ-017–019 and dismissal parts of REQ-003/031).

**Stage 5 alarms are visual and silent.** Audio playback is Stage 7, snooze is
Stage 6, and vibration is Stage 8. Do not expect sound or a Snooze button yet.
Active occurrences remain saved until dismissed or their alarm is disabled or
deleted; closing the screen does not dismiss them. No automatic timeout is used.

## Prepare

1. Connect and unlock the Pixel. Update the normal app from Android Studio, or run:

   ```bash
   ./gradlew :app:installDebug
   ```

   Do not uninstall the app or clear its storage. Existing alarms should remain.
2. Open the main list. Confirm **Alarms & reminders** access is enabled using
   **Allow alarm scheduling** if needed.
3. If **Enable alarm notifications** appears, tap it and allow notifications. If
   previously denied, the button can open Android notification settings. Enable
   the app's **Active alarms** channel as well as its overall notifications.
4. If **Allow full-screen alarms (optional)** appears, use it to enable Android's
   full-screen alarm access. Android may still choose a heads-up notification
   while the phone is unlocked; that alone is not a failure.
5. Use labels starting with `Stage5` for test alarms and leave your other alarms
   intact. Select times at least two minutes ahead. Keep the device clock and
   time zone unchanged during these checks.

## Manual acceptance checks

- [*] **One-time activation.** Create `Stage5 Once` for a future time with no
  weekdays selected. Save and verify Next. At that time, an active alarm screen
  or alarm notification should be available. The screen must show the configured
  time and label, a large **Dismiss** button, and no Snooze button. No sound or
  vibration is expected in this stage.
- [*] **Dismiss on screen.** Open the alarm screen if Android presented a
  notification. Tap Dismiss. The active screen and notification must close when
  no other alarm is active. The one-time alarm must remain in the list, disabled,
  with no Next occurrence. Close and reopen the app and verify it stays disabled.
- [*] **Notification action.** Enable the one-time alarm again with a time two
  minutes ahead, then put the app in the background. When it activates, use
  **Dismiss** on its notification. The occurrence must stop being active, its
  notification must disappear, and the saved one-time alarm must become disabled.
- [*] **Locked screen.** Schedule `Stage5 Locked`, then lock the Pixel before
  its time. With notification and full-screen access allowed, Android should
  present the alarm screen over the lock screen where supported. Otherwise,
  verify an alarm notification and Dismiss action are available. Dismiss without
  opening the main app. Dismissing must not unlock the phone or bypass its normal
  PIN/biometric security. Record the actual presentation if it differs.
- [*] **No automatic dismissal.** Let an alarm activate and leave it for several
  minutes. It must remain active. Press Home/Back or close the active screen,
  then reopen it from the notification or **Open active alarm** on the main list.
  It must still show the same occurrence until you explicitly dismiss it.
- [*] **Rotation/reopening.** With an alarm active, rotate the phone, then reopen
  the active screen. Its time, label, and Dismiss action must remain correct.
  Dismiss must act once, without duplicate occurrences or crashes.
- [*] **Repeating alarm.** Create `Stage5 Repeat` for two minutes ahead with today
  and another weekday selected. After activation and dismissal, it must remain
  enabled and show the next selected weekday occurrence. The dismissed occurrence
  must not reappear when the app is reopened.
- [*] **Two due alarms.** Create `Stage5 First` and `Stage5 Second` for the same
  future time. Both occurrences must be retained. The active screen shows one
  occurrence and a count of others waiting. Dismiss the displayed occurrence;
  the next must appear. Dismiss it too. Neither occurrence should be lost, and
  both one-time configurations should now be disabled. The order of equal-time
  alarms is determined by their internal identifiers, not their labels.
- [*] **Blank label and time format.** Let an alarm with an empty label activate.
  Its screen must still show its time and Dismiss. Check readability in light/dark
  mode and the phone's preferred 12/24-hour format; restore any settings changed.
- [ ] **Full-screen access denied (optional).** Deny full-screen alarm access
  while leaving notifications enabled, then schedule another test occurrence.
  It must remain usable through its notification and Dismiss action. Restore
  your preferred full-screen setting afterwards.
- [ ] **Notifications denied (optional).** Deny this app's notifications and
  return to it. It must warn that background alarms may be hidden. Let a temporary
  alarm's time pass, then open the main app. **Open active alarm** must let you
  view and dismiss it. Grant notifications again before testing background or
  locked-screen presentation; configurations must remain saved throughout.
- [*] **Disable/delete while active.** With a test alarm active, return to the
  main list and disable it, or delete it with confirmation. Its active occurrence
  and notification must disappear without affecting other alarms.
- [*] **Cleanup.** Dismiss remaining Stage5 occurrences, then delete only the
  Stage5 test configurations. Restore your preferred permissions and appearance.

Opening a notification or pressing Home/Back does not count as Dismiss. Android
can restrict notification presentation or allow a notification to be swiped away;
the saved active occurrence remains accessible from the main list. Do not use
Force stop, uninstall, or device reboot to dismiss alarms in these tests: reliable
restart recovery is Stage 9, and explicit Force stop is an Android restriction.

Existing Stage 4 occurrences that reached their time before this update are not
retroactively activated. Toggle those alarms off/on or choose a new future time
when deliberately testing them.

## Automated tests without removing your normal app

Host checks:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Connected tests must use the isolation script (the test ID retains its Stage 4 name):

```bash
./gradlew -I tools/isolated-android-tests.init.gradle :app:connectedDebugAndroidTest
```

The isolated target is `com.smagri.alarmclock.stage4tests`; Gradle prints that ID
during configuration (a cached configuration may omit that message). Its
temporary installation is separate from the normal app and may be removed by
test cleanup. Never run the plain connectedDebugAndroidTest command against the
normal phone installation. Tests grant permissions only to the isolated package;
display tests restore the phone's 12/24-hour preference after checking it.

Automated coverage includes persisted active occurrences, snapshots after edits,
one-time and repeating dismissal, stale actions, failure preservation, multiple
occurrences, screen recreation/reopening, notification fallback and its Dismiss
action, and the existing scheduling/storage/UI regression suite. Physical locked
screen and heads-up/full-screen presentation must still be checked manually.

Record the date, Android version, failed checkbox, and permission settings when
reporting a problem. This unfinished silent build should not be your only wake-up
alarm. Android references: [time-sensitive notifications](https://developer.android.com/develop/ui/views/notifications/time-sensitive)
and [full-screen access](https://developer.android.com/about/versions/14/behavior-changes-14#secure-fsi).
