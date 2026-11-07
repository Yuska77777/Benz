# Benz — Personal Media Feed

Benz is a private, offline-first Android media viewer with a swipe feed inspired by short-video apps. It only displays media that you choose from your device; it does not download or stream content from the internet.

## Features

- Import multiple photos and videos from your device using Android's file picker.
- Three separate feeds: landscape videos (16:9), portrait videos (9:16), and photos.
- Swipe up/down to move between items.
- Search imported media by its original file name.
- Remembers your imported library between launches using persistent document permissions.
- Videos loop automatically; tap a video to pause or resume.

## Build APK

GitHub Actions builds a debug APK on pushes to `main` and pull requests. Open the repository's **Actions** tab, select the latest **Build Benz APK** run, and download the `Benz-debug-apk` artifact. The APK is inside the downloaded artifact as `app-debug.apk`.

## Notes

- Imported media stays on your device. Benz stores document URIs and display names, not copies of your files.
- The app needs access only to the media files you select in the Android picker.
- Video orientation is classified from its stored width and height metadata.
