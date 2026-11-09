# Pink Heart Live Wallpaper

Android live wallpaper featuring the supplied portrait, floating hearts, subtle sparkles, gentle image sway, and touch-triggered heart bursts.

## Build
- Android Studio with JDK 17
- Android Gradle Plugin 8.6.1
- Gradle 8.9
- Compile SDK 35, minimum Android 6.0 (API 23)

Open this repository in Android Studio and build the debug APK, or download the APK artifact from the **Android build** GitHub Actions workflow after it succeeds.

The portrait is bundled at `app/src/main/assets/wallpaper.png`.

This is an Android live wallpaper service, not a video player. After installation, select **Pink Heart Live Wallpaper** from Android's live wallpaper picker. Touch the preview/home screen to create heart bursts (device launcher support can vary).

No network permission or third-party runtime dependencies are required.
