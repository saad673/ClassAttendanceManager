Class Attendance Manager – Android App
This is a native Android Studio project wrapping the existing Attendance Manager.
Offline
The Attendance Manager HTML is bundled under:
`app/src/main/assets/index.html`
The Android app does NOT request the INTERNET permission. It loads the app from local Android assets.
Attendance data uses browser localStorage inside the WebView and remains on the device.
Build
Install Android Studio.
Open this project folder.
Let Gradle sync.
Select Build > Build APK(s).
The debug APK will be under:
`app/build/outputs/apk/debug/app-debug.apk`
Install on Android
Transfer the APK to the phone and open it, or connect the phone with USB debugging enabled and press Run in Android Studio.
Excel
The app supports the existing Excel import interface. For truly offline Excel parsing, the `vendor/xlsx.full.min.js` file must be present inside `app/src/main/assets/vendor/`.
