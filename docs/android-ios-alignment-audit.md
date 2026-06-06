# Android / iOS V2 Alignment Audit

Date: 2026-06-06

## Scope

This baseline compares the Android project at:

- `/Users/huxiaoran/Documents/xiaoranProjects/MyGolfSwingApp-Android`

against the iOS project at:

- `/Users/huxiaoran/Documents/xiaoranProjects/MyGolfSwingApp`

The Android app uses `V2*` Kotlin files. On iOS, most product surfaces live under `MyGolfSwingApp/V2`, but the current swing-recording player still lives under `Views/JustSwing` while using the current V2 product direction.

## Confirmed Module Mapping

| Product area | Android | iOS reference |
| --- | --- | --- |
| Root tabs | `V2RootScreen.kt`, `V2BottomTabBar.kt` | `V2RootView.swift`, `V2BottomTabBar.swift` |
| Home | `ui/home/V2HomeScreen.kt` | `V2/Views/Home/V2HomeView.swift` |
| Memo | `ui/memo/*` | `V2/Views/Memo/*` |
| AI chat/config | `ui/ai/*`, `ui/settings/V2AIServiceConfigScreen.kt` | `V2/Views/AI/*` |
| Plaza | `ui/plaza/*` | `V2/Views/Plaza/*` |
| Profile | `ui/profile/*` | `V2/Views/Profile/*` |
| Swing records | `ui/swing/*`, `data/model/V2SwingSession.kt` | `Views/JustSwing/*`, `V2/Controllers/V2MemoBridge.swift` |
| Shared drawing kit | `ui/common/V2DrawingKit.kt` | `Views/Common/DrawingKit.swift` |
| Design tokens | `ui/designsystem/DSColors.kt` | `V2/DesignSystem/DSColors.swift` |

## Current Swing Player Parity

Android already has:

- Full-screen black player shell with safe insets.
- Video playback through Media3 ExoPlayer.
- Time display, scrubber, play/pause, speed chips.
- Bottom actions: favorite, growth pool, save, details, delete.
- Drawing overlay and drawing tool panel.
- Larger edit handles and improved line hit targets.

iOS still leads in these areas:

- Frame-level player model: current frame, total frames, frame stepping by `-10/-3/-1/+1/+3/+10`.
- Frame scrubber with custom track/thumb and optional impact markers.
- Impact candidate detection and one-tap jump to candidate impact frames.
- Help sheet explaining playback, drawing, impact frames, favorite, growth pool, save, delete.
- Details panel includes duration, fps, file size, and status.
- Promotion/demotion is confirmed with explicit dialogs and user-facing toast.

## Data / Capability Gaps

Android `V2SwingClip` currently stores only:

- `id`
- `sessionId`
- `videoPath`
- `thumbnailPath`
- `timestamp`
- `durationSec`
- `retention`

This means fps, total frames, file size, and impact candidates are not persisted. The first improvement pass can still calculate fps, total frames, and file size at runtime from local video metadata. Impact detection should be treated as a separate subgoal because it needs audio/frame analysis, validation, and UI integration.

## Recommended Small Goals

1. Swing player polish, no new persisted schema.

   Target: make Android feel closer to iOS by adding runtime video metadata, frame info text, frame-based step buttons, custom compact scrubber, help sheet, and richer details panel.

   Expected size: under 2 hours.

2. Drawing toolbar visual redesign after UX Pilot output.

   Target: replace the current temporary black vertical palette with a V2-style warm white/fairway/gold floating tool surface based on the Figma result.

   Expected size: under 2 hours after design is available.

3. Growth-pool behavior audit.

   Target: confirm whether Android `PROMOTED` clips appear in the same places as iOS V2 memo/growth-pool surfaces. If not, add a bridge or UI treatment without changing old data unexpectedly.

   Expected size: under 2 hours for audit plus a small fix if needed.

4. Impact frame MVP.

   Target: add a lightweight, Android-native impact candidate detector and marker UI.

   Expected size: should be split separately because it is algorithmic and riskier than UI parity.

## Working Rules

- Keep each implementation batch small enough to finish, verify, and explain within about 2 hours.
- Commit after meaningful code changes.
- Prefer the full Android gate before code commits:

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest lintRelease assembleDebug assembleRelease
```

- When a device is connected, install and launch the debug build so the visual result can be checked on the Android phone.
