# Lockin

Android app that turns the moment you open a distracting app into the moment you watch the videos
you saved. Share a video to Lockin from any app; when you open an app you chose to guard, Lockin
shows a saved video first.

## Stack
Kotlin, Jetpack Compose, Material 3 Expressive with dynamic colour, Hilt, Room, DataStore,
Navigation Compose, Coil, OkHttp. Material 3 is pinned to a 1.5 release candidate because the Expressive APIs
are not public in stable 1.4. Move to stable 1.5 when it ships.

## Layout
`app/src/main/java/com/lockin/app/`

| Package | Purpose |
| --- | --- |
| `domain` | Pure Kotlin rules: link parsing, intervention policy, queue order |
| `data` | Room, DataStore, metadata fetching, installed apps |
| `feature` | Onboarding, library, capture, guarded apps, settings |
| `intervention` | Accessibility service and the full-screen prompt |
| `core` | Theme, shared components, dependency injection |

## Build
Requires JDK 17 and the Android SDK (`compileSdk` 37).

```sh
./gradlew lint testDebugUnitTest assembleDebug   # checks and debug APK
./gradlew connectedDebugAndroidTest              # needs a device or emulator
./gradlew assemblePreview                        # optimised, installable test build
./gradlew assembleRelease bundleRelease          # release build
```

Judge performance on the `preview` build: debug builds run Compose far slower.

## Release
1. Change `applicationId` and `namespace` to a package ID you own. This cannot change after publishing.
2. Create a keystore and copy `keystore.properties.example` to `keystore.properties`.
3. Host `docs/privacy-policy.md` at a public URL.
4. Complete the Play Console steps in `docs/play-release.md`.

Without `keystore.properties`, `assembleRelease` produces an unsigned APK.

## Screenshots
`./gradlew testDebugUnitTest` renders key screens and the icon to `app/build/screenshots`, so visual
changes can be reviewed without a device.
