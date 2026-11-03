
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

## Codex working rules

1. Work only on the smagriAlarmClock project.
2. Do not inspect, create, modify, or delete unrelated files outside the project directory.
3. Do not modify the user's Emacs configuration or other development environments.
4. Preserve the existing Android Studio project structure and configuration unless changes are necessary.
5. Make changes in small, independently testable increments.
6. Before making significant architectural changes, explain the proposed approach and obtain approval.
7. Prefer straightforward, maintainable Kotlin code over unnecessary complexity.
8. Follow existing project conventions wherever practical.
9. Do not introduce additional libraries or Gradle dependencies without explaining why they are needed.
10. Do not change Codex configuration, permissions, or sandbox settings.
11. Do not run destructive commands or delete project files without explicit approval.
12. Do not create Git commits, push, pull, merge, rebase, or reset branches unless explicitly requested.
13. Do not access GitHub directly from Luqa.

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

- Explain planned changes before implementing each development stage.
- Summarise which files were changed and why.
- Report build errors, unresolved issues, and relevant warnings.
- Keep changes focused on the current task.
- Do not begin implementing additional features without approval.

## Git workflow

The project uses ordinary Git working repositories on two machines:

- Luqa: Codex development and local Android Studio.
- Texas: Android Studio development and the only machine that communicates with GitHub.

Repository synchronisation is controlled by the user. Codex must not independently perform Git synchronisation or push changes.

## Current milestone

Milestone 5: Establish application requirements and begin incremental development.

The immediate priority is to finalise requirements before implementing alarm functionality.

