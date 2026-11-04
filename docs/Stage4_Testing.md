# Stage 4 — Scheduling: Pixel 8 Pro checklist

Stage 4 schedules one-time and repeating alarms, replaces schedules after edits,
and cancels them when disabled or deleted (REQUIREMENTS.md, Stage 4; scheduling
parts of REQ-003–005, REQ-023, REQ-031, and REQ-034).

**Alarms do not ring yet.** Stage 5 adds the ringing screen and Dismiss. Stage 4
records delivery and advances repeating schedules. A delivered one-time alarm
stays enabled pending Stage 5 dismissal, but is not scheduled again automatically.

## Prepare without removing your app

1. Keep the Pixel connected and unlocked. Update the normal app using Android
   Studio Run, or `./gradlew :app:installDebug`. This is an update; do not uninstall
   the app or clear its storage. Existing Stage 3 alarms should remain intact.
2. Open the app. Existing enabled alarms now receive schedules. Confirm their
   labels, times, weekdays, and enabled states have not changed.
3. If asked for alarm access, tap **Allow alarm scheduling**. Enable the app under
   Android's **Alarms & reminders**, then return. This permission is needed for
   exact alarm scheduling on recent Android versions. If you decline, configurations
   must stay saved and the list must say they are not scheduled.
4. Use the temporary labels below. Record each expected date/time and the phone's
   current local date and time. Do not change the phone's clock or time zone to
   accelerate these tests; that can affect your other apps' alarms.

## Manual acceptance checks

- [x] **One-time alarm today.** Add `Stage4 Once` at least two minutes in the
  future, with no weekdays selected. Save. The row's **Next** must show today and
  the selected time. Close and reopen; it must retain that same occurrence.
- [x] **One-time alarm tomorrow.** Add `Stage4 Tomorrow` for a time already passed
  today, without weekdays. **Next** must show tomorrow. Reopen the app and check
  it has not moved to another date.
- [x] **Independent weekdays.** Add `Stage4 Repeat` with only Monday, Thursday,
  and Sunday selected. **Next** must be the next future occurrence on one of those
  days. If today's selected time has passed, it must choose the next selected day.
- [x] **Edit replaces the occurrence.** Edit Once to a different future time.
  Save. **Next** must change to the new date/time. Edit Repeat's time and weekdays;
  its next occurrence must update accordingly. Other alarms' next times must stay
  unchanged. Editing only a label must leave its occurrence unchanged.
- [x] **Disable and re-enable.** Disable Tomorrow. It must remain saved, show
  Disabled, and have no Next occurrence. Re-enable it; it must show the next
  occurrence of the configured time. Repeat must remain unchanged.
- [x] **Delivery without ringing.** Leave Once enabled, put the app in the
  background, and let its selected time pass. Open the app. It should show
  **Time reached** and no Next for that one-time alarm. It must not ring or open a
  ringing screen. Reopen again; it must not re-arm for tomorrow. Toggle it off/on
  to deliberately schedule its next occurrence.
- [x] **Repeating delivery.** For a quick check, edit Repeat to include today and
  another weekday, with a time two minutes ahead. Save and wait past that time
  with the app in the background. Reopen. Next must now point to the next selected
  weekday occurrence, without changing the configured repeat days.
- [x] **Delete cancels.** Set Tomorrow to a future time, then delete it. Cancel
  the confirmation first and check it remains. Delete again and confirm. It must
  stay deleted after reopening, and other alarms must remain intact.
- [x] **Permission denial/recovery (optional).** In Android Settings → Apps →
  Special app access → Alarms & reminders, deny this app's access. Reopen it.
  Saved configurations must remain, with a clear not-scheduled message. Grant
  access again and return; enabled pending occurrences should be scheduled again.
  Android may stop the app when access is revoked, which is expected.
- [x] **Cleanup.** Delete only your Stage4 test alarms using confirmation. Leave
  your original alarms intact and restore your preferred alarm access setting.

The Next label shows the intended occurrence. If a scheduling error banner is
present, it does not prove Android accepted that schedule. For direct OS-level
verification, use Android Studio's Terminal with an authorised device:

```bash
adb shell dumpsys alarm
```

Search the output for `com.smagri.alarmclock` and its `alarm://` operation URIs.
An enabled pending alarm should have a matching trigger; an edited occurrence's
old URI and a disabled/deleted occurrence should not remain scheduled. Android's
output can also mention historical deliveries, which are not pending schedules.
This command reads state and does not change the phone.

## Automated testing safely

Host-only checks do not install or remove anything on the phone:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

For connected tests, **always use the isolation script**:

```bash
./gradlew -I tools/isolated-android-tests.init.gradle :app:connectedDebugAndroidTest
```

The target must print `Isolated Android test target:
com.smagri.alarmclock.stage4tests`. Tests run under that separate application ID;
its temporary installation can be removed by the runner without removing the
normal `com.smagri.alarmclock` app or its saved configurations. Scheduler device
tests refuse to run against the normal application ID. Do not run the plain
connectedDebugAndroidTest command against your personal installation.

The isolated run temporarily grants alarm access only to its own test package;
display tests temporarily change 12/24-hour format and restore it. After testing,
use the ordinary installDebug command (without the isolation script) if you want
to update the normal app. No extra dependencies or persistent Gradle configuration
changes are required.

Automated coverage includes next-day/week calculations, independent weekdays,
DST gaps and overlaps, replacement/cancellation, stale delivery rejection,
permission denial/grant, storage/scheduling failures, migration of old saved
alarms, distinct PendingIntents, and real AlarmManager delivery to the receiver.

## Stage boundaries and results

Normal DST calculation shifts a nonexistent local time forward by the DST gap.
An ambiguous time uses the earlier offset once. Automatic rescheduling after
device reboot, clock/time-zone changes, and missed-alarm recovery are Stage 9.
After a reboot or explicit Force stop, reopen the app to reconcile schedules in
this stage. Exact access restrictions remain subject to Android settings.

Record the date, Android version, and failed checkbox if reporting a defect.
Do not use this unfinished build as your only wake-up alarm: ringing, audio,
snooze, and vibration are still future stages.


## Acceptance record

Stage 4 is complete.

- Manual acceptance checks: all passed on the Pixel 8 Pro, as confirmed by the
  user after enabling Alarms & reminders access. Checked items above record the
  user's results.
- ADB verification: exact-alarm access allowed; enabled pending occurrences
  registered with Android; the disabled alarm had no pending occurrence; the
  delivered one-time occurrence was recorded without automatically re-arming.
- Automated verification: 22 host unit tests and 20 isolated connected tests
  passed, with no failures or skipped connected tests.
- Debug build, Android-test compilation, and lint passed. Lint retains 16 existing
  warnings about tooling/dependency versions, an obsolete custom check, a
  redundant activity label, and unused resources.
- Connected tests used the separate test application ID. The normal app,
  launcher entry, and saved-alarm file remained present afterwards.

No further Stage 4 implementation is pending. Stage 5 (ringing and dismissal)
requires separate approval. Stage 4 changes remain available for local review
and commit; no repository synchronisation is performed by this checklist.
