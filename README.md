# Mehek Car Android app

A native Android Studio project for controlling an Arduino UNO + HC-05 Bluetooth RC car.

## Features
- App name: Mehek Car
- Connects to an already-paired classic Bluetooth device such as HC-05
- Forward, backward, left, right, stop
- Diagonal movement: G, I, H, J
- Speed levels 0–9 and maximum speed command `q`

## Arduino command compatibility
Matches the Bluetooth command switch in the supplied Arduino sketch:
`F`, `B`, `L`, `R`, `S`, `G`, `I`, `H`, `J`, digits `0`–`9`, and `q`.

## Build APK
1. Install Android Studio on a computer.
2. Open this `MehekCarAndroid` folder.
3. Let Gradle sync and install the Android SDK Platform 35 if prompted.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Install the generated APK from `app/build/outputs/apk/debug/app-debug.apk`.

## Pair and use
1. Power the HC-05 and pair it in Android Bluetooth settings (common PINs are `1234` or `0000`, depending on module setup).
2. Open Mehek Car and allow Nearby devices/Bluetooth permission.
3. Choose the paired HC-05 and tap Connect.
4. Use the controls. Press STOP before disconnecting.

## Important
- This project source is provided; a compiled APK is not included because Android SDK/Gradle build tools are not available in this environment.
- The Arduino sketch must be uploaded and its HC-05 UART baud rate must match the module (commonly 9600 baud).
- Use a low speed initially and keep the car's wheels clear of the ground during first testing.

## Themes and HC-06
The app includes a theme selector: Tarar Bari Green, Ocean Blue, Royal Purple, Light, Sunset Orange, and Red Alert. It can connect to paired HC-05 or HC-06 classic Bluetooth serial modules. Pair the module in Android Settings first.

## Build in GitHub Actions (when local Android Studio is unavailable)
1. Create a private or public GitHub repository and upload the contents of this project folder.
2. Open the repository's **Actions** tab and run **Build Mehek Car APK** (or push to `main`).
3. When the workflow finishes, open the run and download the `Mehek-Car-debug-APK` artifact.
4. Extract the downloaded artifact ZIP to get `app-debug.apk`.

This workflow builds a debug APK; it is not a signed release for Play Store publishing.
