# Motion/Pocket Anti-Theft Detector & 15-Second Grace Siren Plan (With Jogging/Pocket Calibration)

Implement motion/accelerometer detection combined with light sensor calibration to identify when the phone is picked up or removed from a pocket. When armed, it triggers a 15-second grace period prompting the owner for PIN or Biometric verification. If unverified within 15 seconds, it sounds a loud siren alarm.

## User Review Required

> [!IMPORTANT]
> **Jogging & Pocket False-Alarm Prevention:** To prevent false alarms while the owner is running or jogging with the phone in their pocket, `MotionAntiTheftService` will use **Light Sensor calibration** (or tilt threshold) so that movement inside a dark pocket does not trigger the alarm; it only triggers when the phone is exposed to light or picked up from a surface.

## Open Questions

None.

## Proposed Changes

### [Motion Anti-Theft Feature]

#### [NEW] [MotionAntiTheftService.kt](file:///C:/Users/hp/AndroidStudioProjects/MyAlarmApp/app/src/main/java/com/example/myalarmapp/security/MotionAntiTheftService.kt)
- Foreground service registering accelerometer sensor and light sensor listeners.
- Detects pickup/removal from pocket (transition from dark to light + movement) and launches verification activity (`MotionVerifyActivity`).

#### [NEW] [MotionVerifyActivity.kt](file:///C:/Users/hp/AndroidStudioProjects/MyAlarmApp/app/src/main/java/com/example/myalarmapp/MotionVerifyActivity.kt)
- Full-screen activity showing a 15-second countdown timer, PIN input, and BiometricPrompt support.
- If countdown reaches 0 without verification, triggers `SecurityActionExecutor.executeSiren(context)` and captures intruder photo.

#### [MODIFY] [AntiTheftScreen.kt](file:///C:/Users/hp/AndroidStudioProjects/MyAlarmApp/app/src/main/java/com/example/myalarmapp/ui/AntiTheftScreen.kt)
- Add "Pocket / Motion Anti-Theft Mode" switch toggle.

## Verification Plan

### Automated Tests
- Build verification (`app:assembleDebug`).

### Manual Verification
- Enable Motion Anti-Theft mode, keep phone in dark pocket while moving (verifying no false alarm), then pull it out / pick it up, wait 15 seconds without verifying, and verify the siren sounds.
