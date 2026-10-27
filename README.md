# Benz

Benz is an Android shortcut-launcher prototype. It is designed to select a shortcut published by another app (such as GameHub) and launch that exact shortcut when the Benz icon is tapped.

## Android limitation

Android's LauncherApps shortcut APIs are restricted to the current Home launcher (or an eligible voice interaction service). To enumerate and launch another app's shortcuts, Benz asks the user to select Benz as the default Home app. This is an Android platform restriction.

## Build

GitHub Actions builds a debug APK on pushes to main and pull requests. The artifact is named `Benz-debug-apk`.

## Test

1. Install the APK.
2. Open **Benz Setup**.
3. Select Benz as the Android Home app when prompted.
4. Choose the app that created the shortcut (GameHub).
5. If Android exposes its shortcuts through LauncherApps, choose the desired shortcut.
6. Tap **Benz** to launch it. Open **Benz Setup** to change the selection.

If GameHub's item is not a real Android ShortcutManager shortcut, Android may not expose it through this API. In that case, we need to inspect how that version of GameHub creates its home-screen icons.
