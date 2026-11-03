# smagriAlarmClock — Application Requirements

**Version:** 1.1  
**Date:** 3 November 2026  
**Status:** Functional requirements — revised missed-alarm handling  
**Project:** smagriAlarmClock  
**Platform:** Android  
**Language:** Kotlin  
**User Interface:** Jetpack Compose  
**Primary Test Device:** Google Pixel 8 Pro

---

## 1. Application Overview

smagriAlarmClock is a native Android alarm clock application developed using Kotlin and Android Studio.

Its purpose is to provide a reliable, easy-to-use alarm clock with flexible scheduling, configurable snooze durations, and user-selected alarm sounds, including MP3 and FLAC files.

The application must operate locally without requiring an internet connection.

Development will be performed incrementally using Codex on Luqa. Each development stage must be reviewed and tested before proceeding to the next stage.

This document defines the functional requirements. AGENTS.md defines how Codex must work within the project.

## 2. Alarm Creation and Scheduling

### REQ-001 — Multiple Alarms

- Users can create and manage multiple alarms.
- The application must not impose a predefined limit on the number of alarms.
- Each alarm must operate independently.
- Each alarm must have a unique internal identifier.

### REQ-002 — Alarm Times

- Users must be able to configure alarms for any time of day.
- Alarm times must support hour and minute selection.
- The time selection interface must follow Android's 12-hour or 24-hour setting.

### REQ-003 — One-Time Alarms

- Users must be able to create one-time alarms.
- A one-time alarm is scheduled for the next occurrence of its selected time.
- If the selected time has already passed today, schedule it for tomorrow.
- After dismissal, a one-time alarm must not automatically repeat.
- A completed one-time alarm must remain in the alarm list but become disabled.
- Re-enabling a completed one-time alarm must schedule its next occurrence.

### REQ-004 — Repeating Alarms

- Users must be able to select any combination of Monday through Sunday.
- Each weekday must be independently selectable.
- Repeating alarms must activate only on selected weekdays.
- Repeating alarms must automatically schedule subsequent occurrences.
- No weekdays selected means the alarm is a one-time alarm.

### REQ-005 — Enable and Disable

- Every alarm must have an independent on/off switch.
- Enabling an alarm must schedule its next occurrence.
- Disabling an alarm must cancel its pending activation and pending snooze.
- Changing one alarm must not affect other alarms.

## 3. Snooze Functionality

### REQ-006 — Independent Snooze Settings

- Each alarm must have its own snooze configuration.
- Snooze must be independently enabled or disabled for each alarm.

### REQ-007 — Snooze Duration

- Default snooze duration: 10 minutes.
- Minimum snooze duration: 10 minutes.
- Maximum snooze duration: 120 minutes.
- Configurable in increments of 10 minutes.
- Available durations are 10, 20, 30, through 120 minutes.

### REQ-008 — Unlimited Snoozes

- Users must be able to snooze alarms an unlimited number of times.
- Snoozing must stop the current alarm sound and vibration.
- The alarm must reactivate after its configured snooze interval.
- Snoozing must not permanently disable the alarm.
- Snooze intervals are measured as elapsed time from the Snooze action.

### REQ-009 — Optional Snooze

- Snooze can be enabled or disabled individually for each alarm.
- When disabled, the active alarm interface must not display a Snooze button.
- The Dismiss button must always be available.

## 4. Alarm Audio

### REQ-010 — Built-in Alarm Sounds

- The application must support Android's existing alarm sounds.
- Users must be able to select an Android alarm sound when configuring an alarm.
- The application does not need to package its own audio files.
- Android's default alarm sound must be available as a fallback.

### REQ-011 — Custom Audio Files

- Users must be able to select their own audio files.
- At minimum, MP3 and FLAC files must be supported.
- Audio files may be selected using Android's file selection facilities.
- The application must retain appropriate permission to access a selected file.
- Audio files must not need to be copied into the application's private storage unless technically necessary.

### REQ-012 — Independent Audio Selection

- Each alarm must have its own selected audio sound.
- Different alarms may use different audio files.
- Audio selection for one alarm must not affect another alarm.

### REQ-013 — Continuous Audio Playback

- Alarm audio must repeat continuously while the alarm is ringing.
- Playback must stop when the alarm is snoozed or dismissed.
- No automatic playback timeout is required.
- Playback must use appropriate Android audio and alarm mechanisms.

### REQ-014 — Alarm Volume

- Alarm audio must use Android's alarm audio volume settings.
- A separate volume control for each alarm is not required.
- The application must respect Android audio permission and volume policies.

### REQ-015 — Audio Preview

- Users must be able to preview a selected alarm sound.
- Provide Play and Stop controls.
- Preview playback must stop when requested.
- Preview must not interfere with an alarm that is actively ringing.

### REQ-016 — Missing Audio Fallback

- If a selected custom MP3 or FLAC file has been deleted or becomes inaccessible, the alarm must still activate.
- The application must automatically attempt to play Android's default alarm sound instead.
- The missing audio file must not cause the application to crash.
- The application should indicate that the selected audio file is unavailable when the user edits the alarm.
- If the fallback sound is unavailable, the alarm interface must still appear and any enabled vibration must be attempted.

## 5. Alarm Activation and Dismissal

### REQ-017 — Locked-Screen Operation

- Alarms must be capable of activating while the phone is locked.
- The application must request appropriate Android permissions.
- Where Android permits, a full-screen alarm interface must appear.
- The interface must remain usable without first opening the application manually.
- Where full-screen presentation is restricted, provide an appropriate alarm notification and available actions.

### REQ-018 — Active Alarm Interface

- Display the alarm time and optional label.
- Provide large, clearly labelled Snooze and Dismiss buttons.
- Display Snooze only when enabled for that alarm.
- Keep the interface simple and readable.

### REQ-019 — Alarm Ringing Duration

- An alarm must continue ringing until manually snoozed or dismissed.
- The application must not impose an automatic ringing timeout.
- Operation remains subject to Android operating-system restrictions.

### REQ-020 — Silent and Do Not Disturb

- Alarms should ring in Silent or Do Not Disturb modes whenever Android permits.
- Use Android's appropriate alarm audio handling.
- Do not attempt to bypass Android security settings or restrictions.
- If Android permissions or settings prevent the requested behaviour, inform the user where appropriate.

### REQ-021 — Vibration

- Vibration must be configurable independently for each alarm.
- Vibration can be enabled or disabled.
- When enabled, the device should vibrate while the alarm is ringing.
- Vibration must stop when the alarm is snoozed or dismissed.

## 6. User Interface

### REQ-022 — Main Alarm Screen

- Display alarms in a vertically scrollable list.
- Each alarm entry must display its time and configured repeat days.
- Each entry must have a visible enable/disable switch.
- Display the alarm's optional label when present.
- Display relevant additional information such as its selected sound.
- Provide access to a separate Missed Alarms screen.

### REQ-023 — Alarm Editing Screen

- Provide a separate screen for creating and editing alarms.
- Users must be able to configure:
  - Alarm time.
  - One-time or selected repeating weekdays.
  - Optional label.
  - Alarm sound.
  - Snooze enable/disable.
  - Snooze duration.
  - Vibration enable/disable.
- Provide a clear Save action.
- Saving changes must update the stored alarm and its schedule.

### REQ-024 — Time Display

- Follow Android's selected 12-hour or 24-hour time format.
- Display AM/PM when using 12-hour mode.
- Time formatting must be consistent throughout the application.

### REQ-025 — Optional Alarm Labels

- Users may assign a descriptive label to an alarm.
- Labels may be empty.
- Examples include "Wake up", "Exercise", and "Medication".
- Display the label when one is configured.

### REQ-026 — Light and Dark Mode

- Follow Android's system light/dark appearance setting.
- Use readable text and controls.
- Prefer standard Jetpack Compose and Android interface conventions.

### REQ-027 — Alarm Ordering

- Sort alarms by configured time of day.
- Display the earliest time first.
- Automatically update ordering when an alarm's time changes.
- Sorting is based on time of day, not the next scheduled occurrence.

## 7. Alarm Persistence and Reliability

### REQ-028 — Persistent Storage

- Save all alarms and their configuration locally.
- Saved settings must persist when the application closes.
- Saved settings must survive normal device restarts.
- Individual alarm enable/disable states must be preserved.
- Alarm configuration and pending activation state must be managed consistently.
- Persist sufficient scheduling information to reconstruct missed alarm occurrences after a device restart.
- Preserve a history of detected missed alarms in local storage.

### REQ-029 — Device Restart Recovery

- After a device restart, automatically restore enabled alarms.
- Reschedule their next occurrences without requiring the user to open the application, where Android permits.
- Handle Android's locked-device and post-unlock restrictions appropriately.
- Avoid creating duplicate pending alarms during recovery.
- Reconstruct missed alarm occurrences for the period during which the device was switched off, using the saved alarm schedules and available recovery information.
- Do not treat an alarm that was already successfully dismissed before shutdown as a missed alarm.

### REQ-030 — Background Operation

- Alarms must work when the application is not open.
- The application must not require a permanently visible foreground interface.
- Use Android-supported scheduling mechanisms.
- Account for Android restrictions, including explicit Force Stop and permission limitations.

### REQ-031 — Automatic Rescheduling

- Editing an alarm must automatically update its pending schedule.
- Disabling or deleting an alarm must cancel its pending activation.
- Dismissing a repeating alarm must schedule its next selected weekday.
- Dismissing a one-time alarm must disable it.
- Snoozing an alarm must schedule its next snooze activation.

### REQ-032 — Missed Alarms After Device Restart

The application must distinguish between recording missed alarms and automatically ringing missed alarms.

#### REQ-032A — Detect All Missed Alarm Occurrences

- The application must identify all scheduled alarm occurrences that were missed while the phone was switched off, where sufficient saved scheduling information is available.
- The detection process must not be limited to the previous 60 minutes.
- If the phone was switched off for several days, every scheduled occurrence during that period must be identified.
- A repeating alarm may therefore generate multiple missed occurrences.
- Each missed occurrence must be recorded separately.
- Each record must contain:
  - Alarm identifier.
  - Alarm label, if available.
  - Scheduled date.
  - Scheduled time.
  - Whether the occurrence qualified for automatic ringing.
- When an alarm has no label, identify it using its scheduled time.
- Do not create duplicate missed-alarm records during repeated recovery operations.
- The application must not invent occurrences for periods when the alarm was disabled.

#### REQ-032B — Missed Alarms Screen

- Provide a dedicated Missed Alarms screen.
- Display all recorded missed alarm occurrences.
- Group occurrences by calendar date.
- Clearly display the scheduled time and alarm label.
- Show the most recent missed date first.
- The list must be vertically scrollable.
- Each occurrence must remain visible until the user explicitly clears it.
- Provide a Clear History action with a confirmation dialog.
- Clearing history must not delete or disable configured alarms.
- Missed-alarm history must survive normal application closures and device restarts.
- The main alarm screen must provide access to the missed-alarm history.
- Display the number of unreviewed missed occurrences where practical.

#### REQ-032C — Automatic Ringing Within 60 Minutes

- Only missed occurrences scheduled within the previous 60 minutes qualify for automatic ringing after restart.
- Older missed occurrences must appear in history but must not ring automatically.
- If one qualifying occurrence exists, activate that alarm after recovery.
- If multiple qualifying occurrences exist, display one combined ringing alarm interface.
- The combined interface must display all qualifying missed alarm labels and their scheduled times.
- A Snooze or Dismiss action on the combined interface applies to the qualifying group.
- Snoozing the group must reactivate it after the shortest enabled snooze interval among its members.
- If no grouped member has snooze enabled, the Snooze button must not appear.
- When the group is dismissed, the missed occurrences remain recorded in history.

#### REQ-032D — Example Behaviour

Suppose the phone is switched off on Monday at 6:00 PM and switched on again on Thursday at 8:00 AM.

The application contains these repeating alarms:

- Wake up — 7:00 AM every day.
- Exercise — 7:30 AM Monday through Friday.
- Medication — 9:00 PM every day.

After restart:

1. The application reconstructs scheduled occurrences missed during the period the phone was switched off.
2. All reconstructed occurrences appear in the Missed Alarms screen.
3. The 7:00 AM and 7:30 AM Thursday occurrences qualify for automatic ringing because they were scheduled within the previous 60 minutes.
4. The two qualifying occurrences appear together in one ringing alarm interface.
5. Older missed occurrences remain visible in history without automatically ringing.
6. Future scheduled alarms continue normally.

#### REQ-032E — Recovery Accuracy and Limitations

- Use saved alarm schedules and available device restart information to identify missed occurrences.
- The application must preserve enough scheduling information to reconstruct occurrences across multiple days.
- Recovery must account for repeating weekdays, one-time alarms, local time zones, and daylight saving changes.
- Recovery must not modify the intended future schedules of repeating alarms.
- Completed one-time alarms must not automatically repeat.
- Recovery must account for Android restrictions, including locked storage before first unlock.
- If Android does not provide enough information to determine the exact powered-off interval, the application must use a documented recovery strategy and avoid falsely claiming that every occurrence is known with certainty.
- The application must distinguish reconstructed missed occurrences from alarms whose execution outcome cannot be established reliably.

### REQ-033 — Local Time Zone Behaviour

- Alarm times must represent local wall-clock times.
- For example, a 7:00 AM alarm configured in Sydney must still ring at 7:00 AM local time when travelling to Perth.
- Pending alarm schedules must be recalculated after time-zone changes.
- Handle daylight saving time transitions appropriately.
- For repeating alarms, the selected weekday must be evaluated using the device's current local time zone.

### REQ-034 — Alarm Deletion

- Users must be able to delete alarms.
- The application must ask for confirmation before deleting an alarm.
- Confirmed deletion must remove the alarm from local storage.
- Any pending activation or snooze for the deleted alarm must be cancelled.
- Previously recorded missed-alarm history must remain available unless the user separately clears that history.

### REQ-035 — Simultaneous Alarms

- If multiple alarms activate at the same scheduled time, they must be handled sequentially.
- Only one alarm should actively ring at a time.
- Remaining alarms must be placed in a pending queue.
- Dismissing or snoozing the active alarm must allow the next queued alarm to ring.
- Each alarm must retain its own audio, snooze, vibration, and other settings.
- An alarm must not be silently discarded simply because another alarm was ringing.

## 8. Technical Requirements

### REQ-036 — Development Platform

- Native Android application.
- Kotlin programming language.
- Jetpack Compose user interface.
- Gradle build system.
- Maintain the existing Android Studio project configuration unless changes are necessary and approved.

### REQ-037 — Local Operation

- Core alarm functions must operate without internet access.
- No cloud account is required.
- No remote service is required for scheduling alarms, snoozing, dismissing, or playing locally available sounds.
- Missed-alarm history must also operate entirely offline.

### REQ-038 — Android Integration

- Use Android-supported scheduling, audio, notification, and permission APIs.
- Account for exact-alarm permissions and applicable Android platform restrictions.
- Handle background execution appropriately.
- Use appropriate techniques for full-screen alarm presentation.
- Avoid unnecessary permissions.
- Do not circumvent Android security restrictions.

### REQ-039 — Error Handling

- Handle missing audio files.
- Handle unavailable permissions.
- Handle invalid or incomplete configurations.
- Handle recovery failures or incomplete missed-alarm information.
- Provide understandable feedback where user action is required.
- Avoid crashes and loss of saved alarm configurations or history.

### REQ-040 — Maintainability

- Use readable, maintainable Kotlin code.
- Separate user interface, alarm scheduling, data storage, audio playback, and missed-alarm history responsibilities.
- Avoid unnecessary external dependencies.
- Add meaningful automated tests where practical.
- Follow AGENTS.md for Codex working instructions.

## 9. Development Sequence

Development must occur incrementally.

### Stage 1 — Main Alarm Interface

- Display the alarm list.
- Display sample alarms initially if needed.
- Support light and dark mode.
- Follow Android time formatting.

### Stage 2 — Create and Edit Alarms

- Alarm creation screen.
- Alarm editing screen.
- Time selection.
- Weekday selection.
- Alarm labels.
- Enable/disable control.

### Stage 3 — Persistent Storage

- Save alarm configurations.
- Load stored alarms.
- Preserve configuration across application restarts.

### Stage 4 — Scheduling

- Schedule one-time alarms.
- Schedule repeating alarms.
- Enable and disable alarms.
- Update schedules after edits.

### Stage 5 — Ringing and Dismissal

- Activate alarms at their scheduled times.
- Display the active alarm interface.
- Implement Dismiss.
- Handle locked-screen operation.
- Implement notification and permission handling.

### Stage 6 — Snooze

- Independent snooze configuration.
- Durations from 10 to 120 minutes.
- Unlimited snoozes.
- Optional Snooze button.

### Stage 7 — Audio

- Android alarm sound selection.
- Custom MP3 and FLAC selection.
- Continuous playback.
- Audio preview.
- Default sound fallback.

### Stage 8 — Vibration

- Independent vibration settings.
- Start and stop vibration with the alarm.

### Stage 9 — Reliability and Missed Alarms

- Device restart recovery.
- Missed-alarm occurrence reconstruction.
- Dedicated Missed Alarms screen.
- Persistent missed-alarm history.
- Automatic ringing of qualifying recently missed alarms.
- Time-zone handling.
- Simultaneous alarm queue.
- Background-operation testing.

### Stage 10 — Final Testing

- Test the application on the Pixel 8 Pro.
- Verify all agreed functional requirements.
- Fix identified defects.
- Review reliability and usability.

Each stage must be reviewed before proceeding to the next major stage.

## 10. Acceptance Criteria

The application is functionally complete when the following have been verified:

- Multiple alarms can be created, edited, enabled, disabled, and deleted.
- One-time alarms ring at their next occurrence.
- Repeating alarms follow independently selected weekdays.
- Individual alarm configurations remain independent.
- Snooze supports 10-minute increments up to 120 minutes.
- Snooze can be used repeatedly without a predefined limit.
- Snooze can be disabled per alarm.
- Android alarm sounds can be selected.
- Custom MP3 and FLAC files can be selected and played.
- Audio repeats until snoozed or dismissed.
- Missing custom audio falls back to the Android default alarm sound where available.
- Play/Stop audio preview functions correctly.
- Locked-screen alarm controls work within Android restrictions.
- Enabled vibration works correctly.
- Alarms operate when the application is not open.
- Alarm configurations survive normal restarts.
- Enabled alarms are recovered and rescheduled.
- All reconstructable missed alarm occurrences are recorded, including those older than 60 minutes.
- The Missed Alarms screen lists recorded occurrences by date and time.
- Repeating alarms missed over several days generate separate history entries.
- Only qualifying occurrences from the previous 60 minutes automatically ring after restart.
- Multiple qualifying missed occurrences appear in a combined alarm interface.
- Missed-alarm history survives application and device restarts.
- Clearing missed-alarm history does not delete configured alarms.
- Simultaneous scheduled alarms are handled sequentially.
- Time-zone changes preserve local clock times.
- Alarm deletion requires confirmation.
- The interface follows Android's time and theme settings.
- The application builds successfully.
- No critical alarm reliability defects remain in the tested scenarios.

## 11. Development Environment and Git Workflow

The application is developed using two machines.

**Luqa**
- Runs Android Studio and Codex.
- Codex works on the smagriAlarmClock project.
- Development changes are reviewed and tested locally.
- No direct GitHub access is required.

**Texas**
- Runs Android Studio.
- Does not run Codex.
- Is the only machine used to communicate with GitHub.
- Receives synchronised changes through the established Git workflow.

Codex must comply with AGENTS.md.

Codex must not independently commit, push, pull, merge, rebase, reset, or synchronise repositories without explicit user instruction.

## 12. Implementation Notes

This document describes the application's intended functional behaviour.

Android operating-system restrictions may affect exact alarm scheduling, full-screen presentation, device restart recovery, sound playback, and background operation.

While the device is powered off, Android applications cannot execute code or log actual alarm events. Missed occurrences must therefore be reconstructed from saved scheduling information after restart.

The application should distinguish between occurrences known to have been scheduled and occurrences whose actual execution status cannot be established.

If Codex identifies a conflict between a requirement and Android's supported behaviour, it must explain the limitation and propose an alternative before changing the requirement.

Decisions about internal architecture, persistence libraries, and implementation details should be made incrementally and reviewed before significant changes.

---

**Document status:** Version 1.1 — Revised functional requirements  
**Next milestone:** Begin incremental Android development using Codex on Luqa.
