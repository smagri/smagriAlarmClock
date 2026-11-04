
# AGENTS.md — smagriAlarmClock

## Project overview

smagriAlarmClock is a native Android alarm clock application developed using Kotlin and Android Studio.

The application will support multiple alarms, independently selectable repeat days, configurable snooze durations, and user-selected alarm audio files.

Detailed application requirements will be maintained in REQUIREMENTS.md.

## Development environment

- Project directory: ~/proj/androidStudioApps/smagriAlarmClock
- Programming language: Kotlin
- IDE: Android Studio
- User interface: Jetpack Compose (existing project)
- Test device: Google Pixel 8 Pro
- Build system: Gradle, using the existing project configuration


## Application requirements

The initial requirements are:

- Create and manage multiple alarms.
- Set alarms for any time of day.
- Independently select any combination of Monday through Sunday for repeating alarms.
- Support configurable snooze durations in 10-minute increments.
- Allow selection of custom alarm audio, including MP3 and FLAC.
- Enable and disable individual alarms.
- Provide a clear, easy-to-use Android interface.

These requirements are preliminary. Do not assume unspecified behaviour is final.

Consult REQUIREMENTS.md when it exists. If an implementation decision depends on an unresolved requirement, ask for clarification before proceeding.

## Android-specific considerations

- Use Android-supported APIs and appropriate permission handling.
- Account for alarm scheduling accuracy and Android background execution restrictions.
- Consider operation while the screen is locked or the application is not in the foreground.
- Consider alarm persistence across application restarts and device reboots.
- Do not request unnecessary Android permissions.

## Build and verification

- Use the project's existing Gradle wrapper for builds.
- After changes, run relevant build or test tasks when the environment supports them.
- Report commands executed and their results.
- Do not claim that a feature works on a physical device unless it has actually been tested on that device.
- Clearly identify any remaining manual tests required in Android Studio or on the Pixel 8 Pro.

## Communication and workflow

- Codex may independently implement, debug, build, and test the currently approved development stage.
- Do not require approval for routine coding changes or fixes within the approved stage.
- Request approval before beginning a new development stage.
- Request approval before changing agreed application requirements.
- Summarise significant changes and test results.
- Stop and report when a major design decision or unresolved blocker requires user input.

## Codex working rules

1. Codex is the primary development assistant for smagriAlarmClock on Luqa.
2. Codex may independently create, modify, and delete project files as necessary to implement approved requirements.
3. Codex may inspect and modify development-related files outside the project when necessary for building, testing, or troubleshooting. Explain significant changes first.
4. Do not modify unrelated personal files, Emacs configuration, or other projects without explicit approval.
5. Codex may run Gradle builds, automated tests, lint, ADB, and other development tools independently, subject to its configured sandbox and approval requirements.
6. Preserve the existing Android Studio project structure unless changes are justified.
7. Work incrementally, following REQUIREMENTS.md.
8. Codex may independently fix ordinary compilation errors, test failures, and minor defects.
9. Explain and obtain approval before introducing major dependencies, making significant architectural changes, or changing agreed functional requirements.
10. Report completed work, tests, unresolved problems, and significant file changes.
11. Do not modify Codex configuration or security settings without explicit approval.
12. Do not run destructive system commands or modify unrelated system configurations without explicit approval.

## Git workflow

1. Luqa is the primary Codex development machine.
2. Texas is the only machine permitted to communicate with GitHub.
3. Codex may run local read-only Git commands, including status, diff, and log.
4. Codex may stage and commit completed, tested project changes locally on Luqa.
5. Codex may create local Git branches when useful.
6. Codex must not independently run Git push, pull, fetch, merge, rebase, reset, clean, or force operations.
7. Codex must not access GitHub or other remote Git repositories.
8. Do not amend existing commits or rewrite Git history without explicit approval.
9. Repository synchronisation between Luqa, Texas, and GitHub remains controlled by the user.
10. Before a local commit, report the changes being committed and ensure unrelated files are not included.
  

Repository synchronisation is controlled by the user. Codex must not independently perform Git synchronisation or push changes.

## Current milestone

Milestone 5: Establish application requirements and begin incremental development.

The immediate priority is to finalise requirements before implementing alarm functionality.

